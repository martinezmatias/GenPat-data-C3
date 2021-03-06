/*
 * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/
package fitlibraryGeneric.set;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fitlibrary.collection.CollectionSetUpTraverse;
import fitlibrary.collection.set.SetTraverse;
import fitlibrary.parser.Parser;
import fitlibrary.parser.collection.SetParser;
import fitlibrary.parser.lookup.ParserFactory;
import fitlibrary.table.Table;
import fitlibrary.traverse.Evaluator;
import fitlibrary.typed.Typed;
import fitlibrary.utility.TestResults;
import fitlibraryGeneric.list.ListSetUpTraverse2;
import fitlibraryGeneric.list.NestingListSetUpTraverse;
import fitlibraryGeneric.typed.GenericTyped;

public class SetParser2 extends SetParser {
	private GenericTyped componentTyped;
    
    public SetParser2(Evaluator evaluator, GenericTyped generic) {
        super(evaluator,generic);
        componentTyped = generic.getComponentTyped();
		generic.assertHasParameters(1);
    }
    @Override
    protected Object parseTable(Table table, TestResults testResults) {
    	switch (componentTyped.typeCases()) {
    	case CLASS_TYPE:
    		if (componentTyped.isArray())
        		return parseNested(table, testResults);
    		if (CollectionSetUpTraverse.hasObjectFactoryMethodFor(table,evaluator))
    			return super.parseTable(table,testResults);
    		ListSetUpTraverse2 setUp = new ListSetUpTraverse2(componentTyped.asClass());
    		setUp.interpretWithinScope(table,evaluator,testResults);
    		return new HashSet<Object>(setUp.getResults());
    	case PARAMETERIZED_TYPE:
    		return parseNested(table, testResults);
    	default:
    		throw new RuntimeException("Type not sufficiently bound: "+componentTyped);
    	}
    }
	private Object parseNested(Table table, TestResults testResults) {
		NestingListSetUpTraverse nestingSetUp = new NestingListSetUpTraverse(componentTyped);
		nestingSetUp.interpretWithinScope(table,evaluator,testResults);
		return new HashSet<Object>(nestingSetUp.getResults());
	}
    @SuppressWarnings({"unchecked", "fallthrough"})
	@Override
    protected boolean tableMatches(Table table, Object resultInitial, TestResults testResults) {
    	Object result = resultInitial;
    	switch (componentTyped.typeCases()) {
    	case CLASS_TYPE:
        	if (result instanceof Map) {
        		if (keyed(table))
        			result = new HashSet<Object>(SetTraverse.mapMapToSet((Map)result));
        		else
        			result = new HashSet<Object>(((Map)result).values());
        	}
        	if (!componentTyped.isArray()) {
        		SetTraverse setChecking = new SetTraverse(null);
        		setChecking.setActualCollection((Set)result);
        		setChecking.setComponentType(componentTyped.asClass());
        		return setChecking.doesInnerTablePass(table,evaluator,testResults);
        	}
    	case PARAMETERIZED_TYPE:
    		NestingSetTraverse nestingSet = new NestingSetTraverse((Set)result,componentTyped);
    		return nestingSet.doesTablePass(table,evaluator,testResults);
    	default:
    		throw new RuntimeException("Type not sufficiently bound: "+componentTyped);
    	}
    }
    private boolean keyed(Table table) {
    	return (table.row(0).cellExists(0) &&
    			table.row(0).text(0,evaluator).toLowerCase().equals("key")) ||
    			( table.row(0).cellExists(1) &&
    					table.row(0).text(1,evaluator).toLowerCase().equals("key"));
    }
    public static ParserFactory parserFactory() {
    	return new ParserFactory() {
    		public Parser parser(Evaluator evaluator, Typed typed) {
    			GenericTyped genericTyped = (GenericTyped) typed;
    			if (genericTyped.isGeneric())
    				return new SetParser2(evaluator,genericTyped);
    			return new SetParser(evaluator,typed);
    		}
    	};
    }
}
