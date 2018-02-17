/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlst.struct;

import java.util.Objects;
import mlst.struct.exceptions.LoopEdgeException;

/**
 *
 * @author Niccolò Ferrari
 * @param <N>
 */
public class SimpleEdge<N extends Node> extends Edge<N> {

  public SimpleEdge(String label, N start, N end) throws LoopEdgeException {
    super(label, start, end);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (!o.getClass().equals(this.getClass())) {
      return false;
    }
    return hashCode() == o.hashCode();
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + Objects.hashCode(getLabel());
    hash = 31 * hash + Objects.hashCode(getNode1());
    hash = 31 * hash + Objects.hashCode(getNode2());
    return hash;
  }

}
