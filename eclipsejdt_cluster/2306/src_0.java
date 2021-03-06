/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.formatter;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;

import junit.framework.Test;

public class FormatterCommentsBugsTest extends FormatterCommentsTests {

	private static final IPath OUTPUT_FOLDER = new Path("out");

public static Test suite() {
	return buildModelTestSuite(FormatterCommentsBugsTest.class);
}

public FormatterCommentsBugsTest(String name) {
    super(name);
}

/* (non-Javadoc)
 * @see org.eclipse.jdt.core.tests.formatter.FormatterCommentsTests#getOutputFolder()
 */
IPath getOutputFolder() {
	return OUTPUT_FOLDER;
}

/**
 * @bug 228652: [formatter] New line inserted while formatting a region of a compilation unit.
 * @test Ensure that no new line is inserted before the formatted region
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=228652"
 */
// TODO (frederic) See https://bugs.eclipse.org/bugs/show_bug.cgi?id=49187
public void _testBug228652() {
	String input =
			"package a;\r\n" +
			"\r\n" +
			"public class Test {\r\n" +
			"\r\n" +
			"	private int field;\r\n" +
			"	\r\n" +
			"	/**\r\n" +
			"	 * fds \r\n" +
			"	 */\r\n" +
			"	public void foo() {\r\n" +
			"	}\r\n" +
			"}";

	String expected =
			"package a;\r\n" +
			"\r\n" +
			"public class Test {\r\n" +
			"\r\n" +
			"	private int field;\r\n" +
			"	\r\n" +
			"	/**\r\n" +
			"	 * fds\r\n" +
			"	 */\r\n" +
			"	public void foo() {\r\n" +
			"	}\r\n" +
			"}";

	formatSource(input, expected, CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS, 0, false, 62, 19, null);
}

/**
 * @bug 230944: [formatter] Formatter does not respect /*-
 * @test Ensure that new formatter does not format block comment starting with '/*-'
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=230944"
 */
public void testBug230944a() throws JavaModelException {
	formatUnit("bugs.b230944", "X01.java");
}
public void testBug230944b() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b230944", "X02.java");
}

/**
 * @bug 231263: [formatter] New JavaDoc formatter wrongly indent tags description
 * @test Ensure that new formatter indent tags description as the old one
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=231263"
 */
public void testBug231263() throws JavaModelException {
	formatUnit("bugs.b231263", "BadFormattingSample.java");
}
public void testBug231263a() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b231263", "X.java");
}

/**
 * @bug 231297: [formatter] New JavaDoc formatter wrongly split inline tags before reference
 * @test Ensure that new formatter do not split reference in inline tags
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=231297"
 */
public void testBug231297() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b231297", "X.java");
}
public void testBug231297a() throws JavaModelException {
	this.preferences.comment_line_length = 30;
	formatUnit("bugs.b231297", "X01.java");
}
public void testBug231297b() throws JavaModelException {
	// Difference with old formatter:
	// 1) fixed non formatted inline tag description
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b231297", "X02.java");
}
public void testBug231297c() throws JavaModelException {
	// Difference with old formatter:
	// 1) fixed non formatted inline tag description
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b231297", "X03.java");
}
public void testBug231297d() throws JavaModelException {
	// Difference with old formatter:
	// 1) fixed non formatted inline tag description
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b231297", "X03b.java");
}

/**
 * @bug 232285: [formatter] New comment formatter wrongly formats javadoc header/footer with several contiguous stars
 * @test Ensure that new formatter do not add/remove stars in header and footer
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=232285"
 */
public void testBug232285a() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X01.java");
}
public void testBug232285b() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X01b.java");
}
public void testBug232285c() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X01c.java");
}
public void testBug232285d() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X01d.java");
}
public void testBug232285e() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X01e.java");
}
public void testBug232285f() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X01f.java");
}
public void testBug232285g() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X02.java");
}
public void testBug232285h() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X03.java");
}
public void testBug232285i() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X04.java");
}
public void testBug232285j() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232285", "X04b.java");
}

/**
 * @bug 232488: [formatter] Code formatter scrambles JavaDoc of Generics
 * @test Ensure that comment formatter format properly generic param tags
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=232488"
 */
public void testBug232488() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232488", "X01.java");
}

/**
 * @bug 232466: [formatter] References of inlined tags are still split in certain circumstances
 * @test Ensure that new formatter do not add/remove stars in header and footer
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=232466"
 */
public void testBug232466a() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232466", "X01.java");
}
public void testBug232466b() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b232466", "X02.java");
}

/**
 * @bug 232768: [formatter] does not format block and single line comment if too much selected
 * @test Ensure that the new comment formatter formats comments touched by the selection
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=232768"
 */
