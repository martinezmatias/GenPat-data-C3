/*
 * Copyright (c) 2009 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/
package fitlibrary.traverse.workflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fitlibrary.definedAction.DefinedActionsRepository;
import fitlibrary.runtime.RuntimeContextInternal;
import fitlibrary.table.Row;
import fitlibrary.table.RowOnParse;
import fitlibrary.table.Table;
import fitlibrary.traverse.workflow.caller.ValidCall;
import fitlibrary.utility.TestResults;

public class PlainTextAnalyser {
	private final RuntimeContextInternal runtime;
	private final DefinedActionsRepository definedActionsRepository;

	public PlainTextAnalyser(RuntimeContextInternal runtime, DefinedActionsRepository definedActionsRepository) {
		this.runtime = runtime;
		this.definedActionsRepository = definedActionsRepository;
	}
	public void analyseAndReplaceRowsIn(Table table, TestResults testResults) {
		for (int r = 0; r < table.size(); r++) {
			Row newRow = analyse(table.row(r), testResults);
			table.replaceAt(r,newRow);
		}
	}
	private Row analyse(Row row, TestResults testResults) {
		String textCall = row.cell(0).fullText();
		List<ValidCall> results = new ArrayList<ValidCall>();
		definedActionsRepository.findPlainTextCall(textCall, results);
		if (results.isEmpty()) {
			row.cell(0).error(testResults, "Unknown action");
			return row;
		}
		removeShorterMatches(results);
		if (results.size() > 1) {
			row.cell(0).error(testResults, "Ambiguous action (see details in logs after table)");
			runtime.showAsAfterTable("plain text","Possible action tables:<br/>");
			for (ValidCall call: results)
				call.possibility(runtime.getGlobal());
			return row;
		}
		Row newRow = new RowOnParse();
		for (String word : results.get(0).getList())
			newRow.addCell(word);
		return newRow;
	}
	private void removeShorterMatches(List<ValidCall> results) {
		int longest = 0;
		for (ValidCall call: results)
			longest = Math.max(longest,call.getList().size());
		for (Iterator<ValidCall> it = results.iterator(); it.hasNext(); )
			if (it.next().getList().size() < longest)
				it.remove();
	}
}
