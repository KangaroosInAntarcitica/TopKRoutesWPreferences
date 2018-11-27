package online;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class WeightFrame {
    private Dataset<Row> dataFrame;

    @lombok.Getter
    @lombok.Setter
    public class VertexWeight {
        private int vertex;
        private double weight;

        public String toString() {
            return String.format("%d (%f)", vertex, weight);
        }
    }

    private List<VertexWeight> readData(String path) {
        File file = new File(path);

        List<VertexWeight> data = new ArrayList<>();

        try (FileReader fileReader = new FileReader(file)) {
            Scanner scanner = new Scanner(fileReader);

            while (scanner.hasNext()) {
                String[] current = scanner.nextLine().split(":");

                VertexWeight item = new VertexWeight();
                item.vertex = Integer.parseInt(current[0]);
                item.weight = Double.parseDouble(current[1]);
                data.add(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    public WeightFrame(SparkSession spark, String path) {
        List<VertexWeight> data = readData(path);
        dataFrame = spark.createDataFrame(data, VertexWeight.class);
        dataFrame.show();
    }
}
