package offline;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

import java.io.*;
import java.util.*;

public class EdgeFrame {
    private List<VertexConnection> data = null;
    private List<VertexConnection> data2Hop = null;
    private int vertexNumber;
    private List<Integer> ranking;

    @lombok.Getter
    @lombok.Setter
    private class VertexConnection extends Vertex {
        private int vertex;
        private int vertexTo;
        private int weight;

        public String toString() {
            return String.format("%d -> %d (%d)", vertex, vertexTo, weight);
        }

        @Override
        public int hashCode() {
            return vertex * vertexNumber + vertexTo;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof VertexConnection)) return false;
            VertexConnection otherConnection = (VertexConnection) other;
            return this.vertex == otherConnection.vertex && this.vertexTo == otherConnection.vertexTo;
        }
    }

    @lombok.Getter
    @lombok.Setter
    private class Vertex implements Serializable {
        private int vertex;
        private int count;

        public String toString() {
            return String.format("%d - %d", vertex, count);
        }
    }

    public void readData(String path) {
        if (this.data != null) {
            throw new RuntimeException("This object has already read a graph");
        }

        File file = new File(path);

        List<VertexConnection> data = null;

        try (FileReader fileReader = new FileReader(file)) {
            Scanner scanner = new Scanner(fileReader);

            this.vertexNumber = scanner.nextInt();

            data = new ArrayList<>(vertexNumber * 3);

            while (scanner.hasNextInt()) {
                VertexConnection item = new VertexConnection();
                item.vertex = scanner.nextInt();
                item.vertexTo = scanner.nextInt();
                item.weight = scanner.nextInt();
                data.add(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.data = data;
    }

    public void writeData(String path) {
        if (this.data2Hop == null) {
            throw new RuntimeException("This object has not generated the data yet");
        }

        File file = new File(path);

        StringBuilder builder = new StringBuilder();
        builder.append(vertexNumber);
        builder.append("\n");
        for (VertexConnection connection: data2Hop) {
            builder.append(String.format("%d %d %d\n", connection.vertex, connection.vertexTo, connection.weight));
        }

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int createId(int vertex, int vertexTo) {
        return vertex * vertexNumber + vertexTo;
    }

    public void generate2HopTable() {
        if (this.data2Hop != null) {
            throw new RuntimeException("This object has already generated all the data");
        }

        // Rank
        rank();

        Map<Integer, Integer> allLabel = new HashMap<>(vertexNumber);
        Map<Integer, Integer> currentLabel;
        Map<Integer, Integer> prevLabel = new HashMap<>(vertexNumber);

        // Initialization
        for (int i = 0; i < vertexNumber; i++) {
            allLabel.put(createId(i, i), 0);
        }

        for (VertexConnection connection: data) {
            // the path goes from the label with lower rank
            int key;
            if (ranking.get(connection.vertex) > ranking.get(connection.vertexTo)) {
                key = createId(connection.vertexTo, connection.vertex);
            } else {
                key = createId(connection.vertex, connection.vertexTo);
            }

            prevLabel.put(key, connection.weight);
        }
        allLabel.putAll(prevLabel);

        // Main loop
        while (prevLabel.size() != 0) {
            System.out.println("Iteration - " + prevLabel.size() + " " + allLabel.size());
            currentLabel = new HashMap<>();

            for (int connection: prevLabel.keySet()) {
                int vertex = connection / vertexNumber;
                int vertexTo = connection % vertexNumber;

                for (int connection2: allLabel.keySet()) {
                    int vertex2 = connection2 / vertexNumber;
                    int vertexTo2 = connection2 % vertexNumber;

                    // Rule 1
                    if (vertex == vertex2) {
                        if (ranking.get(vertexTo) > ranking.get(vertexTo2) && ranking.get(vertexTo2) > ranking.get(vertex)) {
                            int newConnection = createId(vertexTo2, vertexTo);
                            int newWeight = prevLabel.get(connection) + allLabel.get(connection2);
                            if (!allLabel.containsKey(newConnection) || allLabel.get(newConnection) > newWeight)
                                currentLabel.put(newConnection, newWeight);
                        }
                    }
                    // Rule 2
                    if (vertex == vertexTo2) {
                        int newConnection = createId(vertex2, vertexTo);
                        int newWeight = prevLabel.get(connection) + allLabel.get(connection2);
                        if (!allLabel.containsKey(newConnection) || allLabel.get(newConnection) > newWeight)
                            currentLabel.put(newConnection, newWeight);
                    }
                }
            }

            for (int connection: currentLabel.keySet()) {
                if (!allLabel.containsKey(connection) || allLabel.get(connection) > currentLabel.get(connection)) {
                    allLabel.put(connection, currentLabel.get(connection));
                }
            }

            prevLabel = currentLabel;
        }

        // save the result to data2Hop
        List<VertexConnection> data2Hop = new ArrayList<>();
        for (int connection: allLabel.keySet()) {
            VertexConnection item = new VertexConnection();
            item.vertex = connection / vertexNumber;
            item.vertexTo = connection % vertexNumber;
            item.weight = allLabel.get(connection);
            data2Hop.add(item);
        }
        this.data2Hop = data2Hop;
    }

    private void printVertexMap(Map<Integer, Integer> data) {
        // Auxiliary function for testing
        for (int key: data.keySet())
            System.out.printf("%d -> %d - %d\n", key / vertexNumber, key % vertexNumber, data.get(key));
    }

    private void rank() {
        List<Vertex> allVertexes = new ArrayList<>(vertexNumber);
        for (int i = 0; i < vertexNumber; i++) {
            Vertex vertex = new Vertex();
            vertex.vertex = i;
            vertex.count = 0;
            allVertexes.add(vertex);
        }

        for (VertexConnection vertexConnection: data) {
            ++allVertexes.get(vertexConnection.vertex).count;
            ++allVertexes.get(vertexConnection.vertexTo).count;
        }

        allVertexes.sort((Vertex v1, Vertex v2) -> v1.count - v2.count);

        List<Integer> ranking = new ArrayList<>(vertexNumber);
        for (int i = 0; i < vertexNumber; i++) {
            ranking.add(0);
        }
        for (int i = 0; i < vertexNumber; i++) {
            ranking.set(allVertexes.get(i).vertex, i);
        }
        this.ranking = ranking;
    }

    public void processEdges(String pathFrom, String pathTo) {
        readData(pathFrom);
        generate2HopTable();
        writeData(pathTo);
    }

    public void clear() {
        // Clears the object - can generate another graph now
        this.data = null;
        this.data2Hop = null;
        this.vertexNumber = 0;
        this.ranking = null;
    }
}
