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

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 * Test case for ant class loader
 *
 */
public class AntClassLoaderTest extends BuildFileTest {

    private AntClassLoader loader;

    public AntClassLoaderTest(String name) {
        super(name);
    }

    public void setUp() {
        super.configureProject("src/etc/testcases/core/antclassloader.xml");
        getProject().executeTarget("setUp");
    }

    public void tearDown() {
        if (loader != null) {
            loader.cleanup();
        }
        try {
            super.tearDown();
        } catch ( Exception e) {
            System.err.println(e.getMessage());
        }
    }
    //test inspired by bug report 37085
    public void testJarWithManifestInDirWithSpace() {
        String mainjarstring = getProject().getProperty("main.jar");
        String extjarstring = getProject().getProperty("ext.jar");
        Path myPath = new Path(getProject());
        myPath.setLocation(new File(mainjarstring));
        getProject().setUserProperty("build.sysclasspath","ignore");
        loader = getProject().createClassLoader(myPath);
        String path = loader.getClasspath();
        assertEquals(mainjarstring + File.pathSeparator + extjarstring, path);
    }
    public void testJarWithManifestInNonAsciiDir() {
        String mainjarstring = getProject().getProperty("main.jar.nonascii");
        String extjarstring = getProject().getProperty("ext.jar.nonascii");
        Path myPath = new Path(getProject());
        myPath.setLocation(new File(mainjarstring));
        getProject().setUserProperty("build.sysclasspath","ignore");
        loader = getProject().createClassLoader(myPath);
        String path = loader.getClasspath();
        assertEquals(mainjarstring + File.pathSeparator + extjarstring, path);
    }
    public void testCleanup() throws BuildException {
        Path path = new Path(project, ".");
        loader = project.createClassLoader(path);
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            fail("Did not expect to find fubar class");
        } catch (ClassNotFoundException e) {
            // ignore expected
        }

        loader.cleanup();
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            fail("Did not expect to find fubar class");
        } catch (ClassNotFoundException e) {
            // ignore expected
        } catch (NullPointerException e) {
            fail("loader should not fail even if cleaned up");
        }

        // tell the build it is finished
        project.fireBuildFinished(null);
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            fail("Did not expect to find fubar class");
        } catch (ClassNotFoundException e) {
            // ignore expected
        } catch (NullPointerException e) {
            fail("loader should not fail even if project finished");
        }
    }

    public void testGetPackage() throws Exception {
        executeTarget("prepareGetPackageTest");
        Path myPath = new Path(getProject());
        myPath.setLocation(new File(getProject().getProperty("test.jar")));
        getProject().setUserProperty("build.sysclasspath","ignore");
        loader = getProject().createClassLoader(myPath);
        assertNotNull("should find class", loader.findClass("org.example.Foo"));
        assertNotNull("should find package",
                      new GetPackageWrapper(loader).getPackage("org.example"));
    }

    public void testCodeSource() throws Exception {
        executeTarget("prepareGetPackageTest");
        Path myPath = new Path(getProject());
        File testJar = new File(getProject().getProperty("test.jar"));
        myPath.setLocation(testJar);
        getProject().setUserProperty("build.sysclasspath","ignore");
        loader = getProject().createClassLoader(myPath);
        Class foo = loader.findClass("org.example.Foo");
        URL codeSourceLocation =
            foo.getProtectionDomain().getCodeSource().getLocation();
        assertEquals(codeSourceLocation + " should point to test.jar",
                   FileUtils.getFileUtils().getFileURL(testJar), codeSourceLocation);
    }

    public void testSignedJar() throws Exception {
        executeTarget("signTestJar");
        File jar = new File(getProject().getProperty("test.jar"));

        Path myPath = new Path(getProject());
        myPath.setLocation(jar);
        getProject().setUserProperty("build.sysclasspath","ignore");
        loader = getProject().createClassLoader(myPath);
        Class foo = loader.findClass("org.example.Foo");

        assertNotNull("should find class", foo);
        assertNotNull("should have certificates",
                      foo.getProtectionDomain().getCodeSource()
                      .getCertificates());
        assertNotNull("should be signed", foo.getSigners());
    }

    /**
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=47593">
     *     bug 47593, request to log the name of corrupt zip files from which
     *     classes cannot be loaded</a>
     */
    public void testInvalidZipException() throws Exception {
        executeTarget("createNonJar");
        File jar = new File(getProject().getProperty("tmp.dir")
                            + "/foo.jar");

        Path myPath = new Path(getProject());
        myPath.setLocation(jar);
        getProject().setUserProperty("build.sysclasspath","ignore");
        loader = getProject().createClassLoader(myPath);
        PrintStream sysErr = System.err;
        try {
            StringBuffer errBuffer = new StringBuffer();
            PrintStream err =
                new PrintStream(new BuildFileTest.AntOutputStream(errBuffer));
            System.setErr(err);
            loader.getResource("foo.txt");
            String log = getLog();
            int startMessage = log.indexOf("CLASSPATH element ");
            assertTrue(startMessage >= 0);
            assertTrue(log.indexOf("foo.jar is not a JAR", startMessage) > 0);
            log = errBuffer.toString();
            startMessage = log.indexOf("CLASSPATH element ");
            assertTrue(startMessage >= 0);
            assertTrue(log.indexOf("foo.jar is not a JAR", startMessage) > 0);
        } finally {
            System.setErr(sysErr);
        }
    }

    private static class GetPackageWrapper extends ClassLoader {
        GetPackageWrapper(ClassLoader parent) {
            super(parent);
        }
        public Package getPackage(String s) {
            return super.getPackage(s);
        }
    }
}
