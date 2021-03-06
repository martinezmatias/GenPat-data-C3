/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.formatter;

import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.core.CodeFormatter;
import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

public class DefaultCodeFormatter extends CodeFormatter implements ICodeFormatter {

	public static final boolean DEBUG = false;

	private static AstNode[] parseClassBodyDeclarations(char[] source, Map settings) {
		
		if (source == null) {
			throw new IllegalArgumentException();
		}
		CompilerOptions compilerOptions = new CompilerOptions(settings);
		final ProblemReporter problemReporter = new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
					compilerOptions, 
					new DefaultProblemFactory(Locale.getDefault()));
					
		CodeFormatterParser parser =
			new CodeFormatterParser(problemReporter, false);

		ICompilationUnit sourceUnit = 
			new CompilationUnit(
				source, 
				"", //$NON-NLS-1$
				compilerOptions.defaultEncoding);

		return parser.parseClassBodyDeclarations(source, new CompilationUnitDeclaration(problemReporter, new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit), source.length));
	}

	private static CompilationUnitDeclaration parseCompilationUnit(char[] source, Map settings) {
		
		if (source == null) {
			throw new IllegalArgumentException();
		}
		CompilerOptions compilerOptions = new CompilerOptions(settings);
		CodeFormatterParser parser =
			new CodeFormatterParser(
				new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
					compilerOptions, 
					new DefaultProblemFactory(Locale.getDefault())),
			false);
		ICompilationUnit sourceUnit = 
			new CompilationUnit(
				source, 
				"", //$NON-NLS-1$
				compilerOptions.defaultEncoding);
		CompilationUnitDeclaration compilationUnitDeclaration = parser.dietParse(sourceUnit, new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit));
		
		if (compilationUnitDeclaration.ignoreMethodBodies) {
			compilationUnitDeclaration.ignoreFurtherInvestigation = true;
			// if initial diet parse did not work, no need to dig into method bodies.
			return compilationUnitDeclaration; 
		}
		
		//fill the methods bodies in order for the code to be generated
		//real parse of the method....
		parser.scanner.setSource(source);
		org.eclipse.jdt.internal.compiler.ast.TypeDeclaration[] types = compilationUnitDeclaration.types;
		if (types != null) {
			for (int i = types.length; --i >= 0;) {
				types[i].parseMethod(parser, compilationUnitDeclaration);
			}
		}
		return compilationUnitDeclaration;
	}

	private static Expression parseExpression(char[] source, Map settings) {
		
		if (source == null) {
			throw new IllegalArgumentException();
		}
		CompilerOptions compilerOptions = new CompilerOptions(settings);
		final ProblemReporter problemReporter = new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
					compilerOptions, 
					new DefaultProblemFactory(Locale.getDefault()));
					
		CodeFormatterParser parser =
			new CodeFormatterParser(problemReporter, false);

		ICompilationUnit sourceUnit = 
			new CompilationUnit(
				source, 
				"", //$NON-NLS-1$
				compilerOptions.defaultEncoding);

		return parser.parseExpression(source, new CompilationUnitDeclaration(problemReporter, new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit), source.length));
	}

	private static ConstructorDeclaration parseStatements(char[] source, Map settings) {
		
		if (source == null) {
			throw new IllegalArgumentException();
		}
		CompilerOptions compilerOptions = new CompilerOptions(settings);
		final ProblemReporter problemReporter = new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
					compilerOptions, 
					new DefaultProblemFactory(Locale.getDefault()));
		CodeFormatterParser parser = new CodeFormatterParser(problemReporter, false);
		ICompilationUnit sourceUnit = 
			new CompilationUnit(
				source, 
				"", //$NON-NLS-1$
				compilerOptions.defaultEncoding);

		final CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit);
		CompilationUnitDeclaration compilationUnitDeclaration = new CompilationUnitDeclaration(problemReporter, compilationResult, source.length);		

		ConstructorDeclaration constructorDeclaration = new ConstructorDeclaration(compilationResult);
		constructorDeclaration.sourceEnd  = -1;
		constructorDeclaration.declarationSourceEnd = source.length - 1;
		constructorDeclaration.bodyStart = 0;
		constructorDeclaration.bodyEnd = source.length - 1;
		
		parser.scanner.setSource(source);
		parser.parse(constructorDeclaration, compilationUnitDeclaration);
		
		return constructorDeclaration;
	}
	
	private CodeFormatterVisitor newCodeFormatter;
	
	private int[] positionsMapping;
	private FormattingPreferences preferences;
	
	public DefaultCodeFormatter() {
		this.preferences = FormattingPreferences.getDefault();
	}
	
	public DefaultCodeFormatter(FormattingPreferences preferences) {
		this.preferences = preferences;
	}

	/**
	 * @see CodeFormatter#format(int, String, int, int[], String, Map)
	 */
	public String format(
			int kind,
			String source,
			int indentationLevel,
			int[] positions,
			String lineSeparator,
			Map options) {

		String result = source;				
		switch(kind) {
			case K_CLASS_BODY_DECLARATIONS :
				result = formatClassBodyDeclarations(source, indentationLevel, positions, lineSeparator, options);
				break;
			case K_COMPILATION_UNIT :
				result = formatCompilationUnit(source, indentationLevel, positions, lineSeparator, options);
				break;
			case K_EXPRESSION :
				result = formatExpression(source, indentationLevel, positions, lineSeparator, options);
				break;
			case K_STATEMENTS :
				result = formatStatements(source, indentationLevel, positions, lineSeparator, options);
				break;
			case K_UNKNOWN :
				result = probeFormatting(source, indentationLevel, positions, lineSeparator, options);
		}
		this.positionsMapping = positions;
		return result;
	}
	
	public String format(
		String string,
		int start,
		int end,
		int indentationLevel,
		int[] positions,
		String lineSeparator,
		Map options) {
		
		int[] newPositions = null;	
		final int length = positions == null ? 0 : positions.length;
		if (positions == null) {
			newPositions = new int[] { start, end };
		} else {
			newPositions = new int[length + 2];
			System.arraycopy(positions, 0, newPositions, 1, length);
			newPositions[0] = start;
			newPositions[length] = end;
		}
		String formattedString = formatCompilationUnit(string, indentationLevel, newPositions, lineSeparator, options);
		if (positions != null) {
			this.positionsMapping = positions;
			System.arraycopy(newPositions, 1, this.positionsMapping, 0, length);
		}
		
		return formattedString.substring(newPositions[0], newPositions[newPositions.length - 1] + 1);
	}
	
	/**
	 * @see org.eclipse.jdt.core.ICodeFormatter#format(String, int, int[], String)
	 */
	public String format(
		String source,
		int indentationLevel,
		int[] positions,
		String lineSeparator) {
		
		return format(K_UNKNOWN, source, indentationLevel, positions, lineSeparator, JavaCore.getOptions());
	}

	private String formatClassBodyDeclarations(String source, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		AstNode[] bodyDeclarations = parseClassBodyDeclarations(source.toCharArray(), options);
		
		if (bodyDeclarations == null) {
			// a problem occured while parsing the source
			this.positionsMapping = positions;
			return null;
		}
		return internalFormatClassBodyDeclarations(source, indentationLevel, positions, lineSeparator, options, bodyDeclarations);
	}

	private String formatCompilationUnit(String source, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		CompilationUnitDeclaration compilationUnitDeclaration = parseCompilationUnit(source.toCharArray(), options);
		
		if (lineSeparator != null) {
			this.preferences.line_delimiter = lineSeparator;
		}
		this.preferences.initial_indentation_level = indentationLevel;

		this.newCodeFormatter = new CodeFormatterVisitor(this.preferences, options);
		
		String result = this.newCodeFormatter.format(source, positions, compilationUnitDeclaration);
		if (positions != null) {
			System.arraycopy(this.newCodeFormatter.scribe.mappedPositions, 0, positions, 0, positions.length);
		}
		this.positionsMapping = positions;
		return result;
	}

	private String formatExpression(String source, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		Expression expression = parseExpression(source.toCharArray(), options);
		
		if (expression == null) {
			// a problem occured while parsing the source
			this.positionsMapping = positions;
			return null;
		}
		return internalFormatExpression(source, indentationLevel, positions, lineSeparator, options, expression);
	}

	private String formatStatements(String source, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		ConstructorDeclaration constructorDeclaration = parseStatements(source.toCharArray(), options);
		
		if (constructorDeclaration.statements == null) {
			// a problem occured while parsing the source
			this.positionsMapping = positions;
			return null;
		}
		return internalFormatStatements(source, indentationLevel, positions, lineSeparator, options, constructorDeclaration);
	}

	private String probeFormatting(String source, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		Expression expression = parseExpression(source.toCharArray(), options);
		
		if (expression != null) {
			return internalFormatExpression(source, indentationLevel, positions, lineSeparator, options, expression);
		}

		ConstructorDeclaration constructorDeclaration = parseStatements(source.toCharArray(), options);
		
		if (constructorDeclaration.statements != null) {
			return internalFormatStatements(source, indentationLevel, positions, lineSeparator, options, constructorDeclaration);
		}
		
		AstNode[] bodyDeclarations = parseClassBodyDeclarations(source.toCharArray(), options);
		
		if (bodyDeclarations != null) {
			return internalFormatClassBodyDeclarations(source, indentationLevel, positions, lineSeparator, options, bodyDeclarations);
		}

		return formatCompilationUnit(source, indentationLevel, positions, lineSeparator, options);
	}

	public String getDebugOutput() {
		return this.newCodeFormatter.scribe.toString();
	}
	
	public int[] getMappedPositions() {
		return this.positionsMapping;
	}

	private String internalFormatClassBodyDeclarations(String source, int indentationLevel, int[] positions, String lineSeparator, Map options, AstNode[] bodyDeclarations) {
		if (lineSeparator != null) {
			this.preferences.line_delimiter = lineSeparator;
		}
		this.preferences.initial_indentation_level = indentationLevel;

		this.newCodeFormatter = new CodeFormatterVisitor(this.preferences, options);
		
		String result = this.newCodeFormatter.format(source, positions, bodyDeclarations);
		if (positions != null) {
			System.arraycopy(this.newCodeFormatter.scribe.mappedPositions, 0, positions, 0, positions.length);
		}
		this.positionsMapping = positions;
		return result;
	}

	private String internalFormatExpression(String source, int indentationLevel, int[] positions, String lineSeparator, Map options, Expression expression) {
		if (lineSeparator != null) {
			this.preferences.line_delimiter = lineSeparator;
		}
		this.preferences.initial_indentation_level = indentationLevel;

		this.newCodeFormatter = new CodeFormatterVisitor(this.preferences, options);
		
		String result = this.newCodeFormatter.format(source, positions, expression);
		if (positions != null) {
			System.arraycopy(this.newCodeFormatter.scribe.mappedPositions, 0, positions, 0, positions.length);
		}
		this.positionsMapping = positions;
		return result;
	}
	
	private String internalFormatStatements(String source, int indentationLevel, int[] positions, String lineSeparator, Map options, ConstructorDeclaration constructorDeclaration) {
		if (lineSeparator != null) {
			this.preferences.line_delimiter = lineSeparator;
		}
		this.preferences.initial_indentation_level = indentationLevel;

		this.newCodeFormatter = new CodeFormatterVisitor(this.preferences, options);
		
		String result = this.newCodeFormatter.format(source, positions, constructorDeclaration);
		if (positions != null) {
			System.arraycopy(this.newCodeFormatter.scribe.mappedPositions, 0, positions, 0, positions.length);
		}
		this.positionsMapping = positions;
		return result;
	}
}
