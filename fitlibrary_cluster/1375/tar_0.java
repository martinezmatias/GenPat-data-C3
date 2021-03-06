/*
 * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/
package fitlibrary;

import java.util.List;

import fit.Fixture;
import fit.Parse;
import fitlibrary.runResults.TestResults;
import fitlibrary.table.Table;
import fitlibrary.traverse.workflow.DoEvaluator;
import fitlibrary.traverse.workflow.DoTraverse;

/** An alternative to fit.ActionFixture
	@author rick mugridge, july 2003
  * 
  * See the specifications for examples
*/
public class DoFixture extends FitLibraryFixture implements DoEvaluator {
	private DoTraverse doTraverse = new DoTraverse(this);
	
	public DoFixture() {
    	setTraverse(doTraverse);
	}
	public DoFixture(Object sut) {
		this();
	    setSystemUnderTest(sut);
	}

	public void setTraverse(DoTraverse traverse) {
    	this.doTraverse = traverse;
    	super.setTraverse(traverse);
    }
    // Dispatched to from Fixture when a DoFixture is the first fixture in a storytest
    @Override
	final public void interpretTables(Parse parseTables) {
    	throw new RuntimeException("Please use FitLibraryServer instead of FitServer.");
    }
    // Dispatched to from Fixture when Fixture is doTabling the tables one by one (not in flow)
	@Override
	public Object interpretAfterFirstRow(Table table, TestResults testResults) {
    	return ((DoTraverse)traverse()).interpretInFlow(table,testResults);
    }
	/**
	 * if (stopOnError) then we don't continue interpreting a table
	 * as there's been a problem
	 */
	public void setStopOnError(boolean stopOnError) {
		doTraverse.setStopOnError(stopOnError);
	}
	public void abandon() {
		doTraverse.abandonStorytest();
	}
	public void showAfterTable(String s) {
		doTraverse.showAfterTable(s);
	}
	public void showAsAfterTable(String title,String s) {
		doTraverse.showAsAfterTable(title,s);
	}
	public Object getSymbolNamed(String fitSymbolName) {
		return Fixture.getSymbol(fitSymbolName);
	}
	public void setExpandDefinedActions(boolean expandDefinedActions) {
		doTraverse.setExpandDefinedActions(expandDefinedActions);
	}
	public Object interpretInFlow(Table firstTable, TestResults testResults) {
		return doTraverse.interpretInFlow(firstTable,testResults);
	}
	// --------- Interpretation ---------------------------------------
	public List<String> methodsThatAreVisible() {
		return doTraverse.methodsThatAreVisible();
	}
}
