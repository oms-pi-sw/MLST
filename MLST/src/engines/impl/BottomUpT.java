/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.impl;

import engines.exceptions.NotConnectedGraphException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 * @param <E>
 */
public class BottomUpT<N extends Node, E extends Edge<N>> extends BottomUp<N, E> {

  private final Object lock = new Object();

  public BottomUpT(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
  }

  @Override
  public void start() throws InterruptedException, Exception {
    try {
      LabeledUndirectedGraph<N, E> test = new LabeledUndirectedGraph<>(minGraph);

      LabeledUndirectedGraph<N, E> zero = getZeroGraph(test);

      int nthreads = getMaxThreads() == null ? Runtime.getRuntime().availableProcessors() : getMaxThreads();
      List<Thread> pool = new ArrayList<>();

      Queue<LabeledUndirectedGraph<N, E>> graphs = new LinkedList<>();

      zero.getRemovedLabels().forEach(label -> {
        LabeledUndirectedGraph<N, E> g = new LabeledUndirectedGraph<>(zero);
        g.getRemovedEdges().stream()
                .filter(edge -> edge.getLabel().equals(label))
                .forEachOrdered(edge -> g.addEdge(edge));
        graphs.add(g);
      });

      for (int i = 0; i < nthreads; i++) {
        Thread th = new Thread(() -> {
          boolean empty;
          synchronized (lock) {
            empty = graphs.isEmpty();
          }

          while (!empty) {
            LabeledUndirectedGraph<N, E> g;

            synchronized (lock) {
              g = graphs.poll();
            }
            if (g == null) {
              break;
            }
            compute(g);

            synchronized (lock) {
              empty = graphs.isEmpty();
            }
          }
        });
        th.setDaemon(false);
        th.setName("RAlg1Opt1_Calc_" + i);
        th.setPriority(Thread.MAX_PRIORITY);
        th.start();
        pool.add(th);
      }
      for (Thread th : pool) {
        th.join();
      }
    } catch (InterruptedException ex) {
      throw ex;
    }
  }

}
