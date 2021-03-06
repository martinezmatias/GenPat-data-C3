/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

import java.lang.reflect.Method;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.types.XCatalog;
import org.xml.sax.EntityResolver;

/**
 * A Task to process via XSLT a set of XML documents. This is
 * useful for building views of XML based documentation.
 * arguments:
 * <ul>
 * <li>basedir
 * <li>destdir
 * <li>style
 * <li>includes
 * <li>excludes
 * </ul>
 * Of these arguments, the <b>sourcedir</b> and <b>destdir</b> are required.
 * <p>
 * This task will recursively scan the sourcedir and destdir
 * looking for XML documents to process via XSLT. Any other files,
 * such as images, or html files in the source directory will be
 * copied into the destination directory.
 *
 * @version $Revision$ 
 *
 * @author <a href="mailto:kvisco@exoffice.com">Keith Visco</a>
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 * @author <a href="stefan.bodewig@epost.de">Stefan Bodewig</a>
 *
 * @ant.task name="style" category="xml"
 */

public class XSLTProcess extends MatchingTask implements XSLTLogger {
    /** destination directory */
    private File destDir = null;
    
    /** where to find the source XML file, default is the project's basedir */
    private File baseDir = null;
    
    /** XSL stylesheet */
    private String xslFile = null;
    
    /** extension of the files produced by XSL processing */
    private String targetExtension = ".html";

    /** additional parameters to be passed to the stylesheets */
    private Vector params = new Vector();
    
    /** Input XML document to be used */
    private File inFile = null;
    
    /** Output file */
    private File outFile = null;
    
    /** The name of the XSL processor to use */
    private String processor;
    
    /** Classpath to use when trying to load the XSL processor */
    private Path classpath = null;
    
    /** The Liason implementation to use to communicate with the XSL 
     *  processor */
    private XSLTLiaison liaison;
    
    /** Flag which indicates if the stylesheet has been loaded into 
     *  the processor */
    private boolean stylesheetLoaded = false;
    
    /** force output of target files even if they already exist */
    private boolean force = false;
    
    /** Utilities used for file operations */
    private FileUtils fileUtils;
    
    /** XSL output method to be used */
    private String outputtype = null;
    
    /** for resolving entities such as dtds */
    private XCatalog xcatalog;
    
    /** Name of the TRAX Liason class */
    private static final String TRAX_LIAISON_CLASS =
                        "org.apache.tools.ant.taskdefs.optional.TraXLiaison";

    /** Name of the now-deprecated XSLP Liason class */                        
    private static final String XSLP_LIASON_CLASS = 
                        "org.apache.tools.ant.taskdefs.optional.XslpLiaison";

    /** Name of the Xalan liason class */                            
    private static final String XALAN_LIASON_CLASS =
                        "org.apache.tools.ant.taskdefs.optional.XalanLiaison";
                        
    /**
     * Whether to style all files in the included directories as well.
     *
     * @since 1.35, Ant 1.5
     */
    private boolean performDirectoryScan = true;

    /**
     * Creates a new XSLTProcess Task.
     **/
    public XSLTProcess() {
        fileUtils = FileUtils.newFileUtils();
    } //-- XSLTProcess
    
    /**
     * Whether to style all files in the included directories as well.
     *
     * @param b true if files in included directories are processed.
     * @since 1.35, Ant 1.5
     */
    public void setScanIncludedDirectories(boolean b) {
        performDirectoryScan = b;
    }
    
