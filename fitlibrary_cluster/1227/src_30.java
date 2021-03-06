/*
 * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/
package fitlibrary.closure;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fitlibrary.exception.NoSystemUnderTestException;
import fitlibrary.exception.method.MissingMethodException;
import fitlibrary.table.IRow;
import fitlibrary.table.Row;
import fitlibrary.traverse.DomainAdapter;
import fitlibrary.traverse.Evaluator;
import fitlibrary.traverse.Traverse;
import fitlibrary.typed.TypedObject;
import fitlibrary.utility.ClassUtility;
import fitlibrary.utility.ExtendedCamelCase;
import fitlibrary.utility.TestResults;

public class LookupMethodTargetStandard implements LookupMethodTarget {
	public void mustBeThreadSafe() {
		//
	}
	public CalledMethodTarget findSpecialMethod(Evaluator evaluator, String name) {
		if (name.equals(""))
			return null;
		Closure findEntityMethod = findFixturingMethod(evaluator,camel(name),new Class[]{ Row.class, TestResults.class });
		if (findEntityMethod == null)
			findEntityMethod = findFixturingMethod(evaluator,camel(name),new Class[]{ IRow.class });
		if (findEntityMethod == null)
			return null;
		return new CalledMethodTarget(findEntityMethod,evaluator);
	}
	public CalledMethodTarget findPostfixSpecialMethod(Evaluator evaluator, String name) {
		if (name.equals(""))
			return null;
		Closure findEntityMethod = findFixturingMethod(evaluator,camel(name),new Class[]{ TestResults.class, Row.class });
		if (findEntityMethod == null)
			return null;
		return new CalledMethodTarget(findEntityMethod,evaluator);
	}
	public Closure findFixturingMethod(Evaluator evaluator, String name, Class<?>[] argTypes) {
		return findFixturedMethod(evaluator, name, argTypes, new HashSet<Object>());
	}
	private Closure findFixturedMethod(Evaluator evaluator, String name, Class<?>[] argTypes, Set<Object> visitedObjects) {
		if (visitedObjects.contains(evaluator))
				return null;
		visitedObjects.add(evaluator);
		Closure method = asTypedObject(evaluator).findPublicMethodClosureForTypedObject(name,argTypes);
		if (method == null && evaluator.getSystemUnderTest() instanceof Evaluator)
			method = findFixturedMethod((Evaluator)evaluator.getSystemUnderTest(),name,argTypes,visitedObjects);
		if (method == null && evaluator.getSystemUnderTest() instanceof DomainAdapter)
			method = evaluator.getTypedSystemUnderTest().findPublicMethodClosureForTypedObject(name,argTypes);
		Evaluator nextOuterContext = evaluator.getNextOuterContext();
		if (method == null && nextOuterContext != null)
			method = findFixturedMethod(nextOuterContext,name,argTypes,visitedObjects);
		return method;
	}
	private static TypedObject asTypedObject(Object subject) {
		return Traverse.asTypedObject(subject);
	}
	public CalledMethodTarget findMethodInEverySecondCell(Evaluator evaluator, IRow row, int allArgs) throws Exception {
		int parms = allArgs / 2 + 1;
		int argCount = (allArgs + 1) / 2;
		String name = row.text(0,evaluator);
		for (int i = 1; i < parms; i++)
			name += " "+row.text(i*2,evaluator);
		CalledMethodTarget target = findTheMethodMapped(name,argCount,evaluator);
		target.setEverySecond(true);
		return target;
	}
	public CalledMethodTarget findTheMethodMapped(String name, int argCount, Evaluator evaluator) throws Exception {
		return findTheMethod(camel(name), unknownParameterNames(argCount),"Type",evaluator);
	}
	private static List<String> unknownParameterNames(int argCount) {
		List<String> methodArgs = new ArrayList<String>();
		for (int i = 0; i < argCount; i++)
			methodArgs.add("arg"+(i+1));
		return methodArgs;
	}
	public CalledMethodTarget findTheMethod(String name, List<String> methodArgs, String returnType, Evaluator evaluator) throws Exception {
		List<String> signatures = ClassUtility.methodSignatures(name, methodArgs, returnType);
		TypedObject typedObject = asTypedObject(evaluator);
		return typedObject.findSpecificMethodOrPropertyGetter(name,methodArgs.size(),evaluator,signatures);
	}
	public CalledMethodTarget findMethod(String name, List<String> methodArgs, String returnType, Evaluator evaluator) {
		Closure result = asTypedObject(evaluator).findMethodForTypedObject(name,methodArgs.size());
		if (result != null)
			return new CalledMethodTarget(result,evaluator);
		List<String> signatures = ClassUtility.methodSignatures(name, methodArgs, returnType);
		throw new MissingMethodException(signatures,identifiedClassesInOutermostContext(evaluator, true));
	}
	public CalledMethodTarget findSetter(String propertyName, Evaluator evaluator) {
		String methodName = ExtendedCamelCase.camel("set "+propertyName);
		String arg = camel(propertyName);
		TypedObject typedSubject = evaluator.getTypedSystemUnderTest();
    	if (typedSubject == null)
    		throw new NoSystemUnderTestException();
		CalledMethodTarget target = typedSubject.optionallyFindMethodOnTypedObject(methodName,1,evaluator, true);
		if (target != null)
			return target;
		throw new MissingMethodException(signatures("public void "+methodName+"(ArgType "+arg+") { }"),identifiedClassesInSUTChain(typedSubject.getSubject()));
	}
	public CalledMethodTarget findGetterUpContextsToo(TypedObject typedObject, Evaluator evaluator, String propertyName, boolean considerContext) {
		CalledMethodTarget target = typedObject.optionallyFindGetterOnTypedObject(propertyName,evaluator);
		if (considerContext && target == null)
			target = searchForMethodTargetUpOuterContext(propertyName,evaluator.getNextOuterContext(),evaluator);
		if (target != null)
			return target;
		String getMethodName = ExtendedCamelCase.camel("get "+propertyName);
		throw new MissingMethodException(signatures("public ResultType "+ getMethodName+"() { }"),identifiedClassesInSUTChain(typedObject.getSubject()));
	}
	private List<String> signatures(String... signature) {
		return Arrays.asList(signature);
	}
    private static CalledMethodTarget searchForMethodTargetUpOuterContext(String name, Evaluator outerContext, Evaluator evaluator) {
        if (outerContext == null)
            return null;
        CalledMethodTarget target = null;
        if (outerContext.getSystemUnderTest() != null) {
            TypedObject typedObject = outerContext.getTypedSystemUnderTest();
			target = typedObject.optionallyFindGetterOnTypedObject(name,evaluator);
        }
        if (target == null)
            return searchForMethodTargetUpOuterContext(name,outerContext.getNextOuterContext(),evaluator);
        return target;
    }
	public List<Class<?>> identifiedClassesInSUTChain(Object firstObject) {
		List<Class<?>> accumulatingClasses = new ArrayList<Class<?>>();
		identifiedClassListInSutChain(firstObject,accumulatingClasses,true);
		if (accumulatingClasses.isEmpty())
			accumulatingClasses.add(firstObject.getClass());
		return accumulatingClasses;
	}
	private static void identifiedClassListInSutChain(Object firstObject, List<Class<?>> accumulatingClasses, boolean includeSut) {
		Object object = firstObject;
		while (object instanceof DomainAdapter) {
			DomainAdapter domainAdapter = (DomainAdapter)object;
			object = domainAdapter.getSystemUnderTest();
			if (classToBeIncluded(accumulatingClasses, includeSut, object))
				accumulatingClasses.add(object.getClass());
		}
	}
	private static boolean classToBeIncluded(List<Class<?>> accumulatingClasses, boolean includeSut,
			Object object) {
		return object != null && (includeSut || object instanceof DomainAdapter) && 
				!ClassUtility.aFitLibraryClass(object.getClass()) && 
				!accumulatingClasses.contains(object.getClass());
	}
	public List<Class<?>> identifiedClassesInOutermostContext(Object firstObject, boolean includeSut) {
		Object object = firstObject;
		if (firstObject instanceof Evaluator)
			object = ((Evaluator)firstObject).getOutermostContext();
		List<Class<?>> accumulatingClasses = new ArrayList<Class<?>>();
		identifiedClassListInSutChain(object,accumulatingClasses,includeSut);
		if (accumulatingClasses.isEmpty())
			accumulatingClasses.add(firstObject.getClass());
		return accumulatingClasses;
	}
	public Class<?> findClassFromFactoryMethod(Evaluator evaluator, Class<?> type, String typeName) throws IllegalAccessException, InvocationTargetException {
		String methodName = "concreteClassOf"+ClassUtility.simpleClassName(type);
		Closure method = findFixturingMethod(evaluator, methodName, new Class[] { String.class});
		if (method == null) {
			throw new MissingMethodException(signatures("public Class "+methodName+"(String typeName) { }"),
					identifiedClassesInOutermostContext(evaluator, true));
		}
		return (Class<?>)method.invoke(new Object[]{ typeName });
	}
	public Closure findNewInstancePluginMethod(Evaluator evaluator) {
		return findFixturingMethod(evaluator,"newInstancePlugin", new Class[] {Class.class});
	}
	private static String camel(String name) {
		return ExtendedCamelCase.camel(name);
	}
}
