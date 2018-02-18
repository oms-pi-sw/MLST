
import engines.impl.Alg1;
import engines.exceptions.NotConnectedGraphException;
import java.io.IOException;
import java.util.Set;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;
import mlst.struct.exceptions.LoopEdgeException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Niccolò Ferrari
 */
public class Debug {

  @Deprecated
  public static void debug() throws IOException, NotConnectedGraphException, LoopEdgeException, Exception {
    LabeledUndirectedGraph<Node, Edge<Node>> g = new LabeledUndirectedGraph<>();
    LabeledUndirectedGraph<Node, Edge<Node>> gc1, gc2;
    Node a, b, c, d, e;
    g.addNode(a = new Node("A"));
    g.addNode(b = new Node("B"));
    g.addNode(c = new Node("C"));
    g.addNode(d = new Node("D"));
    g.addNode(e = new Node("E"));
    g.addEdge(new Edge("label1", a, b));
    g.addEdge(new Edge("label4", a, b));
    g.addEdge(new Edge("label3", b, c));
    g.addEdge(new Edge("label2", b, c));
    g.addEdge(new Edge("label3", a, c));
    g.addEdge(new Edge("label4", b, d));
    g.addEdge(new Edge("label4", b, d));
    g.addEdge(new Edge("label2", b, e));
    g.addEdge(new Edge("label1", a, e));
    g.addEdge(new Edge("label5", d, e));
    g.addEdge(new Edge("label4", e, d));

    gc1 = new LabeledUndirectedGraph<>(g);
    gc1.removeNode(b);
    gc2 = new LabeledUndirectedGraph<>(g);
    gc2.removeNode(b);
    gc2.removeNode(d);

    System.out.println(">>>GRAPH");
    System.out.println(g);
    System.out.println("Connected? " + g.isConnected());
    Set<Node> n = g.getNeighbors(a);
    System.out.println("Neighbors of " + a + ": " + n);
    System.out.println(g);
    System.out.println(g.isConnected());

    System.out.println(">>>TEST:");
    System.out.println("GC1: " + gc1);
    System.out.println("GC1: connected? " + gc1.isConnected());
    System.out.println("GC2: " + gc2);
    System.out.println("GC2: connected? " + gc2.isConnected());
    System.out.println(">>>END TEST:");

    System.out.println();
    System.out.println(">>>Algorithm1:");
    Alg1<Node, Edge<Node>> al = new Alg1<>(g);
    System.out.println("Spanning tree for g:\n\t" + al.getSpanningTree());
    al.start();
    System.out.println("g:\n\t" + al.getGraph());
    System.out.println("minGraph:\n\t" + al.getMinGraph());
    System.out.println("spanning tree for minGraph:\n\t" + al.getSpanningTree());

    al.getGraph().calculateSpanningTree();
    al.getGraph().plot("G_graph_original.png");
    al.getMinGraph().calculateSpanningTree();
    al.getMinGraph().plot("G_mingraph_sub.png");
    LabeledUndirectedGraph<Node, Edge<Node>> sgraph = al.getSpanningTree();
    sgraph.calculateSpanningTree();
    sgraph.plot("G_spanning_sub.png");
  }
}
