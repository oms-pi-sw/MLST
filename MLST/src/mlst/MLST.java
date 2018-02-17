/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlst;

import ansiTTY.ansi.Ansi;
import static ansiTTY.ansi.Ansi.print;
import static ansiTTY.ansi.Ansi.println;
import ansiTTY.ansi.format.AnsiColor;
import engines.Algorithm;
import engines.alg1.Alg1;
import engines.exceptions.NotConnectedGraphException;
import engines.ralg1.RAlg1;
import engines.vcover.VCover;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;
import mlst.struct.SimpleEdge;
import mlst.struct.exceptions.LoopEdgeException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Niccolò Ferrari
 */
public class MLST {

  //Version
  private static final int MAJOR = 0;
  private static final int MINOR = 1;
  private static final int REVISION = 0;

  private static String version() {
    return MAJOR + "." + MINOR + "." + REVISION;
  }

  public static LabeledUndirectedGraph<Node, SimpleEdge<Node>> readGraph(String filename) throws IOException, LoopEdgeException {
    File file = new File(filename);
    if (file.exists() && file.canRead()) {
      boolean first = true;
      List<Node> nodes = new ArrayList<>();
      List<SimpleEdge> edges = new ArrayList<>();
      for (String line : Files.lines(file.toPath()).collect(Collectors.toList())) {
        if (!line.isEmpty()) {
          if (first) {
            first = false;
            List<String> nodes_names = new ArrayList<>(Arrays.asList(line.split(" ")));
            nodes_names.forEach(name -> {
              name = name.trim();
              if (!name.isEmpty()) {
                nodes.add(new Node(name));
              }
            });
          } else {
            //READ EDGES
            String label = null, node1 = null, node2 = null;
            List<String> edge_parts = new ArrayList<>(Arrays.asList(line.split(" ")));
            int k = 0;

            //READ EDGE PARTS
            for (String part : edge_parts) {
              part = part.trim();
              if (!part.isEmpty()) {
                k++;
                switch (k) {
                  case 1:
                    label = part;
                    break;
                  case 2:
                    node1 = part;
                    break;
                  case 3:
                    node2 = part;
                    break;
                  default:
                    break;
                }
              }
              if (k >= 3) {
                break;
              }
            }

            //SELECT NODES
            if (label != null && node1 != null && node2 != null) {
              Node n1 = null, n2 = null;
              for (Node n : nodes) {
                if (n.getName().equals(node1)) {
                  n1 = n;
                } else if (n.getName().equals(node2)) {
                  n2 = n;
                }
              }

              if (n1 != null && n2 != null) {
                edges.add(new SimpleEdge(label, n1, n2));
              }
            }
          }
        }
      }
      LabeledUndirectedGraph<Node, SimpleEdge<Node>> graph = new LabeledUndirectedGraph<>(nodes);
      edges.forEach(edge -> graph.addEdge(edge));
      return graph;
    } else {
      return null;
    }
  }

