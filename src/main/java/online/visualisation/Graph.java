package online.visualisation;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;

public class Graph {
    private String title;
    private int width, height;
    private int border = 70;
    private int titleHeight = 50;
    private int number;

    public Function<Double, Double> applyX = (value) -> value;
    public Function<Double, Double> applyY = (value) -> value;

    private int pointsHeight = 5;

    private Stage stage;
    private Canvas canvas;
    private GraphicsContext context;

    public Graph(String title, int width, int height, int number) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.number = number;

        createStage();
    }

    public void createStage() {
        this.stage = new Stage();

        int totalWidth = width * number;
        int totalHeight = height + titleHeight;

        Pane pane = new Pane();
        Scene scene = new Scene(pane, totalWidth, totalHeight);

        stage.setTitle(title);
        stage.setScene(scene);

        canvas = new Canvas();
        canvas.setLayoutX(0); canvas.setLayoutY(0);
        pane.getChildren().add(canvas);
        canvas.setWidth(totalWidth);
        canvas.setHeight(totalHeight);

        context = canvas.getGraphicsContext2D();
        context.setFill(Color.color(0.2,0.2,0.2));
        context.setFont(new Font(titleHeight / 2));
        context.fillText(title, border, titleHeight * 3 / 4, width);

        this.stage.show();
    }

    public void drawGraph(int index, String name, String xName, String yName) {
        int x = width * index;
        int y = height + titleHeight;

        // Background
        context.setFill(Color.color(1, 1, 1));
        context.fillRect(x, y - height, width, height);

        context.setFill(Color.color(0.9, 0.9, 0.9));
        context.fillRect(x + border, y - height + border, width - border * 2, height - border * 2);

        // Title
        context.setFill(Color.color(0.7, 0.7, 0.7));
        context.setFont(new Font(border / 2));
        context.fillText(name, x + border, y - height + border - border / 4);

        // Bottom axes name
        context.setFill(Color.color(0.1,0.1,0.1));
        context.setFont(new Font(26));
        context.fillText(xName, x + border + 10, y - border / 4, width);

        // Left axes name
        Rotate r = new Rotate(-90, x + border / 4 + 10, y - border - 10);
        context.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
        context.fillText(yName, x + border / 4 + 10, y - border - 10, height);
        r = new Rotate(0);
        context.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
    }

    public void drawGraphData(int index, double[][] xData, double[][] yData) {
        xData = applyOnData(xData, applyX);
        yData = applyOnData(yData, applyY);

        DataProperties xProp = new DataProperties(xData);
        DataProperties yProp = new DataProperties(yData);

        int x = index * this.width + border;
        int y = this.height + this.titleHeight - border;
        int width = this.width - border * 2;
        int height = this.height - border * 2;
        double charLength = 5;

        context.setStroke(Color.color(0.1, 0.1, 0.1));
        context.setFill(Color.color(0.1, 0.1, 0.1));
        context.setFont(new Font(12));

        // Bottom line
        context.strokeLine(x, y, x + width, y);
        for (int i = 0; i <= xProp.number; i++){
            double lineX = x + width * (i * xProp.stepRelative + xProp.firstStepRelative);
            context.strokeLine(lineX, y - pointsHeight, lineX, y + pointsHeight);

            String value = xProp.getPointString(i);
            double length = charLength * value.length();
            context.fillText(value, lineX - length / 2, y + pointsHeight * 4);
        }

        // Left line
        context.strokeLine(x, y, x, y - height);
        for (int i = 0; i <= yProp.number; i++){
            double lineY = y - height * (i * yProp.stepRelative + yProp.firstStepRelative);
            context.strokeLine(x - pointsHeight, lineY, x + pointsHeight, lineY);

            String value = yProp.getPointString(i);
            double length = charLength * value.length();
            context.fillText(String.format("%3s", value), x - pointsHeight * 3 - length, lineY + 4);
        }

        // Content
        for (int i = 0; i < xData.length; i++){
            context.setStroke(colors[i]);

            double prevX = xProp.convertRelative(xData[i][0]) * width;
            double prevY = yProp.convertRelative(yData[i][0]) * height;

            for (int j = 1; j < xData[i].length; j++){
                double currentX = xProp.convertRelative(xData[i][j]) * width;
                double currentY = yProp.convertRelative(yData[i][j]) * height;

                context.strokeLine(
                        x + prevX,
                        y  - prevY,
                        x + currentX,
                        y + - currentY);

                prevX = currentX;
                prevY = currentY;
            }
        }
    }
    public void drawGraphData(int index, double[] xData, double[] yData) {
        double[][] xData2 = new double[1][xData.length];
        double[][] yData2 = new double[1][yData.length];
        for (int i = 0; i < xData.length; i++) {
            xData2[0][i] = xData[i];
            yData2[0][i] = yData[i];
        }
        drawGraphData(index, xData2, yData2);
    }
    public <T extends Object, P extends Object> void  drawGraphData(int index, T[][] xData, P[][] yData, Function<T, Double> xFunc, Function<P, Double> yFunc){
        double[][] xData2 = new double[xData.length][];
        double[][] yData2 = new double[yData.length][];
        for (int i = 0; i < xData.length; i++) {
            xData2[i] = new double[xData[i].length];
            yData2[i] = new double[yData[i].length];
            for (int j = 0; j < xData[i].length; j++){
                xData2[i][j] = xFunc.apply(xData[i][j]);
                yData2[i][j] = yFunc.apply(yData[i][j]);
            }
        }
        drawGraphData(index, xData2, yData2);
    }

    public void drawGraphLegend(int index, String[] names, int position) {
        int height = 14;
        int boxWidth = 10;
        int boxHeight = 4;
        int margin = 4;
        int number = names.length;

        double nameLength = 0;
        double charLength = height * 0.6;
        for (String name: names) { if (name.length() > nameLength) nameLength = name.length(); }
        double fullWidth = boxWidth + margin * 5 + nameLength * charLength;
        double fullHeight = (height + margin) * number + margin;
        double mainMargin = 20;

        // Position - which corner (like a unit circle parts in trigonometry)
        double x, y;
        if (position == 1 || position == 2) {
            x = width * index + border + mainMargin;
        } else {
            x = this.width - border - mainMargin - fullWidth;
        }
        if (position == 0 || position == 1) {
            y = titleHeight + border + mainMargin;
        } else {
            y = titleHeight + this.height - border - mainMargin - fullHeight;
        }
        x += index * this.width;

        context.setFont(new Font(height * 1.2));

        context.setFill(Color.color(0.95,0.95,0.95));
        context.fillRect(x, y, fullWidth, fullHeight);

        for (int i = 0; i < number; i++){
            double currentX = x + 10;
            double currentY = y + (height + margin) * i + margin;
            context.setFill(colors[i]);
            context.fillRect(currentX, currentY + (height - boxHeight) / 2, boxWidth, boxHeight);
            context.setFill(Color.color(0.5,0.5,0.5));
            context.fillText(names[i], currentX + boxWidth + margin * 2, currentY + height - 2,
                    fullWidth - boxWidth - 5 * margin);
        }
    }
    public void drawGraphLegend(int index, String[] names) {
        drawGraphLegend(index, names, 3);
    }

    public void save(String name) {
        // Saving file
        System.out.println("Saving result to file");
        File file = new File(name);

        try {
            WritableImage writableImage = new WritableImage(width * number, height + titleHeight);
            canvas.snapshot(null, writableImage);
            RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
            ImageIO.write(renderedImage, "png", file);
        } catch (IOException exception) {
            System.out.println("Result wasn't saved");
            exception.printStackTrace();
        }
    }

    private double[][] applyOnData(double[][] array, Function<Double, Double> func) {
        double[][] newArray = new double[array.length][];

        for (int i = 0; i < array.length; i++) {
            newArray[i] = new double[array[i].length];
            for (int j = 0; j < array[i].length; j++) {
                newArray[i][j] = func.apply(array[i][j]);
            }
        }

        return newArray;
    }

    private static Color[] colors = new Color[]{
        // Supports up to 15 colors
        Color.color(1, 0, 0),
        Color.color(0, 1, 0),
        Color.color(0, 0, 1),
        Color.color(0, 1, 1),
        Color.color(1, 0, 1),
        Color.color(1, 0.5, 0),
        Color.color(0.5, 1, 0),
        Color.color(0, 1, 0.5),
        Color.color(0, 0.5, 1),
        Color.color(0.5, 0, 1),
        Color.color(1, 0, 0.5),
        Color.color(0.5, 0.5, 0),
        Color.color(0, 0.5, 0.5),
        Color.color(0.5, 0, 0.5),
        Color.color(1, 1, 0)
    };

    public static class DataProperties {
        public int MIN_POINTS = 3;
        public int MAX_POINTS = 14;

        public final int number;
        public final double length;
        public final double min;
        public final double max;
        public final double step;
        public final double stepRelative;
        public final double firstStep;
        public final double firstStepRelative;

        public DataProperties(double[][] data) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    if (data[i][j] < min) min = data[i][j];
                    if (data[i][j] > max) max = data[i][j];
                }
            }

            double step = 1;
            int number = (int) Math.floor(max - min);
            double firstStep = 0;

            while (number > MAX_POINTS || number < MIN_POINTS)
            {
                firstStep = min;
                while (Math.abs(firstStep) >= step * .99) {
                    firstStep += firstStep > 0 ? -step : step;
                }
                if (firstStep != 0) {
                    firstStep = step - firstStep;
                }

                number = (int) Math.floor((max - min - firstStep) / step);
                if (number > MAX_POINTS) step = step * 2;
                if (number < MIN_POINTS) {
                    step = step / 2;
                    if (Math.floor(step) / step > 0.8)
                        step = Math.floor(step);
                }
            }

            firstStep = min;
            while (Math.abs(firstStep) >= step * .9) {
                firstStep += firstStep > 0 ? -step : step;
            }
            if (firstStep != 0) {
                firstStep = step - firstStep;
            }

            this.min = min;
            this.max = max;
            this.number = number;
            this.step = step;
            this.stepRelative = step / (max - min);
            this.length = max - min;
            this.firstStep = firstStep;
            this.firstStepRelative = firstStep / (max - min);
        }

        public double getPointValue(int index) {
            return min + firstStep + index * step;
        }

        public int getRequiredPointPrecision() {
            int i = 0;
            while (Math.floor(step * Math.pow(10, i)) <= 0) {
                i += 1;
            }
            return i;
        }

        public String getPointString(int index) {
            double value = getPointValue(index);
            int precision = getRequiredPointPrecision();

            if (precision == 0) {
                return String.format("%d", (int) Math.floor(value));
            } else {
                String regex = String.format("(?<=[.,][0-9]{%d})([0-9]+)", precision);
                String result = String.format("%.10f", value).replaceAll(regex, "");
                return result;
            }
        }

        public double convertRelative (double value) {
            return (value - min) / length;
        }
    }
}
