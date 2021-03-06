package edu.rice.cs.dynamicjava.symbol;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.*;

import edu.rice.cs.dynamicjava.Options;
import edu.rice.cs.dynamicjava.symbol.type.*;
import edu.rice.cs.dynamicjava.symbol.type.Type; // resolves ambiguity with java.lang.reflect.Type
import edu.rice.cs.dynamicjava.interpreter.RuntimeBindings;
import edu.rice.cs.dynamicjava.interpreter.EvaluatorException;

import edu.rice.cs.plt.reflect.ReflectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.lambda.Box;
import edu.rice.cs.plt.lambda.LazyThunk;
import edu.rice.cs.plt.lambda.WrappedException;

import static edu.rice.cs.plt.debug.DebugUtil.debug;

/**
 * DJClass implementation that wraps a Java reflection Class object.  The {@link Java5Class} version
 * should be used instead if the class object supports Java 5 methods like
 * {@link Class#getTypeParameters}.
 */
public class JavaClass implements DJClass {
  
  protected Class<?> _c;
  
  public JavaClass(Class<?> c) { _c = c; }
  
  public String packageName() {
    String name = _c.getName();
    int dot = name.lastIndexOf('.');
    if (dot == -1) { return ""; }
    else { return name.substring(0, dot); }
  }
  
  /** Produces the binary name for the given class (as in {@link Class#getName}) */
  public String fullName() { return _c.getName(); }
  
  public boolean isAnonymous() { return ReflectUtil.isAnonymousClass(_c); }
  
  public String declaredName() {
    if (ReflectUtil.isAnonymousClass(_c)) {
      throw new IllegalArgumentException("Anonymous class has no declared name");
    }
    else { return ReflectUtil.simpleName(_c); }
  }
  
  public boolean isInterface() { return Modifier.isInterface(_c.getModifiers()); }
  public boolean isStatic() { return Modifier.isStatic(_c.getModifiers()); }
  public boolean isAbstract() { return Modifier.isAbstract(_c.getModifiers()); }
  public boolean isFinal() { return Modifier.isFinal(_c.getModifiers()); }
  public Access accessibility() { return extractAccessibility(_c.getModifiers()); }  
  public boolean hasRuntimeBindingsParams() { return false; }
  
  public Access.Module accessModule() {
    // Reflection API (1.4) doesn't tell us enclosing classes, so we're limited to declaring classes
    Class<?> result = _c;
    Class<?> outer = result.getDeclaringClass();
    while (outer != null) { result = outer; outer = result.getDeclaringClass(); }
    return new JavaClass(result);
  }

  public DJClass declaringClass() {
    Class<?> outer = _c.getDeclaringClass();
    return (outer == null) ? null : new JavaClass(outer);
  }
  
  /** List all type variables declared by this class (but not by its enclosing classes) */
  public Iterable<VariableType> declaredTypeParameters() { return IterUtil.empty(); }
  
  /** List the declared supertypes of this class */
  public Iterable<Type> declaredSupertypes() {
    Type superC = immediateSuperclass();
    Iterable<Type> superIs;
    if (_c.getInterfaces() == null) { superIs = IterUtil.empty(); }
    else { superIs = IterUtil.mapSnapshot(IterUtil.asIterable(_c.getInterfaces()), CLASS_AS_TYPE); }
    return superC == null ? superIs : IterUtil.compose(superC, superIs);
  }
  
  public Iterable<DJField> declaredFields() {
    return IterUtil.mapSnapshot(IterUtil.asIterable(_c.getDeclaredFields()), CONVERT_FIELD);
  }
  
  public Iterable<DJConstructor> declaredConstructors() {
    return IterUtil.mapSnapshot(IterUtil.asIterable(_c.getDeclaredConstructors()), CONVERT_CONSTRUCTOR);
  }
  
  public Iterable<DJMethod> declaredMethods() {
    return IterUtil.mapSnapshot(IterUtil.asIterable(_c.getDeclaredMethods()), CONVERT_METHOD);
  }
  
  public Iterable<DJClass> declaredClasses() {
    return IterUtil.mapSnapshot(IterUtil.asIterable(_c.getDeclaredClasses()), CONVERT_CLASS);
  }
  

