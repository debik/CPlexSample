package cpx.portfolio.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.platform.symphony.soam.SoamException;

import cpx.portfolio.data.Investment;

/** Widget to view the results of a sampling run.
 * The widget shows the sampling data as it comes in. The view is updated continuously
 * whenever new data arrives.
 */
public class SampleResultView extends JPanel implements ResultView {
  
  /** A single data point in the sampling result.
   * An instance of this class specifies the total return and variance that
   * is expected for a particular value of rho. It thus represents the result
   * of one single optimization problem solved.
   * The class implements the {@link Comparable} interface so that collections
   * of instances can be sorted by their respective rho values.
   */
  private static final class Result implements Comparable<Result> {
    public final double rho;
    public final double totalReturn;
    public final double totalVariance;
    public Result(double rho, double totalReturn, double totalVariance) {
      this.rho = rho;
      this.totalReturn = totalReturn;
      this.totalVariance = totalVariance;
    }
    @Override
    public int compareTo(Result r) {
      if (rho < r.rho)
        return -1;
      else if (rho > r.rho)
        return 1;
      else
        return 0;
    }
  }
  
  private static final long serialVersionUID = 1;
  
  /** A very simple Canvas implementation on which we can draw the results of a sampling run.
   */
  private static final class Canvas extends JLabel implements Scrollable {
    private static final long serialVersionUID = 1; 
    private static final double yMargin = 50;
    private static final double xMargin = 80;
    private static final double width = 1000;
    private static final double height = 500;
    private Dimension minDim = new Dimension((int)(2 * xMargin + width), (int)(2 * yMargin + height));

    private int maxUnitIncrement = 1;
    
    /** Descriptor for a tick label on either the x or the y axis of the plot. */
    private static final class TickLabel {
      /** Label text. */
      public final String label;
      /** Horizontal coordinate of label. */
      public final float x;
      /** Vertical coordinate of label. */
      public final float y;
      /** Is this a label on the y axis (on x axis otherwise). */
      public final boolean yLabel;
      public TickLabel(String label, double x, double y, boolean yLabel) {
        this.label = label;
        this.x = (float)x;
        this.y = (float)y;
        this.yLabel = yLabel;
      }
    }
    
    /** The data points to draw. */
    private final Collection<Shape> points = new Vector<Shape>();
    /** The path between the different elements in {@link #points}. */
    private Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
    /** Tick marks to be drawn on the x and y axis. */
    private Collection<Shape> tickMarks = new Vector<Shape>();
    /** Labels for tick marks. */
    private Collection<TickLabel> tickLabels = new Vector<TickLabel>();

    public Canvas() {
      super();
      setBackground(Color.WHITE);
    }
    
    @Override
    public Dimension getMinimumSize() { return minDim; }
    @Override
    public Dimension getPreferredSize() { return minDim; }
    
    /** Set new plot data for this canvas.
     * Sets up this canvas so that it visualizes the data described by
     * <code>x</code> and <code>y</code>. The arrays are interpreted as
     * corresponding pairs of data.
     * @param x Horizontal coordinates.
     * @param y Vertical coordinates.
     */
    public void setData(double[] x, double[] y) {
      final Collection<Shape> newPoints = new Vector<Shape>(x.length);
      final Path2D.Double newPath = new Path2D.Double(Path2D.WIND_EVEN_ODD);
      double[] transX;
      double[] transY;
      if (x.length > 0) {
        // Find minimum and maximum value in each dimension.
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < x.length; ++i) {
          minX = Math.min(x[i], minX);
          maxX = Math.max(x[i], maxX);
          minY = Math.min(y[i], minY);
          maxY = Math.max(y[i], maxY);
        }
        // Transform the values so that the plot spans the whole
        // width/height of the canvas.
        transX = new double[x.length];
        transY = new double[x.length];
        if (x.length > 1) {
          final double xScale = width / (maxX - minX);
          final double yScale = height / Math.max(0.1, maxY - minY);
          for (int i = 0; i < x.length; ++i) {
            transX[i] = xMargin + (x[i] - minX) * xScale;
            transY[i] = yMargin + height - (y[i] - minY) * yScale;
          }
        }
      }
      else {
        transX = x;
        transY = y;
      }
      