    /**
     * Executes the task.
     *
     * @exception BuildException if there is an execution problem.
     */
    public void execute() throws BuildException {
        DirectoryScanner scanner;
        String[]         list;
        String[]         dirs;
        
        if (xslFile == null) {
            throw new BuildException("no stylesheet specified", location);
        }
        
        if (baseDir == null) {
            baseDir = project.resolveFile(".");
        }
        
        liaison = getLiaison();
        
        // check if liaison wants to log errors using us as logger
        if (liaison instanceof XSLTLoggerAware) {
            ((XSLTLoggerAware)liaison).setLogger(this);
        }
        
        log("Using " + liaison.getClass().toString(), Project.MSG_VERBOSE);
        
        File stylesheet = project.resolveFile(xslFile);
        if (!stylesheet.exists()) {
            stylesheet = fileUtils.resolveFile(baseDir, xslFile);
            /*
             * shouldn't throw out deprecation warnings before we know,
             * the wrong version has been used.
             */
            if (stylesheet.exists()) {
                log("DEPRECATED - the style attribute should be relative " 
                    + "to the project\'s");
                log("             basedir, not the tasks\'s basedir.");
            }
        }
        
        // if we have an in file and out then process them
        if (inFile != null && outFile != null) {
            process(inFile, outFile, stylesheet);
            return;
        }
        
        /*
         * if we get here, in and out have not been specified, we are
         * in batch processing mode.
         */
        
        //-- make sure Source directory exists...
        if (destDir == null ) {
            String msg = "destdir attributes must be set!";
            throw new BuildException(msg);
        }
        scanner = getDirectoryScanner(baseDir);
        log("Transforming into " + destDir, Project.MSG_INFO);
        
        // Process all the files marked for styling
        list = scanner.getIncludedFiles();
        for (int i = 0; i < list.length; ++i) {
            process( baseDir, list[i], destDir, stylesheet );
        }
        if (performDirectoryScan) {
            // Process all the directories marked for styling
            dirs = scanner.getIncludedDirectories();
            for (int j = 0; j < dirs.length; ++j){
                list = new File(baseDir, dirs[j]).list();
                for (int i = 0; i < list.length; ++i) {
                    process( baseDir, list[i], destDir, stylesheet );
                }
            }
        }
    } //-- execute
    
    /**
     * Set whether to check dependencies, or always generate.
     *
      * @param force true if always generate.
     **/
    public void setForce(boolean force) {
        this.force = force;
    } //-- setForce
    
    /**
     * Set the base directory.
     *
     * @param dir the base directory
     **/
    public void setBasedir(File dir) {
        baseDir = dir;
    } //-- setSourceDir
    
    /**
     * Set the destination directory into which the XSL result
     * files should be copied to
     * @param dir the name of the destination directory
     **/
    public void setDestdir(File dir) {
        destDir = dir;
    } //-- setDestDir
    
    /**
     * Set the desired file extension to be used for the target
     * @param name the extension to use
     **/
    public void setExtension(String name) {
        targetExtension = name;
    } //-- setDestDir
    
    /**
     * Sets the file to use for styling relative to the base directory
     * of this task.
     *
     * @param xslFile the stylesheet to use
     */
    public void setStyle(String xslFile) {
        this.xslFile = xslFile;
    }
    
    /**
     * Set the classpath to load the Processor through (attribute).
     *
     * @param classpath the classpath to use when loading the XSL processor
     */
    public void setClasspath(Path classpath) {
        createClasspath().append(classpath);
    }
    
    /**
     * Set the classpath to load the Processor through (nested element).
     *
     * @return a path instance to be configured by the Ant core.
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(project);
        }
        return classpath.createPath();
    }
    
    /**
     * Set the classpath to load the Processor through via reference
     * (attribute).
     *
     * @param r the id of the Ant path instance to act as the classpath 
     *          for loading the XSL processor
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }
    
    /**
     * Set the name of the XSL processor to use 
     *
     * @param processor the name of the XSL processor
     */
    public void setProcessor(String processor) {
        this.processor = processor;
    }
    
    /**
     * store the xcatalog for resolving entities
     * 
     * @param xcatalog the xcatalog instance to use to look up DTDs
     */
    public void addXcatalog(XCatalog xcatalog) {
        this.xcatalog = xcatalog;
    }
    
    /**
     * Load processor here instead of in setProcessor - this will be
     * called from within execute, so we have access to the latest
     * classpath.
     *
     * @param proc the name of the processor to load.
     * @exception Exception if the processor cannot be loaded.
     */
    private void resolveProcessor(String proc) throws Exception {
        if (proc.equals("trax")) {
            final Class clazz =
                loadClass(TRAX_LIAISON_CLASS);
            liaison = (XSLTLiaison)clazz.newInstance();
        } else if (proc.equals("xslp")) {
            log("DEPRECATED - xslp processor is deprecated. Use trax or "
                + "xalan instead.");
            final Class clazz =
                loadClass(XSLP_LIASON_CLASS);
            liaison = (XSLTLiaison) clazz.newInstance();
        } else if (proc.equals("xalan")) {
            final Class clazz =
                loadClass(XALAN_LIASON_CLASS);
            liaison = (XSLTLiaison)clazz.newInstance();
        } else {
            liaison = (XSLTLiaison) loadClass(proc).newInstance();
        }
    }
    
