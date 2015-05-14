package cpx.portfolio.messages;

import java.util.Collection;
import java.util.Vector;

import com.platform.symphony.soam.InputStream;
import com.platform.symphony.soam.Message;
import com.platform.symphony.soam.OutputStream;
import com.platform.symphony.soam.SoamException;

import cpx.portfolio.data.Investment;

/** Portfolio service output.
 * An instance of this class represents the output of the portfolio service.
 * It either specifies an optimal portfolio allocation or indicates that no
 * feasible allocation was found. In the latter case {@link isOptimal()} will
 * return false and functions {@link #getTotalReturn()} and 
 * {@link #getTotalVariance()} will both return {@link Double#NaN}.
 */
public class Output extends Message {
  
  private boolean optimal = false;
  private double wealth = Double.NaN;
  private double rho = Double.NaN;
  private double objValue = Double.NaN;
  private double totalReturn = Double.NaN;
  private double totalVariance = Double.NaN;
  private Collection<Investment> investments = new Vector<Investment>();
  
  public boolean isOptimal() { return optimal; }
  public void setOptimal(boolean optimal) { this.optimal = optimal; }
  
  public double getWealth() { return wealth; }
  public void setWealth(double wealth) { this.wealth = wealth; }
  
  public double getRho() { return rho; }
  public void setRho(double rho) { this.rho = rho; }
  
  public double getObjValue() { return objValue; }
  public void setObjValue(double objValue) { this.objValue = objValue; }

  public double getTotalReturn() { return totalReturn; }
  public void setTotalReturn(double totalReturn) { this.totalReturn = totalReturn; }

  public double getTotalVariance() { return totalVariance; }
  public void setTotalVariance(double totalVariance) { this.totalVariance = totalVariance; }

  public Collection<Investment> getInvestments() { return investments; }
  public void setInvestments(Collection<Investment> investments) { this.investments = investments; }

  private void clear() {
    optimal = false;
    wealth = Double.NaN;
    rho = Double.NaN;
    objValue = Double.NaN;
    totalReturn = Double.NaN;
    totalVariance = Double.NaN;
    investments = new Vector<Investment>();
  }

  @Override
  public void onDeserialize(InputStream stream) throws SoamException {
    clear();
    boolean doReset = true;
    try {
      optimal = stream.readBoolean();
      wealth = stream.readDouble();
      rho = stream.readDouble();
      objValue = stream.readDouble();
      totalReturn = stream.readDouble();
      totalVariance = stream.readDouble();
      final long size = stream.readLong();
      for (int i = 0; i < size; ++i) {
        final Investment investment = new Investment();
        investment.onDeserialize(stream);
        investments.add(investment);
      }
      doReset = false;
    }
    finally {
      if (doReset)
        clear();
    }
  }

  @Override
  public void onSerialize(OutputStream stream) throws SoamException {
    stream.writeBoolean(optimal);
    stream.writeDouble(wealth);
    stream.writeDouble(rho);
    stream.writeDouble(objValue);
    stream.writeDouble(totalReturn);
    stream.writeDouble(totalVariance);
    stream.writeLong(investments.size());
    for (final Investment i : investments)
      i.onSerialize(stream);
  }

}
