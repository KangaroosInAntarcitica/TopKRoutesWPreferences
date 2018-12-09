package online.data;

import offline.Path;
import online.QueryResult;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import online.data.WeightFrame.VertexWeight;

public class TwoHopFrame {
    private List<Path> data;
    private List<List<Path>> dataStartVertex;
    private int vertexNumber;
    private PathComparator pathComparator = new PathComparator();

    public int getVertexNumber() {
        return vertexNumber;
    }

    public class PathComparator implements Comparator<Path> {
        public int compare(Path path1, Path path2) {
            if (path1.getVertex() == path2.getVertex())
                return path1.getVertexTo() - path2.getVertexTo();
            return path1.getVertex() - path2.getVertex();
        }
    }

    public TwoHopFrame(String cityCode) {
        readData(cityCode);
    }

    private void readData(String cityCode) {
        String path = String.format("data/%s_undir_2hop.txt", cityCode);
        File file = new File(path);

        List<Path> data = new ArrayList<>();
        List<List<Path>> dataStartVertex = new ArrayList<>();

        try (FileReader fileReader = new FileReader(file)) {
            Scanner scanner = new Scanner(fileReader);

            vertexNumber = scanner.nextInt();
            scanner.nextLine();
            for (int i = 0; i < vertexNumber; i++) dataStartVertex.add(new ArrayList<>());

            while (scanner.hasNextInt()) {
                Path item = new Path(scanner.nextLine());
                data.add(item);
                dataStartVertex.get(item.getVertex()).add(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < vertexNumber; i++) {
            dataStartVertex.get(i).sort(Comparator.comparingInt((Path p) -> p.getVertexTo()));
        }

        this.data = data;
        data.sort(new PathComparator());
        this.dataStartVertex = dataStartVertex;
    }

    public List<Path> getData() {
        return data;
    }

    public void processQuery(QueryResult queryResult) {
        List<Path> paths = new ArrayList<>(vertexNumber);

        boolean[] vertexIncluded = queryResult.getVertexIncluded();
        boolean[] vertexIncluded1 = new boolean[vertexNumber];
        for (int i = 0; i < vertexNumber; i++) vertexIncluded1[i] = false;
        for (Path path: data) {
            vertexIncluded1[path.getVertex()] = vertexIncluded[path.getVertex()];
            vertexIncluded1[path.getVertexTo()] = vertexIncluded[path.getVertexTo()];
        }

        double budget = queryResult.query.getBudget();
        boolean[] vertexIncluded2 = new boolean[vertexNumber];
        for (int vertex = 0; vertex < queryResult.getVertexNumber(); vertex++) {
            vertexIncluded2[vertex] = false;
            if (vertexIncluded1[vertex]) {
                double totalWeight = queryResult.getVertexWeight(vertex);
                totalWeight += getPathWeight(queryResult.query.getStart(), vertex);
                totalWeight += getPathWeight(vertex, queryResult.query.getEnd());

                if (totalWeight <= budget) {
                    vertexIncluded2[vertex] = true;
                }
            }
        }

        queryResult.setVertexIncluded(vertexIncluded2);
    }

    public void getDataSubIndex(QueryResult queryResult) {
        int vertexNumber = queryResult.getVertexNumber();
        boolean[] vertexIncluded = queryResult.getVertexIncluded();

        int[] usages = new int[vertexNumber];
        for (int i = 0; i < usages.length; i++) usages[i] = 0;

        // Find all the vertexes, that are used as pivots at least twice
        for (int vertex = 0; vertex < vertexNumber; vertex++) {
            if (vertexIncluded[vertex]) {
                for (Path path: dataStartVertex.get(vertex)) {
                    usages[path.getVertexTo()] += 1;
                }
            }
        }

        // Create a list with all required vertexes - subIndex
        List<List<Path>> subDataStartVertex = new ArrayList<>();
        for (int i = 0; i < vertexNumber; i++) subDataStartVertex.add(null);
        for (int vertex = 0; vertex < vertexNumber; vertex++) {
            if (vertexIncluded[vertex]
                    || vertex == queryResult.query.getStart() || vertex == queryResult.query.getEnd()) {
                subDataStartVertex.set(vertex, new ArrayList<>());

                for (Path path : dataStartVertex.get(vertex)) {
                    if (usages[path.getVertexTo()] > 1) {
                        subDataStartVertex.get(vertex).add(path);
                    }
                }
            }
        }

        // Create an array with key - vertex, value - its minimal total weight
        // total weight - weight of vertex + weight of hop to it
        double[] minVertexTotalWeight = new double[vertexNumber];
        for (int i = 0; i < vertexNumber; i++)
            minVertexTotalWeight[i] = Double.MAX_VALUE;
        for (int vertex = 0; vertex < vertexNumber; vertex++) {
            for (Path path: dataStartVertex.get(vertex)) {
                if (usages[path.getVertex()] > 1) {
                    int vertexTo = path.getVertexTo();
                    double totalWeight = path.getWeight() + queryResult.getVertexWeight(vertexTo);

                    if (vertex != vertexTo && totalWeight < minVertexTotalWeight[vertexTo]) {
                        if(vertexIncluded[vertexTo]
                                || vertexTo == queryResult.query.getStart() || vertexTo == queryResult.query.getEnd()) {
                            if (path.getWeight() == 0)
                                System.out.println(path.getWeight());
                            minVertexTotalWeight[vertexTo] = totalWeight;
                        }
                    }
                }
            }
        }

        // sorted list of weights
        // this is indexed according to searchVertexes
        List<VertexWeight> minVertexTotalWeightList = new ArrayList<>();
        for (int i = 0; i < queryResult.getSearchNumber(); i++) {
            int globalI = queryResult.getSearchVertexes().get(i);
            if (minVertexTotalWeight[globalI] <= Double.MAX_VALUE) {
                minVertexTotalWeightList.add(new VertexWeight(i, minVertexTotalWeight[globalI]));
            }
        }
        minVertexTotalWeightList.sort(Comparator.comparingDouble((VertexWeight::getWeight)));

        queryResult.setHops(subDataStartVertex);
        queryResult.setMinVertexTotalWeightByIndex(minVertexTotalWeight);
        queryResult.setMinVertexTotalWeight(minVertexTotalWeightList);
    }

    public List<Integer> getPath(List<List<Path>> dataStartVertex, int start, int end) {
        List<Integer> result = new ArrayList<>();
        getPath(dataStartVertex, result, start, end);
        return result;
    }
    public List<Integer> getPath(int start, int end) {
        return getPath(dataStartVertex, start, end);
    }

    private void getPath(List<List<Path>> dataStartVertex, List<Integer> result, int start, int end) {
        result.add(start);
        if (start == end) return;

        int[] hop = getBestHop(dataStartVertex, start, end);
        int nextVertex = hop[2];
        if (nextVertex < 0) return;

        getPath(dataStartVertex, result, nextVertex, end);
    }

    public int getPathWeight(List<List<Path>> dataStartVertex, int start, int end) {
        int minWeight = Integer.MAX_VALUE;

        List<Path> startPaths = dataStartVertex.get(start);
        List<Path> endPaths = dataStartVertex.get(end);

        int startI = 0;
        int endI = 0;

        while (startI < startPaths.size() && endI < endPaths.size()){
            int startVertexTo = startPaths.get(startI).getVertexTo();
            int endVertexTo = endPaths.get(endI).getVertexTo();

            if (startVertexTo < endVertexTo) {
                ++startI;
            }
            else if (startVertexTo > endVertexTo) {
                ++endI;
            }
            else {
                int weight = startPaths.get(startI).getWeight() + endPaths.get(endI).getWeight();
                if (weight < minWeight) {
                    minWeight = weight;
                }

                ++startI;
                ++endI;
            }
        }

        return minWeight;
    }
    public double getPathWeight(int start, int end) {
        return getPathWeight(dataStartVertex, start, end);
    }

    private int[] getBestHop(List<List<Path>> dataStartVertex, int start, int end) {
        // Returns result as array: [weight, pivot, nextVertex, lastVertex]

        int minWeight = Integer.MAX_VALUE;
        int minPivot = -1;
        int nextVertex = -1;

        List<Path> startPaths = dataStartVertex.get(start);
        List<Path> endPaths = dataStartVertex.get(end);

        int startI = 0;
        int endI = 0;

        while (startI < startPaths.size() && endI < endPaths.size()){
            int startVertexTo = startPaths.get(startI).getVertexTo();
            int endVertexTo = endPaths.get(endI).getVertexTo();

            if (startVertexTo < endVertexTo) {
                ++startI;
            }
            else if (startVertexTo > endVertexTo) {
                ++endI;
            }
            else {
                Path startPath = startPaths.get(startI);
                Path endPath = endPaths.get(endI);
                int pivot = startPath.getVertexTo();
                int weight = startPath.getWeight() + endPath.getWeight();

                if (weight < minWeight) {
                    minWeight = weight;
                    minPivot = startVertexTo;

                    // Next vertex and calculations
                    nextVertex = startPath.getNextVertex();
                    if (pivot == start) nextVertex = endPath.getLastVertex();
                }

                ++startI;
                ++endI;
            }
        }

        return new int[]{minWeight, minPivot, nextVertex};
    }

}
