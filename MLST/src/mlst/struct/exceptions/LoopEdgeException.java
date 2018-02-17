/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mlst.struct.exceptions;

/**
 *
 * @author Niccolò Ferrari
 */
public class LoopEdgeException extends Exception {

  /**
   * Creates a new instance of <code>LoopEdgeException</code> without detail
   * message.
   */
  public LoopEdgeException() {
  }

  /**
   * Constructs an instance of <code>LoopEdgeException</code> with the specified
   * detail message.
   *
   * @param msg the detail message.
   */
  public LoopEdgeException(String msg) {
    super(msg);
  }
}
