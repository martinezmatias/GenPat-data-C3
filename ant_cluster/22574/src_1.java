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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.BuildException;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FileList;

import org.apache.tools.ant.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.Vector; // 1.1
import java.util.Enumeration; // 1.1

/**
 * This class contains the 'concat' task, used to concatenate a series
 * of files into a single stream. The destination of this stream may
 * be the system console, or a file. The following is a sample
 * invocation:
 *
 * <pre>
 * &lt;concat destfile=&quot;${build.dir}/index.xml&quot;
 *   append=&quot;false&quot;&gt;
 *
 *   &lt;fileset dir=&quot;${xml.root.dir}&quot;
 *     includes=&quot;*.xml&quot; /&gt;
 *
 * &lt;/concat&gt;
 * </pre>
 *
 * @author <a href="mailto:derek@activate.net">Derek Slager</a>
 */
public class Concat extends Task {

    // Attributes.

    /**
     * The destination of the stream. If <code>null</code>, the system
     * console is used.
     */
    private File destinationFile = null;

    /**
     * If the destination file exists, should the stream be appended? 
     * Defaults to <code>false</code>.
     */
    private boolean append = false;

    /**
     * Stores the input file encoding.
     */
    private String encoding = null;

    // Child elements.

    /**
     * This buffer stores the text within the 'concat' element.
     */
    private StringBuffer textBuffer;

    /**
     * Stores a collection of file sets and/or file lists, used to
     * select multiple files for concatenation.
     */
    private Vector fileSets = new Vector(); // 1.1

    // Constructors.

    /**
     * Public, no-argument constructor. Required by Ant.
     */
    public Concat() {}

    // Attribute setters.

    /**
     * Sets the destination file for the stream.
     */
    public void setDestfile(File destinationFile) {
        this.destinationFile = destinationFile;
    }

    /**
     * Sets the behavior when the destination file exists, if set to
     * <code>true</code> the stream data will be appended to the
     * existing file, otherwise the existing file will be
     * overwritten. Defaults to <code>false</code>.
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * Sets the encoding for the input files, used when displaying the
     * data via the console.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    // Nested element creators.

    /**
     * Adds a set of files (nested fileset element).
     */
    public void addFileset(FileSet set) {
        fileSets.addElement(set);
    }

    /**
     * Adds a list of files (nested filelist element).
     */
    public void addFilelist(FileList list) {
        fileSets.addElement(list);
    }

    /**
     * This method adds text which appears in the 'concat' element.
     */
    public void addText(String text) {
        if (textBuffer == null) {
            // Initialize to the size of the first text fragment, with
            // the hopes that it's the only one.
            textBuffer = new StringBuffer(text.length());
        }

        // Append the fragment -- we defer property replacement until
        // later just in case we get a partial property in a fragment.
        textBuffer.append(text);
    }

    /**
     * This method performs the concatenation.
     */
    public void execute() 
        throws BuildException {

        // treat empty nested text as no text
        sanitizeText();

        // Sanity check our inputs.
        if (fileSets.size() == 0 && textBuffer == null) {
            // Nothing to concatenate!
            throw new BuildException("At least one file " + 
                                     "must be provided, or " + 
                                     "some text.");
        }

        // Iterate the FileSet collection, concatenating each file as
        // it is encountered.
        for (Enumeration e = fileSets.elements(); e.hasMoreElements(); ) {

            // Root directory for files.
            File fileSetBase = null;

            // List of files.
            String[] srcFiles = null;

            // Get the next file set, which could be a FileSet or a
            // FileList instance.
            Object next = e.nextElement();

            if (next instanceof FileSet) {

                FileSet fileSet = (FileSet) next;

                // Get a directory scanner from the file set, which will
                // determine the files from the set which need to be
                // concatenated.
                DirectoryScanner scanner = 
                    fileSet.getDirectoryScanner(project);

                // Determine the root path.
                fileSetBase = fileSet.getDir(project);

                // Get the list of files.
                srcFiles = scanner.getIncludedFiles();

            } else if (next instanceof FileList) {

                FileList fileList = (FileList) next;

                // Determine the root path.
                fileSetBase = fileList.getDir(project);

                // Get the list of files.
                srcFiles = fileList.getFiles(project);

            }

            // Concatenate the files.
            catFiles(fileSetBase, srcFiles);
        }

        // Now, cat the inline text, if applicable.
        catText();

        // Reset state to default.
        append = false;
        destinationFile = null;
        encoding = null;
        fileSets = new Vector();
    }

