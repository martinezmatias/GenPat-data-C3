/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant;

import org.apache.tools.ant.util.LoaderUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.apache.tools.ant.util.ProxySetup;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.launch.Launcher;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Calendar;
import java.util.TimeZone;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * A little diagnostic helper that output some information that may help
 * in support. It should quickly give correct information about the
 * jar existing in ant.home/lib and the jar versions...
 *
 * @since Ant 1.5
 */
public final class Diagnostics {

    /** the version number for java 1.5 returned from JavaEnvUtils */
    private static final int JAVA_1_5_NUMBER = 15;

    /**
     * value for which a difference between clock and temp file time triggers
     * a warning.
     * {@value}
     */
    private static final int BIG_DRIFT_LIMIT = 10000;

    /**
     * How big a test file to write.
     * {@value}
     */
    private static final int TEST_FILE_SIZE = 32;
    private static final int KILOBYTE = 1024;
    private static final int SECONDS_PER_MILLISECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final String TEST_CLASS = "org.apache.tools.ant.taskdefs.optional.EchoProperties";

    /**
     * The error text when a security manager blocks access to a property.
     * {@value}
     */
    protected static final String ERROR_PROPERTY_ACCESS_BLOCKED
            = "Access to this property blocked by a security manager";

    /** utility class */
    private Diagnostics() {
        // hidden constructor
    }

    /**
     * Check if optional tasks are available. Not that it does not check
     * for implementation version. Use <tt>validateVersion()</tt> for this.
     * @return <tt>true</tt> if optional tasks are available.
     */
    public static boolean isOptionalAvailable() {
        try {
            Class.forName(TEST_CLASS);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * Check if core and optional implementation version do match.
     * @throws BuildException if the implementation version of optional tasks
     * does not match the core implementation version.
     */
    public static void validateVersion() throws BuildException {
        try {
            Class optional = Class.forName(TEST_CLASS);
            String coreVersion = getImplementationVersion(Main.class);
            String optionalVersion = getImplementationVersion(optional);

            if (coreVersion != null && !coreVersion.equals(optionalVersion)) {
                throw new BuildException("Invalid implementation version "
                        + "between Ant core and Ant optional tasks.\n"
                        + " core    : " + coreVersion + " in "
                        + getClassLocation(Main.class)
                        + "\n" + " optional: " + optionalVersion + " in "
                        + getClassLocation(optional));
            }
        } catch (ClassNotFoundException e) {
            // ignore
            ignoreThrowable(e);
        }
    }

    /**
     * return the list of jar files existing in ANT_HOME/lib
     * and that must have been picked up by Ant script.
     * @return the list of jar files existing in ant.home/lib or
     * <tt>null</tt> if an error occurs.
     */
    public static File[] listLibraries() {
        String home = System.getProperty(MagicNames.ANT_HOME);
        if (home == null) {
            return null;
        }
        File libDir = new File(home, "lib");
        return listJarFiles(libDir);

    }

    /**
     * get a list of all JAR files in a directory
     * @param libDir directory
     * @return array of files (or null for no such directory)
     */
    private static File[] listJarFiles(File libDir) {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        };
        File[] files  = libDir.listFiles(filter);
        return files;
    }

    /**
     * main entry point for command line
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        doReport(System.out);
    }

    /**
     * Helper method to get the implementation version.
     * @param clazz the class to get the information from.
     * @return null if there is no package or implementation version.
     * '?.?' for JDK 1.0 or 1.1.
     */
    private static String getImplementationVersion(Class clazz) {
        return clazz.getPackage().getImplementationVersion();
    }

    /**
     * Helper method to get the location.
     * @param clazz the class to get the information from.
     * @since Ant 1.8.0
     */
    private static URL getClassLocation(Class clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation();
    }

    /**
     * what parser are we using.
     * @return the classname of the parser
     */
    private static String getXMLParserName() {
        SAXParser saxParser = getSAXParser();
        if (saxParser == null) {
            return "Could not create an XML Parser";
        }
        // check to what is in the classname
        String saxParserName = saxParser.getClass().getName();
        return saxParserName;
    }

    /**
     * what parser are we using.
     * @return the classname of the parser
     */
    private static String getXSLTProcessorName() {
        Transformer transformer = getXSLTProcessor();
        if (transformer == null) {
            return "Could not create an XSLT Processor";
        }
        // check to what is in the classname
        String processorName = transformer.getClass().getName();
        return processorName;
    }

    /**
     * Create a JAXP SAXParser
     * @return parser or null for trouble
     */
    private static SAXParser getSAXParser() {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        if (saxParserFactory == null) {
            return null;
        }
        SAXParser saxParser = null;
        try {
            saxParser = saxParserFactory.newSAXParser();
        } catch (Exception e) {
            // ignore
            ignoreThrowable(e);
        }
        return saxParser;
    }

    /**
     * Create a JAXP XSLT Transformer
     * @return parser or null for trouble
     */
    private static Transformer getXSLTProcessor() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        if (transformerFactory == null) {
            return null;
        }
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (Exception e) {
            // ignore
            ignoreThrowable(e);
        }
        return transformer;
    }

