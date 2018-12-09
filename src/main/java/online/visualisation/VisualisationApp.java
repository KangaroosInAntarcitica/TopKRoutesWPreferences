package online.visualisation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import offline.EdgeFrame;
import offline.Path;
import online.Query.VisitPath;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

import online.Main;

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

        readCoordinates("sg");
        EdgeFrame edgeFrame = new EdgeFrame();
        edgeFrame.readData("sg");
        edges = edgeFrame.getData();

        parameters = new DrawingParameters();
        parameters.border = 50;
        findParameters();
        drawEdges();
        drawVertexes();

        new Main(this).main();
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

    public void drawCircle(Coordinate coordinate) {
        double radius = 2;
        Coordinate changed = parameters.change(coordinate);
        context.fillOval(changed.x - radius, changed.y - radius, 2 * radius, 2 * radius);
    }

    public void drawVertexes() {
        // Circles
        context.setFill(Color.color(1, 0, 0, 0.5));
        for (Coordinate coordinate: coordinates) {
            drawCircle(coordinate);
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
            drawEdge(edge.getVertex(), edge.getVertexTo());
        }
    }

    public void drawVisitPath(VisitPath path, Paint paint) {
        context.setStroke(paint);
        List<Integer> vertexes = path.getVertexes();
        List<Boolean> visit = path.getVisit();

        for (int i = 0; i < vertexes.size() - 1; i++) {
            drawEdge(vertexes.get(i), vertexes.get(i + 1));
        }
        for (int i = 0; i < vertexes.size(); i++) {
            if (visit.get(i)) {
                context.setFill(Color.color(0, 0, 0));
            } else {
                context.setFill(Color.color(1,0,1));
            }
            drawCircle(coordinates.get(vertexes.get(i)));
        }
    }

    public void drawPath(List<Integer> vertexes, Paint paint) {
        context.setStroke(paint);

        for (int i = 0; i < vertexes.size() - 1; i++) {
            drawEdge(vertexes.get(i), vertexes.get(i + 1));
        }
    }

}
