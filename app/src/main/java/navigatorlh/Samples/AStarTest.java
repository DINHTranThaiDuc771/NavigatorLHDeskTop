package navigatorlh.Samples;

import java.io.IOException;
import java.io.StringReader;

import org.graphstream.algorithm.AStar;
import org.graphstream.algorithm.AStar.DistanceCosts;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSourceDGS;

public class AStarTest {

    // B-(1)-C
    // / \
    // (1) (10)
    // / \
    // A F
    // \ /
    // (1) (1)
    // \ /
    // D-(1)-E
    static String my_graph = "DGS004\n"
            + "my 0 0\n"
            + "an A xy: 0,1\n"
            + "an B xy: 1,2\n"
            + "an C xy: 2,2\n"
            + "an D xy: 1,0\n"
            + "an E xy: 2,0\n"
            + "an F xy: 3,1\n"
            + "ae AB A B weight:1 \n"
            + "ae AD A D weight:1 \n"
            + "ae BC B C weight:1 \n"
            + "ae CF C F weight:10 \n"
            + "ae DE D E weight:1 \n"
            + "ae EF E F weight:1 \n";

    public static void main(String[] args) throws IOException {
        Graph graph = new DefaultGraph("A Test");
        StringReader reader = new StringReader(my_graph);

        FileSourceDGS source = new FileSourceDGS();
        source.addSink(graph);
        source.readAll(reader);

        AStar astar = new AStar(graph);
        // astar.setCosts(new DistanceCosts());
        astar.compute("C", "F");

        System.out.println(astar.getShortestPath());
    }
}
