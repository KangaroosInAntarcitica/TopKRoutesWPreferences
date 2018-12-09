package online;

import javafx.scene.paint.Color;
import offline.EdgeFrame;
import online.Query.VisitPath;
import online.visualisation.Graph;
import online.visualisation.VisualisationApp;

import java.util.List;


public class Main {
    public Main(VisualisationApp app) {
        this.app = app;
    }

    private VisualisationApp app;

    // Run Visualisation App instead
    public void main() {
        // generate2Hop();

        Online online = new Online();

        // drawPathResult(online);
        drawQueryResult(online);
        // drawTimeGraph(online);
    }

    public void drawPathResult(Online online) {
        List<Integer> pathBest = online.getPath(75, 64);
        System.out.println(pathBest);
        app.drawPath(pathBest, Color.color(1,0,1));
    }

    public void drawQueryResult(Online online) {
        long startTime = System.currentTimeMillis();

        Query query = new Query();
        query.setStart(1000);
        query.setEnd(1000);
        query.setBudget(480);
        query.setFeaturePreference(new double[]{0.4, 0.3, 0.3});
        query.setMinFeatureValue(2.5);

        QueryResult result = online.processQuery(query);
        VisitPath path = result.getOptimalPath(0);
        System.out.println(path);
        app.drawVisitPath(path, Color.color(0, 0, 0));

        System.out.format("Run time: %.5fs\n", (double) (System.currentTimeMillis() - startTime) / 1000);
    }

    public void drawTimeGraph(Online online) {
        int minBudget = 4;
        int maxBudget = 10;

        Graph graph = new Graph("PACER+2", 400, 400, 2);

        graph.drawGraph(0, "Run time","budget (hour)", "run time (ms) (log 10)");
        graph.drawGraph(1, "Result", "budget (hour)", "gain");

        double[] budget = new double[maxBudget - minBudget + 1];
        for (int i = minBudget; i <= maxBudget; i++) budget[i - minBudget] = i;
        double[] time = new double[budget.length];
        double[] gain = new double[budget.length];

        Query query = new Query();
        query.setStart(0);
        query.setEnd(0);
        query.setFeaturePreference(new double[]{0.4, 0.3, 0.3});
        query.setMinFeatureValue(2.5);

        // Calculate
        for (int i = 0; i < budget.length; i++) {
            System.out.format("Current budget: %.0f\n", budget[i]);
            long startTime = System.currentTimeMillis();

            query.setBudget(budget[i] * 60);
            QueryResult result = online.processQuery(query);

            long duration = System.currentTimeMillis() - startTime;
            time[i] = duration;
            gain[i] = result.getOptimalPath(0).getGain();
        }

        System.out.println("Finished");

        graph.drawGraphData(1, budget, gain);
        graph.applyY = Math::log10;
        graph.drawGraphData(0, budget, time);

        graph.save("result.png");
    }


    public void generate2Hop() {
        // 2 generate the table - needed only one time
        EdgeFrame edgeFrame = new EdgeFrame();
        edgeFrame.processEdges("sg");
    }
}
