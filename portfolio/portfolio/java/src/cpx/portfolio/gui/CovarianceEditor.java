package cpx.portfolio.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import cpx.portfolio.data.Covariance;
import cpx.portfolio.data.IO;
import cpx.portfolio.data.Investment;

/** Editor for covariance matrix and investments.
 * The editor displays the covariance matrix in a table. It allows editing the covariance
 * values as well as adding/removing investments and editing their expected returns.
 */
public class CovarianceEditor extends JPanel {
  private static final long serialVersionUID = 1;
  
  /** Listener to trigger an optimization run.
   * Register an instance of this via {@link CovarianceEditor#addRunListener(RunListener)} to
   * get notified whenever the user clicks "Run" in a {@link CovarianceEditor}.
   */
  public interface RunListener {
    /** Start an optimization run.
     * This function is invoked when the user requests one or mutliple optimization runs from the
     * {@link CovarianceEditor} widget. The function should start a new run for each combination
     * of wealth and rho given by <code>wealths</code> and <code>rhos</code>.
     * @param investments The list of investments we can choose from.
     * @param covariance  The covariance matrix.
     * @param wealths     The list of initial wealth values.
     * @param rhos        The list of rho values.
     */
    public void run(Collection<Investment> investments, Covariance covariance, Double[] wealths, Double[] rhos);
  }
  
  /** Listener to trigger a sampling run.
   * Register an instance of this via {@link CovarianceEditor#addSampleListener(SampleListener)} to
   * get notified whenever the user clicks "Sample" in a {@link CovarianceEditor}.
   */
  public interface SampleListener {
    /** Start a sampling run.
     * This function is invoked when the user requests a sampling run from the {@link CovarianceEditor} widget.
     * The function should start a new sampling run that samples rho in the interval [<code>minRho</code>, <code>maxRho</code>] with a
     * step width of <code>step</code>.
     * @param investments The list of investments we can choose from.
     * @param covariance  The covariance matrix.
     * @param wealth      The initial wealth.
     * @param minRho      The sampling lower bound for rho.
     * @param maxRho      The sampling upper bound for rho.
     * @param step        The sampling step width.
     */
    public void sample(Collection<Investment> investments, Covariance covariance, double wealth, double minRho, double maxRho, double step);
  }

  /** Table model for displaying investments and covariance.
   * The first row in the table lists the expected return for each investment.
   * Next there are a number of empty rows.
   * Following the empty rows, the covariance matrix is displayed as upper triangular
   * matrix. 
   */
  private final class TableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1;
    /** Index of the row that specifies the return for each investment. */
    private static final int RETURN_ROW = 0;
    /** Row offset for display of covariance values. */
    private static final int COVARIANCE_OFFSET = 2;
    
    /** Array representation of the current set of investments.
     * Since we need to look up investments by index into the collection we keep
     * an array representation of that set.
     */
    private Investment[] investments;
    
    public TableModel(Collection<Investment> is) {
      investments = is.toArray(new Investment[is.size()]);
    }
    
    /** Set the investments currently viewed by this model.
     * This will also reload the covariance matrix and adapt the table structure as needed.
     * @param is The set of investments to display.
     */
    public void setInvestments(Collection<Investment> is) {
      investments = is.toArray(new Investment[is.size()]);
      fireTableStructureChanged();
      fireTableDataChanged();
    }
    
