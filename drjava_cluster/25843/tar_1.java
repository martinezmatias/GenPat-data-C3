/*BEGIN_COPYRIGHT_BLOCK
 *
 * This file is a part of DrJava. Current versions of this project are available
 * at http://sourceforge.net/projects/drjava
 *
 * Copyright (C) 2001-2002 JavaPLT group at Rice University (javaplt@rice.edu)
 *
 * DrJava is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrJava is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * or see http://www.gnu.org/licenses/gpl.html
 *
 * In addition, as a special exception, the JavaPLT group at Rice University
 * (javaplt@rice.edu) gives permission to link the code of DrJava with
 * the classes in the gj.util package, even if they are provided in binary-only
 * form, and distribute linked combinations including the DrJava and the
 * gj.util package. You must obey the GNU General Public License in all
 * respects for all of the code used other than these classes in the gj.util
 * package: Dictionary, HashtableEntry, ValueEnumerator, Enumeration,
 * KeyEnumerator, Vector, Hashtable, Stack, VectorEnumerator.
 *
 * If you modify this file, you may extend this exception to your version of the
 * file, but you are not obligated to do so. If you do not wish to
 * do so, delete this exception statement from your version. (However, the
 * present version of DrJava depends on these classes, so you'd want to
 * remove the dependency first!)
 *
END_COPYRIGHT_BLOCK*/

package edu.rice.cs.drjava.model.repl.newjvm;

import junit.framework.*;
import edu.rice.cs.javaast.parser.*;
import edu.rice.cs.javaast.tree.*;
import edu.rice.cs.javaast.*;

/**
 * Tests the behavior of the InteractionsProcessor.
 * @version $Id$
 */
public final class InteractionsProcessorTest extends TestCase {

  /**
   * InteractionsProcessor to be used in the test methods.
   */
  InteractionsProcessor _ip;

  protected void setUp() {
    _ip = new InteractionsProcessor();
  }

  /**
   * Tests a simple assignment to be sure it works.  More comprehensive
   * parser tests are in edu.rice.cs.javaast.InteractionsParserTest.
   */
  public void testPreProcessAssignment() throws ParseException
  {
    String s = _ip.preProcess("int x = 3;");
    assertEquals("assignment", "int x = 3;", s);
  }
  
  /**
   * Tests that generic statements are type erased.  More comprehensive
   * generic tests are in edu.rice.cs.javaast.TypeEraserTest.
   */
  public void testPreProcessGenerics() throws ParseException
  {
    String s = _ip.preProcess("Vector<String> v = new Vector<String>();");
    assertEquals("type-erased assignment", "Vector v = new Vector();", s);
  }
  
  /**
   * Tests that the correct exception is thrown on a syntax error.
   */
  public void testPreProcessSyntaxError()
  {
    try{
      String s = _ip.preProcess("i+");
      fail("preProcess failed, syntax error expected");
    }
    catch( ParseException pe ){
      // expected this.
    }
  }
  
  /**
   * Tests that the correct exception is thrown on a token manager error.
   */
  public void testPreProcessTokenMgrError()
  {
    try{
      String s = _ip.preProcess("#");
      fail("preProcess failed, token manager error expected");
    }
    catch( ParseException pe ){
      fail("preProcess failed, token manager error expected");
    }
    catch( TokenMgrError tme ){
     // this was what we wanted.
    }
  }
}