      // Create the new objects to be drawn.
      for (int i = 0; i < transX.length; ++i) {
        if (i == 0)
          newPath.moveTo(transX[i], transY[i]);
        else
          newPath.lineTo(transX[i], transY[i]);
        newPoints.add(new Ellipse2D.Double(transX[i] - 2, transY[i] - 2, 4, 4));
      }
      final Collection<Shape> newTickMarks = new Vector<Shape>(4);
      final Collection<TickLabel> newTickLabels = new Vector<TickLabel>(4);
      if (transX.length > 0) {
        newTickMarks.add(new Line2D.Double(transX[0], height + yMargin - 5, transX[0], height + yMargin + 5));
        newTickLabels.add(new TickLabel(String.format("%.2f", x[0]), transX[0], height + yMargin + 10, false));
        newTickMarks.add(new Line2D.Double(xMargin - 5, transY[0], xMargin + 5, transY[0]));
        newTickLabels.add(new TickLabel(String.format("%.2f", y[0]), xMargin - 10, transY[0], true));
      }
      if (transX.length > 1) {
        final int last = transX.length - 1;
        newTickMarks.add(new Line2D.Double(transX[last], height + yMargin - 5, transX[last], height + yMargin + 5));
        newTickLabels.add(new TickLabel(String.format("%.2f",  x[last]), transX[last], height + yMargin + 10, false));
        newTickMarks.add(new Line2D.Double(xMargin - 5, transY[last], xMargin + 5, transY[last]));
        newTickLabels.add(new TickLabel(String.format("%.2f", y[last]), xMargin - 10, transY[last], true));        
      }

      // Update the draing data.
      synchronized (points) {
        points.clear();
        points.addAll(newPoints);
        path = newPath;
        tickMarks = newTickMarks;
        tickLabels = newTickLabels;
      }
      
      // Trigger a repaint.
      revalidate();
      repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
      final Graphics2D g2d = (Graphics2D)g;
      super.paintComponent(g);
      final Color old = g2d.getColor();
      g2d.setColor(Color.RED);
      synchronized (points) {
        for (final Shape s : points)
          g2d.fill(s);
        g2d.draw(path);
        g2d.setColor(Color.BLACK);
        for (final Shape s : tickMarks)
          g2d.draw(s);
        for (final TickLabel t : tickLabels) {
          // Printing strings is a little involved since we have to first figure out the
          // size of the bounding box so that we can move the string to the correct place
          // (so that it ends up being drawn at the expected place).
          final GlyphVector gv = g2d.getFont().createGlyphVector(g2d.getFontRenderContext(), t.label);
          final Rectangle2D rect = gv.getVisualBounds();
          if (t.yLabel)
            g2d.drawGlyphVector(gv, t.x - (float)rect.getWidth(), t.y + (float)rect.getHeight() / 2.0f);
          else
            g2d.drawGlyphVector(gv, t.x - (float)rect.getWidth() / 2.0f, t.y + (float)rect.getHeight());
        }
      }
      g2d.setColor(old);
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      // Get the current position.
      int currentPosition = 0;
      if (orientation == SwingConstants.HORIZONTAL) {
        currentPosition = visibleRect.x;
      } else {
        currentPosition = visibleRect.y;
      }

