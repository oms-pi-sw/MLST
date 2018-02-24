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
import engines.Algorithm.Algorithms;
import engines.exceptions.NotConnectedGraphException;
import engines.impl.TabuSearch;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.logging.log4j.Level;
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
        System.out.println("MLST: v" + version() + " ALPHA");
        Algorithms.getValues().forEach(alg -> {
          System.out.println("\t" + alg + ": " + alg.getDesc());
        });
      } else if (commandLine.hasOption(INPUT)) {
        LogManager.getLogger().info("Reading graph from file...");
        LabeledUndirectedGraph<Node, SimpleEdge<Node>> rg = MLST.readGraph(commandLine.getOptionValue(INPUT).trim());

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

        graph = gg;
      }

      if (graph != null) {
        List<Algorithm<Node, SimpleEdge<Node>>> algs = new ArrayList<>();

        Map<Algorithm<Node, SimpleEdge<Node>>, Date> times = new HashMap<>();
        Map<Algorithm<Node, SimpleEdge<Node>>, Integer> costs = new HashMap<>();
        Map<Algorithm<Node, SimpleEdge<Node>>, LabeledUndirectedGraph<Node, SimpleEdge<Node>>> graphs = new HashMap<>();

        if (commandLine.hasOption(ALGORITHM)) {
          String algs_name = commandLine.getOptionValue(ALGORITHM);
          if (algs_name.equals("all")) {
            for (Algorithms _alg : Algorithms.getValues()) {
              algs.add(Algorithms.getAlgorithmInstance(_alg, graph));
            }
          } else {
            for (String alg_name : algs_name.split(",")) {
              List<Algorithm<Node, SimpleEdge<Node>>> _algs = Algorithms.getAlgorithmsInstances(alg_name, graph);
              if (_algs == null || _algs.isEmpty()) {
                LogManager.getLogger().warn("No algorithm found with name: " + alg_name);
              } else {
                algs.addAll(_algs);
              }
            }
          }
        }
        if (algs.isEmpty()) {
          algs.add(new TabuSearch<>(graph));
        }
        LogManager.getLogger().log(Level.getLevel("NOTICE"), "Selected algorithms:");
        algs.forEach(alg -> LogManager.getLogger().log(Level.getLevel("NOTICE"), alg.getClass().getSimpleName().toLowerCase()));
        println();

        String output = null;
        if (commandLine.hasOption(OUTPUT)) {
          output = commandLine.getOptionValue(OUTPUT);
          output = output.trim();
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected output: " + output);
        }

        String prefix = null;
        if (commandLine.hasOption(GRAPH)) {
          prefix = commandLine.getOptionValue(GRAPH);
          prefix = prefix.trim();
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected prefix for image names: " + prefix);
        }

        boolean nograph = false;
        if (commandLine.hasOption(NOGRAPH)) {
          nograph = true;
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected to NOT view graphs.");
        }

        boolean verbose = false;
        if (commandLine.hasOption(VERBOSE)) {
          verbose = true;
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You VERBOSE modality.");
        }

        //MULTITHREAD
        Integer max_threads = null;
        if (commandLine.hasOption(THREADS)) {
          max_threads = Integer.parseInt(commandLine.getOptionValue(THREADS).trim());
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected max number of threads: " + max_threads);
        }

        //MINQUEUE FOR TABU SEARCH
        Integer min_queue = null;
        if (commandLine.hasOption(MINQUEUE)) {
          min_queue = Integer.parseInt(commandLine.getOptionValue(MINQUEUE).trim());
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected minimum queue length: " + min_queue);
        }

        //DIVERSIFICATION FOR TABU SEARCH
        //MULTISTART
        boolean multi_start = false;
        int greedy_starts = 0, random_starts = 0;
        if (commandLine.hasOption(MULTISTART)) {
          multi_start = true;
          try {
            String int_number = "\\d+";
            String opt_value = commandLine.getOptionValue(MULTISTART).trim();
            if (opt_value.matches(int_number)) {
              greedy_starts = Integer.parseInt(opt_value);
            } else {
              String[] splitted = opt_value.split(",");
              for (String s : splitted) {
                s = s.trim();
                char last = s.charAt(s.length() - 1);
                if (last == 'r' || last == 'R') {
                  s = s.substring(0, s.length() - 1);
                  random_starts += Integer.parseInt(s);
                } else {
                  if (last == 'g' || last == 'G') {
                    s = s.substring(0, s.length() - 1);
                  }
                  greedy_starts += Integer.parseInt(s);
                }
              }
            }
            if (greedy_starts < 0 || random_starts < 0) {
              throw new NumberFormatException("Negative starts not feasible");
            }
          } catch (NumberFormatException ex) {
            throw ex;
          }
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected diversification Multi-Start technique. # of greedy starts: " + greedy_starts + ". # of random starts: " + random_starts);
        }

        //DIVERSIFICATION MOVE
        Boolean diversification_move = null;
        if (commandLine.hasOption(MOVEDIVERSIFICATION)) {
          diversification_move = true;
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected diversification with move technique.");
        }

        //DIVERSIFICATION LEARNING
        Boolean diversification_learning = null;
        if (commandLine.hasOption(DIVERSIFICATION)) {
          diversification_learning = true;
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected diversification learning technique.");
        }

        //INTENSIFICATION FOR TABU SEARCH
        //INTENSIFICATION LEARNING
        Boolean intenstification_learning = null;
        if (commandLine.hasOption(INTENSIFICATION)) {
          intenstification_learning = true;
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected intenstification learning technique.");
        }

        //INTENSIFICATION LEARNING
        Boolean intenstification_local_search = null;
        if (commandLine.hasOption(LOCALSEARCHI)) {
          intenstification_local_search = true;
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected intenstification with local search.");
        }

        //INTENSIFICATION PATH RELINKING
        Boolean path_relinking = null;
        if (commandLine.hasOption(PATHRELINKING)) {
          path_relinking = true;
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected intenstification with Path Relinking.");
        }
        //PATH RELINKING ALT
        Boolean path_relinking_alt = null;
        if (commandLine.hasOption(PATHRELINKINGALT)) {
          path_relinking = true;
          path_relinking_alt = true;
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected intenstification with Path Relinking Alternative Algorithm.");
        }
        //PATH RELINKING MIN OPTION
        Boolean path_relinking_min = null;
        if (path_relinking && commandLine.hasOption(PATHRELINKINGMIN)) {
          path_relinking_min = true;
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected intenstification with Path Relinking MIN option.");
        }

        //MAX ITERATIONS FOR TABU SEARCH
        Integer max_iter = null;
        if (commandLine.hasOption(MAXITER)) {
          max_iter = Integer.parseInt(commandLine.getOptionValue(MAXITER).trim());
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected maximum iterstions: " + max_iter);
        }

        //MAX ITERATIONS WITHOUT IMPROVEMENTS FOR TABU SEARCH
        Integer no_improvement = null;
        if (commandLine.hasOption(NOIMPROVEMENT)) {
          no_improvement = Integer.parseInt(commandLine.getOptionValue(NOIMPROVEMENT).trim());
          LogManager.getLogger().log(Level.getLevel("NOTICE"), "You selected maximum iterations without any improvements: " + no_improvement);
        }

        boolean found = false;
        for (Algorithm alg : algs) {
          if (alg != null && alg instanceof TabuSearch) {
            found = true;
            TabuSearch ts = (TabuSearch) alg;
            if (min_queue != null) {
              ts.setMinQueue(min_queue);
            }
            if (max_iter != null) {
              ts.setMaxIterations(max_iter);
            }
            if (no_improvement != null) {
              ts.setMaxIterationsWithoutImprovement(no_improvement);
            }

            //DIVERSIFICATION
            if (multi_start) {
              ts.setGreedyMultiStart(greedy_starts);
              ts.setRandomMultiStart(random_starts);
            }
            if (diversification_move != null) {
              ts.setMoveDiversification(diversification_move);
            }
            if (diversification_learning != null) {
              ts.setDiversificationLearning(diversification_learning);
            }

            //INTENSIFICATION
            if (path_relinking != null) {
              ts.setPathRelinking(path_relinking);
            }
            if (path_relinking_alt != null) {
              ts.setPathRelinkingAlt(path_relinking_alt);
            }
            if (path_relinking_min != null) {
              ts.setPathRelinkingMin(path_relinking_min);
            }
            if (intenstification_learning != null) {
              ts.setIntensificationLearning(intenstification_learning);
            }
            if (intenstification_local_search != null) {
              ts.setLocalSearchIntensification(intenstification_local_search);
            }
          }
        }
        if (!found) {
          LogManager.getLogger().warn("No Tabu Search found. Settings for TS algorithms does not take any effects.");
        }

        graph.calculateSpanningTree();
        println(Ansi.ansi().format().boldOn().format().fg(AnsiColor.BLUE).a("MAIN GRAPH:").format().reset());
        if (verbose) {
          println(Ansi.ansi().format().boldOn().a(graph).format().reset());
        }
        println();

        println();
        graph.printData();
        println();

        LogManager.getLogger().info("Plotting...");
        graph.plot(prefix + "_main.png", !nograph, (prefix != null));
        if (output != null && !output.isEmpty()) {
          LogManager.getLogger().info("Saving...");
          graph.save(output);
          graph.saveData("data_" + output);
        }
        LogManager.getLogger().info("All done.");

        for (Algorithm alg : algs) {
          String alg_name = alg.getClass().getSimpleName().trim().toLowerCase();
          alg.setMaxThreads(max_threads);

          try {
            LogManager.getLogger().info("Algorithm: " + alg_name);
            println(Ansi.ansi().format().fg(AnsiColor.RED).a("ALGORITHM: ").format().bg(AnsiColor.YELLOW).format().boldOn().a(alg_name).format().reset());

            //START ALGORITHM SECTION
            LogManager.getLogger().info("Starting calculating MLST with " + alg_name + "...");
            Date dstart = new Date();
            alg.run();
            Date dend = new Date();
            times.put(alg, new Date(dend.getTime() - dstart.getTime()));
            LogManager.getLogger().info("Done.");

            LogManager.getLogger().info("Calculating spanning tree...");
            LabeledUndirectedGraph<Node, SimpleEdge<Node>> min = alg.getMinGraph();
            min.calculateSpanningTree();
            costs.put(alg, min.calculateCost());
            graphs.put(alg, min);
            LogManager.getLogger().info("Done.");
            if (verbose) {
              LogManager.getLogger().log(Level.getLevel("VERBOSE"), "Check -> connected: " + min.isConnected());
            }
            //END ALGORITHM SECTION

            println();
            println(Ansi.ansi().format().boldOn().format().fg(AnsiColor.CYAN).a("MINIMUM LABELLING GRAPH:").format().reset());
            if (verbose) {
              println(Ansi.ansi().format().boldOn().a(graph).format().reset());
            }
            println();

            LogManager.getLogger().info("Plotting...");
            min.plot(prefix + "_" + alg_name + "_min.png", !nograph, (prefix != null));
            if (output != null && !output.isEmpty()) {
              LogManager.getLogger().info("Saving...");
              min.save("mingraph_" + alg_name + "_" + output);
              min.saveData("data_mingraph_" + alg_name + "_" + output);
            }

            println();
            min.printData();
            println();
            println();
          } catch (IOException ex) {
            LogManager.getLogger().error("IOException: " + ex.getMessage(), ex);
          } catch (Exception ex) {
            LogManager.getLogger().fatal("Exception: " + ex.getMessage(), ex);
          }
        }

        LogManager.getLogger().info("All done.");
        algs.forEach(alg -> {
          Date time = times.get(alg);
          Integer cost = costs.get(alg);
          LabeledUndirectedGraph<Node, SimpleEdge<Node>> g = graphs.get(alg);

          long diff = time.getTime();
          long diffMills = diff % 1000;
          long diffSeconds = diff / 1000 % 60;
          long diffMinutes = diff / (60 * 1000) % 60;
          long diffHours = diff / (60 * 60 * 1000);
          println(Ansi.ansi()
                  .format().fg(AnsiColor.MAGENTA).a("For algorithm ").a(alg.getClass().getSimpleName().trim().toLowerCase()).a(": ")
                  .format().boldOn().a(diff).format().boldOff().a("ms")
                  .nl().a("\t")
                  .a(diffHours).a(":").a(diffMinutes).a(":").a(diffSeconds).a(".").a(diffMills)
                  .nl().a("\t")
                  .a("Cost: ").a(cost)
                  .nl().a("\t")
                  .a("Consistent: ").a(g.isConnected())
                  .format().reset());
        });
        println(Ansi.ansi()
                .format().fg(AnsiColor.MAGENTA)
                .a("Initial cost: ").a(graph.calculateCost())
                .format().reset());
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
    } catch (InvocationTargetException ex) {
      LogManager.getLogger().fatal("Exception during initialization of algorithms: " + ex.getMessage() + ". Cause: " + ex.getCause().getMessage(), ex);
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
  private static final String VERBOSE = "verbose";
  private static final String THREADS = "threads";
  private static final String MINQUEUE = "min-queue";
  private static final String MAXITER = "max-iter";
  private static final String NOIMPROVEMENT = "no-improvement";
  private static final String MULTISTART = "multistart";
  private static final String MOVEDIVERSIFICATION = "move-diversification";
  private static final String PATHRELINKING = "path-relinking";
  private static final String LOCALSEARCHI = "local-search-intensification";
  private static final String INTENSIFICATION = "intensification";
  private static final String DIVERSIFICATION = "diversification";
  private static final String PATHRELINKINGALT = "path-relinking-alt";
  private static final String PATHRELINKINGMIN = "path-relinking-min";

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

    String desc = "Choose algorithm, you can select more algorithm at same time separating them with a comma:";
    desc = Algorithms.getValues().stream().map((alg) -> System.lineSeparator() + "* " + alg + " " + alg.getDesc()).reduce(desc, String::concat);
    Option algorithm = new Option("a", ALGORITHM, true, desc);
    algorithm.setArgName(ALGORITHM);
    algorithm.setRequired(false);
    algorithm.setOptionalArg(false);
    algorithm.setType(String.class);
    options.addOption(algorithm);

//    Algorithms.getValues().forEach(alg -> {
//      Option al = new Option(null, alg.toString(), false, alg.getDesc() + ". Same to use -a.");
//      al.setArgName(alg.name());
//      al.setRequired(false);
//      options.addOption(al);
//    });
    Option nograph = new Option("n", NOGRAPH, false, "Don't open graphic interface for graph.");
    nograph.setArgName(NOGRAPH);
    nograph.setRequired(false);
    options.addOption(nograph);

    Option verbose = new Option(null, VERBOSE, false, "Enable verbose modality.");
    verbose.setArgName(VERBOSE);
    verbose.setRequired(false);
    options.addOption(verbose);

    Option threads = new Option("t", THREADS, true, "To USE WITH Tabu Search heuristic algorithm or Bottom-Up MultiThreaded algorithm. Specify number of threads. Works only for multithreading algorithms.");
    threads.setArgName(THREADS);
    threads.setRequired(false);
    threads.setOptionalArg(false);
    threads.setType(Integer.class);
    options.addOption(threads);

    Option minqueue = new Option("m", MINQUEUE, true, "To USE WITH Tabu Search heuristic algorithm. Specify the max queue of forbidden moves in Tabu Search.");
    minqueue.setArgName(MINQUEUE);
    minqueue.setRequired(false);
    minqueue.setOptionalArg(false);
    minqueue.setType(Integer.class);
    options.addOption(minqueue);

    Option maxiter = new Option("I", MAXITER, true, "To USE WITH Tabu Search heuristic algorithm. Specify the max number of iterations.");
    maxiter.setArgName(MAXITER);
    maxiter.setRequired(false);
    maxiter.setOptionalArg(false);
    maxiter.setType(Integer.class);
    options.addOption(maxiter);

    Option noimprovement = new Option("N", NOIMPROVEMENT, true, "To USE WITH Tabu Search heuristic algorithm. Specify the max number of iterations without improvement.");
    noimprovement.setArgName(NOIMPROVEMENT);
    noimprovement.setRequired(false);
    noimprovement.setOptionalArg(false);
    noimprovement.setType(Integer.class);
    options.addOption(noimprovement);

    Option multistart = new Option("M", MULTISTART, true, "To USE WITH Tabu Search heuristic algorithm. Specify the number of multistart. [DIVERSIFICATION OPTION]");
    multistart.setArgName(MULTISTART);
    multistart.setRequired(false);
    multistart.setOptionalArg(false);
    multistart.setType(Integer.class);
    options.addOption(multistart);

    Option movediversification = new Option("R", MOVEDIVERSIFICATION, false, "To USE WITH Tabu Search heuristic algorithm. Specify a stronger policy for tabu queue rejection in order to diversify more. [DIVERSIFICATION OPTION]");
    movediversification.setArgName(MOVEDIVERSIFICATION);
    movediversification.setRequired(false);
    options.addOption(movediversification);

    Option pathrelinking = new Option("P", PATHRELINKING, false, "To USE WITH Tabu Search heuristic algorithm. Require to use path relinking intensification heuristic after tabu search collected all elite results. [INTENSIFICATION OPTION]");
    pathrelinking.setArgName(PATHRELINKING);
    pathrelinking.setRequired(false);
    options.addOption(pathrelinking);

    Option pathrelinkingalt = new Option("S", PATHRELINKINGALT, false, "To USE WITH Tabu Search heuristic algorithm. Require to use path relinking alternative algorithm (simplier and faster) intensification heuristic after tabu search collected all elite results. [INTENSIFICATION OPTION]");
    pathrelinkingalt.setArgName(PATHRELINKINGALT);
    pathrelinkingalt.setRequired(false);
    options.addOption(pathrelinkingalt);

    Option pathrelinkingmin = new Option(null, PATHRELINKINGMIN, false, "To USE WITH Tabu Search heuristic algorithm. Require to use path relinking alternative algorithm (simplier and faster) intensification heuristic after tabu search collected all elite results. [INTENSIFICATION OPTION]");
    pathrelinkingmin.setArgName(PATHRELINKINGMIN);
    pathrelinkingmin.setRequired(false);
    options.addOption(pathrelinkingmin);

    Option localsearchi = new Option("L", LOCALSEARCHI, false, "To USE WITH Tabu Search heuristic algorithm. Require to use path relinking intensification heuristic after tabu search collected all elite results. [INTENSIFICATION OPTION]");
    localsearchi.setArgName(LOCALSEARCHI);
    localsearchi.setRequired(false);
    options.addOption(localsearchi);

    Option intensification = new Option("A", INTENSIFICATION, false, "To USE WITH Tabu Search heuristic algorithm. Use a \"good moves\" list to learn best moves and evaluate after exploration of neighborhood. [INTENSIFICATION OPTION]");
    intensification.setArgName(INTENSIFICATION);
    intensification.setRequired(false);
    options.addOption(intensification);

    Option diversification = new Option("B", DIVERSIFICATION, false, "To USE WITH Tabu Search heuristic algorithm. Use a \"frequently moves\" list to learn too frequent moves and reject after exploration of neighborhood. [DIVERSIFICATION OPTION]");
    diversification.setArgName(DIVERSIFICATION);
    diversification.setRequired(false);
    options.addOption(diversification);

    required.setRequired(true);
    options.addOptionGroup(required);

    return options;
  }
}
