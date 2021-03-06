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
package org.apache.tools.ant.types.resources;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.Reference;

/**
 * A Resource representation of something loadable via a Java classloader.
 * @since Ant 1.7
 */
public class JavaResource extends AbstractClasspathResource {

    /**
     * Default constructor.
     */
    public JavaResource() {
    }

    /**
     * Construct a new JavaResource using the specified name and
     * classpath.
     *
     * @param name   the resource name.
     * @param path   the classpath.
     */
    public JavaResource(String name, Path path) {
        setName(name);
        classpath = path;
    }

    /**
     * open the inpout stream from a specific classloader
     * @param cl the classloader to use. Will be null if the system classloader is used
     * @return an open input stream for the resource
     * @throws IOException if an error occurs.
     */
    protected InputStream openInputStream(ClassLoader cl) throws IOException {
        return cl == null ? ClassLoader.getSystemResourceAsStream(getName())
            : cl.getResourceAsStream(getName());
    }

    /**
     * Compare this JavaResource to another Resource.
     * @param another the other Resource against which to compare.
     * @return a negative integer, zero, or a positive integer as this
     * JavaResource is less than, equal to, or greater than the
     * specified Resource.
     */
    public int compareTo(Object another) {
        if (isReference()) {
            return ((Comparable) getCheckedRef()).compareTo(another);
        }
        if (another.getClass().equals(getClass())) {
            JavaResource otherjr = (JavaResource) another;
            if (!getName().equals(otherjr.getName())) {
                return getName().compareTo(otherjr.getName());
            }
            if (loader != otherjr.loader) {
                if (loader == null) {
                    return -1;
                }
                if (otherjr.loader == null) {
                    return 1;
                }
                return loader.getRefId().compareTo(otherjr.loader.getRefId());
            }
            Path p = getClasspath();
            Path op = otherjr.getClasspath();
            if (p != op) {
                if (p == null) {
                    return -1;
                }
                if (op == null) {
                    return 1;
                }
                return p.toString().compareTo(op.toString());
            }
            return 0;
        }
        return super.compareTo(another);
    }

}
