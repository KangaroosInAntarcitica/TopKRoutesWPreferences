package online;

import offline.Path;
import online.data.FeaturesFrame;
import online.data.TwoHopFrame;
import online.data.WeightFrame;

import java.util.*;

public class QueryResult {
    public Query query;
    private FeaturesFrame featuresFrame;
    private WeightFrame weightFrame;
    private TwoHopFrame twoHopFrame;

    public int featureNumber;
    public int vertexNumber;

    public boolean[] vertexIncluded;
    // The vertexes the search will be performed on
    public List<Integer> searchVertexes;
    public int searchNumber;

    public List<FeaturesFrame.VertexFeature> features;

    public QueryResult(Query query, FeaturesFrame featuresFrame, WeightFrame weightFrame, TwoHopFrame twoHopFrame) {
        this.query = query;
        this.featuresFrame = featuresFrame;
        this.weightFrame = weightFrame;
        this.twoHopFrame = twoHopFrame;

        this.featureNumber = featuresFrame.getFeatureNumber();
        this.vertexNumber = twoHopFrame.getVertexNumber();
    }

    public void processQuery() {
        featuresFrame.processQuery(this);
        twoHopFrame.processQuery(this);
        getSearchVertexes();
    }

    public double[] getVertexFeatures(int vertex) {
        return featuresFrame.getVertexFeatures(features, vertex);
    }

    public double getVertexWeight(int vertex) {
        return weightFrame.getVertexWeight(vertex);
    }

    public double getPathWeight(int start, int end) {
        return twoHopFrame.getPathWeight(start, end);
    }

    public void getSearchVertexes() {
        searchVertexes = new ArrayList<Integer>();

        for (int i = 0; i < vertexIncluded.length; i++) {
            if (vertexIncluded[i]) searchVertexes.add(i);
        }
        searchNumber = searchVertexes.size();
    }

    public class VertexSet {
        private int id;

        public VertexSet() { this(0); }
        public VertexSet(int id) {
            this.id = id;
        }

        public VertexSet add(int vertexIndex) {
            return new VertexSet(id + 1 << vertexIndex);
        }
        public VertexSet remove(int vertexIndex) {
            return new VertexSet(id & ~(1 << vertexIndex));
        }
        public boolean contains(int vertexIndex) {
            return (id >>> vertexIndex) % 2 == 1;
        }
    }

    public class VertexPath {
        private int size;
        private long id;

        public VertexPath() {
            this(0, 0);
        }
        public VertexPath(long id, int size) {
            this.id = id;
            this.size = size;
        }

        public VertexPath push(int vertex) {
            VertexPath result = new VertexPath(id + vertex * (long) Math.pow(searchNumber, size), size + 1);
            return result;
        }
        public VertexPath add(int position, int vertex) {
            long split = (long) Math.pow(searchNumber, position);
            long lowerPart = id % split;
            long upperPart = id / split * split * searchNumber;

            VertexPath result = new VertexPath(id + lowerPart + vertex * split + upperPart, size + 1);
            return result;
        }
        public int getLast() {
            return (int) (id / (long) Math.pow(searchNumber, size - 1));
        }
        public VertexPath pop() {
            VertexPath result = new VertexPath(id % (id / (long) Math.pow(searchNumber, size - 1)), size - 1);
            return result;
        }

        public int hashCode() {
            return (int) id;
        }
        public boolean equals(Object object) {
            if (!(object instanceof VertexPath)) return false;
            return ((VertexPath) object).id == id;
        }
    }

    private Map<VertexSet, Double> gain;
    private Map<VertexSet, VertexPath> optimalPathForSet;
    private Map<VertexPath, Double> pathWeight;

    private List<VertexPath> optimalPaths;
    private List<Double> optimalGain;

    public void optimalRoutesSearch() {
        int k = 10;

        gain = new HashMap<>();
        optimalPathForSet = new HashMap<>();
        pathWeight = new HashMap<>();

        optimalPaths = new LinkedList<>();
        optimalGain = new LinkedList<>();
        for (int i = 0; i < k; i ++) {
            optimalGain.add(0.0);
            optimalPaths.add(null);
        }

        PACER(new VertexSet());
    }

    public void PACER(VertexSet vertexSet) {
        for (int i = 0; i < searchNumber; i++) {

            VertexSet currentVertexSet = vertexSet.add(i);
            double currentGain = calculateGain(vertexSet);

            for (int pathVertex = 0; pathVertex < searchNumber; pathVertex++) {
                if (currentVertexSet.contains(pathVertex)) {
                    VertexSet pathPartSet = currentVertexSet.remove(pathVertex);

                    // TODO iterate through all and recalculate
                    VertexPath pathPart = optimalPathForSet.get(pathPartSet);
                    VertexPath fullPath = pathPart.push(pathVertex);
                    double weight = calculatePathWeight(fullPath);

                    if (weight <= query.budget) {
                        // Update the k-paths
                        // TODO reverse search
                        i = 0;
                        while (true) {
                            if (optimalGain.get(i) >= currentGain) {
                                i++;
                            } else {
                                optimalGain.add(i, currentGain);
                                optimalPaths.add(i, fullPath);
                            }
                            if (i >= optimalGain.size()) break;
                        }
                    }
                }
            }

            PACER(currentVertexSet);
        }
    }

    public double calculateGain(VertexSet vertexSet) {
        if (gain.containsKey(vertexSet))
            return gain.get(vertexSet);

        // TODO calculate otherwise
        // gain.put(vertexSet, currentGain);
        return 0;
    }

    public double calculatePathWeight(VertexPath path) {
        if (pathWeight.containsKey(path)) {
            return pathWeight.get(path);
        }

        if (path.size > 1) {
            int endVertex = path.getLast();
            VertexPath pathStart = path.pop();
            int lastVertex = path.getLast();

            if (pathWeight.containsKey(pathStart)) {
                double weight = pathWeight.get(pathStart);

                weight -= getPathWeight(lastVertex, query.end);
                weight += getPathWeight(lastVertex, endVertex);
                weight += getVertexWeight(endVertex);
                weight += getPathWeight(endVertex, query.end);
                pathWeight.put(path, weight);
                return weight;
            } else {
                return Double.MAX_VALUE;
            }
        }
        else {
            int vertex = path.getLast();
            double weight = getPathWeight(query.start, vertex);
            weight += getVertexWeight(vertex);
            weight += getPathWeight(vertex, query.end);
            pathWeight.put(path, weight);
            return weight;
        }
    }
}
