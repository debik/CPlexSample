package cpx.portfolio.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

/** Utility functions for input/output of data.
 * The files that are written and read by the functions have the following format:
 * - Whitespace at beginning or end of line is ignored
 * - Lines starting with '#' are ignored
 * - Empty lines are ignored.
 * - Any other line must conform to one of the following two
 *     Investment specification: I <id> <return> <name>
 *     Covariance specification: C <id1> <id2> <covariance>
 *   The ordering of these lines is arbitrary.
 */
public class IO {
  /** Load data from a file.
   */
  public static void load(InputStream input, Collection<Investment> investments, Covariance covariance) throws IOException {
    investments.clear();
    covariance.clear();
    
    class Triple {
      public final long id1;
      public final long id2;
      public final double cov;
      public Triple(long id1, long id2, double cov) { this.id1 = id1; this.id2 = id2; this.cov = cov; }
    } 
    final Collection<Triple> triples = new Vector<Triple>();
    final Map<Long, Investment> is = new TreeMap<Long, Investment>();
    
    final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    int lineno = 0;
    for (String rawLine = reader.readLine(); rawLine != null; rawLine = reader.readLine()) {
      String line = rawLine.trim();
      ++lineno;
      // Ignore empty and comment lines
      if (line.length() == 0 || line.startsWith("#"))
        continue;
      if (line.startsWith("I")) {
        final String[] fields = line.split("\\s+", 4);
        if (fields.length != 4)
          throw new IOException("Invalid line " + lineno + ": " + rawLine);
        try {
          final Investment i = new Investment();
          i.setId(Long.parseLong(fields[1]));
          i.setReturn(Double.parseDouble(fields[2]));
          i.setName(fields[3]);
          if (is.containsKey(i.getId()))
            throw new IOException("Line " + lineno + ": investment id " + i.getId() + " already defined");
          is.put(i.getId(), i);
        }
        catch (NumberFormatException e) {
          throw new IOException("Invalid number on line " + lineno + ": " + rawLine);
        }
      }
      else if (line.startsWith("C")) {
        final String[] fields = line.split("\\s+");
        if (fields.length < 4)
          throw new IOException("Invalid line " + lineno + ": " + rawLine);
        try {
          final long id1 = Long.parseLong(fields[1]);
          final long id2 = Long.parseLong(fields[2]);
          final double cov = Double.parseDouble(fields[3]);
          triples.add(new Triple(id1, id2, cov));
        }
        catch (NumberFormatException e) {
          throw new IOException("Invalid number on line " + lineno + ": " + rawLine);
        }
      }
      else
        throw new IOException("Cannot parse line " + lineno + ": " + rawLine);
    }
    
    // Setup the return values. We ignore covariance values for non-existent investements.
    investments.addAll(is.values());
    for (final Triple t : triples) {
      if (is.containsKey(t.id1) && is.containsKey(t.id2))
        covariance.setCovariance(t.id1, t.id2, t.cov);
    }
  }

  /** Save data to a file.
   */
  public static void save(OutputStream output, Collection<Investment> investments, Covariance covariance) throws IOException {
    final Investment[] array = investments.toArray(new Investment[investments.size()]);
    final PrintStream ps = new PrintStream(output);
    for (final Investment i : array) {
      ps.println("I " + i.getId() + " " + i.getReturn() + " " + i.getName());
    }
    for (int i = 0; i < array.length; ++i)
      for (int j = i; j < array.length; ++j)
        ps.println("C " + array[i].getId() + " " + array[j].getId() + " " + covariance.getCovariance(array[i].getId(), array[j].getId()));
  }
}
