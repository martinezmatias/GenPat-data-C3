package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 * <p>Collects all failing test <i>cases</i> and creates a new JUnit test class containing
 * a suite() method which calls these failed tests.</p>
 * <p>Having classes <i>A</i> ... <i>D</i> with each several testcases you could earn a new
 * test class like
 * <pre>
 * // generated on: 2007.08.06 09:42:34,555
 * import junit.framework.*;
 * public class FailedTests extends TestCase {
 *     public FailedTests(String s) {
 *         super(s);
 *     }
 *     public static Test suite() {
 *         TestSuite suite = new TestSuite();
 *         suite.addTest( new B("test04") );
 *         suite.addTest( new org.D("test10") );
 *         return suite;
 *     }
 * }
 * </pre>
 *
 * @since Ant 1.7.1
 */
/*
 * Because each running test case gets its own formatter, we collect
 * the failing test cases in a static list. Because we dont have a finalizer
 * method in the formatters "lifecycle", we regenerate the new java source
 * at each end of a test suite. The last run will contain all failed tests.
 */
public class FailureRecorder implements JUnitResultFormatter {

    /**
     * This is the name of a magic System property ({@value}). The value of this
     * <b>System</b> property should point to the location where to store the
     * generated class (without suffix).
     * Default location and name is defined in DEFAULT_CLASS_LOCATION.
     * @see #DEFAULT_CLASS_LOCATION
     */
    public static final String MAGIC_PROPERTY_CLASS_LOCATION = "ant.junit.failureCollector";

    /** Default location and name for the generated JUnit class file. {@value} */
    public static final String DEFAULT_CLASS_LOCATION = System.getProperty("java.io.tmpdir") + "FailedTests";

    /** Class names of failed tests without duplicates. */
    private static HashSet/*<Test>*/ failedTests = new HashSet();

    /** A writer for writing the generated source to. */
    private PrintWriter writer;

    /**
     * Location and name of the generated JUnit class.
     * Lazy instantiated via getLocationName().
     */
    private static String locationName;

    //TODO: Dont set the locationName via System.getProperty - better
    //      via Ant properties. But how to access these?
    private String getLocationName() {
        if (locationName == null) {
            String propValue = System.getProperty(MAGIC_PROPERTY_CLASS_LOCATION);
            locationName = (propValue != null) ? propValue : DEFAULT_CLASS_LOCATION;
        }
        return locationName;
    }

    /**
     * After each test suite, the whole new JUnit class will be regenerated.
     * @see org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter#endTestSuite(org.apache.tools.ant.taskdefs.optional.junit.JUnitTest)
     */
    public void endTestSuite(JUnitTest suite) throws BuildException {
        if (failedTests.isEmpty()) return;
        try {
            File sourceFile = new File(getLocationName() + ".java");
            sourceFile.delete();
            writer = new PrintWriter(new FileOutputStream(sourceFile));

            createClassHeader();
            createTestSuiteHeader();
            for (Iterator iter = failedTests.iterator(); iter.hasNext();) {
                Test test = (Test) iter.next();
                if (test!=null) {
                    createAddTestToSuite(test);
                }
            }
            createTestSuiteFooter();
            createClassFooter();

            FileUtils.close(writer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void addError(Test test, Throwable throwable) {
        failedTests.add(test);
    }

    public void addFailure(Test test, AssertionFailedError error) {
        failedTests.add(test);
    }

    public void setOutput(OutputStream out) {
        // not in use
    }

    public void setSystemError(String err) {
        // not in use
    }

    public void setSystemOutput(String out) {
        // not in use
    }

    public void startTestSuite(JUnitTest suite) throws BuildException {
        // not in use
    }

    public void endTest(Test test) {
        // not in use
    }

    public void startTest(Test test) {
        // not in use
    }

    // "Templates" for generating the JUnit class

    private void createClassHeader() {
        String className = getLocationName().replace('\\', '/');
        if (className.indexOf('/') > -1) {
            className = className.substring(className.lastIndexOf('/')+1);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss,SSS");
        writer.print("// generated on: ");
        writer.println(sdf.format(new Date()));
        writer.println("import junit.framework.*;");
        writer.print("public class ");
        writer.print( className );
        // If this class does not extend TC, Ant doesnt run these
        writer.println(" extends TestCase {");
        // no-arg constructor
        writer.print("    public ");
        writer.print(className);
        writer.println("(String s) {");
        writer.println("        super(s);");
        writer.println("    }");
    }

    private void createTestSuiteHeader() {
        writer.println("    public static Test suite() {");
        writer.println("        TestSuite suite = new TestSuite();");
    }

    private void createAddTestToSuite(Test test) {
        writer.print("        suite.addTest( new ");
        writer.print( getClassName(test) );
        writer.print("(\"");
        writer.print( getMethodName(test) );
        writer.println("\") );");
    }

    private void createTestSuiteFooter() {
        writer.println("        return suite;");
        writer.println("    }");
    }

    private void createClassFooter() {
        writer.println("}");
    }

    // Helper methods

    private String getMethodName(Test test) {
        String methodName = test.toString();
        return methodName.substring(0, methodName.indexOf('('));
    }

    private String getClassName(Test test) {
        return test.getClass().getName();
    }

}
