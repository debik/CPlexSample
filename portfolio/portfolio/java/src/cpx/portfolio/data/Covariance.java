package cpx.portfolio.data;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.platform.symphony.soam.InputStream;
import com.platform.symphony.soam.OutputStream;
import com.platform.symphony.soam.SoamException;

/** Covariance descriptor.
 * Instances of this class are used as input to the portfolio optimization service.
 * This class represents a covariance matrix my means of a map that is indexed by
 * pairs of investment ids.
 */
public final class Covariance {
  /** A key in the covariance matrix.
   * The covariance matrix is stored as sparse matrix, that is, as a list of
   * triples (row, column, value). To make updates easier the triples are sorted
   * by (row,column). Also, since the matrix is symmetric, only one of the two
   * (row,column,value) and (column,row,value) triples is stored.
   */
  private static final class Key implements Comparable<Key> {
    private final long first;
    private final long second;
    public Key(long first, long second) {
      if (first > second) {
        this.first = second;
        this.second = first;
      }
      else {
        this.first = first;
        this.second = second;
      }
    }
    @Override
    public int compareTo(Key other) {
      if (first < other.first)
        return -1;
      else if (first > other.first)
        return 1;
      else if (second < other.second)
        return -1;
      else if (second > other.second)
        return 1;
      else
        return 0;
    }
  }

  /** The non-zeros in the covariance matrix. */
  private final Map<Key, Double> map = new TreeMap<Key, Double>();
  /** Delete all non-zeros from the covariance matrix. */
  public void clear() { map.clear(); }
  
  /** Remove all covariance values for the investment identified by <code>id</code>. */
  public void remove(long id) {
    for (Iterator<Map.Entry<Key, Double>> it = map.entrySet().iterator(); it.hasNext(); /* nothing */) {
      final Key k = it.next().getKey();
      if (k.first == id || k.second == id)
        it.remove();
    }
  }
  
  // Set/get covariance for a pair of investments.
  // The investments to query are given by their id. The order does not
  // matter since a covariance matrix is symmetric.
  // If no covariance is stored for the specified pair the get-function
  // returns NaN.
  public void setCovariance(long i1, long i2, double covariance) {
    map.put(new Key(i1, i2), covariance);
  }
  public double getCovariance(long i1, long i2) {
    final Double d = map.get(new Key(i1, i2));
    return (d != null) ? d.doubleValue() : Double.NaN;
  }
  
  /** Copy the non-zeros from this covariance matrix into <code>other</code>. */
  public void copy(Covariance other) {
    map.clear();
    for (final Map.Entry<Key, Double> entry : other.map.entrySet())
      setCovariance(entry.getKey().first, entry.getKey().second, entry.getValue());
  }
  
  /** Load this covariance matrix from a Symphony input stream.
   * The function first calls {@link #clear()} and then starts reading <code>stream</code>.
   * In case of an exception {@link #clear()} is called again, so that the matrix is
   * always in a consistent state.
   * @param stream The stream from which to load the matrix.
   * @throws SoamException on input error.
   */
  public void onDeserialize(InputStream stream) throws SoamException {
    clear();
    boolean doReset = true;
    try {
      final long size = stream.readLong();
      for (long i = 0; i < size; ++i) {
        final long first = stream.readLong();
        final long second = stream.readLong();
        final double value = stream.readDouble();
        setCovariance(first, second, value);
      }
      doReset = false;
    }
    finally {
      if (doReset)
        clear();
    }
  }

  /** Write this covariance matrix to a Symphony stream.
   * @param stream The stream to which to write.
   * @throws SoamException on output error.
   */
  public void onSerialize(OutputStream stream) throws SoamException {
    stream.writeLong(map.size());
    for (final Map.Entry<Key, Double> e : map.entrySet()) {
      stream.writeLong(e.getKey().first);
      stream.writeLong(e.getKey().second);
      stream.writeDouble(e.getValue());
    }
  }
}