    /**
     * This method concatenates a series of files to a single
     * destination.
     *
     * @param base the base directory for the list of file names.
     *
     * @param files the names of the files to be concatenated,
     * relative to the <code>base</code>.
     */
    private void catFiles(File base, String[] files) {

        // First, create a list of absolute paths for the input files.
        final int len = files.length;
        String[] input = new String[len];
        for (int i = 0; i < len; i++) {

            File current = new File(base, files[i]);

            // Make sure the file exists. This will rarely fail when
            // using file sets, but it could be rather common when
            // using file lists.
            if (!current.exists()) {
                // File does not exist, log an error and continue.
                log("File " + current + " does not exist.", 
                    Project.MSG_ERR);
                continue;
            }

            input[i] = current.getAbsolutePath();
        }

        // Next, perform the concatenation.
        if (destinationFile == null) {

            // No destination file, dump to stdout via Ant's logging
            // interface, which requires that we assume the input data
            // is line-oriented. Generally, this is a safe assumption,
            // as most users won't (intentionally) attempt to cat
            // binary files to the console.
            for (int i = 0; i < len; i++) {

                BufferedReader reader = null;
                try {
                    if (encoding == null) {
                        // Use default encoding.
                        reader = new BufferedReader(
                            new FileReader(input[i])
                        );
                    } else {
                        // Use specified encoding.
                        reader = new BufferedReader(
                            new InputStreamReader(
                                new FileInputStream(input[i]), 
                                encoding
                            )
                        );
                    }

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Log the line, using WARN so it displays in
                        // 'quiet' mode.
                        log(line, Project.MSG_WARN);
                    }

                } catch (IOException ioe) {
                    throw new BuildException("Error while concatenating " + 
                                             "file.", ioe);
                } finally {
                    // Close resources.
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception ignore) {}
                    }
                }
            }

        } else {

            // Use the provided file, making no assumptions about
            // whether or not the file is character or line-oriented.
            final int bufferSize = 1024;
            OutputStream os = null;
            try {
                os = new FileOutputStream(destinationFile.getAbsolutePath(), 
                                          append);

                // This flag should only be recognized for the first
                // file. In the context of a single 'cat', we always
                // want to append.
                append = true;

            } catch (IOException ioe) {
                throw new BuildException("Unable to open destination " + 
                                         "file.", ioe);
            }

            // Concatenate the file.
            try {

                for (int i = 0; i < len; i++) {

                    // Make sure input != output.
                    if (destinationFile.getAbsolutePath().equals(input[i])) {
                        log(destinationFile.getName() + ": input file is " + 
                            "output file.", Project.MSG_WARN);
                    }

                    InputStream is = null;
                    try {
                        is = new FileInputStream(input[i]);
                        byte[] buffer = new byte[bufferSize];
                        while (true) {
                            int bytesRead = is.read(buffer);
                            if (bytesRead == -1) { // EOF
                                break;
                            }

                            // Write the read data.
                            os.write(buffer, 0, bytesRead);
                        }

                        os.flush();

                    } catch (IOException ioex) {
                        throw new BuildException("Error writing file.", ioex);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (Exception ignore) {}
                        }
                    }
                }

            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception ignore) {}
                }
            }
        }
    }

    /**
     * This method concatenates the text which was added inside the
     * 'concat' tags. If the text between the tags consists only of
     * whitespace characters, it is ignored.
     */
    private void catText() {

        // Check the buffer.
        if (textBuffer == null) {
            // No text to write.
            return;
        }

        String text = textBuffer.toString();

        // If using filesets, disallow inline text. This is similar to
        // using GNU 'cat' with file arguments -- stdin is simply
        // ignored.
        if (fileSets.size() > 0) {
            throw new BuildException("Cannot include inline text " + 
                                     "when using filesets.");
        }

        // Replace ${property} strings.
        text = ProjectHelper.replaceProperties(project, text, 
                                               project.getProperties());

        // Set up a writer if necessary.
        FileWriter writer = null;
        if (destinationFile != null) {
            try {
                writer = new FileWriter(destinationFile.getAbsolutePath(), 
                                        append);
            } catch (IOException ioe) {
                throw new BuildException("Error creating destination " + 
                                         "file.", ioe);
            }
        }

        // Reads the text, line by line.
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                new StringReader(text)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (destinationFile == null) {
                    // Log the line, using WARN so it displays in
                    // 'quiet' mode.
                    log(line, Project.MSG_WARN);
                } else {
                    writer.write(line);
                    writer.write(StringUtils.LINE_SEP);
                    writer.flush();
                }
            }

        } catch (IOException ioe) {
            throw new BuildException("Error while concatenating " + 
                                     "text.", ioe);
        } finally {
            // Close resources.
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignore) {}
            }

            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignore) {}
            }
        }
    }

    /**
     * Treat empty nested text as no text.
     *
     * <p>Depending on the XML parser, addText may have been called
     * for &quot;ignorable whitespace&quot; as well.</p>
     */
    private void sanitizeText() {
        if (textBuffer != null) {
            if (textBuffer.toString().trim().length() == 0) {
                textBuffer = null;
            }
        }
    }

}
