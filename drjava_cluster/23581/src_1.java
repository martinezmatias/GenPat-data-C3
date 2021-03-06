package edu.rice.cs.dynamicjava.interpreter;

import java.util.*;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.iter.SequenceIterator;
import edu.rice.cs.plt.lambda.LambdaUtil;
import edu.rice.cs.plt.text.TextUtil;

import edu.rice.cs.dynamicjava.symbol.*;
import edu.rice.cs.dynamicjava.symbol.type.Type;
import edu.rice.cs.dynamicjava.symbol.type.ClassType;

import static edu.rice.cs.plt.debug.DebugUtil.debug;

/**
 * The context at the top level of a source file or local block.  Manages package and
 * import statements.
 */
public class TopLevelContext extends DelegatingContext {

  private final TypeContext _next; // need to save here for making copies
  private final String _currentPackage;
  private final Iterator<Integer> _anonymousCounter;
    
  // The following fields refer to specific implementation classes in order to use the clone() method.
  
  /** Packages whose top-level classes are all imported */
  private final HashSet<String> _onDemandPackages;
  /** Classes whose member classes are all imported */
  private final HashSet<DJClass> _onDemandClasses;
  /** Classes whose static members (fields, methods, and classes) are all imported */
  private final HashSet<DJClass> _staticOnDemandClasses;
  
  /** Top-level classes that are individually imported */
  private final HashMap<String, DJClass> _importedTopLevelClasses;
  /** Classes containing an individually-imported member class */
  private final HashMap<String, DJClass> _importedMemberClasses;
  /** Classes containing an individually-imported field */
  private final HashMap<String, DJClass> _importedFields;
  /** Classes containing an individually-imported method */
  private final HashMap<String, DJClass> _importedMethods;
  
  /**
   * Make a top-level context that delegates to a LibraryContext based on the given class loader.
   * The context is initialized with an on-demand import of "java.lang".
   */
  public TopLevelContext(ClassLoader loader) {
    this(new LibraryContext(SymbolUtil.classLibrary(loader)));
  }
  
  /**
   * Make a top-level context that delegates to the given context.
   * The context is initialized with an on-demand import of "java.lang".
   */
  public TopLevelContext(TypeContext next) {
    super(next);
    _next = next;
    _currentPackage = "";
    _anonymousCounter = new SequenceIterator<Integer>(1, LambdaUtil.INCREMENT_INT);
    _onDemandPackages = new HashSet<String>();
    _onDemandClasses = new HashSet<DJClass>();
    _staticOnDemandClasses = new HashSet<DJClass>();
    _importedTopLevelClasses = new HashMap<String, DJClass>();
    _importedMemberClasses = new HashMap<String, DJClass>();
    _importedFields = new HashMap<String, DJClass>();
    _importedMethods = new HashMap<String, DJClass>();
    
    _onDemandPackages.add("java.lang");
  }
  
  private TopLevelContext(TopLevelContext copy) {
    this(copy._next, copy._currentPackage, copy);
  }
  
  @SuppressWarnings("unchecked")
  private TopLevelContext(TypeContext next, String currentPackage, TopLevelContext bindings) {
    super(next);
    _next = next;
    _currentPackage = currentPackage;
    _anonymousCounter = bindings._anonymousCounter;
    _onDemandPackages = (HashSet<String>) bindings._onDemandPackages.clone();
    _onDemandClasses = (HashSet<DJClass>) bindings._onDemandClasses.clone();
    _staticOnDemandClasses = (HashSet<DJClass>) bindings._staticOnDemandClasses.clone();
    _importedTopLevelClasses = (HashMap<String, DJClass>) bindings._importedTopLevelClasses.clone();
    _importedMemberClasses = (HashMap<String, DJClass>) bindings._importedMemberClasses.clone();
    _importedFields = (HashMap<String, DJClass>) bindings._importedFields.clone();
    _importedMethods = (HashMap<String, DJClass>) bindings._importedMethods.clone();
  }
  
  protected TypeContext duplicate(TypeContext next) {
    return new TopLevelContext(next, _currentPackage, this);
  }

  
  /* PACKAGE AND IMPORT MANAGEMENT */
  
  /** Set the current package to the given package name */
  public TypeContext setPackage(String name) { return new TopLevelContext(_next, name, this); }
  
  /** Import on demand all top-level classes in the given package */
  public TypeContext importTopLevelClasses(String pkg) {
    TopLevelContext result = new TopLevelContext(this);
    result._onDemandPackages.add(pkg);
    return result;
  }
  
  /** Import on demand all member classes of the given class */
  public TypeContext importMemberClasses(DJClass outer) {
    TopLevelContext result = new TopLevelContext(this);
    result._onDemandClasses.add(outer);
    return result;
  }    
  
