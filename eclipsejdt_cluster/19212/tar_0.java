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
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;

public abstract class PatternLocator implements IIndexConstants {

protected int matchMode;
protected boolean isCaseSensitive;

/* match levels */
public static final int IMPOSSIBLE_MATCH = 0;
public static final int INACCURATE_MATCH = 1;
public static final int POSSIBLE_MATCH = 2;
public static final int ACCURATE_MATCH = 3;

/* match container */
public static final int COMPILATION_UNIT_CONTAINER = 1;
public static final int CLASS_CONTAINER = 2;
public static final int METHOD_CONTAINER = 4;
public static final int FIELD_CONTAINER = 8;
public static final int ALL_CONTAINER =
	COMPILATION_UNIT_CONTAINER | CLASS_CONTAINER | METHOD_CONTAINER | FIELD_CONTAINER;

public static PatternLocator patternLocator(SearchPattern pattern) {
	switch (((InternalSearchPattern)pattern).kind) {
		case IIndexConstants.PKG_REF_PATTERN :
			return new PackageReferenceLocator((PackageReferencePattern) pattern);
		case IIndexConstants.PKG_DECL_PATTERN :
			return new PackageDeclarationLocator((PackageDeclarationPattern) pattern);
		case IIndexConstants.TYPE_REF_PATTERN :
			return new TypeReferenceLocator((TypeReferencePattern) pattern);
		case IIndexConstants.TYPE_DECL_PATTERN :
			return new TypeDeclarationLocator((TypeDeclarationPattern) pattern);
		case IIndexConstants.SUPER_REF_PATTERN :
			return new SuperTypeReferenceLocator((SuperTypeReferencePattern) pattern);
		case IIndexConstants.CONSTRUCTOR_PATTERN :
			return new ConstructorLocator((ConstructorPattern) pattern);
		case IIndexConstants.FIELD_PATTERN :
			return new FieldLocator((FieldPattern) pattern);
		case IIndexConstants.METHOD_PATTERN :
			return new MethodLocator((MethodPattern) pattern);
		case IIndexConstants.OR_PATTERN :
			return new OrLocator((OrPattern) pattern);
		case IIndexConstants.LOCAL_VAR_PATTERN :
			return new LocalVariableLocator((LocalVariablePattern) pattern);
	}
	return null;
}
public static char[] qualifiedPattern(char[] simpleNamePattern, char[] qualificationPattern) {
	// NOTE: if case insensitive search then simpleNamePattern & qualificationPattern are assumed to be lowercase
	if (simpleNamePattern == null) {
		if (qualificationPattern == null) return null;
		return CharOperation.concat(qualificationPattern, ONE_STAR, '.');
	} else {
		return qualificationPattern == null
			? CharOperation.concat(ONE_STAR, simpleNamePattern)
			: CharOperation.concat(qualificationPattern, simpleNamePattern, '.');
	}
}
public static char[] qualifiedSourceName(TypeBinding binding) {
	if (binding instanceof ReferenceBinding) {
		ReferenceBinding type = (ReferenceBinding) binding;
		if (type.isLocalType())
			return type.isMemberType()
				? CharOperation.concat(qualifiedSourceName(type.enclosingType()), type.sourceName(), '.')
				: CharOperation.concat(qualifiedSourceName(type.enclosingType()), new char[] {'.', '1', '.'}, type.sourceName());
	}
	return binding != null ? binding.qualifiedSourceName() : null;
}

public PatternLocator(SearchPattern pattern) {
	int matchRule = pattern.getMatchRule();
	this.isCaseSensitive = (matchRule & SearchPattern.R_CASE_SENSITIVE) != 0;
	this.matchMode = matchRule - (this.isCaseSensitive ? SearchPattern.R_CASE_SENSITIVE : 0);
}
/* (non-Javadoc)
 * Modify PatternLocator.qualifiedPattern behavior:
 * do not add star before simple name pattern when qualification pattern is null.
 * This avoid to match p.X when pattern is only X...
 */
protected char[] getQualifiedPattern(char[] simpleNamePattern, char[] qualificationPattern) {
	// NOTE: if case insensitive search then simpleNamePattern & qualificationPattern are assumed to be lowercase
	if (simpleNamePattern == null) {
		if (qualificationPattern == null) return null;
		return CharOperation.concat(qualificationPattern, ONE_STAR, '.');
	} else if (qualificationPattern == null) {
		return simpleNamePattern;
	} else {
		return CharOperation.concat(qualificationPattern, simpleNamePattern, '.');
	}
}
/* (non-Javadoc)
 * Modify PatternLocator.qualifiedSourceName behavior:
 * also concatene enclosing type name when type is a only a member type.
 */
protected char[] getQualifiedSourceName(TypeBinding binding) {
	TypeBinding type = binding instanceof ArrayBinding ? ((ArrayBinding)binding).leafComponentType : binding;
	if (type instanceof ReferenceBinding) {
		if (type.isLocalType()) {
			return CharOperation.concat(qualifiedSourceName(type.enclosingType()), new char[] {'.', '1', '.'}, binding.sourceName());
		} else if (type.isMemberType()) {
			return CharOperation.concat(qualifiedSourceName(type.enclosingType()), binding.sourceName(), '.');
		}
	}
	return binding != null ? binding.qualifiedSourceName() : null;
}
/*
 * Get binding of type argument from a class unit scope and its index position.
 * Cache is lazy initialized and if no binding is found, then store a problem binding
 * to avoid making research twice...
 */
protected TypeBinding getTypeNameBinding(int index) {
	return null;
}
/**
 * Initializes this search pattern so that polymorphic search can be performed.
 */ 
public void initializePolymorphicSearch(MatchLocator locator) {
	// default is to do nothing
}
/**
 * Check if the given ast node syntactically matches this pattern.
 * If it does, add it to the match set.
 * Returns the match level.
 */
public int match(ASTNode node, MatchingNodeSet nodeSet) { // needed for some generic nodes
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(Expression node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(FieldDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(LocalDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(MethodDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(MessageSend node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(Reference node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(TypeDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(TypeReference node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
/**
 * Returns the type(s) of container for this pattern.
 * It is a bit combination of types, denoting compilation unit, class declarations, field declarations or method declarations.
 */
protected int matchContainer() {
	// override if the pattern can be more specific
	return ALL_CONTAINER;
}
/**
 * Returns whether the given name matches the given pattern.
 */
protected boolean matchesName(char[] pattern, char[] name) {
	if (pattern == null) return true; // null is as if it was "*"
	if (name != null) {
		switch (this.matchMode) {
			case SearchPattern.R_EXACT_MATCH :
				return CharOperation.equals(pattern, name, this.isCaseSensitive);
			case SearchPattern.R_PREFIX_MATCH :
				return CharOperation.prefixEquals(pattern, name, this.isCaseSensitive);
			case SearchPattern.R_PATTERN_MATCH :
				if (!this.isCaseSensitive)
					pattern = CharOperation.toLowerCase(pattern);
				return CharOperation.match(pattern, name, this.isCaseSensitive);
		}
	}
	return false;
}
/**
 * Returns whether the given type reference matches the given pattern.
 */
protected boolean matchesTypeReference(char[] pattern, TypeReference type) {
	if (pattern == null) return true; // null is as if it was "*"
	if (type == null) return true; // treat as an inexact match

	char[][] compoundName = type.getTypeName();
	char[] simpleName = compoundName[compoundName.length - 1];
	int dimensions = type.dimensions() * 2;
	if (dimensions > 0) {
		int length = simpleName.length;
		char[] result = new char[length + dimensions];
		System.arraycopy(simpleName, 0, result, 0, length);
		for (int i = length, l = result.length; i < l;) {
			result[i++] = '[';
			result[i++] = ']';
		}
		simpleName = result;
	}

	return matchesName(pattern, simpleName);
}
/**
 * Returns the match level for the given importRef.
 */
protected int matchLevel(ImportReference importRef) {
	// override if interested in import references which are caught by the generic version of match(ASTNode, MatchingNodeSet)
	return IMPOSSIBLE_MATCH;
}
/**
 * Reports the match of the given import reference if the resolveLevel is high enough.
 */
protected void matchLevelAndReportImportRef(ImportReference importRef, Binding binding, MatchLocator locator) throws CoreException {
	int level = resolveLevel(binding);
	if (level >= INACCURATE_MATCH) {
		matchReportImportRef(
			importRef, 
			binding, 
			locator.createImportHandle(importRef), 
			level == ACCURATE_MATCH
				? SearchMatch.A_ACCURATE
				: SearchMatch.A_INACCURATE,
			locator);
	}
}
/**
 * Reports the match of the given import reference.
 */
protected void matchReportImportRef(ImportReference importRef, Binding binding, IJavaElement element, int accuracy, MatchLocator locator) throws CoreException {
	if (locator.encloses(element)) {
		// default is to report a match as a regular ref.
		this.matchReportReference(importRef, element, accuracy, locator);
	}
}
/**
 * Reports the match of the given reference.
 */
protected void matchReportReference(ASTNode reference, IJavaElement element, int accuracy, MatchLocator locator) throws CoreException {
	SearchMatch match = null;
	int referenceType = referenceType();
	int offset = reference.sourceStart;
	switch (referenceType) {
		case IJavaElement.PACKAGE_FRAGMENT:
			match = locator.newPackageReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaElement.TYPE:
			match = locator.newTypeReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaElement.FIELD:
			match = locator.newFieldReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaElement.METHOD:
			boolean isConstructor = reference instanceof ExplicitConstructorCall;
			boolean isSynthetic = isConstructor ? ((ExplicitConstructorCall) reference).isImplicitSuper() : false;
			match = locator.newMethodReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, isConstructor, isSynthetic, reference);
			break;
		case IJavaElement.LOCAL_VARIABLE:
			match = locator.newLocalVariableReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
	}
	if (match != null) {
		locator.report(match);
	}
}
public SearchMatch newDeclarationMatch(ASTNode reference, IJavaElement element, int accuracy, int length, MatchLocator locator) {
    return locator.newDeclarationMatch(element, accuracy, reference.sourceStart, length);
}
protected int referenceType() {
	return 0; // defaults to unknown (a generic JavaSearchMatch will be created)
}
/**
 * Finds out whether the given ast node matches this search pattern.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 * Returns INACCURATE_MATCH if it potentially matches this search pattern (ie. 
 * it has already been resolved but resolving failed.)
 * Returns ACCURATE_MATCH if it matches exactly this search pattern (ie. 
 * it doesn't need to be resolved or it has already been resolved.)
 */
public int resolveLevel(ASTNode possibleMatchingNode) {
	// only called with nodes which were possible matches to the call to matchLevel
	// need to do instance of checks to find out exact type of ASTNode
	return IMPOSSIBLE_MATCH;
}
/*
 * Refine accuracy for a match.
 * Typically this happens while search references to parameterized type.
 * In this case we need locator to be able to resolve type arguments and verify
 * if binding is compatible with pattern...
 */
protected int refineAccuracy(int accuracy, ParameterizedTypeBinding parameterizedBinding, char[] typeSignature, MatchLocator locator) {

	// Try to make accuracy more... accurate
	if (locator.unitScope != null) {
		char[][] patternTypeArguments = Signature.getTypeArguments(typeSignature);
		int patternTypeArgsLength = patternTypeArguments.length;
		if (patternTypeArgsLength == 0) {
			TypeBinding enclosingType = parameterizedBinding.enclosingType();
			if (enclosingType != null && enclosingType.isParameterizedType()) {
				int idx = CharOperation.lastIndexOf('.', typeSignature);
				if (idx > 0) {
					char[] enclosingTypeSignature = new char[idx+1];
					System.arraycopy(typeSignature, 0, enclosingTypeSignature, 0, idx);
					enclosingTypeSignature[idx] = Signature.C_SEMICOLON;
					return refineAccuracy(accuracy, (ParameterizedTypeBinding)enclosingType, enclosingTypeSignature, locator);
				}
			}
			return accuracy;
		}
		TypeBinding[] argumentsBinding = parameterizedBinding.arguments;
		int typeArgumentsLength = argumentsBinding == null ? 0 : argumentsBinding.length;
		if (patternTypeArgsLength != typeArgumentsLength) return -1;
		for (int i=0; i<typeArgumentsLength; i++) {
			char[] patternTypeArgument = patternTypeArguments[i];
			char wildcard = patternTypeArgument[0];
			switch (wildcard) {
				case Signature.C_STAR :
					// unbound parameter always match
					continue;
				case Signature.C_EXTENDS :
				case Signature.C_SUPER :
					patternTypeArgument = CharOperation.subarray(patternTypeArgument, 1, patternTypeArgument.length);
				default :
					break;
			}
			patternTypeArgument = Signature.toCharArray(patternTypeArgument);
			TypeBinding patternBinding = locator.getType(patternTypeArgument);
			if (patternBinding != null) {
				TypeBinding argumentBinding = argumentsBinding[i];
				// We can bind pattern type name => verify that types are compatible
				if (argumentBinding == patternBinding) continue;
				if (argumentBinding.isWildcard()) {
					TypeBinding bound = ((WildcardBinding) argumentBinding).bound;
					switch (wildcard) {
						default : //UNBOUND
							// unbound always match => skip to next argument
							continue;
						case Signature.C_EXTENDS :
							if (bound == null || bound.isCompatibleWith(patternBinding))
								// argument type match a subclass of bound => skip to next argument
								continue;
							break;
						case Signature.C_SUPER :
							if (bound == null || patternBinding.isCompatibleWith(bound))
								// argument type match a superclass in bound hierarchy => skip to next argument
								continue;
							break;
					}
				}
				return -1; // finally the match was impossible
			}
		}
	}
	return SearchMatch.A_ACCURATE;
}
/**
 * Finds out whether the given binding matches this search pattern.
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed but match is still possible.
 * Returns IMPOSSIBLE_MATCH otherwise.
 * Default is to return INACCURATE_MATCH.
 */
public int resolveLevel(Binding binding) {
	// override if the pattern can match the binding
	return INACCURATE_MATCH;
}
/**
 * Returns whether the given type binding matches the given simple name pattern 
 * and qualification pattern.
 * Note that from since 3.1, this method resolve to accurate member or local types
 * even if they are not fully qualified (ie. X.Member instead of p.X.Member).
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 */
protected int resolveLevelForType(char[] simpleNamePattern, char[] qualificationPattern, TypeBinding binding) {
//	return resolveLevelForType(qualifiedPattern(simpleNamePattern, qualificationPattern), type);
	char[] qualifiedPattern = getQualifiedPattern(simpleNamePattern, qualificationPattern);
	int level = resolveLevelForType(qualifiedPattern, binding);
	if (level == ACCURATE_MATCH || binding == null) return level;
	boolean match = false;
	TypeBinding type = binding instanceof ArrayBinding ? ((ArrayBinding)binding).leafComponentType : binding;
	if (type.isMemberType() || type.isLocalType()) {
		if (qualificationPattern != null) {
			match = CharOperation.equals(qualifiedPattern, getQualifiedSourceName(binding), this.isCaseSensitive);
		} else {
			match = CharOperation.equals(qualifiedPattern, binding.sourceName(), this.isCaseSensitive); // need to keep binding to get source name
		}
	} else if (qualificationPattern == null) {
		match = CharOperation.equals(qualifiedPattern, getQualifiedSourceName(binding), this.isCaseSensitive);
	}
	return match ? ACCURATE_MATCH : IMPOSSIBLE_MATCH;

}

/**
 * Returns whether the given type binding matches the given qualified pattern.
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 */
protected int resolveLevelForType(char[] qualifiedPattern, TypeBinding type) {
	if (qualifiedPattern == null) return ACCURATE_MATCH;
	if (type == null) return INACCURATE_MATCH;

	// NOTE: if case insensitive search then qualifiedPattern is assumed to be lowercase

	char[] qualifiedPackageName = type.qualifiedPackageName();
	char[] qualifiedSourceName = qualifiedSourceName(type);
	char[] fullyQualifiedTypeName = qualifiedPackageName.length == 0
		? qualifiedSourceName
		: CharOperation.concat(qualifiedPackageName, qualifiedSourceName, '.');
	return CharOperation.match(qualifiedPattern, fullyQualifiedTypeName, this.isCaseSensitive)
		? ACCURATE_MATCH
		: IMPOSSIBLE_MATCH;
}
/* (non-Javadoc)
 * Resolve level for type with a given binding with all pattern information.
 */
protected int resolveLevelForType (char[] simpleNamePattern,
									char[] qualificationPattern,
									char[] patternTypeSignature,
									boolean mustResolve,
									boolean declaration,
									TypeBinding type) {
	// standard search with no generic additional information must succeed
	int level = resolveLevelForType(simpleNamePattern, qualificationPattern, type);
	if (level == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;
	if (type == null || patternTypeSignature == null) return level;

	// pattern has no type parameter, return standard result
	char[][] patternTypeArguments = Signature.getTypeArguments(patternTypeSignature);
	if (patternTypeArguments == null || patternTypeArguments.length == 0) {
		return level;
	}

	// pattern has type parameter(s) or type argument(s)
	boolean isRawType = type.isRawType();
	if (type.isGenericType()) {
		// Binding is generic, get its type variable(s)
		TypeVariableBinding[] typeVariables = null;
		if (type instanceof SourceTypeBinding) {
			SourceTypeBinding sourceTypeBinding = (SourceTypeBinding) type;
			typeVariables = sourceTypeBinding.typeVariables;
		} else if (type instanceof BinaryTypeBinding) {
			BinaryTypeBinding binaryTypeBinding = (BinaryTypeBinding) type;
			if (mustResolve)
				typeVariables = binaryTypeBinding.typeVariables(); // TODO (frederic) verify performance
		}
		// type variables length must match at least specified type names length
		if (typeVariables == null || typeVariables.length == 0) {
			return IMPOSSIBLE_MATCH;
		}
		int length = patternTypeArguments.length;
		if (typeVariables.length != length) return IMPOSSIBLE_MATCH;
		// TODO (frederic) do we need to verify each parameter?
		return level; // we can't do better
	} else if (!type.isParameterizedType() && !isRawType) {
		// Standard types (ie. neither generic nor parameterized nor raw types)
		// cannot match pattern with type parameters or arguments
		return IMPOSSIBLE_MATCH;
	} else {
		// Binding is parameterized type
		ParameterizedTypeBinding paramTypeBinding = (ParameterizedTypeBinding) type;
		if (paramTypeBinding.arguments == null) {
			// binding has no type parameters => ok for raw types
			if (isRawType) return level;
			// need to verify hierarchy for member types (raw type of generic member
			// are stored as parameterized types...)
			if (type.isMemberType() && qualificationPattern != null) {
				int lastDot = CharOperation.lastIndexOf('.', qualificationPattern);
				char[] enclosingQualificationPattern = lastDot==-1 ? null : CharOperation.subarray(qualificationPattern, 0, lastDot);
				char[] enclosingSimpleNamePattern = lastDot==-1 ? qualificationPattern : CharOperation.subarray(qualificationPattern, lastDot+1, qualificationPattern.length);
				if (resolveLevelForType(enclosingSimpleNamePattern, enclosingQualificationPattern, patternTypeSignature, mustResolve, declaration, paramTypeBinding.enclosingType()) == IMPOSSIBLE_MATCH) {
					return IMPOSSIBLE_MATCH;
				}
				return level;
			}
			return IMPOSSIBLE_MATCH;
		}

		// type parameters length must match at least specified type names length
		int length = patternTypeArguments.length;
		if (paramTypeBinding.arguments.length != length) return IMPOSSIBLE_MATCH;

		// for generic type declaration, verification is different than for parameterized type
		if (declaration) {
			// TODO (frederic) more verification to do here with type parameter bounds?
			return level;
		}

		// verify each pattern type parameter
		nextTypeArgument: for (int i= 0; i<length; i++) {
			char[] typeArgument = patternTypeArguments[i];
			TypeBinding argTypeBinding = paramTypeBinding.arguments[i];
			// get corresponding pattern wildcard
			switch (patternTypeArguments[i][0]) {
				case Signature.C_STAR : // unbound parameter always match
				case Signature.C_SUPER : // needs pattern type parameter binding
					// skip to next type argument as it will be resolved later
					continue;
				case Signature.C_EXTENDS :
					// remove wildcard from patter type argument
					typeArgument = CharOperation.subarray(typeArgument, 1, typeArgument.length);
				default :
					// no wildcard
					break;
			}
			// get pattern type argument from its signature
			typeArgument = Signature.toCharArray(typeArgument);

			// Verify that names match...
			// First if type argument is a wildcard
			if (argTypeBinding.isWildcard()) {
				WildcardBinding wildcardBinding = (WildcardBinding) argTypeBinding;
				switch (wildcardBinding.kind) {
					case Wildcard.EXTENDS:
						// We cannot know in this case...
						level = INACCURATE_MATCH;
					case Wildcard.UNBOUND:
						// there's no bound name to match => valid
						continue;
				}
				// try to match name in hierarchy
				ReferenceBinding boundBinding = (ReferenceBinding) wildcardBinding.bound;
				while (boundBinding != null) {
					if (CharOperation.equals(typeArgument, boundBinding.shortReadableName(), this.isCaseSensitive) ||
						CharOperation.equals(typeArgument, boundBinding.readableName(), this.isCaseSensitive)) {
						// found name in hierarchy => match
						continue nextTypeArgument;
					}
					boundBinding = boundBinding.superclass();
				}
				return IMPOSSIBLE_MATCH;
			}
			
			// try to match names when there's no wildcard
			// first get real binding
			ReferenceBinding refBinding = null;
			if (argTypeBinding.isArrayType()) {
				TypeBinding leafBinding = ((ArrayBinding) argTypeBinding).leafComponentType;
				if (!leafBinding.isBaseType()) {
					refBinding = (ReferenceBinding) leafBinding;
				}
			} else if (!argTypeBinding.isBaseType()) {
				refBinding = (ReferenceBinding) argTypeBinding;
			}
			// Compare name
			if (refBinding == null) {
				// Based type
				if (!CharOperation.equals(typeArgument, argTypeBinding.shortReadableName(), this.isCaseSensitive) &&
					!CharOperation.equals(typeArgument, argTypeBinding.readableName(), this.isCaseSensitive)) {
					return IMPOSSIBLE_MATCH;
				}
			} else {
				while (refBinding != null) {
					if (CharOperation.equals(typeArgument, refBinding.shortReadableName(), this.isCaseSensitive) ||
						CharOperation.equals(typeArgument, refBinding.readableName(), this.isCaseSensitive)) {
						// found name in hierarchy => match
						continue nextTypeArgument;
					}
					refBinding = refBinding.superclass();
				}
				return IMPOSSIBLE_MATCH;
			}
		}
		return level;
	}
}
protected void setUnitScope(CompilationUnitScope unitScope) {
	// do nothing
}
public String toString(){
	return "SearchPattern"; //$NON-NLS-1$
}
}