    /**
     * Load named class either via the system classloader or a given
     * custom classloader.
     *
     * @param classname the name of the class to load.
     * @return the requested class.
     * @exception Exception if the class could not be loaded.
     */
    private Class loadClass(String classname) throws Exception {
        if (classpath == null) {
            return Class.forName(classname);
        } else {
            AntClassLoader al = new AntClassLoader(project, classpath);
            Class c = al.loadClass(classname);
            AntClassLoader.initializeClass(c);
            return c;
        }
    }
    
    /**
     * Sets an out file
     *
     * @param outFile the output File instance.
     */
    public void setOut(File outFile){
        this.outFile = outFile;
    }
    
    /**
     * Sets an input xml file to be styled
     *
     * @param inFile the input file
     */
    public void setIn(File inFile){
        this.inFile = inFile;
    }
    
    /**
     * Processes the given input XML file and stores the result
     * in the given resultFile.
     *
     * @param baseDir the base directory for resolving files.
     * @param xmlFile the input file
     * @param destDir the destination directory
     * @param stylesheet the stylesheet to use.
     * @exception BuildException if the processing fails.
     */
    private void process(File baseDir, String xmlFile, File destDir,
                         File stylesheet)
        throws BuildException {
        
        String fileExt = targetExtension;
        File   outFile = null;
        File   inFile = null;
        
        try {
            long styleSheetLastModified = stylesheet.lastModified();
            inFile = new File(baseDir, xmlFile);

            if (inFile.isDirectory()) {
                log("Skipping " + inFile + " it is a directory.",
                    Project.MSG_VERBOSE);
                return;
            }
            
            int dotPos = xmlFile.lastIndexOf('.');
            if (dotPos > 0) {
                outFile = new File(destDir, 
                    xmlFile.substring(0, xmlFile.lastIndexOf('.')) + fileExt);
            } else {
                outFile = new File(destDir, xmlFile + fileExt);
            }
            if (force ||
                inFile.lastModified() > outFile.lastModified() ||
                styleSheetLastModified > outFile.lastModified()) {
                ensureDirectoryFor( outFile );
                log("Processing " + inFile + " to " + outFile);
                
                configureLiaison(stylesheet);
                liaison.transform(inFile, outFile);
            }
        }
        catch (Exception ex) {
            // If failed to process document, must delete target document,
            // or it will not attempt to process it the second time
            log("Failed to process " + inFile, Project.MSG_INFO);
            if (outFile != null) {
                outFile.delete();
            }
            
            throw new BuildException(ex);
        }
        
    } //-- processXML
    
    /**
     * Process the input file to the output file with the given stylesheet.
     *
     * @param inFile the input file to process.
     * @param outFile the detination file.
     * @param stylesheet the stylesheet to use.
     * @exception BuildException if the processing fails.
     */
    private void process(File inFile, File outFile, File stylesheet) 
         throws BuildException {
        try {
            long styleSheetLastModified = stylesheet.lastModified();
            log("In file " + inFile + " time: " + inFile.lastModified(),
                Project.MSG_DEBUG);
            log("Out file " + outFile + " time: " + outFile.lastModified(),
                Project.MSG_DEBUG);
            log("Style file " + xslFile + " time: " + styleSheetLastModified,
                Project.MSG_DEBUG);
            if (force ||
                inFile.lastModified() > outFile.lastModified() ||
                styleSheetLastModified > outFile.lastModified()) {
                ensureDirectoryFor( outFile );
                log("Processing " + inFile + " to " + outFile, 
                    Project.MSG_INFO);
                configureLiaison(stylesheet);
                liaison.transform(inFile, outFile);
            }
        } catch (Exception ex) {
            log("Failed to process " + inFile, Project.MSG_INFO);
            if (outFile != null) {
                outFile.delete();
            }
            throw new BuildException(ex);
        }
    }
    