  public static LabeledUndirectedGraph<Node, SimpleEdge<Node>> generateGraph(int nNodes, int nLabels, int nMoreEdges) throws LoopEdgeException {
    if ((nMoreEdges + nNodes - 1) > ((nNodes) * (nNodes - 1) / 2)) {
      nMoreEdges = ((nNodes) * (nNodes - 1) / 2) - (nNodes - 1);
    }

    List<Node> nodes = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    List<SimpleEdge> edges = new ArrayList<>();

    Random rand = new Random();

    for (int i = 0; i < nLabels; i++) {
      labels.add("Label" + i);
    }

    Node n = new Node(Integer.toString(0));
    nodes.add(n);
    for (int i = 1; i < nNodes; i++) {
      n = new Node(Integer.toString(i));
      Node randNode = nodes.get(rand.nextInt(nodes.size()));
      String label = labels.get(rand.nextInt(labels.size()));
      edges.add(new SimpleEdge(label, n, randNode));
      nodes.add(n);
    }

    for (int i = 0; i < nMoreEdges; i++) {
      boolean ok = false;
      Node n1 = null, n2 = null;
      while (!ok) {
        n1 = nodes.get(rand.nextInt(nodes.size()));
        n2 = nodes.get(rand.nextInt(nodes.size()));
        if (n1 != n2) {
          final Node _n1 = n1, _n2 = n2;
          if (edges.stream()
                  .filter(edge -> (edge.getNode1().equals(_n1) && edge.getNode2().equals(_n2)) || (edge.getNode1().equals(_n2) && edge.getNode2().equals(_n1)))
                  .collect(Collectors.toList()).isEmpty()) {

            ok = true;
          }
        }
      }
      String label = labels.get(rand.nextInt(labels.size()));
      if (n1 != null && n2 != null) {
        edges.add(new SimpleEdge(label, n1, n2));
      }
    }

    LabeledUndirectedGraph<Node, SimpleEdge<Node>> graph = new LabeledUndirectedGraph<>(nodes);
    edges.forEach(edge -> graph.addEdge(edge));

    return graph;
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    try {
      System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

      Options options = setOptions();
      DefaultParser parser = new DefaultParser();
      CommandLine commandLine = parser.parse(options, args);

      LabeledUndirectedGraph<Node, SimpleEdge<Node>> graph = null;

      if (commandLine.hasOption(HELP)) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("MLST", options);
      } else if (commandLine.hasOption(VERSION)) {
        System.out.println("MLST: v" + version() + " DEMO");
        System.out.println("\t" + Alg1.class.getSimpleName().toLowerCase() + ": exact algorithm: start from complete graph and removes edges.");
        System.out.println("\t" + RAlg1.class.getSimpleName().toLowerCase() + ": exact algorithm: start from empty graph and adds edges. [NOT SUPPORTED YET]");
        System.out.println("\t" + VCover.class.getSimpleName().toLowerCase() + ": Heuristic algorithm. [NOT SUPPORTED YET]");
      } else if (commandLine.hasOption(INPUT)) {
        LogManager.getLogger().info("Reading graph from file...");
        LabeledUndirectedGraph<Node, SimpleEdge<Node>> rg = MLST.readGraph(commandLine.getOptionValue(INPUT).trim());
        rg.calculateSpanningTree();
        println(Ansi.ansi().format().boldOn().format().fg(AnsiColor.BLUE).a("LOADED GRAPH:").format().reset().nl().format().boldOn().a(rg).format().reset());
        println();

        graph = rg;
      } else if (commandLine.hasOption(RANDOM)) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        print(Ansi.ansi().format().fg(AnsiColor.YELLOW).a("# OF NODES [int]? ").format().reset());
        int nnodes = Integer.parseInt(bufferedReader.readLine());

        print(Ansi.ansi().format().fg(AnsiColor.YELLOW).a("# OF LABELS [int]? ").format().reset());
        int nlabels = Integer.parseInt(bufferedReader.readLine());

        print(Ansi.ansi().format().fg(AnsiColor.YELLOW).a("# OF EDGES [int]? ").format().reset());
        int nedges = Integer.parseInt(bufferedReader.readLine());

        if (nnodes < 2) {
          LogManager.getLogger().warn("Min number of nodes: 2. You entered: " + nnodes);
          nnodes = 2;
        }

        if (nlabels < 1) {
          LogManager.getLogger().warn("Min number of labels: 1. You entered: " + nlabels);
          nlabels = 1;
        }

        if ((nedges) > ((nnodes) * (nnodes - 1) / 2)) {
          LogManager.getLogger().warn("Max number of edges: " + ((nnodes) * (nnodes - 1) / 2) + ". You entered " + nedges);
          nedges = ((nnodes) * (nnodes - 1) / 2);
        }

        if ((nedges) < (nnodes - 1)) {
          LogManager.getLogger().warn("Min number of edges: " + (nnodes - 1) + ". You entered " + nedges);
          nedges = (nnodes - 1);
        }

        nedges -= (nnodes - 1);

        println(Ansi.ansi()
                .a("Nodes: ")
                .format().fg(AnsiColor.GREEN).a(nnodes).format().reset()
                .a(" Labels: ")
                .format().fg(AnsiColor.GREEN).a(nlabels).format().reset()
                .a(" Edges: ")
                .format().fg(AnsiColor.GREEN).a((nedges + (nnodes - 1))).format().reset());
        println();

        LogManager.getLogger().info("Generating random graph...");
        LabeledUndirectedGraph<Node, SimpleEdge<Node>> gg = MLST.generateGraph(nnodes, nlabels, nedges);
        gg.calculateSpanningTree();
        println(Ansi.ansi().format().boldOn().format().fg(AnsiColor.BLUE).a("GENERATED GRAPH:").format().reset().nl().format().boldOn().a(gg).format().reset());
        println();

        graph = gg;
      }

