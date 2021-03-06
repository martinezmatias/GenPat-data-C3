/*BEGIN_COPYRIGHT_BLOCK*

PLT Utilities BSD License

Copyright (c) 2007-2008 JavaPLT group at Rice University
All rights reserved.

Developed by:   Java Programming Languages Team
                Rice University
                http://www.cs.rice.edu/~javaplt/

Redistribution and use in source and binary forms, with or without modification, are permitted 
provided that the following conditions are met:

    - Redistributions of source code must retain the above copyright notice, this list of conditions 
      and the following disclaimer.
    - Redistributions in binary form must reproduce the above copyright notice, this list of 
      conditions and the following disclaimer in the documentation and/or other materials provided 
      with the distribution.
    - Neither the name of the JavaPLT group, Rice University, nor the names of the library's 
      contributors may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR 
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS AND 
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*END_COPYRIGHT_BLOCK*/

package edu.rice.cs.plt.iter;

import java.util.Iterator;
import java.io.Serializable;

/**
 * Contains all but the first element of a wrapped iterable.  (If the wrapped iterable is
 * empty, this is empty as well.)  Changes made to the underlying list are reflected here.
 * This provides a general, but clumsy, way to decompose arbitrary iterables (that is, access 
 * the "rest" of some iterable).  Care should be taken in using this approach, however, as the 
 * first value is skipped on <em>every</em> invocation of {@code iterator}.  Thus, an iterable 
 * composed of multiple nested {@code SkipFirstIterable}s will have poor performance in 
 * comparison to other solutions.  For better performance or recursive list-decomposing 
 * algorithms, use a {@link edu.rice.cs.plt.collect.ConsList}.
 */
public class SkipFirstIterable<T> extends AbstractIterable<T>
                                  implements SizedIterable<T>, OptimizedLastIterable<T>, Serializable {
  
  private final Iterable<T> _iterable;
  
  public SkipFirstIterable(Iterable<T> iterable) { _iterable = iterable; }

  public Iterator<T> iterator() {
    Iterator<T> result = _iterable.iterator();
    if (result.hasNext()) { result.next(); }
    return result;
  }
  
  public boolean isEmpty() { return IterUtil.sizeOf(_iterable, 2) < 2; }
  
  public int size() {
    // This can't be implemented in a strictly correct manner.  If the nestedSize is MAX_VALUE,
    // that means the size of this iterable is >= MAX_VALUE-1.  If we return MAX_VALUE-1, we're
    // asserting that it is *equal to* that size; if we return MAX_VALUE, we're asserting it's 
    // size is *greater than* that size.  There's no way to communicate the *greater than or 
    // equal* result.
    int nestedSize = IterUtil.sizeOf(_iterable);
    if (nestedSize == 0) { return 0; }
    else if (nestedSize == Integer.MAX_VALUE) { return Integer.MAX_VALUE; }
    else { return nestedSize - 1; }
  }
  
  public int size(int bound) {
    if (bound == Integer.MAX_VALUE) { return size(); }
    else {
      int nestedSize = IterUtil.sizeOf(_iterable, bound + 1);
      return (nestedSize == 0) ? 0 : nestedSize - 1;
    }
  }
  
  public boolean isInfinite() { return IterUtil.isInfinite(_iterable); }
  
  public boolean hasFixedSize() { return IterUtil.hasFixedSize(_iterable); }
  
  public boolean isStatic() { return IterUtil.isStatic(_iterable); }
  
  public T last() {
    // assert that there is at least one element
    IterUtil.first(this);
    return IterUtil.last(_iterable);
  }
  
  /** Call the constructor (allows {@code T} to be inferred) */
  public static <T> SkipFirstIterable<T> make(Iterable<T> iterable) {
    return new SkipFirstIterable<T>(iterable);
  }
  
  /**
   * Create a {@code SkipFirstIterable} and wrap it in a {@code SnapshotIterable}, forcing
   * immediate traversal of the list.
   */
  public static <T> SnapshotIterable<T> makeSnapshot(Iterable<T> iterable) { 
    return new SnapshotIterable<T>(new SkipFirstIterable<T>(iterable));
  }
}
