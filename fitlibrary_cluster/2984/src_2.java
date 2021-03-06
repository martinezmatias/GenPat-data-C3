/*
 * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/
package fitlibrary.parser;

import java.lang.reflect.Method;

import fitlibrary.DoFixture;
import fitlibrary.parser.lookup.ResultParser;
import fitlibrary.table.Cell;
import fitlibrary.traverse.Traverse;
import fitlibrary.utility.TestResults;

public class TestSimpleValueAdapter extends ParserTestCase {
	public int aProp = 567;

	public void testParseAlone() throws Exception {
		Parser parser = Traverse.asTyped(int.class).parser(evaluatorWithRuntime());
		String cellText = "12";
		Cell cell = new Cell(cellText);
		Integer expectedResult = new Integer(12);
		TestResults testResults = new TestResults();
		assertEquals(expectedResult,parser.parseTyped(cell,testResults).getSubject());
		assertTrue(parser.matches(cell, expectedResult,testResults));
		assertEquals(cellText,parser.show(expectedResult));
	}
	public void testParseWithMethod() throws Exception {
		Method method = getClass().getMethod("aMethod", new Class[] {});
		ResultParser adapter = Traverse.asTypedObject(this).resultParser(new DoFixture(), method);
		adapter.setTarget(this);
		assertEquals(new Integer(43),adapter.getResult());
		assertEquals("43",adapter.show(adapter.getResult()));
	}
	public int aMethod() {
		return 43;
	}
}
