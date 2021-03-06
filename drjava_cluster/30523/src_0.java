package edu.rice.cs.plt.recur;

import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import edu.rice.cs.plt.tuple.IdentityWrapper;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.LambdaUtil;

/**
 * <p>A stack used to store the arguments of a recursive invocation in order to prevent
 * infinite recursion.  By checking that a given argument has not been used previously before
 * recurring, a client can prevent infinite recursion in some circumstances (such as
 * when traversing an infinite, immutable data structure).</p>
 * 
 * <p>While {@link RecursionStack} allows arbitrary application result values to be provided 
 * for the infinite case, this class follows a stricter discipline: the infinite case result
 * must be provided at the time of the <em>first</em> invocation of an argument; that value
 * will be stored, and a second invocation will return it.  In this way, the result of
 * a recursive computation is always precomputed -- that is, it must be determined before 
 * the computation takes place.  Classes like {@link edu.rice.cs.plt.lambda.DelayedThunk} can be 
 * used to create precomputed values, providing an initial "empty box" that can be "filled" when 
 * computation is complete.  This allows the definition, for example, of data structures that 
 * contain themselves.  Due to the restricted applicability of this class (in comparison to
 * {@code RecursionStack}), methods that involve invoking {@code Command}s or recurring multiple
 * times based on a threshold value are not defined here.</p>
 * 
 * <p>The client may either choose to explicity check for containment, {@link #push} the argument, 
 * recur, and then {@link #pop}, or invoke one of a variety of lambda-based methods that perform 
 * these bookkeeping tasks automatically.  In the latter case, when an exception occurs between
 * a {@code push} and a matching {@code pop}, the {@code pop} is guaranteed to execute before 
 * the exception propagates upward.  Thus, clients who do not directly invoke {@link #push}
 * and {@link #pop} may assume that the stack is always in a consistent state.</p>
 * 
 * @see RecursionStack
 * @see PrecomputedRecursionStack2
 * @see PrecomputedRecursionStack3
 * @see PrecomputedRecursionStack4
 */
public class PrecomputedRecursionStack<T, R> {
  
  private Map<IdentityWrapper<T>, Lambda<? super T, ? extends R>> _previous;
  private LinkedList<IdentityWrapper<T>> _stack;
  
  /** Create an empty recursion stack */
  public PrecomputedRecursionStack() {
    _previous = new HashMap<IdentityWrapper<T>, Lambda<? super T, ? extends R>>();
    _stack = new LinkedList<IdentityWrapper<T>>();
  }
  
  /** 
   * @return  {@code true} iff a value identical (according to {@code ==}) to {@code arg}
   *          is currently on the stack
   */
  public boolean contains(T arg) { return _previous.containsKey(new IdentityWrapper<T>(arg)); }
  
  /** 
   * @return  The infinite-case result provided for {@code arg}
   * @throws  IllegalStateException  If {@code arg} is not on the stack
   */
  public R get(T arg) {
    Lambda<? super T, ? extends R> result = _previous.get(new IdentityWrapper<T>(arg));
    if (result == null) { throw new IllegalArgumentException("Value is not on the stack"); }
    return result.value(arg);
  }
  
  /**
   * Add {@code arg} to the top of the stack with the given infinite-case result.
   * @throws IllegalArgumentException  If {@code arg} is already on the stack
   */
  public void push(T arg, R value) { push(arg, LambdaUtil.valueLambda(value)); }
  
  /**
   * Add {@code arg} to the top of the stack with the given thunk producing its infinite-case result.
   * @throws IllegalArgumentException  If {@code arg} is already on the stack
   */
  public void push(T arg, Thunk<? extends R> value) { push(arg, LambdaUtil.promote(value)); }
  
  /**
   * Add {@code arg} to the top of the stack with the given lambda producing its infinite-case result.
   * @throws IllegalArgumentException  If {@code arg} is already on the stack
   */
  public void push(T arg, Lambda<? super T, ? extends R> value) {
    IdentityWrapper<T> wrapped = new IdentityWrapper<T>(arg);
    if (_previous.containsKey(wrapped)) {
      throw new IllegalArgumentException("arg is already on the stack");
    }
    _stack.addLast(wrapped);
    _previous.put(wrapped, value);
  }
  
