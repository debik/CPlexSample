package cpx.portfolio.messages;

import java.util.Collection;
import java.util.Vector;

import com.platform.symphony.soam.InputStream;
import com.platform.symphony.soam.Message;
import com.platform.symphony.soam.OutputStream;
import com.platform.symphony.soam.SoamException;

import cpx.portfolio.data.Covariance;
import cpx.portfolio.data.Investment;

/** Input for the portfolio service.
 * An instance of this messages provides all the data the portfolio service
 * needs to compute an optimal portfolio allocation.
 */
public class Input extends Message {
  private Collection<Investment> investments = new Vector<Investment>();
  private Covariance covariance = new Covariance();
  private double wealth = Double.NaN;
  private double rho = Double.NaN;
  
  public Input() {}
  public Input(Collection<Investment> investments, Covariance covariance, double wealth, double rho) {
    this.investments.addAll(investments);
    this.covariance.copy(covariance);
    this.wealth = wealth;
    this.rho = rho;
  }
  
  private void clear() {
    investments = new Vector<Investment>();
    covariance = new Covariance();
    wealth = Double.NaN;
    rho = Double.NaN;
  }
  
  public Collection<Investment> getInvestments() { return investments; }
  public void setInvestments(Collection<Investment> investments) { this.investments = investments; }

  public Covariance getCovariance() { return covariance; }
  public void setCovariance(Covariance covariance) { this.covariance = covariance; }

  public double getWealth() { return wealth; }
  public void setWealth(double wealth) { this.wealth = wealth; }

  public double getRho() { return rho; }
  public void setRho(double rho) { this.rho = rho; }

  @Override
  public void onDeserialize(InputStream stream) throws SoamException {
     clear();
     boolean doReset = true;
     try {
       final long size = stream.readLong();
       for (long i = 0; i < size; ++i) {
         final Investment investment = new Investment();
         investment.onDeserialize(stream);
         investments.add(investment);
       }
       covariance.onDeserialize(stream);
       wealth = stream.readDouble();
       rho = stream.readDouble();
       doReset = false;
     }
     finally {
       if (doReset)
         clear();
     }
  }

  @Override
  public void onSerialize(OutputStream stream) throws SoamException {
    stream.writeLong(investments.size());
    for (final Investment i : investments)
      i.onSerialize(stream);
    covariance.onSerialize(stream);
    stream.writeDouble(wealth);
    stream.writeDouble(rho);
  }

}