      // Return the number of pixels between currentPosition
      // and the nearest tick mark in the indicated direction.
      if (direction < 0) {
        int newPosition = currentPosition - (currentPosition / maxUnitIncrement) * maxUnitIncrement;
        return (newPosition == 0) ? maxUnitIncrement : newPosition;
      } else {
        return ((currentPosition / maxUnitIncrement) + 1) * maxUnitIncrement - currentPosition;
      }
    }
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      if (orientation == SwingConstants.HORIZONTAL) {
        return visibleRect.width - maxUnitIncrement;
      } else {
        return visibleRect.height - maxUnitIncrement;
      }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return false;
    }    
  }
  
  /** The canvas that plots the return values versus rho values. */
  private final Canvas returnView = new Canvas();
  /** The canvas that plots the variance values versus rho values. */
  private final Canvas varianceView = new Canvas();
  /** The result data points we have accumulated so far. */
  private Vector<Result> results = new Vector<Result>();

  /** When we started this sampling process. */
  private final Date start;
  /** Label to show the results of polling. */
  private JTextArea pollLabel = new JTextArea(5, 80);
  /** Listeners that are invoked when this view is deleted. */
  private Collection<CloseListener> closeListeners = new Vector<SampleResultView.CloseListener>();
  /** Number of results we expected. If this is -1 we don't know yet how many results to expect. */
  private int totalResults = -1;
  /** Progress bar that shows how may results we already got and how many are still to expect. */
  private final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 1);
  
  public SampleResultView(Date start, double wealth) {
    this.start = start;
    pollLabel.setText("Started at " + start);
    
    setLayout(new BorderLayout(5, 5));
    
    add(progressBar, BorderLayout.NORTH);
    progressBar.setStringPainted(true);
    
    final JPanel center = new JPanel();
    center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
    
    for (final String s : new String[]{ "Return", "Variance"}) {
      final Canvas canvas = s.equals("Return") ? returnView : varianceView;
      
      final JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1, true),
          BorderFactory.createEmptyBorder(2, 2, 2, 2)));
      panel.setLayout(new BorderLayout());
      panel.add(new JLabel(s), BorderLayout.NORTH);
      panel.add(new JScrollPane(canvas), BorderLayout.CENTER);
      panel.add(new JLabel("wealth=" + String.format("%.2f", wealth)), BorderLayout.WEST);
      
      center.add(panel);
    }
    add(center, BorderLayout.CENTER);
    add(pollLabel, BorderLayout.SOUTH);
  }
  
  private void updateProgress() {
    int total = totalResults;
    int nResults;
    synchronized (results) { nResults = results.size(); }
    String label;
    if (total < 0) {
      label = "Got " + nResults + " of ? results";
      total = 100;
      progressBar.setMaximum(total);
    }
    else {
      label = "Got " + nResults + " of " + totalResults + " results";
      progressBar.setMaximum(total);
    }
    progressBar.setValue((int)Math.round(((1.0 * nResults) / total) * progressBar.getMaximum()));
    progressBar.setString(label);
    if (nResults >= total)
      setComplete();
  }
  
  public void setTotalResults(int totalResults) {
    this.totalResults = totalResults;
    updateProgress();
  }
  
  public void addCloseListener(CloseListener closeListener) { closeListeners.add(closeListener); }
  public void removeCloseListener(CloseListener closeListener) { closeListeners.remove(closeListener); }
  
  /** Mark the view as complete.
   * Replaces the poll information label by a button that can be used to close the view.
   * @param text Text for the button.
   */
  private void setComplete(String text) {
    remove(pollLabel);
    add(new JButton(new AbstractAction(text) {
      private static final long serialVersionUID = 1;      
      @Override
      public void actionPerformed(ActionEvent e) {
        for (final CloseListener closeListener : closeListeners)
          closeListener.resultViewClosed(SampleResultView.this);
        
      }
    }), BorderLayout.SOUTH);
  }
  /** Mark this view as complete.
   * The layout of the view slightly changes at the point at which all
   * data has been collected.
   */
  public void setComplete() { setComplete("Close"); }
      
  /** Set the results to be displayed in this instance.
   * @param investments
   * @param totalReturn
   * @param totalVariance
   */
  public void addResults(Collection<Investment> investments, double rho, double totalReturn, double totalVariance) {
    synchronized (results) {
      results.add(new Result(rho, totalReturn, totalVariance));
      Collections.sort(results);
      final double[] x = new double[results.size()];
      final double[] yReturn = new double[results.size()];
      final double[] yVariance = new double[results.size()];
      int i = 0;
      for (final Result r : results) {
        x[i] = r.rho;
        yReturn[i] = r.totalReturn;
        yVariance[i] = r.totalVariance;
        ++i;
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          returnView.setData(x, yReturn);
          varianceView.setData(x, yVariance);
          updateProgress();
        }
      });
    }
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
    setComplete(e.getMessage() + " (close)");
  }

  @Override
  public void taskFailed(SoamException exception) { setException(exception); }
}