  /** 
   * Remove {@code arg} from the top of the stack
   * @throws IllegalArgumentException  If {@code arg} is not at the top of the stack
   */
  public void pop(T arg) {
    IdentityWrapper<T> wrapped = new IdentityWrapper<T>(arg);
    if (_stack.isEmpty() || !_stack.getLast().equals(wrapped)) {
      throw new IllegalArgumentException("arg is not on top of the stack");
    }
    _stack.removeLast();
    _previous.remove(wrapped);
  }
  
  /** @return  The current size (depth) of the stack */
  public int size() { return _stack.size(); }
  
  /** @return  {@code true} iff the stack is currently empty */
  public boolean isEmpty() { return _stack.isEmpty(); }
  
  /**
   * Evaluate the given thunk, unless {@code arg} is already on the stack; push {@code arg}
   * onto the stack with the given precomputed result during {@code thunk}'s evaluation
   * 
   * @return  The value of {@code thunk}, or a previously-provided precomputed value
   */
  public R apply(Thunk<? extends R> thunk, R precomputed, T arg) {
    if (!contains(arg)) { 
      push(arg, precomputed);
      try { return thunk.value(); }
      finally { pop(arg); }
    }
    else { return get(arg); }
  }
  
  /**
   * Evaluate the given thunk, unless {@code arg} is already on the stack; push {@code arg}
   * onto the stack with the given precomputed result during {@code thunk}'s evaluation
   * 
   * @return  The value of {@code thunk}, or a previously-provided precomputed value
   */
  public R apply(Thunk<? extends R> thunk, Thunk<? extends R> precomputed, T arg) {
    if (!contains(arg)) { 
      push(arg, precomputed);
      try { return thunk.value(); }
      finally { pop(arg); }
    }
    else { return get(arg); }
  }
  
  /**
   * Evaluate the given thunk, unless {@code arg} is already on the stack; push {@code arg}
   * onto the stack with the given precomputed result during {@code thunk}'s evaluation
   * 
   * @return  The value of {@code thunk}, or a previously-provided precomputed value
   */
  public R apply(Thunk<? extends R> thunk, Lambda<? super T, ? extends R> precomputed, T arg) {
    if (!contains(arg)) { 
      push(arg, precomputed);
      try { return thunk.value(); }
      finally { pop(arg); }
    }
    else { return get(arg); }
  }
  
  /**
   * Evaluate the given lambda with argument {@code arg}, unless {@code arg} is already on the 
   * stack; push {@code arg} onto the stack with the given precomputed result during 
   * {@code lambda}'s evaluation
   * 
   * @return  The value of {@code lambda}, or a previously-provided precomputed value
   */
  public R apply(Lambda<? super T, ? extends R> lambda, R precomputed, T arg) {
    if (!contains(arg)) { 
      push(arg, precomputed);
      try { return lambda.value(arg); }
      finally { pop(arg); }
    }
    else { return get(arg); }
  }
  
  /**
   * Evaluate the given lambda with argument {@code arg}, unless {@code arg} is already on the 
   * stack; push {@code arg} onto the stack with the given precomputed result during 
   * {@code lambda}'s evaluation
   * 
   * @return  The value of {@code lambda}, or a previously-provided precomputed value
   */
  public R apply(Lambda<? super T, ? extends R> lambda, Thunk<? extends R> precomputed, T arg) {
    if (!contains(arg)) { 
      push(arg, precomputed);
      try { return lambda.value(arg); }
      finally { pop(arg); }
    }
    else { return get(arg); }
  }
  
  /**
   * Evaluate the given lambda with argument {@code arg}, unless {@code arg} is already on the 
   * stack; push {@code arg} onto the stack with the given precomputed result during 
   * {@code lambda}'s evaluation
   * 
   * @return  The value of {@code lambda}, or a previously-provided precomputed value
   */
  public R apply(Lambda<? super T, ? extends R> lambda, Lambda<? super T, ? extends R> precomputed, 
                     T arg) {
    if (!contains(arg)) { 
      push(arg, precomputed);
      try { return lambda.value(arg); }
      finally { pop(arg); }
    }
    else { return get(arg); }
  }
    
  /** Call the constructor (allows the type arguments to be inferred) */
  public static <T, R> PrecomputedRecursionStack<T, R> make() {
    return new PrecomputedRecursionStack<T, R>();
  }
  
}
