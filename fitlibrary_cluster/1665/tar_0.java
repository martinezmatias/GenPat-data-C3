/*
 * Copyright (c) 2009 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/
package fitlibrary.debug;

import fit.Counts;
import fit.FitServerBridge;
import fit.FixtureListener;
import fit.Parse;
import fit.exception.FitParseException;
import fitlibrary.suite.BatchFitLibrary;
import fitlibrary.table.TablesOnParse;
import fitlibrary.utility.ParseUtility;
import fitlibrary.utility.TableListener;
import fitlibrary.utility.TestResults;

public class RunDirectly {
	protected FixtureListener fixtureListener = new FixtureListener() {
		public void tableFinished(@SuppressWarnings("unused") Parse table) {
			//
		}
		public void tablesFinished(@SuppressWarnings("unused") Counts count) {
			//
		}
	};
	BatchFitLibrary batchFitLibrary = new BatchFitLibrary(new TableListener(fixtureListener));

	private void run(String wiki) throws FitParseException {
		String html = html(wiki);
		Parse parse = new Parse(html);
		System.out.println("\n----------\nHTML\n----------\n"+html);
		TablesOnParse tables = new TablesOnParse(parse);
		FitServerBridge.setFitNesseUrl(""); // Yuck passing important info through a global. See method for links.
		TestResults testResults = batchFitLibrary.doStorytest(tables);
		System.out.println("\n----------\nHTML Report\n----------\n"+ParseUtility.toString(parse));
		System.out.println(testResults);
	}
	@SuppressWarnings("unused")
	private String html(String wiki) {
		String result = "<table><tr><td>fitlibrary.specify.dynamicVariable.DynamicVariablesUnderTest</td></tr></table>";
		return result;
	}
	private static void running(String wiki) {
		try {
			new RunDirectly().run(wiki);
		} catch (FitParseException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		String html = "|a|";
		running(html);
	}
}
