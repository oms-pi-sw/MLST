/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlst.struct;

import mlst.struct.exceptions.LoopEdgeException;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 */
public class Edge<N extends Node> {

  private final String label;
  private final N node1, node2;

  public Edge(String label, N start, N end) throws LoopEdgeException {
    if (start.equals(end)) {
      throw new LoopEdgeException("Loops not supported.");
    }
    this.label = label;
    this.node1 = start;
    this.node2 = end;
  }

  public String getLabel() {
    return label;
  }

  public N getNode1() {
    return node1;
  }

  public N getNode2() {
    return node2;
  }

  public boolean has(N node) {
    return node1.equals(node) || node2.equals(node);
  }

  public N other(N node) {
    if (node1.equals(node)) {
      return node2;
    } else if (node2.equals(node)) {
      return node1;
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return "(" + label + ", " + node1 + ", " + node2 + ')';
  }

}
