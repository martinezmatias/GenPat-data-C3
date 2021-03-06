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

package edu.rice.cs.plt.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.iter.SizedIterable;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.lambda.WrappedException;
import edu.rice.cs.plt.reflect.ReflectUtil;
import edu.rice.cs.plt.text.TextUtil;

import static edu.rice.cs.plt.io.IOUtil.attemptAbsoluteFiles;
import static edu.rice.cs.plt.iter.IterUtil.snapshot;
import static edu.rice.cs.plt.collect.CollectUtil.snapshot;
import static edu.rice.cs.plt.debug.DebugUtil.debug;

/**
 * Creates Java subprocesses via an interface similar to that of {@link ProcessBuilder}.  Each JVMBuilder
 * object is immutable, but can be used as a template for creating other JVMBuilder with altered parameters.
 * (Unlike ProcessBuilder, there is no implicit mutation via mutable maps returned by getters.)  The following
 * parameters are supported:<ul>
 * <li>{@code javaCommand}: Command, file, or directory used to invoke the JVM.  If a directory is given,
 *     the relative directories {@code bin} and {@code ../bin} are also searched for a {@code java} executable.
 *     Default: the {@code java.home} property.</li>
 * <li>{@code jvmArgs}: Arguments to pass to the {@code java} executable.  These are passed <em>before</em> all
 *      standard arguments supported here (e.g. {@code "-classpath"}).  Default: empty.</li>
 * <li>{@code classPath}: Class path to use in the JVM.  Default: {@code ReflectUtil.SYSTEM_CLASS_PATH}.</li>
 * <li>{@code dir}: Working directory of the new process.  Default: {@code System.getProperty("user.dir", "")}.</li>
 * <li>{@code properties}: Java properties to define in the new JVM (passed to the {@code java} executable with
 *     arguments of the form {@code "-D<key>=<value>"}); non-string entries are converted to strings via
 *     {@code toString}.  Default: empty.</li>
 * <li>{@code environment}: System environment variables to define in the new JVM, or {@code null} signifying that
 *     the current environment should be duplicated.  Default: {@code null}.
 * </ul>
 */
public class JVMBuilder implements Lambda2<String, Iterable<? extends String>, Process> {
  
  private static final String DEFAULT_JAVA_COMMAND = findJavaCommand(System.getProperty("java.home", ""));
  private static final SizedIterable<String> DEFAULT_JVM_ARGS = IterUtil.empty();
  private static final SizedIterable<File> DEFAULT_CLASS_PATH = attemptAbsoluteFiles(ReflectUtil.SYSTEM_CLASS_PATH);
  private static final File DEFAULT_DIR = IOUtil.WORKING_DIRECTORY;
  private static final Properties DEFAULT_PROPERTIES = new Properties();
  private static final Map<String, String> DEFAULT_ENVIRONMENT = null;
  
  public static final JVMBuilder DEFAULT = new JVMBuilder();
  
  private final String _javaCommand;
  private final SizedIterable<String> _jvmArgs;
  private final SizedIterable<File> _classPath;
  private final File _dir;
  private final Properties _properties;
  private final Map<String, String> _environment;
  
  private JVMBuilder() {
    this(DEFAULT_JAVA_COMMAND, DEFAULT_JVM_ARGS, DEFAULT_CLASS_PATH, DEFAULT_DIR, DEFAULT_PROPERTIES,
         DEFAULT_ENVIRONMENT, true);
  }
  
  public JVMBuilder(String javaCommand) {
    this(findJavaCommand(javaCommand), DEFAULT_JVM_ARGS, DEFAULT_CLASS_PATH, DEFAULT_DIR,
         DEFAULT_PROPERTIES, DEFAULT_ENVIRONMENT, true);
  }
  
  public JVMBuilder(String javaCommand, Iterable<? extends String> jvmArgs) {
    this(findJavaCommand(javaCommand), snapshot(jvmArgs), DEFAULT_CLASS_PATH, DEFAULT_DIR,
         DEFAULT_PROPERTIES, DEFAULT_ENVIRONMENT, true);
  }
  
  public JVMBuilder(Iterable<? extends File> classPath) {
    this(DEFAULT_JAVA_COMMAND, DEFAULT_JVM_ARGS, attemptAbsoluteFiles(classPath), DEFAULT_DIR,
         DEFAULT_PROPERTIES, DEFAULT_ENVIRONMENT, true);
  }
  
  public JVMBuilder(File dir) {
    this(DEFAULT_JAVA_COMMAND, DEFAULT_JVM_ARGS, DEFAULT_CLASS_PATH, dir, DEFAULT_PROPERTIES,
         DEFAULT_ENVIRONMENT, true);
  }
  
  public JVMBuilder(Properties properties) {
    this(DEFAULT_JAVA_COMMAND, DEFAULT_JVM_ARGS, DEFAULT_CLASS_PATH, DEFAULT_DIR,
         copyProps(properties), DEFAULT_ENVIRONMENT, true);
  }
  
