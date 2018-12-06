package online;

import offline.Path;
import online.data.FeaturesFrame;
import online.data.TwoHopFrame;
import online.data.WeightFrame;

import java.math.BigInteger;
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

    public List<List<FeaturesFrame.VertexFeature>> features;

    public QueryResult(Query query, FeaturesFrame featuresFrame, WeightFrame weightFrame, TwoHopFrame twoHopFrame) {
        this.query = query;
        this.featuresFrame = featuresFrame;
        this.weightFrame = weightFrame;
        this.twoHopFrame = twoHopFrame;

        this.featureNumber = featuresFrame.getFeatureNumber();
        this.vertexNumber = twoHopFrame.getVertexNumber();
    }

    /* API - functions for external user */
    public void processQuery() {
        featuresFrame.processQuery(this);
        twoHopFrame.processQuery(this);
        createSearchVertexes();
        System.out.println("Query search size: " + searchVertexes.size());
        optimalRoutesSearch();

        for (int i = 0; i < optimalPaths.size(); i++) {
            System.out.format("Path %d - gain %.2f \n", i, optimalPaths.get(i).gain);
        }
    }

    public double getVertexWeight(int vertex) {
        return weightFrame.getVertexWeight(vertex);
    }

    public double getPathWeight(int start, int end) {
        return twoHopFrame.getPathWeight(start, end);
    }

    /* Inner functions for calculation */

    private void createSearchVertexes() {
        searchVertexes = new ArrayList<Integer>();

        for (int i = 0; i < vertexIncluded.length; i++) {
            if (vertexIncluded[i]) searchVertexes.add(i);
        }
        searchNumber = searchVertexes.size();
    }
    private double getSearchVertexWeight(int vertex) {
        return weightFrame.getVertexWeight(searchVertexes.get(vertex));
    }

    private double getSearchPathWeight(int start, int end) {
        return twoHopFrame.getPathWeight(searchVertexes.get(start), searchVertexes.get(end));
    }

    public class VertexSet {
        private BigInteger id;

        public VertexSet() {
            this(BigInteger.ZERO);
        }
        public VertexSet(BigInteger id) {
            this.id = id;
        }

        public VertexSet add(int vertexIndex) {
            if (!id.testBit(vertexIndex))
                return new VertexSet(id.flipBit(vertexIndex));
            return new VertexSet(id);
        }
        public VertexSet remove(int vertexIndex) {
            if (id.testBit(vertexIndex))
                return new VertexSet(id.flipBit(vertexIndex));
            return new VertexSet(id);
        }
        public VertexSet flip(int vertexIndex) {
            return new VertexSet(id.flipBit(vertexIndex));
        }
        public boolean contains(int vertexIndex) {
            return id.testBit(vertexIndex);
        }

        public int hashCode() {
            return id.hashCode();
        }

        public boolean equals(Object object) {
            if (!(object instanceof VertexSet)) return false;
            return id.equals(((VertexSet) object).id);
        }

        public boolean isEmpty() {
            return id.equals(BigInteger.ZERO);
        }

        public String toString() {
            return id.toString(2);
        }
    }

    public class VertexPath {
        private VertexSet pathStart;
        private int previousVertex;
        private int lastVertex;
        private double cost;
        private double gain;

        public VertexPath() {
            this(new VertexSet(), 0);
        }
        public VertexPath(VertexSet pathStart, int lastVertex) {
            this.pathStart = pathStart;
            this.lastVertex = lastVertex;
            this.cost = Double.MAX_VALUE;
            this.previousVertex = -1;
        }

        public VertexPath push(int vertex) {
            return new VertexPath(pathStart.add(lastVertex), vertex);
        }
        public int getLastVertex() {
            return lastVertex;
        }

        public int hashCode() {
            return lastVertex * pathStart.hashCode();
        }
        public boolean equals(Object object) {
            if (!(object instanceof VertexPath)) return false;
            VertexPath path = ((VertexPath) object);
            return path.pathStart.equals(pathStart) && path.lastVertex == lastVertex;
        }
    }

    private Map<VertexSet, Double> gain;
    private Map<VertexSet, Map<Integer, Double>> pathsForSet;
    public List<VertexPath> optimalPaths;

    private void optimalRoutesSearch() {
        int k = 10;

        gain = new HashMap<>();
        pathsForSet = new HashMap<>();


        optimalPaths = new LinkedList<>();
        for (int i = 0; i < k; i ++) {
            optimalPaths.add(null);
        }

        pathsForSet.put(new VertexSet(), new HashMap<>());
        PACER(new VertexSet(), searchNumber);
    }

    private static int lastSize = 0;
    private void PACER(VertexSet vertexSet, int maxVertex) {
        int size = vertexSet.toString().length();
        if (size > lastSize) {
            lastSize = size;
            System.out.println("Current search size: " + size);
        }

        // If set is unpromising
        if (!vertexSet.isEmpty() && pathsForSet.get(vertexSet).isEmpty())
            return;

        // Loop through all vertexes not in this set
        for (int newVertex = 0; newVertex < maxVertex; newVertex++) {
            if (!vertexSet.contains(newVertex)) {

                // Add new element to vertex set
                VertexSet currentVertexSet = vertexSet.add(newVertex);
                if (!pathsForSet.containsKey(currentVertexSet))
                    pathsForSet.put(currentVertexSet, new HashMap<>());
                double currentGain = calculateGain(vertexSet);

                // Create all possible path endings - loop through all vertexes is current set
                for (int pathVertex = 0; pathVertex < searchNumber; pathVertex++) {
                    if (currentVertexSet.contains(pathVertex)) {

                        VertexSet pathPartSet = currentVertexSet.remove(pathVertex);
                        VertexPath path = bestPathToVertex(pathPartSet, pathVertex);
                        path.gain = currentGain;

                        if (path.cost <= query.budget) {
                            // Save the path to our map
                            pathsForSet.get(currentVertexSet).put(pathVertex, path.cost);

                            // Update the k-paths
                            int i = optimalPaths.size();
                            while (i > 0 && (optimalPaths.get(i - 1) == null || optimalPaths.get(i - 1).gain < path.gain)) {
                                i--;
                            }
                            if (i < optimalPaths.size()) {
                                // insert this path and remove last
                                optimalPaths.add(i, path);
                                optimalPaths.remove(optimalPaths.size() - 1);
                            }
                        }
                    }
                }

                PACER(currentVertexSet, newVertex);
            }
        }
    }

    private VertexPath bestPathToVertex(VertexSet set, int vertex) {
        int bestLastVertex = 0;
        double bestCost = Double.MAX_VALUE;
        VertexPath bestPath = new VertexPath(set, vertex);
        int size;

        if (set.isEmpty()) {
            bestPath.cost = getPathWeight(query.start, searchVertexes.get(vertex));
            bestPath.cost += getPathWeight(searchVertexes.get(vertex), query.end);
            return bestPath;
        }

        if (!pathsForSet.containsKey(set) || pathsForSet.get(set).isEmpty()) {
            return bestPath;
        }

        for (int lastVertex = 0; lastVertex < searchNumber; lastVertex++) {
            if (set.contains(lastVertex)) {
                double cost = pathsForSet.get(set).get(lastVertex);
                cost -= getPathWeight(searchVertexes.get(lastVertex), query.end);
                cost += getPathWeight(searchVertexes.get(lastVertex), searchVertexes.get(vertex));
                cost += getPathWeight(searchVertexes.get(lastVertex), query.end);

                if (cost < bestCost) {
                    bestLastVertex = lastVertex;
                    bestCost = cost;
                }
            }
        }

        bestPath.previousVertex = bestLastVertex;
        bestPath.cost = bestCost;
        return bestPath;
    }

    public List<Integer> retrievePath(VertexPath path) {
        List<Integer> result = new ArrayList<>();
        result.add(0, query.end);

        VertexSet set = path.pathStart;
        while (true) {
            result.add(0, searchVertexes.get(path.lastVertex));
            int previousVertex = path.previousVertex;
            if (previousVertex == -1) break;

            set = set.remove(previousVertex);
            path = bestPathToVertex(set, previousVertex);
        }

        result.add(0, query.start);
        return result;
    }

    private double calculateGain(VertexSet vertexSet) {
        if (gain.containsKey(vertexSet))
            return gain.get(vertexSet);

        // TODO calculate otherwise
        double currentGain = 0;
        for (int feature = 0; feature < query.featurePreference.length; feature++) {
            currentGain += query.featurePreference[feature] *
                    query.routeDiversityFunction.call(this, feature, vertexSet);
        }

        gain.put(vertexSet, currentGain);
        return currentGain;
    }
}
