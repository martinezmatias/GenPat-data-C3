/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core;

import org.eclipse.text.edits.TextEdit;

/**
 * Specification for a generic source code formatter. This is still subject to change.
 * 
 * @since 3.0
 */
public abstract class CodeFormatter {

	/**
	 * Unknown kind
	 */
	public static final int K_UNKNOWN = 0x00;

	/**
	 * Kind used to format an expression
	 */
	public static final int K_EXPRESSION = 0x01;
	
	/**
	 * Kind used to format a set of statements
	 */
	public static final int K_STATEMENTS = 0x02;
	
	/**
	 * Kind used to format a set of class body declarations
	 */
	public static final int K_CLASS_BODY_DECLARATIONS = 0x04;
	
	/**
	 * Kind used to format a compilation unit
	 */
	public static final int K_COMPILATION_UNIT = 0x08;

	/** 
	 * Formats the String <code>sourceString</code>,
	 * and returns a text edit that correspond to the difference between the given string and the formatted string.
	 * It returns null if the given string cannot be formatted.
	 * 
	 * @param kind Use to specify the kind of the code snippet to format. It can be any of these:
	 * 		  K_EXPRESSION, K_STATEMENTS, K_CLASS_BODY_DECLARATIONS, K_COMPILATION_UNIT, K_UNKNOWN
	 * @param string the string to format
	 * @param region The region to format. If null, the whole given string is formatted.
	 * @param indentationLevel the initial indentation level, used 
	 *      to shift left/right the entire source fragment. An initial indentation
	 *      level of zero or below has no effect.
	 * @param lineSeparator the line separator to use in formatted source,
	 *     if set to <code>null</code>, then the platform default one will be used.
	 * @return the text edit
	 */
	public abstract TextEdit format(int kind, String string, int offset, int length, int indentationLevel, String lineSeparator);
}
