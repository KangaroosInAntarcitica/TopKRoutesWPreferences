package online;

import javafx.util.Pair;
import offline.Path;
import online.data.FeaturesFrame;
import online.data.TwoHopFrame;
import online.data.WeightFrame;

import java.math.BigInteger;
import java.util.*;
import online.data.WeightFrame.VertexWeight;
import online.Query.VisitVertex;
import online.Query.VisitPath;

public class QueryResult {
    public Query query;
    private FeaturesFrame featuresFrame;
    private WeightFrame weightFrame;
    private TwoHopFrame twoHopFrame;

    @lombok.Getter
    private int featureNumber;
    @lombok.Getter
    private int vertexNumber;

    @lombok.Getter @lombok.Setter
    private boolean[] vertexIncluded;
    // The vertexes the search will be performed on
    @lombok.Getter
    private List<Integer> searchVertexes;
    @lombok.Getter
    private int searchNumber;

    // subIndexes
    @lombok.Getter @lombok.Setter
    private List<List<FeaturesFrame.VertexFeature>> features;
    @lombok.Getter @lombok.Setter
    private List<List<Path>> hops;
    // minimal total vertex weight = minimal path weight + vertex weight
    @lombok.Getter @lombok.Setter
    private double[] minVertexTotalWeightByIndex;
    @lombok.Getter @lombok.Setter
    private List<VertexWeight> minVertexTotalWeight;

    public QueryResult(Query query, FeaturesFrame featuresFrame, WeightFrame weightFrame, TwoHopFrame twoHopFrame) {
        this.query = query;
        this.featuresFrame = featuresFrame;
        this.weightFrame = weightFrame;
        this.twoHopFrame = twoHopFrame;

        this.featureNumber = featuresFrame.getFeatureNumber();
        this.vertexNumber = twoHopFrame.getVertexNumber();
    }

    /* Function provided for external use */
    public void processQuery() {
        featuresFrame.processQuery(this);
        twoHopFrame.processQuery(this);
        createSearchVertexes();
        twoHopFrame.getDataSubIndex(this);
        if (query.isDebug())
            System.out.println("Query search size: " + searchVertexes.size());

        optimalRoutesSearch();

        if (query.isDebug()) {
            for (int i = 0; i < optimalPaths.size(); i++) {
                if (optimalPaths.get(i) == null) break;
                VertexPath path = optimalPaths.get(i);
                System.out.format("Path %d - gain %.2f - cost %.2f\n", i, path.gain, path.cost);
            }
        }
    }

    public VisitPath getOptimalPath(int index) {
        if (index > optimalPaths.size() || optimalPaths.get(index) == null)
            return null;

        VisitPath result = query.new VisitPath();
        result.setGain(optimalPaths.get(index).gain);
        result.setCost(optimalPaths.get(index).cost);
        List<Integer> optimalPath = retrievePath(optimalPaths.get(index));

        for (int i = 1; i < optimalPath.size(); i++) {
            List<Integer> path = twoHopFrame.getPath(optimalPath.get(i-1), optimalPath.get(i));
            for (int vertex: path) {
                if (result.getVertexes().size() != 0 &&
                        result.getVertexes().get(result.getVertexes().size() - 1) == vertex) continue;
                result.getVertexes().add(vertex);
                if (vertex == optimalPath.get(i-1) || vertex == optimalPath.get(i))
                    result.getVisit().add(true);
                else
                    result.getVisit().add(false);
            }
        }

        return result;
    }

    public List<VisitPath> getOptimalPaths() {
        List<VisitPath> allPaths = new ArrayList<>();
        VisitPath path;
        int i = 0;
        while ((path = getOptimalPath(i)) != null) {
            allPaths.add(path);
            i++;
        }
        return allPaths;
    }

    public int getOptimalPathNumber() {
        int i = 0;
        while(i < optimalPaths.size() && optimalPaths.get(i) != null) i += 1;
        return i;
    }

    public double getVertexWeight(int vertex) {
        return weightFrame.getVertexWeight(vertex);
    }

