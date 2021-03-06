/*BEGIN_COPYRIGHT_BLOCK
 *
 * Copyright (c) 2001-2007, JavaPLT group at Rice University (javaplt@rice.edu)
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

package edu.rice.cs.util;

import edu.rice.cs.util.newjvm.ExecJVM;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class abstracts out creation of another JVM.
 */

public class JVMProcessCreator extends ProcessCreator { 
  protected List<String> _jvmArgs;

  /**
   * Creates a new process creator.
   * @param jvmArgs arguments for the JVM
   * @param command the command and its arguments
   */
  public JVMProcessCreator(List<String> jvmArgs, List<String> command) {
    super(command);
    _jvmArgs = new ArrayList<String>(jvmArgs);
  }
  
  /**
   * Constructs a process creator using varargs.
   * @param command the command and its arguments
   */
  public JVMProcessCreator(List<String> jvmArgs, String... command) {
    super(command);
    _jvmArgs = new ArrayList<String>(jvmArgs);
  }

  /**
   * Get the command and arguments of this process creator.
   * @return command and arguments
   */
  public List<String> command() {
    LinkedList<String> args = new LinkedList<String>();
    args.add(ExecJVM.getExecutable());
    args.addAll(_jvmArgs);
    args.addAll(_command);
    return args;
  }
  
  /**
   * Starts a new JCM process using the attributes of this process creator.
   */
  public Process start() throws IOException {
    LinkedList<String> args = new LinkedList<String>();
    args.add(ExecJVM.getExecutable());
    args.addAll(_jvmArgs);
    args.addAll(_command);
    String[] cmdarray = args.toArray(new String[args.size()]);
    String[] env = null;
    if ((_env!=null) && (_env.size()>0)) {
      env = new String[_env.size()];
      int i = 0;
      for(String key: _env.keySet()) {
        String value = _env.get(key);
        env[i] = key+"="+value;
      }
    }
    return Runtime.getRuntime().exec(cmdarray,env,_dir);
  }
}
