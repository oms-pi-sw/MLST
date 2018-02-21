/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.impl;

import engines.Algorithm;
import engines.exceptions.NotConnectedGraphException;
import java.util.ArrayList;
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
 *
 * @author Niccolò Ferrari
 * @param <N>
 * @param <E>
 */
public class TabuSearch<N extends Node, E extends Edge<N>> extends Algorithm<N, E> {

  private int minCost = 1, maxIterations = 100, maxIterationsWithoutImprovement = 50,
          minQueue = 10, multiStart = 1;

  public TabuSearch(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
  }

  protected class Move implements Comparable<Move> {

    private final E out, in;
    private final LabeledUndirectedGraph<N, E> graph;
    private final int afterCost, beforeCost;

    public Move(E out, E in, LabeledUndirectedGraph<N, E> graph) {
      this.out = out;
      this.in = in;
      this.graph = new LabeledUndirectedGraph<>(graph);
      beforeCost = this.graph.calculateCost();
      this.graph.removeEdge(out);
      this.graph.addEdge(in);
      afterCost = this.graph.calculateCost();
    }

    public E getOut() {
      return out;
    }

    public E getIn() {
      return in;
    }

    public LabeledUndirectedGraph<N, E> getGraph() {
      return graph;
    }

    public int getAfterCost() {
      return afterCost;
    }

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
      return (Objects.equals(this.out, other.out) && Objects.equals(this.in, other.in));
    }

    @Override
    public int compareTo(Move o) {
      return afterCost - beforeCost;
    }

    @Override
    public String toString() {
      return "Move{" + "out=" + out + ", in=" + in + ", afterCost=" + afterCost + ", beforeCost=" + beforeCost + '}';
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
   * Compute taboo search.
   *
   * @param s0 the initial solution.
   */
  protected void compute(final LabeledUndirectedGraph<N, E> s0) {
    final Queue<Move> tabuQueue = new LinkedList<>();

    LabeledUndirectedGraph<N, E> localMinGraph = s0;

    int iterationsWithoutImprovement = 0;
    int iterations = 0;

    final List<LabeledUndirectedGraph<N, E>> localMinimum = new ArrayList<>();
    localMinimum.add(s0);

    SortedSet<Move> sk_neighborhood;
    LabeledUndirectedGraph<N, E> sk = new LabeledUndirectedGraph<>(s0);
    while (sk != null
            && iterations < maxIterations
            && iterationsWithoutImprovement < maxIterationsWithoutImprovement
            && localMinGraph.calculateCost() > minCost) {
      iterations++;
      iterationsWithoutImprovement++;

      sk_neighborhood = getEdgeNeighborhood(sk);

      Move move = null;
      for (Move m : sk_neighborhood) {
        if ((!tabuQueue.contains(m) || m.getGraph().calculateCost() < localMinGraph.calculateCost()) && m.getGraph().isConnected()) {
          move = m;
          break;
        }
      }
      if (move == null) {
        sk = null;
      } else {
        sk = move.getGraph();
        tabuQueue.add(move);
        if (sk.calculateCost() < localMinGraph.calculateCost()) {
          localMinGraph = sk;
          iterationsWithoutImprovement = 0;
        } else {
          if (tabuQueue.size() > minQueue) {
            tabuQueue.poll();
          }
        }
      }
//      System.out.println(iterations + " : " + iterationsWithoutImprovement + "; tabuQueue=" + tabuQueue.size());
    }
    minGraph = localMinGraph;
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
      MVCAO1 mvcao1 = new MVCAO1(graph);
      mvcao1.run();
      minGraph = mvcao1.getSpanningTree();
      compute(minGraph);
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

  public int getMultiStart() {
    return multiStart;
  }

  public void setMultiStart(int multiStart) {
    this.multiStart = multiStart;
  }
}
