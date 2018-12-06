package online;

import online.data.FeaturesFrame;

import java.util.List;

@lombok.Getter
@lombok.Setter
public class Query {
    public interface AggregationFunction {
        double call(QueryResult queryResult, int feature, QueryResult.VertexSet vertexSet);
    }

    public Query() {
        minFeatureValue = 0.5;
        routeDiversityFunction = new PowerLawFunction();
    }

    public int start;
    public int end;
    public double budget;
    public double[] featurePreference;
    public double minFeatureValue;
    public AggregationFunction routeDiversityFunction;

    public class PowerLawFunction implements AggregationFunction {
        private double alpha;

        public PowerLawFunction(double alpha) {
            this.alpha = alpha;
        }
        public PowerLawFunction() { this(0.8); }

        public double call(QueryResult queryResult, int feature, QueryResult.VertexSet vertexSet) {
            double result = 0;

            List<FeaturesFrame.VertexFeature> vertexes = queryResult.features.get(feature);
            int rating = 0;
            for (FeaturesFrame.VertexFeature vertexFeature: vertexes) {
                if (vertexSet.contains(vertexFeature.vertex)) {
                    ++rating;

                    result += Math.pow(rating, - alpha) * vertexFeature.rating;
                }
            }

            return result;
        }
    }
}