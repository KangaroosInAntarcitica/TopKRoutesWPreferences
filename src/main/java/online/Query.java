package online;

import online.data.FeaturesFrame;

import java.util.List;

@lombok.Getter
@lombok.Setter
public class Query {
    public interface AggregationFunction {
        double call(int feature, QueryResult.VertexSet vertexSet);
    }

    public int start;
    public int end;
    public double budget;
    public double[] featurePreference;
    public double minFeatureValue;
    public AggregationFunction routeDiversityFunction;

    public class PowerLawFunction implements AggregationFunction {
        private List<Integer> searchVertexes;
        private FeaturesFrame featuresFrame;

        public PowerLawFunction(List<Integer> searchVertexes, FeaturesFrame featuresFrame) {
            this.searchVertexes = searchVertexes;
            this.featuresFrame = featuresFrame;
        }

        public double call(int feature, QueryResult.VertexSet vertexSet) {
            double result = 0;

            // TODO write function body

            return result;
        }
    }
}