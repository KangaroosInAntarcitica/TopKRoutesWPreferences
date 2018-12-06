import offline.EdgeFrame;
import online.Online;
import online.Query;

import java.math.BigInteger;


public class Main {
    public static void main(String[] args) {
        // generate2Hop();

        Online online = new Online();
        Query query = new Query();
        query.start = 0;
        query.end = 1;
        query.budget = 120;
        query.featurePreference = new double[]{0.5, 0.5};
        query.minFeatureValue = 0.5;

        online.processQuery(query);
    }


    public static void generate2Hop() {
        // 2 generate the table - needed only one time
        EdgeFrame edgeFrame = new EdgeFrame();
        edgeFrame.processEdges("sg");
    }
}