  public JVMBuilder(Iterable<? extends File> classPath, File dir, Properties properties) {
    this(DEFAULT_JAVA_COMMAND, DEFAULT_JVM_ARGS, attemptAbsoluteFiles(classPath), dir,
         copyProps(properties), DEFAULT_ENVIRONMENT, true);
  }
  
  public JVMBuilder(Map<? extends String, ? extends String> environment) {
    this(DEFAULT_JAVA_COMMAND, DEFAULT_JVM_ARGS, DEFAULT_CLASS_PATH, DEFAULT_DIR, DEFAULT_PROPERTIES,
         (environment == null) ? null : snapshot(environment), true);
  }
  
  public JVMBuilder(String javaCommand, Iterable<? extends String> jvmArgs, Iterable<? extends File> classPath,
                    File dir, Properties properties, Map<? extends String, ? extends String> environment) {
    this(findJavaCommand(javaCommand), snapshot(jvmArgs), attemptAbsoluteFiles(classPath), dir,
         copyProps(properties), (environment == null) ? null : snapshot(environment), true);
  }
  
  /**
   * Private constructor.  Preprocessing of arguments has already happened; no arguments will be externally
   * mutated.  dummy parameter is to distinguish this constructor from other overloads.
   */
  private JVMBuilder(String javaCommand, SizedIterable<String> jvmArgs, SizedIterable<File> classPath,
                     File dir, Properties properties, Map<String, String> environment, boolean dummy) {
    _javaCommand = javaCommand;
    _jvmArgs = jvmArgs;
    _classPath = classPath;
    _dir = dir;
    _properties = properties;
    _environment = environment;
  }
  
  public String javaCommand() { return _javaCommand; }
  
  public JVMBuilder javaCommand(String javaCommand) {
    return new JVMBuilder(findJavaCommand(javaCommand), _jvmArgs, _classPath, _dir, _properties, _environment, true);
  }
  
  public JVMBuilder javaCommand(File javaCommand) {
    return new JVMBuilder(findJavaCommand(javaCommand), _jvmArgs, _classPath, _dir, _properties, _environment, true);
  }
  
  public SizedIterable<String> jvmArguments() { return _jvmArgs; }
  
  public JVMBuilder jvmArguments(Iterable<? extends String> jvmArgs) {
    return new JVMBuilder(_javaCommand, IterUtil.snapshot(jvmArgs), _classPath, _dir, _properties, _environment, true);
  }
  
  /** Due to overloading rules, this cannot be invoked via varargs with 0 arguments (the getter matches instead). */
  public JVMBuilder jvmArguments(String... jvmArgs) {
    return new JVMBuilder(_javaCommand, IterUtil.make(jvmArgs), _classPath, _dir, _properties, _environment, true);
  }
  
  public SizedIterable<File> classPath() { return _classPath; }
  
  public JVMBuilder classPath(Iterable<? extends File> classPath) {
    return new JVMBuilder(_javaCommand, _jvmArgs, attemptAbsoluteFiles(classPath), _dir,
                          _properties, _environment, true);
  }
  
  public JVMBuilder classPath(String classPath) {
    return new JVMBuilder(_javaCommand, _jvmArgs, attemptAbsoluteFiles(IOUtil.parsePath(classPath)), _dir,
                          _properties, _environment, true);
  }
  
  /** Due to overloading rules, this cannot be invoked via varargs with 0 arguments (the getter matches instead). */
  public JVMBuilder classPath(File... classPath) {
    return new JVMBuilder(_javaCommand, _jvmArgs, attemptAbsoluteFiles(IterUtil.asIterable(classPath)), _dir,
                          _properties, _environment, true);
  }
  
  public File directory() { return _dir; }
  
  public JVMBuilder directory(File dir) {
    return new JVMBuilder(_javaCommand, _jvmArgs, _classPath, dir, _properties, _environment, true);
  }
  
  public JVMBuilder directory(String dir) {
    return new JVMBuilder(_javaCommand, _jvmArgs, _classPath, new File(dir), _properties, _environment, true);
  }
  
  /**
   * Get an immutable view of the properties.  All keys and values are strings (the Properties class doesn't
   * make this guarantee or provide this type signature).
   */
  public Map<String, String> properties() {
    // cast is safe because we've guaranteed that all keys and values are strings
    @SuppressWarnings("unchecked") Map<String, String> strings = (Map<String, String>) (Map<?, ?>) _properties;
    return CollectUtil.immutable(strings);
  }
  
  /**
   * Return a mutable copy of the properties.  Changes to the result do not affect this JVMBuilder,
   * but a modified Properties map can be used to create another JVMBuilder.
   */
  public Properties propertiesCopy() { return copyProps(_properties); }
  
  public JVMBuilder properties(Properties properties) {
    return new JVMBuilder(_javaCommand, _jvmArgs, _classPath, _dir, copyProps(properties), _environment, true);
  }
  
  public JVMBuilder properties(Map<? extends String, ? extends String> properties) {
    return new JVMBuilder(_javaCommand, _jvmArgs, _classPath, _dir, copyProps(properties), _environment, true);
  }
  