    /**
     * Ensure the directory exists for a given file 
     *
     * @param targetFile the file for which the directories are required.
     * @exception BuildException if the directories cannot be created.
     */
    private void ensureDirectoryFor(File targetFile) 
         throws BuildException {
        File directory = new File( targetFile.getParent() );
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new BuildException("Unable to create directory: "
                + directory.getAbsolutePath() );
            }
        }
    }
    
    /**
     * Get the Liason implementation to use in processing.
     *
     * @return an instance of the XSLTLiason interface.
     */
    protected XSLTLiaison getLiaison() {
        // if processor wasn't specified, see if TraX is available.  If not,
        // default it to xslp or xalan, depending on which is in the classpath
        if (liaison == null) {
            if (processor != null) {
                try {
                    resolveProcessor(processor);
                } catch (Exception e) {
                    throw new BuildException(e);
                }
            } else {
                try {
                    resolveProcessor("trax");
                } catch (Throwable e1) {
                    try {
                        resolveProcessor("xalan");
                    } catch (Throwable e2) {
                        try {
                            resolveProcessor("xslp");
                        } catch (Throwable e3) {
                            e3.printStackTrace();
                            e2.printStackTrace();
                            throw new BuildException(e1);
                        }
                    }
                }
            }
        }
        return liaison;
    }
    
    /**
     * Create an instance of an XSL parameter for configuration by Ant.
     *
     * @return an instance of the Param class to be configured.
     */
    public Param createParam() {
        Param p = new Param();
        params.addElement(p);
        return p;
    }
    
    /**
     * The Param inner class used to store XSL parameters
     */
    public class Param {
        /** The parameter name */
        private String name = null;
        
        /** The parameter's XSL expression */
        private String expression = null;
        
        /** 
         * Set the parameter name.
         * 
         * @param name the name of the parameter.
         */
        public void setName(String name){
            this.name = name;
        }
        
        /** 
         * The XSL expression for the parameter value
         *
         * @param expression the XSL expression representing the 
         *   parameter's value.
         */
        public void setExpression(String expression){
            this.expression = expression;
        }
        
        /**
         * Get the parameter name
         *
         * @return the parameter name
         * @exception BuildException if the name is not set.
         */
        public String getName() throws BuildException{
            if (name == null) {
                throw new BuildException("Name attribute is missing.");
            }
            return name;
        }
        
        /**
         * Get the parameter expression
         *
         * @return the parameter expression
         * @exception BuildException if the expression is not set.
         */
        public String getExpression() throws BuildException{
            if (expression == null) {
                throw new BuildException("Expression attribute is missing.");
            }
            return expression;
        }
    }
    
    /**
     * Set the output type to use for the transformation.  Only "xml" (the
     * default) is guaranteed to work for all parsers.  Xalan2 also
     * supports "html" and "text".
     * @param type the output method to use
     */
    public void setOutputtype(String type) {
        this.outputtype = type;
    }
    
    /**
     * Loads the stylesheet and set xsl:param parameters.
     *
     * @param stylesheet the file form which to load the stylesheet.
     * @exception BuildException if the stylesheet cannot be loaded.
     */
    protected void configureLiaison(File stylesheet) throws BuildException {
        if (stylesheetLoaded) {
            return;
        }
        stylesheetLoaded = true;
        
        try {
            log( "Loading stylesheet " + stylesheet, Project.MSG_INFO);
            liaison.setStylesheet( stylesheet );
            for (Enumeration e = params.elements(); e.hasMoreElements(); ) {
                Param p = (Param)e.nextElement();
                liaison.addParam( p.getName(), p.getExpression() );
            }
            // if liaison is a TraxLiason, use XCatalog as the entity
            // resolver
            if (liaison.getClass().getName().equals(TRAX_LIAISON_CLASS) &&
                xcatalog != null) {
                log("Configuring TraxLiaison and calling entity resolver",
                    Project.MSG_DEBUG);
                Method resolver = liaison.getClass()
                                    .getDeclaredMethod("setEntityResolver", 
                                        new Class[] {EntityResolver.class});
                resolver.invoke(liaison, new Object[] {xcatalog});
            }
        } catch (Exception ex) {
            log("Failed to read stylesheet " + stylesheet, Project.MSG_INFO);
            throw new BuildException(ex);
        }
    }
    
} //-- XSLTProcess