  /**
   * Return the type bound to {@code super} in the context of this class, or 
   * {@code null} if {@code super} is not defined
   */
  public Type immediateSuperclass() {
    Class<?> superT = _c.getSuperclass();
    return (superT == null) ? null : classAsType(superT);
  }
  
  public Class<?> load() { return _c; }
  
  public String toString() { return "JavaClass(" + _c.getName() + ")"; }
  
  public boolean equals(Object o) {
    if (this == o) { return true; }
    else if (!o.getClass().equals(getClass())) { return false; }
    else { return _c.equals(((JavaClass) o)._c); }
  }
  
  public int hashCode() { return (getClass().hashCode() << 1) ^ _c.hashCode(); }
  
  /** Convert a class object to a type */
  private static Type classAsType(Class<?> c) {
    if (c.isPrimitive()) { return SymbolUtil.typeOfPrimitiveClass(c); }
    else if (c.isArray()) { return new SimpleArrayType(classAsType(c.getComponentType())); }
    else { return new SimpleClassType(new JavaClass(c)); }
  }
  
  @SuppressWarnings("unchecked") // java.lang.Class methods return (raw) type Class[] in Java 5 (fixed in Java 6)
  private static final Lambda<Class, Type> CLASS_AS_TYPE = new Lambda<Class, Type>() {
    public Type value(Class c) {
      return classAsType(c);
    }
  };
  
  @SuppressWarnings("unchecked") // java.lang.Class methods return (raw) type Class[] in Java 5 (fixed in Java 6)
  private static final Lambda<Class, DJClass> CONVERT_CLASS = new Lambda<Class, DJClass>() {
    public DJClass value(Class c) { return new JavaClass(c); }
  };
  
  /** Non-static because JavaField is non-static. */
  private final Lambda<Field, DJField> CONVERT_FIELD = new Lambda<Field, DJField>() {
    public DJField value(Field f) { return new JavaField(f); }
  };
  
  /** Non-static because JavaConstructor is non-static. */
  @SuppressWarnings("unchecked") // java.lang.Class methods return (raw) type Constructor[] in Java 5 (fixed in Java 6)
  private final Lambda<Constructor, DJConstructor> CONVERT_CONSTRUCTOR =
    new Lambda<Constructor, DJConstructor>() {
    public DJConstructor value(Constructor k) { return new JavaConstructor(k); }
  };
  
  /** Non-static because JavaMethod is non-static. */
  private final Lambda<Method, DJMethod> CONVERT_METHOD = new Lambda<Method, DJMethod>() {
    public DJMethod value(Method m) { return new JavaMethod(m); }
  };

  protected class JavaField implements DJField {
    protected final Field _f;
    public JavaField(Field f) { _f = f; }
    public String declaredName() { return _f.getName(); }
    public Type type() { return classAsType(_f.getType()); }
    public boolean isFinal() { return Modifier.isFinal(_f.getModifiers()); }
    public boolean isStatic() { return Modifier.isStatic(_f.getModifiers()); }
    public Access accessibility() { return extractAccessibility(_f.getModifiers()); }
    public Access.Module accessModule() { return JavaClass.this.accessModule(); }
    
    public Box<Object> boxForReceiver(final Object receiver) {
      return new Box<Object>() {

        public Object value() {
          if (!isStatic() && receiver == null) {
            throw new WrappedException(new EvaluatorException(new NullPointerException()));
          }
          try { _f.setAccessible(true); }
          catch (SecurityException e) { debug.log(e); /* ignore -- we can't relax accessibility */ }
          try { return _f.get(receiver); }
          catch (IllegalAccessException e) {
            // should have been caught by static analysis
            throw new RuntimeException(e);
          }
          catch (LinkageError e) {
            // may be ExceptionInInitializerError, NoClassDefFoundError, etc.
            throw new WrappedException(new EvaluatorException(e, FIELD_GET_EXTRA_STACK));
          }
        }
        
        public void set(Object o) {
          if (!isStatic() && receiver == null) {
            throw new WrappedException(new EvaluatorException(new NullPointerException()));
          }
          try { _f.setAccessible(true); }
          catch (SecurityException e) { debug.log(e); /* ignore -- we can't relax accessibility */ }
          try { _f.set(receiver, o); }
          catch (IllegalAccessException e) {
            // should have been caught by static analysis
            throw new RuntimeException(e);
          }
          catch (LinkageError e) {
            // may be ExceptionInInitializerError, NoClassDefFoundError, etc.
            throw new WrappedException(new EvaluatorException(e, FIELD_SET_EXTRA_STACK));
          }
        }
        
      };
    }
      
