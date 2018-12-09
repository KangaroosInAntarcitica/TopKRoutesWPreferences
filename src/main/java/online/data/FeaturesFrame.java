package online.data;

import online.QueryResult;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class FeaturesFrame {
    private int featureNumber = 0;
    private List<List<VertexFeature>> data;

    public int getFeatureNumber() {
        return featureNumber;
    }

    @lombok.Getter
    @lombok.Setter
    public class VertexFeature implements Serializable {
        private int vertex;
        private double rating;

        public String toString() {
            return String.format("%d %.2f", vertex, rating);
        }
    }

    private void readData(String cityCode) {
        String path = String.format("data/%s_inverted_kwds.txt", cityCode);
        File file = new File(path);

        List<List<VertexFeature>> data = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(file);
            Scanner scanner = new Scanner(fileReader);
            scanner.useDelimiter(":");

            while (scanner.hasNextInt()) {
                int key = scanner.nextInt();
                data.add(new ArrayList<>());

                scanner.skip(":");
                String[] items = scanner.nextLine().split("\\s");
                for (int i = 0; i < items.length / 2; i++) {
                    VertexFeature item = new VertexFeature();

                    item.vertex = Integer.parseInt(items[2 * i]);
                    item.rating = Float.parseFloat(items[2 * i + 1]);
                    data.get(key).add(item);
                }

                if (key + 1 > featureNumber) featureNumber = key + 1;
            }

            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.data = data;
    }

    private void printData(List<VertexFeature> data) {
        for (VertexFeature item: data) {
            System.out.println(item);
        }
    }

    public FeaturesFrame(String cityCode) {
        // data - feature by index
        readData(cityCode);
    }

    public void processQuery(QueryResult queryResult) {
        // Function returns a list of vertex-feature-rating for given criteria

        List<List<VertexFeature>> result = new ArrayList<>();
        boolean[] vertexIncluded = new boolean[queryResult.getVertexNumber()];
        for (int i = 0; i < queryResult.getVertexNumber(); i++) vertexIncluded[i] = false;

        double[] featureRating = queryResult.query.getFeaturePreference();
        double minRating = queryResult.query.getMinFeatureValue();

        for (int feature = 0; feature < featureNumber; feature++) {
            List<VertexFeature> vertexes = data.get(feature);
            result.add(new ArrayList<>());

            for (VertexFeature vertex: vertexes) {
                double rating = vertex.rating;
                if (feature < featureRating.length && rating > featureRating[feature] && rating > minRating) {
                    result.get(feature).add(vertex);
                    vertexIncluded[vertex.vertex] = true;
                }
            }
        }

        queryResult.setFeatures(result);
        queryResult.setVertexIncluded(vertexIncluded);
    }
}
