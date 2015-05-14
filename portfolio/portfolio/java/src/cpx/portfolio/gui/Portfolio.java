package cpx.portfolio.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.platform.symphony.soam.Connection;
import com.platform.symphony.soam.DefaultSecurityCallback;
import com.platform.symphony.soam.EnumItems;
import com.platform.symphony.soam.Session;
import com.platform.symphony.soam.SessionCloseFlags;
import com.platform.symphony.soam.SessionCreationAttributes;
import com.platform.symphony.soam.SessionOpenAttributes;
import com.platform.symphony.soam.SoamException;
import com.platform.symphony.soam.SoamFactory;
import com.platform.symphony.soam.TaskOutputHandle;
import com.platform.symphony.soam.TaskSubmissionAttributes;

import cpx.portfolio.data.Covariance;
import cpx.portfolio.data.Investment;
import cpx.portfolio.messages.Input;
import cpx.portfolio.messages.Output;

/** The main GUI class.
 * This class also implements all interaction with Symphony.
 */
public class Portfolio extends JFrame implements CovarianceEditor.RunListener, CovarianceEditor.SampleListener, ResultView.CloseListener {
  private static final long serialVersionUID = 1;
  
  private final JTabbedPane tabs = new JTabbedPane();
  private final CovarianceEditor covarianceEditor;
  
  private DefaultSecurityCallback securityCallback = null;
  private Connection connection = null;
  
  /** Class to poll for results from a Symphony task.
   * Each instance of this class is associated with a Symphony session. Since
   * we only start one task per session this is the same as being associated with a
   * single task. Each task corresponds to an optimization problem.
   * In the {@link #poll()} function an instance of this class checks whether the
   * optimization problem submitted to PlatformSymphony has already been solved. If
   * it has then the results are displayed in the GUI. 
   *
   */
  private abstract class Poller {
    public final String sessionId;
    public final String sessionName;
    private final ResultView view;
    
    protected Poller(String sessionId, String sessionName, ResultView view) {
      this.sessionId = sessionId;
      this.sessionName = sessionName;
      this.view = view;
    }
    
    protected abstract void taskComplete(Output output);
    