    @Override
    public int getColumnCount() { return investments.length + 1; }
    @Override
    public int getRowCount() { return COVARIANCE_OFFSET + investments.length; }
    @Override
    public Object getValueAt(int row, int col) {
      if (col == 0) {
        if (row == RETURN_ROW)
          return "Return";
        else if (row >= COVARIANCE_OFFSET)
          return investments[row - COVARIANCE_OFFSET].getName();
        else
          return "";
      }
      else {
        --col;
        if (row == RETURN_ROW)
          return new Double(investments[col].getReturn());
        else if (row < COVARIANCE_OFFSET)
          return "";
        row -= COVARIANCE_OFFSET;
        if (row > col)
          return "";
        else
          return new Double(covariance.getCovariance(investments[row].getId(), investments[col].getId()));
      }
    }
    @Override
    public Class<?> getColumnClass(int col) { return String.class; }
    @Override
    public boolean isCellEditable(int row, int col) {
      if (col < 1)
        return false;
      --col;
      if (row == RETURN_ROW)
        return true;
      row -= COVARIANCE_OFFSET;
      return row <= col;
    }
    @Override
    public void setValueAt(Object value, int row, int col) {
      try {
        final double d = Double.parseDouble(value.toString().trim());
        if (row == RETURN_ROW)
          investments[col - 1].setReturn(d);
        else {
          --col;
          row -= COVARIANCE_OFFSET;
          covariance.setCovariance(investments[row].getId(), investments[col].getId(), d);
        }
      }
      catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CovarianceEditor.this), e.getMessage(), "Not a number", JOptionPane.ERROR_MESSAGE);
      }
    }
    @Override
    public String getColumnName(int col) {
      if (col == 0)
        return "";
      else
        return investments[col - 1].getName();
    }
  }
  
  private final Collection<Investment> investments;
  private final Covariance covariance;
  private final TableModel tableModel;
  private final JTable covarianceTable;
  private final JTextField runWealth = new JTextField(20);
  private final JTextField runRho = new JTextField(20);
  /** Action to start an optimization run.
   * The {@link #actionPerformend(ActionEvent)} method of this action starts a
   * new optimization run for each rho/wealth combination currently set in
   * {@link #runWealth} and {@link #runRho}.
   * An optimization run is started by invoking {@link RunListener#run(Collection, Covariance, Double[], Double[])} on
   * all registered {@link RunListener}s.
   */
  private final Action RUN = new AbstractAction("Run") {
    private static final long serialVersionUID = 1;
    @Override
    public void actionPerformed(ActionEvent event) {
      try {
        final Double[] wealths = text2doubles(runWealth, "wealth");
        final Double[] rhos = text2doubles(runRho, "rho");
        for (RunListener r : runListeners)
          r.run(investments, covariance, wealths, rhos);
      }
      catch (IllegalStateException e) {
        JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CovarianceEditor.this), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  };
  private final JTextField sampleWealth = new JTextField(20);
  private final JTextField sampleRhoLb = new JTextField(20);
  private final JTextField sampleRhoUb = new JTextField(20);
  private final JTextField sampleStep = new JTextField(20);
  /** Action to start a sampling run.
   * The {@link #actionPerformed(ActionEvent)} function of this class starts a new sampling
   * run that samples rho between {@link #sampleRhoLb} and {@link #sampleRhoUb} with a
   * step width of {@link #sampleStep}. The initial wealth for the sampling run is {@link #sampleWealth}.
   * A sampling run is started by invoking {@link SampleListener#sample(Collection, Covariance, double, double, double, double)}
   * on all registered {@link SampleListener}s.
   */
  private final Action SAMPLE = new AbstractAction("Sample") {
    private static final long serialVersionUID = 1;
    @Override
    public void actionPerformed(ActionEvent event) {
      try {
        final double wealth = Double.parseDouble(sampleWealth.getText().trim());
        final double rhoLb = Double.parseDouble(sampleRhoLb.getText().trim());
        final double rhoUb = Double.parseDouble(sampleRhoUb.getText().trim());
        final double step = Double.parseDouble(sampleStep.getText().trim());
        for (SampleListener s : sampleListeners)
          s.sample(investments, covariance, wealth, rhoLb, rhoUb, step);
      }
      catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CovarianceEditor.this), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  };
  
  /** Create a new investments.
   * Pops up a modal dialog that allows input of a new investment.
   */
  private final Action NEW_INVESTMENT = new AbstractAction("New Investment") {
    private static final long serialVersionUID = 1;
    @Override
    public void actionPerformed(ActionEvent e) {
      long maxId = 0;
      for (final Investment i : investments)
        maxId = Math.max(maxId, i.getId());
      final long newId = maxId + 1;
      
      final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(CovarianceEditor.this), true);
      dialog.setTitle("Create new Investment");
      ((JComponent)dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      dialog.getContentPane().setLayout(new BorderLayout(5, 5));
      
      final JPanel panel = new JPanel();
      final Insets inset = new Insets(2, 2, 2, 2);
      panel.setLayout(new GridBagLayout());
      panel.add(new JLabel("Name"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
      final JTextField nameInput = new JTextField(20);
      nameInput.setText("Investment " + newId);
      panel.add(nameInput, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, inset, 0, 0));
      panel.add(new JLabel("Return"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, inset, 0, 0));
      final JTextField returnInput = new JTextField(20);
      returnInput.setText("0.0");
      panel.add(returnInput, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, inset, 0, 0));
      panel.add(new JLabel("Covariance"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, inset, 0, 0));
      final Double[] newCovariance = new Double[investments.size() + 1];
      final Investment[] currentInvestments = investments.toArray(new Investment[investments.size()]);
      Arrays.fill(newCovariance, new Double(0.0));
      final JTable covarianceInput = new JTable(new AbstractTableModel() {
        private static final long serialVersionUID = 1;
        @Override
        public int getColumnCount() { return 2; }
        @Override
        public int getRowCount() { return newCovariance.length; }
        @Override
        public Object getValueAt(int row, int col) {
          if (col == 0) {
            if (row == 0)
              return "";
            else
              return currentInvestments[row - 1].getName();
          }
          else
            return newCovariance[row];
        }
        @Override
        public Class<?> getColumnClass(int col) { return (col == 0) ? String.class : Double.class; }
        @Override
        public boolean isCellEditable(int row, int col) { return col == 1; }
        @Override
        public void setValueAt(Object value, int row, int col) {
          newCovariance[row] = Double.parseDouble(value.toString());
        }
        @Override
        public String getColumnName(int col) { return (col == 0) ? "Investment" : "Covariance"; }
      });
      panel.add(new JScrollPane(covarianceInput), new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, inset, 0, 0));
      dialog.getContentPane().add(panel, BorderLayout.CENTER);
      
      final JPanel buttons = new JPanel();
      buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
      buttons.add(new JButton(new AbstractAction("Add") {
        private static final long serialVersionUID = 1;
        @Override
        public void actionPerformed(ActionEvent event) {
          final String name = nameInput.getText().trim();
          if (name.length() == 0) {
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(dialog), "Name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
          }
          double ret = Double.NaN; 
          try { ret = Double.parseDouble(returnInput.getText().trim()); }
          catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(dialog), "Not a number: " + returnInput.getText().trim(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
          }
          final Investment i = new Investment();
          i.setId(newId);
          i.setName(name);
          i.setReturn(ret);
          investments.add(i);
          covariance.setCovariance(i.getId(), i.getId(), newCovariance[0]);
          for (int c = 0; c < currentInvestments.length; ++c)
            covariance.setCovariance(currentInvestments[c].getId(), i.getId(), newCovariance[c + 1]);
          tableModel.setInvestments(investments);
          dialog.setVisible(false);
        }
      }));
      buttons.add(Box.createHorizontalStrut(5));
      buttons.add(new JButton(new AbstractAction("Cancel") {
        private static final long serialVersionUID = 1;
        @Override
        public void actionPerformed(ActionEvent event) { dialog.setVisible(false); }
      }));
      buttons.add(Box.createVerticalStrut(5));
      dialog.getContentPane().add(buttons, BorderLayout.SOUTH);

      dialog.pack();
      dialog.setVisible(true);
    }
  };
  
  /** Deletes the investment that is currently selected in the covariance table.
   * If no table row is selected or the selected table row does not correspond to
   * an investment then nothing happens.
   */
  private final Action DELETE_INVESTMENT = new AbstractAction("Delete Investment") {
    private static final long serialVersionUID = 1;
    @Override
    public void actionPerformed(ActionEvent e) {
      final int selectedRow = covarianceTable.getSelectedRow();
      if (selectedRow < TableModel.COVARIANCE_OFFSET)
        return;
      final int row = selectedRow - TableModel.COVARIANCE_OFFSET;
      final Investment investment = tableModel.investments[row];
      investments.remove(investment);
      covariance.remove(investment.getId());
      tableModel.setInvestments(investments);
    }
  };
  
  /** File chooser used in {@link #SAVE} and {@link #LOAD}.
   * The actions share a static file chooser so that loads and saves always
   * take off where the previous load/save left.
   */
  private JFileChooser chooser = null;
  private void initChooser() {
    if (chooser == null) {
      chooser = new JFileChooser();
    }
  }
  /** Save the current editor state to a file.
   * Pops up a modal dialog that prompts for a save file name and saves the editor
   * content to that file.
   */
  private final Action SAVE = new AbstractAction("Save ...") {
    private static final long serialVersionUID = 1;
    @Override
    public void actionPerformed(ActionEvent e) {
      initChooser();
      if (chooser.showSaveDialog(JOptionPane.getFrameForComponent(CovarianceEditor.this)) == JFileChooser.APPROVE_OPTION) {
        final File file = chooser.getSelectedFile();
        try {
          final FileOutputStream fos = new FileOutputStream(file);
          try { IO.save(fos, investments, covariance); }
          finally { fos.close(); }
        }
        catch (IOException exception) {
          JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CovarianceEditor.this), exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  };
  /** Load editor from a file.
   * Pops up a modal dialog that prompts for an input file name and loads the data
   * specified in that file.
   */
  private final Action LOAD = new AbstractAction("Load ...") {
    private static final long serialVersionUID = 1;
    @Override
    public void actionPerformed(ActionEvent e) {
      initChooser();
      if (chooser.showOpenDialog(JOptionPane.getFrameForComponent(CovarianceEditor.this)) == JFileChooser.APPROVE_OPTION) {
        final File file = chooser.getSelectedFile();
        try {
          final FileInputStream fos = new FileInputStream(file);
          try { IO.load(fos, investments, covariance); }
          finally {
            fos.close();
            tableModel.setInvestments(investments);
          }
        }
        catch (IOException exception) {
          JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CovarianceEditor.this), exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  };
  private Collection<RunListener> runListeners = new Vector<CovarianceEditor.RunListener>();
  private Collection<SampleListener> sampleListeners = new Vector<CovarianceEditor.SampleListener>();
  
  /** Convert a string specifying a list of double values to an array of doubles. */
  private Double[] text2doubles(JTextField field, String what) throws IllegalStateException {
    final String[] text = field.getText().trim().split(" +");
    if (text.length == 0)
      throw new IllegalStateException("Invalid value for " + what);
    final Double[] ret = new Double[text.length];
    for (int i = 0; i < text.length; ++i) {
      try { ret[i] = new Double(text[i]); }
      catch (NumberFormatException e) { throw new IllegalStateException("Invalid value for " + what + " (" + e.getMessage() + ")"); }
    }
    return ret;
  }
  
  public CovarianceEditor(Collection<Investment> initialInvestments, Covariance InitialCovariance, double defaultWealth, double defaultRho) {
    investments = initialInvestments;
    covariance = InitialCovariance;
    
    setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    setLayout(new BorderLayout(5, 5));
    
    // Create the central component: the table that shows covariances and investments.
    tableModel = new TableModel(investments);
    covarianceTable = new JTable(tableModel);
    covarianceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    add(new JScrollPane(covarianceTable), BorderLayout.CENTER);
   
    // In the top row of the layout create "New Investment", "Delete Investment", "Load", "Save" buttons.
    final JPanel buttons = new JPanel();
    buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
    JButton b;
    buttons.add(b = new JButton(NEW_INVESTMENT));
    b.setToolTipText("Create a new investment");
    buttons.add(Box.createHorizontalStrut(5));
    buttons.add(b = new JButton(DELETE_INVESTMENT));
    b.setToolTipText("Delete the currently selected investment");
    buttons.add(Box.createHorizontalStrut(5));
    buttons.add(b = new JButton(SAVE));
    b.setToolTipText("Save current investment and covariance settings to a file");
    buttons.add(Box.createHorizontalStrut(5));
    buttons.add(b = new JButton(LOAD));
    b.setToolTipText("Load investment and covariance settings from a file");
    add(buttons, BorderLayout.NORTH);
    
    // Create input controls for "Run".
    final JPanel runButtons = new JPanel();
    runButtons.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true),
        BorderFactory.createEmptyBorder(2, 2, 2, 2)));
    runButtons.setLayout(new GridBagLayout());
    final Insets inset = new Insets(2, 2, 2, 2);
    runButtons.add(new JLabel("Wealth"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    runButtons.add(runWealth, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    runButtons.add(new JLabel("Rho"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    runButtons.add(runRho, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    runButtons.add(b = new JButton(RUN), new GridBagConstraints(2, 0, 1, 2, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.BOTH, inset, 0, 0));
    b.setToolTipText("Find optimal allocation for specified wealth and rho");
    
    // Create input controls for "Sample".
    final JPanel sampleButtons = new JPanel();
    sampleButtons.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
    sampleButtons.setLayout(new GridBagLayout());
    sampleButtons.add(new JLabel("Wealth"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    sampleButtons.add(sampleWealth, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    sampleButtons.add(new JLabel("min rho"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    sampleButtons.add(sampleRhoLb, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    sampleButtons.add(new JLabel("max rho"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    sampleButtons.add(sampleRhoUb, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    sampleButtons.add(new JLabel("step"), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    sampleButtons.add(sampleStep, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, inset, 0, 0));
    sampleButtons.add(b = new JButton(SAMPLE), new GridBagConstraints(2, 1, 1, 2, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.BOTH, inset, 0, 0));
    b.setToolTipText("Sample return and total variance over rho for a given wealth");
    
    // Put the run and sample buttons into a box and add them at the bottom of the main component.
    final JPanel lower = new JPanel();
    lower.setLayout(new BoxLayout(lower, BoxLayout.X_AXIS));
    lower.add(runButtons);
    lower.add(Box.createHorizontalStrut(5));
    lower.add(sampleButtons);
    add(lower, BorderLayout.SOUTH);
    
    // Apply default values to input controls.
    runWealth.setText("" + defaultWealth);
    runRho.setText("" + defaultRho);
    sampleWealth.setText("" + defaultWealth);
    sampleRhoLb.setText("0.0");
    sampleRhoUb.setText("1.0");
    sampleStep.setText("0.01");
  }
  
  public void addRunListener(RunListener runListener) { runListeners.add(runListener); }
  public void removeRunListener(RunListener runListener) { runListeners.add(runListener); }
  
  public void addSampleListener(SampleListener sampleListener) { sampleListeners.add(sampleListener); }
  public void removeSampleListener(SampleListener sampleListener) { sampleListeners.add(sampleListener); }
}