    public double getPathWeight(int start, int end) {
        return twoHopFrame.getPathWeight(hops, start, end);
    }

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
    private List<VertexPath> optimalPaths;
    private VertexPath lastOptimalPath;

    private void optimalRoutesSearch() {
        pathsForSet = new HashMap<>();

        optimalPaths = new LinkedList<>();
        for (int i = 0; i < query.getPathNumber(); i++) {
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

                        if (path.cost <= query.getBudget()) {
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
            bestPath.cost = getPathWeight(query.getStart(), searchVertexes.get(vertex));
            bestPath.cost += getPathWeight(searchVertexes.get(vertex), query.getEnd());
            bestPath.cost += getVertexWeight(searchVertexes.get(vertex));
            return bestPath;
        }

        if (!pathsForSet.containsKey(set) || pathsForSet.get(set).isEmpty()) {
            return bestPath;
        }

        for (int lastVertex = 0; lastVertex < searchNumber; lastVertex++) {
            if (set.contains(lastVertex) && pathsForSet.get(set).containsKey(lastVertex)) {
                double cost = pathsForSet.get(set).get(lastVertex);
                cost -= getPathWeight(searchVertexes.get(lastVertex), query.getEnd());
                cost += getPathWeight(searchVertexes.get(lastVertex), searchVertexes.get(vertex));
                cost += getPathWeight(searchVertexes.get(lastVertex), query.getEnd());
                cost += getVertexWeight(searchVertexes.get(vertex));

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
        // Returns the vertxes, that are included in the optimal path

        List<Integer> result = new ArrayList<>();
        result.add(0, query.getEnd());

        VertexSet set = path.pathStart;
        while (true) {
            result.add(0, searchVertexes.get(path.lastVertex));
            int previousVertex = path.previousVertex;
            if (previousVertex == -1) break;

            set = set.remove(previousVertex);
            path = bestPathToVertex(set, previousVertex);
        }

        result.add(0, query.getStart());
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

        double cost = path.cost - getPathWeight(searchVertexes.get(path.lastVertex), query.getEnd());
        cost += (minVertexTotalWeightByIndex[query.getEnd()]);
                // + minVertexTotalWeightByIndex[searchVertexes.get(path.lastVertex)]);

        double initialSetGain = calculateGain(vertexSet);
        List<VertexGain> vertexGainList = new ArrayList<>();
        for (int i = 0; i < searchNumber; i++) {
            if (!vertexSet.contains(i) &&
                    searchVertexes.get(i) != query.getStart() && searchVertexes.get(i) != query.getEnd()) {
                double gain = calculateGain(vertexSet.flip(i)) - initialSetGain;
                vertexGainList.add(new VertexGain(i, gain,
                        gain / minVertexTotalWeightByIndex[searchVertexes.get(i)]));
            }
        }
        vertexGainList.sort(Comparator.comparingDouble((VertexGain vg) -> vg.costGain));
        Collections.reverse(vertexGainList);

        double upperBound = 0;
        double deltaCost = 1;
        for (int i = 0; i < vertexGainList.size(); ++i) {
            VertexGain vertexGain = vertexGainList.get(i);

            deltaCost = minVertexTotalWeightByIndex[searchVertexes.get(vertexGain.vertex)];
            if (cost + deltaCost > query.getBudget()) {
                upperBound += vertexGainList.get(i).gain * deltaCost /
                        minVertexTotalWeightByIndex[searchVertexes.get(i)];
                break;
            }
            cost += deltaCost;

            upperBound += vertexGainList.get(i).gain;
        }

        return upperBound;
    }

    private double calculateGain(VertexSet vertexSet) {
        double currentGain = 0;
        for (int feature = 0; feature < query.getFeaturePreference().length; feature++) {
            if (query.getFeaturePreference()[feature] == 0) continue;
            currentGain += query.getFeaturePreference()[feature] *
                    query.getRouteDiversityFunction().call(this, feature, vertexSet);
        }

        return currentGain;
    }
}