    /**
     * get the location of the parser
     * @return path or null for trouble in tracking it down
     */
    private static String getXMLParserLocation() {
        SAXParser saxParser = getSAXParser();
        if (saxParser == null) {
            return null;
        }
        URL location = getClassLocation(saxParser.getClass());
        return location != null ? location.toString() : null;
    }

    private static String getNamespaceParserName() {
        try {
            XMLReader reader = JAXPUtils.getNamespaceXMLReader();
            return reader.getClass().getName();
        } catch (BuildException e) {
            //ignore
            ignoreThrowable(e);
            return null;
        }
    }

    private static String getNamespaceParserLocation() {
        try {
            XMLReader reader = JAXPUtils.getNamespaceXMLReader();
            URL location = getClassLocation(reader.getClass());
            return location != null ? location.toString() : null;
        } catch (BuildException e) {
            //ignore
            ignoreThrowable(e);
            return null;
        }
    }

    /**
     * get the location of the parser
     * @return path or null for trouble in tracking it down
     */
    private static String getXSLTProcessorLocation() {
        Transformer transformer = getXSLTProcessor();
        if (transformer == null) {
            return null;
        }
        URL location = getClassLocation(transformer.getClass());
        return location != null ? location.toString() : null;
    }

    /**
     * ignore exceptions. This is to allow future
     * implementations to log at a verbose level
     * @param thrown
     */
    private static void ignoreThrowable(Throwable thrown) {
    }


    /**
     * Print a report to the given stream.
     * @param out the stream to print the report to.
     */
    public static void doReport(PrintStream out) {
        doReport(out, Project.MSG_INFO);
    }

    /**
     * Print a report to the given stream.
     * @param out the stream to print the report to.
     * @param logLevel denotes the level of detail requested as one of
     * Project's MSG_* constants.
     */
    public static void doReport(PrintStream out, int logLevel) {
        out.println("------- Ant diagnostics report -------");
        out.println(Main.getAntVersion());
        header(out, "Implementation Version");

        out.println("core tasks     : " + getImplementationVersion(Main.class)
                    + " in " + getClassLocation(Main.class));

        Class optional = null;
        try {
            optional = Class.forName(TEST_CLASS);
            out.println("optional tasks : " + getImplementationVersion(optional)
                        + " in " + getClassLocation(optional));
        } catch (ClassNotFoundException e) {
            ignoreThrowable(e);
            out.println("optional tasks : not available");
        }

        header(out, "ANT PROPERTIES");
        doReportAntProperties(out);

        header(out, "ANT_HOME/lib jar listing");
        doReportAntHomeLibraries(out);

        header(out, "USER_HOME/.ant/lib jar listing");
        doReportUserHomeLibraries(out);

        header(out, "Tasks availability");
        doReportTasksAvailability(out);

        header(out, "org.apache.env.Which diagnostics");
        doReportWhich(out);

        header(out, "XML Parser information");
        doReportParserInfo(out);

        header(out, "XSLT Processor information");
        doReportXSLTProcessorInfo(out);

        header(out, "System properties");
        doReportSystemProperties(out);

        header(out, "Temp dir");
        doReportTempDir(out);

        header(out, "Locale information");
        doReportLocale(out);

        header(out, "Proxy information");
        doReportProxy(out);

        out.println();
    }

