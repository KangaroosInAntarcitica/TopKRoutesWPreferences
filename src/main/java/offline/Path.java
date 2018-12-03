package offline;

import java.io.Serializable;

@lombok.Getter
@lombok.Setter
public class Path implements Serializable {
    public int vertex;
    public int vertexTo;
    public int weight;
    public int nextVertex;
    public int lastVertex;

    public Path(String representation) {
        String[] data = representation.split(" ");
        vertex = Integer.parseInt(data[0]);
        vertexTo = Integer.parseInt(data[1]);
        if (data.length > 2) {
            weight = Integer.parseInt(data[2]);
        }
        if (data.length > 3) {
            nextVertex = Integer.parseInt(data[3]);
            lastVertex = Integer.parseInt(data[4]);
        } else {
            nextVertex = lastVertex = -1;
        }
    }

    public Path(int vertex, int vertexTo) {
        this.vertex = vertex;
        this.vertexTo = vertexTo;
    }
    public Path() {}

    public String toString() {
        return String.format("%d %d %d %d %d", vertex, vertexTo, weight, nextVertex, lastVertex);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Path)) return false;
        Path otherConnection = (Path) other;
        return this.vertex == otherConnection.vertex && this.vertexTo == otherConnection.vertexTo;
    }
}
