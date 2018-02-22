/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.impl;

import engines.Algorithm;
import engines.exceptions.NotConnectedGraphException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;

/**
 * Tabu Search algorithm class.
 *
 * @author Niccolò Ferrari
 * @param <N> The type of Node.
 * @param <E> The type of Edge.
 */
public class TabuSearch<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  private volatile int minCost = 1,
          maxIterations = 100,
          maxIterationsWithoutImprovement = 50,
          minQueue = 10;

  private final List<LabeledUndirectedGraph<N, E>> minGraphs = new ArrayList<>();
  private final Object lock = new Object();

  private final List<LabeledUndirectedGraph<N, E>> elites = new ArrayList<>();

  // Intensifications options
  private volatile boolean pathRelinking = false,
          localSearchIntensification = false,
          intensificationLearning = false;

  // Diversifications options
  private volatile int greedyMultiStart = 1, randomMultiStart = 0;
  private volatile boolean moveDiversification = false,
          diversificationLearning = false;

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
  protected class Move implements Comparable<Move> {

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
    public Move(E out, E in, LabeledUndirectedGraph<N, E> graph) {
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
      final Move other = (Move) obj;
      if (!moveDiversification) {
        return (Objects.equals(in, other.out));
      } else {
        return (Objects.equals(in, other.out) || Objects.equals(out, other.in));
      }
    }

    @Override
    public int compareTo(Move o) {
      return afterCost - beforeCost;
    }

    @Override
    public String toString() {
      return "Move{" + "out=" + out + ", in=" + in + ", afterCost=" + afterCost + ", beforeCost=" + beforeCost + '}';
    }

    /**
     * Check if this move is identical to another.
     *
     * @param other the other move.
     * @return true if the other move is identical to this.
     */
    public boolean isSame(Move other) {
      if (other == this) {
        return true;
      }
      return Objects.equals(in, other.in) && Objects.equals(out, other.out);
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
  public SortedSet<Move> getEdgeNeighborhood(final LabeledUndirectedGraph<N, E> sk) {
    final SortedSet<Move> neighborood = new TreeSet<>();
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
        candidates.forEach(candidate -> neighborood.add(new Move(edge, candidate, sk)));
      }
      csk.addEdge(edge);
    });
    return neighborood;
  }

  /**
   * Class that stores move statistics, used to implement some intensifications
   * and diversifications techniques.
   */
  protected class MoveStat {

    private final Move move;
    private int occurences = 1;
    private int improvements = 0;

    /**
     * Constructor.
     *
     * @param move the move.
     */
    public MoveStat(Move move) {
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

    public Move getMove() {
      return move;
    }

    public boolean hasMove(Move move) {
      return this.move.isSame(move);
    }
  }

  /**
   * Get statistics for a given move.
   *
   * @param moveStatsList the move statistics list.
   * @param move the move.
   * @return the move statistics related to given move.
   */
  protected MoveStat getStat(List<MoveStat> moveStatsList, Move move) {
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
  protected void incOccurrences(List<MoveStat> moveStatsList, Move move) {
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

  protected double occurrencesRate(List<MoveStat> moveStatsList, Move move) {
    double total = 0;
    double x = 0;
    for (MoveStat s : moveStatsList) {
      total += s.getOccurences();
      if (s.hasMove(move)) {
        x = s.getOccurences();
      }
    }
    if (total == 0) {
      return 0;
    }
    return x / total;
  }

  /**
   *
   * @param moveStatsList the move statistics list.
   * @param move the move.
   */
  protected void incImprovements(List<MoveStat> moveStatsList, Move move) {
    for (MoveStat s : moveStatsList) {
      if (s.hasMove(move)) {
        s.incImprovements();
        break;
      }
    }
  }

  protected double improvementsRate(List<MoveStat> moveStatsList, Move move) {
    double total = 0;
    double x = 0;
    for (MoveStat s : moveStatsList) {
      if (s.hasMove(move)) {
        total = s.getOccurences();
        x = s.getOccurences();
      }
    }
    if (total == 0) {
      return 0;
    }
    return x / total;
  }

  /**
   * Compute taboo search.
   *
   * @param s0 the initial solution.
   */
  protected void compute(final LabeledUndirectedGraph<N, E> s0) {
    final Queue<Move> tabuQueue = new LinkedList<>();

    final List<MoveStat> moveStatsList = new ArrayList<>();

    LabeledUndirectedGraph<N, E> localMinGraph = s0;

    int iterationsWithoutImprovement = 0;
    int iterations = 0;

    final List<LabeledUndirectedGraph<N, E>> localMinimumsList = new ArrayList<>();
    final List<LabeledUndirectedGraph<N, E>> candidateMinimumsList = new ArrayList<>();

    int cCost = s0.calculateCost();
    int km1Cost = cCost + 1, km2Cost;
    boolean inMinimum = false;

    SortedSet<Move> sk_neighborhood;
    LabeledUndirectedGraph<N, E> sk = new LabeledUndirectedGraph<>(s0);
    while (sk != null
            && iterations < maxIterations
            && iterationsWithoutImprovement < maxIterationsWithoutImprovement
            && localMinGraph.calculateCost() > minCost) {
      iterations++;
      iterationsWithoutImprovement++;

      sk_neighborhood = getEdgeNeighborhood(sk);

      List<Move> ordered_neighborhood = new ArrayList<>(sk_neighborhood);
      Collections.sort(ordered_neighborhood, (m1, m2) -> {
        double improv = 0, occurr = 0;
        if (intensificationLearning) {
          improv = improvementsRate(moveStatsList, m1);
        }
        if (diversificationLearning) {
          occurr = occurrencesRate(moveStatsList, m1);
        }
        int r = (int) (((m1.afterCost - m1.beforeCost) - improv + occurr) * 1000);
        return r;
      });

      Move move = null;
      for (Move m : ordered_neighborhood) {
        if ((!tabuQueue.contains(m) || m.getGraph().calculateCost() < localMinGraph.calculateCost()) && m.getGraph().isConnected()) {
          move = m;
//          System.out.println("out=" + move.out + " in=" + move.in);
          break;
        }
      }
      if (move == null) {
        sk = null;
      } else {
        incOccurrences(moveStatsList, move);

        LabeledUndirectedGraph<N, E> km1Sk = sk;
        sk = move.getGraph();

        km2Cost = km1Cost;
        km1Cost = cCost;
        cCost = sk.calculateCost();

        if (cCost < km1Cost) {
          incImprovements(moveStatsList, move);
        }

//        Set<E> edges = km1Sk.getEdges();
//        edges.removeAll(sk.getEdges());
//        System.out.println("[(k-1)-k] diff=" + edges);
//        edges = sk.getEdges();
//        edges.removeAll(km1Sk.getEdges());
//        System.out.println("[k-(k-1)] diff=" + edges);
        if (km2Cost > km1Cost && km1Cost < cCost) {
          localMinimumsList.add(km1Sk);
          inMinimum = false;
//          System.out.println("MINIMUM " + km1Sk.calculateCost());
//
//          int k1 = localMinimum.size() - 2;
//          int k = localMinimum.size() - 1;
//          if (k - 2 >= 0) {
//            edges = localMinimum.get(k1).getEdges();
//            edges.removeAll(localMinimum.get(k).getEdges());
//            System.out.println("[(m-1)-m] diff=" + edges);
//            edges = localMinimum.get(k).getEdges();
//            edges.removeAll(localMinimum.get(k1).getEdges());
//            System.out.println("[m-(m-1)] diff=" + edges);
//          }
        } else if (!inMinimum && (km2Cost > km1Cost && km1Cost == cCost)) {
          inMinimum = true;
          candidateMinimumsList.add(km1Sk);
//          System.out.println("ADD CANDIDATE");
        } else if (inMinimum && (km1Cost == cCost)) {
          candidateMinimumsList.add(km1Sk);
//          System.out.println("ADD CANDIDATE");
        } else if (inMinimum && (km1Cost < cCost)) {
          inMinimum = false;
          candidateMinimumsList.add(km1Sk);
          localMinimumsList.addAll(candidateMinimumsList);
//          System.out.println("ALL MINIMUM");
        } else {
          inMinimum = false;
          candidateMinimumsList.clear();
        }

//        System.out.println("cCost=" + cCost);
        tabuQueue.add(move);
//        System.out.println("added");
//        System.out.println();

        if (cCost < localMinGraph.calculateCost()) {
          localMinGraph = sk;
          iterationsWithoutImprovement = 0;
        } else {
          if (tabuQueue.size() > minQueue) {
            tabuQueue.poll();
          }
        }
      }
//      System.out.println("CURRENT THREAD=" + Thread.currentThread().getName() + ";\t" + iterations + " : " + iterationsWithoutImprovement + "; tabuQueue=" + tabuQueue.size());
    }
//    System.out.println();
//    LabeledUndirectedGraph<N, E> last = null;
//    for (LabeledUndirectedGraph<N, E> min : localMinimum) {
//      if (last != null) {
//        Set<E> edges = last.getEdges();
//        edges.removeAll(min.getEdges());
//        System.out.println("[last-min] diff=" + edges);
//        edges = min.getEdges();
//        edges.removeAll(last.getEdges());
//        System.out.println("[min-last] diff=" + edges);
//      }
//      System.out.println("min=" + min.calculateCost());
//      last = min;
//    }
    synchronized (lock) {
      elites.addAll(localMinimumsList);
      minGraphs.add(localMinGraph);
    }
  }

  /**
   * Compute initial solution via greedy algorithm.
   *
   * @throws Exception
   */
  @Override
  protected void start() throws Exception {
    System.out.println("MINQUEUE=" + minQueue + "; MAXITER=" + maxIterations + "; MAXITERATIONSWITHOUTIMPROVEMENT=" + maxIterationsWithoutImprovement);
    LabeledUndirectedGraph<N, E> zero;
    if (!(zero = getZeroGraph(minGraph)).isConnected()) {
      minCost = zero.calculateCost() + 1;

      Queue<LabeledUndirectedGraph<N, E>> startsQueue = new LinkedList<>();
      for (int i = 0; i < greedyMultiStart; i++) {
        MVCAO1 mvcao1 = new MVCAO1(graph);
        mvcao1.run();
        startsQueue.add(mvcao1.getSpanningTree());
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
      System.out.println("# OF THREADS=" + nthreads);
      System.out.println("# STARTS=" + startsQueue.size());
      System.out.println("# GREEDY=" + greedyMultiStart + " # RANDOM=" + randomMultiStart);
      startsQueue.forEach(_g -> System.out.println("\t# COST=" + _g.calculateCost()));
      if (nthreads == 1) {
        while (!startsQueue.isEmpty()) {
          compute(startsQueue.poll());
        }
      } else {
        List<Thread> pool = new ArrayList<>();
        for (int i = 0; i < nthreads; i++) {
          Thread th = new Thread(() -> {
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
              }
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
      System.out.println("# RESULTS=" + minGraphs.size());
      minGraphs.forEach(_g -> System.out.println("\t# COST=" + _g.calculateCost()));
      minGraphs.forEach(_min -> {
        if (_min.calculateCost() < minGraph.calculateCost()) {
          minGraph = _min;
        }
      });
    } else {
      minGraph = zero;
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
}
