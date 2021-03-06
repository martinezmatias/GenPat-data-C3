package org.eclipse.jdt.internal.compiler.ast;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class Assignment extends Expression {

	public Reference lhs;
	public Expression expression;

	public Assignment(Expression lhs, Expression expression, int sourceEnd) {
		//lhs is always a reference by construction ,
		//but is build as an expression ==> the checkcast cannot fail

		this.lhs = (Reference) lhs;
		this.expression = expression;

		this.sourceStart = lhs.sourceStart;
		this.sourceEnd = sourceEnd;
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {
		// record setting a variable: various scenarii are possible, setting an array reference, 
		// a field reference, a blank final field reference, a field of an enclosing instance or 
		// just a local variable.

		return lhs
			.analyseAssignment(currentScope, flowContext, flowInfo, this, false)
			.unconditionalInits();
	}

	public void generateCode(
		BlockScope currentScope,
		CodeStream codeStream,
		boolean valueRequired) {

		// various scenarii are possible, setting an array reference, 
		// a field reference, a blank final field reference, a field of an enclosing instance or 
		// just a local variable.

		int pc = codeStream.position;
		lhs.generateAssignment(currentScope, codeStream, this, valueRequired);
		// variable may have been optimized out
		// the lhs is responsible to perform the implicitConversion generation for the assignment since optimized for unused local assignment.
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	public TypeBinding resolveType(BlockScope scope) {

		// due to syntax lhs may be only a NameReference, a FieldReference or an ArrayReference
		constant = NotAConstant;
		TypeBinding lhsTb = lhs.resolveType(scope);
		TypeBinding expressionTb = expression.resolveType(scope);
		if (lhsTb == null || expressionTb == null)
			return null;

		// Compile-time conversion of base-types : implicit narrowing integer into byte/short/character
		// may require to widen the rhs expression at runtime
		if ((expression.isConstantValueOfTypeAssignableToType(expressionTb, lhsTb)
			|| (lhsTb.isBaseType() && BaseTypeBinding.isWidening(lhsTb.id, expressionTb.id)))
			|| (scope.areTypesCompatible(expressionTb, lhsTb))) {
			expression.implicitWidening(lhsTb, expressionTb);
			return lhsTb;
		}
		scope.problemReporter().typeMismatchErrorActualTypeExpectedType(
			expression,
			expressionTb,
			lhsTb);
		return null;
	}

	public String toString(int tab) {

		//no () when used as a statement 
		return tabString(tab) + toStringExpressionNoParenthesis();
	}

	public String toStringExpression() {

		//subclass redefine toStringExpressionNoParenthesis()
		return "(" + toStringExpressionNoParenthesis() + ")"; //$NON-NLS-2$ //$NON-NLS-1$
	} 
	
	public String toStringExpressionNoParenthesis() {

		return lhs.toStringExpression() + " " //$NON-NLS-1$
			+ "=" //$NON-NLS-1$
			+ ((expression.constant != null) && (expression.constant != NotAConstant)
				? " /*cst:" + expression.constant.toString() + "*/ " //$NON-NLS-1$ //$NON-NLS-2$
				: " ")  //$NON-NLS-1$
			+ expression.toStringExpression();
	}
	public void traverse(IAbstractSyntaxTreeVisitor visitor, BlockScope scope) {
		
		if (visitor.visit(this, scope)) {
			lhs.traverse(visitor, scope);
			expression.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
}