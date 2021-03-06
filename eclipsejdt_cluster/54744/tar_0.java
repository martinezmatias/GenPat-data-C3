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

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.apt.dispatch.BaseProcessingEnvImpl;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodVerifier;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;

/**
 * Utilities for working with language elements.
 * There is one of these for every ProcessingEnvironment.
 */
public class ElementsImpl implements Elements {
	
	private final BaseProcessingEnvImpl _env;
	
	/*
	 * The processing env creates and caches an ElementsImpl.  Other clients should
	 * not create their own; they should ask the env for it.
	 */
	public ElementsImpl(BaseProcessingEnvImpl env) {
		_env = env;
	}

	/**
	 * Return all the annotation mirrors on this element, including inherited annotations.
	 * Annotations are inherited only if the annotation type is meta-annotated with @Inherited,
	 * and the annotation is on a class: e.g., annotations are not inherited for interfaces, methods,
	 * or fields.
	 */
	@Override
	public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
		// if e is a class, walk up its superclass hierarchy looking for @Inherited annotations not already in the list
		if (e.getKind() == ElementKind.CLASS && e instanceof TypeElementImpl) {
			List<AnnotationBinding> annotations = new ArrayList<AnnotationBinding>();
			// A class can only have one annotation of a particular annotation type.
			Set<ReferenceBinding> annotationTypes = new HashSet<ReferenceBinding>();
			ReferenceBinding binding = (ReferenceBinding)((TypeElementImpl)e)._binding;
			while (null != binding) {
				for (AnnotationBinding annotation : binding.getAnnotations()) {
					ReferenceBinding annotationType = annotation.getAnnotationType(); 
					if (!annotationTypes.contains(annotationType)) {
						annotationTypes.add(annotationType);
						annotations.add(annotation);
					}
				}
				binding = binding.superclass();
			}
			List<AnnotationMirror> list = new ArrayList<AnnotationMirror>(annotations.size());
			for (AnnotationBinding annotation : annotations) {
				list.add(Factory.newAnnotationMirror(annotation));
			}
			return Collections.unmodifiableList(list);
		}
		else {
			return e.getAnnotationMirrors();
		}
	}

	/**
	 * Compute a list of all the visible entities in this type.  Specifically:
	 * <ul>
	 * <li>All nested types declared in this type, including interfaces and enums</li>
	 * <li>All protected or public nested types declared in this type's superclasses 
	 * and superinterfaces, that are not hidden by a name collision</li>
	 * <li>All methods declared in this type, including constructors but not
	 * including static or instance initializers, and including abstract
	 * methods and unimplemented methods declared in interfaces</li>
	 * <li>All protected or public methods declared in this type's superclasses,
	 * that are not overridden by another method, but not including constructors
	 * or initializers.  Includes abstract methods and methods declared in 
	 * superinterfaces but not implemented</li>
	 * <li>All fields declared in this type, including constants</li>
	 * <li>All non-private fields declared in this type's superclasses and
	 * superinterfaces, that are not hidden by a name collision.</li>
	 * </ul>
	 */
	@Override
	public List<? extends Element> getAllMembers(TypeElement type) {
		if (null == type || !(type instanceof TypeElementImpl)) {
			return Collections.emptyList();
		}
		ReferenceBinding binding = (ReferenceBinding)((TypeElementImpl)type)._binding;
		// Map of element simple name to binding
		Map<String, ReferenceBinding> types = new HashMap<String, ReferenceBinding>();
		// Javac implementation does not take field name collisions into account
		List<FieldBinding> fields = new ArrayList<FieldBinding>();
		// For methods, need to compare parameters, not just names
		Map<String, Set<MethodBinding>> methods = new HashMap<String, Set<MethodBinding>>();
		Set<ReferenceBinding> superinterfaces = new LinkedHashSet<ReferenceBinding>();
		boolean ignoreVisibility = true;
		while (null != binding) {
			addMembers(binding, ignoreVisibility, types, fields, methods);
			Set<ReferenceBinding> newfound = new LinkedHashSet<ReferenceBinding>();
			collectSuperInterfaces(binding, superinterfaces, newfound);
			for (ReferenceBinding superinterface : newfound) {
				addMembers(superinterface, false, types, fields, methods);
			}
			superinterfaces.addAll(newfound);
			binding = binding.superclass();
			ignoreVisibility = false;
		}
		List<Element> allMembers = new ArrayList<Element>();
		for (ReferenceBinding nestedType : types.values()) {
			allMembers.add(Factory.newElement(nestedType));
		}
		for (FieldBinding field : fields) {
			allMembers.add(Factory.newElement(field));
		}
		for (Set<MethodBinding> sameNamedMethods : methods.values()) {
			for (MethodBinding method : sameNamedMethods) {
				allMembers.add(Factory.newElement(method));
			}
		}
		return allMembers;
	}
	
	/**
	 * Recursively depth-first walk the tree of superinterfaces of a type, collecting
	 * all the unique superinterface bindings.  (Note that because of generics, a type may
	 * have multiple unique superinterface bindings corresponding to the same interface
	 * declaration.)
	 * @param existing bindings already in this set will not be re-added or recursed into
	 * @param newfound newly found bindings will be added to this set
	 */
	private void collectSuperInterfaces(ReferenceBinding type, 
			Set<ReferenceBinding> existing, Set<ReferenceBinding> newfound) {
		for (ReferenceBinding superinterface : type.superInterfaces()) {
			if (!existing.contains(superinterface) && !newfound.contains(superinterface)) {
				newfound.add(superinterface);
				collectSuperInterfaces(superinterface, existing, newfound);
			}
		}
	}

	/**
	 * Add the members of a type to the maps of subtypes, fields, and methods.  Add only those
	 * which are non-private and which are not overridden by an already-discovered member. 
	 * For fields, add them all; javac implementation does not take field hiding into account.
	 * @param binding the type whose members will be added to the lists
	 * @param ignoreVisibility if true, all members will be added regardless of whether they
	 * are private, overridden, etc.
	 * @param types a map of type simple name to type binding
	 * @param fields a list of field bindings
	 * @param methods a map of method simple name to set of method bindings with that name
	 */
	private void addMembers(ReferenceBinding binding, boolean ignoreVisibility, Map<String, ReferenceBinding> types,
			List<FieldBinding> fields, Map<String, Set<MethodBinding>> methods)
	{
		for (ReferenceBinding subtype : binding.memberTypes()) {
			if (ignoreVisibility || !subtype.isPrivate()) {
				String name = new String(subtype.sourceName());
				if (null == types.get(name)) {
					types.put(name, subtype);
				}
			}
		}
		for (FieldBinding field : binding.fields()) {
			if (ignoreVisibility || !field.isPrivate()) {
				fields.add(field);
			}
		}
		for (MethodBinding method : binding.methods()) {
			if (!method.isSynthetic() && (ignoreVisibility || (!method.isPrivate() && !method.isConstructor()))) {
				String methodName = new String(method.selector);
				Set<MethodBinding> sameNamedMethods = methods.get(methodName);
				if (null == sameNamedMethods) {
					// New method name.  Create a set for it and add it to the list.
					// We don't expect many methods with same name, so only 4 slots:
					sameNamedMethods = new HashSet<MethodBinding>(4); 
					methods.put(methodName, sameNamedMethods);
					sameNamedMethods.add(method);
				}
				else {
					// We already have a method with this name.  Is this method overridden?
					boolean unique = true;
					if (!ignoreVisibility) {
						for (MethodBinding existing : sameNamedMethods) {
							MethodVerifier verifier = _env.getLookupEnvironment().methodVerifier();
							if (verifier.doesMethodOverride(existing, method)) {
								unique = false;
								break;
							}
						}
					}
					if (unique) {
						sameNamedMethods.add(method);
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.util.Elements#getBinaryName(javax.lang.model.element.TypeElement)
	 */
	@Override
	public Name getBinaryName(TypeElement type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.util.Elements#getConstantExpression(java.lang.Object)
	 */
	@Override
	public String getConstantExpression(Object value) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.util.Elements#getDocComment(javax.lang.model.element.Element)
	 */
	@Override
	public String getDocComment(Element e) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI"); //$NON-NLS-1$
	}

	/**
	 * @return all the annotation instance's explicitly set values, plus default values
	 *         for all the annotation members that are not explicitly set but that have
	 *         defaults. By comparison, {@link AnnotationMirror#getElementValues()} only
	 *         returns the explicitly set values.
	 * @see javax.lang.model.util.Elements#getElementValuesWithDefaults(javax.lang.model.element.AnnotationMirror)
	 */
	@Override
	public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
			AnnotationMirror a) {
		return ((AnnotationMirrorImpl)a).getElementValuesWithDefaults();
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.util.Elements#getName(java.lang.CharSequence)
	 */
	@Override
	public Name getName(CharSequence cs) {
		return new NameImpl(cs);
	}

	@Override
	public PackageElement getPackageElement(CharSequence name) {
		LookupEnvironment le = _env.getLookupEnvironment();
		PackageBinding packageBinding = le.getTopLevelPackage(name.toString().toCharArray());
		if (packageBinding == null) {
			return null;
		}
		return new PackageElementImpl(packageBinding);
	}

	@Override
	public PackageElement getPackageOf(Element type) {
		switch(type.getKind()) {
			case ANNOTATION_TYPE :
			case CLASS :
			case ENUM :
			case INTERFACE :
				TypeElementImpl typeElementImpl = (TypeElementImpl) type;
				ReferenceBinding referenceBinding = (ReferenceBinding)typeElementImpl._binding;
				return (PackageElement) Factory.newElement(referenceBinding.fPackage);
			case PACKAGE :
				return (PackageElement) type;
			case CONSTRUCTOR :
			case METHOD :
				ExecutableElementImpl executableElementImpl = (ExecutableElementImpl) type;
				MethodBinding methodBinding = (MethodBinding) executableElementImpl._binding;
				return (PackageElement) Factory.newElement(methodBinding.declaringClass.fPackage);
			case ENUM_CONSTANT :
			case FIELD :
				VariableElementImpl variableElementImpl = (VariableElementImpl) type;
				FieldBinding fieldBinding = (FieldBinding) variableElementImpl._binding;
				return (PackageElement) Factory.newElement(fieldBinding.declaringClass.fPackage);
			case PARAMETER :
				variableElementImpl = (VariableElementImpl) type;
				LocalVariableBinding localVariableBinding = (LocalVariableBinding) variableElementImpl._binding;
				return (PackageElement) Factory.newElement(localVariableBinding.declaringScope.classScope().referenceContext.binding.fPackage);
			case EXCEPTION_PARAMETER :
			case INSTANCE_INIT :
			case OTHER :
			case STATIC_INIT :
			case TYPE_PARAMETER :
			case LOCAL_VARIABLE :
				return null;
		}
		// unreachable
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.util.Elements#getTypeElement(java.lang.CharSequence)
	 */
	@Override
	public TypeElement getTypeElement(CharSequence name) {
		LookupEnvironment le = _env.getLookupEnvironment();
		final char[][] compoundName = CharOperation.splitOn('.', name.toString().toCharArray());
		ReferenceBinding binding = le.getType(compoundName);
		// If we didn't find the binding, maybe it's a nested type;
		// try finding the top-level type and then working downwards.
		if (null == binding) {
			ReferenceBinding topLevelBinding = null;
			int topLevelSegments = compoundName.length; 
			while (--topLevelSegments > 0) {
				char[][] topLevelName = new char[topLevelSegments][];
				for (int i = 0; i < topLevelSegments; ++i) {
					topLevelName[i] = compoundName[i];
				}
				topLevelBinding = le.getType(topLevelName);
				if (null != topLevelBinding) {
					break;
				}
			}
			if (null == topLevelBinding) {
				return null;
			}
			binding = topLevelBinding;
			for (int i = topLevelSegments; null != binding && i < compoundName.length; ++i) {
				binding = binding.getMemberType(compoundName[i]);
			}
		}
		if (null == binding) {
			return null;
		}
		return new TypeElementImpl(binding);
	}

	/* (non-Javadoc)
	 * Element A hides element B if: A and B are both fields, both nested types, or both methods; and
	 * the enclosing element of B is a superclass or superinterface of the enclosing element of A.
	 * @see javax.lang.model.util.Elements#hides(javax.lang.model.element.Element, javax.lang.model.element.Element)
	 */
	@Override
	public boolean hides(Element hider, Element hidden) {
		throw new UnsupportedOperationException("NYI"); //$NON-NLS-1$
		// return ((ElementImpl)hider).hides(hidden);
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.util.Elements#isDeprecated(javax.lang.model.element.Element)
	 */
	@Override
	public boolean isDeprecated(Element e) {
		if (!(e instanceof ElementImpl)) {
			return false;
		}
		return (((ElementImpl)e)._binding.getAnnotationTagBits() & TagBits.AnnotationDeprecated) != 0;
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.util.Elements#overrides(javax.lang.model.element.ExecutableElement, javax.lang.model.element.ExecutableElement, javax.lang.model.element.TypeElement)
	 */
	@Override
	public boolean overrides(ExecutableElement overrider, ExecutableElement overridden,
			TypeElement type) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.lang.model.util.Elements#printElements(java.io.Writer, javax.lang.model.element.Element[])
	 */
	@Override
	public void printElements(Writer w, Element... elements) {
		// TODO Auto-generated method stub

	}

}
