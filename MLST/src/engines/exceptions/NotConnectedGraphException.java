/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package engines.exceptions;

/**
 *
 * @author Niccolò Ferrari
 */
public class NotConnectedGraphException extends Exception {

  /**
   * Creates a new instance of <code>NotConnectedGraphException</code> without
   * detail message.
   */
  public NotConnectedGraphException() {
  }

  /**
   * Constructs an instance of <code>NotConnectedGraphException</code> with the
   * specified detail message.
   *
   * @param msg the detail message.
   */
  public NotConnectedGraphException(String msg) {
    super(msg);
  }
}
