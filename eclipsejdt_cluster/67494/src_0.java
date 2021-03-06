/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

import java.util.List;

/**
 * Member value pair node (added in 3.0 API). Member value pairs appear in annotations.
 * <p>
 * <pre>
 * MemberValuePair:
 *   SimpleName <b>=</b> Expression
 * </pre>
 * </p>
 * <p>
 * Note: Support for annotation metadata is an experimental language feature 
 * under discussion in JSR-175 and under consideration for inclusion
 * in the 1.5 release of J2SE. The support here is therefore tentative
 * and subject to change.
 * </p>
 * @see NormalAnnotation
 * @since 3.0
 */
public class MemberValuePair extends ASTNode {
	
	/**
	 * The "name" structural property of this node type.
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY = 
		new ChildPropertyDescriptor(MemberValuePair.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "value" structural property of this node type.
	 * @since 3.0
	 */
	public static final ChildPropertyDescriptor VALUE_PROPERTY = 
		new ChildPropertyDescriptor(MemberValuePair.class, "value", Expression.class, MANDATORY, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type: 
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;
	
	static {
		createPropertyList(MemberValuePair.class);
		addProperty(NAME_PROPERTY);
		addProperty(VALUE_PROPERTY);
		PROPERTY_DESCRIPTORS = reapPropertyList();
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 * 
	 * @param apiLevel the API level; one of the AST.LEVEL_* constants
	 * @return a list of property descriptors (element type: 
	 * {@link StructuralPropertyDescriptor})
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}
						
	/**
	 * The member name; lazily initialized; defaults to a unspecified,
	 * legal name.
	 */
	private SimpleName name = null;

	/**
	 * The value; lazily initialized; defaults to a unspecified,
	 * legal expression.
	 */
	private Expression value = null;

	/**
	 * Creates a new AST node for a member value pair owned by the given 
	 * AST. By default, the node has an unspecified (but legal) member
	 * name and value.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 * 
	 * @param ast the AST that is to own this node
	 */
	MemberValuePair(AST ast) {
		super(ast);
	    unsupportedIn2();
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}
	
	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == NAME_PROPERTY) {
			if (get) {
				return getName();
			} else {
				setName((SimpleName) child);
				return null;
			}
		}
		if (property == VALUE_PROPERTY) {
			if (get) {
				return getValue();
			} else {
				setValue((Expression) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}
	
	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	public int getNodeType() {
		return MEMBER_VALUE_PAIR;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	ASTNode clone(AST target) {
		MemberValuePair result = new MemberValuePair(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setName((SimpleName) ASTNode.copySubtree(target, getName()));
		result.setValue((Expression) ASTNode.copySubtree(target, getValue()));
		return result;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	public boolean subtreeMatch(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}
	
	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			// visit children in normal left to right reading order
			acceptChild(visitor, getName());
			acceptChild(visitor, getValue());
		}
		visitor.endVisit(this);
	}
	
	/**
	 * Returns the member name.
	 * 
	 * @return the member name node
	 */ 
	public SimpleName getName() {
		if (this.name == null) {
			preLazyInit();
			this.name = new SimpleName(this.ast);
			postLazyInit(this.name, NAME_PROPERTY);
		}
		return this.name;
	}
	
	/**
	 * Sets the member name.
	 * 
	 * @param name the member name node
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */ 
	public void setName(SimpleName name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		preReplaceChild(this.name, name, NAME_PROPERTY);
		this.name = name;
		postReplaceChild(this.name, name, NAME_PROPERTY);
	}

	/**
	 * Returns the value expression.
	 * 
	 * @return the value expression
	 */ 
	public Expression getValue() {
		if (this.value == null) {
			preLazyInit();
			this.value= new SimpleName(this.ast);
			postLazyInit(this.value, VALUE_PROPERTY);
		}
		return this.value;
	}

	/**
	 * Sets the value of this pair.
	 * 
	 * @param value the new value
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * <li>a cycle in would be created</li>
	 * </ul>
	 */ 
	public void setValue(Expression value) {
		if (value == null) {
			throw new IllegalArgumentException();
		}
		preReplaceChild(this.value, value, VALUE_PROPERTY);
		this.value = value;
		postReplaceChild(this.value, value, VALUE_PROPERTY);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int memSize() {
		return BASE_NODE_SIZE + 2 * 4;
	}
	
	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int treeSize() {
		return 
			memSize()
			+ (this.name == null ? 0 : getName().treeSize())
			+ (this.value == null ? 0 : getValue().treeSize());
	}
}
