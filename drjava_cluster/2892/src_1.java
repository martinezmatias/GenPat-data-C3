/*BEGIN_COPYRIGHT_BLOCK
 *
 * Copyright (c) 2001-2010, JavaPLT group at Rice University (drjava@rice.edu)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the names of DrJava, the JavaPLT group, Rice University, nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software is Open Source Initiative approved Open Source Software.
 * Open Source Initative Approved is a trademark of the Open Source Initiative.
 * 
 * This file is part of DrJava.  Download the current version of this project
 * from http://www.drjava.org/ or http://sourceforge.net/projects/drjava/
 * 
 * END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.ui;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import edu.rice.cs.drjava.DrJava;
import edu.rice.cs.drjava.DrJavaRoot;
import edu.rice.cs.drjava.config.OptionConstants;


/** A file filter for files with extensions ".java" and ".gj". Used in the file choosers for open and save. 
 *  @version $Id$
 */
public class JavaSourceFilter extends FileFilter {
  
  /** Returns true if the file's extension matches Java or GJ. */
  public boolean accept(File f) {
    if (f.isDirectory()) {
      return true;
    }
    String extension = getExtension(f);
    if (extension != null) {
      switch (DrJava.getConfig().getSetting(OptionConstants.LANGUAGE_LEVEL)) {
        case (DrJavaRoot.FULL_JAVA): return (extension.equals("java") || extension.equals("j"));
        case (DrJavaRoot.ELEMENTARY_LEVEL): return extension.equals("dj0");
        case (DrJavaRoot.INTERMEDIATE_LEVEL): return extension.equals("dj1");
        case (DrJavaRoot.ADVANCED_LEVEL): return extension.equals("dj2");
      }
    }
    return false;
  }

  /** @return A description of this filter to display. */
  public String getDescription() {
    switch (DrJava.getConfig().getSetting(OptionConstants.LANGUAGE_LEVEL)) {
        case (DrJavaRoot.FULL_JAVA): return "Java source files";
        case (DrJavaRoot.ELEMENTARY_LEVEL): return "Elementary source files (.dj0)";
        case (DrJavaRoot.INTERMEDIATE_LEVEL): return "Intermediate source files (.dj1)";
        case (DrJavaRoot.ADVANCED_LEVEL): return "Advanced source files (.dj2)";
      }
    return "Java source files";
  }

  /* Get the extension of a file. */
  public static String getExtension(File f) {
    String ext = null;
    String s = f.getName();
    int i = s.lastIndexOf('.');
    if (i > 0 && i < s.length() - 1) {
      ext = s.substring(i + 1).toLowerCase();
    }
    return ext;
  }
}