    public String toString() { return "JavaField(" + declaredName() + ")"; }
}

  private static final String[] FIELD_GET_EXTRA_STACK =
    new String[]{ "java.lang.reflect.Field.get",
                  "java.lang.reflect.Field.getFieldAccessor",
                  "java.lang.reflect.Field.acquireFieldAccessor",
                  "sun.reflect.ReflectionFactory.newFieldAccessor",
                  "sun.reflect.UnsafeFieldAccessorFactory.newFieldAccessor",
                  "sun.misc.Unsafe.ensureClassInitialized" };

  private static final String[] FIELD_SET_EXTRA_STACK =
    new String[]{ "java.lang.reflect.Field.set",
                  "java.lang.reflect.Field.getFieldAccessor",
                  "java.lang.reflect.Field.acquireFieldAccessor",
                  "sun.reflect.ReflectionFactory.newFieldAccessor",
                  "sun.reflect.UnsafeFieldAccessorFactory.newFieldAccessor",
                  "sun.misc.Unsafe.ensureClassInitialized" };  
  
  
  /** Non-static in order to determine the outer type. */
  protected class JavaConstructor implements DJConstructor {
    protected final Constructor<?> _k;
    protected final Type _outerType;
    private final Thunk<Iterable<LocalVariable>> _params;
    
    public JavaConstructor(Constructor<?> k) {
      _k = k;
      DJClass outer = SymbolUtil.dynamicOuterClass(JavaClass.this);
      _outerType = (outer == null) ? null : SymbolUtil.thisType(outer);
      _params = makeParamThunk(); /* allows overriding */
    }
    
    public String declaredName() { return JavaClass.this.declaredName(); }
    public Access accessibility() { return extractAccessibility(_k.getModifiers()); }
    public Access.Module accessModule() { return JavaClass.this.accessModule(); }
    protected Thunk<Iterable<LocalVariable>> makeParamThunk() { return paramFactory(_k.getParameterTypes()); }
    public Iterable<VariableType> declaredTypeParameters() { return IterUtil.empty(); }
    
    public Iterable<LocalVariable> declaredParameters() {
      Iterable<LocalVariable> result = _params.value();
      if (_outerType != null) { result = IterUtil.skipFirst(result); }
      return result;
    }
    
    public Type returnType() { return SymbolUtil.thisType(JavaClass.this); }
    
    public Iterable<Type> thrownTypes() {
      return IterUtil.mapSnapshot(IterUtil.asIterable(_k.getExceptionTypes()), CLASS_AS_TYPE);
    }
    
    public Object evaluate(Object outer, Iterable<Object> args, RuntimeBindings bindings, Options options) 
      throws EvaluatorException {
      if (_outerType != null) {
        if (outer == null) {
          throw new WrappedException(new EvaluatorException(new NullPointerException()));
        }
        args = IterUtil.compose(outer, args);
      }
      
      try { _k.setAccessible(true); }
      catch (SecurityException e) { debug.log(e); /* ignore -- we can't relax accessibility */ }
      
      try {
        return _k.newInstance(IterUtil.toArray(args, Object.class));
      }
      catch (InvocationTargetException e) {
        throw new EvaluatorException(e.getCause(), CONSTRUCTOR_EXTRA_STACK);
      }
      catch (LinkageError e) {
        // may be ExceptionInInitializerError, NoClassDefFoundError, etc.
        throw new EvaluatorException(e, CONSTRUCTOR_EXTRA_STACK);
      }
      catch (IllegalAccessException e) {
        // This should have been caught by static analysis
        throw new RuntimeException(e);
      }
      catch (InstantiationException e) {
        // This should have been caught by static analysis
        throw new RuntimeException(e);
      }
    }
    
    public String toString() { return "JavaConstructor(" + declaredName() + ")"; }
  }
  