    private static void header(PrintStream out, String section) {
        out.println();
        out.println("-------------------------------------------");
        out.print(" ");
        out.println(section);
        out.println("-------------------------------------------");
    }

    /**
     * Report a listing of system properties existing in the current vm.
     * @param out the stream to print the properties to.
     */
    private static void doReportSystemProperties(PrintStream out) {
        Properties sysprops = null;
        try {
            sysprops = System.getProperties();
        } catch (SecurityException  e) {
            ignoreThrowable(e);
            out.println("Access to System.getProperties() blocked " + "by a security manager");
        }
        for (Enumeration keys = sysprops.propertyNames();
            keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            String value = getProperty(key);
            out.println(key + " : " + value);
        }
    }

    /**
     * Get the value of a system property. If a security manager
     * blocks access to a property it fills the result in with an error
     * @param key
     * @return the system property's value or error text
     * @see #ERROR_PROPERTY_ACCESS_BLOCKED
     */
    private static String getProperty(String key) {
        String value;
        try {
            value = System.getProperty(key);
        } catch (SecurityException e) {
            value = ERROR_PROPERTY_ACCESS_BLOCKED;
        }
        return value;
    }

    /**
     * Report the content of ANT_HOME/lib directory
     * @param out the stream to print the content to
     */
    private static void doReportAntProperties(PrintStream out) {
        Project p = new Project();
        p.initProperties();
        out.println(MagicNames.ANT_VERSION + ": " + p.getProperty(MagicNames.ANT_VERSION));
        out.println(MagicNames.ANT_JAVA_VERSION + ": "
                + p.getProperty(MagicNames.ANT_JAVA_VERSION));
        out.println(MagicNames.ANT_LIB + ": " + p.getProperty(MagicNames.ANT_LIB));
        out.println(MagicNames.ANT_HOME + ": " + p.getProperty(MagicNames.ANT_HOME));
    }

    /**
     * Report the content of ANT_HOME/lib directory
     * @param out the stream to print the content to
     */
    private static void doReportAntHomeLibraries(PrintStream out) {
        out.println(MagicNames.ANT_HOME + ": " + System.getProperty(MagicNames.ANT_HOME));
        File[] libs = listLibraries();
        printLibraries(libs, out);
    }

    /**
     * Report the content of ~/.ant/lib directory
     *
     * @param out the stream to print the content to
     */
    private static void doReportUserHomeLibraries(PrintStream out) {
        String home = System.getProperty(Launcher.USER_HOMEDIR);
        out.println("user.home: " + home);
        File libDir = new File(home, Launcher.USER_LIBDIR);
        File[] libs = listJarFiles(libDir);
        printLibraries(libs, out);
    }

    /**
     * list the libraries
     * @param libs array of libraries (can be null)
     * @param out output stream
     */
    private static void printLibraries(File[] libs, PrintStream out) {
        if (libs == null) {
            out.println("No such directory.");
            return;
        }
        for (int i = 0; i < libs.length; i++) {
            out.println(libs[i].getName() + " (" + libs[i].length() + " bytes)");
        }
    }


    /**
     * Call org.apache.env.Which if available
     * @param out the stream to print the content to.
     */
    private static void doReportWhich(PrintStream out) {
        Throwable error = null;
        try {
            Class which = Class.forName("org.apache.env.Which");
            Method method = which.getMethod(
                "main", new Class[] {String[].class});
            method.invoke(null, new Object[]{new String[]{}});
        } catch (ClassNotFoundException e) {
            out.println("Not available.");
            out.println("Download it at http://xml.apache.org/commons/");
        } catch (InvocationTargetException e) {
            error = e.getTargetException() == null ? e : e.getTargetException();
        } catch (Throwable e) {
            error = e;
        }
        // report error if something weird happens...this is diagnostic.
        if (error != null) {
            out.println("Error while running org.apache.env.Which");
            error.printStackTrace();
        }
    }

