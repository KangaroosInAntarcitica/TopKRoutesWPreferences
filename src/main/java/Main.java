import offline.EdgeFrame;
import online.FeaturesFrame;
import online.TwoHopFrame;
import online.WeightFrame;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.*;

public class Main {
    public static void main(String[] args) {
        SparkSession spark = SparkSession
                .builder()
                .appName("Keywords")
                .config("spark.master", "local")
                .getOrCreate();

        SparkContext jsc = spark.sparkContext();
        SQLContext sqlc = spark.sqlContext();

        String featuresPath = "./data/sg_inverted_kwds.txt";
        String twoHopPath = "./data/sg_undir_2hop.txt";
        String weightPath = "./data/sg_stayTime.txt";

        double[] featureRating = new double[]{0.5, 0.5};

        FeaturesFrame featuresFrame = new FeaturesFrame(spark, featuresPath);
        TwoHopFrame twoHopFrame = new TwoHopFrame(spark, twoHopPath);
        WeightFrame weightFrame = new WeightFrame(spark, weightPath);

        Dataset<Row> vertexes = featuresFrame.retrieveForFeatures(featureRating, 4);
        System.out.println(vertexes.count());
        Dataset<Row> result = twoHopFrame.retrieveForVertexes(vertexes, 10).sort(functions.col("vertex"));
        System.out.println(result.count());

        spark.close();
    }


    public void generate2Hop() {
        // 2 generate the table - needed only one time
        EdgeFrame edgeFrame = new EdgeFrame();
        edgeFrame.processEdges("./data/as_udig_edges.txt", "./data/as_undir_2hop.txt");
    }
}
