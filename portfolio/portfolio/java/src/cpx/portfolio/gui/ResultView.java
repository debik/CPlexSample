package cpx.portfolio.gui;

import java.util.Date;

import javax.swing.JComponent;

import com.platform.symphony.soam.SoamException;

/** Interface implemented by widgets that show the results of a task.
 * The functions in this interface are guaranteed to be only invoked
 * in the event dispatcher thread.
 */
public interface ResultView {
  /** Invoked when the GUI polls for the associated task.
   * @param date The current date and time.
   */
  public void setLastPoll(Date date);
  /** Invoked when the GUI detects that the associated task failed. 
   * @param exception The exception with which the task failed.
   */
  public void taskFailed(SoamException exception);
  /** Invoked when a problem occurs while interacting with the associated task.
   * @param exception The exception that occurred.
   */
  public void setException(Exception exception);
  
  public interface CloseListener {
    public void resultViewClosed(JComponent view);
  }
  
  /** Add a listener that is notified when this view is deleted. */
  public void addCloseListener(CloseListener closeListener);
  /** Remove a listener that was added by {@link #addCloseListener(CloseListener)} */
  public void removeCloseListener(CloseListener closeListener);
}
