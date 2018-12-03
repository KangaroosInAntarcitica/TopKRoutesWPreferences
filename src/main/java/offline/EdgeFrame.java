package offline;

import java.io.*;
import java.util.*;

public class EdgeFrame {
    private List<Path> data = null;
    private List<Path> data2Hop = null;
    private int vertexNumber;
    private List<Integer> ranking;

    // Used for ranking
    private class Vertex implements Serializable {
        private int vertex;
        private int count;

        public String toString() {
            return String.format("%d - %d", vertex, count);
        }
    }

    // To represent Path in a HashMap
    private int createId(int vertex, int vertexTo) {
        return vertex * vertexNumber + vertexTo;
    }
    private class PathData {
        // This class is used in a HashMap, where the vertexes represent the key
        private int weight;
        private int nextVertex;
        private int lastVertex;

        private PathData(int weight, int nextVertex, int lastVertex) {
            this.weight = weight;
            this.nextVertex = nextVertex;
            this.lastVertex = lastVertex;
        }

        private PathData() {}
    }

    public void readData(String cityCode) {
        String path = String.format("data/%s_undir_edges.txt", cityCode);

        if (this.data != null) {
            throw new RuntimeException("This object has already read a graph");
        }

        File file = new File(path);

        List<Path> data = null;

        try (FileReader fileReader = new FileReader(file)) {
            Scanner scanner = new Scanner(fileReader);

            this.vertexNumber = scanner.nextInt();
            scanner.nextLine();

            data = new ArrayList<>(vertexNumber * 3);

            while (scanner.hasNextInt()) {
                Path item = new Path(scanner.nextLine());
                data.add(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.data = data;
    }

    public void writeData(String cityCode) {
        String filePath = String.format("data/%s_undir_2hop.txt", cityCode);

        if (this.data2Hop == null) {
            throw new RuntimeException("This object has not generated the data yet");
        }

        File file = new File(filePath);

        StringBuilder builder = new StringBuilder();
        builder.append(vertexNumber);
        builder.append("\n");
        for (Path path: data2Hop) {
            builder.append(path);
            builder.append("\n");
        }

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Path> getData() {
        return data;
    }

    public void generate2HopTable() {
        if (this.data2Hop != null) {
            throw new RuntimeException("This object has already generated all the data");
        }

        // Rank
        rank();

        Map<Integer, PathData> allLabel = new HashMap<>(vertexNumber);
        Map<Integer, PathData> currentLabel;
        Map<Integer, PathData> prevLabel = new HashMap<>(vertexNumber);

        // Initialization
        for (int i = 0; i < vertexNumber; i++) {
            allLabel.put(createId(i, i), new PathData(0, i, i));
        }

        // Initialization - edges
        for (Path connection: data) {
            // the path goes from the label with lower rank
            PathData pathData = new PathData();
            int key;
            if (ranking.get(connection.vertex) > ranking.get(connection.vertexTo)) {
                key = createId(connection.vertexTo, connection.vertex);
                pathData.nextVertex = connection.vertex;
                pathData.lastVertex = connection.vertexTo;
            } else {
                key = createId(connection.vertex, connection.vertexTo);
                pathData.nextVertex = connection.vertexTo;
                pathData.lastVertex = connection.vertex;
            }
            pathData.weight = connection.weight;
            prevLabel.put(key, pathData);
        }
        allLabel.putAll(prevLabel);

        // Main loop
        while (prevLabel.size() != 0) {
            System.out.format("Iteration - prevLabel.size: %10d, allLabel.size: %10d\n", prevLabel.size(), allLabel.size());
            currentLabel = new HashMap<>();

            for (int pathId: prevLabel.keySet()) {
                int vertex = pathId / vertexNumber;
                int vertexTo = pathId % vertexNumber;
                PathData pathData = prevLabel.get(pathId);

                for (int pathId2: allLabel.keySet()) {
                    int vertex2 = pathId2 / vertexNumber;
                    int vertexTo2 = pathId2 % vertexNumber;
                    PathData pathData2 = allLabel.get(pathId2);

                    // Rule 1
                    if (vertex == vertex2) {
                        if (ranking.get(vertexTo) > ranking.get(vertexTo2) /* && ranking.get(vertexTo2) > ranking.get(vertex) */) {
                            int newConnection = createId(vertexTo2, vertexTo);
                            int newWeight = pathData.weight + pathData2.weight;
                            if (!allLabel.containsKey(newConnection) || allLabel.get(newConnection).weight > newWeight) {
                                int nextVertex = pathData2.lastVertex;
                                int lastVertex = pathData.lastVertex;
                                currentLabel.put(newConnection, new PathData(newWeight, nextVertex, lastVertex));
                            }
                        }
                    }
                    // Rule 2
                    if (vertex == vertexTo2) {
                        int newConnection = createId(vertex2, vertexTo);
                        int newWeight = prevLabel.get(pathId).weight + allLabel.get(pathId2).weight;
                        if (!allLabel.containsKey(newConnection) || allLabel.get(newConnection).weight > newWeight) {
                            int nextVertex = pathData2.nextVertex;
                            int lastVertex = pathData.lastVertex;
                            currentLabel.put(newConnection, new PathData(newWeight, nextVertex, lastVertex));
                        }
                    }
                }
            }

            for (int connection: currentLabel.keySet()) {
                if (!allLabel.containsKey(connection) || allLabel.get(connection).weight > currentLabel.get(connection).weight) {
                    allLabel.put(connection, currentLabel.get(connection));
                }
            }

            prevLabel = currentLabel;
        }

        // save the result to data2Hop
        List<Path> data2Hop = new ArrayList<>();
        for (int connection: allLabel.keySet()) {
            Path item = new Path();
            item.vertex = connection / vertexNumber;
            item.vertexTo = connection % vertexNumber;
            PathData connectionData = allLabel.get(connection);
            item.weight = connectionData.weight;
            item.nextVertex = connectionData.nextVertex;
            item.lastVertex = connectionData.lastVertex;
            data2Hop.add(item);
        }
        this.data2Hop = data2Hop;
    }

    private void printVertexMap(Map<Integer, PathData> data) {
        // Auxiliary function for testing
        for (int key: data.keySet())
            System.out.printf("%d -> %d - %d\n", key / vertexNumber, key % vertexNumber, data.get(key).weight);
    }

    private void rank() {
        List<Vertex> allVertexes = new ArrayList<>(vertexNumber);
        for (int i = 0; i < vertexNumber; i++) {
            Vertex vertex = new Vertex();
            vertex.vertex = i;
            vertex.count = 0;
            allVertexes.add(vertex);
        }

        for (Path path : data) {
            ++allVertexes.get(path.vertex).count;
            ++allVertexes.get(path.vertexTo).count;
        }

        allVertexes.sort(Comparator.comparingInt((Vertex v) -> v.count));

        List<Integer> ranking = new ArrayList<>(vertexNumber);
        for (int i = 0; i < vertexNumber; i++) {
            ranking.add(0);
        }
        for (int i = 0; i < vertexNumber; i++) {
            ranking.set(allVertexes.get(i).vertex, i);
        }
        this.ranking = ranking;
    }

    public void processEdges(String cityCode) {
        readData(cityCode);
        generate2HopTable();
        writeData(cityCode);
    }

    public void clear() {
        // Clears the object - can generate another graph now
        this.data = null;
        this.data2Hop = null;
        this.vertexNumber = 0;
        this.ranking = null;
    }
}
