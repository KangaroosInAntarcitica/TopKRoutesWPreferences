package online.data;

import online.QueryResult;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class FeaturesFrame {
    private int featureNumber = 0;
    private List<VertexFeature> data;

    public int getFeatureNumber() {
        return featureNumber;
    }

    @lombok.Getter
    @lombok.Setter
    public class VertexFeature implements Serializable {
        private int feature;
        private int vertex;
        private double rating;

        public String toString() {
            return String.format("%d: %d (%.2f)", feature, vertex, rating);
        }
    }

    private List<VertexFeature> readData(String cityCode) {
        String path = String.format("data/%s_inverted_kwds.txt", cityCode);
        File file = new File(path);

        List<VertexFeature> data = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(file);
            Scanner scanner = new Scanner(fileReader);
            scanner.useDelimiter(":");

            while (scanner.hasNextInt()) {
                int key = scanner.nextInt();
                scanner.skip(":");
                String[] items = scanner.nextLine().split("\\s");
                for (int i = 0; i < items.length / 2; i++) {
                    VertexFeature item = new VertexFeature();
                    item.feature = key;
                    item.vertex = Integer.parseInt(items[2 * i]);
                    item.rating = Float.parseFloat(items[2 * i + 1]);
                    data.add(item);
                }

                if (key + 1 > featureNumber) featureNumber = key + 1;
            }

            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    private void printData(List<VertexFeature> data) {
        for (VertexFeature item: data) {
            System.out.println(item);
        }
    }


    public FeaturesFrame(String cityCode) {
        // data - sorted by vertexes
        data = readData(cityCode);
        data.sort(Comparator.comparingInt((VertexFeature f) -> f.vertex));
    }

    public void processQuery(QueryResult queryResult) {
        // Function returns a list of vertex-feature-rating for given criteria

        List<VertexFeature> result = new ArrayList<>();
        double[] featureRating = queryResult.query.featurePreference;
        double minRating = queryResult.query.minFeatureValue;

        for (VertexFeature vertexFeature : data) {
            int feature = vertexFeature.feature;
            double rating = vertexFeature.rating;
            if (feature < featureRating.length && rating > featureRating[feature] && rating > minRating) {
                result.add(vertexFeature);
            }
        }

        queryResult.features = result;

        boolean[] vertexIncluded = new boolean[queryResult.vertexNumber];
        for (int i = 0; i < queryResult.vertexNumber; i++) vertexIncluded[i] = false;
        for (VertexFeature vertexFeature: queryResult.features) {
            vertexIncluded[vertexFeature.vertex] = true;
        }

        queryResult.vertexIncluded = vertexIncluded;
    }

    public double[] getVertexFeatures(List<VertexFeature> features, int vertex) {
        // Returns the features of a corresponding vertex
        double[] vertexFeatures = new double[featureNumber];
        for (int i = 0; i < featureNumber; i++) vertexFeatures[i] = 0;

        VertexFeature sampleVertexFeature = new VertexFeature();
        sampleVertexFeature.vertex = vertex;
        int vertexI = Collections.binarySearch(features, sampleVertexFeature, Comparator.comparingInt((VertexFeature f) -> f.vertex));
        int deltaI = 0;
        while (features.get(vertexI + deltaI - 1).vertex == vertex) deltaI--;

        while (true) {
            VertexFeature feature = features.get(vertexI + deltaI);
            if (feature.vertex != vertex) break;

            vertexFeatures[feature.feature] = feature.rating;
            ++deltaI;
        }

        return vertexFeatures;
    }

    @Deprecated
    public int[] getVertexOccurrences(QueryResult queryResult) {
        // Deprecated
        // Takes the vertex-feature-rating list and returns only the numbers of important vertexes

        int[] occurrences = new int[queryResult.vertexNumber];
        for (int i = 0; i < queryResult.vertexNumber; i++) occurrences[i] = 0;

        for (VertexFeature vertexFeature: queryResult.features) {
            occurrences[vertexFeature.vertex]++;
        }

        return occurrences;
    }
}