  /** Import on demand all static members of the given class */
  public TypeContext importStaticMembers(DJClass c) {
    TopLevelContext result = new TopLevelContext(this);
    result._staticOnDemandClasses.add(c);
    return result;
  }
  
  /** Import the given top-level class */
  public TypeContext importTopLevelClass(DJClass c) {
    TopLevelContext result = new TopLevelContext(this);
    String name = c.declaredName();
    // Under strict circumstances, a duplicate import for a name is illegal, but DynamicJava allows it
    result._importedMemberClasses.remove(name);
    result._importedTopLevelClasses.put(name, c);
    return result;
  }
  
  /** Import the member class(es) of {@code outer} with the given name */
  public TypeContext importMemberClass(DJClass outer, String name) {
    TopLevelContext result = new TopLevelContext(this);
    // Under strict circumstances, a duplicate import for a name is illegal, but DynamicJava allows it
    result._importedTopLevelClasses.remove(name);
    result._importedMemberClasses.put(name, outer);
    return result;
  }
  
  /** Import the field(s) of {@code c} with the given name */
  public TypeContext importField(DJClass c, String name) {
    TopLevelContext result = new TopLevelContext(this);
    // Under strict circumstances, a duplicate import for a name is illegal, but DynamicJava allows it
    result._importedFields.put(name, c);
    return result;
  }
  
  /** Import the method(s) of {@code c} with the given name */
  public TypeContext importMethod(DJClass c, String name) {
    TopLevelContext result = new TopLevelContext(this);
    // Under strict circumstances, a duplicate import for a name is illegal, but DynamicJava allows it
    result._importedMethods.put(name, c);
    return result;
  }
    
  
  /* TYPES: TOP-LEVEL CLASSES, MEMBER CLASSES, AND TYPE VARIABLES */
  
  /** Test whether {@code name} is an in-scope top-level class, member class, or type variable */
  public boolean typeExists(String name, TypeSystem ts) {
    return topLevelClassExists(name, ts) || memberClassExists(name, ts) || super.typeVariableExists(name, ts);
  }
  
  /** Test whether {@code name} is an in-scope top-level class */
  public boolean topLevelClassExists(String name, TypeSystem ts) {
    try { return getTopLevelClass(name, ts) != null; }
    catch (AmbiguousNameException e) { return true; }
  }
  
  /**
   * Return the top-level class with the given name, or {@code null} if it does not exist.  If the name 
   * contains a {@code '.'}, it is assumed to be a fully-qualified name; otherwise, the result relies on 
   * the current import and package settings.  Note that a member class may shadow a top-level class, 
   * resulting in a null result here.
   */
  public DJClass getTopLevelClass(String name, TypeSystem ts) throws AmbiguousNameException {
    if (TextUtil.contains(name, '.')) { return super.getTopLevelClass(name, ts); }
    else {
      DJClass result = _importedTopLevelClasses.get(name);
      if (result == null) {
        result = super.getTopLevelClass(makeClassName(name), ts);
        if (result == null) {
          LinkedList<String> onDemandNames = new LinkedList<String>();
          for (String p : _onDemandPackages) {
            String fullName = p + "." + name;
            if (super.topLevelClassExists(fullName, ts)) { onDemandNames.add(fullName); }
          }
          if (onDemandNames.size() > 1) { throw new AmbiguousNameException(); }
          else if (onDemandNames.size() == 1) { result = super.getTopLevelClass(onDemandNames.get(0), ts); }
        }
      }
      return result;
    }
  }
  
  /** Test whether {@code name} is an in-scope member class */
  public boolean memberClassExists(String name, TypeSystem ts) {
    try { return typeContainingMemberClass(name, ts) != null; }
    catch (AmbiguousNameException e) { return true; }
  }
  
  /**
   * Return the most inner type containing a class with the given name, or {@code null}
   * if there is no such type.
   */
  public ClassType typeContainingMemberClass(String name, TypeSystem ts) throws AmbiguousNameException {
    DJClass explicitImport = _importedMemberClasses.get(name);
    ClassType result = explicitImport == null ? null : ts.makeClassType(explicitImport);
    if (result == null) {
      LinkedList<ClassType> onDemandMatches = new LinkedList<ClassType>();
      for (DJClass c : _onDemandClasses) {
        ClassType t = ts.makeClassType(c);
        if (ts.containsClass(t, name)) { onDemandMatches.add(t); }
      }
      for (DJClass c : _staticOnDemandClasses) {
        ClassType t = ts.makeClassType(c);
        if (ts.containsStaticClass(t, name)) { onDemandMatches.add(t); }
      }
      if (onDemandMatches.size() > 1) { throw new AmbiguousNameException(); }
      else if (onDemandMatches.size() == 1) { result = onDemandMatches.getFirst(); }
      if (result == null) {
        result = super.typeContainingMemberClass(name, ts);
      }
    }
    return result;
  }
  
