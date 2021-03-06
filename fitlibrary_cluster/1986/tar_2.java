/*
 * Copyright (c) 2010 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/

package fitlibrary.flow;

import java.util.Iterator;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fitlibrary.runResults.ITableListener;
import fitlibrary.runResults.TestResults;
import fitlibrary.runResults.TestResultsFactory;
import fitlibrary.runtime.RuntimeContextInternal;
import fitlibrary.table.Cell;
import fitlibrary.table.Row;
import fitlibrary.table.Table;
import fitlibrary.table.Tables;
import fitlibrary.traverse.workflow.DoTraverse;
import fitlibrary.traverse.workflow.FlowEvaluator;
import fitlibrary.utility.CollectionUtility;
import fitlibraryGeneric.typed.GenericTypedObject;

@RunWith(JMock.class)
public class TestDoFlowWithNestedTables {
	final Mockery context = new Mockery();
	final FlowEvaluator flowEvaluator = context.mock(FlowEvaluator.class);
	final IScopeStack scopeStack = context.mock(IScopeStack.class);
	final TestResults testResults = TestResultsFactory.testResults();
	final ITableListener tableListener = context.mock(ITableListener.class);
	final IScopeState scopeState = context.mock(IScopeState.class);
	final RuntimeContextInternal runtime = context.mock(RuntimeContextInternal.class);
	final SetUpTearDown setUpTearDown = context.mock(SetUpTearDown.class);
	DoFlow doFlow;
	
	final Tables tables = context.mock(Tables.class,"tables");
	final Table table1 = context.mock(Table.class,"table1");
	final Row row1 = context.mock(Row.class,"row1");
	final Cell cell1 = context.mock(Cell.class,"cell1");
	final Row row2 = context.mock(Row.class,"row2");
	final Cell cell2 = context.mock(Cell.class,"cell2");
	final Table innerTable1 = context.mock(Table.class,"innerTable1");
	final Row innerRow1 = context.mock(Row.class,"innerRow1");
	final Cell innerCell = context.mock(Cell.class,"innerCell");

	@Before
	public void createDoFlow() {
		context.checking(new Expectations() {{
			allowing(tableListener).getTestResults(); will(returnValue(testResults));
			oneOf(scopeStack).clearAllButSuite();
			oneOf(scopeStack).setAbandon(false);
			oneOf(tableListener).storytestFinished();
			oneOf(runtime).setStopOnError(false);
			oneOf(runtime).reset();
			oneOf(runtime).setCurrentTable(table1);
			oneOf(runtime).pushTestResults(with(any(TestResults.class)));
			allowing(runtime).isAbandoned(with(any(TestResults.class))); will(returnValue(false));
			oneOf(runtime).setCurrentRow(row1);
			oneOf(runtime).setCurrentRow(innerRow1);
			oneOf(runtime).popTestResults();
			oneOf(runtime).addAccumulatedFoldingText(table1);
		}});
		expectTwoRowsInFirstCellOfTable();
		doFlow = new DoFlow(flowEvaluator,scopeStack,runtime,setUpTearDown);
	}
	private void expectTwoRowsInFirstCellOfTable() {
		context.checking(new Expectations() {{
			allowing(tables).size(); will(returnValue(1));
			allowing(tables).at(0); will(returnValue(table1));
			allowing(tables).last(); will(returnValue(table1));
			allowing(table1).size(); will(returnValue(2));
			allowing(table1).at(0); will(returnValue(row1));
			allowing(row1).at(0); will(returnValue(cell1));
			allowing(cell1).hasEmbeddedTables(); will(returnValue(false));
			allowing(cell1).hadError(); will(returnValue(false));
			allowing(row1).size(); will(returnValue(2));
			allowing(row2).size(); will(returnValue(2));
			
			allowing(table1).isPlainTextTable(); will(returnValue(false));
			allowing(table1).at(1); will(returnValue(row2));
			allowing(row2).at(0); will(returnValue(cell2));
			allowing(cell2).hasEmbeddedTables(); will(returnValue(true));
			allowing(cell2).getEmbeddedTables(); will(returnValue(cell2));
			allowing(cell2).hadError(); will(returnValue(false));
			allowing(cell2).size(); will(returnValue(1));
			Iterator<Table> result = list(innerTable1).iterator();
			allowing(cell2).iterator(); will(returnValue(result));
			allowing(cell2).at(0); will(returnValue(innerTable1));
			allowing(innerTable1).size(); will(returnValue(1));
			allowing(innerTable1).at(0); will(returnValue(innerRow1));
			allowing(innerRow1).at(0); will(returnValue(innerCell));
			allowing(innerCell).hasEmbeddedTables(); will(returnValue(false));
			allowing(innerCell).hadError(); will(returnValue(false));
			allowing(innerRow1).size(); will(returnValue(2));
		}});
	}
	
	@Test
	public void innerTableIsRun() {
		final GenericTypedObject typedResult1 = new GenericTypedObject(new DoTraverse("s"));
		final GenericTypedObject typedResult2 = new GenericTypedObject(new DoTraverse("t"));
		final GenericTypedObject genS = new GenericTypedObject("s");
		final GenericTypedObject genT = new GenericTypedObject("t");
		context.checking(new Expectations() {{
			oneOf(flowEvaluator).interpretRow(row1,testResults);
			  will(returnValue(typedResult1));
			oneOf(scopeStack).push(genS);
			oneOf(setUpTearDown).callSetUpSutChain("s", row1, testResults);
			oneOf(setUpTearDown).callTearDownSutChain("s", row1, testResults);
			
			oneOf(scopeStack).currentState(); will(returnValue(scopeState));
			
			oneOf(flowEvaluator).interpretRow(innerRow1,testResults);
			  will(returnValue(typedResult2));
			oneOf(scopeStack).push(genT);
			oneOf(setUpTearDown).callSetUpSutChain("t", innerRow1, testResults);
			oneOf(setUpTearDown).callTearDownSutChain("t", row1, testResults);
			
			oneOf(scopeState).restore();

			oneOf(scopeStack).poppedAtEndOfStorytest(); will(returnValue(list(genT,genS)));
			oneOf(tableListener).tableFinished(table1);
		}});
		doFlow.runStorytest(tables,tableListener);
	}
	protected <T> List<T> list(T... ss) {
		return CollectionUtility.list(ss);
	}
}