  private static final String[] CONSTRUCTOR_EXTRA_STACK =
    new String[]{ "java.lang.reflect.Constructor.newInstance",
                  "sun.reflect.DelegatingConstructorAccessorImpl.newInstance",
                  "sun.reflect.NativeConstructorAccessorImpl.newInstance",
                  "sun.reflect.NativeConstructorAccessorImpl.newInstance0" };
  
  
  protected class JavaMethod implements DJMethod {
    protected final Method _m;
    private final Thunk<Iterable<LocalVariable>> _params;
    public JavaMethod(Method m) { _m = m; _params = makeParamThunk(); /* allows overriding */ }
    protected Thunk<Iterable<LocalVariable>> makeParamThunk() { return paramFactory(_m.getParameterTypes()); }
    public String declaredName() { return _m.getName(); }
    public boolean isStatic() { return Modifier.isStatic(_m.getModifiers()); }
    public boolean isAbstract() { return Modifier.isAbstract(_m.getModifiers()); }
    public boolean isFinal() { return Modifier.isFinal(_m.getModifiers()); }
    public Access accessibility() { return extractAccessibility(_m.getModifiers()); }
    public Access.Module accessModule() { return JavaClass.this.accessModule(); }
    public Type returnType() { return classAsType(_m.getReturnType()); }
    public Iterable<VariableType> declaredTypeParameters() { return IterUtil.empty(); }
    public Iterable<LocalVariable> declaredParameters() { return _params.value(); }
    public Iterable<Type> thrownTypes() {
      return IterUtil.mapSnapshot(IterUtil.asIterable(_m.getExceptionTypes()), CLASS_AS_TYPE);
    }
    
    public Object evaluate(Object receiver, Iterable<Object> args, RuntimeBindings bindings, 
                           Options options) throws EvaluatorException {
      if (!isStatic() && receiver == null) {
        throw new WrappedException(new EvaluatorException(new NullPointerException()));
      }
      
      try { _m.setAccessible(true); }
      catch (SecurityException e) { debug.log(e); /* ignore -- we can't relax accessibility */ }
      
      try {
        return _m.invoke(receiver, IterUtil.toArray(args, Object.class));
      }
      catch (InvocationTargetException e) {
        throw new EvaluatorException(e.getCause(), METHOD_EXTRA_STACK);
      }
      catch (LinkageError e) {
        // may be ExceptionInInitializerError, NoClassDefFoundError, etc.
        throw new EvaluatorException(e, METHOD_EXTRA_STACK);
      }
      catch (IllegalAccessException e) {
        // This should have been caught by static analysis
        throw new RuntimeException(e);
      }
    }
    
    public String toString() { return "JavaMethod(" + declaredName() + ")"; }
  }
  
  private static final String[] METHOD_EXTRA_STACK =
    new String[] { "java.lang.reflect.Method.invoke",
                   "sun.reflect.DelegatingMethodAccessorImpl.invoke",
                   "sun.reflect.NativeMethodAccessorImpl.invoke",
                   "sun.reflect.NativeMethodAccessorImpl.invoke0" };
  
  private static Thunk<Iterable<LocalVariable>> paramFactory(final Class<?>[] cs) {
    // Caches LocalVariables so we don't create duplicates
    return LazyThunk.make(new Thunk<Iterable<LocalVariable>>() {
      public Iterable<LocalVariable> value() {
        List<LocalVariable> result = new ArrayList<LocalVariable>(cs.length);
        // TODO: can we access better information about the parameters -- names, final declarations?
        int argNum = 1;
        for (Class<?> c : cs) { result.add(new LocalVariable("a" + argNum++, classAsType(c), false)); }
        // Must wrap result in something that implements Iterable for Retroweaver compatibility:
        // normally, it's not a problem; but erasure inserts casts to the thunk's parameter type,
        // is translated to the Retroweaver Iterable type.
        return IterUtil.asSizedIterable(result);
      }
    });
  }
  
  /** Convert a reflection modifier int to an appropriate Access object */
  private static Access extractAccessibility(int mods) {
    if (Modifier.isPublic(mods)) { return Access.PUBLIC; }
    else if (Modifier.isProtected(mods)) { return Access.PROTECTED; }
    else if (Modifier.isPrivate(mods)) { return Access.PRIVATE; }
    else { return Access.PACKAGE; }
  }

}
