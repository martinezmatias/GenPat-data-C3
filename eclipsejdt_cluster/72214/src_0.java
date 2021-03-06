/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.dom;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class ASTConverterJavadocTest extends ConverterTestSetup {

	// Test counters
	protected static int[] testCounters = { 0, 0, 0, 0 };
	// Unicode tests
	protected static boolean unicode = false;
	// Doc Comment support
	static final String DOC_COMMENT_SUPPORT = System.getProperty("doc.support");
	final String docCommentSupport;

	// List of comments read from source of test
	private final int LINE_COMMENT = 100;
	private final int BLOCK_COMMENT =200;
	private final int DOC_COMMENT = 300;
	List comments = new ArrayList();
	private String chars;
	// List of tags contained in each comment read from test source.
	List allTags = new ArrayList();
	// Current compilation unit
	protected ICompilationUnit sourceUnit;
	// Test package binding
	protected boolean resolveBinding = true;
	protected boolean packageBinding = true;
	// Debug
	protected String prefix = "";
	protected boolean debug = false;
	protected StringBuffer problems;
	protected String compilerOption = JavaCore.IGNORE;
	protected List failures;
	protected boolean stopOnFailure = true;
	protected IJavaProject currentProject;

	/**
	 * @param name
	 * @param support
	 */
	public ASTConverterJavadocTest(String name, String support) {
		super(name);
		this.docCommentSupport = support;
	}
	/**
	 * @param name
	 */
	public ASTConverterJavadocTest(String name) {
		this(name, JavaCore.ENABLED);
	}

	public static Test suite() {
		TestSuite suite = new Suite(ASTConverterJavadocTest.class.getName());		
//		String param = System.getProperty("unicode");
//		if ("true".equals(param)) {
//			unicode = true;
//		}
		if (true) {
			if (DOC_COMMENT_SUPPORT == null) {
				buildSuite(suite, JavaCore.ENABLED);
				buildSuite(suite, JavaCore.DISABLED);
			} else {
				String support = DOC_COMMENT_SUPPORT==null ? JavaCore.DISABLED : (DOC_COMMENT_SUPPORT.equals(JavaCore.DISABLED)?JavaCore.DISABLED:JavaCore.ENABLED);
				buildSuite(suite, support);
			}
			return suite;
		}
		suite.addTest(new ASTConverterJavadocTest("testBug54776"));
		return suite;
	}

	public static void buildSuite(TestSuite suite, String support) {
		Class c = ASTConverterJavadocTest.class;
		Method[] methods = c.getMethods();
		for (int i = 0, max = methods.length; i < max; i++) {
			if (methods[i].getName().startsWith("test")) { //$NON-NLS-1$
				suite.addTest(new ASTConverterJavadocTest(methods[i].getName(), support));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#getName()
	 */
	public String getName() {
		return "Doc "+this.docCommentSupport+" - "+super.getName();
	}
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		testCounters[0]++;
		this.failures = new ArrayList();
		this.problems = new StringBuffer();
	}
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		int size = failures.size();
		String title = size+" positions/bindings were incorrect in "+getName();
		if (size == 0) {
			testCounters[1]++;
		} else if (problems.length() > 0) {
			if (debug) {
				System.out.println("Compilation warnings/errors occured:");
				System.out.println(problems.toString());
			}
			testCounters[2]++;
		} else {
			testCounters[3]++;
			System.out.println(title+":");
			for (int i=0; i<size; i++) {
				System.out.println("	- "+failures.get(i));
			}
		}
		if (!stopOnFailure) {
			assertTrue(title, size==0 || problems.length() > 0);
		}
		// put default options on project
		if (this.currentProject != null) this.currentProject.setOptions(JavaCore.getOptions());
	}
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	public void tearDownSuite() throws Exception {
		super.tearDownSuite();
		if (testCounters[0] != testCounters[1]) {
			NumberFormat intFormat = NumberFormat.getInstance();
			intFormat.setMinimumIntegerDigits(3);
			intFormat.setMaximumIntegerDigits(3);
			System.out.println("=====================================");
			System.out.println(intFormat.format(testCounters[0])+" tests have been executed:");
			System.out.println("  - "+intFormat.format(testCounters[1])+" tests have been actually executed.");
			System.out.println("  - "+intFormat.format(testCounters[2])+" tests were skipped due to compilation errors.");
			System.out.println("  - "+intFormat.format(testCounters[3])+" tests failed.");
		}
	}

	public ASTNode runConversion(char[] source, String unitName, IJavaProject project) {
		ASTParser parser = ASTParser.newParser(AST.LEVEL_2_0);
		parser.setSource(source);
		parser.setUnitName(unitName);
		parser.setProject(project);
		parser.setResolveBindings(this.resolveBinding);
		return parser.createAST(null);
	}

// NOT USED
//	class ASTConverterJavadocFlattener extends ASTVisitor {
//
//		/**
//		 * The string buffer into which the serialized representation of the AST is
//		 * written.
//		 */
//		private StringBuffer buffer;
//		
//		private String comment;
//		
//		/**
//		 * Creates a new AST printer.
//		 */
//		ASTConverterJavadocFlattener(String comment) {
//			this.buffer = new StringBuffer();
//			this.comment = comment;
//		}
//		
//		/**
//		 * Returns the string accumulated in the visit.
//		 *
//		 * @return the serialized 
//		 */
//		public String getResult() {
//			return this.buffer.toString();
//		}
//		
//		/**
//		 * Resets this printer so that it can be used again.
//		 */
//		public void reset() {
//			this.buffer.setLength(0);
//		}
//
//		/*
//		 * @see ASTVisitor#visit(ArrayType)
//		 */
//		public boolean visit(ArrayType node) {
//			node.getComponentType().accept(this);
//			this.buffer.append("[]");//$NON-NLS-1$
//			return false;
//		}
//	
//		/*
//		 * @see ASTVisitor#visit(BlockComment)
//		 * @since 3.0
//		 */
//		public boolean visit(BlockComment node) {
//			this.buffer.append(this.comment);
//			return false;
//		}
//	
//		/*
//		 * @see ASTVisitor#visit(Javadoc)
//		 */
//		public boolean visit(Javadoc node) {
//			
//			// ignore deprecated node.getComment()
//			this.buffer.append("/**");//$NON-NLS-1$
//			ASTNode e = null;
//			int start = 3;
//			for (Iterator it = node.tags().iterator(); it.hasNext(); ) {
//				e = (ASTNode) it.next();
//				try {
//					this.buffer.append(this.comment.substring(start, e.getStartPosition()-node.getStartPosition()));
//					start = e.getStartPosition()-node.getStartPosition();
//				} catch (IndexOutOfBoundsException ex) {
//					// do nothing
//				}
//				e.accept(this);
//				start += e.getLength();
//			}
//			this.buffer.append(this.comment.substring(start, node.getLength()));
//			return false;
//		}
//	
//		/*
//		 * @see ASTVisitor#visit(LineComment)
//		 * @since 3.0
//		 */
//		public boolean visit(LineComment node) {
//			this.buffer.append(this.comment);
//			return false;
//		}
//	
//		/*
//		 * @see ASTVisitor#visit(MemberRef)
//		 * @since 3.0
//		 */
//		public boolean visit(MemberRef node) {
//			if (node.getQualifier() != null) {
//				node.getQualifier().accept(this);
//			}
//			this.buffer.append("#");//$NON-NLS-1$
//			node.getName().accept(this);
//			return true;
//		}
//		
//		/*
//		 * @see ASTVisitor#visit(MethodRef)
//		 * @since 3.0
//		 */
//		public boolean visit(MethodRef node) {
//			if (node.getQualifier() != null) {
//				node.getQualifier().accept(this);
//			}
//			this.buffer.append("#");//$NON-NLS-1$
//			node.getName().accept(this);
//			this.buffer.append("(");//$NON-NLS-1$
//			for (Iterator it = node.parameters().iterator(); it.hasNext(); ) {
//				MethodRefParameter e = (MethodRefParameter) it.next();
//				e.accept(this);
//				if (it.hasNext()) {
//					this.buffer.append(",");//$NON-NLS-1$
//				}
//			}
//			this.buffer.append(")");//$NON-NLS-1$
//			return true;
//		}
//		
//		/*
//		 * @see ASTVisitor#visit(MethodRefParameter)
//		 * @since 3.0
//		 */
//		public boolean visit(MethodRefParameter node) {
//			node.getType().accept(this);
//			if (node.getName() != null) {
//				this.buffer.append(" ");//$NON-NLS-1$
//				node.getName().accept(this);
//			}
//			return true;
//		}
//
//		/*
//		 * @see ASTVisitor#visit(TagElement)
//		 * @since 3.0
//		 */
//		public boolean visit(TagElement node) {
//			Javadoc javadoc = null;
//			int start = 0;
//			if (node.isNested()) {
//				// nested tags are always enclosed in braces
//				this.buffer.append("{");//$NON-NLS-1$
//				javadoc = (Javadoc) node.getParent().getParent();
//				start++;
//			} else {
//				javadoc = (Javadoc) node.getParent();
//			}
//			start += node.getStartPosition()-javadoc.getStartPosition();
//			if (node.getTagName() != null) {
//				this.buffer.append(node.getTagName());
//				start += node.getTagName().length();
//			}
//			for (Iterator it = node.fragments().iterator(); it.hasNext(); ) {
//				ASTNode e = (ASTNode) it.next();
//				try {
//					this.buffer.append(this.comment.substring(start, e.getStartPosition()-javadoc.getStartPosition()));
//					start = e.getStartPosition()-javadoc.getStartPosition();
//				} catch (IndexOutOfBoundsException ex) {
//					// do nothing
//				}
//				start += e.getLength();
//				e.accept(this);
//			}
//			if (node.isNested()) {
//				this.buffer.append("}");//$NON-NLS-1$
//			}
//			return true;
//		}
//		
//		/*
//		 * @see ASTVisitor#visit(TextElement)
//		 * @since 3.0
//		 */
//		public boolean visit(TextElement node) {
//			this.buffer.append(node.getText());
//			return false;
//		}
//
//		/*
//		 * @see ASTVisitor#visit(PrimitiveType)
//		 */
//		public boolean visit(PrimitiveType node) {
//			this.buffer.append(node.getPrimitiveTypeCode().toString());
//			return false;
//		}
//	
//		/*
//		 * @see ASTVisitor#visit(QualifiedName)
//		 */
//		public boolean visit(QualifiedName node) {
//			node.getQualifier().accept(this);
//			this.buffer.append(".");//$NON-NLS-1$
//			node.getName().accept(this);
//			return false;
//		}
//
//		/*
//		 * @see ASTVisitor#visit(SimpleName)
//		 */
//		public boolean visit(SimpleName node) {
//			this.buffer.append(node.getIdentifier());
//			return false;
//		}
//
//		/*
//		 * @see ASTVisitor#visit(SimpleName)
//		 */
//		public boolean visit(SimpleType node) {
//			node.getName().accept(this);
//			return false;
//		}
//	}

	private char getNextChar(char[] source, int idx) {
			// get next char
			char ch = source[idx];
			int charLength = 1;
			int pos = idx;
			this.chars = null;
			if (ch == '\\' && source[idx+1] == 'u') {
				//-------------unicode traitement ------------
				int c1, c2, c3, c4;
				charLength++;
				while (source[idx+charLength] == 'u') charLength++;
				if (((c1 = Character.getNumericValue(source[idx+charLength++])) > 15
					|| c1 < 0)
					|| ((c2 = Character.getNumericValue(source[idx+charLength++])) > 15 || c2 < 0)
					|| ((c3 = Character.getNumericValue(source[idx+charLength++])) > 15 || c3 < 0)
					|| ((c4 = Character.getNumericValue(source[idx+charLength++])) > 15 || c4 < 0)) {
					return ch;
				}
				ch = (char) (((c1 * 16 + c2) * 16 + c3) * 16 + c4);
				this.chars = new String(source, pos, charLength);
			}
			return ch;
	}
	/*
	 * Convert Javadoc source to match Javadoc.toString().
	 * Store converted comments and their corresponding tags respectively
	 * in this.comments and this.allTags fields
	 */
	protected void setSourceComment(char[] source) throws ArrayIndexOutOfBoundsException {
		this.comments = new ArrayList();
		this.allTags = new ArrayList();
		StringBuffer buffer = null;
		int comment = 0;
		boolean end = false, lineStarted = false;
		String tag = null;
		List tags = new ArrayList();
		int length = source.length;
		char previousChar=0, currentChar=0;
		for (int i=0; i<length;) {
			previousChar = currentChar;
			// get next char
			currentChar = getNextChar(source, i);
			i += (this.chars==null) ? 1 : this.chars.length();

			// 
			switch (comment) {
				case 0: 
					switch (currentChar) {
						case '/':
							comment = 1; // first char for comments...
							buffer = new StringBuffer();
							if (this.chars == null) buffer.append(currentChar);
							else buffer.append(this.chars);
							break;
						case '\'':
							while (i<length) {
								// get next char
								currentChar = getNextChar(source, i);
								i += (this.chars==null) ? 1 : this.chars.length();
								if (currentChar == '\\') {
									// get next char
									currentChar = getNextChar(source, i);
									i += (this.chars==null) ? 1 : this.chars.length();
								} else {
									if (currentChar == '\'') {
										break;
									}
								}
							}
							break;
						case '"':
							while (i<length) {
								// get next char
								currentChar = getNextChar(source, i);
								i += (this.chars==null) ? 1 : this.chars.length();
								if (currentChar == '\\') {
									// get next char
									currentChar = getNextChar(source, i);
									i += (this.chars==null) ? 1 : this.chars.length();
								} else {
									if (currentChar == '"') {
										// get next char
										currentChar = getNextChar(source, i);
										if (currentChar == '"') {
											i += (this.chars==null) ? 1 : this.chars.length();
										} else {
											break;
										}
									}
								}
							}
							break;
					}
					break;
				case 1: // first '/' has been found...
					switch (currentChar) {
						case '/':
							if (this.chars == null) buffer.append(currentChar);
							else buffer.append(this.chars);
							comment = LINE_COMMENT;
							break;
						case '*':
							if (this.chars == null) buffer.append(currentChar);
							else buffer.append(this.chars);
							comment = 2; // next step
							break;
						default:
							comment = 0;
							break;
					}
					break;
				case 2: // '/*' has been found...
					if (currentChar == '*') {
						comment = 3; // next step...
					} else {
						comment = BLOCK_COMMENT;
					}
					if (this.chars == null) buffer.append(currentChar);
					else buffer.append(this.chars);
					break;
				case 3: // '/**' has bee found, verify that's not an empty block comment
					if (this.chars == null) buffer.append(currentChar);
					else buffer.append(this.chars);
					if (currentChar == '/') { // empty block comment
						this.comments.add(buffer.toString());
						this.allTags.add(new ArrayList());
						comment = 0;
					} else {
						comment = DOC_COMMENT;
					}
					break;
				case LINE_COMMENT:
					if (currentChar == '\r' || currentChar == '\n') {
						/*
						if (currentChar == '\r' && source[i+1] == '\n') {
							buffer.append(source[++i]);
						}
						*/
						comment = 0;
						this.comments.add(buffer.toString());
						this.allTags.add(tags);
					} else {
						if (this.chars == null) buffer.append(currentChar);
						else buffer.append(this.chars);
					}
					break;
				case DOC_COMMENT:
					if (tag != null) {
						if (currentChar >= 'a' && currentChar <= 'z') {
							tag += currentChar;
						} else {
							tags.add(tag);
							tag = null;
						}
					}
					switch (currentChar) {
						case '@':
							if (!lineStarted || previousChar == '{') {
								tag = "";
								lineStarted = true;
							}
							break;
						case '\r':
						case '\n':
							lineStarted = false;
							break;
						case '*':
							break;
						default:
							if (!Character.isWhitespace(currentChar)) {
								lineStarted = true;
							}
					}
				case BLOCK_COMMENT:
					if (this.chars == null) buffer.append(currentChar);
					else buffer.append(this.chars);
					if (end && currentChar == '/') {
						comment = 0;
						lineStarted = false;
						this.comments.add(buffer.toString());
						this.allTags.add(tags);
						tags = new ArrayList();
					}
					end = currentChar == '*';
					break;
				default:
					// do nothing
					break;
			}
		}
	}

	/*
	 * Convert Javadoc source to match Javadoc.toString().
	 * Store converted comments and their corresponding tags respectively
	 * in this.comments and this.allTags fields
	 */
	char[] getUnicodeSource(char[] source) {
		int length = source.length;
		int unicodeLength = length*6;
		char[] unicodeSource = new char[unicodeLength];
		int u=0;
		for (int i=0; i<length; i++) {
			// get next char
			if (source[i] == '\\' && source[i+1] == 'u') {
				//-------------unicode traitement ------------
				int c1, c2, c3, c4;
				unicodeSource[u++] = source[i];
				unicodeSource[u++] = source[++i];
				if (((c1 = Character.getNumericValue(source[i+1])) > 15
					|| c1 < 0)
					|| ((c2 = Character.getNumericValue(source[i+2])) > 15 || c2 < 0)
					|| ((c3 = Character.getNumericValue(source[i+3])) > 15 || c3 < 0)
					|| ((c4 = Character.getNumericValue(source[i+4])) > 15 || c4 < 0)) {
					throw new RuntimeException("Invalid unicode in source at "+i);
				}
				for (int j=0; j<4; j++) unicodeSource[u++] = source[++i];
			} else {
				unicodeSource[u++] = '\\';
				unicodeSource[u++] = 'u';
				unicodeSource[u++] = '0';
				unicodeSource[u++] = '0';
				int val = source[i]/16;
				unicodeSource[u++] = (char) (val<10 ? val+ 0x30 : val-10+0x61);
				val = source[i]%16;
				unicodeSource[u++] = (char) (val<10 ? val+ 0x30 : val-10+0x61);
			}
		}
		// Return one well sized array
		if (u != unicodeLength) {
			char[] result = new char[u];
			System.arraycopy(unicodeSource, 0, result, 0, u);
			return result;
		} else {
			return unicodeSource;
		}
	}
	
	/*
	 * Return all tags number for a given Javadoc
	 */
	int allTags(Javadoc docComment) {
		int all = 0;
		// Count main tags
		Iterator tags = docComment.tags().listIterator();
		while (tags.hasNext()) {
			TagElement tagElement = (TagElement) tags.next();
			if (tagElement.getTagName() != null) {
				all++;
			}
			Iterator fragments = tagElement.fragments().listIterator();
			while (fragments.hasNext()) {
				ASTNode node = (ASTNode) fragments.next();
				if (node.getNodeType() == ASTNode.TAG_ELEMENT) {
					all++;
				}
			}
		}
		return all;
	}

	/*
	 * Add a failure to the list. Use only one method as it easier to put breakpoint to
	 * debug failure when it occurs...
	 */
	private void addFailure(String msg) {
		this.failures.add(msg);
	}

	/*
	 * Put the failure message in list instead of throwing exception immediately.
	 * This allow to store several failures per test...
	 * @see tearDown method which finally throws the execption to signal that test fails.
	 */
	protected void assumeTrue(String msg, boolean cond) {
		if (!cond) {
			addFailure(msg);
			if (this.stopOnFailure) assertTrue(msg, cond);
		}
	}

	/*
	 * Put the failure message in list instead of throwing exception immediately.
	 * This allow to store several failures per test...
	 * @see tearDown method which finally throws the execption to signal that test fails.
	 */
	protected void assumeNull(String msg, Object obj) {
		if (obj != null) {
			addFailure(msg);
			if (this.stopOnFailure) assertNull(msg, obj);
		}
	}

	/*
	 * Put the failure message in list instead of throwing exception immediately.
	 * This allow to store several failures per test...
	 * @see tearDown method which finally throws the execption to signal that test fails.
	 */
	protected void assumeNotNull(String msg, Object obj) {
		if (obj == null) {
			addFailure(msg);
			if (this.stopOnFailure) assertNotNull(msg, obj);
		}
	}

	/*
	 * Put the failure message in list instead of throwing exception immediately.
	 * This allow to store several failures per test...
	 * @see tearDown method which finally throws the execption to signal that test fails.
	 */
	protected void assumeEquals(String msg, int expected, int actual) {
		if (expected != actual) {
			addFailure(msg+", expected="+expected+" actual="+actual);
			if (this.stopOnFailure) assertEquals(expected, actual);
		}
	}

	/*
	 * Put the failure message in list instead of throwing exception immediately.
	 * This allow to store several failures per test...
	 * @see tearDown method which finally throws the execption to signal that test fails.
	 */
	protected void assumeEquals(String msg, Object expected, Object actual) {
		if (expected == null && actual == null)
			return;
		if (expected != null && expected.equals(actual))
			return;
		addFailure(msg+", expected:<"+expected+"> actual:<"+actual+'>');
		if (this.stopOnFailure) assertEquals(expected, actual);
	}

	/*
	 * Verify positions of tags in source
	 */
	private void verifyPositions(Javadoc docComment, char[] source) {
		boolean stop = this.stopOnFailure;
		this.stopOnFailure = false;
		// Verify javadoc start and end position
		int start = docComment.getStartPosition();
		int end = start+docComment.getLength()-1;
		assumeTrue(this.prefix+"Misplaced javadoc start at <"+start+">: "+docComment, source[start++] == '/' && source[start++] == '*' && source[start++] == '*');
		// Get first meaningful character
		int tagStart = start;
		// Verify tags
		Iterator tags = docComment.tags().listIterator();
		while (tags.hasNext()) {
			while (source[tagStart] == '*' || Character.isWhitespace(source[tagStart])) {
				tagStart++; // purge non-stored characters
			}
			TagElement tagElement = (TagElement) tags.next();
			int teStart = tagElement.getStartPosition();
			assumeEquals(this.prefix+"Wrong start position <"+teStart+"> for tag element: "+tagElement, tagStart, teStart);
			verifyPositions(tagElement, source);
			tagStart += tagElement.getLength();
		}
		while (source[tagStart] == '*' || Character.isWhitespace(source[tagStart])) {
			tagStart++; // purge non-stored characters
		}
		assumeTrue(this.prefix+"Misplaced javadoc end at <"+tagStart+'>', source[tagStart-1] == '*' && source[tagStart] == '/');
		assumeEquals(this.prefix+"Wrong javadoc length at <"+end+">: ", tagStart, end);
		this.stopOnFailure = stop;
		assertTrue(!stop || this.failures.size()==0);
	}

	/*
	 * Verify positions of fragments in source
	 */
	private void verifyPositions(TagElement tagElement, char[] source) {
		String text = null;
		// Verify tag name
		String tagName = tagElement.getTagName();
		int tagStart = tagElement.getStartPosition();
		if (tagElement.isNested()) {
			assumeEquals(this.prefix+"Wrong start position <"+tagStart+"> for "+tagElement, '{', source[tagStart++]);
		}
		if (tagName != null) {
			text= new String(source, tagStart, tagName.length());
			assumeEquals(this.prefix+"Misplaced tag name at <"+tagStart+">: ", tagName, text);
			tagStart += tagName.length();
		}
		// Verify each fragment
		ASTNode previousFragment = null;
		Iterator elements = tagElement.fragments().listIterator();
		while (elements.hasNext()) {
			ASTNode fragment = (ASTNode) elements.next();
			if (fragment.getNodeType() == ASTNode.TEXT_ELEMENT) {
				if (previousFragment == null) {
					if (tagName != null && (source[tagStart] == '\r' || source[tagStart] == '\n')) {
						while (source[tagStart] == '*' || Character.isWhitespace(source[tagStart])) {
							tagStart++; // purge non-stored characters
						}
					}
				} else {
					if (previousFragment.getNodeType() == ASTNode.TEXT_ELEMENT) {
						assumeTrue(this.prefix+"Wrong length at <"+previousFragment.getStartPosition()+"> for text element "+previousFragment, (source[tagStart] == '\r' && source[tagStart+1] == '\n' || source[tagStart] == '\n'));
						while (source[tagStart] == '*' || Character.isWhitespace(source[tagStart])) {
							tagStart++; // purge non-stored characters
						}
					} else {
						int start = tagStart;
						boolean newLine = false;
						while (source[start] == '*' || Character.isWhitespace(source[start])) {
							start++; // purge non-stored characters
							if (source[tagStart] == '\r' || source[tagStart] == '\n') {
								newLine = true;
							}
						}
						if (newLine) tagStart = start;
					}
				}
				text = new String(source, tagStart, fragment.getLength());
				assumeEquals(this.prefix+"Misplaced text element at <"+fragment.getStartPosition()+">: ", text, ((TextElement) fragment).getText());
			} else {
				while (source[tagStart] == '*' || Character.isWhitespace(source[tagStart])) {
					tagStart++; // purge non-stored characters
				}
				if (fragment.getNodeType() == ASTNode.SIMPLE_NAME || fragment.getNodeType() == ASTNode.QUALIFIED_NAME) {
					verifyNamePositions(tagStart, (Name) fragment, source);
				} else if (fragment.getNodeType() == ASTNode.TAG_ELEMENT) {
					TagElement inlineTag = (TagElement) fragment;
					assumeEquals(this.prefix+"Tag element <"+inlineTag+"> has wrong start position", tagStart, inlineTag.getStartPosition());
					verifyPositions(inlineTag, source);
				} else if (fragment.getNodeType() == ASTNode.MEMBER_REF) {
					MemberRef memberRef = (MemberRef) fragment;
					// Store start position
					int start = tagStart;
					// Verify qualifier position
					Name qualifier = memberRef.getQualifier();
					if (qualifier != null) {
						verifyNamePositions(start, qualifier, source);
						start += qualifier.getLength();
						while (source[start] == '*' || Character.isWhitespace(source[start])) {
							start++; // purge non-stored characters
						}
					}
					// Verify member separator position
					assumeEquals(this.prefix+"Misplaced # separator at <"+start+"> for member ref "+memberRef, '#', source[start]);
					start++;
					while (source[start] == '*' || Character.isWhitespace(source[start])) {
						start++; // purge non-stored characters
					}
					// Verify member name position
					Name name = memberRef.getName();
					text = new String(source, start, name.getLength());
					assumeEquals(this.prefix+"Misplaced member ref at <"+start+">: ", text, name.toString());
					verifyNamePositions(start, name, source);
				} else if (fragment.getNodeType() == ASTNode.METHOD_REF) {
					MethodRef methodRef = (MethodRef) fragment;
					// Store start position
					int start = tagStart;
					// Verify qualifier position
					Name qualifier = methodRef.getQualifier();
					if (qualifier != null) {
						verifyNamePositions(start, qualifier, source);
						start += qualifier.getLength();
						while (source[start] == '*' || Character.isWhitespace(source[start])) {
							start++; // purge non-stored characters
						}
					}
					// Verify member separator position
					assumeEquals(this.prefix+"Misplaced # separator at <"+start+"> for method ref: "+methodRef, '#', source[start]);
					start++;
					while (source[start] == '*' || Character.isWhitespace(source[start])) {
						start++; // purge non-stored characters
					}
					// Verify member name position
					Name name = methodRef.getName();
					text = new String(source, start, name.getLength());
					assumeEquals(this.prefix+"Misplaced method ref name at <"+start+">: ", text, name.toString());
					verifyNamePositions(start, name, source);
					start += name.getLength();
					// Verify arguments starting open parenthesis
					while (source[start] == '*' || Character.isWhitespace(source[start])) {
						start++; // purge non-stored characters
					}
					assumeEquals(this.prefix+"Misplaced ( at <"+start+"> for method ref: "+methodRef, '(', source[start]);
					start++;
					// Verify parameters
					Iterator parameters = methodRef.parameters().listIterator();
					while (parameters.hasNext()) {
						MethodRefParameter param = (MethodRefParameter) parameters.next();
						// Verify parameter type positions
						while (source[start] == '*' || Character.isWhitespace(source[start])) {
							 start++; // purge non-stored characters
						}
						Type type = param.getType();
						if (type.isSimpleType()) {
							verifyNamePositions(start, ((SimpleType)type).getName(), source);
						} else if (type.isPrimitiveType()) {
							text = new String(source, start, type.getLength());
							assumeEquals(this.prefix+"Misplaced method ref parameter type at <"+start+"> for method ref: "+methodRef, text, type.toString());
						} else if (type.isArrayType()) {
							Type elementType = ((ArrayType) param.getType()).getElementType();
							if (elementType.isSimpleType()) {
								verifyNamePositions(start, ((SimpleType)elementType).getName(), source);
							} else if (elementType.isPrimitiveType()) {
								text = new String(source, start, elementType.getLength());
								assumeEquals(this.prefix+"Misplaced method ref parameter type at <"+start+"> for method ref: "+methodRef, text, elementType.toString());
							}
						}
						start += type.getLength();
						// Verify parameter name positions
						while (Character.isWhitespace(source[start])) { // do NOT accept '*' in parameter declaration
							 start++; // purge non-stored characters
						}
						name = param.getName();
						if (name != null) {
							text = new String(source, start, name.getLength());
							assumeEquals(this.prefix+"Misplaced method ref parameter name at <"+start+"> for method ref: "+methodRef, text, name.toString());
							start += name.getLength();
						}
						// Verify end parameter declaration
						while (source[start] == '*' || Character.isWhitespace(source[start])) {
							start++;
						}
						assumeTrue(this.prefix+"Misplaced parameter end at <"+start+"> for method ref: "+methodRef, source[start] == ',' || source[start] == ')');
						start++;
						if (source[start] == ')') {
							break;
						}
					}
				}
			}
			tagStart += fragment.getLength();
			previousFragment = fragment;
		}
		if (tagElement.isNested()) {
			assumeEquals(this.prefix+"Wrong end character at <"+tagStart+"> for "+tagElement, '}', source[tagStart++]);
		}
	}

	/*
	 * Verify each name component positions.
	 */
	private void verifyNamePositions(int nameStart, Name name, char[] source) {
		if (name.isQualifiedName()) {
			QualifiedName qualified = (QualifiedName) name;
			int start = qualified.getName().getStartPosition();
			String str = new String(source, start, qualified.getName().getLength());
			assumeEquals(this.prefix+"Misplaced or wrong name at <"+start+"> for qualified name: "+name, str, qualified.getName().toString());
			verifyNamePositions(nameStart, ((QualifiedName) name).getQualifier(), source);
		}
		String str = new String(source, nameStart, name.getLength());
		if (str.indexOf('\n') < 0) { // cannot compare if text contains new line
			assumeEquals(this.prefix+"Misplaced name at <"+nameStart+"> for qualified name: ", str, name.toString());
		} else {
			System.out.println(this.prefix+"Name contains new line at <"+nameStart+"> for qualified name: "+name);
		}
	}

	/*
	 * Verify that bindings of Javadoc comment structure are resolved or not.
	 * For expected unresolved binding, verify that following text starts with 'Unknown'
	 */
	private void verifyBindings(Javadoc docComment) {
		boolean stop = this.stopOnFailure;
		this.stopOnFailure = false;
		// Verify tags
		Iterator tags = docComment.tags().listIterator();
		while (tags.hasNext()) {
			verifyBindings((TagElement) tags.next());
		}
		this.stopOnFailure = stop;
		assertTrue(!stop || this.failures.size()==0);
	}

	/*
	 * Verify that bindings of Javadoc tag structure are resolved or not.
	 * For expected unresolved binding, verify that following text starts with 'Unknown'
	 */
	private void verifyBindings(TagElement tagElement) {
		// Verify each fragment
		int tagElementStart = tagElement.getStartPosition();
		Iterator elements = tagElement.fragments().listIterator();
		IBinding previousBinding = null;
		boolean resolvedBinding = false;
		while (elements.hasNext()) {
			ASTNode fragment = (ASTNode) elements.next();
			int fragmentStart = fragment.getStartPosition();
			if (fragment.getNodeType() == ASTNode.TEXT_ELEMENT) {
				TextElement text = (TextElement) fragment;
				if (resolvedBinding) {
					if (previousBinding == null) {
						assumeTrue(this.prefix+"Reference at <"+fragmentStart+"> in '"+fragment+"' should be bound!", text.getText().trim().startsWith("Unknown"));
					} else {
						assumeTrue(this.prefix+"Unknown reference at <"+fragmentStart+"> in '"+fragment+"' should NOT be bound!", !text.getText().trim().startsWith("Unknown"));
					}
				}
				previousBinding = null;
				resolvedBinding = false;
			} else if (fragment.getNodeType() == ASTNode.TAG_ELEMENT) {
				verifyBindings((TagElement) fragment);
				previousBinding = null;
				resolvedBinding = false;
			} else {
				resolvedBinding = true;
				if (fragment.getNodeType() == ASTNode.SIMPLE_NAME) {
					previousBinding = ((Name)fragment).resolveBinding();
				} else if (fragment.getNodeType() == ASTNode.QUALIFIED_NAME) {
					QualifiedName name = (QualifiedName) fragment;
					previousBinding = name.resolveBinding();
					verifyNameBindings(name);
				} else if (fragment.getNodeType() == ASTNode.MEMBER_REF) {
					MemberRef memberRef = (MemberRef) fragment;
					previousBinding = memberRef.resolveBinding();
					if (previousBinding != null) {
						assumeNotNull(this.prefix+""+memberRef.getName()+" binding at <"+fragmentStart+"> was not found!", memberRef.getName().resolveBinding());
						verifyNameBindings(memberRef.getQualifier());
					}
				} else if (fragment.getNodeType() == ASTNode.METHOD_REF) {
					MethodRef methodRef = (MethodRef) fragment;
					previousBinding = methodRef.resolveBinding();
					if (previousBinding != null) {
						assumeNotNull(this.prefix+""+methodRef.getName()+" binding at <"+fragmentStart+"> was not found!", methodRef.getName().resolveBinding());
						verifyNameBindings(methodRef.getQualifier());
						Iterator parameters = methodRef.parameters().listIterator();
						while (parameters.hasNext()) {
							MethodRefParameter param = (MethodRefParameter) parameters.next();
							assumeNotNull(this.prefix+""+param.getType()+" binding at <"+fragmentStart+"> was not found!", param.getType().resolveBinding());
							if (param.getType().isSimpleType()) {
								verifyNameBindings(((SimpleType)param.getType()).getName());
							}
							//	Do not verify parameter name as no binding is expected for them
						}
					}
				}
			}
		}
		assumeTrue(this.prefix+"Reference at <"+tagElementStart+"> in '"+tagElement+"' should be bound!", (!resolvedBinding || previousBinding != null));
	}

	/*
	 * Verify each name component binding.
	 */
	private void verifyNameBindings(Name name) {
		if (name != null) {
			int nameStart = name.getStartPosition();
			IBinding binding = name.resolveBinding();
			if (name.toString().indexOf("Unknown") > 0) {
				assumeNull(this.prefix+name+" binding at <"+nameStart+"> should be null!", binding);
			} else {
				assumeNotNull(this.prefix+name+" binding at <"+nameStart+"> was not found!", binding);
			}
			SimpleName simpleName = null;
			int index = 0;
			while (name.isQualifiedName()) {
				simpleName = ((QualifiedName) name).getName();
				binding = simpleName.resolveBinding();
				int simpleNameStart = simpleName.getStartPosition();
				if (simpleName.getIdentifier().equalsIgnoreCase("Unknown")) {
					assumeNull(this.prefix+simpleName+" binding at <"+simpleNameStart+"> should be null!", binding);
				} else {
					assumeNotNull(this.prefix+simpleName+" binding at <"+simpleNameStart+"> was not found!", binding);
				}
				if (index > 0 && this.packageBinding) {
					assumeEquals(this.prefix+"Wrong binding type at <"+simpleNameStart+">", IBinding.PACKAGE, binding.getKind());
				}
				index++;
				name = ((QualifiedName) name).getQualifier();
				binding = name.resolveBinding();
				if (name.toString().indexOf("Unknown") > 0) {
					assumeNull(this.prefix+name+" binding at <"+nameStart+"> should be null!", binding);
				} else {
					assumeNotNull(this.prefix+name+" binding at <"+nameStart+"> was not found!", binding);
				}
				if (this.packageBinding) {
					assumeEquals(this.prefix+"Wrong binding type at <"+nameStart+">", IBinding.PACKAGE, binding.getKind());
				}
			}
		}
	}

	/*
	 * Verify the comments of a compilation unit.
	 */
	protected CompilationUnit verifyComments(ICompilationUnit unit) throws JavaModelException {
		// Get test file
		this.sourceUnit = unit;
		this.prefix = unit.getElementName()+": ";

		// Create DOM AST nodes hierarchy
		String sourceStr = this.sourceUnit.getSource();
		this.currentProject = this.sourceUnit.getJavaProject();

		// set up java project options
		this.currentProject.setOption(JavaCore.COMPILER_PB_INVALID_JAVADOC, this.compilerOption);
		this.currentProject.setOption(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS, this.compilerOption);
		this.currentProject.setOption(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS, this.compilerOption);
		this.currentProject.setOption(JavaCore.COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME, JavaCore.IGNORE);
		this.currentProject.setOption(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, this.docCommentSupport);

		// Verify source regardings converted comments
		char[] source = sourceStr.toCharArray();
		String fileName = unit.getPath().toString();
		try {
			return verifyComments(fileName, source);
		}
		catch (RuntimeException ex) {
			testCounters[3]++;
			throw ex;
		}
	}
	
	protected CompilationUnit verifyComments(String fileName, char[] source) {

		// Get comments infos from test file
		setSourceComment(source);
		
		// Verify comments either in unicode or not
		char[] testedSource = source;
		if (unicode) {
			testedSource = getUnicodeSource(source);
		}
//		Map originalOptions = this.currentProject.getOptions(true);
		List unitComments = null;
//		try {
//			this.currentProject.setOption(JavaCore.COMPILER_PB_INVALID_JAVADOC, this.compilerOption);
//			this.currentProject.setOption(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS, this.compilerOption);
//			this.currentProject.setOption(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS, this.compilerOption);
//			this.currentProject.setOption(JavaCore.COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME, JavaCore.IGNORE);
//			this.currentProject.setOption(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, this.docCommentSupport);
			CompilationUnit compilUnit = (CompilationUnit) runConversion(testedSource, fileName, this.currentProject);
//			CompilationUnit compilUnit = (CompilationUnit) runConversion(this.sourceUnit, this.resolveBinding); // resolve bindings
			if (this.compilerOption.equals(JavaCore.ERROR)) {
				assumeEquals(this.prefix+"Unexpected problems", 0, compilUnit.getProblems().length); //$NON-NLS-1$
			} else if (this.compilerOption.equals(JavaCore.WARNING)) {
				IProblem[] problemsList = compilUnit.getProblems();
				int length = problemsList.length;
				if (length > 0) {
					problems.append("  - "+this.prefix+length+" problems:"); //$NON-NLS-1$
					for (int i = 0; i < problemsList.length; i++) {
						problems.append("	+ "+problemsList[i]);
					}
				}
			}
			unitComments = compilUnit.getCommentList();
//		} finally {
//			this.currentProject.setOptions(originalOptions);
//		}
		assumeNotNull(this.prefix+"Unexpected problems", unitComments);
		
		// Basic comments verification
		int size = unitComments.size();
		assumeEquals(this.prefix+"Wrong number of comments!", this.comments.size(), size);

		// Verify comments positions and bindings
		for (int i=0; i<size; i++) {
			Comment comment = (Comment) unitComments.get(i);
			List tags = (List) allTags.get(i);
			// Verify flattened content
			String stringComment = (String) this.comments.get(i);
//			ASTConverterJavadocFlattener printer = new ASTConverterJavadocFlattener(stringComment);
//			comment.accept(printer);
			String text = new String(testedSource, comment.getStartPosition(), comment.getLength());
			assumeEquals(this.prefix+"Flattened comment does NOT match source!", stringComment, text);
			// Verify javdoc tags positions and bindings
			if (comment.isDocComment()) {
				Javadoc docComment = (Javadoc)comment;
				if (this.docCommentSupport.equals(JavaCore.ENABLED)) {
					assumeEquals(this.prefix+"Invalid tags number in javadoc:\n"+docComment+"\n", tags.size(), allTags(docComment));
					verifyPositions(docComment, testedSource);
					if (this.resolveBinding) {
						verifyBindings(docComment);
					}
				} else {
					assumeEquals("Javadoc should be flat!", 0, docComment.tags().size());
				}
			}
		}
		
		/* Verify each javadoc: not implemented yet
		Iterator types = compilUnit.types().listIterator();
		while (types.hasNext()) {
			TypeDeclaration typeDeclaration = (TypeDeclaration) types.next();
			verifyJavadoc(typeDeclaration.getJavadoc());
		}
		*/

		// Return compilation unit for possible further verifications
		return compilUnit;
	}

	/* 
	 * Verify each javadoc
	 * Not implented yet
	private void verifyJavadoc(Javadoc docComment) {
	}
	*/

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void verifyComments(String test) throws JavaModelException {
		ICompilationUnit[] units = getCompilationUnits("Converter" , "src", "javadoc."+test); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (int i=0; i<units.length; i++) {
			verifyComments(units[i]);
		}
	}

	/**
	 * Check javadoc for MethodDeclaration
	 */
	public void test000() throws JavaModelException {
		verifyComments("test000");
	}

	/**
	 * Check javadoc for invalid syntax
	 */
	public void test001() throws JavaModelException {
		verifyComments("test001");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50781
	 */
	public void test002() throws JavaModelException {
		verifyComments("test002");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50784
	 */
	public void test003() throws JavaModelException {
		verifyComments("test003");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50785
	 */
	public void test004() throws JavaModelException {
		verifyComments("test004");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50838
	 */
	public void test005() throws JavaModelException {
		verifyComments("test005");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50877
	 */
	public void test006() throws JavaModelException {
		verifyComments("test006");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50877
	 */
	public void test007() throws JavaModelException {
		verifyComments("test007");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50877
	 */
	public void test008() throws JavaModelException {
		verifyComments("test008");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50877
	 */
	public void test009() throws JavaModelException {
		verifyComments("test009");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50880
	 */
	public void test010() throws JavaModelException {
		verifyComments("test010");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=47396
	 */
	public void test011() throws JavaModelException {
		this.problems = new StringBuffer();
		this.sourceUnit = getCompilationUnit("Converter" , "src", "javadoc.test011", "Test.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		ASTNode result = runConversion(this.sourceUnit, true);
		assumeNotNull("No compilation unit", result);
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50938
	 */
	public void test012() throws JavaModelException {
		verifyComments("test012");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51104
	 */
	public void test013() throws JavaModelException {
		verifyComments("test013");
	}

	/**
	 * Verify that text on next line following empty tag element
	 * is well positionned.
	 */
	public void test014() throws JavaModelException {
		verifyComments("test014");
	}

	/**
	 * Verify that we do not report failure when types are written on several lines
	 * in Javadoc comments.
	 */
	public void test015() throws JavaModelException {
		verifyComments("test015");
	}

	/**
	 * Verify DefaultCommentMapper heuristic to get leading and trailing comments
	 */
	protected void verifyMapper(String folder, int count, int[] indexes) throws JavaModelException {
		ICompilationUnit[] units = getCompilationUnits("Converter" , "src", "javadoc."+folder); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (int i=0; i<units.length; i++) {
			this.sourceUnit = units[i];
			ASTNode result = runConversion(this.sourceUnit, false);
			final CompilationUnit compilUnit = (CompilationUnit) result;
			assumeEquals(this.prefix+"Wrong number of problems", 0, compilUnit.getProblems().length); //$NON-NLS-1$
			assumeEquals(this.prefix+"Wrong number of comments", count, compilUnit.getCommentList().size());
			// Verify first method existence
			ASTNode node = getASTNode((CompilationUnit) result, 0, 0);
			assumeNotNull("We should get a non-null ast node", node);
			assumeTrue("Not a method declaration", node.getNodeType() == ASTNode.METHOD_DECLARATION); //$NON-NLS-1$
			MethodDeclaration method = (MethodDeclaration) node;
			// Verify first method extended positions
			int commentStart = method.getStartPosition();
			if (indexes[0]>=0) {
				Comment comment = (Comment) compilUnit.getCommentList().get(indexes[0]);
				commentStart = comment.getStartPosition();
			}
			int startPosition = compilUnit.getExtendedStartPosition(method);
			assumeEquals("Method "+node+" does not start at the right position", commentStart, startPosition);
			int methodEnd = startPosition + compilUnit.getExtendedLength(method) - 1;
			int commentEnd = method.getStartPosition() + method.getLength() - 1;
			if (indexes[1]>=0) {
				Comment comment = (Comment) compilUnit.getCommentList().get(indexes[1]);
				commentEnd = comment.getStartPosition() + comment.getLength() - 1;
			}
			assumeEquals("Method "+node+" does not have the correct length", commentEnd, methodEnd);
			// Verify second method existence
			node = getASTNode((CompilationUnit) result, 0, 1);
			assumeNotNull("We should get a non-null ast node", node);
			assumeTrue("Not a method declaration", node.getNodeType() == ASTNode.METHOD_DECLARATION); //$NON-NLS-1$
			method = (MethodDeclaration) node;
			// Verify second method extended positions
			commentStart = method.getStartPosition();
			if (indexes[2]>=0) {
				Comment comment = (Comment) compilUnit.getCommentList().get(indexes[2]);
				commentStart = comment.getStartPosition();
			}
			startPosition = compilUnit.getExtendedStartPosition(method);
			assumeEquals("Method "+node+" does not start at the right position", commentStart, startPosition);
			methodEnd = startPosition + compilUnit.getExtendedLength(method) - 1;
			commentEnd = method.getStartPosition() + method.getLength() - 1;
			if (indexes[3]>=0) {
				Comment comment = (Comment) compilUnit.getCommentList().get(indexes[3]);
				commentEnd = comment.getStartPosition() + comment.getLength() - 1;
			}
			assumeEquals("Method "+node+" does not have the correct length", commentEnd, methodEnd);
		}
	}

	/**
	 * Verify DefaultCommentMapper heuristic to get leading and trailing comments
	 */
	public void test100() throws JavaModelException {
		verifyMapper("test100", 16, new int[] {2,7,8,15});
	}
	public void test101() throws JavaModelException {
		verifyMapper("test101", 8, new int[] {1,3,4,7});
	}
	public void test102() throws JavaModelException {
		verifyMapper("test102", 16, new int[] {4,9,10,13});
	}
	public void test103() throws JavaModelException {
		verifyMapper("test103", 8, new int[] {2,4,5,6});
	}
	public void test104() throws JavaModelException {
		verifyMapper("test104", 16, new int[] {2,7,8,15});
	}
	public void test105() throws JavaModelException {
		verifyMapper("test105", 16, new int[] {-1,11,-1,15});
	}
	public void test106() throws JavaModelException {
		verifyMapper("test106", 8, new int[] {-1,5,-1,7});
	}
	public void test107() throws JavaModelException {
		verifyMapper("test107", 16, new int[] {2,7,8,-1});
	}
	public void test108() throws JavaModelException {
		verifyMapper("test108", 8, new int[] {1,3,4,-1});
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=54776
	 */
	public void testBug54776() throws JavaModelException {
		this.sourceUnit = getCompilationUnit("Converter" , "src", "javadoc.testBug54776", "Test.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		ASTNode result = runConversion(this.sourceUnit, false);
		final CompilationUnit compilUnit = (CompilationUnit) result;
		assumeEquals(this.prefix+"Wrong number of problems", 0, compilUnit.getProblems().length); //$NON-NLS-1$
		assumeEquals(this.prefix+"Wrong number of comments", 2, compilUnit.getCommentList().size());
		Comment comment = (Comment) compilUnit.getCommentList().get(0);
		int commentStart = comment.getStartPosition();
		int commentLength = ((Comment) compilUnit.getCommentList().get(1)).getStartPosition()-commentStart+comment.getLength();
		ASTNode node = getASTNode((CompilationUnit) result, 0);
		assumeNotNull("We should get a non-null ast node", node);
		assumeTrue("Not a type declaration", node.getNodeType() == ASTNode.TYPE_DECLARATION); //$NON-NLS-1$
		TypeDeclaration typeDecl = (TypeDeclaration) node;
		FieldDeclaration[] fields = typeDecl.getFields();
		assumeEquals("We should have a field declaration", 1, fields.length);
		List fragments = fields[0].fragments();
		assumeEquals("We should have a variable fragment", 1, fragments.size());
		VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
		Expression expression = fragment.getInitializer();
		assumeTrue("We should get an expression", expression instanceof MethodInvocation);
		MethodInvocation methodInvocation = (MethodInvocation) expression;
		int methodStart = compilUnit.getExtendedStartPosition(methodInvocation);
		assumeEquals("Method invocation "+methodInvocation+" does not start at the right position", commentStart, methodStart);
		int methodLength = compilUnit.getExtendedLength(methodInvocation);
		assumeEquals("Method invocation "+methodInvocation+" does not have the correct length", commentLength, methodLength);
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=54xxx
	 */
	public void _testBug54xxx() throws JavaModelException {
		this.sourceUnit = getCompilationUnit("Converter" , "src", "javadoc.testBug54xxx", "Test.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		ASTNode result = runConversion(this.sourceUnit, false);
		final CompilationUnit compilUnit = (CompilationUnit) result;
		assumeEquals(this.prefix+"Wrong number of problems", 0, compilUnit.getProblems().length); //$NON-NLS-1$
		assumeEquals(this.prefix+"Wrong number of comments", 1, compilUnit.getCommentList().size());
		ASTNode node = getASTNode(compilUnit, 0, 0);
		assumeNotNull("We should get a non-null ast node", node);
		assumeTrue("Not a method declaration", node.getNodeType() == ASTNode.METHOD_DECLARATION); //$NON-NLS-1$
		MethodDeclaration method = (MethodDeclaration) node;
		node = method.getBody();
		assumeNotNull("We should get a non-null ast node", node);
		assumeTrue("Not a block", node.getNodeType() == ASTNode.BLOCK); //$NON-NLS-1$
		Block block = (Block) node;
		int blockStart = compilUnit.getExtendedStartPosition(block);
		assumeEquals("Body block "+block+" does not start at the right position", block.getStartPosition(), blockStart);
		int blockLength = compilUnit.getExtendedLength(block);
		assumeEquals("Body block "+block+" does not have the correct length", block.getLength(), blockLength);
	}
	/*
	 * End DefaultCommentMapper verifications
	 */

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=48489
	 */
	public void testBug48489() throws JavaModelException {
		verifyComments("testBug48489");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=50898
	 */
	public void testBug50898() throws JavaModelException {
		ICompilationUnit unit = getCompilationUnit("Converter" , "src", "javadoc.testBug50898", "Test.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		this.packageBinding = false;
		verifyComments(unit);
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51226
	 */
	public void testBug51226() throws JavaModelException {
		ICompilationUnit[] units = getCompilationUnits("Converter" , "src", "javadoc.testBug51226"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (int i=0; i<units.length; i++) {
			ASTNode result = runConversion(units[i], false);
			final CompilationUnit unit = (CompilationUnit) result;
			assumeEquals(this.prefix+"Wrong number of problems", 0, unit.getProblems().length); //$NON-NLS-1$
			assumeEquals(this.prefix+"Wrong number of comments", 1, unit.getCommentList().size());
			Comment comment = (Comment) unit.getCommentList().get(0);
			assumeTrue(this.prefix+"Comment should be a Javadoc one", comment.isDocComment());
			Javadoc docComment = (Javadoc) comment;
			assumeEquals(this.prefix+"Wrong number of tags", 1, docComment.tags().size());
			TagElement tagElement = (TagElement) docComment.tags().get(0);
			assumeNull(this.prefix+"Wrong type of tag ["+tagElement+"]", tagElement.getTagName());
			assumeEquals(this.prefix+"Wrong number of fragments in tag ["+tagElement+"]", 1, tagElement.fragments().size());
			ASTNode fragment = (ASTNode) tagElement.fragments().get(0);
			assumeEquals(this.prefix+"Invalid type for fragment ["+fragment+"]", ASTNode.TEXT_ELEMENT, fragment.getNodeType());
			TextElement textElement = (TextElement) fragment;
			assumeEquals(this.prefix+"Invalid content for text element ", "Test", textElement.getText());
			if (debug) System.out.println(docComment+"\nsuccessfully verified.");
		}
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51241
	 */
	public void testBug51241() throws JavaModelException {
		verifyComments("testBug51241");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51363
	 */
	public void testBug51363() throws JavaModelException {
		this.sourceUnit = getCompilationUnit("Converter" , "src", "javadoc.testBug51363", "Test.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		ASTNode result = runConversion(this.sourceUnit, false);
		final CompilationUnit unit = (CompilationUnit) result;
		assumeEquals(this.prefix+"Wrong number of problems", 0, unit.getProblems().length); //$NON-NLS-1$
		assumeEquals(this.prefix+"Wrong number of comments", 2, unit.getCommentList().size());
		// verify first comment
		Comment comment = (Comment) unit.getCommentList().get(0);
		assumeTrue(this.prefix+"Comment should be a line comment ", comment.isLineComment());
		String sourceStr = this.sourceUnit.getSource();
		int startPos = comment.getStartPosition()+comment.getLength();
		assumeEquals("Wrong length for line comment "+comment, "\\u000D\\u000A", sourceStr.substring(startPos, startPos+12));
		if (debug) System.out.println(comment+"\nsuccessfully verified.");
		// verify second comment
		comment = (Comment) unit.getCommentList().get(1);
		assumeTrue(this.prefix+"Comment should be a line comment", comment.isLineComment());
		sourceStr = this.sourceUnit.getSource();
		startPos = comment.getStartPosition()+comment.getLength();
		assumeEquals("Wrong length for line comment "+comment, "\\u000Dvoid", sourceStr.substring(startPos, startPos+10));
		if (debug) System.out.println(comment+"\nsuccessfully verified.");
//		verifyComments("testBug51363");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51476
	 */
	public void testBug51476() throws JavaModelException {
		verifyComments("testBug51476");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51478
	 */
	public void testBug51478() throws JavaModelException {
		verifyComments("testBug51478");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51508
	 */
	public void testBug51508() throws JavaModelException {
		verifyComments("testBug51508");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51650
	 */
	public void testBug51650() throws JavaModelException {
		verifyComments("testBug51650");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=51770
	 */
	public void testBug51770() throws JavaModelException {
		verifyComments("testBug51770");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=52908
	 */
	public void testBug52908() throws JavaModelException {
		verifyComments("testBug52908");
	}
	public void testBug52908a() throws JavaModelException {
		verifyComments("testBug52908a");
	}
	public void testBug52908unicode() throws JavaModelException {
		verifyComments("testBug52908unicode");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=53276
	 */
	public void testBug53276() throws JavaModelException {
		verifyComments("testBug53276");
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=53075
	 */
	public void testBug53075() throws JavaModelException {
		ICompilationUnit unit = getCompilationUnit("Converter" , "src", "javadoc.testBug53075", "X.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		this.packageBinding = false;
		CompilationUnit compilUnit = verifyComments(unit);
		if (this.docCommentSupport.equals(JavaCore.ENABLED)) {
			Comment comment = (Comment) compilUnit.getCommentList().get(0);
			assumeTrue(this.prefix+"Comment should be a javadoc comment ", comment.isDocComment());
			Javadoc docComment = (Javadoc) comment;
			TagElement tagElement = (TagElement) docComment.tags().get(0);
			assumeEquals("Wrong tag type!", TagElement.TAG_LINK, tagElement.getTagName());
			tagElement = (TagElement) docComment.tags().get(1);
			assumeEquals("Wrong tag type!", TagElement.TAG_LINKPLAIN, tagElement.getTagName());
		}
	}

	/**
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=53757
	 */
	public void testBug53757() throws JavaModelException {
		verifyComments("testBug53757");
	}
}
