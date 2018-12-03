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
    private List<Path> edges;

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
        edgeFrame.readData(String.format("data/%s_undir_edges.txt", "sg"));
        edges = edgeFrame.getData();
        draw();
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

    public void draw() {
        double borders = 50;

        double minX = Collections.min(coordinates, Comparator.comparingDouble((Coordinate c) -> c.x)).x;
        double maxX = Collections.max(coordinates, Comparator.comparingDouble((Coordinate c) -> c.x)).x;
        double minY = Collections.min(coordinates, Comparator.comparingDouble((Coordinate c) -> c.y)).y;
        double maxY = Collections.max(coordinates, Comparator.comparingDouble((Coordinate c) -> c.y)).y;

        double distanceX = maxX - minX;
        double distanceY = maxY - minY;
        double scaleX, scaleY;
        double borderX = borders, borderY = borders;

        if (distanceX > distanceY) {
            scaleX = (width - borders * 2) / distanceX;
            scaleY = (height - borders * 2) / distanceX;
            borderY = (distanceX - distanceY) / 2 + borders;
        } else {
            scaleX = (width - borders * 2) / distanceY;
            scaleY = (height - borders * 2) / distanceY;
            borderX = (distanceY - distanceX) / 2 + borders;
        }

        // Edges
        context.setStroke(Color.color(0, 0, 1, 0.03));
        for (Path edge: edges) {
            Coordinate coordinateFrom = coordinates.get(edge.vertex);
            Coordinate coordinateTo = coordinates.get(edge.vertexTo);

            double xFrom = (coordinateFrom.x - minX) * scaleX + borderX;
            double yFrom = (coordinateFrom.y - minY) * scaleY + borderY;
            double xTo = (coordinateTo.x - minX) * scaleX + borderX;
            double yTo = (coordinateTo.y - minY) * scaleY + borderY;

            context.strokeLine(xFrom, yFrom, xTo, yTo);
        }

        // Circles
        context.setFill(Color.color(1, 0, 0, 0.5));
        for (Coordinate coordinate: coordinates) {
            double radius = 2;

            double x = (coordinate.x - minX) * scaleX + borderX;
            double y = (coordinate.y - minY) * scaleY + borderY;

            context.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
        }
    }
}