  /* VARIABLES: FIELDS AND LOCAL VARIABLES */  
  
  /** Test whether {@code name} is an in-scope field or local variable */
  public boolean variableExists(String name, TypeSystem ts) {
    return fieldExists(name, ts) || super.localVariableExists(name, ts);
  }
  
  /** Test whether {@code name} is an in-scope field */
  public boolean fieldExists(String name, TypeSystem ts) {
    try { return typeContainingField(name, ts) != null; }
    catch (AmbiguousNameException e) { return true; }
  }
    
  
  /**
   * Return the most inner type containing a field with the given name, or {@code null}
   * if there is no such type.
   */
  public ClassType typeContainingField(String name, TypeSystem ts) throws AmbiguousNameException {
    DJClass explicitImport = _importedFields.get(name);
    ClassType result = explicitImport == null ? null : ts.makeClassType(explicitImport);
    if (result == null) {
      LinkedList<ClassType> onDemandMatches = new LinkedList<ClassType>();
      for (DJClass c : _staticOnDemandClasses) {
        ClassType t = ts.makeClassType(c);
        if (ts.containsStaticField(t, name)) { onDemandMatches.add(t); }
      }
      if (onDemandMatches.size() > 1) { throw new AmbiguousNameException(); }
      else if (onDemandMatches.size() == 1) { result = onDemandMatches.getFirst(); }
      if (result == null) {
        result = super.typeContainingField(name, ts);
      }
    }
    return result;
  }
  
  
  /* METHODS */
  
  /** Test whether {@code name} is an in-scope method or local function */
  public boolean functionExists(String name, TypeSystem ts) {
    return methodExists(name, ts) || super.localFunctionExists(name, ts);
  }
  
  /** Test whether {@code name} is an in-scope method */
  public boolean methodExists(String name, TypeSystem ts) {
    try { return typeContainingMethod(name, ts) != null; }
    catch (AmbiguousNameException e) { return true; }
  }
  
  /**
   * Return the most inner type containing a method with the given name, or {@code null}
   * if there is no such type.
   */
  public ClassType typeContainingMethod(String name, TypeSystem ts) throws AmbiguousNameException {
    DJClass explicitImport = _importedMethods.get(name);
    ClassType result = explicitImport == null ? null : ts.makeClassType(explicitImport);
    if (result == null) {
      LinkedList<ClassType> onDemandMatches = new LinkedList<ClassType>();
      for (DJClass c : _staticOnDemandClasses) {
        ClassType t = ts.makeClassType(c);
        if (ts.containsStaticMethod(t, name)) { onDemandMatches.add(t); }
      }
      if (onDemandMatches.size() > 1) { throw new AmbiguousNameException(); }
      else if (onDemandMatches.size() == 1) { result = onDemandMatches.getFirst(); }
      if (result == null) {
        result = super.typeContainingMethod(name, ts);
      }
    }
    return result;
  }
  
  
  /* MISC CONTEXTUAL INFORMATION */
  
  public String getPackage() { return _currentPackage; }
  
  /** Return a full name for a class with the given name declared here. */
  public String makeClassName(String n) { return _currentPackage.equals("") ? n : _currentPackage + "." + n; }
  
  /** Return a full name for an anonymous class declared here. */
  public String makeAnonymousClassName() { return makeClassName("$" + _anonymousCounter.next().toString()); }
  
  /**
   * Return the type of {@code this} in the current context, or {@code null}
   * if there is no such value (for example, in a static context).
   */
  public DJClass getThis() { return null; }
  
  /**
   * Return the type of {@code className.this} in the current context, or {@code null}
   * if there is no such value (for example, in a static context).
   */
  public DJClass getThis(String className) { return null; }
  
  /**
   * Return the type referenced by {@code super} in the current context, or {@code null}
   * if there is no such type (for example, in a static context).
   */
  public Type getSuperType(TypeSystem ts) { return null; }
  
  /**
   * The expected type of a {@code return} statement in the given context, or {@code null}
   * if {@code return} statements should not appear here.
   */
  public Type getReturnType() { return null; }
  
  /**
   * The types that are allowed to be thrown in the current context.  If there is no
   * such declaration, the list will be empty.
   */
  public Iterable<Type> getDeclaredThrownTypes() {
    // the top level "catches" anything that is thrown.
    return IterUtil.<Type>singleton(TypeSystem.THROWABLE);
  }
  
}
