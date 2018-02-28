/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.impl;

import ansiTTY.ansi.Ansi;
import static ansiTTY.ansi.Ansi.print;
import static ansiTTY.ansi.Ansi.println;
import engines.Algorithm;
import engines.exceptions.NotConnectedGraphException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

/**
 * Tabu Search algorithm class.
 *
 * @author Niccolò Ferrari
 * @param <N> The type of Node.
 * @param <E> The type of Edge.
 */
public class TabuSearch<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  private static final Level ALGORITHM = Level.getLevel("ALGORITHM");

  private volatile int minCost = 1,
          maxIterations = 100,
          maxIterationsWithoutImprovement = 50,
          minQueue = 10,
          risingTrend = 1;

  private final List<LabeledUndirectedGraph<N, E>> minGraphs = new ArrayList<>();
  private final Object lock = new Object();

  private final List<LabeledUndirectedGraph<N, E>> elites = new ArrayList<>();

  // Intensifications options
  private volatile boolean pathRelinking = false, pathRelinkingAlt = false, pathRelinkingMin = false, pathRelinkingDesc = false,
          localSearchIntensification = false,
          intensificationLearning = false,
          intensification = false;

  // Diversifications options
  private volatile int greedyMultiStart = 1, randomMultiStart = 0;
  private volatile boolean moveDiversification = false,
          diversificationLearning = false,
          diversification = false;
  private volatile int diversificationMaxEquivalentMoves = 5;
  private volatile Integer randomIters = null, notRandomIters = null;

  /**
   * Constructor.
   *
   * @param graph the graph to minimize.
   * @throws NotConnectedGraphException
   */
  public TabuSearch(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
  }

  /**
   * Class that describe a move.
   */
  public class MoveEdge implements Comparable<MoveEdge> {

    private final E out, in;
    private final LabeledUndirectedGraph<N, E> graph;
    private final int afterCost, beforeCost;

    /**
     * Constructor.
     *
     * @param out the removed Edge.
     * @param in the added Edge.
     * @param graph The graph to modify.
     */
    public MoveEdge(E out, E in, LabeledUndirectedGraph<N, E> graph) {
      this.out = out;
      this.in = in;
      this.graph = new LabeledUndirectedGraph<>(graph);
      beforeCost = this.graph.calculateCost();
      this.graph.removeEdge(out);
      this.graph.addEdge(in);
      afterCost = this.graph.calculateCost();
    }

    /**
     * Get removed edge.
     *
     * @return the removed edge.
     */
    public E getOut() {
      return out;
    }

    /**
     * Get the added edge.
     *
     * @return the added edge.
     */
    public E getIn() {
      return in;
    }

    /**
     * Get the modified graph.
     *
     * @return the modified graph.
     */
    public LabeledUndirectedGraph<N, E> getGraph() {
      return graph;
    }

    /**
     * Get the cost of resulting graph.
     *
     * @return the cost of resulting graph.
     */
    public int getAfterCost() {
      return afterCost;
    }

    /**
     * Get the cost of original graph.
     *
     * @return the cost of original graph.
     */
    public int getBeforeCost() {
      return beforeCost;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 97 * hash + Objects.hashCode(this.out);
      hash = 97 * hash + Objects.hashCode(this.in);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final MoveEdge other = (MoveEdge) obj;
      if (!moveDiversification) {
        return (Objects.equals(in, other.out));
      } else {
        return (Objects.equals(in, other.out) || Objects.equals(out, other.in));
      }
    }

    @Override
    public int compareTo(MoveEdge o) {
      return (afterCost - beforeCost) - (o.afterCost - o.beforeCost);
    }

    @Override
    public String toString() {
      return "MoveEdge{" + "out=" + out + ", in=" + in + ", afterCost=" + afterCost + ", beforeCost=" + beforeCost + '}';
    }

    /**
     * Check if this move is identical to another.
     *
     * @param other the other move.
     * @return true if the other move is identical to this.
     */
    public boolean isSame(MoveEdge other) {
      if (other == this) {
        return true;
      }
      return Objects.equals(in, other.in) && Objects.equals(out, other.out);
    }
  }

  public class MoveLabel implements Comparable<MoveLabel> {

    private final Set<E> out, in;
    private final LabeledUndirectedGraph<N, E> graph;
    private final int afterCost, beforeCost;

    /**
     * Constructor.
     *
     * @param out the removed Edge.
     * @param in the added Edge.
     * @param graph The graph to modify.
     */
    public MoveLabel(Set<E> out, Set<E> in, LabeledUndirectedGraph<N, E> graph) {
      this.out = out;
      this.in = in;
      this.graph = new LabeledUndirectedGraph<>(graph);
      beforeCost = this.graph.calculateCost();
      this.out.forEach(e -> this.graph.removeEdge(e));
      this.in.forEach(e -> this.graph.addEdge(e));
      afterCost = this.graph.calculateCost();
    }

    /**
     * Get removed edge.
     *
     * @return the removed edge.
     */
    public Set<E> getOut() {
      return out;
    }

    /**
     * Get the added edge.
     *
     * @return the added edge.
     */
    public Set<E> getIn() {
      return in;
    }

    /**
     * Get the modified graph.
     *
     * @return the modified graph.
     */
    public LabeledUndirectedGraph<N, E> getGraph() {
      return graph;
    }

    /**
     * Get the cost of resulting graph.
     *
     * @return the cost of resulting graph.
     */
    public int getAfterCost() {
      return afterCost;
    }

    /**
     * Get the cost of original graph.
     *
     * @return the cost of original graph.
     */
    public int getBeforeCost() {
      return beforeCost;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 97 * hash + Objects.hashCode(out);
      hash = 97 * hash + Objects.hashCode(in);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final MoveEdge other = (MoveEdge) obj;
      if (!moveDiversification) {
        return (Objects.equals(in, other.out));
      } else {
        return (Objects.equals(in, other.out) || Objects.equals(out, other.in));
      }
    }

    @Override
    public int compareTo(MoveLabel o) {
      return (afterCost - beforeCost) - (o.afterCost - o.beforeCost);
    }

    @Override
    public String toString() {
      return "MoveLabel{" + "out=" + out + ", in=" + in + ", afterCost=" + afterCost + ", beforeCost=" + beforeCost + '}';
    }

    /**
     * Check if this move is identical to another.
     *
     * @param other the other move.
     * @return true if the other move is identical to this.
     */
    public boolean isSame(MoveLabel other) {
      if (other == this) {
        return true;
      }
      return Objects.equals(in, other.in) && Objects.equals(out, other.out);
    }

  }

  /**
   * Class that stores move statistics, used to implement some intensifications
   * and diversifications techniques.
   */
  public class MoveStat {

    private final MoveEdge move;
    private int occurences = 1;
    private int improvements = 0;

    /**
     * Constructor.
     *
     * @param move the move.
     */
    public MoveStat(MoveEdge move) {
      this.move = move;
    }

    public int getOccurences() {
      return occurences;
    }

    public void incOccurences() {
      occurences++;
    }

    public int getImprovements() {
      return improvements;
    }

    public void incImprovements() {
      improvements++;
    }

    public MoveEdge getMove() {
      return move;
    }

    public boolean hasMove(MoveEdge move) {
      return this.move.isSame(move);
    }

    @Override
    public String toString() {
      return "MoveStat{" + "move=" + move + ", occurences=" + occurences + ", improvements=" + improvements + '}';
    }
  }

  /**
   * Get the set of moves from a feasible solution (<code>sk</code> must be a
   * spanning tree)
   *
   * @param sk a spanning tree.
   * @return the set of moves ordered by the delta of cost before and after this
   * move.
   */
  public List<MoveEdge> getEdgeNeighborhood(final LabeledUndirectedGraph<N, E> sk) {
    final List<MoveEdge> neighborood = new ArrayList<>();
    final LabeledUndirectedGraph<N, E> csk = new LabeledUndirectedGraph<>(sk);
    // For each spanning edges...
    csk.getEdges().forEach(edge -> {
      csk.removeEdge(edge);
      final List<Set<N>> nodes = new ArrayList<>(csk.getSubGraphsNodes());
      if (nodes.size() == 2) {
        final Set<E> candidates = new HashSet<>();
        csk.getRemovedEdges().stream()
                .filter(redge -> (!redge.equals(edge))
                && ((nodes.get(0).contains(redge.getNode1()) && nodes.get(1).contains(redge.getNode2()))
                || (nodes.get(0).contains(redge.getNode2()) && nodes.get(1).contains(redge.getNode1()))))
                .forEachOrdered(redge -> candidates.add(redge));
        candidates.forEach(candidate -> neighborood.add(new MoveEdge(edge, candidate, sk)));
      }
      csk.addEdge(edge);
    });
    return neighborood;
  }

  /**
   * Get statistics for a given move.
   *
   * @param moveStatsList the move statistics list.
   * @param move the move.
   * @return the move statistics related to given move.
   */
  protected MoveStat getStat(List<MoveStat> moveStatsList, MoveEdge move) {
    MoveStat stat = null;
    for (MoveStat s : moveStatsList) {
      if (s.hasMove(move)) {
        stat = s;
        break;
      }
    }
    return stat;
  }

  /**
   * Increment occurrences of a move.
   *
   * @param moveStatsList the move statistics list.
   * @param move the move.
   */
  protected void incOccurrences(List<MoveStat> moveStatsList, MoveEdge move) {
    boolean found = false;
    for (MoveStat s : moveStatsList) {
      if (s.hasMove(move)) {
        s.incOccurences();
        found = true;
        break;
      }
    }
    if (!found) {
      moveStatsList.add(new MoveStat(move));
    }
  }

  protected int occurrencesRate(List<MoveStat> moveStatsList, MoveEdge move) {
    int total = 0;
    int x = 0;
    for (MoveStat s : moveStatsList) {
      total += s.getOccurences();
      if (s.hasMove(move)) {
        x = s.getOccurences();
      }
    }
    if (total == 0) {
      return 0;
    }
    return x * 1000 / total;
  }

  /**
   *
   * @param moveStatsList the move statistics list.
   * @param move the move.
   */
  protected void incImprovements(List<MoveStat> moveStatsList, MoveEdge move) {
    for (MoveStat s : moveStatsList) {
      if (s.hasMove(move)) {
        s.incImprovements();
        break;
      }
    }
  }

  protected int improvementsRate(List<MoveStat> moveStatsList, MoveEdge move) {
    int total = 0;
    int x = 0;
    for (MoveStat s : moveStatsList) {
      total += s.getOccurences();
      if (s.hasMove(move)) {
        x = s.getImprovements();
      }
    }
    if (total == 0) {
      return 0;
    }
    return x * 1000 / total;
  }

  protected LabeledUndirectedGraph<N, E> applyMVCA() throws NotConnectedGraphException, Exception {
    MVCAO1 mvcao1 = new MVCAO1(graph);
    mvcao1.run();
    return mvcao1.getSpanningTree();
  }

  protected LabeledUndirectedGraph<N, E> applyERA(LabeledUndirectedGraph<N, E> sk) throws NotConnectedGraphException, Exception {
    ERA era = new ERA(sk);
    era.run();
    return era.getSpanningTree();
  }

  /**
   * Compute taboo search.
   *
   * @param s0 the initial solution.
   * @throws java.lang.Exception
   */
  protected void compute(final LabeledUndirectedGraph<N, E> s0) throws Exception {
    //Tabu queue
    final Queue<MoveEdge> tabuQueue = new LinkedList<>();

    //Long term memory
    final List<MoveStat> moveStatsList = new ArrayList<>();

    //Minimum graph for this start instance
    LabeledUndirectedGraph<N, E> localMinGraph = s0;

    //Iterations counters
    int iterationsWithoutImprovement = 0;
    int iterations = 0;

    //Minimum lists
    final List<LabeledUndirectedGraph<N, E>> localMinimumsList = new ArrayList<>();
    final List<LabeledUndirectedGraph<N, E>> candidateMinimumsList = new ArrayList<>();

    //Cost & minimum variables
    int cCost = s0.calculateCost();
    int km1Cost = cCost + 1, km2Cost;
    boolean inMinimum = false, bestUpdated = false;

    //Random iterations counter
    final int lRandomIters = ((randomIters == null) ? (maxIterations / 20) : randomIters),
            lNotRandomIters = ((notRandomIters == null) ? (lRandomIters * 2) : notRandomIters);
    int randomItersCount = 0, notRandomItersCount = 0;

    int equivalentMovesCount = 0;

    boolean abolishQueue = false;

    int risingCount = 0;

    //Start computation
    List<MoveEdge> ordered_neighborhood;
    LabeledUndirectedGraph<N, E> sk = new LabeledUndirectedGraph<>(s0);
    while (sk != null
            && iterations < maxIterations
            && iterationsWithoutImprovement < maxIterationsWithoutImprovement
            && localMinGraph.calculateCost() > minCost) {
      //Iterations increment
      iterations++;
      iterationsWithoutImprovement++;

      //Diversification: if you found a new minimum and you just updated best graph or computation exceeded max equivalent moves start to diversificate
      if (diversification && ((bestUpdated && inMinimum) || equivalentMovesCount > diversificationMaxEquivalentMoves)) {
        randomItersCount = lRandomIters;
      }

      //GET ALL NEIGHBORS
      ordered_neighborhood = getEdgeNeighborhood(sk);

      //Order neighbors by  improvement
      if (!diversification || (randomItersCount <= 0 || notRandomItersCount > 0)) {
        //If intensification learning or diversification learning is setted then weight cost with occurrences
        Collections.sort(ordered_neighborhood, (m1, m2) -> {
          int improvM1 = 0, improvM2 = 0, occurrM1 = 0, occurrM2 = 0;
          if (intensificationLearning) {
            improvM1 = improvementsRate(moveStatsList, m1);
            improvM2 = improvementsRate(moveStatsList, m2);
          }
          if (diversificationLearning) {
            occurrM1 = occurrencesRate(moveStatsList, m1);
            occurrM2 = occurrencesRate(moveStatsList, m2);
          }
          int costM1 = (((m1.afterCost - m1.beforeCost) * 1000 - improvM1 + occurrM1));
          int costM2 = (((m2.afterCost - m2.beforeCost) * 1000 - improvM2 + occurrM2));

          int r = (costM1 - costM2);
          return r;
        });

        notRandomItersCount--;
      } else {
        //To diversificate after a local minimum shuffle ordered neighbors
        Collections.shuffle(ordered_neighborhood, new Random(System.currentTimeMillis()));
        randomItersCount--;
        if (randomItersCount <= 0) {
          notRandomItersCount = lNotRandomIters;
        }
      }

      //Select a move that VIOLATE a tabu condition or SATISFY an aspiration condition
      MoveEdge move = null;
      for (MoveEdge m : ordered_neighborhood) {
        long c = tabuQueue.stream()
                .filter(_m -> (_m.getOut().equals(m.getIn()) || (moveDiversification && _m.getIn().equals(m.getOut()))))
                .count();

        if (((c <= 0 && (moveDiversification || m.afterCost != m.beforeCost)) || m.getGraph().calculateCost() < localMinGraph.calculateCost() || abolishQueue) && m.getGraph().isConnected()) {
          move = m;

          if (m.afterCost - m.beforeCost == 0) {
            equivalentMovesCount++;
            abolishQueue = false;
          } else {
            equivalentMovesCount = 0;
          }

          //INTENSIFICATION
          if (intensification) {
            abolishQueue = (((m.afterCost - m.beforeCost) < 0) && (risingCount + (((m.afterCost - m.beforeCost) < 0) ? 1 : 0) >= risingTrend));
          }

          break;
        }
      }

      if (move == null) {
        sk = null;
      } else {
        //Increment occurrences in move list
        incOccurrences(moveStatsList, move);

        LabeledUndirectedGraph<N, E> km1Sk = sk;
        sk = move.getGraph();

        km2Cost = km1Cost;
        km1Cost = cCost;
        cCost = sk.calculateCost();

        //If local search intensification is specified apply ERA local search to a solution that improve the previous one.
        if (cCost < km1Cost) {
          risingCount++;
        } else {
          risingCount = 0;
        }

        if (risingCount >= risingTrend && localSearchIntensification) {
          sk = applyERA(sk);
        }

        //If this solution improve the previous one then increment improvements
        if (cCost < km1Cost) {
          incImprovements(moveStatsList, move);
        }

        //Online search for elite solutions
        if (km2Cost > km1Cost && km1Cost < cCost) {
          localMinimumsList.add(km1Sk);
          inMinimum = false;
        } else if (!inMinimum && (km2Cost > km1Cost && km1Cost == cCost)) {
          inMinimum = true;
          candidateMinimumsList.add(km1Sk);
        } else if (inMinimum && (km1Cost == cCost)) {
          candidateMinimumsList.add(km1Sk);
        } else if (inMinimum && (km1Cost < cCost)) {
          inMinimum = false;
          candidateMinimumsList.add(km1Sk);
          localMinimumsList.addAll(candidateMinimumsList);
        } else {
          inMinimum = false;
          candidateMinimumsList.clear();
        }

        //Add move to tabu queue
        tabuQueue.add(move);

        //If this solution improve best solution for this instance then update.
        if (cCost < localMinGraph.calculateCost()) {
          localMinGraph = sk;
          iterationsWithoutImprovement = 0;
          bestUpdated = true;
        } else {
          //Remove older solution if tabu queue is longer than minQueue threshold.
          if (tabuQueue.size() > minQueue) {
            tabuQueue.poll();
          }
          bestUpdated = false;
        }
      }
    }

    localMinimumsList.addAll(candidateMinimumsList);

    //Update global elites and min graphs.
    synchronized (lock) {
      elites.addAll(localMinimumsList);
      minGraphs.add(localMinGraph);
    }
  }

  private volatile double prog = 0, tot = 0;

  /**
   * Compute initial solution via greedy algorithm.
   *
   * @throws Exception
   */
  @Override
  protected void start() throws Exception {
    LogManager.getLogger().log(ALGORITHM, "STARTING TABU SEARCH...");
    LogManager.getLogger().log(ALGORITHM, "MAX ITERATIONS: " + maxIterations);
    LogManager.getLogger().log(ALGORITHM, "MAX ITERATIONS WITHOUT IMPROVEMENTS: " + maxIterationsWithoutImprovement);
    LogManager.getLogger().log(ALGORITHM, "MIN QUEUE: " + minQueue);
    LogManager.getLogger().log(ALGORITHM, "RISING TREND: " + risingTrend);
    LogManager.getLogger().log(ALGORITHM, "MAX THREADS: " + (maxThreads != null ? maxThreads : "DEFAULT"));
    LogManager.getLogger().log(ALGORITHM, "MULTI-STARTS -> # OF GREEDY: " + greedyMultiStart + ", # OF RANDOM: " + randomMultiStart);

    String iSettings = null;
    if (intensificationLearning) {
      iSettings = "LEARNING";
    }
    if (localSearchIntensification) {
      if (iSettings != null) {
        iSettings += ", ";
      } else {
        iSettings = "";
      }
      iSettings += "LOCAL SEARCH";
    }
    if (pathRelinking) {
      if (iSettings != null) {
        iSettings += ", ";
      } else {
        iSettings = "";
      }
      iSettings += "PATH RELINKING";
    }
    if (intensification) {
      if (iSettings != null) {
        iSettings += ", ";
      } else {
        iSettings = "";
      }
      iSettings += "INTENSIFICATION";
    }
    if (iSettings == null) {
      iSettings = "NONE";
    }
    LogManager.getLogger().log(ALGORITHM, "INTENSIFICATION SETTINGS: " + iSettings);

    String dSettings = null;
    if (diversificationLearning) {
      dSettings = "LEARNING";
    }
    if (greedyMultiStart + randomMultiStart > 1) {
      if (dSettings != null) {
        dSettings += ", ";
      } else {
        dSettings = "";
      }
      dSettings += "MULTI-STARTS";
    }
    if (moveDiversification) {
      if (dSettings != null) {
        dSettings += ", ";
      } else {
        dSettings = "";
      }
      dSettings += "MOVE DIVERSIFY";
    }
    if (diversification) {
      if (dSettings != null) {
        dSettings += ", ";
      } else {
        dSettings = "";
      }
      dSettings += "DIVERSIFICATION";
    }
    if (dSettings == null) {
      dSettings = "NONE";
    }
    LogManager.getLogger().log(ALGORITHM, "DIVERSIFICATION SETTINGS: " + dSettings);

    LabeledUndirectedGraph<N, E> zero = getZeroGraph(minGraph);

    if (!zero.isConnected()) {
      minCost = zero.calculateCost() + 1;

      Queue<LabeledUndirectedGraph<N, E>> startsQueue = new LinkedList<>();
      for (int i = 0; i < greedyMultiStart; i++) {
        startsQueue.add(applyMVCA());
      }
      for (int i = 0; i < randomMultiStart; i++) {
        startsQueue.add(new LabeledUndirectedGraph<>(getSpanningTree(graph)));
      }
      int nthreads = Runtime.getRuntime().availableProcessors();
      if (maxThreads != null) {
        if (maxThreads <= 1) {
          nthreads = 1;
        } else {
          nthreads = maxThreads;
        }
      }
      nthreads = Integer.min(nthreads, (greedyMultiStart + randomMultiStart));

      LogManager.getLogger().log(ALGORITHM, "THREADS: " + nthreads);

      //PRINT PROGRESS
      prog = 0;
      tot = startsQueue.size();
      print(Ansi.ansi().cursor().save().erase().eraseLine().a(String.format("\t%.2f%%", prog)));

      if (nthreads == 1) {
        while (!startsQueue.isEmpty()) {
          compute(startsQueue.poll());

          //PRINT PROGRESS
//          prog = (tot - startsQueue.size()) * 100 / tot;
          prog = 100;
          print(Ansi.ansi().cursor().load().erase().eraseLine().a(String.format("\t%.2f%%", prog)));
        }
      } else {

        List<Thread> pool = new ArrayList<>();
        for (int i = 0; i < nthreads; i++) {
          Thread th = new Thread(() -> {
            try {
              boolean empty;
              synchronized (lock) {
                empty = startsQueue.isEmpty();
              }

              while (!empty) {
                LabeledUndirectedGraph<N, E> g;

                synchronized (lock) {
                  g = startsQueue.poll();
                }
                compute(g);
                synchronized (lock) {
                  empty = startsQueue.isEmpty();

                  //PRINT PROGRESS
//                  prog = (tot - startsQueue.size()) * 100 / tot;
                  prog++;
                  double _prog = this.prog * 100 / tot;
                  print(Ansi.ansi().cursor().load().erase().eraseLine().a(String.format("\t%.2f%%", _prog)));
                }
              }
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
          });
          th.setDaemon(false);
          th.setName("TabuSearch_Calc_" + i);
          th.setPriority(Thread.MAX_PRIORITY);
          th.start();
          pool.add(th);
        }
        for (Thread th : pool) {
          th.join();
        }
      }

      println();

      minGraphs.forEach(_min -> {
        if (_min.calculateCost() < minGraph.calculateCost()) {
          minGraph = _min;
        }
      });

      LogManager.getLogger().log(ALGORITHM, "END TABU SEARCH.");
      LogManager.getLogger().log(ALGORITHM, "BEST AT THIS POINT: " + minGraph.calculateCost());

      if (pathRelinking) {
        LogManager.getLogger().log(ALGORITHM, "STARTING PATH RELINKING...");

        pathRelinking(pathRelinkingAlt);

        println();
        LogManager.getLogger().log(ALGORITHM, "BEST AT THIS POINT: " + minGraph.calculateCost());
      }
    } else {
      minGraph = zero;
    }

    LogManager.getLogger().log(ALGORITHM, "END.");
  }

  protected class Best {

    private LabeledUndirectedGraph<N, E> graph;

    public Best(LabeledUndirectedGraph<N, E> graph) {
      this.graph = graph;
    }

    public void setGraph(LabeledUndirectedGraph<N, E> graph) {
      this.graph = graph;
    }

    public LabeledUndirectedGraph<N, E> getGraph() {
      return graph;
    }
  }

  protected LabeledUndirectedGraph<N, E> repair(final LabeledUndirectedGraph<N, E> g, final Set<String> label, final Set<E> allEdges) {
    final LabeledUndirectedGraph<N, E> cg = new LabeledUndirectedGraph<>(g);
    LabeledUndirectedGraph<N, E> best = cg;

    Set<String> local = new HashSet<>(label);
    for (String l : label) {
      local.remove(l);
      allEdges.stream()
              .filter(e -> e.getLabel().equals(l))
              .forEachOrdered(e -> cg.addEdge(e));
      if (!cg.isConnected()) {
        LabeledUndirectedGraph<N, E> t = repair(cg, local, allEdges);
        if (t.calculateCost() < best.calculateCost()) {
          best = t;
        }
      }
      local.add(l);
    }
    return best;
  }

  /**
   * Get the set of moves from a feasible solution (<code>sk</code> must be a
   * spanning tree)
   *
   * @param sk a spanning tree.
   * @param objective
   * @return the set of moves ordered by the delta of cost before and after this
   * move.
   */
  public List<MoveLabel> getLabelNeighborhood(final LabeledUndirectedGraph<N, E> sk, final LabeledUndirectedGraph<N, E> objective) {
    final List<MoveLabel> neighborood = new ArrayList<>();
    final LabeledUndirectedGraph<N, E> csk = new LabeledUndirectedGraph<>(sk);

    Set<String> diff = csk.getLabels();
    diff.removeAll(objective.getLabels());

    Set<E> allEdges = new HashSet<>(sk.getEdges());
    allEdges.addAll(sk.getRemovedEdges());

    diff.forEach(label -> {
      final LabeledUndirectedGraph<N, E> test = new LabeledUndirectedGraph<>(csk);
      test.removeLabel(label);

      //Add all edges for all active label in test graph
      test.getLabels().forEach(_label -> {
        allEdges.stream()
                .filter(_edge -> _edge.getLabel().equals(_label))
                .forEachOrdered(_edge -> test.addEdge(_edge));
      });

      LabeledUndirectedGraph<N, E> new_graph;
      if (!test.isConnected()) {
        Set<String> _labels = objective.getLabels();
        _labels.removeAll(test.getLabels());
        _labels.remove(label);
        new_graph = repair(test, _labels, allEdges);
      } else {
        new_graph = test;
      }

      if (new_graph.isConnected()) {
        final LabeledUndirectedGraph<N, E> stest = getSpanningTree(new_graph);
        Set<E> out = stest.getRemovedEdges(), in = stest.getEdges();
        out.removeAll(csk.getRemovedEdges());
        in.removeAll(csk.getEdges());
        neighborood.add(new MoveLabel(out, in, sk));
      }
    });

    return neighborood;
  }

  /**
   * Get the set of moves from a feasible solution (<code>sk</code> must be a
   * spanning tree)
   *
   * @param sk a spanning tree.
   * @param objective
   * @return the set of moves ordered by the delta of cost before and after this
   * move.
   */
  public List<MoveLabel> getSimpleLabelNeighborhood(final LabeledUndirectedGraph<N, E> sk, final LabeledUndirectedGraph<N, E> objective) {
    final List<MoveLabel> neighborood = new ArrayList<>();
    final LabeledUndirectedGraph<N, E> csk = new LabeledUndirectedGraph<>(sk);

    Set<String> diff = csk.getLabels();
    diff.removeAll(objective.getLabels());

    Set<E> allEdges = new HashSet<>(sk.getEdges());
    allEdges.addAll(sk.getRemovedEdges());

    diff.forEach(label -> {
      final LabeledUndirectedGraph<N, E> test = new LabeledUndirectedGraph<>(csk);

      //Remove label
      test.removeLabel(label);

      //Add all edges for all active label in test graph
      test.getLabels().forEach(_label -> {
        allEdges.stream()
                .filter(_edge -> _edge.getLabel().equals(_label))
                .forEachOrdered(_edge -> test.addEdge(_edge));
      });

      //Select all labels of objective graph that are different from removed label
      Set<String> _labels = objective.getLabels();
      _labels.remove(label);

      _labels.forEach(_label -> {
        //Add label from candidates label in objective graph
        allEdges.stream()
                .filter(edge -> edge.getLabel().endsWith(_label))
                .forEachOrdered(edge -> test.addEdge(edge));

        final LabeledUndirectedGraph<N, E> stest;
        if (test.isConnected()) {
          stest = getSpanningTree(test);
        } else {
          stest = test;
        }
        Set<E> out = stest.getRemovedEdges(), in = stest.getEdges();
        out.removeAll(csk.getRemovedEdges());
        in.removeAll(csk.getEdges());
        neighborood.add(new MoveLabel(out, in, csk));

        test.removeLabel(_label);
      });
    });

    if (neighborood.isEmpty() && !csk.isConnected()) {
      diff = objective.getLabels();
      diff.removeAll(csk.getLabels());
      diff.forEach(label -> {
        Set<E> in = new HashSet<>();
        allEdges.stream()
                .filter(edge -> edge.getLabel().equals(label))
                .forEachOrdered(edge -> in.add(edge));
        neighborood.add(new MoveLabel(new HashSet<>(), in, csk));
      });
    }

    return neighborood;
  }

  /**
   * Path relinking procedure.
   *
   * @param alt alternative mode.
   */
  protected void pathRelinking(boolean alt) {
    if (!pathRelinkingDesc) {
      Collections.shuffle(elites, new Random(System.currentTimeMillis()));
    } else {
      Collections.sort(elites, (e1, e2) -> {
        return e2.calculateCost() - e1.calculateCost();
      });
    }

    Queue<LabeledUndirectedGraph<N, E>> elitesQueue = new LinkedList<>(elites);

    LogManager.getLogger().log(ALGORITHM, "PATH RELINKING WITH " + elites.size() + " ELITES GRAPHS...");
    if (alt) {
      LogManager.getLogger().log(ALGORITHM, "YOU CHOOSE ALTERNATIVE ALGORITHM");
    }
    if (pathRelinkingMin) {
      LogManager.getLogger().log(ALGORITHM, "YOU CHOOSE MIN OPTION");
    }

    if (elitesQueue.isEmpty()) {
      return;
    }

    LabeledUndirectedGraph<N, E> start, end = new LabeledUndirectedGraph<>(elitesQueue.poll());

    prog = 0;
    tot = elitesQueue.size();
    print(Ansi.ansi().cursor().save().erase().eraseLine().a(String.format("\t%.2f%%", prog)));

    while (!elitesQueue.isEmpty()) {
      start = new LabeledUndirectedGraph<>(end);
      end = elitesQueue.poll();

      LabeledUndirectedGraph<N, E> elite1, elite2;
      if (pathRelinkingMin) {
        if (start.calculateCost() < end.calculateCost()) {
          elite1 = new LabeledUndirectedGraph<>(start);
          elite2 = new LabeledUndirectedGraph<>(end);
        } else {
          elite1 = new LabeledUndirectedGraph<>(end);
          elite2 = new LabeledUndirectedGraph<>(start);
        }
      } else {
        elite1 = new LabeledUndirectedGraph<>(start);
        elite2 = new LabeledUndirectedGraph<>(end);
      }

      while (!elite1.getLabels().equals(elite2.getLabels())) {
        List<MoveLabel> neighborhood = (alt ? getSimpleLabelNeighborhood(elite1, elite2) : getLabelNeighborhood(elite1, elite2));
        if (alt) {
          Collections.sort(neighborhood, (m1, m2) -> {
            int d;
            int m1Delta = m1.afterCost - m1.beforeCost;
            int m2Delta = m2.afterCost - m2.beforeCost;
            if (!m1.getGraph().isConnected() && m2.getGraph().isConnected()) {
              return Integer.MAX_VALUE;
            } else if (m1.getGraph().isConnected() && !m2.getGraph().isConnected()) {
              return Integer.MIN_VALUE;
            }
            d = (m1Delta) - (m2Delta);
            return d;
          });
        } else {
          Collections.sort(neighborhood);
        }

        if (neighborhood.isEmpty()) {
          break;
        }
        elite1 = neighborhood.get(0).getGraph();
        if (elite1.calculateCost() < minGraph.calculateCost() && elite1.isConnected()) {
          minGraph = elite1;
        }
      }

      //PRINT PROGRESS
      prog = (tot - elitesQueue.size()) * 100 / tot;
      print(Ansi.ansi().cursor().load().erase().eraseLine().a(String.format("\t%.2f%%", prog)));
    }

  }

  public int getMaxIterations() {
    return maxIterations;
  }

  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }

  public int getMaxIterationsWithoutImprovement() {
    return maxIterationsWithoutImprovement;
  }

  public void setMaxIterationsWithoutImprovement(int maxIterationsWithoutImprovements) {
    this.maxIterationsWithoutImprovement = maxIterationsWithoutImprovements;
  }

  public int getMinQueue() {
    return minQueue;
  }

  public void setMinQueue(int minQueue) {
    this.minQueue = minQueue;
  }

  public int getGreedyMultiStart() {
    return greedyMultiStart;
  }

  public void setGreedyMultiStart(int greedyMultiStart) {
    if (greedyMultiStart >= 0) {
      this.greedyMultiStart = greedyMultiStart;
    }
  }

  public int getRandomMultiStart() {
    return randomMultiStart;
  }

  public void setRandomMultiStart(int randomMultiStart) {
    if (randomMultiStart >= 0) {
      this.randomMultiStart = randomMultiStart;
    }
  }

  public boolean isPathRelinking() {
    return pathRelinking;
  }

  public void setPathRelinking(boolean pathRelinking) {
    this.pathRelinking = pathRelinking;
  }

  public boolean isLocalSearchIntensification() {
    return localSearchIntensification;
  }

  public void setLocalSearchIntensification(boolean localSearchIntensification) {
    this.localSearchIntensification = localSearchIntensification;
  }

  public boolean isMoveDiversification() {
    return moveDiversification;
  }

  public void setMoveDiversification(boolean moveDiversification) {
    this.moveDiversification = moveDiversification;
  }

  public boolean isIntensificationLearning() {
    return intensificationLearning;
  }

  public void setIntensificationLearning(boolean intensificationLearning) {
    this.intensificationLearning = intensificationLearning;
  }

  public boolean isDiversificationLearning() {
    return diversificationLearning;
  }

  public void setDiversificationLearning(boolean diversificationLearning) {
    this.diversificationLearning = diversificationLearning;
  }

  public boolean isPathRelinkingAlt() {
    return pathRelinkingAlt;
  }

  public void setPathRelinkingAlt(boolean pathRelinkingAlt) {
    this.pathRelinkingAlt = pathRelinkingAlt;
  }

  public boolean isPathRelinkingMin() {
    return pathRelinkingMin;
  }

  public void setPathRelinkingMin(boolean pathRelinkingMin) {
    this.pathRelinkingMin = pathRelinkingMin;
  }

  public boolean isIntensification() {
    return intensification;
  }

  public void setIntensification(boolean intensification) {
    this.intensification = intensification;
  }

  public boolean isDiversification() {
    return diversification;
  }

  public void setDiversification(boolean diversification) {
    this.diversification = diversification;
  }

  public int getDiversificationMaxEquivalentMoves() {
    return diversificationMaxEquivalentMoves;
  }

  public void setDiversificationMaxEquivalentMoves(int diversificationMaxEquivalentMoves) {
    this.diversificationMaxEquivalentMoves = diversificationMaxEquivalentMoves;
  }

  public Integer getRandomIters() {
    return randomIters;
  }

  public void setRandomIters(Integer randomIters) {
    this.randomIters = randomIters;
  }

  public Integer getNotRandomIters() {
    return notRandomIters;
  }

  public void setNotRandomIters(Integer notRandomIters) {
    this.notRandomIters = notRandomIters;
  }

  public boolean isPathRelinkingDesc() {
    return pathRelinkingDesc;
  }

  public void setPathRelinkingDesc(boolean pathRelinkingDesc) {
    this.pathRelinkingDesc = pathRelinkingDesc;
  }

  public int getRisingTrend() {
    return risingTrend;
  }

  public void setRisingTrend(int risingTrend) {
    this.risingTrend = risingTrend;
  }
}
