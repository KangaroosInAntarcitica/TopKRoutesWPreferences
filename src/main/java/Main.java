import offline.EdgeFrame;
import online.Online;

public class Main {
    public static void main(String[] args) {
        // generate2Hop();

        Online online = new Online();
    }


    public static void generate2Hop() {
        // 2 generate the table - needed only one time
        EdgeFrame edgeFrame = new EdgeFrame();
        edgeFrame.processEdges("sg");
    }
}
