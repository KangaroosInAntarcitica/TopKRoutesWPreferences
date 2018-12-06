package visualisation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import offline.EdgeFrame;
import offline.Path;
import online.Online;
import online.Query;
import online.QueryResult;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class VisualisationApp extends Application {
    private int width = 1000;
    private int height = 1000;
    private GraphicsContext context;

    @lombok.Getter
    @lombok.Setter
    public class Coordinate {
        private int vertex;
        private double x;
        private double y;
    }

    private List<Coordinate> coordinates;
    private DrawingParameters parameters;
    private List<Path> edges;

    private class DrawingParameters {
        double border;
        double minX, maxX, minY, maxY;
        double scaleX, scaleY;
        double borderX, borderY;

        Coordinate change(Coordinate coordinate) {
            Coordinate result = new Coordinate();
            result.x = borderX + (coordinate.x - minX) * scaleX;
            result.y = borderY + (coordinate.y - minY) * scaleY;
            return result;
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane parent = new BorderPane();

        Canvas canvas = new Canvas(width, height);
        parent.setCenter(canvas);
        context = canvas.getGraphicsContext2D();

        primaryStage.setScene(new Scene(parent, width, height));
        primaryStage.show();

        Online online = new Online("sg");

        readCoordinates("sg");
        EdgeFrame edgeFrame = new EdgeFrame();
        edgeFrame.readData("sg");
        edges = edgeFrame.getData();

        parameters = new DrawingParameters();
        parameters.border = 50;
        findParameters();
        drawEdges();
        drawVertexes();

//        List<Integer> path = online.getPath(1202, 1201);
//        System.out.println(path);
//        drawPath(path);

        Query query = new Query();
        query.start = 0;
        query.end = 1;
        query.budget = 110;
        query.featurePreference = new double[]{0.5, 0.5};
        query.minFeatureValue = 0.5;

        QueryResult result = online.processQuery(query);
        List<Integer> path = result.retrievePath(result.optimalPaths.get(0));
        System.out.println(path);
        drawPath(path);
        List<Integer> path2 = result.retrievePath(result.optimalPaths.get(0));
        System.out.println(path2);
        drawPath(path2);
    }

    public void readCoordinates(String cityCode) {
        File file = new File(String.format("data/%s_coors.txt", cityCode));
        coordinates = new ArrayList<>();

        try (FileReader fileReader = new FileReader(file)) {
            Scanner scanner = new Scanner(fileReader);

            while (scanner.hasNextInt()) {
                String[] values = scanner.nextLine().split("[\\s,]+");

                Coordinate coordinate = new Coordinate();
                coordinate.vertex = Integer.parseInt(values[0]);
                coordinate.x = Double.parseDouble(values[1]);
                coordinate.y = Double.parseDouble(values[2]);
                coordinates.add(coordinate);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void findParameters() {
        double border = parameters.border;

        parameters.minX = Collections.min(coordinates, Comparator.comparingDouble((Coordinate c) -> c.x)).x;
        parameters.maxX = Collections.max(coordinates, Comparator.comparingDouble((Coordinate c) -> c.x)).x;
        parameters.minY = Collections.min(coordinates, Comparator.comparingDouble((Coordinate c) -> c.y)).y;
        parameters.maxY = Collections.max(coordinates, Comparator.comparingDouble((Coordinate c) -> c.y)).y;

        double distanceX = parameters.maxX - parameters.minX;
        double distanceY = parameters.maxY - parameters.minY;
        parameters.borderX = parameters.borderY = border;

        if (distanceX > distanceY) {
            parameters.scaleX = (width - border * 2) / distanceX;
            parameters.scaleY = (height - border * 2) / distanceX;
            parameters.borderY = (distanceX - distanceY) / 2 + border;
        } else {
            parameters.scaleX = (width - border * 2) / distanceY;
            parameters.scaleY = (height - border * 2) / distanceY;
            parameters.borderX = (distanceY - distanceX) / 2 + border;
        }
    }

    public void drawVertexes() {
        // Circles
        context.setFill(Color.color(1, 0, 0, 0.5));
        for (Coordinate coordinate: coordinates) {
            double radius = 2;
            Coordinate changed = parameters.change(coordinate);
            context.fillOval(changed.x - radius, changed.y - radius, 2 * radius, 2 * radius);
        }
    }

    private void drawEdge(int fromVertex, int toVertex) {
        Coordinate coordinateFrom = parameters.change(coordinates.get(fromVertex));
        Coordinate coordinateTo = parameters.change(coordinates.get(toVertex));

        context.strokeLine(coordinateFrom.x, coordinateFrom.y, coordinateTo.x, coordinateTo.y);
    }

    public void drawEdges() {
        // Edges
        context.setStroke(Color.color(0, 0, 1, 0.03));
        for (Path edge: edges) {
            drawEdge(edge.vertex, edge.vertexTo);
        }
    }

    public void drawPath(List<Integer> vertexes) {
        context.setStroke(Color.color(0, 0, 0, 1));
        for (int i = 0; i < vertexes.size() - 1; i++) {
            drawEdge(vertexes.get(i), vertexes.get(i + 1));
        }
    }

}
