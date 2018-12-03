package online;

@lombok.Getter
@lombok.Setter
public class Query {
    public interface AggregationFunction {
        public double call();
    }

    public int start;
    public int end;
    public double budget;
    public double[] featurePreference;
    public double minFeatureValue;
    public AggregationFunction routeDiversityFunction;
}