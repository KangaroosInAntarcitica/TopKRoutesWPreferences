package online;

import org.apache.spark.sql.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TwoHopFrame {
    private Dataset<Row> dataFrame;

    @lombok.Getter
    @lombok.Setter
    public class VertexConnection implements Serializable {
        private int vertex;
        private int vertexTo;
        private int weight;

        public String toString() {
            return String.format("%d -> %d (%d)", vertex, vertexTo, weight);
        }
    }

    private List<VertexConnection> readData(String path) {
        File file = new File(path);

        List<VertexConnection> data = new ArrayList<>();

        try (FileReader fileReader = new FileReader(file)) {
            Scanner scanner = new Scanner(fileReader);

            int vertexNumber = scanner.nextInt();
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

        return data;
    }

    public TwoHopFrame(SparkSession spark, String path) {
        List<VertexConnection> data = readData(path);
        dataFrame = spark.createDataFrame(data, VertexConnection.class);
    }

    public Dataset<Row> retrieveForVertexes(Dataset<Row> vertexDataFrame, int maxWeight) {
        return vertexDataFrame.join(dataFrame, "vertex")
                .filter(functions.col("weight").$less$eq(maxWeight))
                .drop("rating")
                .drop("feature");
    }
}
