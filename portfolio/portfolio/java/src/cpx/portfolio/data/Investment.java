package cpx.portfolio.data;

import com.platform.symphony.soam.InputStream;
import com.platform.symphony.soam.OutputStream;
import com.platform.symphony.soam.SoamException;

/** Investment descriptor.
 * Instances of this class describe an investment in the input and the
 * output of the service. When used as input the service then the allocation
 * is usually {@link Double#NaN}. When used as output from the service the
 * allocation specifies the amount of this investment in the optimal portfolio
 * allocation.
 * Each investment has a unique id that identifies it. The name of an
 * investment is only used for display.
 */
public final class Investment {
  /** Unique id of this investment. */
  private long id;
  /** The expected return for this investment. */
  private double ret;
  /** The name of this investment. */
  private String name;
  /** The suggested allocation for this investment. */
  private double allocation;
  
  /** Reset all values in this investment to default values. */
  private void reset() {
    id = -1;
    ret = Double.NaN;
    name = "";
    allocation = Double.NaN;
  }
  
  public Investment() { reset(); }
  
  public long getId() { return id; }
  public void setId(long id) { this.id = id; }
  
  public double getReturn() { return ret; }
  public void setReturn(double ret) { this.ret = ret; }
  
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  public double getAllocation() { return allocation; }
  public void setAllocation(double allocation) { this.allocation = allocation; }
  
  /** Initialize this investment from <code>stream</code>.
   * In case of an exception all fields in this instance will be reset to their
   * default values.
   * @param stream The stream from which to initialize.
   * @throws SoamException if initialization fails.
   */
  public void onDeserialize(InputStream stream) throws SoamException {
    reset();
    boolean doReset = true;
    try {
      id = stream.readLong();
      name = stream.readString();
      ret = stream.readDouble();
      allocation = stream.readDouble();
      doReset = false;
    }
    finally {
      if (doReset)
        reset();
    }
  }

  /** Write this investment to <code>stream</code>.
   * @param stream The output stream.
   * @throws SoamException if serialization or output fails.
   */
  public void onSerialize(OutputStream stream) throws SoamException {
    stream.writeLong(id);
    stream.writeString(name);
    stream.writeDouble(ret);
    stream.writeDouble(allocation);
  }
}
