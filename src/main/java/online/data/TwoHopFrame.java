package online.data;

import offline.Path;
import online.QueryResult;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TwoHopFrame {
    private List<Path> data;
    private int vertexNumber;
    private PathComparator pathComparator = new PathComparator();

    public int getVertexNumber() {
        return vertexNumber;
    }

    public class PathComparator implements Comparator<Path> {
        public int compare(Path path1, Path path2) {
            if (path1.vertex == path2.vertex)
                return path1.vertexTo - path2.vertexTo;
            return path1.vertex - path2.vertex;
        }
    }

    public TwoHopFrame(String cityCode) {
        data = readData(cityCode);
        data.sort(new PathComparator());
    }

    private List<Path> readData(String cityCode) {
        String path = String.format("data/%s_undir_2hop.txt", cityCode);
        File file = new File(path);

        List<Path> data = new ArrayList<>();

        try (FileReader fileReader = new FileReader(file)) {
            Scanner scanner = new Scanner(fileReader);

            vertexNumber = scanner.nextInt();
            scanner.nextLine();
            while (scanner.hasNextInt()) {
                Path item = new Path(scanner.nextLine());
                data.add(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    public void printData() {
        for (Path path: data) {
            System.out.println(path);
        }
    }

    public List<Path> getData() {
        return data;
    }

    public void processQuery(QueryResult queryResult) {
        List<Path> paths = new ArrayList<>(vertexNumber);

        boolean[] vertexIncluded1 = new boolean[vertexNumber];
        for (int i = 0; i < vertexNumber; i++) vertexIncluded1[i] = false;
        for (Path path: data) {
            vertexIncluded1[path.vertex] = queryResult.vertexIncluded[path.vertex];
            vertexIncluded1[path.vertexTo] = queryResult.vertexIncluded[path.vertexTo];
        }

        double budget = queryResult.query.budget;
        boolean[] vertexIncluded2 = new boolean[vertexNumber];
        for (int vertex = 0; vertex < queryResult.vertexNumber; vertex++) {
            vertexIncluded2[vertex] = false;
            if (vertexIncluded1[vertex]) {
                double totalWeight = queryResult.getVertexWeight(vertex);
                totalWeight += getPathWeight(queryResult.query.start, vertex);
                totalWeight += getPathWeight(vertex, queryResult.query.end);

                if (totalWeight <= budget) {
                    vertexIncluded2[vertex] = true;
                }
            }
        }

        queryResult.vertexIncluded = vertexIncluded2;
    }

    public List<Integer> getPath(int start, int end) {
        List<Integer> result = new ArrayList<>();
        getPath(result, start, end);
        return result;
    }

    private void getPath(List<Integer> result, int start, int end) {
        result.add(start);
        if (start == end) return;

        int[] hop1 = getBestHopOneDirection(start, end);
        int[] hop2 = getBestHopOneDirection(end, start);

        int nextVertex;
        if (hop1[1] < 0 || hop2[1] >= 0 && hop2[0] > hop1[0]) {
            nextVertex = hop1[2];
        } else  {
            nextVertex = hop2[3];
        }

        if (nextVertex < 0) return;

        getPath(result, nextVertex, end);
    }

    public int getPathWeight(int start, int end) {
        int weight1 = getBestWeightOneDirection(start, end);
        int weight2 = getBestWeightOneDirection(end, start);

        if (weight1 < weight2) {
            return weight1;
        } else {
            return weight2;
        }
    }

    private int getBestWeightOneDirection(int start, int end) {
        // Returns result as array: [weight, pivot, nextVertex, lastVertex]

        List<Path> paths = data;

        // Binary search of starting points
        Path sampleStartPath = new Path(start, 0);
        int startI = Collections.binarySearch(data, sampleStartPath, Comparator.comparingInt((Path p) -> p.vertex));

        if (startI < 0) return Integer.MAX_VALUE;
        int deltaI = 0;
        while (startI + deltaI - 1 >= 0 && paths.get(startI + deltaI - 1).vertex == start) {
            --deltaI;
        }

        int minWeight = Integer.MAX_VALUE;

        while (paths.get(startI + deltaI).vertex == start) {
            Path startPath = paths.get(startI + deltaI);
            int pivot = startPath.vertexTo;

            // Take both forward and backward paths
            for (int i = 0; i < 2; i++) {
                Path endPath;
                if (i == 0) endPath = new Path(pivot, end);
                else endPath = new Path(end, pivot);

                int endI = Collections.binarySearch(data, endPath, pathComparator);

                if (endI >= 0) {
                    int weight = startPath.weight + paths.get(endI).weight;
                    if (weight < minWeight) {
                        minWeight = weight;
                    }
                }
            }

            deltaI += 1;
        }

        return minWeight;
    }

    private int[] getBestHopOneDirection(int start, int end) {
        // Returns result as array: [weight, pivot, nextVertex, lastVertex]

        List<Path> paths = data;

        // Binary search of starting points
        Path sampleStartPath = new Path(start, 0);
        int startI = Collections.binarySearch(data, sampleStartPath, Comparator.comparingInt((Path p) -> p.vertex));

        if (startI < 0) return new int[]{Integer.MAX_VALUE, -1};
        int deltaI = 0;
        while (startI + deltaI - 1 >= 0 && paths.get(startI + deltaI - 1).vertex == start) {
            --deltaI;
        }

        int minWeight = Integer.MAX_VALUE;
        int minPivot = -1;
        int nextVertex = -1, lastVertex = -1;

        while (paths.get(startI + deltaI).vertex == start) {
            Path startPath = paths.get(startI + deltaI);
            int pivot = startPath.vertexTo;

            // Take both forward and backward paths
            for (int i = 0; i < 2; i++) {
                Path endPath;
                if (i == 0) endPath = new Path(pivot, end);
                else endPath = new Path(end, pivot);

                int endI = Collections.binarySearch(data, endPath, pathComparator);

                if (endI >= 0) {
                    endPath = paths.get(endI);

                    int weight = startPath.weight + endPath.weight;
                    if (weight < minWeight) {
                        minWeight = weight;
                        minPivot = pivot;

                        // Next vertex and last vertex calculations
                        nextVertex = startPath.nextVertex;
                        if (nextVertex == start) {
                            nextVertex = i == 0 ? endPath.nextVertex : endPath.lastVertex;
                        }
                        lastVertex = i == 0 ? endPath.lastVertex : endPath.nextVertex;
                        if (lastVertex == end) {
                            lastVertex = startPath.lastVertex;
                        }
                    }
                }
            }

            deltaI += 1;
        }

        return new int[]{minWeight, minPivot, nextVertex, lastVertex};
    }

}
