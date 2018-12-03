package online.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class WeightFrame {
    private List<VertexWeight> data;

    @lombok.Getter
    @lombok.Setter
    public class VertexWeight {
        private int vertex;
        private double weight;

        public String toString() {
            return String.format("%d (%f)", vertex, weight);
        }
    }

    private List<VertexWeight> readData(String cityCode) {
        String path = String.format("data/%s_stayTime.txt", cityCode);
        File file = new File(path);

        List<VertexWeight> data = new ArrayList<>();

        try (FileReader fileReader = new FileReader(file)) {
            Scanner scanner = new Scanner(fileReader);

            while (scanner.hasNext()) {
                String[] current = scanner.nextLine().split(":");

                VertexWeight item = new VertexWeight();
                item.vertex = Integer.parseInt(current[0]);
                item.weight = Double.parseDouble(current[1]);
                data.add(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    public WeightFrame(String cityCode) {
        data = readData(cityCode);
        data.sort(Comparator.comparingInt((VertexWeight v) -> v.vertex));
    }

    public double getVertexWeight(int vertex) {
        return data.get(vertex).weight;
    }
}