    /**
     * Create a report about non-available tasks that are defined in the
     * mapping but could not be found via lookup. It might generally happen
     * because Ant requires multiple libraries to compile and one of them
     * was missing when compiling Ant.
     * @param out the stream to print the tasks report to
     * <tt>null</tt> for a missing stream (ie mapping).
     */
    private static void doReportTasksAvailability(PrintStream out) {
        InputStream is = Main.class.getResourceAsStream(
                MagicNames.TASKDEF_PROPERTIES_RESOURCE);
        if (is == null) {
            out.println("None available");
        } else {
            Properties props = new Properties();
            try {
                props.load(is);
                for (Enumeration keys = props.keys(); keys.hasMoreElements();) {
                    String key = (String) keys.nextElement();
                    String classname = props.getProperty(key);
                    try {
                        Class.forName(classname);
                        props.remove(key);
                    } catch (ClassNotFoundException e) {
                        out.println(key + " : Not Available "
                                + "(the implementation class is not present)");
                    } catch (NoClassDefFoundError e) {
                        String pkg = e.getMessage().replace('/', '.');
                        out.println(key + " : Missing dependency " + pkg);
                    } catch (LinkageError e) {
                        out.println(key + " : Initialization error");
                    }
                }
                if (props.size() == 0) {
                    out.println("All defined tasks are available");
                } else {
                    out.println("A task being missing/unavailable should only "
                            + "matter if you are trying to use it");
                }
            } catch (IOException e) {
                out.println(e.getMessage());
            }
        }
    }

    /**
     * tell the user about the XML parser
     * @param out
     */
    private static void doReportParserInfo(PrintStream out) {
        String parserName = getXMLParserName();
        String parserLocation = getXMLParserLocation();
        printParserInfo(out, "XML Parser", parserName, parserLocation);
        printParserInfo(out, "Namespace-aware parser", getNamespaceParserName(),
                getNamespaceParserLocation());
    }

    /**
     * tell the user about the XSLT processor
     * @param out
     */
    private static void doReportXSLTProcessorInfo(PrintStream out) {
        String processorName = getXSLTProcessorName();
        String processorLocation = getXSLTProcessorLocation();
        printParserInfo(out, "XSLT Processor", processorName, processorLocation);
    }

    private static void printParserInfo(PrintStream out, String parserType, String parserName,
            String parserLocation) {
        if (parserName == null) {
            parserName = "unknown";
        }
        if (parserLocation == null) {
            parserLocation = "unknown";
        }
        out.println(parserType + " : " + parserName);
        out.println(parserType + " Location: " + parserLocation);
    }

