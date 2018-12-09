package online;

import online.data.FeaturesFrame;

import java.util.ArrayList;
import java.util.List;

@lombok.Getter
@lombok.Setter
public class Query {
    public interface AggregationFunction {
        double call(QueryResult queryResult, int feature, QueryResult.VertexSet vertexSet);
    }

    @lombok.Getter
    @lombok.Setter
    public static class VisitVertex {
        private int vertex;
        private boolean visited;

        public VisitVertex(int vertex, boolean visited) {
            this.vertex = vertex;
            this.visited = visited;
        }

        public String toString() {
            return String.format("%s%d%s", visited ? "{" : "", vertex, visited ? "}" : "");
        }
    }

    @lombok.Getter
    @lombok.Setter
    public class VisitPath {
        public VisitPath() {
            vertexes = new ArrayList<>();
            visit = new ArrayList<>();
        }

        private List<Integer> vertexes;
        private List<Boolean> visit;
        private double cost;
        private double gain;
    }

    public Query() {
        debug = false;
        pathNumber = 10;
        minFeatureValue = 0.5;
        routeDiversityFunction = new PowerLawFunction();
    }

    private boolean debug;
    private int pathNumber;
    private int start;
    private int end;
    private double budget;
    private double[] featurePreference;
    private double minFeatureValue;
    private AggregationFunction routeDiversityFunction;

    public class PowerLawFunction implements AggregationFunction {
        private double alpha;

        public PowerLawFunction(double alpha) {
            this.alpha = alpha;
        }
        public PowerLawFunction() { this(0.8); }

        public double call(QueryResult queryResult, int feature, QueryResult.VertexSet vertexSet) {
            double result = 0;

            List<FeaturesFrame.VertexFeature> vertexes = queryResult.getFeatures().get(feature);
            int rating = 0;
            for (FeaturesFrame.VertexFeature vertexFeature: vertexes) {
                if (vertexSet.contains(vertexFeature.getVertex())) {
                    ++rating;

                    result += Math.pow(rating, - alpha) * vertexFeature.getRating();
                }
            }

            return result;
        }
    }
}