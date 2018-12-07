package online;

import javafx.util.Pair;
import offline.Path;
import online.data.FeaturesFrame;
import online.data.TwoHopFrame;
import online.data.WeightFrame;

import java.math.BigInteger;
import java.util.*;
import online.data.WeightFrame.VertexWeight;

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

    // subIndexes
    public List<List<FeaturesFrame.VertexFeature>> features;
    public List<List<Path>> hops;
    // minimal total vertex weight = minimal path weight + vertex weight
    public double[] minVertexTotalWeightByIndex;
    public List<VertexWeight> minVertexTotalWeight;

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
        twoHopFrame.getDataSubIndex(this);
        System.out.println("Query search size: " + searchVertexes.size());

        optimalRoutesSearch();

        for (int i = 0; i < optimalPaths.size(); i++) {
            if (optimalPaths.get(i) == null) break;
            VertexPath path = optimalPaths.get(i);
            System.out.format("Path %d - gain %.2f - cost %.2f\n", i, path.gain, path.cost);
        }
    }

    public double getVertexWeight(int vertex) {
        return weightFrame.getVertexWeight(vertex);
    }

    public double getPathWeight(int start, int end) {
        return twoHopFrame.getPathWeight(hops, start, end);
    }

    /* Inner functions for calculation */

    private void createSearchVertexes() {
        searchVertexes = new ArrayList<Integer>();

        for (int i = 0; i < vertexIncluded.length; i++) {
            if (vertexIncluded[i]){
                searchVertexes.add(i);
            }
        }
        searchNumber = searchVertexes.size();
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
        public VertexSet flipAll() {
            BigInteger mask = BigInteger.ONE.shiftLeft(searchNumber).subtract(BigInteger.ONE);
            return new VertexSet(id.xor(mask));
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

    private Map<VertexSet, Map<Integer, Double>> pathsForSet;
    public List<VertexPath> optimalPaths;
    public VertexPath lastOptimalPath;

    private void optimalRoutesSearch() {
        int k = 10;

        pathsForSet = new HashMap<>();

        optimalPaths = new LinkedList<>();
        for (int i = 0; i < k; i++) {
            optimalPaths.add(null);
        }
        lastOptimalPath = null;

        pathsForSet.put(new VertexSet(), new HashMap<>());
        PACER(new VertexSet(), searchNumber);
    }

    private static int lastSize = 0;
    private void PACER(VertexSet vertexSet, int maxVertex) {
        // If set is unpromising
        if (!vertexSet.isEmpty() && (!pathsForSet.containsKey(vertexSet) || pathsForSet.get(vertexSet).isEmpty()))
            return;

        // Loop through all vertexes not in this set
        for (int newVertex = 0; newVertex < maxVertex; newVertex++) {
            if (!vertexSet.contains(newVertex)) {
                // Add new element to vertex set
                VertexSet currentVertexSet = vertexSet.add(newVertex);
                if (!pathsForSet.containsKey(currentVertexSet))
                    pathsForSet.put(currentVertexSet, new HashMap<>());
                double currentGain = calculateGain(vertexSet);

                VertexPath bestPath = null;

                // Create all possible path endings - loop through all vertexes is current set
                for (int pathVertex = 0; pathVertex < searchNumber; pathVertex++) {
                    if (currentVertexSet.contains(pathVertex)) {

                        VertexSet pathPartSet = currentVertexSet.remove(pathVertex);
                        VertexPath path = bestPathToVertex(pathPartSet, pathVertex);
                        path.gain = currentGain;

                        if (path.cost <= query.budget) {
                            if (lastOptimalPath == null || calculateGainUP(path) + path.gain > lastOptimalPath.gain) {
                                // Save the path to our map
                                pathsForSet.get(currentVertexSet).put(pathVertex, path.cost);

                                if (bestPath == null || bestPath.gain < path.gain) {
                                    bestPath = path;
                                }
                            }
                        }
                    }
                }

                if (pathsForSet.get(currentVertexSet).isEmpty())
                    pathsForSet.remove(currentVertexSet);

                if (bestPath != null) {
                    // Update the k-paths
                    int i = optimalPaths.size();
                    while (i > 0 && (optimalPaths.get(i - 1) == null || optimalPaths.get(i - 1).gain < bestPath.gain)) {
                        i--;
                    }
                    if (i < optimalPaths.size()) {
                        // insert this path and remove last
                        optimalPaths.add(i, bestPath);
                        optimalPaths.remove(optimalPaths.size() - 1);

                        lastOptimalPath = optimalPaths.get(optimalPaths.size() - 1);
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

        if (set.isEmpty()) {
            bestPath.cost = getPathWeight(query.start, searchVertexes.get(vertex));
            bestPath.cost += getPathWeight(searchVertexes.get(vertex), query.end);
            return bestPath;
        }

        if (!pathsForSet.containsKey(set) || pathsForSet.get(set).isEmpty()) {
            return bestPath;
        }

        for (int lastVertex = 0; lastVertex < searchNumber; lastVertex++) {
            if (set.contains(lastVertex) && pathsForSet.get(set).containsKey(lastVertex)) {
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

    private class VertexGain {
        public VertexGain(int vertex, double gain, double costGain) {
            this.vertex = vertex;
            this.gain = gain;
            this.costGain = costGain;
        }

        private int vertex;
        private double gain;
        private double costGain;
    }

    private double calculateGainUP(VertexPath path) {
        VertexSet vertexSet = path.pathStart.add(path.lastVertex);

        double cost = path.cost - getPathWeight(searchVertexes.get(path.lastVertex), query.end);
        cost += (minVertexTotalWeightByIndex[query.end]);
                // + minVertexTotalWeightByIndex[searchVertexes.get(path.lastVertex)]);

        int maxCount = 0;
        int maxCountIndex = 0;
        double deltaCost = 1;
        for (int i = 0; i < minVertexTotalWeight.size(); ++i) {
            VertexWeight vertexWeight = minVertexTotalWeight.get(i);
            int vertex = vertexWeight.getVertex();

            if (!vertexSet.contains(vertex) &&
                    searchVertexes.get(vertex) != query.start && searchVertexes.get(vertex) != query.end) {
                deltaCost = vertexWeight.getWeight();
                if (cost + deltaCost > query.budget) {
                    maxCountIndex = i;
                    break;
                }
                cost += deltaCost;

                maxCount += 1;
            }
        }


        double initialSetGain = calculateGain(vertexSet);
        List<VertexGain> vertexGain = new ArrayList<>();
        for (int i = 0; i < searchNumber; i++) {
            if (!vertexSet.contains(i) &&
                    searchVertexes.get(i) != query.start && searchVertexes.get(i) != query.end) {
                double gain = calculateGain(vertexSet.flip(i)) - initialSetGain;
                vertexGain.add(new VertexGain(i, gain,
                        gain / minVertexTotalWeightByIndex[searchVertexes.get(i)]));
            }
        }
        vertexGain.sort(Comparator.comparingDouble((VertexGain vg) -> vg.costGain));
        Collections.reverse(vertexGain);

        double upperBound = 0;
        for (int i = 0; i < maxCount - 1; i++) {
            upperBound += vertexGain.get(i).gain;
        }
        if (maxCount < vertexGain.size()) {
            upperBound += vertexGain.get(maxCount).gain * deltaCost /
                    minVertexTotalWeightByIndex[searchVertexes.get(maxCountIndex)];
        }

        return upperBound;
    }

    private double calculateGain(VertexSet vertexSet) {
        double currentGain = 0;
        for (int feature = 0; feature < query.featurePreference.length; feature++) {
            if (query.featurePreference[feature] == 0) continue;
            currentGain += query.featurePreference[feature] *
                    query.routeDiversityFunction.call(this, feature, vertexSet);
        }

        return currentGain;
    }
}
