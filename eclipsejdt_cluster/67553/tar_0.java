/*******************************************************************************
 * Copyright (c) 2006 BEA Systems, Inc. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    wharley@bea.com - initial API and implementation
 *    
 *******************************************************************************/

package org.eclipse.jdt.internal.compiler.apt.model;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

/**
 * Implementation of DeclaredType, which refers to a particular usage or instance of a type.
 * Contrast with {@link TypeElement}, which is an element that potentially defines a family
 * of DeclaredTypes.
 */
public class DeclaredTypeImpl extends TypeMirrorImpl implements DeclaredType {
	
	/* package */ DeclaredTypeImpl(ReferenceBinding binding) {
		super(binding);
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.DeclaredType#asElement()
	 */
	@Override
	public Element asElement() {
		// The JDT compiler does not distinguish between type elements and declared types
		return Factory.newElement((ReferenceBinding)_binding);
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.DeclaredType#getEnclosingType()
	 */
	@Override
	public TypeMirror getEnclosingType() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.DeclaredType#getTypeArguments()
	 */
	@Override
	public List<? extends TypeMirror> getTypeArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.TypeMirror#accept(javax.lang.model.type.TypeVisitor, java.lang.Object)
	 */
	@Override
	public <R, P> R accept(TypeVisitor<R, P> v, P p) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.type.TypeMirror#getKind()
	 */
	@Override
	public TypeKind getKind() {
		return TypeKind.DECLARED;
	}

	@Override
	public String toString() {
		return _binding.toString();
	}

}