    protected void handleException(final SoamException exception) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          view.setException(exception);
          displayException(exception);
        }
      });
    }
    
    /** Poll the task associated with this instance.
     * If the task is found complete then {@link #taskComplete(Output)} will be invoked with
     * the task's output. Do not invoke this function again once it returned <code>true</code>.
     * @return <code>true</code> if the task is complete, <code>false</code> otherwise.
     */
    public boolean poll() {
      Session session = null;
      try {
        // Open the session.
        final SessionOpenAttributes sessionAttributes = new SessionOpenAttributes();
        sessionAttributes.setSessionId(sessionId);
        sessionAttributes.setSessionName(sessionName);
        sessionAttributes.setSessionFlags(Session.RECEIVE_SYNC);
        session = connection.openSession(sessionAttributes);
        
        // Check for output. Specifying a timeout of 0 means that the function will
        // return immediately.
        final EnumItems enumItems = session.fetchTaskOutput(1, 0);
        if (enumItems.getCount() == 0) {
          // No output available means the task is not finished yet.
          final Date now = new Date();
          SwingUtilities.invokeLater(new Runnable() {
            public void run() { view.setLastPoll(now); }
          });
          return false;
        }
        
        // Results are available.
        final TaskOutputHandle outputHandle = enumItems.getNext();
        if (!outputHandle.isSuccessful()) {
          // The task failed. Post the exception message to the result viewer.
          final SoamException exception = outputHandle.getException();
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              view.taskFailed(exception);
              displayException(exception);
            }
          });
        }
        else {
          // Task was successful.
          final Output output = new Output();
          outputHandle.populateTaskOutput(output);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              taskComplete(output);
            }
          });
        }
        session.close(SessionCloseFlags.DESTROY_ON_CLOSE);
        session = null;
        
        return true;
      }
      catch (final SoamException e) {
        handleException(e);
        // There is something wrong with this session/task. So no longer poll for it.
        return true;
      }
      finally {
        if (session != null) {
          try { session.close(SessionCloseFlags.DETACH_ON_CLOSE); }
          catch (SoamException exception) { handleException(exception); }
        }
      }
    }
  }
  
  /** Implementation of {@link #Poller} for optimization runs. */
  private final class RunPoller extends Poller {
    /** The view to which results are posted once they become available. */
    public final RunResultView view;
    public RunPoller(String sessionId, String sessionName, RunResultView view) {
      super(sessionId, sessionName, view);
      this.view = view;
    }
    public void taskComplete(Output output) {
      view.setResults(output.getInvestments(), output.getTotalReturn(), output.getTotalVariance());
    }
  }
  
  /** Implementation of {@link Poller} for sampling runs. */
  private final class SamplePoller extends Poller {
    /** The view to which results are posted once they become available. */
    public final SampleResultView view;
    public SamplePoller(String sessionId, String sessionName, SampleResultView view) {
      super(sessionId, sessionName, view);
      this.view = view;
    }
    public void taskComplete(Output output) {
      view.addResults(output.getInvestments(), output.getRho(), output.getTotalReturn(), output.getTotalVariance());
    }
  }
  
  /** List of active tasks that we still need to poll for. */
  private final LinkedList<Poller> pollers = new LinkedList<Poller>();
  /** Timer for polling.
   * Using this timer we periodically poll all the tasks in {@link #pollers}.
   */
  private final Timer pollTimer = new Timer("polling timer");
  
  /** Initialize the credentials to log in to PlatformSymphony.
   * In a real world application this function would prompt the user for the
   * credentials. In this example we just use guest/guest.
   */
  private void initCredentials() {
    if (securityCallback == null) {
      securityCallback = new DefaultSecurityCallback("Admin", "Admin");
    }
    if (connection == null) {
      try {
        connection = SoamFactory.connect("PortfolioClient", securityCallback);
      }
      catch (SoamException e) {
        System.err.println(e.getMessage());
        e.printStackTrace();
        JOptionPane.showMessageDialog(Portfolio.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
  
  private long nextId = 0;

  /** Create a unique name for a new session.
   * @return The new session name.
   */
  private synchronized String createSessionName() {
    /** FIXME: This should be world-unique. */
    return "Portfolio" + nextId++;
  }
  
  /** Submit a task to Symphony.
   * The function creates a new session with a single task in it. Then it detaches
   * from the session and returns the id of the newly created session.
   * @param sessionName Name for the newly created task.
   * @param input Input to the new task.
   * @return The session ID of the newly submitted task.
   * @throws SoamException if there is a problem with PlatformSymphony.
   */
  private String startTask(String sessionName, Input input) throws SoamException {
    final SessionCreationAttributes attributes = new SessionCreationAttributes();
    attributes.setSessionName(sessionName);
    attributes.setSessionType("ShortRunningTasks");
    attributes.setSessionFlags(Session.RECEIVE_SYNC);
    
    final Session session = connection.createSession(attributes);
    final String sessionId = session.getId();
    /** FIXME: If any of the below throws then we should close the session? */

    final TaskSubmissionAttributes taskAttributes = new TaskSubmissionAttributes();
    taskAttributes.setTaskInput(input);
    session.sendTaskInput(taskAttributes);
    session.close(SessionCloseFlags.DETACH_ON_CLOSE);
    
    return sessionId;
  }
  
  /** Start a sampling job.
   * This function is invoked when the corresponding {@link CovarianceEditor} detects a request to
   * submit a job that samples a range of rho values.
   */
  @Override
  public void sample(Collection<Investment> investments, Covariance covariance, double wealth, double minRho, double maxRho, double step) {
    initCredentials();
    
    // Create a new task for each value of rho we want to sample.
    try {
      final SampleResultView view = new SampleResultView(new Date(), wealth);
      view.addCloseListener(this);
      int count = 0;
      for (double rho = minRho; rho <= maxRho; rho += step) {
        ++count;
        final String sessionName = createSessionName();
        final String sessionId = startTask(sessionName, new Input(investments, covariance, wealth, rho));
        synchronized (pollers) {
          pollers.add(new SamplePoller(sessionId, sessionId, view));
        }
      }
      view.setTotalResults(count);
      final String tabName = "wealth = " + wealth + ", rho = [" + minRho + ", " + maxRho + "]";
      tabs.addTab(tabName, view);
    }
    catch (SoamException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      JOptionPane.showMessageDialog(Portfolio.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }
  
  /** Start an optimization job.
   * This function is invoked when the associated {@link CovarianceEditor} detects a request to find an
   * optimal portfolio for a wealth/row combination.
   */
  @Override
  public void run(Collection<Investment> investments, Covariance covariance, Double[] wealths, Double[] rhos) {
    initCredentials();
    
    // Create a new solve for each combination of wealth and rho
    try {
      for (final Double wealth : wealths) {
        for (final Double rho : rhos) {
          final String sessionName = createSessionName();
          final String sessionId = startTask(sessionName, new Input(investments, covariance, wealth, rho));
          
          final String tabName = "wealth = " + wealth + ", rho = " + rho;
          final RunResultView view = new RunResultView(new Date(), wealth, rho);
          view.addCloseListener(this);
          tabs.addTab(tabName, view);
          synchronized (pollers) {
            pollers.add(new RunPoller(sessionId, sessionName, view));
          }
        }
      }
    }
    catch (SoamException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      JOptionPane.showMessageDialog(Portfolio.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }
  
  @Override
  public void resultViewClosed(JComponent view) {
    tabs.remove(view);
  }
  
  public Portfolio(Collection<Investment> investments, Covariance covariance, double defaultWealth, double defaultRho) {
    super("Portfolio");
    covarianceEditor = new CovarianceEditor(investments, covariance, defaultWealth, defaultRho);
    covarianceEditor.addRunListener(this);
    covarianceEditor.addSampleListener(this);
    tabs.addTab("Data", covarianceEditor);
    
    getContentPane().add(tabs);
    // Start a timer that periodically looks for finished jobs.
    pollTimer.schedule(new TimerTask() {      
      @Override
      public void run() {
        synchronized (pollers) {
          for (Iterator<Poller> it = pollers.iterator(); it.hasNext(); /* nothing */) {
            if (it.next().poll())
              it.remove();
          }
        }
      }
    }, 1000, 2500);
  }
  
  private void displayException(Exception e) {
    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this), e.getMessage(), "error", JOptionPane.ERROR_MESSAGE);
  }
  
  /** Gracefully exit from the application. */
  public void exit() {
    pollTimer.cancel();
    if (connection != null) {
      try { connection.close(); }
      catch (SoamException ignored) {
        System.err.println(ignored.getMessage());
        ignored.printStackTrace();
      }
    }
  }
  
  /** Hard-coded example data. */
  private static final class Example {
    private static final double[] returns = new double[]{
        1.00125, 1.56359, 1.19330, 1.80874, 1.58501,
        1.47987, 1.35029, 1.89596, 1.82284, 1.74660,
        1.17411, 1.85894, 1.71050, 1.51353, 1.30399,
        1.01498, 1.09140, 1.36445, 1.14731, 1.16590
     };

     private static final double[][] covariance = new double[][]{
        { 10.95, -0.119083, -0.0089114, 0.531663, 0.601764, 0.166234, -0.450789, 0.0570391, 0.783319, -0.519883, 0.875973, 0.955901, -0.539354, -0.462081, -0.862239, 0.779656, 0.996796, -0.611499, -0.266213, -0.840144},
        { -0.119083, 9.81777, -0.677206, 0.00878933, -0.275887, 0.587909, 0.837611, -0.484939, -0.743736, 0.457961, -0.744438, -0.599048, 0.735008, -0.572405, -0.151555, 0.425153, 0.517106, -0.751549, 0.168981, -0.491897},
        { -0.0089114, -0.677206, 9.54527, 0.147496, 0.141575, -0.692892, -0.426557, 0.966613, 0.153233, 0.82168, -0.191351, -0.817194, 0.155553, -0.732017, 0.27958, 0.682241, -0.721915, -0.12302, -0.834681, 0.517014},
        { 0.531663, 0.00878933, 0.147496, 11.229, 0.949339, -0.549547, -0.471725, -0.84698, -0.456099, -0.982971, 0.739189, 0.19599, -0.839442, 0.5009, 0.0274667, -0.572588, -0.531327, 0.843043, -0.657613, -0.842158},
        { 0.601764, -0.275887, 0.141575, 0.949339, 8.9361, 0.314066, -0.286081, 0.140263, 0.83462, 0.600238, -0.252724, -0.00161748, 0.806238, -0.210578, -0.553209, -0.113773, 0.75222, -0.543443, -0.43672, -0.696219},
        { 0.166234, 0.587909, -0.692892, -0.549547, 0.314066, 9.26831, 0.577868, -0.628681, 0.504135, 0.695761, -0.189947, 0.17835, 0.457442, 0.0975066, -0.0943938, -0.931516, -0.89462, 0.227302, -0.410718, 0.628071},
        { -0.450789, 0.837611, -0.426557, -0.471725, -0.286081, 0.577868, 11.0518, 0.597827, 0.854793, 0.624775, -0.565752, 0.184271, 0.555132, -0.242866, 0.604724, -0.584613, -0.494461, 0.740745, 0.62038, -0.804529},
        { 0.0570391, -0.484939, 0.966613, -0.84698, 0.140263, -0.628681, 0.597827, 9.91122, 0.911557, -0.727683, 0.667776, 0.315012, -0.305826, 0.108554, 0.851222, -0.154881, -0.0793481, 0.64098, -0.545091, -0.408979},
        { 0.783319, -0.743736, 0.153233, -0.456099, 0.83462, 0.504135, 0.854793, 0.911557, 11.0092, -0.152654, -0.737999, 0.826685, 0.873348, 0.300058, -0.127232, 0.784967, 0.609638, 0.0722678, -0.653859, 0.104801},
        { -0.519883, 0.457961, 0.82168, -0.982971, 0.600238, 0.695761, 0.624775, -0.727683, -0.152654, 11.4757, 0.91998, -0.662801, -0.492538, 0.496811, -0.509262, 0.688162, -0.606281, 0.00589007, 0.10062, -0.863247},
        { 0.875973, -0.744438, -0.191351, 0.739189, -0.252724, -0.189947, -0.565752, 0.667776, -0.737999, 0.91998, 9.71151, 0.380108, -0.552812, 0.955718, -0.17658, 0.131626, 0.95172, -0.0278329, 0.0559099, 0.131626},
        { 0.955901, -0.599048, -0.817194, 0.19599, -0.00161748, 0.17835, 0.184271, 0.315012, 0.826685, -0.662801, 0.380108, 8.88156, 0.721641, -0.0146794, 0.707907, -0.217566, 0.16892, -0.340983, 0.367565, 0.802393},
        { -0.539354, 0.735008, 0.155553, -0.839442, 0.806238, 0.457442, 0.555132, -0.305826, 0.873348, -0.492538, -0.552812, 0.721641, 10.189, 0.798181, 0.14481, -0.402417, 0.13654, -0.0661641, -0.573351, -0.548051},
        { -0.462081, -0.572405, -0.732017, 0.5009, -0.210578, 0.0975066, -0.242866, 0.108554, 0.300058, 0.496811, 0.955718, -0.0146794, 0.798181, 9.25395, -0.808039, 0.284249, 0.89523, 0.743797, 0.361126, 0.228492},
        { -0.862239, -0.151555, 0.27958, 0.0274667, -0.553209, -0.0943938, 0.604724, 0.851222, -0.127232, -0.509262, -0.17658, 0.707907, 0.14481, -0.808039, 8.40558, 0.542405, -0.0538041, -0.524674, -0.0946684, -0.891537},
        { 0.779656, 0.425153, 0.682241, -0.572588, -0.113773, -0.931516, -0.584613, -0.154881, 0.784967, 0.688162, 0.131626, -0.217566, -0.402417, 0.284249, 0.542405, 9.46003, -0.931639, -0.0470901, 0.336406, -0.398602},
        { 0.996796, 0.517106, -0.721915, -0.531327, 0.75222, -0.89462, -0.494461, -0.0793481, 0.609638, -0.606281, 0.95172, 0.16892, 0.13654, 0.89523, -0.0538041, -0.931639, 11.3699, 0.534227, -0.693533, -0.259163},
        { -0.611499, -0.751549, -0.12302, 0.843043, -0.543443, 0.227302, 0.740745, 0.64098, 0.0722678, 0.00589007, -0.0278329, -0.340983, -0.0661641, 0.743797, -0.524674, -0.0470901, 0.534227, 8.2202, -0.398694, -0.585559},
        { -0.266213, 0.168981, -0.834681, -0.657613, -0.43672, -0.410718, 0.62038, -0.545091, -0.653859, 0.10062, 0.0559099, 0.367565, -0.573351, 0.361126, -0.0946684, 0.336406, -0.693533, -0.398694, 8.41221, 0.435499},
        { -0.840144, -0.491897, 0.517014, -0.842158, -0.696219, 0.628071, -0.804529, -0.408979, 0.104801, -0.863247, 0.131626, 0.802393, -0.548051, 0.228492, -0.891537, -0.398602, -0.259163, -0.585559, 0.435499, 11.0019}
     };
     
     public static final double wealth = 100.0;
     public static final double rho = 0.01;
     
     public static void populate(Collection<Investment> investments, Covariance covariance) {
       for (int i = 0; i < returns.length; ++i) {
         final Investment investment = new Investment();
         investment.setName("Investment" + i);
         investment.setId(i);
         investment.setReturn(returns[i]);
         investments.add(investment);
       }
       for (int i = 0; i < Example.covariance.length; ++i) {
         for (int j = 0; j < Example.covariance[i].length; ++j)
           covariance.setCovariance(i, j, Example.covariance[i][j]);
       }
     }
  }
  
  public static void main(String[] args) throws SoamException {
    SoamFactory.initialize();
    
    final Collection<Investment> investments = new Vector<Investment>();
    final Covariance covariance = new Covariance();
    Example.populate(investments, covariance);
    
    final Portfolio portfolio = new Portfolio(investments, covariance, Example.wealth, Example.rho);
    portfolio.addWindowListener(new WindowAdapter() {

      @Override
      public void windowClosing(WindowEvent e) {
        portfolio.exit();
        SoamFactory.uninitialize();
        System.exit(0);
      }
    });
    portfolio.pack();
    portfolio.setVisible(true);
  }
}