    /**
     * try and create a temp file in our temp dir; this
     * checks that it has space and access.
     * We also do some clock reporting.
     * @param out
     */
    private static void doReportTempDir(PrintStream out) {
        String tempdir = System.getProperty("java.io.tmpdir");
        if (tempdir == null) {
            out.println("Warning: java.io.tmpdir is undefined");
            return;
        }
        out.println("Temp dir is " + tempdir);
        File tempDirectory = new File(tempdir);
        if (!tempDirectory.exists()) {
            out.println("Warning, java.io.tmpdir directory does not exist: " + tempdir);
            return;
        }
        //create the file
        long now = System.currentTimeMillis();
        File tempFile = null;
        FileOutputStream fileout = null;
        FileInputStream filein = null;
        try {
            tempFile = File.createTempFile("diag", "txt", tempDirectory);
            //do some writing to it
            fileout = new FileOutputStream(tempFile);
            byte[] buffer = new byte[KILOBYTE];
            for (int i = 0; i < TEST_FILE_SIZE; i++) {
                fileout.write(buffer);
            }
            fileout.close();
            fileout = null;

            // read to make sure the file has been written completely
            Thread.sleep(1000);
            filein = new FileInputStream(tempFile);
            int total = 0;
            int read = 0;
            while ((read = filein.read(buffer, 0, KILOBYTE)) > 0) {
                total += read;
            }
            filein.close();
            filein = null;

            long filetime = tempFile.lastModified();
            long drift = filetime - now;
            tempFile.delete();

            out.print("Temp dir is writeable");
            if (total != TEST_FILE_SIZE * KILOBYTE) {
                out.println(", but seems to be full.  Wrote "
                            + (TEST_FILE_SIZE * KILOBYTE)
                            + "but could only read " + total + " bytes.");
            } else {
                out.println();
            }

            out.println("Temp dir alignment with system clock is " + drift + " ms");
            if (Math.abs(drift) > BIG_DRIFT_LIMIT) {
                out.println("Warning: big clock drift -maybe a network filesystem");
            }
        } catch (IOException e) {
            ignoreThrowable(e);
            out.println("Failed to create a temporary file in the temp dir " + tempdir);
            out.println("File  " + tempFile + " could not be created/written to");
        } catch (InterruptedException e) {
            ignoreThrowable(e);
            out.println("Failed to check whether tempdir is writable");
        } finally {
            FileUtils.close(fileout);
            FileUtils.close(filein);
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Report locale information
     * @param out stream to print to
     */
    private static void doReportLocale(PrintStream out) {
        //calendar stuff.
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        out.println("Timezone "
                + tz.getDisplayName()
                + " offset="
                + tz.getOffset(cal.get(Calendar.ERA), cal.get(Calendar.YEAR), cal
                        .get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), cal
                        .get(Calendar.DAY_OF_WEEK), ((cal.get(Calendar.HOUR_OF_DAY)
                        * MINUTES_PER_HOUR + cal.get(Calendar.MINUTE))
                        * SECONDS_PER_MINUTE + cal.get(Calendar.SECOND))
                        * SECONDS_PER_MILLISECOND + cal.get(Calendar.MILLISECOND)));
    }

    /**
     * print a property name="value" pair if the property is set;
     * print nothing if it is null
     * @param out stream to print on
     * @param key property name
     */
    private static void printProperty(PrintStream out, String key) {
        String value = getProperty(key);
        if (value != null) {
            out.print(key);
            out.print(" = ");
            out.print('"');
            out.print(value);
            out.println('"');
        }
    }

    /**
     * Report proxy information
     *
     * @param out stream to print to
     * @since Ant1.7
     */
    private static void doReportProxy(PrintStream out) {
        printProperty(out, ProxySetup.HTTP_PROXY_HOST);
        printProperty(out, ProxySetup.HTTP_PROXY_PORT);
        printProperty(out, ProxySetup.HTTP_PROXY_USERNAME);
        printProperty(out, ProxySetup.HTTP_PROXY_PASSWORD);
        printProperty(out, ProxySetup.HTTP_NON_PROXY_HOSTS);
        printProperty(out, ProxySetup.HTTPS_PROXY_HOST);
        printProperty(out, ProxySetup.HTTPS_PROXY_PORT);
        printProperty(out, ProxySetup.HTTPS_NON_PROXY_HOSTS);
        printProperty(out, ProxySetup.FTP_PROXY_HOST);
        printProperty(out, ProxySetup.FTP_PROXY_PORT);
        printProperty(out, ProxySetup.FTP_NON_PROXY_HOSTS);
        printProperty(out, ProxySetup.SOCKS_PROXY_HOST);
        printProperty(out, ProxySetup.SOCKS_PROXY_PORT);
        printProperty(out, ProxySetup.SOCKS_PROXY_USERNAME);
        printProperty(out, ProxySetup.SOCKS_PROXY_PASSWORD);

        if (JavaEnvUtils.getJavaVersionNumber() < JAVA_1_5_NUMBER) {
            return;
        }
        printProperty(out, ProxySetup.USE_SYSTEM_PROXIES);
        final String proxyDiagClassname = "org.apache.tools.ant.util.java15.ProxyDiagnostics";
        try {
            Class proxyDiagClass = Class.forName(proxyDiagClassname);
            Object instance = proxyDiagClass.newInstance();
            out.println("Java1.5+ proxy settings:");
            out.println(instance.toString());
        } catch (ClassNotFoundException e) {
            //not included, do nothing
        } catch (IllegalAccessException e) {
            //not included, do nothing
        } catch (InstantiationException e) {
            //not included, do nothing
        } catch (NoClassDefFoundError e) {
            // not included, to nothing
        }
    }

}