      if (graph != null) {
        Algorithm<Node, SimpleEdge<Node>> alg = new Alg1<>(graph);

        if (commandLine.hasOption(ALGORITHM)) {
          String alg_name = commandLine.getOptionValue(ALGORITHM);
          if (alg_name.trim().toLowerCase().equalsIgnoreCase(Alg1.class.getSimpleName().toLowerCase())) {
            alg = new Alg1<>(graph);
          } else if (alg_name.trim().toLowerCase().equalsIgnoreCase(RAlg1.class.getSimpleName().toLowerCase())) {
            alg = new RAlg1<>(graph);
          } else if (alg_name.trim().toLowerCase().equalsIgnoreCase(VCover.class.getSimpleName().toLowerCase())) {
            alg = new VCover<>(graph);
          } else {
            LogManager.getLogger().warn("No algorithm found, set to default...");
          }
        }

        LogManager.getLogger().info("Algorithm: " + alg.getClass().getSimpleName().toLowerCase());

        String output = null;
        if (commandLine.hasOption(OUTPUT)) {
          output = commandLine.getOptionValue(OUTPUT);
          output = output.trim();
        }

        String prefix = null;
        if (commandLine.hasOption(GRAPH)) {
          prefix = commandLine.getOptionValue(GRAPH);
          prefix = prefix.trim();
        }

        boolean nograph = false;
        if (commandLine.hasOption(NOGRAPH)) {
          nograph = true;
        }

        LogManager.getLogger().info("Plotting...");
        graph.plot(prefix + "_main.png", !nograph, (prefix != null));
        if (output != null && !output.isEmpty()) {
          LogManager.getLogger().info("Saving...");
          graph.save(output);
          graph.saveData("data_" + output);
        }
        LogManager.getLogger().info("All done.");

        LogManager.getLogger().info("Starting calculating MLST with " + alg.getClass().getSimpleName().toLowerCase() + "...");
        alg.start();
        LogManager.getLogger().info("Done.");

        LogManager.getLogger().info("Calculating spanning tree...");
        LabeledUndirectedGraph<Node, SimpleEdge<Node>> min = alg.getMinGraph();
        min.calculateSpanningTree();
        LogManager.getLogger().info("Done.");
        println();
        println(Ansi.ansi().format().boldOn().format().fg(AnsiColor.CYAN).a("MINIMUM LABELLING GRAPH:").format().reset().nl().format().boldOn().a(graph).format().reset());
        println();
        LogManager.getLogger().info("Plotting...");
        min.plot(prefix + "_min.png", !nograph, (prefix != null));
        if (output != null && !output.isEmpty()) {
          LogManager.getLogger().info("Saving...");
          min.save("mingraph_alg1_" + output);
          min.saveData("data_mingraph_alg1_" + output);
        }

        println();
        min.printData();
        println();

        LogManager.getLogger().info("All done.");
      }
    } catch (NotConnectedGraphException ex) {
      LogManager.getLogger().error("NotConnectedGraphException: " + ex.getMessage(), ex);
    } catch (IOException ex) {
      LogManager.getLogger().error("IOException: " + ex.getMessage(), ex);
    } catch (ParseException ex) {
      LogManager.getLogger().error("ParseException: " + ex.getMessage(), ex);
    } catch (NumberFormatException ex) {
      LogManager.getLogger().error("NumberFormatException: " + ex.getMessage(), ex);
    } catch (LoopEdgeException ex) {
      LogManager.getLogger().fatal("LoopEdgeException: " + ex.getMessage(), ex);
    } catch (Exception ex) {
      LogManager.getLogger().fatal("Exception: " + ex.getMessage(), ex);
    }
  }
  //Options
  private static final String INPUT = "input";
  private static final String RANDOM = "random";
  private static final String HELP = "help";
  private static final String VERSION = "version";
  private static final String OUTPUT = "output";
  private static final String GRAPH = "graph";
  private static final String NOGRAPH = "nograph";
  private static final String ALGORITHM = "algorithm";

  private static Options setOptions() {
    Options options = new Options();
    OptionGroup required = new OptionGroup();

    //OptionGroup of mutual exclusive required options
    Option input = new Option("i", INPUT, true, "The input graph file.");
    input.setArgName(INPUT);
    input.setRequired(true);
    input.setOptionalArg(false);
    input.setType(String.class);
    required.addOption(input);

    Option help = new Option("h", HELP, false, "Print help.");
    help.setArgName(HELP);
    help.setRequired(true);
    required.addOption(help);

    Option version = new Option("v", VERSION, false, "Print version.");
    version.setArgName(VERSION);
    version.setRequired(true);
    required.addOption(version);

    Option random = new Option("r", RANDOM, false, "Generate random graph.");
    random.setArgName(RANDOM);
    random.setRequired(true);
    required.addOption(random);

    //Other options
    Option filename = new Option("o", OUTPUT, true, "The output filename where to save the graph.");
    filename.setArgName(OUTPUT);
    filename.setOptionalArg(false);
    filename.setRequired(false);
    filename.setType(String.class);
    options.addOption(filename);

    Option graph = new Option("g", GRAPH, true, "Prefix of filenames where save graphs.");
    graph.setArgName(GRAPH);
    graph.setRequired(false);
    graph.setOptionalArg(false);
    graph.setType(String.class);
    options.addOption(graph);

    Option algorithm = new Option("a", ALGORITHM, true, "Choose algorithm:" + System.lineSeparator()
            + "* " + Alg1.class.getSimpleName().toLowerCase() + System.lineSeparator()
            + "* " + RAlg1.class.getSimpleName().toLowerCase() + System.lineSeparator()
            + "* " + VCover.class.getSimpleName().toLowerCase());
    algorithm.setArgName(ALGORITHM);
    algorithm.setRequired(false);
    algorithm.setOptionalArg(false);
    algorithm.setType(String.class);
    options.addOption(algorithm);

    Option nograph = new Option("n", NOGRAPH, false, "Don't open graphic interface for graph.");
    nograph.setArgName(NOGRAPH);
    nograph.setRequired(false);
    options.addOption(nograph);

    required.setRequired(true);
    options.addOptionGroup(required);

    return options;
  }
}