package cpx.portfolio.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

import com.platform.symphony.soam.SoamException;

import cpx.portfolio.data.Investment;

/** Widget to visualize the results from a single optimization run.
 * Before results are available, the widget shows a simple label that displays when
 * we last polled for results.
 * Once results are available the results are shown in a table.  
 */
public class RunResultView extends JPanel implements ResultView {
  
  private static final long serialVersionUID = 1;
  
  /** When the corresponding run was started. */
  private final Date start;
  private JPanel results = new JPanel();
  private JTextArea pollLabel = new JTextArea(5, 80);
  private JComponent table = new JScrollPane(pollLabel);
  private Collection<CloseListener> clearListeners = new Vector<CloseListener>();
  
  public RunResultView(Date start, double wealth, double rho) {
    this.start = start;
    pollLabel.setText("Started at " + start);
    setLayout(new BorderLayout(5, 5));
    
    final JPanel top = new JPanel();
    top.setLayout(new GridBagLayout());
    final Insets inset = new Insets(2, 2, 2, 2);
    top.add(new JLabel("Wealth"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
    top.add(new JLabel("" + wealth), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
    top.add(new JLabel("Rho"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
    top.add(new JLabel("" + rho), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
    add(top, BorderLayout.NORTH);
    
    add(table, BorderLayout.CENTER);
    
    add(results, BorderLayout.SOUTH);
  }
  
  @Override
  public void addCloseListener(CloseListener closeListener) { clearListeners.add(closeListener); }
  @Override
  public void removeCloseListener(CloseListener closeListener) { clearListeners.remove(closeListener); }
  
  /** Set the results to be displayed in this instance.
   * @param investments
   * @param totalReturn
   * @param totalVariance
   */
  public void setResults(Collection<Investment> investments, double totalReturn, double totalVariance) {
    // 	pollLabel = null;
    results.setVisible(false);
    remove(results);
    table.setVisible(false);
    remove(table);
    
    final Investment[] array = investments.toArray(new Investment[investments.size()]);
    
    table = new JScrollPane(new JTable(new AbstractTableModel() {
      private static final long serialVersionUID = 1;
      @Override
      public Object getValueAt(int row, int column) {
        switch (column) {
        case 0: return array[row].getName();
        case 1: return String.format("%.3f", array[row].getAllocation());
        default: return null;
        }
      }
      
      @Override
      public int getRowCount() { return array.length; }
      
      @Override
      public int getColumnCount() { return 2; }

      @Override
      public String getColumnName(int column) {
        switch (column) {
        case 0: return "Investment";
        case 1: return "Allocation";
        default: return null;
        }
      }
      
    }));
    table.setVisible(true);
    add(table, BorderLayout.CENTER);
    
    
    results = new JPanel();
    results.setLayout(new GridBagLayout());
    final Insets inset = new Insets(2, 2, 2, 2);
    results.add(new JLabel("Total return"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
    results.add(new JLabel("" + totalReturn), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
    results.add(new JLabel("Total variance"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
    results.add(new JLabel("" + totalVariance), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
    results.add(new JButton(new AbstractAction("Close") {
      private static final long serialVersionUID = 1;      
      @Override
      public void actionPerformed(ActionEvent e) {
        for (final CloseListener closeListener : clearListeners)
          closeListener.resultViewClosed(RunResultView.this);
        
      }
    }), new GridBagConstraints(2, 0, 1, 2, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, inset, 0, 0));
    results.setVisible(true);
    add(results, BorderLayout.SOUTH);
  }
  
  /** Update the poll message displayed in this instance.
   * Updates the message so that is shows <code>pollDate</code> as the date at
   * which we last polled PlatformSymphony for this job.
   * @param pollDate Last date at which we polled PlatformSymphony.
   */
  @Override
  public void setLastPoll(Date pollDate) {
    if (pollLabel != null)
      pollLabel.setText("Started at " + start + "\nLast polled at " + pollDate);
  }
  
  /** Display an exception in this view.
   * This should be used in case we detect that something went wrong with
   * the task associated with this view.
   * @param message The exception to display.
   */
  @Override
  public void setException(Exception e) {
    final StringWriter s = new StringWriter();
    final PrintWriter w = new PrintWriter(s);
    w.println(e.getMessage());
    e.printStackTrace(w);
    w.flush();
    pollLabel.setText(s.toString());
  }

  @Override
  public void taskFailed(SoamException exception) { setException(exception); }
}
