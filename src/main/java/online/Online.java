package online;

import online.data.FeaturesFrame;
import online.data.TwoHopFrame;
import online.data.WeightFrame;

import java.util.ArrayList;
import java.util.List;

public class Online {
    private FeaturesFrame featuresFrame;
    private TwoHopFrame twoHopFrame;
    private WeightFrame weightFrame;

    public Online(String cityCode) {

        featuresFrame = new FeaturesFrame(cityCode);
        twoHopFrame = new TwoHopFrame(cityCode);
        weightFrame = new WeightFrame(cityCode);

        System.out.println("Loaded");

        double[] featureRating = {0.5, 0.5};
        double minFeatureValue = 0.5;
    }
    public Online() {
        this("sg");
    }

    public double getPathWeight(int start, int end) {
        return twoHopFrame.getPathWeight(start, end);
    }

    public List<Integer> getPath(int start, int end) {
        return twoHopFrame.getPath(start, end);
    }

    public QueryResult processQuery(Query query) {
        // retrieving subIndex
        QueryResult result = new QueryResult(query, featuresFrame, weightFrame, twoHopFrame);
        result.processQuery();
        return result;
    }

    public List<Integer> getImportantVertexes(int[] vertexOccurrences, int vertexNumber) {
        // Returns a list of important vertexes out of the vertexOccurrences array

        List<Integer> important = new ArrayList<>();
        for (int i = 0; i < vertexNumber; i++) {
            if (vertexOccurrences[i] > 0)
                important.add(i);
        }

        return important;
    }
}