  /** Get an immutable view of the environment, or {@code null} if the current process's environment is used. */
  public Map<String, String> environment() {
    return (_environment == null) ? null : CollectUtil.immutable(_environment);
  }
  
  /**
   * Get a mutable copy of the environment.  If the environment is {@code null} (meaning the the current
   * process's environment should be used), the result is a copy of {@link System#getenv()}. Changes
   * to the result do not affect this JVMBuilder, but a modified environment map can be used to create
   * another JVMBuilder.
   */
  public Map<String, String> environmentCopy() {
    return snapshot((_environment == null) ? System.getenv() : _environment);
  }
  
  public JVMBuilder environment(Map<? extends String, ? extends String> environment) {
    return new JVMBuilder(_javaCommand, _jvmArgs, _classPath, _dir, _properties,
                          (_environment == null) ? null : snapshot(environment), true);
  }
  
  
  /**
   * Start a JVM process via {@link Runtime#exec(String[], String[], File)}. Varargs shortcut for
   * {@link #start(String, Iterable)}.
   */
  public Process start(String mainClass, String... mainParams) throws IOException {
    return start(mainClass, IterUtil.asIterable(mainParams));
  }

  /**
   * Start a JVM process via {@link Runtime#exec(String[], String[], File)}. The array of command strings
   * contains, in order: the java command, the JVM args, {@code "-classpath"} followed by the class path, each
   * property using {@code "-D<key>=<value>"} notation, the name of the given main class, and the main
   * parameters.
   * @throws IOException  Per {@link Runtime#exec(String[], String[], File)}.
   * @throws SecurityException  Per {@link Runtime#exec(String[], String[], File)}.
   */
  public Process start(String mainClass, Iterable<? extends String> mainParams) throws IOException {
    List<String> commandL = new LinkedList<String>();
    commandL.add(_javaCommand);
    CollectUtil.addAll(commandL, _jvmArgs);
    commandL.add("-classpath");
    commandL.add(IOUtil.pathToString(_classPath));
    for (Map.Entry<Object, Object> prop : _properties.entrySet()) {
      commandL.add("-D" + prop.getKey() + "=" + prop.getValue());
    }
    commandL.add(mainClass);
    CollectUtil.addAll(commandL, mainParams);
    String[] command = IterUtil.toArray(commandL, String.class);
    
    String[] env;
    if (_environment == null) { env = null; }
    else {
      List<String> envL = new LinkedList<String>();
      for (Map.Entry<String, String> binding : _environment.entrySet()) {
        envL.add(binding.getKey() + "=" + binding.getValue());
      }
      env = IterUtil.toArray(envL, String.class);
    }
    
    // IMPORTANT: Do not leave this logging message uncommented, or setting debug to an RMILogSink won't work
    //debug.logValues("Starting JVM", new String[]{"command", "env", "dir"}, command, env, _dir);
    return Runtime.getRuntime().exec(command, env, _dir);
  }
  
  public Process value(String mainClass, Iterable<? extends String> mainParams) {
    try { return start(mainClass, mainParams); }
    catch (IOException e) { throw new WrappedException(e); }
  }

  private static String findJavaCommand(String command) {
    return findJavaCommand(new File(command));
  }
  
  /** Find the java executable command. */
  private static String findJavaCommand(File f) {
    if (IOUtil.attemptIsFile(f)) { return f.getPath(); }
    else if (IOUtil.attemptIsDirectory(f)) {
      // This logic originally came from Ant.
      f = IOUtil.attemptAbsoluteFile(f);
      String os = System.getProperty("os.name", "");
      File[] candidates = new File[]{ new File(f, "../bin"), new File(f, "bin"), f };
      
      if (!TextUtil.containsIgnoreCase(os, "netware")) { // based on comments from Ant's code
        if (TextUtil.containsIgnoreCase(os, "windows")) {
          for (File dir : candidates) {
            File result = new File(dir, "javaw.exe");
            if (IOUtil.attemptExists(result)) { return result.getPath(); }
            result = new File(dir, "java.exe");
            if (IOUtil.attemptExists(result)) { return result.getPath(); }
          }
        }
        else {
          for (File dir : candidates) {
            File result = new File(dir, "java");
            if (IOUtil.attemptExists(result)) { return result.getPath(); }
          }
        }
      }
    }
    // If nothing works, use the original file or command string unchanged
    return f.toString();
  }
  
  
  /**
   * Make a copy of the given Properties.  Like {code Properties.clone()}, makes an independent copy.
   * Additionally, guarantees that all keys and values in the result are strings.  (Also avoids the need for 
   * cooperation from {@code p}'s class, which might produce a clone that isn't independent.) 
   */
  private static Properties copyProps(Map<?, ?> p) {
    Properties result = new Properties();
    for (Map.Entry<?, ?> e : p.entrySet()) {
      result.setProperty(e.getKey().toString(), e.getValue().toString());
    }
    return result;
  }
  
}
