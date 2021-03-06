/* $Id$ */

package edu.rice.cs.drjava;

import java.util.HashMap;

import java.io.StringReader;
import java.io.IOException;
import java.io.InputStream;

import koala.dynamicjava.interpreter.Interpreter;
import koala.dynamicjava.interpreter.InterpreterException;
import koala.dynamicjava.interpreter.TreeInterpreter;
import koala.dynamicjava.interpreter.TreeClassLoader;

import koala.dynamicjava.interpreter.context.GlobalContext;

import koala.dynamicjava.parser.wrapper.JavaCCParserFactory;

public class DynamicJavaAdapter implements JavaInterpreter {
  private Interpreter _djInterpreter;
  private static final ClassLoadChecker _checker = new ClassLoadChecker();

  public DynamicJavaAdapter() {
    _djInterpreter = new InterpreterExtension();

    // Allow access to private fields/methods from interpreter!
    _djInterpreter.setAccessible(true);
  }

  public Object interpret(String s) {

		boolean print = false;
		
		/**
		 * trims the whitespace from beginning and end of string
		 * checks the end to see if it is a semicolon
		 * adds a semicolon if necessary
		 */
		s = s.trim();

		if(!s.endsWith(";")) {
			s += ";";
			print = true;
		}
		
		StringReader reader = new StringReader(s);

    try {
			Object result = _djInterpreter.interpret(reader, "DrJava");
			if(print)
				return result;
			else
				return JavaInterpreter.NO_RESULT;
    }
    catch (Throwable ie) {
			//System.out.println("\n\ngotinterpreter exception\n\n");
      throw new RuntimeException(ie.getMessage());
    }
  }

  public void addClassPath(String path) {
    //System.err.println("Added class path: " + path);
    _djInterpreter.addClassPath(path);
  }

  /**
   * An extension of DynamicJava's interpreter that makes sure classes are
   * not loaded by the system class loader (when possible) so that future
   * interpreters will be able to reload the classes.
   * This is somewhat a hack (it might be better to just modify TreeInterpreter)
   * but we don't want to modify DynamicJava right now.
   */
  private class InterpreterExtension extends TreeInterpreter {
    public InterpreterExtension() {
      super(new JavaCCParserFactory());
      classLoader = new ClassLoaderExtension(this);

      // We have to reinitialize these variables because they automatically
      // fetch pointers to classLoader in their constructors.
      nameVisitorContext  = new GlobalContext(this);
      nameVisitorContext.setAdditionalClassLoaderContainer(classLoader);
      checkVisitorContext = new GlobalContext(this);
      checkVisitorContext.setAdditionalClassLoaderContainer(classLoader);
      evalVisitorContext  = new GlobalContext(this);
      evalVisitorContext.setAdditionalClassLoaderContainer(classLoader);

      //System.err.println("set loader: " + classLoader);
    }
  }

  private class ClassLoaderExtension extends TreeClassLoader
  {
    public ClassLoaderExtension(Interpreter i) {
      super(i);
      //System.err.println("created loader extension");
    }

    /**
     * Overrides loadClass to try to load all classes we can ourselves, only
     * delegating to the system loader if we can't load a class.
     * This is because any classes loaded by the system loader are never
     * unloaded.
     */
    protected Class loadClass(String name, boolean resolve)
      throws ClassNotFoundException
    {
      //System.err.println("loadClass: " + name);

      if (_checker.mustUseSystemLoader(name)) {
        return super.loadClass(name, resolve);
      }
      else
      {
        return findClass(name);
      }
    }

    /**
     * Try to load the class ourselves. We do this so its classloader property
     * will be set to this loader, so all other classes that are referenced
     * from this class will be loaded by our loader.
     */
    protected Class findClass(String name) throws ClassNotFoundException {
      //System.err.println("findClass: " + name);

      // check the cache
      if (classes.containsKey(name)) {
        return (Class)classes.get(name);
      }
	
      try {
        // classLoader contains URL class loaders to load from other
        // paths/urls. if we have one, try to load there.
        if (classLoader != null) {
          // getResourceAsStream finds a file that's in the classpath. It's
          // generally used to load resources (like images) from the same
          // location as class files. However for our purposes of loading the
          // bytes of a class file, this works perfectly. It will find the class
          // in any place in the classpath, and it doesn't force us to search
          // the classpath ourselves.
          // (The classpath includes URLs to other places even!)
          String fileName = name.replace('.', '/') + ".class";
          InputStream stream = classLoader.getResourceAsStream(fileName);

          if (stream == null) {
            throw new IOException();
          }

          byte[] data = new byte[stream.available()];
          stream.read(data);

          return defineClass(name, data, 0, data.length);
        }
      }
      catch (IOException ioe) {}
      // If it exceptions, just fall through to here to try the interpreter.

      // If all else fails, try loading the class through the interpreter.
      // That's used for classes defined in the interpreter.
      return interpreter.loadClass(name);
    }
  }
}

/** Figures out whether a class can be loaded by a custom class loader or not. */
class ClassLoadChecker {
  private final SecurityManager _security = System.getSecurityManager();

  /**
   * Map of package name (string) to whether must use system loader (boolean).
   */
  private HashMap _checkedPackages = new HashMap();

  public boolean mustUseSystemLoader(String name) {
    // If name begins with java., must use System loader. This
    // is regardless of the security manager.
    // javax. too, though this is not documented
    if (name.startsWith("java.") || name.startsWith("javax.")) {
      return true;
    }

    // No security manager? We can do whatever we want!
    if (_security == null) {
      return false;
    }
    
    int lastDot = name.lastIndexOf('.');
    String packageName;
    if (lastDot == -1) {
      packageName = "";
    }
    else {
      packageName = name.substring(0, lastDot);
    }

    // Check the cache first
    Object cacheCheck = _checkedPackages.get(packageName);
    if (cacheCheck != null) {
      return ((Boolean) cacheCheck).booleanValue();
    }

    // Now try to get the package info. If it fails, it's a system class.
    try {
      _security.checkPackageDefinition(packageName);
      // Succeeded, so does not require system loader.
      _checkedPackages.put(packageName, Boolean.FALSE);
      return false;
    }
    catch (SecurityException se) {
      // Failed, so does require system loader.
      _checkedPackages.put(packageName, Boolean.TRUE);
      return true;
    }
  }
}
