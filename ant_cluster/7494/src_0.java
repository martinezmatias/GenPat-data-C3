/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.Properties;


/**
 * A test sequence listener listening for a batch test run.
 *
 * <i>
 * This code is based on the code from Erich Gamma made for the
 * JUnit plugin for <a href="http://www.eclipse.org">Eclipse</a> and is
 *  merged with code originating from Ant 1.4.x.
 * </i>
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public interface TestRunListener {

    /** Some tests failed. */
    public final static int STATUS_FAILURE = 1;

    /** An error occured. */
    public final static int STATUS_ERROR = 2;

    /**
     * A test has started.
     * @param a testname made of the testname and testcase classname.
     * in the following format: <tt>&lt;testname&gt;(&lt;testcase&gt;)</tt>
     */
    public void onTestStarted(String testname);

    /**
     * A test ended.
     * @param a testname made of the testname and testcase classname.
     * in the following format: <tt>&lt;testname&gt;(&lt;testcase&gt;)</tt>
     */
    public void onTestEnded(String testname);

    /**
     * A test has failed.
     * @param status failure or error status code.
     * @param a testname made of the testname and testcase classname.
     * in the following format: <tt>&lt;testname&gt;(&lt;testcase&gt;)</tt>
     * @param trace the error/failure stacktrace.
     * @todo change this to a testFailure / testError ?
     */
    public void onTestFailed(int status, String testname, String trace);

    /** test logged this line on stdout */
    public void onTestStdOutLine(String testname, String line);

    /** test logged this line on sterr */
    public void onTestStdErrLine(String testname, String line);

    /** these system properties are used on the remote client */
    public void onTestRunSystemProperties(Properties props);

    /** starting a sequence of <tt>testcount</tt> tests. */
    public void onTestRunStarted(int testcount);

    /** ending gracefully the sequence after <tt>elapsedtime</tt> ms. */
    public void onTestRunEnded(long elapsedtime);

    /** stopping the sequence after <tt>elapsedtime</tt> ms. */
    public void onTestRunStopped(long elapsedtime);

}
