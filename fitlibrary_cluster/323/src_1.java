/*
 * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/
package fitlibrary.table;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fit.Fixture;
import fit.Parse;
import fitlibrary.dynamicVariable.VariableResolver;
import fitlibrary.exception.table.SingleNestedTableExpected;
import fitlibrary.global.PlugBoard;
import fitlibrary.runResults.TestResults;
import fitlibrary.utility.ExtendedCamelCase;
import fitlibrary.utility.HtmlUtils;

public class CellOnList extends TablesOnList implements Cell {
    static final Pattern COLSPAN_PATTERN = Pattern.compile(".*\\b(colspan\\s*=\\s*\"?\\s*(\\d+)\\s*\"?).*");
    private boolean cellIsInHiddenRow = false;
    private String fullText = "";
    
	public CellOnList() {
        super("td");
    }
    public CellOnList(String cellText) {
        this();
        this.fullText = cellText;
    }
	public CellOnList(Cell cell) {
		this(cell.fullText());
		setInnerTables(cell.getEmbeddedTables());
	}
	public CellOnList(Tables tables) {
		this();
		addTables(tables);
	}
	public CellOnList(String preamble, Tables tables) {
		this(tables);
		addToStartOfLeader(Fixture.label(preamble));
	}
	public void setText(String text) {
		this.fullText = text;
	}
	public String text(VariableResolver resolver) {
		String s = text();
		String resolve = resolver.resolve(s);
		if (!s.equals(resolve))
			fullText = resolve;
		return resolve;
	}
	public String text() {
        return Parse.unescape(Parse.unformat(fullText)).trim();
    }
	public boolean unresolved(VariableResolver resolver) {
		return text().startsWith("@{") && text().indexOf("}") == text().length()-1 &&
				text().equals(text(resolver));
	}
	public String camelledText(VariableResolver resolver) {
		return ExtendedCamelCase.camel(text(resolver));
	}
   public String textLower(VariableResolver resolver) {
        return text(resolver).toLowerCase();
    }
    public boolean matchesTextInLowerCase(String s, VariableResolver resolver) {
        return text(resolver).toLowerCase().equals(s.toLowerCase());
    }
    public boolean isBlank(VariableResolver resolver) {
        return text(resolver).equals("");
    }
	@Override
	public Cell deepCopy() {
		Cell copy = TableFactory.cell(fullText);
		for (Table table: this)
			copy.add(table.deepCopy());
		copy.setLeader(getLeader());
		copy.setTrailer(getTrailer());
		copy.setTagLine(getTagLine());
		return copy;
	}
    public void expectedElementMissing(TestResults testResults) {
        fail(testResults);
        addToBody(label("missing"));
    }
    public void actualElementMissing(TestResults testResults) {
        fail(testResults);
        addToBody(label("surplus"));
    }
	public void unexpected(TestResults testResults, String s) {
        fail(testResults);
        addToBody(label("unexpected "+s));
	}
    public void actualElementMissing(TestResults testResults, String value) {
        fail(testResults);
        fullText = Fixture.gray(Fixture.escape(value.toString()));
        addToBody(label("surplus"));
    }
    @Override
	public void pass(TestResults testResults) {
    	if (cellIsInHiddenRow)
    		System.out.println("Bug: colouring a cell in a hidden table");
    	super.pass(testResults);
    }
	public void pass(TestResults testResults, String msg) {
    	if (cellIsInHiddenRow)
    		System.out.println("Bug: colouring a cell in a hidden table");
    	super.pass(testResults);
        addToBody("<hr>" + Fixture.escape(msg) + label("actual"));
    }
    @Override
	public void fail(TestResults testResults) {
    	if (cellIsInHiddenRow)
    		System.out.println("Bug: colouring a cell in a hidden table");
    	super.fail(testResults);
    }
    public void fail(TestResults testResults, String msg, VariableResolver resolver) {
    	if (fullText.isEmpty() && !hasEmbeddedTables()) {
    		failHtml(testResults,msg);
    		return;
    	}
        fail(testResults);
        String resolved = "";
        if (!text().equals(text(resolver)))
        	resolved = " = "+text(resolver);
        addToBody(resolved+label("expected") + "<hr>" + Fixture.escape(msg)
                + label("actual"));
    }
    public void failWithStringEquals(TestResults testResults, String actual, VariableResolver resolver) {
    	if (fullText.isEmpty() && !hasEmbeddedTables()) {
    		failHtml(testResults,actual);
    		return;
    	}
        fail(testResults);
        String resolved = "";
        if (!text().equals(text(resolver)))
        	resolved = " = "+text(resolver);
        addToBody(resolved+label("expected") + "<hr>" + Fixture.escape(actual)
                + label("actual")+ differences(Fixture.escape(text(resolver)),Fixture.escape(actual)));
    }
	public static String differences(String actual, String expected) {
		return PlugBoard.stringDifferencing.differences(actual, expected);
	}
    public void failHtml(TestResults testResults, String msg) {
        fail(testResults);
        addToBody(msg);
    }
    @Override
	public void error(TestResults testResults, Throwable e) {
    	if (cellIsInHiddenRow)
    		System.out.println("Bug: colouring a cell in a hidden table");
        addToBody(PlugBoard.exceptionHandling.exceptionMessage(e));
        addToTag(ERROR);
        testResults.exception();
    }
   public void error(TestResults testResults, String msg) {
    	if (cellIsInHiddenRow)
    		System.out.println("Bug: colouring a cell in a hidden table");
        addToBody("<hr/>" + Fixture.label(msg));
        addToTag(ERROR);
        testResults.exception();
    }
   public void error(TestResults testResults) {
	   if (cellIsInHiddenRow)
		   System.out.println("Bug: colouring a cell in a hidden table");
	   addToTag(ERROR);
	   testResults.exception();
   }
	public void ignore(TestResults testResults) {
    	if (tagAnnotation.contains(CALLS))
    		return;
    	if (cellIsInHiddenRow)
    		System.out.println("Bug: colouring a cell in a hidden table");
        if (tagAnnotation.indexOf("class") >= 0)
        	throw new RuntimeException("Duplicate cell class in tag. Tag is already: "+
        			tagAnnotation);
        addToTag(IGNORE);
        testResults.ignore();
    }
    public void exceptionExpected(boolean exceptionExpected, Exception e, TestResults testResults) {
    	if (exceptionExpected)
    		pass(testResults);
    	else
    		error(testResults,e);
    }
    public Table getEmbeddedTable() {
        Tables tables = getEmbeddedTables();
        if (tables.size() != 1)
        	throw new SingleNestedTableExpected();
		return tables.at(0);
    }
    public void wrongHtml(TestResults counts, String actual) {
        fail(counts);
        addToBody(label("expected") + "<hr>" + actual
                + label("actual"));
    }
    private void addToBody(String msg) {
        fullText += msg;
    }
	public void setEscapedText(String text) {
		setText(Fixture.escape(text));
	}
	public void setMultilineEscapedText(String text) {
		setText(HtmlUtils.escape(text));
	}
	public String fullText() {
		return fullText;
	}
	public void setUnvisitedEscapedText(String s) {
		setUnvisitedText(Fixture.escape(s));
	}
	public void setUnvisitedMultilineEscapedText(String s) {
		setUnvisitedText(HtmlUtils.escape(s));
	}
	public void setUnvisitedText(String s) {
		setText(Fixture.gray(s));
	}
	public void passOrFailIfBlank(TestResults counts, VariableResolver resolver) {
		if (isBlank(resolver))
			pass(counts);
		else
			fail(counts,"",resolver);
	}
	public void passIfNotEmbedded(TestResults counts) {
		if (!hasEmbeddedTables()) // already coloured
			pass(counts);
	}
	public void setIsHidden() {
		this.cellIsInHiddenRow = true;
	}
	public void setInnerTables(Tables tables) {
		addTables(tables);
	}
	public int getColumnSpan() {
		Matcher matcher = COLSPAN_PATTERN.matcher(tagAnnotation);
		int colspan = 1;
		if (matcher.matches())
			colspan = Integer.parseInt(matcher.group(2));
		return colspan;
	}
	public void setColumnSpan(int colspan) {
		if (colspan < 1)
			return;
		Matcher matcher = COLSPAN_PATTERN.matcher(tagAnnotation);
		if (matcher.matches())
			tagAnnotation = tagAnnotation.replace(matcher.group(1), getColspanHtml(colspan));
		else
			addToTag(getColspanHtml(colspan));
	}
	private static String getColspanHtml(int colspan) {
		return " colspan=\""+colspan+"\"";
	}
    public Tables getEmbeddedTables() {
		return fromAt(0);
    }
    public boolean hasEmbeddedTables() {
        return !isEmpty();
    }
	@Override
	public String getType() {
		return "Cell";
	}
	@Override
	protected void appendBody(StringBuilder builder) {
		builder.append(fullText);
	}
	@Override
	public void addPrefixToFirstInnerTable(String s) {
		at(0).setLeader(Fixture.label(s)+getLeader());
	}
}
