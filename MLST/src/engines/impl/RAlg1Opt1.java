/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.impl;

import engines.exceptions.NotConnectedGraphException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import mlst.struct.Edge;
import mlst.struct.LabeledUndirectedGraph;
import mlst.struct.Node;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 * @param <E>
 */
public class RAlg1Opt1<N extends Node, E extends Edge<N>> extends RAlg1<N, E> {

  private final Object lock = new Object();

  public RAlg1Opt1(LabeledUndirectedGraph<N, E> graph) throws NotConnectedGraphException {
    super(graph);
  }

  @Override
  public void start() throws InterruptedException, Exception {
    try {
      Set<String> nlabels = new HashSet<>();
      LabeledUndirectedGraph<N, E> test = new LabeledUndirectedGraph<>(graph);
      test.getLabels().forEach(label -> {
        Set<E> ledges = test.getEdges().stream().filter(edge -> edge.getLabel().equals(label)).collect(Collectors.toSet());
        for (E edge : ledges) {
          test.removeEdge(edge);
          if (!test.isConnected()) {
            nlabels.add(label);
            break;
          }
          test.addEdge(edge);
        }
      });
      LabeledUndirectedGraph<N, E> zero = new LabeledUndirectedGraph<>(graph);
      zero.getEdges().forEach(edge -> {
        if (!nlabels.contains(edge.getLabel())) {
          zero.removeEdge(edge);
        }
      });

      int nthreads = Runtime.getRuntime().availableProcessors();
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
