package online;

import org.apache.spark.sql.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FeaturesFrame {
    private Dataset<Row> dataFrame;

    @lombok.Getter
    @lombok.Setter
    public class KeywordConnection implements Serializable {
        private int feature;
        private int vertex;
        private double rating;

        public String toString() {
            return String.format("%d: %d (%.2f)", feature, vertex, rating);
        }
    }

    private List<KeywordConnection> readData(String path) {
        File file = new File(path);

        List<KeywordConnection> data = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(file);
            Scanner scanner = new Scanner(fileReader);
            scanner.useDelimiter(":");

            while (scanner.hasNextInt()) {
                int key = scanner.nextInt();
                scanner.skip(":");
                String[] items = scanner.nextLine().split("\\s");
                for (int i = 0; i < items.length / 2; i++) {
                    KeywordConnection item = new KeywordConnection();
                    item.feature = key;
                    item.vertex = Integer.parseInt(items[2 * i]);
                    item.rating = Float.parseFloat(items[2 * i + 1]);
                    data.add(item);
                }
            }

            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    private void printData(List<KeywordConnection> data) {
        for (KeywordConnection item: data) {
            System.out.println(item);
        }
    }


    public FeaturesFrame(SparkSession spark, String path) {
        List<KeywordConnection> data = readData(path);
        dataFrame = spark.createDataFrame(data, KeywordConnection.class);
    }

    public Dataset<Row> retrieveForFeatures(double[] featureRating, double minRating) {
        Column col = null;
        for (int i = 0; i < featureRating.length; i++) {
            if (featureRating[i] > 0) {
                Column current = functions.col("feature").equalTo(i)
                        .and(functions.col("rating").$greater(minRating));
                if (col == null) col = current;
                else col = col.or(current);
            }
        }

        return dataFrame.filter(col);
    }
}