public void testBug232768a() throws JavaModelException {
	String source = "public class A {\r\n" +
			"[#\r\n" +
			"        /*\r\n" +
			"         * A block comment \r\n" +
			"         * on two lines\r\n" +
			"         */\r\n" +
			"\r\n#]" +
			"}\r\n" +
			"";
	// TODO fix the incorrect indentation before the javadoc comment (also in 3.3 and 3.4M6)
	formatSource(source,
		"public class A {\n" +
		"\n" +
		"        /*\n" +
		"	 * A block comment on two lines\n" +
		"	 */\n" +
		"\n" +
		"}\n"
	);
}
public void testBug232768b() throws JavaModelException {
	String source = "public class B {\r\n" +
			"[#\r\n" +
			"        public void \r\n" +
			"        foo() {}\r\n" +
			"#]\r\n" +
			"        /*\r\n" +
			"         * A block comment \r\n" +
			"         * on two lines\r\n" +
			"         */\r\n" +
			"\r\n" +
			"}\r\n";
	formatSource(source,
		"public class B {\n" +
		"\n" +
		"	public void foo() {\n" +
		"	}\n" +
		"\n" +
		"	/*\n" +
		"         * A block comment \n" +
		"         * on two lines\n" +
		"         */\n" +
		"\n" +
		"}\n"
	);
}
public void testBug232768_Javadoc01() throws JavaModelException {
	// Selection starts before and ends after the javadoc comment
	String source = "public class C {\n" +
		"	\n" +
		"[#        /**\n" +
		"         * a\n" +
		"         * b\n" +
		"         * c\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		m1  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"#]\n" +
		"\n" +
		"}";
	// TODO fix the incorrect indentation before the javadoc comment (also in 3.3 and 3.4M6)
	formatSource(source,
		"public class C {\n" +
		"	\n" +
		"        /**\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"	void m1() {\n" +
		"\n" +
		"	}     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Javadoc02() throws JavaModelException {
	// Selection starts at javadoc comment begin and ends after it
	String source = "public class C {\n" +
		"	\n" +
		"        [#/**\n" +
		"         * a\n" +
		"         * b\n" +
		"         * c\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		m1  (   )   {\n" +
		"	#]\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// Note that the incorrect indentation before the javadoc is fixed in this test case...
	// This is due to the fact that the region is adapted to include the edit just before the comment
	formatSource(source,
		"public class C {\n" +
		"\n" +
		"	/**\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"	void m1() {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Javadoc03() throws JavaModelException {
	// Selection starts inside the javadoc comment and ends after it
	String source = "public class C {\n" +
		"	\n" +
		"        /**\n" +
		"         * a\n" +
		"         * b\n" +
		"         * [#c\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		#]m1  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// Note that the incorrect indentation before the javadoc is fixed in this test case...
	// This is due to the fact that the region is adapted to include the edit just before the comment
	formatSource(source,
		"public class C {\n" +
		"\n" +
		"	/**\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"	void m1  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Javadoc04() throws JavaModelException {
	// Selection starts before the javadoc comment and ends at its end
	String source = "public class C {\n" +
		"[#	\n" +
		"        /**\n" +
		"         * a\n" +
		"         * b\n" +
		"         * c\n" +
		"         * d\n" +
		"         * .\n" +
		"         */#]\n" +
		"        void		m1  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// TODO fix the incorrect indentation before the javadoc comment (also in 3.3 and 3.4M6)
	formatSource(source,
		"public class C {\n" +
		"	\n" +
		"        /**\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"        void		m1  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Javadoc05() throws JavaModelException {
	// Selection starts before the javadoc comment and ends inside it
	String source = "[#   public     class			C{\n" +
		"	\n" +
		"        /**\n" +
		"         * a\n" +
		"         * b\n" +
		"         * c#]\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		m1  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// Note that the incorrect indentation before the javadoc is fixed in this test case...
	// This is due to the fact that the region is adapted to include the edit just before the comment
	formatSource(source,
		"public class C {\n" +
		"\n" +
		"	/**\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"        void		m1  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Javadoc06() throws JavaModelException {
	// Selection starts and ends inside the javadoc comment
	String source = "   public     class			C{    \n" +
		"	\n" +
		"        /**\n" +
		"         * a\n" +
		"         * b\n" +
		"         * [#c#]\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		m1  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// Note that the incorrect indentation before the javadoc is fixed in this test case...
	// This is due to the fact that the region is adapted to include the edit just before the comment
	formatSource(source,
		"   public     class			C{\n" +
		"\n" +
		"	/**\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"        void		m1  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Block01() throws JavaModelException {
	// Selection starts before and ends after the block comment
	String source = "public class D {\n" +
		"	\n" +
		"[#        /*\n" +
		"         * a\n" +
		"         * b\n" +
		"         * c\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		m2  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"#]\n" +
		"\n" +
		"}";
	// TODO fix the incorrect indentation before the block comment (also in 3.3 and 3.4M6)
	formatSource(source,
		"public class D {\n" +
		"	\n" +
		"        /*\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"	void m2() {\n" +
		"\n" +
		"	}     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Block02() throws JavaModelException {
	// Selection starts at block comment begin and ends after it
	String source = "public class D {\n" +
		"	\n" +
		"        [#/*\n" +
		"         * a\n" +
		"         * b\n" +
		"         * c\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		m2  (   )   {\n" +
		"	#]\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// Note that the incorrect indentation before the javadoc is fixed in this test case...
	// This is due to the fact that the region is adapted to include the edit just before the comment
	formatSource(source,
		"public class D {\n" +
		"\n" +
		"	/*\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"	void m2() {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Block03() throws JavaModelException {
	// Selection starts inside the block comment and ends after it
	String source = "public class D {\n" +
		"	\n" +
		"        /*\n" +
		"         * a\n" +
		"         * b\n" +
		"         * [#c\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		#]m2  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// Note that the incorrect indentation before the javadoc is fixed in this test case...
	// This is due to the fact that the region is adapted to include the edit just before the comment
	formatSource(source,
		"public class D {\n" +
		"\n" +
		"	/*\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"	void m2  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Block04() throws JavaModelException {
	// Selection starts before the block comment and ends at its end
	String source = "public class D {\n" +
		"[#	\n" +
		"        /*\n" +
		"         * a\n" +
		"         * b\n" +
		"         * c\n" +
		"         * d\n" +
		"         * .\n" +
		"         */#]\n" +
		"        void		m2  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// TODO fix the incorrect indentation before the block comment (also in 3.3 and 3.4M6)
	formatSource(source,
		"public class D {\n" +
		"	\n" +
		"        /*\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"        void		m2  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Block05() throws JavaModelException {
	// Selection starts before the block comment and ends inside it
	String source = "[#   public     class			D{\n" +
		"	\n" +
		"        /*\n" +
		"         * a\n" +
		"         * b\n" +
		"         * c#]\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		m2  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// Note that the incorrect indentation before the javadoc is fixed in this test case...
	// This is due to the fact that the region is adapted to include the edit just before the comment
	formatSource(source,
		"public class D {\n" +
		"\n" +
		"	/*\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"        void		m2  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Block06() throws JavaModelException {
	// Selection starts and ends inside the block comment
	String source = "   public     class			D{    \n" +
		"	\n" +
		"        /*\n" +
		"         * a\n" +
		"         * b\n" +
		"         * [#c#]\n" +
		"         * d\n" +
		"         * .\n" +
		"         */\n" +
		"        void		m2  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}";
	// Note that the incorrect indentation before the javadoc is fixed in this test case...
	// This is due to the fact that the region is adapted to include the edit just before the comment
	formatSource(source,
		"   public     class			D{\n" +
		"\n" +
		"	/*\n" +
		"	 * a b c d .\n" +
		"	 */\n" +
		"        void		m2  (   )   {\n" +
		"	\n" +
		"        }     \n" +
		"\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Line01() throws JavaModelException {
	// Selection starts before and ends after the line comment
	String source = "public class E {\n" +
		"	\n" +
		"\n" +
		"[#        void            m3()         { // this        is        a    bug\n" +
		"\n" +
		"        }\n" +
		"#]   \n" +
		"}";
	// TODO fix the incorrect indentation before the method declaration (also in 3.3 and 3.4M6)
	// Note that the line comment wasn't formatted using 3.3 and 3.4 M6
	formatSource(source,
		"public class E {\n" +
		"	\n" +
		"\n" +
		"        void m3() { // this is a bug\n" +
		"\n" +
		"	}\n" +
		"   \n" +
		"}"
	);
}
public void testBug232768_Line02() throws JavaModelException {
	// Selection starts at line comment begin and ends after it
	String source = "public class E {\n" +
		"	\n" +
		"\n" +
		"        void            m3()         { [#// this        is        a    bug\n" +
		"\n#]" +
		"        }\n" +
		"   \n" +
		"}";
	// Note that the line comment wasn't formatted using 3.3 and 3.4 M6
	formatSource(source,
		"public class E {\n" +
		"	\n" +
		"\n" +
		"        void            m3()         { // this is a bug\n" +
		"\n" +
		"        }\n" +
		"   \n" +
		"}"
	);
}
public void testBug232768_Line03() throws JavaModelException {
	// Selection starts inside line comment and ends after it
	String source = "public class E {\n" +
		"	\n" +
		"\n" +
		"        void            m3()         { // this        [#is        a    bug\n" +
		"\n" +
		"        }\n" +
		"   \n" +
		"              }#]";
	// Note that the line comment wasn't formatted using 3.3 and 3.4 M6
	formatSource(source,
		"public class E {\n" +
		"	\n" +
		"\n" +
		"        void            m3()         { // this is a bug\n" +
		"\n" +
		"	}\n" +
		"\n" +
		"}"
	);
}
public void testBug232768_Line04() throws JavaModelException {
	// Selection starts before the line comment and ends at its end
	String source = "public class E {[#       \n" +
		"	\n" +
		"\n" +
		"        void            m3()         { // this        is        a    bug#]\n" +
		"\n" +
		"        }\n" +
		"   \n" +
		"		}";
	// Note that the line comment wasn't formatted using 3.3 and 3.4 M6
	formatSource(source,
		"public class E {\n" +
		"\n" +
		"	void m3() { // this is a bug\n" +
		"\n" +
		"        }\n" +
		"   \n" +
		"		}"
	);
}
public void testBug232768_Line05() throws JavaModelException {
	// Selection starts before the line comment and ends inside it
	String source = "public class E {       \n" +
		"	\n" +
		"\n[#" +
		"        void            m3()         { // this   #]     is        a    bug\n" +
		"\n" +
		"        }\n" +
		"   \n" +
		"		}";
	// Note that the line comment wasn't formatted using 3.3 and 3.4 M6
	formatSource(source,
		"public class E {       \n" +
		"	\n" +
		"\n" +
		"        void m3() { // this is a bug\n" +
		"\n" +
		"        }\n" +
		"   \n" +
		"		}"
	);
}
public void testBug232768_Line06() throws JavaModelException {
	// Selection starts and ends inside the line comment
	String source = "public class E {       \n" +
		"	\n" +
		"\n" +
		"        void            m3()         { // this        is        [#a#]    bug\n" +
		"\n" +
		"        }\n" +
		"   \n" +
		"              }";
	// Note that the line comment wasn't formatted using 3.3 and 3.4 M6
	formatSource(source,
		"public class E {       \n" +
		"	\n" +
		"\n" +
		"        void            m3()         { // this is a bug\n" +
		"\n" +
		"        }\n" +
		"   \n" +
		"              }"
	);
}
public void testBug232768_Line07() throws JavaModelException {
	// Selection starts and ends inside the line comment
	String source = "public class F {\n" +
		"	\n" +
		"\n" +
		"        void            m3()         {     \n" +
		"[#        	// this        is        a    bug\n" +
		"#]\n" +
		"        }\n" +
		"   \n" +
		"}";
	// Note that the line comment wasn't formatted using 3.3 and 3.4 M6
	formatSource(source,
		"public class F {\n" +
		"	\n" +
		"\n" +
		"        void            m3()         {     \n" +
		"        	// this is a bug\n" +
		"\n" +
		"        }\n" +
		"   \n" +
		"}"
	);
}
public void testBug232768_Line08() throws JavaModelException {
	// Selection starts and ends inside the line comment
	String source = "public class G {\n" +
		"	void foo() {\n" +
		"	\n" +
		"        // Now we parse one of 'CustomActionTagDependent',\n" +
		"        // 'CustomActionJSPContent', or 'CustomActionScriptlessContent'.\n" +
		"        // depending on body-content in TLD.\n" +
		"	}\n" +
		"}";
	// Note that the line comment wasn't formatted using 3.3 and 3.4 M6
	formatSource(source,
		"public class G {\n" +
		"	void foo() {\n" +
		"\n" +
		"		// Now we parse one of 'CustomActionTagDependent',\n" +
		"		// 'CustomActionJSPContent', or 'CustomActionScriptlessContent'.\n" +
		"		// depending on body-content in TLD.\n" +
		"	}\n" +
		"}"
	);
}

/**
 * @bug 232788: [formatter] Formatter misaligns stars when formatting block comments
 * @test Ensure that block comment formatting is correct even with indentation size=1
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=232788"
 */
public void testBug232788_Tabs01() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	this.preferences.tab_size = 1;
	this.preferences.indentation_size = 1;
	formatUnit("bugs.b232788", "X01_tabs.java");
}
public void testBug232788_Spaces01() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	this.preferences.tab_char = DefaultCodeFormatterOptions.SPACE;
	this.preferences.tab_size = 1;
	this.preferences.indentation_size = 1;
	formatUnit("bugs.b232788", "X01_spaces.java");
}
public void testBug232788_Mixed01() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	this.preferences.tab_char = DefaultCodeFormatterOptions.MIXED;
	this.preferences.tab_size = 1;
	this.preferences.indentation_size = 1;
	formatUnit("bugs.b232788", "X01_mixed.java");
}
public void testBug232788_Tabs02() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	this.preferences.tab_size = 0;
	this.preferences.indentation_size = 0;
	formatUnit("bugs.b232788", "X02_tabs.java");
}
public void testBug232788_Spaces02() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	this.preferences.tab_char = DefaultCodeFormatterOptions.SPACE;
	this.preferences.tab_size = 0;
	this.preferences.indentation_size = 0;
	formatUnit("bugs.b232788", "X02_spaces.java");
}
public void testBug232788_Mixed02() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	this.preferences.tab_char = DefaultCodeFormatterOptions.MIXED;
	this.preferences.tab_size = 0;
	this.preferences.indentation_size = 0;
	formatUnit("bugs.b232788", "X02_mixed.java");
}
public void testBug232788_Tabs03() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	this.preferences.tab_size = 1;
	this.preferences.indentation_size = 1;
	formatUnit("bugs.b232788", "X03_tabs.java");
}
public void testBug232788_Spaces03() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	this.preferences.tab_char = DefaultCodeFormatterOptions.SPACE;
	this.preferences.tab_size = 1;
	this.preferences.indentation_size = 1;
	formatUnit("bugs.b232788", "X03_spaces.java");
}
public void testBug232788_Mixed03() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	this.preferences.tab_char = DefaultCodeFormatterOptions.MIXED;
	this.preferences.tab_size = 1;
	this.preferences.indentation_size = 1;
	formatUnit("bugs.b232788", "X03_mixed.java");
}

/**
 * @bug 233011: [formatter] Formatting edited lines has problems (esp. with comments)
 * @test Ensure that new comment formatter format all comments concerned by selections
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=233011"
 */
public void testBug233011() throws JavaModelException {
	String source = "\n" +
		"public class E01 {\n" +
		"        /** \n" +
		"         * Javadoc      [# #]            \n" +
		"         * comment\n" +
		"         */\n" +
		"        /*\n" +
		"         * block           [# #]            \n" +
		"         * comment\n" +
		"         */\n" +
		"        // single          [# #]            line comment\n" +
		"}";
	formatSource(source,
		"\n" +
		"public class E01 {\n" +
		"	/**\n" +
		"	 * Javadoc comment\n" +
		"	 */\n" +
		"	/*\n" +
		"	 * block comment\n" +
		"	 */\n" +
		"	// single line comment\n" +
		"}"
	);
}

/**
 * @bug 233228: [formatter] line comments which contains \\u are not correctly formatted
 * @test Ensure that the new formatter is not screwed up by invalid unicode value inside comments
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=233228"
 */
public void testBug233228a() throws JavaModelException {
	formatUnit("bugs.b233228", "X01.java");
}
public void testBug233228b() throws JavaModelException {
	formatUnit("bugs.b233228", "X01b.java");
}
public void testBug233228c() throws JavaModelException {
	formatUnit("bugs.b233228", "X01c.java");
}
public void testBug233228d() throws JavaModelException {
	formatUnit("bugs.b233228", "X02.java");
}
public void testBug233228e() throws JavaModelException {
	formatUnit("bugs.b233228", "X03.java");
}

/**
 * @bug 233224: [formatter] Xdoclet tags looses @ on format
 * @test Ensure that doclet tags are preserved while formatting
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=233224"
 */
public void testBug233224() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	formatUnit("bugs.b233224", "X01.java");
}

/**
 * @bug 233259: [formatter] html tag should not be split by formatter
 * @test Ensure that html tag is not split by the new comment formatter
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=233259"
 */
public void testBug233259a() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	String source =
		"public class X {\n" +
		"        /**\n" +
		"         * @see <a href=\"http://0\">Test</a>\n" +
		"         */\n" +
		"        void foo() {\n" +
		"        }\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"	/**\n" +
		"	 * @see <a href=\"http://0\">Test</a>\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug233259b() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	// difference with 3.3 formatter:
	// split html reference as this allow not to go over the max line width
	String source =
		"public class X {\n" +
		"        /**\n" +
		"         * @see <a href=\"http://0123\">Test</a>\n" +
		"         */\n" +
		"        void foo() {\n" +
		"        }\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"	/**\n" +
		"	 * @see <a\n"+
		"	 *      href=\"http://0123\">Test</a>\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug233259c() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	String source =
		"public class X {\n" +
		"        /**\n" +
		"         * @see <a href=\"http://012346789\">Test</a>\n" +
		"         */\n" +
		"        void foo() {\n" +
		"        }\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"	/**\n" +
		"	 * @see <a\n" +
		"	 *      href=\"http://012346789\">Test</a>\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug233259d() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	String source =
		"public class X {\n" +
		"        /**\n" +
		"         * @see <a href=\"http://012346789012346789012346789\">Test</a>\n" +
		"         */\n" +
		"        void foo() {\n" +
		"        }\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"	/**\n" +
		"	 * @see <a\n" +
		"	 *      href=\"http://012346789012346789012346789\">Test</a>\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n"
	);
}

/**
 * @bug 237942: [formatter] String references are put on next line when over the max line length
 * @test Ensure that string reference is not put on next line when over the max line width
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=237942"
 */
public void testBug237942a() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	String source =
		"public class X {\n" +
		"        /**\n" +
		"         * @see \"string reference: 01234567\"\n" +
		"         */\n" +
		"        void foo() {\n" +
		"        }\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"	/**\n" +
		"	 * @see \"string reference: 01234567\"\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug237942b() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	// difference with 3.3 formatter:
	// do not split string reference as this can lead to javadoc syntax error
	String source =
		"public class X {\n" +
		"        /**\n" +
		"         * @see \"string reference: 012345678\"\n" +
		"         */\n" +
		"        void foo() {\n" +
		"        }\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"	/**\n" +
		"	 * @see \"string reference: 012345678\"\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug237942c() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	// difference with 3.3 formatter:
	// do not split string reference as this can lead to javadoc syntax error
	String source =
		"public class X {\n" +
		"        /**\n" +
		"         * @see \"string reference: 01234567 90\"\n" +
		"         */\n" +
		"        void foo() {\n" +
		"        }\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"	/**\n" +
		"	 * @see \"string reference: 01234567 90\"\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug237942d() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	// difference with 3.3 formatter:
	// do not split string reference as this can lead to javadoc syntax error
	String source =
		"public class X {\n" +
		"        /**\n" +
		"         * @see \"string reference: 01234567890123\"\n" +
		"         */\n" +
		"        void foo() {\n" +
		"        }\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"	/**\n" +
		"	 * @see \"string reference: 01234567890123\"\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n"
	);
}

/**
 * @bug 234336: [formatter] JavaDocTestCase.testMultiLineCommentIndent* tests fail in I20080527-2000 build
 * @test Ensure that new comment formatter format all comments concerned by selections
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=234336"
 */
public void testBug234336() throws JavaModelException {
	String source =
		"public class Test {\n" +
		"	[#/**\n" +
		"			 * test test\n" +
		"				 */#]\n" +
		"}\n";
	formatSource(source,
		"public class Test {\n" +
		"	/**\n" +
		"	 * test test\n" +
		"	 */\n" +
		"}\n",
		CodeFormatter.K_JAVA_DOC,
		1 /* indentation level */
	);
}

/**
 * @bug 236230: [formatter] SIOOBE while formatting a compilation unit.
 * @test Ensure that no exception occurs while formatting
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=236230"
 */
public void testBug236230() throws JavaModelException {
	String source =
		"/**\n" +
		" * Need a javadoc comment before to get the exception.\n" +
		" */\n" +
		"public class Test {\n" +
		"\n" +
		"  /**\n" +
		"   * <p>If there is an authority, it is:\n" +
		"   * <pre>\n" +
		"   *   //authority/device/pathSegment1/pathSegment2...</pre>\n" +
		"   */\n" +
		"  public String devicePath() {\n" +
		"	  return null;\n" +
		"  }\n" +
		"}\n";
	formatSource(source,
		"/**\n" +
		" * Need a javadoc comment before to get the exception.\n" +
		" */\n" +
		"public class Test {\n" +
		"\n" +
		"	/**\n" +
		"	 * <p>\n" +
		"	 * If there is an authority, it is:\n" +
		"	 * \n" +
		"	 * <pre>\n" +
		"	 * // authority/device/pathSegment1/pathSegment2...\n" +
		"	 * </pre>\n" +
		"	 */\n" +
		"	public String devicePath() {\n" +
		"		return null;\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug236230b() throws JavaModelException {
	String source =
		"/**\n" +
		" * Need a javadoc comment before to get the exception.\n" +
		" */\n" +
		"public class Test {\n" +
		"\n" +
		"  /**\n" +
		"   * <p>If there is an authority, it is:\n" +
		"   * <pre>//authority/device/pathSegment1/pathSegment2...</pre>\n" +
		"   */\n" +
		"  public String devicePath() {\n" +
		"	  return null;\n" +
		"  }\n" +
		"}\n";
	formatSource(source,
		"/**\n" +
		" * Need a javadoc comment before to get the exception.\n" +
		" */\n" +
		"public class Test {\n" +
		"\n" +
		"	/**\n" +
		"	 * <p>\n" +
		"	 * If there is an authority, it is:\n" +
		"	 * \n" +
		"	 * <pre>\n" +
		"	 * // authority/device/pathSegment1/pathSegment2...\n" +
		"	 * </pre>\n" +
		"	 */\n" +
		"	public String devicePath() {\n" +
		"		return null;\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug236230c() throws JavaModelException {
	this.preferences.comment_format_header = true;
	String source =
		"/**\n" +
		" * Need a javadoc comment before to get the exception.\n" +
		" */\n" +
		"public class Test {\n" +
		"\n" +
		"  /**\n" +
		"   * <p>If there is an authority, it is:\n" +
		"   * <pre>\n" +
		"			import java.util.List;\n" +
		"			//            CU         snippet\n" +
		"			public class X implements List {}\n" +
		"		</pre>\n" +
		"   */\n" +
		"  public String devicePath() {\n" +
		"	  return null;\n" +
		"  }\n" +
		"}\n";
	formatSource(source,
		"/**\n" +
		" * Need a javadoc comment before to get the exception.\n" +
		" */\n" +
		"public class Test {\n" +
		"\n" +
		"	/**\n" +
		"	 * <p>\n" +
		"	 * If there is an authority, it is:\n" +
		"	 * \n" +
		"	 * <pre>\n" +
		"	 * import java.util.List;\n" +
		"	 * \n" +
		"	 * // CU snippet\n" +
		"	 * public class X implements List {\n" +
		"	 * }\n" +
		"	 * </pre>\n" +
		"	 */\n" +
		"	public String devicePath() {\n" +
		"		return null;\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug236230d() throws JavaModelException {
	String source =
		"/**\n" +
		" * Need a javadoc comment before to get the exception.\n" +
		" */\n" +
		"public class Test {\n" +
		"\n" +
		"  /**\n" +
		"   * <p>If there is an authority, it is:\n" +
		"   * <pre>\n" +
		"			//class	body		snippet\n" +
		"			public class X {}\n" +
		"		</pre>\n" +
		"   */\n" +
		"  public String devicePath() {\n" +
		"	  return null;\n" +
		"  }\n" +
		"}\n";
	// TODO (frederic) line comment should be formatted when F_INCLUDE_COMMENTS
	// flag will work for all snippet kinds
	formatSource(source,
		"/**\n" +
		" * Need a javadoc comment before to get the exception.\n" +
		" */\n" +
		"public class Test {\n" +
		"\n" +
		"	/**\n" +
		"	 * <p>\n" +
		"	 * If there is an authority, it is:\n" +
		"	 * \n" +
		"	 * <pre>\n" +
		"	 * //class	body		snippet\n" +
		"	 * public class X {\n" +
		"	 * }\n" +
		"	 * </pre>\n" +
		"	 */\n" +
		"	public String devicePath() {\n" +
		"		return null;\n" +
		"	}\n" +
		"}\n"
	);
}
// Following tests showed possible regressions while implementing the fix...
public void testBug236230e() throws JavaModelException {
	String source =
		"public class X02 {\n" +
		"\n" +
		"\n" +
		"	/**\n" +
		"	/**\n" +
		"	 * Removes the Java nature from the project.\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n";
	formatSource(source,
		"public class X02 {\n" +
		"\n" +
		"	/**\n" +
		"	 * /** Removes the Java nature from the project.\n" +
		"	 */\n" +
		"	void foo() {\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug236230f() throws JavaModelException {
	String source =
		"public class X03 {\n" +
		"  /** The value of <tt>System.getProperty(\"java.version\")<tt>. **/\n" +
		"  static final String JAVA_VERSION = System.getProperty(\"java.version\");\n" +
		"\n" +
		"}\n";
	formatSource(source,
		"public class X03 {\n" +
		"	/** The value of <tt>System.getProperty(\"java.version\")<tt>. **/\n" +
		"	static final String JAVA_VERSION = System.getProperty(\"java.version\");\n" +
		"\n" +
		"}\n"
	);
}

/**
 * @bug 237051: [formatter] Formatter insert blank lines after javadoc if javadoc contains Commons Attributes @@ annotations
 * @test Ensure that Commons Attributes @@ annotations do not screw up the comment formatter
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=237051"
 */
public void testBug237051() throws JavaModelException {
	String source =
		"public interface Test {\n" +
		"/**\n" +
		" * foo\n" +
		" * \n" +
		" * @@Foo(\"foo\")\n" +
		" */\n" +
		"Object doSomething(Object object) throws Exception;\n" +
		"}\n" +
		"\n";
	formatSource(source,
		"public interface Test {\n" +
		"	/**\n" +
		"	 * foo\n" +
		"	 * \n" +
		"	 * @@Foo(\"foo\")\n" +
		"	 */\n" +
		"	Object doSomething(Object object) throws Exception;\n" +
		"}\n"
	);
}
public void testBug237051b() throws JavaModelException {
	String source =
		"public interface Test {\n" +
		"/**\n" +
		" * foo\n" +
		" * @@Foo(\"foo\")\n" +
		" */\n" +
		"Object doSomething(Object object) throws Exception;\n" +
		"}\n" +
		"\n";
	formatSource(source,
		"public interface Test {\n" +
		"	/**\n" +
		"	 * foo\n" +
		"	 * \n" +
		"	 * @@Foo(\"foo\")\n" +
		"	 */\n" +
		"	Object doSomething(Object object) throws Exception;\n" +
		"}\n"
	);
}
public void testBug237051c() throws JavaModelException {
	String source =
		"public class X {\n" +
		"\n" +
		"	/**\n" +
		"	 * Returns the download rate in bytes per second.  If the rate is unknown,\n" +
		"	 * @{link {@link #UNKNOWN_RATE}} is returned.\n" +
		"	 * @return the download rate in bytes per second\n" +
		"	 */\n" +
		"	public long getTransferRate() {\n" +
		"		return -1;\n" +
		"	}\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"\n" +
		"	/**\n" +
		"	 * Returns the download rate in bytes per second. If the rate is unknown,\n" +
		"	 * \n" +
		"	 * @{link {@link #UNKNOWN_RATE} is returned.\n" +
		"	 * @return the download rate in bytes per second\n" +
		"	 */\n" +
		"	public long getTransferRate() {\n" +
		"		return -1;\n" +
		"	}\n" +
		"}\n"
	);
}
public void testBug237051d() throws JavaModelException {
	String source =
		"public class X {\n" +
		"\n" +
		"	\n" +
		"	/**\n" +
		"	 * Copies specified input stream to the output stream. Neither stream\n" +
		"	 * is closed as part of this operation.\n" +
		"	 * \n" +
		"	 * @param is input stream\n" +
		"	 * @param os output stream\n" +
		"	 * @param monitor progress monitor\n" +
		"     * @param expectedLength - if > 0, the number of bytes from InputStream will be verified\n" +
		"	 * @@return the offset in the input stream where copying stopped. Returns -1 if end of input stream is reached.\n" +
		"	 * @since 2.0\n" +
		"	 */\n" +
		"	public static long foo() {\n" +
		"		return -1;\n" +
		"	}\n" +
		"}\n";
	formatSource(source,
		"public class X {\n" +
		"\n" +
		"	/**\n" +
		"	 * Copies specified input stream to the output stream. Neither stream is\n" +
		"	 * closed as part of this operation.\n" +
		"	 * \n" +
		"	 * @param is\n" +
		"	 *            input stream\n" +
		"	 * @param os\n" +
		"	 *            output stream\n" +
		"	 * @param monitor\n" +
		"	 *            progress monitor\n" +
		"	 * @param expectedLength\n" +
		"	 *            - if > 0, the number of bytes from InputStream will be\n" +
		"	 *            verified\n" +
		"	 * @@return the offset in the input stream where copying stopped. Returns -1\n" +
		"	 *          if end of input stream is reached.\n" +
		"	 * @since 2.0\n" +
		"	 */\n" +
		"	public static long foo() {\n" +
		"		return -1;\n" +
		"	}\n" +
		"}\n"
	);
}

/**
 * @bug 238090: [formatter] Formatter insert blank lines after javadoc if javadoc contains Commons Attributes @@ annotations
 * @test Ensure that no unexpected empty new lines are added while formatting a reference
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=238090"
 */
public void testBug238090() throws JavaModelException {
	this.preferences.comment_line_length = 40;
	String source =
		"package test.bugs;\n" +
		"public class LongNameClass {\n" +
		"/**\n" +
		" * @see test.bugs.\n" +
		" * LongNameClass#longNameMethod(java.lang.String)\n" +
		" */\n" +
		"public void foo() {\n" +
		"}\n" +
		"\n" +
		"void longNameMethod(String str) {\n" +
		"}\n" +
		"}\n";
	formatSource(source,
		"package test.bugs;\n" +
		"\n" +
		"public class LongNameClass {\n" +
		"	/**\n" +
		"	 * @see test.bugs.\n" +
		"	 *      LongNameClass#longNameMethod(java.lang.String)\n" +
		"	 */\n" +
		"	public void foo() {\n" +
		"	}\n" +
		"\n" +
		"	void longNameMethod(String str) {\n" +
		"	}\n" +
		"}\n"
	);
}

/**
 * @bug 238210: [formatter] CodeFormatter wraps line comments without whitespaces
 * @test Ensure that line without spaces are not wrapped by the comment formatter
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=238210"
 */
public void testBug238210() throws JavaModelException {
	String source = 
		"/**\n" + 
		" * LineCommentTestCase\n" + 
		" * \n" + 
		" * Formatting this compilation unit with line comment enabled and comment line width set to 100 or\n" + 
		" * lower will result in both protected region comments to be wrapped although they do not contain\n" + 
		" * any whitespace (excluding leading whitespace which should be / is being ignored altogether)\n" + 
		" * \n" + 
		" * @author Axel Faust, PRODYNA AG\n" + 
		" */\n" + 
		"public class LineCommentTestCase {\n" + 
		"\n" + 
		"    public void someGeneratedMethod() {\n" + 
		"        //protected-region-start_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"        // some manually written code\n" + 
		"        // protected-region-end_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"    }\n" + 
		"}\n";
	formatSource(source,
		"/**\n" + 
		" * LineCommentTestCase\n" + 
		" * \n" + 
		" * Formatting this compilation unit with line comment enabled and comment line\n" + 
		" * width set to 100 or lower will result in both protected region comments to be\n" + 
		" * wrapped although they do not contain any whitespace (excluding leading\n" + 
		" * whitespace which should be / is being ignored altogether)\n" + 
		" * \n" + 
		" * @author Axel Faust, PRODYNA AG\n" + 
		" */\n" + 
		"public class LineCommentTestCase {\n" + 
		"\n" + 
		"	public void someGeneratedMethod() {\n" + 
		"		// protected-region-start_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"		// some manually written code\n" + 
		"		// protected-region-end_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"	}\n" + 
		"}\n"
	);
}
public void _testBug238210_block() throws JavaModelException {
	String source = 
		"public class BlockCommentTestCase {\n" + 
		"\n" + 
		"    public void someGeneratedMethod() {\n" + 
		"        /*\n" + 
		"         * protected-region-start_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"         * some manually written code\n" + 
		"         * protected-region-end_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"         */\n" + 
		"    }\n" + 
		"}\n";
	formatSource(source,
		"public class BlockCommentTestCase {\n" + 
		"\n" + 
		"	public void someGeneratedMethod() {\n" + 
		"		/*\n" + 
		"		 * protected-region-start_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"		 * some manually written code\n" + 
		"		 * protected-region-end_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"		 */\n" + 
		"	}\n" + 
		"}\n"
	);
}
public void _testBug238210_javadoc() throws JavaModelException {
	String source = 
		"public class JavadocCommentTestCase {\n" + 
		"\n" + 
		"    public void someGeneratedMethod() {\n" + 
		"        /**\n" + 
		"         * protected-region-start_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"         * some manually written code\n" + 
		"         * protected-region-end_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"         */\n" + 
		"    }\n" + 
		"}\n";
	formatSource(source,
		"public class JavadocCommentTestCase {\n" + 
		"\n" + 
		"	public void someGeneratedMethod() {\n" + 
		"		/**\n" + 
		"		 * protected-region-start_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"		 * some manually written code\n" + 
		"		 * protected-region-end_[id=_14_0_1_3dd20592_1202209856234_914658_24183_someGeneratedMethod]\n" + 
		"		 */\n" + 
		"	}\n" + 
		"}\n"
	);
}
}
