package org.eclipse.swt.custom;

/*
 * Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */

import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.printing.*;
import org.eclipse.swt.widgets.*;


/**
 * A StyledText is an editable user interface object that displays lines 
 * of text.  The following style attributes can be defined for the text: 
 * <ul>
 * <li>foreground color 
 * <li>background color
 * <li>font style (bold, regular)
 * </ul>
 * <p>
 * In addition to text style attributes, the background color of a line may 
 * be specified.
 * </p>
 * <p>
 * There are two ways to use this widget when specifying text style information.  
 * You may use the API that is defined for StyledText or you may define your own 
 * LineStyleListener.  If you define your own listener, you will be responsible 
 * for maintaining the text style information for the widget.  IMPORTANT: You may 
 * not define your own listener and use the StyledText API.  The following
 * StyledText API is not supported if you have defined a LineStyleListener:
 * <ul>
 * <li>getStyleRangeAtOffset(int)
 * <li>getStyleRanges()
 * <li>setStyleRange(StyleRange)
 * <li>setStyleRanges(StyleRange[])
 * </ul>
 * </p>
 * <p>
 * There are two ways to use this widget when specifying line background colors.
 * You may use the API that is defined for StyledText or you may define your own 
 * LineBackgroundListener.  If you define your own listener, you will be responsible 
 * for maintaining the line background color information for the widget.  
 * IMPORTANT: You may not define your own listener and use the StyledText API.  
 * The following StyledText API is not supported if you have defined a 
 * LineBackgroundListener:
 * <ul>
 * <li>getLineBackground(int)
 * <li>setLineBackground(int,int,Color)
 * </ul>
 * </p>
 * <p>
 * The content implementation for this widget may also be user-defined.  To do so,
 * you must implement the StyledTextContent interface and use the StyledText API
 * setContent(StyledTextContent) to initialize the widget. 
 * </p>
 * <p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 * <dl>
 * <dt><b>Styles:</b><dd>FULL_SELECTION, MULTI, READ_ONLY, SINGLE, WRAP
 * <dt><b>Events:</b><dd>ExtendedModify, LineGetBackground, LineGetSegments, LineGetStyle, Modify, Selection, Verify, VerifyKey
 * </dl>
 */
public class StyledText extends Canvas {
	static final char TAB = '\t';
	static final String PlatformLineDelimiter = System.getProperty("line.separator");
	static final int BIDI_CARET_WIDTH = 4;		
	static final int XINSET = BIDI_CARET_WIDTH - 1;
	static final int DEFAULT_WIDTH	= 64;
	static final int DEFAULT_HEIGHT = 64;
	
	static final int ExtendedModify = 3000;
	static final int LineGetBackground = 3001;
	static final int LineGetStyle = 3002;
	static final int TextChanging = 3003;
	static final int TextSet = 3004;
	static final int VerifyKey = 3005;
	static final int TextChanged = 3006;
	static final int LineGetSegments = 3007;
	
	StyledTextContent logicalContent;	// native content (default or user specified)
	StyledTextContent content;			// line wrapping content, same as logicalContent if word wrap is off
	DisplayRenderer renderer;
	TextChangeListener textChangeListener;	// listener for TextChanging, TextChanged and TextSet events from StyledTextContent
	DefaultLineStyler defaultLineStyler;// used for setStyles API when no LineStyleListener is registered
	LineCache lineCache;
	boolean userLineStyle = false;		// true=widget is using a user defined line style listener for line styles. false=widget is using the default line styler to store line styles
	boolean userLineBackground = false;	// true=widget is using a user defined line background listener for line backgrounds. false=widget is using the default line styler to store line backgrounds
	int verticalScrollOffset = 0;		// pixel based
	int horizontalScrollOffset = 0;		// pixel based
	int topIndex = 0;					// top visible line
	int clientAreaHeight = 0;			// the client area height. Needed to calculate content width for new 
										// visible lines during Resize callback
	int clientAreaWidth = 0;			// the client area width. Needed during Resize callback to determine 
										// if line wrap needs to be recalculated
	int lineHeight;						// line height=font height
	int tabLength = 4;					// number of characters in a tab
	int lineEndSpaceWidth;				// space, in pixel, used to indicated a selected line break
	int leftMargin = 1;
	int topMargin = 1;
	int rightMargin = 2;
	int bottomMargin = 2;
	Cursor ibeamCursor;		
	int caretOffset = 0;
	int caretLine = 0;
	Point selection = new Point(0, 0);	// x is character offset, y is length
	int selectionAnchor;				// position of selection anchor. 0 based offset from beginning of text
	boolean editable = true;
	boolean wordWrap = false;
	boolean doubleClickEnabled = true;	// see getDoubleClickEnabled 
	boolean overwrite = false;			// insert/overwrite edit mode
	int textLimit = -1;					// limits the number of characters the user can type in the widget. Unlimited by default.
	Hashtable keyActionMap = new Hashtable();
	Color background = null;			// workaround for bug 4791
	Color foreground = null;			//
	Clipboard clipboard;
	boolean mouseDoubleClick = false;	// true=a double click ocurred. Don't do mouse swipe selection.
	int autoScrollDirection = SWT.NULL;	// the direction of autoscrolling (up, down, right, left)
	int lastTextChangeStart;			// cache data of the 
	int lastTextChangeNewLineCount;		// last text changing 
	int lastTextChangeNewCharCount;		// event for use in the 
	int lastTextChangeReplaceLineCount;	// text changed handler
	int lastTextChangeReplaceCharCount;	
	boolean isBidi;
	boolean bidiColoring = false;		// apply the BIDI algorithm on text segments of the same color
	Image leftCaretBitmap = null;
	Image rightCaretBitmap = null;
	int caretDirection = SWT.NULL;
	PaletteData caretPalette = null;	
	int lastCaretDirection = SWT.NULL;
	
	/**
	 * The Printing class implements printing of a range of text.
	 * An instance of <class>Printing </class> is returned in the 
	 * StyledText#print(Printer) API. The run() method may be 
	 * invoked from any thread.
	 */
	class Printing implements Runnable {
		Printer printer;
		PrintRenderer renderer;
		StyledTextContent printerContent;				// copy of the widget content
		Rectangle clientArea;							// client area to print on
		Font printerFont;
		FontData displayFontData;
		Hashtable printerColors;						// printer color cache for line backgrounds and style
		Hashtable lineBackgrounds = new Hashtable();	// cached line backgrounds
		Hashtable lineStyles = new Hashtable();			// cached line styles
		Hashtable bidiSegments = new Hashtable();		// cached bidi segments when running on a bidi platform
		GC gc;											// printer GC
		int startPage;									// first page to print
		int endPage;									// last page to print
		int pageSize;									// number of lines on a page
		int startLine;									// first (wrapped) line to print
		int endLine;									// last (wrapped) line to print
		boolean singleLine;								// widget single line mode

	/**
	 * Creates an instance of <class>Printing</class>.
	 * Copies the widget content and rendering data that needs 
	 * to be requested from listeners.
	 * </p>
	 * @param parent StyledText widget to print.
	 * @param printer printer device to print on.
	 */		
	Printing(StyledText parent, Printer printer) {
		PrinterData data = printer.getPrinterData();
		
		this.printer = printer;
		singleLine = parent.isSingleLine();
		startPage = 1;
		endPage = Integer.MAX_VALUE;
		if (data.scope == PrinterData.PAGE_RANGE) {
			startPage = data.startPage;
			endPage = data.endPage;
		}	
		displayFontData = getFont().getFontData()[0];
		copyContent(parent.getContent());
		cacheLineData(printerContent);
	}
	/**
	 * Caches the bidi segments of the given line.
	 * </p>
	 * @param lineOffset offset of the line to cache bidi segments for. 
	 * 	Relative to the start of the document.
	 * @param line line to cache bidi segments for. 
	 */
	void cacheBidiSegments(int lineOffset, String line) {
		int[] segments = getBidiSegments(lineOffset, line);
		
		if (segments != null) {
			bidiSegments.put(new Integer(lineOffset), segments);
		}
	}
	/**
	 * Caches the line background color of the given line.
	 * </p>
	 * @param lineOffset offset of the line to cache the background 
	 * 	color for. Relative to the start of the document.
	 * @param line line to cache the background color for
	 */
	void cacheLineBackground(int lineOffset, String line) {
		StyledTextEvent event = getLineBackgroundData(lineOffset, line);
		
		if (event != null) {
			lineBackgrounds.put(new Integer(lineOffset), event);
		}
	}
	/**
	 * Caches all line data that needs to be requested from a listener.
	 * </p>
	 * @param printerContent <class>StyledTextContent</class> to request 
	 * 	line data for.
	 */
	void cacheLineData(StyledTextContent printerContent) {	
		for (int i = 0; i < printerContent.getLineCount(); i++) {
			int lineOffset = printerContent.getOffsetAtLine(i);
			String line = printerContent.getLine(i);
	
			cacheLineBackground(lineOffset, line);
			cacheLineStyle(lineOffset, line);
			if (isBidi()) {
				cacheBidiSegments(lineOffset, line);
			}
		}
	}
	/**
	 * Caches all line styles of the given line.
	 * </p>
	 * @param lineOffset offset of the line to cache the styles for.
	 * 	Relative to the start of the document.
	 * @param line line to cache the styles for.
	 */
	void cacheLineStyle(int lineOffset, String line) {
		StyledTextEvent event = getLineStyleData(lineOffset, line);
		
		if (event != null) {
			lineStyles.put(new Integer(lineOffset), event);
		}
	}
	/**
	 * Copies the text of the specified <class>StyledTextContent</class>.
	 * </p>
	 * @param original the <class>StyledTextContent</class> to copy.
	 */
	void copyContent(StyledTextContent original) {
		int lineCount = original.getLineCount();
		int insertOffset = 0;
		printerContent = new DefaultContent();
		for (int i = 0; i < original.getLineCount(); i++) {
			int insertEndOffset;
			if (i < original.getLineCount() - 1) {
				insertEndOffset = original.getOffsetAtLine(i + 1);
			}
			else {
				insertEndOffset = original.getCharCount();
			}
			printerContent.replaceTextRange(insertOffset, 0, original.getTextRange(insertOffset, insertEndOffset - insertOffset));
			insertOffset = insertEndOffset;
		}
	}
	/**
	 * Disposes of the resources and the <class>PrintRenderer</class>.
	 */
	void dispose() {
		if (printerColors != null) {
			Enumeration colors = printerColors.elements();
			
			while (colors.hasMoreElements()) {
				Color color = (Color) colors.nextElement();
				color.dispose();
			}
			printerColors = null;
		}
		if (gc != null) {
			gc.dispose();
			gc = null;
		}
		if (printerFont != null) {
			printerFont.dispose();
			printerFont = null;
		}
		if (renderer != null) {
			renderer.dispose();
			renderer = null;
		}
	}
	/**
	 * Creates a <class>PrintRenderer</class> and calculate the line range
	 * to print.
	 */
	void initializeRenderer() {
		Rectangle trim = printer.computeTrim(0, 0, 0, 0);
		Point dpi = printer.getDPI();
		
		printerFont = new Font(printer, displayFontData.getName(), displayFontData.getHeight(), SWT.NORMAL);
		clientArea = printer.getClientArea();
		// one inch margin around text
		clientArea.x = dpi.x + trim.x; 				
		clientArea.y = dpi.y + trim.y;
		clientArea.width -= (clientArea.x + trim.width);
		clientArea.height -= (clientArea.y + trim.height);
		
		gc = new GC(printer);
		gc.setFont(printerFont);				
		renderer = new PrintRenderer(
			printer, printerFont, isBidi(), gc, printerContent,
			lineBackgrounds, lineStyles, bidiSegments, 
			tabLength, clientArea);
		pageSize = clientArea.height / renderer.getLineHeight();
		startLine = (startPage - 1) * pageSize;
		endLine = Math.max(Integer.MAX_VALUE, endPage * pageSize);
	}
	/**
	 * Returns the printer color for the given display color.
	 * </p>
	 * @param color display color
	 * @return color create on the printer with the same RGB values 
	 * 	as the display color.
 	 */
	Color getPrinterColor(Color color) {
		Color printerColor = null;
		
		if (color != null) {
			printerColor = (Color) printerColors.get(color);		
			if (printerColor == null) {
				printerColor = new Color(printer, color.getRGB());
				printerColors.put(color, printerColor);
			}
		}
		return printerColor;
	}
	/**
	 * Replaces all display colors in the cached line backgrounds and 
	 * line styles with printer colors.
	 */
	void createPrinterColors() {
		Enumeration values = lineBackgrounds.elements();
		printerColors = new Hashtable();
		while (values.hasMoreElements()) {
			StyledTextEvent event = (StyledTextEvent) values.nextElement();
			event.lineBackground = getPrinterColor(event.lineBackground);
		}
		
		values = lineStyles.elements();
		while (values.hasMoreElements()) {
			StyledTextEvent event = (StyledTextEvent) values.nextElement();
			for (int i = 0; i < event.styles.length; i++) {
				StyleRange style = event.styles[i];
				Color printerBackground = getPrinterColor(style.background);
				Color printerForeground = getPrinterColor(style.foreground);
				
				if (printerBackground != style.background || 
					printerForeground != style.foreground) {
					style = (StyleRange) style.clone();
					style.background = printerBackground;
					style.foreground = printerForeground;
					event.styles[i] = style;
				}
			}
		}		
	}
	/**
	 * Starts a print job and prints the pages specified in the constructor.
	 */
	public void run() {
		if (printer.startJob("Printing")) {
			createPrinterColors();
			initializeRenderer();
			print();
			dispose();
			printer.endJob();			
		}
	}
	/**
	 * Prints the lines in the specified page range.
	 */
	void print() {
		StyledTextContent content = renderer.getContent();
		FontData printerFontData = gc.getFont().getFontData()[0];
		Color background = gc.getBackground();
		Color foreground = gc.getForeground();
		int lineHeight = renderer.getLineHeight();
		int lineCount = content.getLineCount();
		int paintY = clientArea.y;
		
		if (singleLine) {
			lineCount = 1;
		}
		if (startPage == 1) {
			printer.startPage();
		}			
		for (int i = startLine; i < lineCount && i < endLine; i++, paintY += lineHeight) {
			String line = content.getLine(i);
			
			if (paintY + lineHeight > clientArea.y + clientArea.height) {
				printer.endPage();
				printer.startPage();
				paintY = clientArea.y;
			}
			renderer.drawLine(
				line, i, paintY, gc, background, foreground, printerFontData, true);
		}
		if (paintY > clientArea.y && paintY <= clientArea.y + clientArea.height) {
			printer.endPage();
		}
	}	
	}
	/**
	 * The <code>RTFWriter</code> class is used to write widget content as
	 * rich text. The implementation complies with the RTF specification 
	 * version 1.5.
	 * <p>
	 * toString() is guaranteed to return a valid RTF string only after 
	 * close() has been called. 
	 * </p>
	 * <p>
	 * Whole and partial lines and line breaks can be written. Lines will be
	 * formatted using the styles queried from the LineStyleListener, if 
	 * set, or those set directly in the widget. All styles are applied to
	 * the RTF stream like they are rendered by the widget. In addition, the 
	 * widget font name and size is used for the whole text.
	 * </p>
	 */
	class RTFWriter extends TextWriter {
		final int DEFAULT_FOREGROUND = 0;
		final int DEFAULT_BACKGROUND = 1;
		Vector colorTable = new Vector();
	
	/**
	 * Creates a RTF writer that writes content starting at offset "start"
	 * in the document.  <code>start</code> and <code>length</code>can be set to specify partial 
	 * lines.
	 * <p>
	 *
	 * @param start start offset of content to write, 0 based from 
	 * 	beginning of document
	 * @param length length of content to write
	 */
	public RTFWriter(int start, int length) {
		super(start, length);
		colorTable.addElement(getForeground());
		colorTable.addElement(getBackground());
	}
	/**
	 * Closes the RTF writer. Once closed no more content can be written.
	 * <b>NOTE:</b>  <code>toString()</code> does not return a valid RTF string until 
	 * <code>close()</code> has been called.
	 */
	public void close() {
		if (isClosed() == false) {
			writeHeader();
			write("\n}}\0");
			super.close();
		}
	}	
	/**
	 * Returns the index of the specified color in the RTF color table.
	 * <p>
	 *
	 * @param color the color
	 * @param defaultIndex return value if color is null
	 * @return the index of the specified color in the RTF color table
	 * 	or "defaultIndex" if "color" is null.
	 */
	int getColorIndex(Color color, int defaultIndex) {
		int index;
		
		if (color == null) {
			index = defaultIndex;
		}
		else {		
			index = colorTable.indexOf(color);
			if (index == -1) {
				index = colorTable.size();
				colorTable.addElement(color);
			}
		}
		return index;
	}
	/**
	 * Writes the RTF header including font table and color table.
	 */
	void writeHeader() {
		StringBuffer header = new StringBuffer();
		FontData fontData = getFont().getFontData()[0];
		header.append("{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0\\fnil");
		// specify code page, necessary for copy to work in bidi 
		// systems
		String cpg = System.getProperty("file.encoding");
		if (cpg.startsWith("Cp") || cpg.startsWith("MS")) {
			cpg = cpg.substring(2, cpg.length());
			header.append("\\cpg");
			header.append(cpg);
		}
		header.append(" ");
		header.append(fontData.getName());
		header.append(";}}\n{\\colortbl");
		for (int i = 0; i < colorTable.size(); i++) {
			Color color = (Color) colorTable.elementAt(i);
			header.append("\\red");
			header.append(color.getRed());
			header.append("\\green");
			header.append(color.getGreen());
			header.append("\\blue");
			header.append(color.getBlue());
			header.append(";");
		} 
		// some RTF readers ignore the deff0 font tag. Explicitly 
		// set the font for the whole document to work around this.
		header.append("}\n{\\f0\\fs");
		// font size is specified in half points
		header.append(fontData.getHeight() * 2);
		header.append(" ");
		write(header.toString(), 0);
	}
	/**
	 * Appends the specified line text to the RTF data.  Lines will be formatted 
	 * using the styles queried from the LineStyleListener, if set, or those set 
	 * directly in the widget.
	 * <p>
	 *
	 * @param line line text to write as RTF. Must not contain line breaks
	 * 	Line breaks should be written using writeLineDelimiter()
	 * @param lineOffset offset of the line. 0 based from the start of the 
	 * 	widget document. Any text occurring before the start offset or after the 
	 * 	end offset specified during object creation is ignored.
	 * @exception SWTException <ul>
	 *   <li>ERROR_IO when the writer is closed.</li>
	 * </ul>
	 */
	public void writeLine(String line, int lineOffset) {
		StyleRange[] styles = new StyleRange[0];
		Color lineBackground = null;
		StyledTextEvent event;
		
		if (isClosed()) {
			SWT.error(SWT.ERROR_IO);
		}
		event = renderer.getLineStyleData(lineOffset, line);
		if (event != null) {
			styles = event.styles;
		}
		event = renderer.getLineBackgroundData(lineOffset, line);
		if (event != null) {
			lineBackground = event.lineBackground;
		}
		if (lineBackground == null) {
			lineBackground = getBackground();
		}
		writeStyledLine(line, lineOffset, styles, lineBackground);
	}
	/**
	 * Appends the specified line delmimiter to the RTF data.
	 * <p>
	 *
	 * @param lineDelimiter line delimiter to write as RTF.
	 * @exception SWTException <ul>
	 *   <li>ERROR_IO when the writer is closed.</li>
	 * </ul>
	 */
	public void writeLineDelimiter(String lineDelimiter) {
		if (isClosed()) {
			SWT.error(SWT.ERROR_IO);
		}
		write(lineDelimiter, 0, lineDelimiter.length());
		write("\\par ");
	}
	/**
	 * Appends the specified segment of "string" to the RTF data.
	 * Copy from <code>start</code> up to, but excluding, <code>end</code>.
	 * <p>
	 *
	 * @param string string to copy a segment from. Must not contain
	 * 	line breaks. Line breaks should be written using writeLineDelimiter()
	 * @param start start offset of segment. 0 based.
	 * @param end end offset of segment
	 */
	void write(String string, int start, int end) {
		int index;
		
		for (index = start; index < end; index++) {
			char c = string.charAt(index);
			if (c == '}' || c == '{' || c == '\\') {
				break;
			}
		}
		if (index == end) {
			write(string.substring(start, end));	// string doesn't contain RTF formatting characters, write as is
		}
		else {										// string needs to be transformed
			char[] text = new char[end - start];
			
			string.getChars(start, end, text, 0);
			for (index = 0; index < text.length; index++) {
				switch (text[index]) {
					case '}':
					case '{':
					case '\\':
						write("\\");
					default:
						write(text[index]);
				}			
			}			
		}
	}	
	/**
	 * Appends the specified line text to the RTF data.
	 * Use the colors and font styles specified in "styles" and "lineBackground".
	 * Formatting is written to reflect the text rendering by the text widget.
	 * Style background colors take precedence over the line background color.
	 * Background colors are written using the \highlight tag (vs. the \cb tag).
	 * <p>
	 *
	 * @param line line text to write as RTF. Must not contain line breaks
	 * 	Line breaks should be written using writeLineDelimiter()
	 * @param lineOffset offset of the line. 0 based from the start of the 
	 * 	widget document. Any text occurring before the start offset or after the 
	 * 	end offset specified during object creation is ignored.
	 * @param styles styles to use for formatting. Must not be null.
	 * @param linebackground line background color to use for formatting. 
	 * 	May be null.
	 */
	void writeStyledLine(String line, int lineOffset, StyleRange[] styles, Color lineBackground) {
		int lineLength = line.length();
		int lineIndex;
		int copyEnd;
		int startOffset = getStart();		
		int endOffset = startOffset + super.getCharCount();
		int writeOffset = startOffset - lineOffset;
		
		if (writeOffset >= line.length()) {
			return;					// whole line is outside write range
		}
		else
		if (writeOffset > 0) {
			lineIndex = writeOffset;		// line starts before RTF write start
		}
		else {
			lineIndex = 0;
		}
		if (lineBackground != null) {
			write("{\\highlight");
			write(getColorIndex(lineBackground, DEFAULT_BACKGROUND));
			write(" "); 
		}
		for (int i = 0; i < styles.length; i++) {		
			StyleRange style = styles[i];
			int start = style.start - lineOffset;
			int end = start + style.length;
			int colorIndex;
			// skip over partial first line
			if (end < writeOffset) {
				continue;
			}
			// break on partial last line
			if (style.start > endOffset) {
				break;
			}
			// write any unstyled text
			if (lineIndex < start) { 
				// copy to start of style or end of write range (specified 
				// during object creation) or end of line
				copyEnd = Math.min(start, endOffset - lineOffset);
				copyEnd = Math.min(copyEnd, lineLength);
				write(line, lineIndex, copyEnd);
				lineIndex = copyEnd;
				if (copyEnd != start) {
					break;
				}
			}
			// write styled text
			colorIndex = getColorIndex(style.background, DEFAULT_BACKGROUND);
			write("{\\cf");
			write(getColorIndex(style.foreground, DEFAULT_FOREGROUND));
			if (colorIndex != DEFAULT_BACKGROUND) {
				write("\\highlight");
				write(colorIndex);
			}
			if (style.fontStyle == SWT.BOLD) {
				write("\\b"); 
			}
			write(" "); 
			// copy to end of style or end of write range (specified 
			// during object creation) or end of line
			copyEnd = Math.min(end, endOffset - lineOffset);
			copyEnd = Math.min(copyEnd, lineLength);
			write(line, lineIndex, copyEnd);
			if (style.fontStyle == SWT.BOLD) {
				write("\\b0"); 
			}
			write("}");
			lineIndex = copyEnd;
			if (copyEnd != end) {
				break;
			}
		}
		copyEnd = Math.min(lineLength, endOffset - lineOffset);
		if (lineIndex < copyEnd) {
			write(line, lineIndex, copyEnd);
		}		
		if (lineBackground != null) {
			write("}");
		}
	}
	}
	/**
	 * The <code>TextWriter</code> class is used to write widget content to
	 * a string.  Whole and partial lines and line breaks can be written. To write 
	 * partial lines, specify the start and length of the desired segment 
	 * during object creation.
	 * <p>
	 * </b>NOTE:</b> <code>toString()</code> is guaranteed to return a valid string only after close() 
	 * has been called. 
	 */
	class TextWriter {
		private StringBuffer buffer;
		private int startOffset;	// offset of first character that will be written
		private int endOffset;		// offset of last character that will be written. 
									// 0 based from the beginning of the widget text. 
		private boolean isClosed = false;
	
	/**
	 * Creates a writer that writes content starting at offset "start"
	 * in the document.  <code>start</code> and <code>length</code> can be set to specify partial lines.
	 * <p>
	 *
	 * @param start start offset of content to write, 0 based from beginning of document
	 * @param length length of content to write
	 */
	public TextWriter(int start, int length) {
		buffer = new StringBuffer(length);
		startOffset = start;
		endOffset = start + length;
	}
	/**
	 * Closes the writer. Once closed no more content can be written.
	 * <b>NOTE:</b>  <code>toString()</code> is not guaranteed to return a valid string unless
	 * the writer is closed.
	 */
	public void close() {
		if (isClosed == false) {
			isClosed = true;
		}
	}
	/** 
	 * Returns the number of characters to write.
	 */
	public int getCharCount() {
		return endOffset - startOffset;
	}	
	/** 
	 * Returns the offset where writing starts. 0 based from the start of 
	 * the widget text. Used to write partial lines.
	 */
	public int getStart() {
		return startOffset;
	}
	/**
	 * Returns whether the writer is closed.
	 */
	public boolean isClosed() {
		return isClosed;
	}
	/**
	 * Returns the string.  <code>close()</code> must be called before <code>toString()</code> 
	 * is guaranteed to return a valid string.
	 * <p>
	 *
	 * @return the string
	 */
	public String toString() {
		return buffer.toString();
	}
	/**
	 * Appends the given string to the data.
	 */
	void write(String string) {
		buffer.append(string);
	}	
	/**
	 * Inserts the given string to the data at the specified offset.
	 * Do nothing if "offset" is < 0 or > getCharCount()
	 * <p>
	 *
	 * @param string text to insert
	 * @param offset offset in the existing data to insert "string" at.
	 */
	void write(String string, int offset) {
		if (offset < 0 || offset > buffer.length()) {
			return;
		}
		buffer.insert(offset, string);
	}	
	/**
	 * Appends the given int to the data.
	 */
	void write(int i) {
		buffer.append(i);
	}
	/**
	 * Appends the given character to the data.
	 */
	void write(char i) {
		buffer.append(i);
	}			
	/**
	 * Appends the specified line text to the data.
	 * <p>
	 *
	 * @param line line text to write. Must not contain line breaks
	 * 	Line breaks should be written using writeLineDelimiter()
	 * @param lineOffset offset of the line. 0 based from the start of the 
	 * 	widget document. Any text occurring before the start offset or after the 
	 *	end offset specified during object creation is ignored.
	 * @exception SWTException <ul>
	 *   <li>ERROR_IO when the writer is closed.</li>
	 * </ul>
	 */
	public void writeLine(String line, int lineOffset) {
		int lineLength = line.length();
		int lineIndex;
		int copyEnd;
		int writeOffset = startOffset - lineOffset;
		
		if (isClosed) {
			SWT.error(SWT.ERROR_IO);
		}		
		if (writeOffset >= lineLength) {
			return;							// whole line is outside write range
		}
		else
		if (writeOffset > 0) {
			lineIndex = writeOffset;		// line starts before write start
		}
		else {
			lineIndex = 0;
		}
		copyEnd = Math.min(lineLength, endOffset - lineOffset);
		if (lineIndex < copyEnd) {
			write(line.substring(lineIndex, copyEnd));
		}		
	}
	/**
	 * Appends the specified line delmimiter to the data.
	 * <p>
	 *
	 * @param lineDelimiter line delimiter to write
	 * @exception SWTException <ul>
	 *   <li>ERROR_IO when the writer is closed.</li>
	 * </ul>
	 */
	public void writeLineDelimiter(String lineDelimiter) {
		if (isClosed) {
			SWT.error(SWT.ERROR_IO);
		}
		write(lineDelimiter);
	}
	}
	/**
	 * LineCache provides an interface to calculate and invalidate 
	 * line based data.
	 * Implementors need to return a line width in <code>getWidth</code>.
	 */
	interface LineCache {
	/**
	 * Calculates the lines in the specified range.
	 * <p>
	 * 
	 * @param startLine first line to calculate
	 * @param lineCount number of lines to calculate
	 */
	public void calculate(int startLine, int lineCount);
	/**
	 * Returns a width that will be used by the <code>StyledText</code> 
	 * widget to size a horizontal scroll bar.
	 * <p>
	 *
	 * @return the line width
	 */
	public int getWidth();
	/**
	 * Resets the lines in the specified range.
	 * This method is called in <code>StyledText.redraw()</code>
	 * and allows implementors to call redraw themselves during reset.
	 * <p>
	 *
	 * @param startLine the first line to reset
	 * @param lineCount the number of lines to reset
	 * @param calculateMaxWidth true=implementors should retain a 
	 * 	valid width even if it is affected by the reset operation.
	 * 	false=the width may be set to 0
	 */
	public void redrawReset(int startLine, int lineCount, boolean calculateMaxWidth);
	/**
	 * Resets the lines in the specified range.
	 * <p>
	 *
	 * @param startLine the first line to reset
	 * @param lineCount the number of lines to reset
	 * @param calculateMaxWidth true=implementors should retain a 
	 * 	valid width even if it is affected by the reset operation.
	 * 	false=the width may be set to 0
	 */
	public void reset(int startLine, int lineCount, boolean calculateMaxWidth);
	/** 
	 * Called when a text change occurred.
	 * <p>
	 *
	 * @param startOffset	the start offset of the text change
	 * @param newLineCount the number of inserted lines
	 * @param replaceLineCount the number of deleted lines
	 * @param newCharCount the number of new characters
	 * @param replaceCharCount the number of deleted characters
	 */  
	public void textChanged(int startOffset, int newLineCount, int replaceLineCount, int newCharCount, int replaceCharCount);
	}
	/**
	 * Keeps track of line widths and the longest line in the 
	 * StyledText document.
	 * Line widths are calculated when requested by a call to 
	 * <code>calculate</code> and cached until reset by a call 
	 * to <code>redrawReset</code> or <code>reset</code>.
	 */
	class ContentWidthCache implements LineCache {
		StyledText parent;				// parent widget, used to create a GC for line measuring
		int[] lineWidth;				// width in pixel of each line in the document, -1 for unknown width
		int lineCount;					// number of lines in lineWidth array
		int maxWidth;					// maximum line width of all measured lines
		int maxWidthLineIndex;			// index of the widest line
				
	/** 
	 * Creates a new <code>ContentWidthCache</code> and allocates space 
	 * for the given number of lines.
	 * <p>
	 *
	 * @param parent the StyledText widget used to create a GC for 
	 * 	line measuring
	 * @param lineCount initial number of lines to allocate space for
	 */
	public ContentWidthCache(StyledText parent, int lineCount) {
		this.lineCount = lineCount;
		this.parent = parent;
		lineWidth = new int[lineCount];
		reset(0, lineCount, false);
	}
	/**
	 * Calculates the width of each line in the given range if it has
	 * not been calculated yet.
	 * If any line in the given range is wider than the currently widest
	 * line, the maximum line width is updated,
	 * <p>
	 * 
	 * @param startLine first line to calculate the line width of
	 * @param lineCount number of lines to calculate the line width for
	 */
	public void calculate(int startLine, int lineCount) {
		GC gc = null;
		FontData currentFont = null;
		int caretWidth = 0;
		int stopLine = startLine + lineCount;
			
		for (int i = startLine; i < stopLine; i++) {
			if (lineWidth[i] == -1) {
				String line = content.getLine(i);
				int lineOffset = content.getOffsetAtLine(i);
		
				if (gc == null) {
					gc = new GC(parent);
					caretWidth = getCaretWidth();
					if (isBidi() == false) {
						currentFont = gc.getFont().getFontData()[0];
					}
				}		
				lineWidth[i] = contentWidth(line, lineOffset, gc, currentFont) + caretWidth;
			}
			if (lineWidth[i] > maxWidth) {
				maxWidth = lineWidth[i];
				maxWidthLineIndex = i;
			}
		}
		if (gc != null) {
			gc.dispose();	
		}
	}
	/** 
	 * Calculates the width of the visible lines in the specified 
	 * range.
	 * <p>
	 *
	 * @param startLine	the first changed line
	 * @param newLineCount the number of inserted lines
	 */  
	void calculateVisible(int startLine, int newLineCount) {
		int topIndex = parent.getTopIndex();
		int bottomLine = Math.min(getPartialBottomIndex(), startLine + newLineCount);
		
		startLine = Math.max(startLine, topIndex);
		calculate(startLine, bottomLine - startLine + 1);
	}
	/**
	 * Measures the width of the given line.
	 * <p>
	 * 
	 * @param line the line to measure
	 * @param lineOffset start offset of the line to measure, relative 
	 * 	to the start of the document
	 * @param gc the GC to use for measuring the line
	 * @param currentFont the font currently set in gc. Cached for better 
	 * 	performance. Null when running in a bidi locale.
	 * @return the width of the given line
	 */
	int contentWidth(String line, int lineOffset, GC gc, FontData currentFont) {
		int width;
		
		if (isBidi()) {
			StyledTextBidi bidi = getStyledTextBidi(line, lineOffset, gc);
			width = bidi.getTextWidth();
		}
		else {
			StyledTextEvent event = renderer.getLineStyleData(lineOffset, line);
			StyleRange[] styles = null;
			if (event != null) {
				styles = renderer.filterLineStyles(event.styles);
			}
			width = renderer.textWidth(line, lineOffset, 0, line.length(), styles, 0, gc, currentFont);
		}
		return width + leftMargin;
	}
	/**
	 * Grows the <code>lineWidth</code> array to accomodate new line width
	 * information.
	 * <p>
	 *
	 * @param numLines the number of elements to increase the array by
	 */
	void expandLines(int numLines) {
		int size = lineWidth.length;
		if (size - lineCount >= numLines) {
			return;
		}
		int[] newLines = new int[Math.max(size * 2, size + numLines)];
		System.arraycopy(lineWidth, 0, newLines, 0, size);
		lineWidth = newLines;
		reset(size, lineWidth.length - size, false);
	}
	/**
	 * Returns the width of the longest measured line.
	 * <p>
	 *
	 * @return the width of the longest measured line.
	 */
	public int getWidth() {
		return maxWidth;
	}
	/**
	 * Updates the line width array to reflect inserted or deleted lines.
	 * <p>
	 *
	 * @param start	the starting line of the change that took place
	 * @param delta	the number of lines in the change, > 0 indicates lines inserted,
	 * 	< 0 indicates lines deleted
	 */
	void linesChanged(int startLine, int delta) {
		boolean inserting = delta > 0;
		
		if (delta == 0) {
			return;
		}
		if (inserting) {
			// shift the lines down to make room for new lines
			expandLines(delta);
			for (int i = lineCount - 1; i >= startLine; i--) {
				lineWidth[i + delta] = lineWidth[i];
			}
			// reset the new lines
			for (int i = startLine + 1; i <= startLine + delta && i < lineWidth.length; i++) {
				lineWidth[i] = -1;
			}
			// have new lines been inserted above the longest line?
			if (maxWidthLineIndex >= startLine) {
				maxWidthLineIndex += delta;
			}
		} 
		else {
			// shift up the lines
			for (int i = startLine - delta; i < lineCount; i++) {
				lineWidth[i+delta] = lineWidth[i];
			}
			// has the longest line been removed?
			if (maxWidthLineIndex > startLine && maxWidthLineIndex <= startLine - delta) {
				maxWidth = 0;
				maxWidthLineIndex = -1;
			}
			else
			if (maxWidthLineIndex >= startLine - delta) {
				maxWidthLineIndex += delta;
			}
		}
		lineCount += delta;
	}
	/**
	 * Resets the line width of the lines in the specified range.
	 * <p>
	 *
	 * @param startLine	the first line to reset
	 * @param lineCount the number of lines to reset
	 * @param calculateMaxWidth true=if the widest line is being 
	 * 	reset the maximum width of all remaining cached lines is 
	 * 	calculated. false=the maximum width is set to 0 if the 
	 * 	widest line is being reset.
	 */
	public void redrawReset(int startLine, int lineCount, boolean calculateMaxWidth) {
		reset(startLine, lineCount, calculateMaxWidth);
	}
	/**
	 * Resets the line width of the lines in the specified range.
	 * <p>
	 *
	 * @param startLine	the first line to reset
	 * @param lineCount the number of lines to reset
	 * @param calculateMaxWidth true=if the widest line is being 
	 * 	reset the maximum width of all remaining cached lines is 
	 * 	calculated. false=the maximum width is set to 0 if the 
	 * 	widest line is being reset.
	 */
	public void reset(int startLine, int lineCount, boolean calculateMaxWidth) {
		int endLine = startLine + lineCount;
		
		if (startLine < 0 || endLine > lineWidth.length) {
			return;
		}
		for (int i = startLine; i < endLine; i++) {
			lineWidth[i] = -1;
		}		
		// if the longest line is one of the reset lines, the maximum line 
		// width is no longer valid
		if (maxWidthLineIndex >= startLine && maxWidthLineIndex < endLine) {
			maxWidth = 0;
			maxWidthLineIndex = -1;
			if (calculateMaxWidth) {
				for (int i = 0; i < lineCount; i++) {
					if (lineWidth[i] > maxWidth) {
						maxWidth = lineWidth[i];
						maxWidthLineIndex = i;
					}
				}			
			}
		}
	}
	/** 
	 * Updates the line width array to reflect a text change.
	 * Lines affected by the text change will be reset.
	 * <p>
	 *
	 * @param startOffset	the start offset of the text change
	 * @param newLineCount the number of inserted lines
	 * @param replaceLineCount the number of deleted lines
	 * @param newCharCount the number of new characters
	 * @param replaceCharCount the number of deleted characters
	 */  
	public void textChanged(int startOffset, int newLineCount, int replaceLineCount, int newCharCount, int replaceCharCount) {
		int startLine = parent.getLineAtOffset(startOffset);
		boolean removedMaxLine = (maxWidthLineIndex > startLine && maxWidthLineIndex <= startLine + replaceLineCount);
		// entire text deleted?
		if (startLine == 0 && replaceLineCount == lineCount) {
			lineCount = newLineCount;
			lineWidth = new int[lineCount];
			reset(0, lineCount, false);
			maxWidth = 0;
		}
		else {
			linesChanged(startLine, -replaceLineCount);
			linesChanged(startLine, newLineCount);
			lineWidth[startLine] = -1;
		}
		// only calculate the visible lines. otherwise measurements of changed lines 
		// outside the visible area may subsequently change again without the 
		// lines ever being visible.
		calculateVisible(startLine, newLineCount);
		// maxWidthLineIndex will be -1 (i.e., unknown line width) if the widget has 
		// not been visible yet and the changed lines have therefore not been
		// calculated above.
		if (removedMaxLine || 
			(maxWidthLineIndex != -1 && lineWidth[maxWidthLineIndex] < maxWidth)) {
			// longest line has been removed or changed and is now shorter.
			// need to recalculate maximum content width for all lines
			maxWidth = 0;
			for (int i = 0; i < lineCount; i++) {
				if (lineWidth[i] > maxWidth) {
					maxWidth = lineWidth[i];
					maxWidthLineIndex = i;
				}
			}			
		}
	}
	}
	/**
	 * Updates the line wrapping of the content.
	 * The line wrapping must always be in a consistent state. 
	 * Therefore, when <code>reset</code> or <code>redrawReset</code>
	 * is called, the line wrapping is recalculated immediately 
	 * instead of in <code>calculate</code>.
	 */
	class WordWrapCache implements LineCache {
		StyledText parent;
		WrappedContent visualContent;
				
	/** 
	 * Creates a new <code>WordWrapCache</code> and calculates an initial
	 * line wrapping.
	 * <p>
	 *
	 * @param parent the StyledText widget to wrap content in.
	 * @param content the content provider that does the actual line wrapping.
	 */
	public WordWrapCache(StyledText parent, WrappedContent content) {
		this.parent = parent;
		visualContent = content;
		visualContent.wrapLines();
	}
	/**
	 * Do nothing. Lines are wrapped immediately after reset.
	 * <p>
	 * 
	 * @param startLine first line to calculate
	 * @param lineCount number of lines to calculate
	 */
	public void calculate(int startLine, int lineCount) {
	}
	/**
	 * Returns the client area width. Lines are wrapped so there
	 * is no horizontal scroll bar.
	 * <p>
	 *
	 * @return the line width
	 */
	public int getWidth() {
		return parent.getClientArea().width;
	}
	/**
	 * Wraps the lines in the specified range.
	 * This method is called in <code>StyledText.redraw()</code>.
	 * A redraw is therefore not necessary.
	 * <p>
	 *
	 * @param startLine the first line to reset
	 * @param lineCount the number of lines to reset
	 * @param calculateMaxWidth true=implementors should retain a 
	 * 	valid width even if it is affected by the reset operation.
	 * 	false=the width may be set to 0
	 */
	public void redrawReset(int startLine, int lineCount, boolean calculateMaxWidth) {
	    if (lineCount == visualContent.getLineCount()) {
			// do a full rewrap if all lines are reset
			visualContent.wrapLines();
	    }
	    else {
		    visualContent.reset(startLine, lineCount);
	    }
	}
	/**
	 * Rewraps the lines in the specified range and redraws
	 * the widget if the line wrapping has changed.
	 * <p>
	 *
	 * @param startLine the first line to reset
	 * @param lineCount the number of lines to reset
	 * @param calculateMaxWidth true=implementors should retain a 
	 * 	valid width even if it is affected by the reset operation.
	 * 	false=the width may be set to 0
	 */
	public void reset(int startLine, int lineCount, boolean calculateMaxWidth) {
		int endLine = startLine + lineCount;
		int itemCount = getPartialBottomIndex() - topIndex + 1;
	    int[] oldLineOffsets = new int[itemCount];
	    
	    for (int i = 0; i < itemCount; i++) {
	    	oldLineOffsets[i] = visualContent.getOffsetAtLine(i + topIndex);
	    }
	    redrawReset(startLine, lineCount, calculateMaxWidth);
		// check for cases which will require a full redraw
	    if (getPartialBottomIndex() - topIndex + 1 != itemCount) {
	    	// number of visible lines has changed
	    	parent.internalRedraw();
	    }
	    else {
		    for (int i = 0; i < itemCount; i++) {
		    	if (visualContent.getOffsetAtLine(i + topIndex) != oldLineOffsets[i]) {
		    		// wrapping of one of the visible lines has changed
		    		parent.internalRedraw();
		    		break;
		    	}
	    	}	    	
	    }
	}
	/** 
	 * Passes the text change notification to the line wrap content.
	 * <p>
	 *
	 * @param startOffset	the start offset of the text change
	 * @param newLineCount the number of inserted lines
	 * @param replaceLineCount the number of deleted lines
	 * @param newCharCount the number of new characters
	 * @param replaceCharCount the number of deleted characters
	 */  
	public void textChanged(int startOffset, int newLineCount, int replaceLineCount, int newCharCount, int replaceCharCount) {
		int startLine = visualContent.getLineAtOffset(startOffset);
		
		visualContent.textChanged(startOffset, newLineCount, replaceLineCount, newCharCount, replaceCharCount);
		if (startLine <= getPartialBottomIndex()) {
			// only redraw if the text change is inside or above the 
			// visible lines. if it is below the visible lines it will
			// not affect the word wrapping. fixes bug 14047.
			parent.internalRedraw();
		}
	}
	}

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a widget which will be the parent of the new instance (cannot be null)
 * @param style the style of widget to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 * </ul>
 *
 * @see SWT#FULL_SELECTION
 * @see SWT#MULTI
 * @see SWT#READ_ONLY
 * @see SWT#SINGLE
 * @see SWT#WRAP
 * @see #getStyle
 */
public StyledText(Composite parent, int style) {
	super(parent, checkStyle(style | SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND));
	Display display = getDisplay();
	isBidi = StyledTextBidi.isBidiPlatform();
	if ((style & SWT.READ_ONLY) != 0) {
		setEditable(false);
	}
	if ((style & SWT.BORDER) == 0 || (style & SWT.SINGLE) == 0) {
		leftMargin = topMargin = rightMargin = bottomMargin = 0;
	}
	clipboard = new Clipboard(display);
	installDefaultContent();
	initializeRenderer();
	if ((style & SWT.WRAP) != 0) {
		setWordWrap(true);
	}
	else {
	    lineCache = new ContentWidthCache(this, content.getLineCount());
	}	
	if (isBidi() == false) {
		Caret caret = new Caret(this, SWT.NULL);
		caret.setSize(1, caret.getSize().y);
	} 
	else {
		createCaretBitmaps();
		createBidiCaret();
		Runnable runnable = new Runnable() {
			public void run() {
				// setBidiCaretLocation calculates caret location like during 
				// cursor movement and takes keyboard language into account. 
				// Fixes 1GKPYMK
				setBidiCaretLocation(null);
			}
		};
		StyledTextBidi.addLanguageListener(this, runnable);
	}
	// set the caret width, the height of the caret will default to the line height
	calculateScrollBars();
	createKeyBindings();
	ibeamCursor = new Cursor(display, SWT.CURSOR_IBEAM);
	setCursor(ibeamCursor);
	installListeners();
	installDefaultLineStyler();
}
/**	 
 * Adds an extended modify listener. An ExtendedModify event is sent by the 
 * widget when the widget text has changed.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void addExtendedModifyListener(ExtendedModifyListener extendedModifyListener) {
	checkWidget();
	if (extendedModifyListener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	StyledTextListener typedListener = new StyledTextListener(extendedModifyListener);
	addListener(ExtendedModify, typedListener);
}
/** 
 * Maps a key to an action.
 * One action can be associated with N keys. However, each key can only 
 * have one action (key:action is N:1 relation).
 * <p>
 *
 * @param key a key code defined in SWT.java or a character. 
 * 	Optionally ORd with a state mask (one or more of SWT.CTRL, SWT.SHIFT, SWT.ALT)
 * @param action one of the predefined actions defined in ST.java. 
 * 	Use SWT.NULL to remove a key binding.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setKeyBinding(int key, int action) {
	checkWidget();
	if (action == SWT.NULL) {
		keyActionMap.remove(new Integer(key));
	}
	else {
	 	keyActionMap.put(new Integer(key), new Integer(action));
	}
}
/**
 * Adds a bidirectional segment listener. A BidiSegmentEvent is sent 
 * whenever a line of text is measured or rendered. The user can 
 * specify text ranges in the line that should be treated as if they 
 * had a different direction than the surrounding text.
 * This may be used when adjacent segments of right-to-left text should
 * not be reordered relative to each other. 
 * E.g., Multiple Java string literals in a right-to-left language
 * should generally remain in logical order to each other, that is, the
 * way they are stored. 
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 * @see BidiSegmentEvent
 * @since 2.0
 */
public void addBidiSegmentListener(BidiSegmentListener listener) {
	checkWidget();
	if (listener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	StyledTextListener typedListener = new StyledTextListener(listener);
	addListener(LineGetSegments, typedListener);	
}
/**
 * Adds a line background listener. A LineGetBackground event is sent by the 
 * widget to determine the background color for a line.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void addLineBackgroundListener(LineBackgroundListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (userLineBackground == false) {
		removeLineBackgroundListener(defaultLineStyler);
		defaultLineStyler.setLineBackground(0, logicalContent.getLineCount(), null);
		userLineBackground = true;
	}	
	StyledTextListener typedListener = new StyledTextListener(listener);
	addListener(LineGetBackground, typedListener);	
}
/**
 * Adds a line style listener. A LineGetStyle event is sent by the widget to 
 * determine the styles for a line.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void addLineStyleListener(LineStyleListener listener) {
	checkWidget();
	if (listener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	if (userLineStyle == false) {
		removeLineStyleListener(defaultLineStyler);
		defaultLineStyler.setStyleRange(null);
		userLineStyle = true;
	}
	StyledTextListener typedListener = new StyledTextListener(listener);
	addListener(LineGetStyle, typedListener);	
}
/**	 
 * Adds a modify listener. A Modify event is sent by the widget when the widget text 
 * has changed.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void addModifyListener(ModifyListener modifyListener) {
	checkWidget();
	if (modifyListener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	TypedListener typedListener = new TypedListener(modifyListener);
	addListener(SWT.Modify, typedListener);
}
/**	 
 * Adds a selection listener. A Selection event is sent by the widget when the 
 * selection has changed.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void addSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	TypedListener typedListener = new TypedListener(listener);
	addListener(SWT.Selection, typedListener);	
}
/**	 
 * Adds a verify key listener. A VerifyKey event is sent by the widget when a key 
 * is pressed. The widget ignores the key press if the listener sets the doit field 
 * of the event to false. 
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void addVerifyKeyListener(VerifyKeyListener listener) {
	checkWidget();
	if (listener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	StyledTextListener typedListener = new StyledTextListener(listener);
	addListener(VerifyKey, typedListener);	
}
/**	 
 * Adds a verify listener. A Verify event is sent by the widget when the widget text 
 * is about to change. The listener can set the event text and the doit field to 
 * change the text that is set in the widget or to force the widget to ignore the 
 * text change.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void addVerifyListener(VerifyListener verifyListener) {
	checkWidget();
	if (verifyListener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	TypedListener typedListener = new TypedListener(verifyListener);
	addListener(SWT.Verify, typedListener);
}
/** 
 * Appends a string to the text at the end of the widget.
 * <p>
 *
 * @param string the string to be appended
 * @see #replaceTextRange(int,int,String)
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void append(String string) {
	checkWidget();
	if (string == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	int lastChar = Math.max(getCharCount(), 0);
	replaceTextRange(lastChar, 0, string);
}
/**
 * Calculates the width of the widest visible line.
 */
void calculateContentWidth() {
	if (lineHeight != 0) {
	    lineCache = getLineCache(content);
		lineCache.calculate(topIndex, getPartialBottomIndex() - topIndex + 1);
	}
}
/**
 * Calculates the scroll bars
 */
void calculateScrollBars() {
	ScrollBar horizontalBar = getHorizontalBar();
	ScrollBar verticalBar = getVerticalBar();
	
	setScrollBars();
	if (verticalBar != null) {
		verticalBar.setIncrement(getVerticalIncrement());
	}	
	if (horizontalBar != null) {
		horizontalBar.setIncrement(getHorizontalIncrement());
	}
}
/**
 * Hides the scroll bars if widget is created in single line mode.
 */
static int checkStyle(int style) {
	if ((style & SWT.SINGLE) != 0) {
		style &= ~(SWT.H_SCROLL | SWT.V_SCROLL);
	}
	return style;
}
/**
 * Scrolls down the text to use new space made available by a resize or by 
 * deleted lines.
 */
void claimBottomFreeSpace() {
	int newVerticalOffset = Math.max(0, content.getLineCount() * lineHeight - getClientArea().height);
	
	if (newVerticalOffset < verticalScrollOffset) {
		// Scroll up so that empty lines below last text line are used.
		// Fixes 1GEYJM0
		setVerticalScrollOffset(newVerticalOffset, true);
	}
}
/**
 * Scrolls text to the right to use new space made available by a resize.
 */
void claimRightFreeSpace() {
	int newHorizontalOffset = Math.max(0, lineCache.getWidth() - (getClientArea().width - leftMargin - rightMargin));
	
	if (newHorizontalOffset < horizontalScrollOffset) {			
		// item is no longer drawn past the right border of the client area
		// align the right end of the item with the right border of the 
		// client area (window is scrolled right).
		scrollHorizontalBar(newHorizontalOffset - horizontalScrollOffset);					
	}
}
/**
 * Clears the widget margin.
 * 
 * @param gc GC to render on
 * @param background background color to use for clearing the margin
 * @param clientArea widget client area dimensions
 * @param renderHeight height in pixel of the rendered lines
 */
void clearMargin(GC gc, Color background, Rectangle clientArea, int renderHeight) {
	// clear the margin background
	gc.setBackground(background);
	gc.fillRectangle(0, 0, clientArea.width, topMargin);
	gc.fillRectangle(0, 0, leftMargin, renderHeight);	
	gc.fillRectangle(
		0, clientArea.height - bottomMargin, 
		clientArea.width, bottomMargin);
	gc.fillRectangle(
		clientArea.width - rightMargin, 0, 
		rightMargin, renderHeight);
}
/**
 * Removes the widget selection.
 * <p>
 *
 * @param sendEvent a Selection event is sent when set to true and when the selection is actually reset.
 */
void clearSelection(boolean sendEvent) {
	int selectionStart = selection.x;
	int selectionEnd = selection.y;
	int length = content.getCharCount();
	
	resetSelection();
	// redraw old selection, if any
	if (selectionEnd - selectionStart > 0) {
		// called internally to remove selection after text is removed
		// therefore make sure redraw range is valid.
		int redrawStart = Math.min(selectionStart, length);
		int redrawEnd = Math.min(selectionEnd, length);
		if (redrawEnd - redrawStart > 0) {
			internalRedrawRange(redrawStart, redrawEnd - redrawStart, true);
		}
		if (sendEvent == true) {
			sendSelectionEvent();
		}
	}
}
/**
 * Computes the preferred size.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget();
	int count, width, height;
	boolean singleLine = (getStyle() & SWT.SINGLE) != 0;
	
	if (singleLine) {
		count = 1;
	}
	else {
		count = content.getLineCount();
	}
	if (wordWrap) {
		if (((WrappedContent) content).getVisualLineCount() != 0) {
			// lines have already been wrapped to a specific width.
			// use existing line count. fixes bug 9191
			width = lineCache.getWidth();
			if (singleLine == false) {
				count = content.getLineCount();
			}
		}
		else {
			// lines have not been wrapped yet. wrap to width hint 
			// or to default width.
			if (wHint != SWT.DEFAULT) {
				width = wHint;
			} 
			else {
				width = DEFAULT_WIDTH;
			}
			if (singleLine == false) {
				((WrappedContent) content).wrapLines(width);
				count = content.getLineCount();
			}
		}
	}
	else
	if (wHint != SWT.DEFAULT) {
		width = wHint;
	} 
	else {
		// Only calculate what can actually be displayed.
		// Do this because measuring each text line is a 
		// time-consuming process.
		int visibleCount = Math.min (count, getDisplay().getBounds().height / lineHeight);
		lineCache.calculate(0, visibleCount);
		width = lineCache.getWidth() + leftMargin + rightMargin;
	}
	// If a height or width has been specified (via hHint and wHint),
	// use those values.  Otherwise calculate the size based on the
	// text that is defined.
	if (hHint != SWT.DEFAULT) {
		height = hHint;
	} 
	else {
		height = count * lineHeight + topMargin + bottomMargin;
	}
	// Use default values if no text is defined.
	if (width == 0) {
		width = DEFAULT_WIDTH;
	}
	if (height == 0) {
		if (singleLine) {
			height = lineHeight;
		}
		else {
			height = DEFAULT_HEIGHT;
		}
	}
	Rectangle rect = computeTrim(0, 0, width, height);
	return new Point (rect.width, rect.height);
}
/**
 * Copies the selected text to the clipboard.  The text will be put in the 
 * clipboard in plain text format and RTF format.
 * <p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void copy() {
	checkWidget();
	int length = selection.y - selection.x;
	if (length > 0) {
		try {
			setClipboardContent(selection.x, length);
		}
		catch (SWTError error) {
			// Copy to clipboard failed. This happens when another application 
			// is accessing the clipboard while we copy. Ignore the error.
			// Fixes 1GDQAVN
			// Rethrow all other errors. Fixes bug 17578.
			if (error.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
				throw error;
			}
		}
	}
}
/**
 * Returns a string that uses only the line delimiter specified by the 
 * StyledTextContent implementation.
 * Returns only the first line if the widget has the SWT.SINGLE style.
 * <p>
 *
 * @param text the text that may have line delimiters that don't 
 * 	match the model line delimiter. Possible line delimiters 
 * 	are CR ('\r'), LF ('\n'), CR/LF ("\r\n")
 * @return the converted text that only uses the line delimiter 
 * 	specified by the model. Returns only the first line if the widget 
 * 	has the SWT.SINGLE style.
 */
String getModelDelimitedText(String text) {
	StringBuffer convertedText;
	String delimiter = getLineDelimiter();
	int length = text.length();	
	int crIndex = 0;
	int lfIndex = 0;
	int i = 0;
	
	if (length == 0) {
		return text;
	}
	convertedText = new StringBuffer(length);
	while (i < length) {
		if (crIndex != -1) {
			crIndex = text.indexOf(SWT.CR, i);
		}
		if (lfIndex != -1) {
			lfIndex = text.indexOf(SWT.LF, i);
		}
		if (lfIndex == -1 && crIndex == -1) {	// no more line breaks?
			break;
		}
		else									// CR occurs before LF or no LF present?
		if ((crIndex < lfIndex && crIndex != -1) || lfIndex == -1) {	
			convertedText.append(text.substring(i, crIndex));
			if (lfIndex == crIndex + 1) {		// CR/LF combination?
				i = lfIndex + 1;
			}
			else {
				i = crIndex + 1;
			}
		}
		else {									// LF occurs before CR!
			convertedText.append(text.substring(i, lfIndex));
			i = lfIndex + 1;
		}
		if (isSingleLine()) {
			break;
		}
		convertedText.append(delimiter);
	}
	// copy remaining text if any and if not in single line mode or no 
	// text copied thus far (because there only is one line)
	if (i < length && (isSingleLine() == false || convertedText.length() == 0)) {
		convertedText.append(text.substring(i));
	}
	return convertedText.toString();
}
/**
 * Creates default key bindings.
 */
void createKeyBindings() {
	// Navigation
	setKeyBinding(SWT.ARROW_UP, ST.LINE_UP);	
	setKeyBinding(SWT.ARROW_DOWN, ST.LINE_DOWN);
	setKeyBinding(SWT.HOME, ST.LINE_START);
	setKeyBinding(SWT.END, ST.LINE_END);
	setKeyBinding(SWT.ARROW_LEFT, ST.COLUMN_PREVIOUS);
	setKeyBinding(SWT.ARROW_RIGHT, ST.COLUMN_NEXT);
	setKeyBinding(SWT.PAGE_UP, ST.PAGE_UP);
	setKeyBinding(SWT.PAGE_DOWN, ST.PAGE_DOWN);
	setKeyBinding(SWT.ARROW_LEFT | SWT.CTRL, ST.WORD_PREVIOUS);
	setKeyBinding(SWT.ARROW_RIGHT | SWT.CTRL, ST.WORD_NEXT);
	setKeyBinding(SWT.HOME | SWT.CTRL, ST.TEXT_START);	
	setKeyBinding(SWT.END | SWT.CTRL, ST.TEXT_END);
	setKeyBinding(SWT.PAGE_UP | SWT.CTRL, ST.WINDOW_START);
	setKeyBinding(SWT.PAGE_DOWN | SWT.CTRL, ST.WINDOW_END);
	// Selection
	setKeyBinding(SWT.ARROW_UP | SWT.SHIFT, ST.SELECT_LINE_UP);	
	setKeyBinding(SWT.ARROW_DOWN | SWT.SHIFT, ST.SELECT_LINE_DOWN);
	setKeyBinding(SWT.HOME | SWT.SHIFT, ST.SELECT_LINE_START);
	setKeyBinding(SWT.END | SWT.SHIFT, ST.SELECT_LINE_END);
	setKeyBinding(SWT.ARROW_LEFT | SWT.SHIFT, ST.SELECT_COLUMN_PREVIOUS);
	setKeyBinding(SWT.ARROW_RIGHT | SWT.SHIFT, ST.SELECT_COLUMN_NEXT);
	setKeyBinding(SWT.PAGE_UP | SWT.SHIFT, ST.SELECT_PAGE_UP);
	setKeyBinding(SWT.PAGE_DOWN | SWT.SHIFT, ST.SELECT_PAGE_DOWN);
	setKeyBinding(SWT.ARROW_LEFT | SWT.CTRL | SWT.SHIFT, ST.SELECT_WORD_PREVIOUS);
	setKeyBinding(SWT.ARROW_RIGHT | SWT.CTRL | SWT.SHIFT, ST.SELECT_WORD_NEXT);
	setKeyBinding(SWT.HOME | SWT.CTRL | SWT.SHIFT, ST.SELECT_TEXT_START);	
	setKeyBinding(SWT.END | SWT.CTRL | SWT.SHIFT, ST.SELECT_TEXT_END);
	setKeyBinding(SWT.PAGE_UP | SWT.CTRL | SWT.SHIFT, ST.SELECT_WINDOW_START);
	setKeyBinding(SWT.PAGE_DOWN | SWT.CTRL | SWT.SHIFT, ST.SELECT_WINDOW_END);
	// Modification
	// Cut, Copy, Paste
	// CUA style
	setKeyBinding('\u0018' | SWT.CTRL, ST.CUT);
	setKeyBinding('\u0003' | SWT.CTRL, ST.COPY);
	setKeyBinding('\u0016' | SWT.CTRL, ST.PASTE);
	// Wordstar style
	setKeyBinding(SWT.DEL | SWT.SHIFT, ST.CUT);
	setKeyBinding(SWT.INSERT | SWT.CTRL, ST.COPY);
	setKeyBinding(SWT.INSERT | SWT.SHIFT, ST.PASTE);
	setKeyBinding(SWT.BS | SWT.SHIFT, ST.DELETE_PREVIOUS);
	setKeyBinding(SWT.BS, ST.DELETE_PREVIOUS);
	setKeyBinding(SWT.DEL, ST.DELETE_NEXT);
	
	// Miscellaneous
	setKeyBinding(SWT.INSERT, ST.TOGGLE_OVERWRITE);
}
/**
 * Create the bidi caret.  Use the caret for the current keyboard
 * mode.
 */
void createBidiCaret() {
	Caret caret = getCaret();
	if (caret == null) {
		caret = new Caret(this, SWT.NULL);			
	}
	int direction = StyledTextBidi.getKeyboardLanguageDirection();
	if (direction == caretDirection) {
		return;
	}
	caretDirection = direction;
	if (caretDirection == SWT.LEFT) {
		caret.setImage(leftCaretBitmap);			
	} 
	else 
	if (caretDirection == SWT.RIGHT) {
		caret.setImage(rightCaretBitmap);			
	}	
}
/**
 * Create the bitmaps to use for the caret in bidi mode.  This
 * method only needs to be called upon widget creation and when the
 * font changes (the caret bitmap height needs to match font height).
 */
void createCaretBitmaps() {
	int caretWidth = BIDI_CARET_WIDTH;
	
	Display display = getDisplay();	
	if (caretPalette == null) {
		caretPalette = new PaletteData(new RGB[] {new RGB (0,0,0), new RGB (255,255,255)});
	}	
	if (leftCaretBitmap != null) {
		leftCaretBitmap.dispose();
	}
	ImageData imageData = new ImageData(caretWidth, lineHeight, 1, caretPalette);
	leftCaretBitmap = new Image(display, imageData);
	GC gc = new GC (leftCaretBitmap);
	gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
	gc.drawLine(0,0,0,lineHeight);
	gc.drawLine(0,0,caretWidth-1,0);
	gc.drawLine(0,1,1,1);
	gc.dispose();	
	
	if (rightCaretBitmap != null) {
		rightCaretBitmap.dispose();
	}
	rightCaretBitmap = new Image(display, imageData);
	gc = new GC (rightCaretBitmap);
	gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
	gc.drawLine(caretWidth-1,0,caretWidth-1,lineHeight);
	gc.drawLine(0,0,caretWidth-1,0);
	gc.drawLine(caretWidth-1,1,1,1);
	gc.dispose();	
}
/**
 * Moves the selected text to the clipboard.  The text will be put in the 
 * clipboard in plain text format and RTF format.
 * <p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void cut(){
	checkWidget();
	int length = selection.y - selection.x;
	
	if (length > 0) {
		try {
			setClipboardContent(selection.x, length);
		}
		catch (SWTError error) {
			// Copy to clipboard failed. This happens when another application 
			// is accessing the clipboard while we copy. Ignore the error.
			// Fixes 1GDQAVN
			// Rethrow all other errors. Fixes bug 17578.
			if (error.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
				throw error;
			}
			// Abort cut operation if copy to clipboard fails.
			// Fixes bug 21030.
			return;
		}
		doDelete();
	}
}
/** 
 * A mouse move event has occurred.  See if we should start autoscrolling.  If
 * the move position is outside of the client area, initiate autoscrolling.  
 * Otherwise, we've moved back into the widget so end autoscrolling.
 */
void doAutoScroll(Event event) {
	Rectangle area = getClientArea();		
	
	if (event.y > area.height) {
		doAutoScroll(SWT.DOWN);
	}
	else 
	if (event.y < 0) {
		doAutoScroll(SWT.UP);
	}
	else 
	if (event.x < leftMargin && wordWrap == false) {
		doAutoScroll(SWT.LEFT);
	}
	else 
	if (event.x > area.width - leftMargin - rightMargin && wordWrap == false) {
		doAutoScroll(SWT.RIGHT);
	}
	else {
		endAutoScroll();
	}
}
/** 
 * Initiates autoscrolling.
 * <p>
 *
 * @param direction SWT.UP, SWT.DOWN, SWT.RIGHT, SWT.LEFT
 */
void doAutoScroll(int direction) {
	Runnable timer = null;
	final int TIMER_INTERVAL = 5;
	
	// If we're already autoscrolling in the given direction do nothing
	if (autoScrollDirection == direction) {
		return;
	}
	
	final Display display = getDisplay();
	// Set a timer that will simulate the user pressing and holding
	// down a cursor key (i.e., arrowUp, arrowDown).
	if (direction == SWT.UP) {
		timer = new Runnable() {
			public void run() {
				if (autoScrollDirection == SWT.UP) {
					doLineUp();
					doSelection(SWT.LEFT);
					display.timerExec(TIMER_INTERVAL, this);
				}
			}
		};
	} else if (direction == SWT.DOWN) {
		timer = new Runnable() {
			public void run() {
				if (autoScrollDirection == SWT.DOWN) {
					doLineDown();
					doSelection(SWT.RIGHT);
					display.timerExec(TIMER_INTERVAL, this);
				}
			}
		};
	} else if (direction == SWT.RIGHT) {
		timer = new Runnable() {
			public void run() {
				if (autoScrollDirection == SWT.RIGHT) {
					doColumnRight();
					doSelection(SWT.RIGHT);
					display.timerExec(TIMER_INTERVAL, this);
				}
			}
		};
	} else if (direction == SWT.LEFT) {
		timer = new Runnable() {
			public void run() {
				if (autoScrollDirection == SWT.LEFT) {
					doColumnLeft();
					doSelection(SWT.LEFT);
					display.timerExec(TIMER_INTERVAL, this);
				}
			}
		};
	} 	
	if (timer != null) {
		autoScrollDirection = direction;
		display.timerExec(TIMER_INTERVAL, timer);
	}
}
/**
 * Deletes the previous character. Delete the selected text if any.
 * Move the caret in front of the deleted text.
 */
void doBackspace() {
	Event event = new Event();
	event.text = "";
	if (selection.x != selection.y) {
		event.start = selection.x;
		event.end = selection.y;
		sendKeyEvent(event);
	}
	else
	if (caretOffset > 0) {
		int line = content.getLineAtOffset(caretOffset);
		int lineOffset = content.getOffsetAtLine(line);			
	
		if (caretOffset == lineOffset) {
			lineOffset = content.getOffsetAtLine(line - 1);
			event.start = lineOffset + content.getLine(line - 1).length();
			event.end = caretOffset;
		}
		else {
			event.start = caretOffset - 1;
			event.end = caretOffset;
		}
		sendKeyEvent(event);
	}
}
/**
 * Moves the caret to the specified location.
 * <p>
 *
 * @param x	x location of the new caret position
 * @param y	y location of the new caret position
 * @param select the location change is a selection operation.
 * 	include the line delimiter in the selection
 */
void doBidiMouseLocationChange(int x, int y, boolean select) {
	int line = (y + verticalScrollOffset) / lineHeight;
	int lineCount = content.getLineCount();
	
	if (line > lineCount - 1) {
		line = lineCount - 1;
	}	
	// allow caret to be placed below first line only if receiver is 
	// not in single line mode. fixes 4820.
	if (line == 0 || (isSingleLine() == false && line > 0)) {
		int newCaretOffset = getBidiOffsetAtMouseLocation(x, line);	
		if (x >= 0 || 
			content.getLineAtOffset(newCaretOffset) != content.getLineAtOffset(caretOffset)) {
			// Only change the caret offset when the mouse is within 
			// the left client area border or on a different line. 
			// Otherwise the autoscroll selection may be reset. 
			// Fixes 1GKM3XS
			caretOffset = newCaretOffset;
			caretLine = line;
			if (select) {
				doMouseSelection();
			}
			setBidiCaretLocation(null);
			setBidiKeyboardLanguage();
		}
		if (select == false) {
			clearSelection(true);
		}
	}
}
/**
 * Moves the caret one character to the left.  Do not go to the previous line.
 * When in a bidi locale and at a R2L character the caret is moved to the 
 * beginning of the R2L segment (visually right) and then one character to the 
 * left (visually left because it's now in a L2R segment).
 */
void doColumnLeft() {
	int line = content.getLineAtOffset(caretOffset);
	int lineOffset = content.getOffsetAtLine(line);	
	int offsetInLine = caretOffset - lineOffset;
	
	if (isBidi()) {
		String lineText = content.getLine(line);
		int lineLength = lineText.length();
		GC gc = new GC(this);
		StyledTextBidi bidi = getStyledTextBidi(lineText, lineOffset, gc);
		
		if (horizontalScrollOffset > 0 || offsetInLine > 0) {
			if (offsetInLine < lineLength && bidi.isRightToLeft(offsetInLine)) {
				// advance caret logically if in R2L segment (move visually left)
				caretOffset++;
				doSelection(SWT.RIGHT);
				if (caretOffset - lineOffset == lineLength) {
					// if the line end is reached in a R2L segment, make the 
					// caret position (visual left border) visible before 
					// jumping to segment start
					showCaret();
				}
				// end of R2L segment reached (visual left side)?
				if (bidi.isRightToLeft(caretOffset - lineOffset) == false) {
					if (bidi.getCaretPosition(caretOffset - lineOffset) < horizontalScrollOffset) {
						// make beginning of R2L segment visible before going 
						// left, to L2R segment important if R2L segment ends 
						// at visual left in order to scroll all the way to the
						// left. Fixes 1GKM3XS
						showCaret();
					}
					// go to beginning of R2L segment (visually end of next L2R
					// segment)/beginning of line
					caretOffset--;
					while (caretOffset - lineOffset > 0 &&
						   bidi.isRightToLeft(caretOffset - lineOffset)) {
						caretOffset--;
					}
				}
			}
			else
			if (offsetInLine == lineLength && 
				bidi.getCaretPosition(lineLength) != XINSET) {
				// at logical line end in R2L segment but there's more text (a
				// L2R segment) go to end of R2L segment (visually left of next
				// L2R segment)/end of line
				caretOffset--;
				while (caretOffset - lineOffset > 0 && 
					   bidi.isRightToLeft(caretOffset - lineOffset)) {
					caretOffset--;
				}
			}
			else
			if (offsetInLine > 0 && bidi.isRightToLeft(offsetInLine) == false) {
				// decrease caret logically if in L2R segment (move visually left)
				caretOffset--;
				doSelection(SWT.LEFT);
				// end of L2R segment reached (visual left side of preceeding R2L 
				// segment)?
				if (caretOffset - lineOffset > 0 && 
					bidi.isRightToLeft(caretOffset - lineOffset - 1)) {
					// go to beginning of R2L segment (visually start of next L2R
					// segment)/beginning of line
					caretOffset--;
					while (caretOffset - lineOffset > 0 &&
						   bidi.isRightToLeft(caretOffset - lineOffset - 1)) {
						caretOffset--;
					}
				}
			}
			// if new caret position is to the left of the client area
			if (bidi.getCaretPosition(caretOffset - lineOffset) < horizontalScrollOffset) {
				// scroll to the caret position
				showCaret();
			}
			else {
				// otherwise just update caret position without scrolling it into view
				setCaretLocation();
				setBidiKeyboardLanguage();
			}
			// Beginning of line reached (auto scroll finished) but not scrolled 
			// completely to the left? Fixes 1GKM193
			if (caretOffset - lineOffset == 0 && horizontalScrollOffset > 0 && 
				horizontalScrollOffset <= XINSET) {
				scrollHorizontalBar(-horizontalScrollOffset);
			}
		}
		gc.dispose();
	}
	else
	if (offsetInLine > 0) {
		caretOffset--;
		showCaret();
	}
}
/**
 * Moves the caret one character to the right.  Do not go to the next line.
 * When in a bidi locale and at a R2L character the caret is moved to the 
 * end of the R2L segment (visually left) and then one character to the 
 * right (visually right because it's now in a L2R segment).
 */
void doColumnRight() {
	int line = content.getLineAtOffset(caretOffset);
	int lineOffset = content.getOffsetAtLine(line);	
	int offsetInLine = caretOffset - lineOffset;
	String lineText = content.getLine(line);
	int lineLength = lineText.length();
	
	if (isBidi()) {
		GC gc = new GC(this);
		StyledTextBidi bidi = getStyledTextBidi(lineText, lineOffset, gc);
		if (bidi.getTextWidth() + leftMargin > horizontalScrollOffset + getClientArea().width || 
			offsetInLine < lineLength) {
			if (bidi.isRightToLeft(offsetInLine) == false && 
				offsetInLine < lineLength) {
				// advance caret logically if in L2R segment (move visually right)
				caretOffset++;
				doSelection(SWT.RIGHT);
				// end of L2R segment reached (visual right side)?
				if (bidi.isRightToLeft(caretOffset - lineOffset)) {
					// go to end of R2L segment (visually left of next R2L segment)/ 
					// end of line
					caretOffset++;
					while (caretOffset < lineOffset + lineLength &&
						   bidi.isRightToLeft(caretOffset - lineOffset)) {
						caretOffset++;
					}
				}
			}
			else
			if (offsetInLine > 0 && 
				(bidi.isRightToLeft(offsetInLine) || 
				bidi.getTextWidth() + leftMargin > horizontalScrollOffset + getClientArea().width ||
				offsetInLine < lineLength)) {
				// advance caret visually if in R2L segment or logically at line end 
				// but right end of line is not fully visible yet
				caretOffset--;
				doSelection(SWT.LEFT);
				offsetInLine = caretOffset - lineOffset;
				// end of R2L segment reached (visual right side)?
				if (offsetInLine > 0 && bidi.isRightToLeft(offsetInLine) == false) {
					// go to end of R2L segment (visually left of next L2R segment)/ 
					// end of line
					caretOffset++;
					while (caretOffset < lineOffset + lineLength &&
						   bidi.isRightToLeft(caretOffset - lineOffset)) {
						caretOffset++;
					}
				}
			}
			else
			if (offsetInLine == 0 && bidi.getCaretPosition(0) != bidi.getTextWidth()) {
				// at logical line start in R2L segment but there's more text (a L2R
				// segment) go to end of R2L segment (visually left of next L2R
				// segment)/end of line
				caretOffset++;
				while (caretOffset < lineOffset + lineLength &&
					   bidi.isRightToLeft(caretOffset - lineOffset - 1)) {
					caretOffset++;
				}
			}
			offsetInLine = caretOffset - lineOffset;
			// if new caret position is to the right of the client area
			if (bidi.getCaretPosition(offsetInLine) >= horizontalScrollOffset) {
				// scroll to the caret position
				showCaret();
			}
			else {
				// otherwise just update caret position without scrolling it into view
				setCaretLocation();
				setBidiKeyboardLanguage();
			}
			if (offsetInLine > 0 && offsetInLine < lineLength - 1) {
				int clientAreaEnd = horizontalScrollOffset + getClientArea().width;
				boolean directionChange = bidi.isRightToLeft(offsetInLine - 1) == false && bidi.isRightToLeft(offsetInLine);
				int textWidth = bidi.getTextWidth() + leftMargin;
				// between L2R and R2L segment and second character of R2L segment is
				// left of right border and logical line end is left of right border
				// but visual line end is not left of right border
				if (directionChange && 
					bidi.isRightToLeft(offsetInLine + 1) &&
					bidi.getCaretPosition(offsetInLine + 1) + leftMargin < clientAreaEnd && 
					bidi.getCaretPosition(lineLength) + leftMargin < clientAreaEnd && textWidth > clientAreaEnd) {
					// make visual line end visible
					scrollHorizontalBar(textWidth - clientAreaEnd);
				}
			}
		}
		gc.dispose();
	}
	else
	if (offsetInLine < lineLength) {
		caretOffset++;
		showCaret();
	}
}
/**
 * Replaces the selection with the character or insert the character at the 
 * current caret position if no selection exists.
 * If a carriage return was typed replace it with the line break character 
 * used by the widget on this platform.
 * <p>
 *
 * @param key the character typed by the user
 */
void doContent(char key) {
	Event event;
	
	if (textLimit > 0 && 
		content.getCharCount() - (selection.y - selection.x) >= textLimit) {
		return;
	}	
	event = new Event();
	event.start = selection.x;
	event.end = selection.y;
	// replace a CR line break with the widget line break
	// CR does not make sense on Windows since most (all?) applications
	// don't recognize CR as a line break.
	if (key == SWT.CR || key == SWT.LF) {
		if (isSingleLine() == false) {
			event.text = getLineDelimiter();
		}
	}
	// no selection and overwrite mode is on and the typed key is not a
	// tab character (tabs are always inserted without overwriting)?
	else
	if (selection.x == selection.y && overwrite == true && key != TAB) {
		int lineIndex = content.getLineAtOffset(event.end);
		int lineOffset = content.getOffsetAtLine(lineIndex);
		String line = content.getLine(lineIndex);
		// replace character at caret offset if the caret is not at the 
		// end of the line
		if (event.end < lineOffset + line.length()) {
			event.end++;
		}
		event.text = new String(new char[] {key});
	}
	else {
		event.text = new String(new char[] {key});
	}
	if (event.text != null) {
		sendKeyEvent(event);
	}
}
/**
 * Moves the caret after the last character of the widget content.
 */
void doContentEnd() {
	// place caret at end of first line if receiver is in single 
	// line mode. fixes 4820.
	if (isSingleLine()) {
		doLineEnd();
	}
	else {
		int length = content.getCharCount();		
		if (caretOffset < length) {
			caretOffset = length;
			caretLine = content.getLineCount() - 1;
			showCaret();
		}
	}
}
/**
 * Moves the caret in front of the first character of the widget content.
 */
void doContentStart() {
	if (caretOffset > 0) {
		caretOffset = 0;
		caretLine = 0;
		showCaret();
	}
}
/**
 * Moves the caret to the start of the selection if a selection exists.
 * Otherwise, if no selection exists move the cursor according to the 
 * cursor selection rules.
 * <p>
 *
 * @see #doSelectionCursorPrevious
 */
void doCursorPrevious() {
	if (selection.y - selection.x > 0) {
		caretOffset = selection.x;
		caretLine = content.getLineAtOffset(caretOffset);
		showCaret();
	}
	else {
		doSelectionCursorPrevious();
	}
}
/**
 * Moves the caret to the end of the selection if a selection exists.
 * Otherwise, if no selection exists move the cursor according to the 
 * cursor selection rules.
 * <p>
 *
 * @see #doSelectionCursorNext
 */
void doCursorNext() {
	if (selection.y - selection.x > 0) {
		caretOffset = selection.y;
		caretLine = content.getLineAtOffset(caretOffset);
		showCaret();
	}
	else {
		doSelectionCursorNext();
	}
}
/**
 * Deletes the next character. Delete the selected text if any.
 */
void doDelete() {
	Event event = new Event();
	event.text = "";
	if (selection.x != selection.y) {
		event.start = selection.x;
		event.end = selection.y;
		sendKeyEvent(event);
	}
	else
	if (caretOffset < content.getCharCount()) {
		int line = content.getLineAtOffset(caretOffset);
		int lineOffset = content.getOffsetAtLine(line);
		int lineLength = content.getLine(line).length();
				
		if (caretOffset == lineOffset + lineLength) {
			event.start = caretOffset;
			event.end = content.getOffsetAtLine(line + 1);
		}
		else {
			event.start = caretOffset;
			event.end = caretOffset + 1;
		}
		sendKeyEvent(event);
	}
}
/**
 * Moves the caret one line down and to the same character offset relative 
 * to the beginning of the line. Move the caret to the end of the new line 
 * if the new line is shorter than the character offset.
 * Make the new caret position visible.
 */
void doLineDown() {
	doSelectionLineDown();
	showCaret();
}
/**
 * Moves the caret to the end of the line.
 */
void doLineEnd() {
	int line = content.getLineAtOffset(caretOffset);
	int lineOffset = content.getOffsetAtLine(line);	
	int lineLength = content.getLine(line).length();
	int lineEndOffset = lineOffset + lineLength;
	
	if (wordWrap && line != caretLine) {
		// caret is at the start of a visual line
		lineEndOffset = content.getOffsetAtLine(caretLine) + content.getLine(caretLine).length();
	}	
	if (caretOffset < lineEndOffset) {
		caretOffset = lineEndOffset;
		showCaret();
	}
}
/**
 * Moves the caret to the beginning of the line.
 */
void doLineStart() {
	int line = content.getLineAtOffset(caretOffset);
	int lineOffset = content.getOffsetAtLine(line);	
	if (caretOffset > lineOffset && line == caretLine) {
		caretOffset = lineOffset;
		showCaret();
	}
}
/**
 * Moves the caret one line up and to the same character offset relative 
 * to the beginning of the line. Move the caret to the end of the new line 
 * if the new line is shorter than the character offset.
 */
void doLineUp() {
	int line = content.getLineAtOffset(caretOffset);
	
	if (line != caretLine) {
		// caret is at end of visual line/start of next visual line in 
		// word wrap mode
		line = caretLine;
	}
	if (line > 0) {
		String lineText = content.getLine(line);
		int lineOffset = content.getOffsetAtLine(line);
		int offsetInLine = caretOffset - lineOffset;		
		int caretX = getXAtOffset(lineText, line, offsetInLine);
		caretLine = --line;
		if (isBidi()) {
			caretOffset = getBidiOffsetAtMouseLocation(caretX, line);
		}
		else {
			caretOffset = getOffsetAtMouseLocation(caretX, line);
		}		
	}
	showCaret();
}
/**
 * Moves the caret to the specified location.
 * <p>
 *
 * @param x x location of the new caret position
 * @param y y location of the new caret position
 * @param select the location change is a selection operation.
 * 	include the line delimiter in the selection
 */
void doMouseLocationChange(int x, int y, boolean select) {
	int line = (y + verticalScrollOffset) / lineHeight;
	int lineCount = content.getLineCount();
	
	if (line > lineCount - 1) {
		line = lineCount - 1;
	}	
	// allow caret to be placed below first line only if receiver is 
	// not in single line mode. fixes 4820.
	if (line == 0 || (isSingleLine() == false && line > 0)) {
		int newCaretOffset = getOffsetAtMouseLocation(x, line);
		
		if (newCaretOffset != caretOffset) {
			caretOffset = newCaretOffset;
			caretLine = line;
			if (select) {
				doMouseSelection();
			}
			setCaretLocation();
		}
		if (select == false) {
			clearSelection(true);
		}
	}
}
/**
 * Updates the selection based on the caret position
 */
void doMouseSelection() {
	if (caretOffset <= selection.x || 
		(caretOffset > selection.x && 
		 caretOffset < selection.y && selectionAnchor == selection.x)) {
		doSelection(SWT.LEFT);
	}
	else {
		doSelection(SWT.RIGHT);
	}
}
/**
 * Scrolls one page down so that the last line (truncated or whole)
 * of the current page becomes the fully visible top line.
 * The caret is scrolled the same number of lines so that its location 
 * relative to the top line remains the same. The exception is the end 
 * of the text where a full page scroll is not possible. In this case 
 * the caret is moved after the last character.
 * <p>
 *
 * @param select whether or not to select the page
 */
void doPageDown(boolean select) {
	int line = content.getLineAtOffset(caretOffset);
	int lineCount = content.getLineCount();
	
	// do nothing if in single line mode. fixes 5673
	if (isSingleLine()) {
		return;
	}
	if (line < lineCount - 1) {
		int offsetInLine = caretOffset - content.getOffsetAtLine(line);
		int verticalMaximum = content.getLineCount() * getVerticalIncrement();
		int pageSize = getClientArea().height;
		int scrollLines = Math.min(lineCount - line - 1, getLineCountWhole());
		int scrollOffset;
		int caretX = getXAtOffset(content.getLine(line), line, offsetInLine);
		
		// ensure that scrollLines never gets negative and at leat one 
		// line is scrolled. fixes bug 5602.
		scrollLines = Math.max(1, scrollLines);
		line += scrollLines;
		caretLine = line;
		if (isBidi()) {
			caretOffset = getBidiOffsetAtMouseLocation(caretX, line);
		}
		else {
			caretOffset = getOffsetAtMouseLocation(caretX, line);
		}	
		if (select) {
			doSelection(SWT.RIGHT);
		}
		// scroll one page down or to the bottom
		scrollOffset = verticalScrollOffset + scrollLines * getVerticalIncrement();
		if (scrollOffset + pageSize > verticalMaximum) {
			scrollOffset = verticalMaximum - pageSize;
		}
		if (scrollOffset > verticalScrollOffset) {
			setVerticalScrollOffset(scrollOffset, true);
		}
	}
	showCaret();
}
/**
 * Moves the cursor to the end of the last fully visible line.
 */
void doPageEnd() {
	// go to end of line if in single line mode. fixes 5673
	if (isSingleLine()) {
		doLineEnd();
	}
	else {
		int line = getBottomIndex();
		int bottomCaretOffset = content.getOffsetAtLine(line) + content.getLine(line).length();
		
		if (caretOffset < bottomCaretOffset) {
			caretOffset = bottomCaretOffset;
			caretLine = line;
			showCaret();
		}
	}
}
/**
 * Moves the cursor to the beginning of the first fully visible line.
 */
void doPageStart() {
	int topCaretOffset = content.getOffsetAtLine(topIndex);
	
	if (caretOffset > topCaretOffset) {
		caretOffset = topCaretOffset;
		caretLine = topIndex;
		showCaret();
	}
}
/**
 * Scrolls one page up so that the first line (truncated or whole)
 * of the current page becomes the fully visible last line.
 * The caret is scrolled the same number of lines so that its location 
 * relative to the top line remains the same. The exception is the beginning 
 * of the text where a full page scroll is not possible. In this case the
 * caret is moved in front of the first character.
 */
void doPageUp() {
	int line = content.getLineAtOffset(caretOffset);

	if (line > 0) {	
		int offsetInLine = caretOffset - content.getOffsetAtLine(line);
		int scrollLines = Math.max(1, Math.min(line, getLineCountWhole()));
		int scrollOffset;
		int caretX = getXAtOffset(content.getLine(line), line, offsetInLine);
		
		line -= scrollLines;
		caretLine = line;
		if (isBidi()) {
			caretOffset = getBidiOffsetAtMouseLocation(caretX, line);
		}
		else {
			caretOffset = getOffsetAtMouseLocation(caretX, line);
		}	
		// scroll one page up or to the top
		scrollOffset = Math.max(0, verticalScrollOffset - scrollLines * getVerticalIncrement());
		if (scrollOffset < verticalScrollOffset) {		
			setVerticalScrollOffset(scrollOffset, true);
		}
	}
	showCaret();
}
/**
 * Updates the selection to extend to the current caret position.
 */
void doSelection(int direction) {
	int redrawStart = -1;
	int redrawEnd = -1;
	
	if (selectionAnchor == -1) {
		selectionAnchor = selection.x;
	}
	if (direction == SWT.LEFT) {
		if (caretOffset < selection.x) {
			// grow selection
			redrawEnd = selection.x; 
			redrawStart = selection.x = caretOffset;		
			// check if selection has reversed direction
			if (selection.y != selectionAnchor) {
				redrawEnd = selection.y;
				selection.y = selectionAnchor;
			}
		}
		else	// test whether selection actually changed. Fixes 1G71EO1
		if (selectionAnchor == selection.x && caretOffset < selection.y) {
			// caret moved towards selection anchor (left side of selection). 
			// shrink selection			
			redrawEnd = selection.y;
			redrawStart = selection.y = caretOffset;		
		}
	}
	else {
		if (caretOffset > selection.y) {
			// grow selection
			redrawStart = selection.y;
			redrawEnd = selection.y = caretOffset;
			// check if selection has reversed direction
			if (selection.x != selectionAnchor) {
				redrawStart = selection.x;				
				selection.x = selectionAnchor;
			}
		}
		else	// test whether selection actually changed. Fixes 1G71EO1
		if (selectionAnchor == selection.y && caretOffset > selection.x) {
			// caret moved towards selection anchor (right side of selection). 
			// shrink selection			
			redrawStart = selection.x;
			redrawEnd = selection.x = caretOffset;		
		}
	}
	if (redrawStart != -1 && redrawEnd != -1) {
		internalRedrawRange(redrawStart, redrawEnd - redrawStart, true);
		sendSelectionEvent();
	}
}
/**
 * Moves the caret to the next character or to the beginning of the 
 * next line if the cursor is at the end of a line.
 */
void doSelectionCursorNext() {
	int line = content.getLineAtOffset(caretOffset);
	int offsetInLine;
	
	if (line != caretLine) {
		// caret is at end of visual line/start of next visual line in 
		// word wrap mode
		line = caretLine;
	}
	offsetInLine = caretOffset - content.getOffsetAtLine(line);
	if (offsetInLine < content.getLine(line).length()) {
		// Remember the last direction. Always update lastCaretDirection,
		// even though it's not used in non-bidi mode in order to avoid 
		// extra methods.		
		lastCaretDirection = ST.COLUMN_NEXT;
		caretOffset++;
		showCaret();
	}
	else
	if (line < content.getLineCount() - 1 && isSingleLine() == false) {
		// only go to next line if not in single line mode. fixes 5673
		line++;
		caretOffset = content.getOffsetAtLine(line);
		caretLine = line;
		showCaret();
	}
}
/**
 * Moves the caret to the previous character or to the end of the previous 
 * line if the cursor is at the beginning of a line.
 */
void doSelectionCursorPrevious() {
	int line = content.getLineAtOffset(caretOffset);
	int lineOffset;
	int offsetInLine;
	
	if (line != caretLine) {
		// caret is at end of visual line/start of next visual line in 
		// word wrap mode
		line = caretLine;
	}
	lineOffset = content.getOffsetAtLine(line);	
	offsetInLine = caretOffset - lineOffset;
	if (offsetInLine > 0) {
		// Remember the last direction. Always update lastCaretDirection,
		// even though it's not used in non-bidi mode in order to avoid 
		// extra methods.			
		lastCaretDirection = ST.COLUMN_PREVIOUS;		
		caretOffset--;
		showCaret();
	}
	else
	if (line > 0) {
		line--;
		lineOffset = content.getOffsetAtLine(line);
		caretOffset = lineOffset + content.getLine(line).length();
		caretLine = line;
		showCaret();
	}
}
/**
 * Moves the caret one line down and to the same character offset relative 
 * to the beginning of the line. Move the caret to the end of the new line 
 * if the new line is shorter than the character offset.
 */
int doSelectionLineDown() {
	int line = content.getLineAtOffset(caretOffset);
	
	if (line != caretLine) {
		// caret is at end of visual line/start of next visual line in 
		// word wrap mode
		line = caretLine;
	}
	// allow line down action only if receiver is not in single line mode.
	// fixes 4820.
	if (isSingleLine() == false && line < content.getLineCount() - 1) {
		String lineText = content.getLine(line);
		int offsetInLine = caretOffset - content.getOffsetAtLine(line);
		int caretX = getXAtOffset(lineText, line, offsetInLine);
		
		caretLine = ++line;
		if (isBidi()) {
			caretOffset = getBidiOffsetAtMouseLocation(caretX, line);
		}
		else {
			caretOffset = getOffsetAtMouseLocation(caretX, line);
		}		
	}
	return line;
}
/**
 * Moves the caret to the end of the next word .
 */
void doSelectionWordNext() {
	int newCaretOffset = getWordEnd(caretOffset);
	
	// don't change caret position if in single line mode and the cursor 
	// would be on a different line. fixes 5673
	if (isSingleLine() == false || 
		content.getLineAtOffset(caretOffset) == content.getLineAtOffset(newCaretOffset)) {
		lastCaretDirection = ST.COLUMN_NEXT;
		caretOffset = newCaretOffset;
		caretLine = content.getLineAtOffset(caretOffset);
		showCaret();
	}
}
/**
 * Moves the caret to the start of the previous word.
 */
void doSelectionWordPrevious() {
	lastCaretDirection = ST.COLUMN_PREVIOUS;
	caretOffset = getWordStart(caretOffset);
	caretLine = content.getLineAtOffset(caretOffset);
	showCaret();
}
/**
 * Moves the caret to the end of the next word.
 * If a selection exists, move the caret to the end of the selection
 * and remove the selection.
 */
void doWordNext() {
	if (selection.y - selection.x > 0) {
		caretOffset = selection.y;
		showCaret();
	}
	else {
		doSelectionWordNext();
	}
}
/**
 * Moves the caret to the start of the previous word.
 * If a selection exists, move the caret to the start of the selection
 * and remove the selection.
 */
void doWordPrevious() {
	if (selection.y - selection.x > 0) {
		caretOffset = selection.x;
		showCaret();
	}
	else {
		doSelectionWordPrevious();
	}
}
/**
 * Draws the specified rectangle.
 * Draw directly without invalidating the affected area when clearBackground is 
 * false.
 * <p>
 *
 * @param x the x position
 * @param y the y position
 * @param width the width
 * @param height the height
 * @param clearBackground true=clear the background by invalidating the requested 
 * 	redraw area, false=draw the foreground directly without invalidating the 
 * 	redraw area.
 */
void draw(int x, int y, int width, int height, boolean clearBackground) {
	if (clearBackground) {
		redraw(x + leftMargin, y + topMargin, width, height, true);
	}
	else {
		int startLine = (y + verticalScrollOffset) / lineHeight;
		int endY = y + height;
		int paintYFromTopLine = (startLine - topIndex) * lineHeight;
		int topLineOffset = (topIndex * lineHeight - verticalScrollOffset);
		int paintY = paintYFromTopLine + topLineOffset + topMargin;	// adjust y position for pixel based scrolling
		int lineCount = content.getLineCount();
		Color background = getBackground();
		Color foreground = getForeground();
		GC gc = new GC(this);
		FontData fontData = gc.getFont().getFontData()[0];
	
		if (isSingleLine()) {
			lineCount = 1;
			if (startLine > 1) {
				startLine = 1;
			}
		}
		for (int i = startLine; paintY < endY && i < lineCount; i++, paintY += lineHeight) {
			String line = content.getLine(i);
			renderer.drawLine(line, i, paintY, gc, background, foreground, fontData, clearBackground);
		}
		gc.dispose();	
	}
}
/** 
 * Ends the autoscroll process.
 */
void endAutoScroll() {
	autoScrollDirection = SWT.NULL;
}
/**
 * @see org.eclipse.swt.widgets.Control#getBackground
 */
public Color getBackground() {
	checkWidget();
	if (background == null) {
		return getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
	}
	return background;
}
/**
 * Gets the BIDI coloring mode.  When true the BIDI text display
 * algorithm is applied to segments of text that are the same
 * color.
 *
 * @return the current coloring mode
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * <p>
 * @deprecated use BidiSegmentListener instead.
 * </p>
 */
public boolean getBidiColoring() {
	checkWidget();
	return bidiColoring;
}
/**
 * Returns the offset at the specified x location in the specified line.
 * Also sets the caret direction so that the caret is placed correctly 
 * depending on whether the mouse location is in a R2L or L2R segment.
 * <p>
 *
 * @param x	x location of the mouse location
 * @param line	line the mouse location is in
 * @return the offset at the specified x location in the specified line,
 * 	relative to the beginning of the document
 */
int getBidiOffsetAtMouseLocation(int x, int line) {
	String lineText = content.getLine(line);
	int lineOffset = content.getOffsetAtLine(line);
	GC gc = new GC(this);
	StyledTextBidi bidi = getStyledTextBidi(lineText, lineOffset, gc);
	int[] values;
	int offsetInLine;
	x += horizontalScrollOffset;
	values = bidi.getCaretOffsetAndDirectionAtX(x - leftMargin);
	offsetInLine = values[0];
	lastCaretDirection = values[1];
	gc.dispose();
	
	return lineOffset + offsetInLine;
}
/** 
 * Returns the index of the last fully visible line.
 * <p>
 *
 * @return index of the last fully visible line.
 */
int getBottomIndex() {
	int lineCount = 1;
	
	if (lineHeight != 0) {
		// calculate the number of lines that are fully visible
		int partialTopLineHeight = topIndex * lineHeight - verticalScrollOffset;
		lineCount = (getClientArea().height - partialTopLineHeight) / lineHeight;
	}
	return Math.min(content.getLineCount() - 1, topIndex + Math.max(0, lineCount - 1));
}
/**
 * Returns the caret position relative to the start of the text.
 * <p>
 *
 * @return the caret position relative to the start of the text.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getCaretOffset() {
	checkWidget();
	
	return caretOffset;
}
/**
 * Returns the caret offset at the given x location in the line.
 * The caret offset is the offset of the character where the caret will be
 * placed when a mouse click occurs. The caret offset will be the offset of 
 * the character after the clicked one if the mouse click occurs at the second 
 * half of a character.
 * Doesn't properly handle ligatures and other context dependent characters 
 * unless the current locale is a bidi locale. 
 * Ligatures are handled properly as long as they don't occur at lineXOffset.
 * <p>
 *
 * @param line text of the line to calculate the offset in
 * @param lineOffset offset of the first character in the line. 
 * 	0 based from the beginning of the document.
 * @param lineXOffset x location in the line
 * @return caret offset at the x location relative to the start of the line.
 */
int getCaretOffsetAtX(String line, int lineOffset, int lineXOffset) {
	int offset = 0;
	GC gc = new GC(this);
	FontData currentFont = gc.getFont().getFontData()[0];
	StyleRange[] styles = null;
	StyledTextEvent event = renderer.getLineStyleData(lineOffset, line);
	
	lineXOffset += horizontalScrollOffset;
	if (event != null) {
		styles = renderer.filterLineStyles(event.styles);
	}
	int low = -1;
	int high = line.length();
	while (high - low > 1) {
		offset = (high + low) / 2;
		int x = renderer.textWidth(line, lineOffset, 0, offset, styles, 0, gc, currentFont) + leftMargin;
		int charWidth = renderer.textWidth(line, lineOffset, 0, offset + 1, styles, 0, gc, currentFont) + leftMargin - x;
		if (lineXOffset <= x + charWidth / 2) {
			high = offset;			
		}
		else {
			low = offset;
		}
	}
	offset = high;
	gc.dispose();
	return offset;	
}
/**
 * Returns the caret width.
 * <p>
 *
 * @return the caret width, 0 if caret is null.
 */
int getCaretWidth() {
	Caret caret = getCaret();
	if (caret == null) return 0;
	return caret.getSize().x;
}
/**
 * Returns the content implementation that is used for text storage
 * or null if no user defined content implementation has been set.
 * <p>
 *
 * @return content implementation that is used for text storage or null 
 * if no user defined content implementation has been set.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public StyledTextContent getContent() {
	checkWidget();
	
	return logicalContent;
}
/** 
 * Returns whether the widget implements double click mouse behavior.
 * <p>
 *
 * @return true if double clicking a word selects the word, false if double clicks
 * have the same effect as regular mouse clicks
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean getDoubleClickEnabled() {
	checkWidget();
	return doubleClickEnabled;
}
/**
 * Returns whether the widget content can be edited.
 * <p>
 *
 * @return true if content can be edited, false otherwise
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public boolean getEditable() {
	checkWidget();
	return editable;
}
/**
 * @see org.eclipse.swt.widgets.Control#getForeground
 */
public Color getForeground() {
	checkWidget();
	if (foreground == null) {
		return getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
	}
	return foreground;
}
/** 
 * Returns the horizontal scroll increment.
 * <p>
 *
 * @return horizontal scroll increment.
 */
int getHorizontalIncrement() {
	GC gc = new GC(this);
	int increment = gc.getFontMetrics().getAverageCharWidth();
	
	gc.dispose();
	return increment;
}
/** 
 * Returns the horizontal scroll offset relative to the start of the line.
 * <p>
 *
 * @return horizontal scroll offset relative to the start of the line,
 * measured in character increments starting at 0, if > 0 the content is scrolled
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getHorizontalIndex() {	
	checkWidget();
	return horizontalScrollOffset / getHorizontalIncrement();
}
/** 
 * Returns the horizontal scroll offset relative to the start of the line.
 * <p>
 *
 * @return the horizontal scroll offset relative to the start of the line,
 * measured in pixel starting at 0, if > 0 the content is scrolled.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getHorizontalPixel() {	
	checkWidget();
	return horizontalScrollOffset;
}
/** 
 * Returns the action assigned to the key.
 * Returns SWT.NULL if there is no action associated with the key.
 * <p>
 *
 * @param key a key code defined in SWT.java or a character. 
 * 	Optionally ORd with a state mask (one or more of SWT.CTRL, SWT.SHIFT, SWT.ALT)
 * @return one of the predefined actions defined in ST.java or SWT.NULL 
 * 	if there is no action associated with the key.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getKeyBinding(int key) {
	checkWidget();
	Integer action = (Integer) keyActionMap.get(new Integer(key));
	int intAction;
	
	if (action == null) {
		intAction = SWT.NULL;
	}
	else {
		intAction = action.intValue();
	}
	return intAction;
}
/**
 * Gets the number of characters.
 * <p>
 *
 * @return number of characters in the widget
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getCharCount() {
	checkWidget();
	return content.getCharCount();
}
/**
 * Returns the background color of the line at the given index.
 * Returns null if a LineBackgroundListener has been set or if no background 
 * color has been specified for the line. Should not be called if a
 * LineBackgroundListener has been set since the listener maintains the
 * line background colors.
 * <p>
 *
 * @return the background color of the line at the given index.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT when the index is invalid</li>
 * </ul>
 */
public Color getLineBackground(int index) {
	checkWidget();
	Color lineBackground = null;
	
	if (index < 0 || index > logicalContent.getLineCount()) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}
	if (userLineBackground == false) {
		lineBackground = defaultLineStyler.getLineBackground(index);
	}
	return lineBackground;
}
/**
 * Returns the line background data for the given line or null if 
 * there is none.
 * <p>
 * @param lineOffset offset of the line start relative to the start
 * 	of the content.
 * @param line line to get line background data for
 * @return line background data for the given line.
 */
StyledTextEvent getLineBackgroundData(int lineOffset, String line) {
	return sendLineEvent(LineGetBackground, lineOffset, line);
}
/** 
 * Gets the number of text lines.
 * <p>
 *
 * @return the number of lines in the widget
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getLineCount() {
	checkWidget();
	return getLineAtOffset(getCharCount()) + 1;
}
/**
 * Returns the number of lines that can be completely displayed in the 
 * widget client area.
 * <p>
 *
 * @return number of lines that can be completely displayed in the widget 
 * 	client area.
 */
int getLineCountWhole() {
	int lineCount;
	
	if (lineHeight != 0) {
		lineCount = getClientArea().height / lineHeight;
	}
	else {
		lineCount = 1;
	}
	return lineCount;
}
/**
 * Returns the line at the specified offset in the text.
 * 0 <= offset <= getCharCount() so that getLineAtOffset(getCharCount())
 * returns the line of the insert location.
 * <p>
 *
 * @param offset offset relative to the start of the content. 
 * 	0 <= offset <= getCharCount()
 * @return line at the specified offset in the text
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when the offset is outside the valid range (< 0 or > getCharCount())</li> 
 * </ul>
 */
public int getLineAtOffset(int offset) {
	checkWidget();
	
	if (offset < 0 || offset > getCharCount()) {
		SWT.error(SWT.ERROR_INVALID_RANGE);		
	}
	return logicalContent.getLineAtOffset(offset);
}
/**
 * Returns the line delimiter used for entering new lines by key down
 * or paste operation.
 * <p>
 *
 * @return line delimiter used for entering new lines by key down
 * or paste operation.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public String getLineDelimiter() {
	checkWidget();
	return content.getLineDelimiter();
}
/**
 * Returns a StyledTextEvent that can be used to request data such 
 * as styles and background color for a line.
 * The specified line may be a visual (wrapped) line if in word 
 * wrap mode. The returned object will always be for a logical 
 * (unwrapped) line.
 * <p>
 *
 * @param lineOffset offset of the line. This may be the offset of
 * 	a visual line if the widget is in word wrap mode.
 * @param line line text. This may be the text of a visualline if 
 * 	the widget is in word wrap mode.
 * @return StyledTextEvent that can be used to request line data 
 * 	for the given line.
 */
StyledTextEvent sendLineEvent(int eventType, int lineOffset, String line) {
	StyledTextEvent event = null;
	
	if (isListening(eventType)) {
		event = new StyledTextEvent(logicalContent);		
		if (wordWrap) {
		    // if word wrap is on, the line offset and text may be visual (wrapped)
		    int lineIndex = logicalContent.getLineAtOffset(lineOffset);
		    
		    event.detail = logicalContent.getOffsetAtLine(lineIndex);
			event.text = logicalContent.getLine(lineIndex);
		}
		else {
			event.detail = lineOffset;
			event.text = line;
		}
		notifyListeners(eventType, event);
	}
	return event;	
}
/**
 * Returns the line height.
 * <p>
 *
 * @return line height in pixel.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getLineHeight() {
	checkWidget();
	return lineHeight;
}
/**
 * Returns a LineCache implementation. Depending on whether or not
 * word wrap is on this may be a line wrapping or line width 
 * calculating implementaiton.
 * <p>
 * 
 * @param content StyledTextContent to create the LineCache on.
 * @return a LineCache implementation
 */
LineCache getLineCache(StyledTextContent content) {
    LineCache lineCache;
    
	if (wordWrap) {
	    lineCache = new WordWrapCache(this, (WrappedContent) content);
	}
	else {
	    lineCache = new ContentWidthCache(this, content.getLineCount());
	}
	return lineCache;
}
/**
 * Returns the line style data for the given line or null if there is 
 * none. If there is a LineStyleListener but it does not set any styles, 
 * the StyledTextEvent.styles field will be initialized to an empty 
 * array.
 * <p>
 * 
 * @param lineOffset offset of the line start relative to the start of 
 * 	the content.
 * @param line line to get line styles for
 * @return line style data for the given line. Styles may start before 
 * 	line start and end after line end
 */
StyledTextEvent getLineStyleData(int lineOffset, String line) {
	return sendLineEvent(LineGetStyle, lineOffset, line);
}
/**
 * Returns the x, y location of the upper left corner of the character 
 * bounding box at the specified offset in the text. The point is 
 * relative to the upper left corner of the widget client area.
 * <p>
 *
 * @param offset offset relative to the start of the content. 
 * 	0 <= offset <= getCharCount()
 * @return x, y location of the upper left corner of the character 
 * 	bounding box at the specified offset in the text.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when the offset is outside the valid range (< 0 or > getCharCount())</li> 
 * </ul>
 */
public Point getLocationAtOffset(int offset) {
	checkWidget();
	if (offset < 0 || offset > getCharCount()) {
		SWT.error(SWT.ERROR_INVALID_RANGE);		
	}
	int line = content.getLineAtOffset(offset);
	int lineOffset = content.getOffsetAtLine(line);
	String lineContent = content.getLine(line);
	int x = getXAtOffset(lineContent, line, offset - lineOffset);
	int y = line * lineHeight - verticalScrollOffset;
	
	return new Point(x, y);
}
/**
 * Returns the character offset of the first character of the given line.
 * <p>
 *
 * @param lineIndex index of the line, 0 based relative to the first 
 * 	line in the content. 0 <= lineIndex < getLineCount(), except
 * 	lineIndex may always be 0
 * @return offset offset of the first character of the line, relative to
 * 	the beginning of the document. The first character of the document is
 *	at offset 0.  
 *  When there are not any lines, getOffsetAtLine(0) is a valid call that 
 * 	answers 0.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when the offset is outside the valid range (< 0 or > getCharCount())</li> 
 * </ul>
 * @since 2.0
 */
public int getOffsetAtLine(int lineIndex) {
	checkWidget();
	
	if (lineIndex < 0 || 
		(lineIndex > 0 && lineIndex >= logicalContent.getLineCount())) {
		SWT.error(SWT.ERROR_INVALID_RANGE);		
	}
	return logicalContent.getOffsetAtLine(lineIndex);
}
/**
 * Returns the offset of the character at the given location relative 
 * to the first character in the document.
 * The return value reflects the character offset that the caret will
 * be placed at if a mouse click occurred at the specified location.
 * If the x coordinate of the location is beyond the center of a character
 * the returned offset will be behind the character.
 * <p>
 *
 * @param point the origin of character bounding box relative to 
 * 	the origin of the widget client area.
 * @return offset of the character at the given location relative 
 * 	to the first character in the document.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT when point is null</li>
 *   <li>ERROR_INVALID_ARGUMENT when there is no character at the specified location</li>
 * </ul>
 */
public int getOffsetAtLocation(Point point) {
	checkWidget();
	int line;
	int lineOffset;
	int offsetInLine;
	String lineText;
	
	if (point == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	// is y above first line or is x before first column?
	if (point.y + verticalScrollOffset < 0 || point.x + horizontalScrollOffset < 0) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}	
	line = (getTopPixel() + point.y) / lineHeight;	
	// does the referenced line exist?
	if (line >= content.getLineCount()) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}	
	lineText = content.getLine(line);
	lineOffset = content.getOffsetAtLine(line);	
	offsetInLine = getOffsetAtX(lineText, lineOffset, point.x);
	// is the x position within the line?
	if (offsetInLine == -1) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}
	return lineOffset + offsetInLine;
}
/**
 * Returns the offset at the specified x location in the specified line.
 * <p>
 *
 * @param x	x location of the mouse location
 * @param line	line the mouse location is in
 * @return the offset at the specified x location in the specified line,
 * 	relative to the beginning of the document
 */
int getOffsetAtMouseLocation(int x, int line) {
	String lineText = content.getLine(line);
	int lineOffset = content.getOffsetAtLine(line);
	int offsetInLine = getCaretOffsetAtX(lineText, lineOffset, x);
	return lineOffset + offsetInLine;
}
/**
 * Returns the offset of the character at the given x location in the line.
 * <p>
 *
 * @param line text of the line to calculate the offset in
 * @param lineOffset offset of the first character in the line. 
 * 	0 based from the beginning of the document.
 * @param lineXOffset x location in the line
 * @return offset of the character at the x location relative to the start 
 * 	of the line. -1 if the x location is past the end if the line.
 */
int getOffsetAtX(String line, int lineOffset, int lineXOffset) {
	GC gc = new GC(this);
	int offset;	
	
	lineXOffset += (horizontalScrollOffset - leftMargin);
	if (isBidi()) {
		StyledTextBidi bidi = getStyledTextBidi(line, lineOffset, gc);
		offset = bidi.getOffsetAtX(lineXOffset);
	}		
	else {
		FontData currentFont = gc.getFont().getFontData()[0];
		StyleRange[] styles = null;
		StyledTextEvent event = renderer.getLineStyleData(lineOffset, line);
					
		if (event != null) {
			styles = renderer.filterLineStyles(event.styles);
		}
		int low = -1;
		int high = line.length();
		while (high - low > 1) {
			offset = (high + low) / 2;
			// Restrict right/high search boundary only if x is within searched text segment.
			// Fixes 1GL4ZVE.			
			if (lineXOffset < renderer.textWidth(line, lineOffset, 0, offset + 1, styles, 0, gc, currentFont)) {
				high = offset;			
			}
			else 
			if (high == line.length() && high - offset == 1) {
				// requested x location is past end of line
				high = -1;
			}
			else {
				low = offset;
			}
		}
		offset = high;
	}
	gc.dispose();	
	return offset;	
}
/** 
 * Returns the index of the last partially visible line.
 *
 * @return index of the last partially visible line.
 */
int getPartialBottomIndex() {
	int partialLineCount = Compatibility.ceil(getClientArea().height, lineHeight);
	return Math.min(content.getLineCount(), topIndex + partialLineCount) - 1;
}
/**
 * Returns the content in the specified range using the platform line 
 * delimiter to separate lines.
 * <p>
 *
 * @param writer the TextWriter to write line text into
 * @return the content in the specified range using the platform line 
 * 	delimiter to separate lines as written by the specified TextWriter.
 */
String getPlatformDelimitedText(TextWriter writer) {
	int end = writer.getStart() + writer.getCharCount();
	int startLine = logicalContent.getLineAtOffset(writer.getStart());
	int endLine = logicalContent.getLineAtOffset(end);
	String endLineText = logicalContent.getLine(endLine);
	int endLineOffset = logicalContent.getOffsetAtLine(endLine);
	
	for (int i = startLine; i <= endLine; i++) {
		writer.writeLine(logicalContent.getLine(i), logicalContent.getOffsetAtLine(i));
		if (i < endLine) {
			writer.writeLineDelimiter(PlatformLineDelimiter);
		}
	}
	if (end > endLineOffset + endLineText.length()) {
		writer.writeLineDelimiter(PlatformLineDelimiter);
	}
	writer.close();
	return writer.toString();
}
/**
 * Returns the selection.
 * <p>
 * Text selections are specified in terms of caret positions.  In a text
 * widget that contains N characters, there are N+1 caret positions, 
 * ranging from 0..N
 * <p>
 *
 * @return start and end of the selection, x is the offset of the first 
 * 	selected character, y is the offset after the last selected character
 * @see #getSelectionRange
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point getSelection() {
	checkWidget();
	return new Point(selection.x, selection.y);
}
/**
 * Returns the selection.
 * <p>
 *
 * @return start and length of the selection, x is the offset of the 
 * 	first selected character, relative to the first character of the 
 * 	widget content. y is the length of the selection. 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Point getSelectionRange() {
	checkWidget();
	return new Point(selection.x, selection.y - selection.x);
}
/**
 * Gets the number of selected characters.
 * <p>
 *
 * @return the number of selected characters.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getSelectionCount() {
	checkWidget();
	return getSelectionRange().y;
}
/**
 * Returns the selected text.
 * <p>
 *
 * @return selected text, or an empty String if there is no selection.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public String getSelectionText() {
	checkWidget();
	return content.getTextRange(selection.x, selection.y - selection.x);
}
/**
 * Returns the text segments that should be treated as if they 
 * had a different direction than the surrounding text.
 * <p>
 *
 * @param lineOffset offset of the first character in the line. 
 * 	0 based from the beginning of the document.
 * @param line text of the line to specify bidi segments for
 * @return text segments that should be treated as if they had a
 * 	different direction than the surrounding text. Only the start 
 * 	index of a segment is specified, relative to the start of the 
 * 	line. Always starts with 0 and ends with the line length. 
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the segment indices returned 
 * 		by the listener do not start with 0, are not in ascending order,
 * 		exceed the line length or have duplicates</li>
 * </ul>
 */
int [] getBidiSegments(int lineOffset, String line) {
	if (isListening(LineGetSegments) == false) {
		return getBidiSegmentsCompatibility(line, lineOffset);
	}
	StyledTextEvent event = sendLineEvent(LineGetSegments, lineOffset, line);
	int lineLength = line.length();
	int[] segments;
	if (event == null || event.segments == null || event.segments.length == 0) {
		segments = new int[] {0, lineLength};
	}
	else {
		int segmentCount = event.segments.length;
		
		// test segment index consistency
		if (event.segments[0] != 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		} 	
		for (int i = 1; i < segmentCount; i++) {
			if (event.segments[i] <= event.segments[i - 1] || event.segments[i] > lineLength) {
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			} 	
		}
		// ensure that last segment index is line end offset
		if (event.segments[segmentCount - 1] != lineLength) {
			segments = new int[segmentCount + 1];
			System.arraycopy(event.segments, 0, segments, 0, segmentCount);
			segments[segmentCount] = lineLength;
		}
		else {
			segments = event.segments;
		}
	}
	return segments;
}
/**
 * @see getBidiSegments
 * Supports deprecated setBidiColoring API. Remove when API is removed.
 */
int [] getBidiSegmentsCompatibility(String line, int lineOffset) {
	StyledTextEvent event;
	StyleRange [] styles = new StyleRange [0];
	int lineLength = line.length();
	if (bidiColoring == false) {
		return new int[] {0, lineLength};
	}
	event = renderer.getLineStyleData(lineOffset, line);
	if (event != null) {
		styles = event.styles;
	}
	if (styles.length == 0) {
		return new int[] {0, lineLength};
	}
	int k=0, count = 1;
	while (k < styles.length && styles[k].start == 0 && styles[k].length == lineLength) {
		k++;
	}
	int[] offsets = new int[(styles.length - k) * 2 + 2];
	for (int i = k; i < styles.length; i++) {
		StyleRange style = styles[i];
		int styleLineStart = Math.max(style.start - lineOffset, 0);
		int styleLineEnd = Math.max(style.start + style.length - lineOffset, styleLineStart);
		styleLineEnd = Math.min (styleLineEnd, line.length ());
		if (i > 0 && count > 1 &&
			((styleLineStart >= offsets[count-2] && styleLineStart <= offsets[count-1]) ||
			 (styleLineEnd >= offsets[count-2] && styleLineEnd <= offsets[count-1])) &&
			 style.similarTo(styles[i-1])) {
			offsets[count-2] = Math.min(offsets[count-2], styleLineStart);
			offsets[count-1] = Math.max(offsets[count-1], styleLineEnd);
		} else {
			if (styleLineStart > offsets[count - 1]) {
				offsets[count] = styleLineStart;
				count++;
			}
			offsets[count] = styleLineEnd;
			count++;
		}
	}
	// add offset for last non-colored segment in line, if any
	if (lineLength > offsets[count-1]) {
		offsets [count] = lineLength;
		count++;
	}		
	if (count == offsets.length) {
		return offsets;
	}
	int [] result = new int [count];
	System.arraycopy (offsets, 0, result, 0, count);
	return result;
}
/**
 * Returns the style range at the given offset.
 * Returns null if a LineStyleListener has been set or if a style is not set
 * for the offset. 
 * Should not be called if a LineStyleListener has been set since the 
 * listener maintains the styles.
 * <p>
 *
 * @param offset the offset to return the style for. 
 * 	0 <= offset < getCharCount() must be true.
 * @return a StyleRange with start == offset and length == 1, indicating
 * 	the style at the given offset. null if a LineStyleListener has been set 
 * 	or if a style is not set for the given offset.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_ARGUMENT when the offset is invalid</li>
 * </ul>
 */
public StyleRange getStyleRangeAtOffset(int offset) {
	checkWidget();
	if (offset < 0 || offset >= getCharCount()) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	} 	
	if (userLineStyle == false) {
		return defaultLineStyler.getStyleRangeAtOffset(offset);
	} 
	return null;
}
/**
 * Returns the styles.
 * Returns an empty array if a LineStyleListener has been set. 
 * Should not be called if a LineStyleListener has been set since the 
 * listener maintains the styles.
 * <p>
 *
 * @return the styles or null if a LineStyleListener has been set.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public StyleRange [] getStyleRanges() {
	checkWidget();
	StyleRange styles[];
	
	if (userLineStyle == false) {
		styles = defaultLineStyler.getStyleRanges();
	}
	else {
		styles = new StyleRange[0];
	}
	return styles;
}
/**
 * Returns a StyledTextBidi object for the specified line.
 * <p>
 * 
 * @param lineText the line that the StyledTextBidi object should 
 * 	work on.
 * @param lineOffset offset of the beginning of the line, relative 
 * 	to the beginning of the document
 * @param gc GC to use when creating a new StyledTextBidi object.
 * @return a StyledTextBidi object for the specified line.
 */
StyledTextBidi getStyledTextBidi(String lineText, int lineOffset, GC gc) {
	return getStyledTextBidi(lineText, lineOffset, gc, null);
}
/**
 * Returns a StyledTextBidi object for the specified line.
 * <p>
 * 
 * @param lineText the line that the StyledTextBidi object should 
 * 	work on.
 * @param lineOffset offset of the beginning of the line, relative 
 * 	to the beginning of the document
 * @param gc GC to use when creating a new StyledTextBidi object.
 * @param styles StyleRanges to use when creating a new StyledTextBidi 
 * 	object.
 * @return a StyledTextBidi object for the specified line.
 */
StyledTextBidi getStyledTextBidi(String lineText, int lineOffset, GC gc, StyleRange[] styles) {
	return renderer.getStyledTextBidi(lineText, lineOffset, gc, styles);
}		
/**
 * Returns the tab width measured in characters.
 *
 * @return tab width measured in characters
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getTabs() {
	checkWidget();
	return tabLength;
}
/**
 * Returns a copy of the widget content.
 * <p>
 *
 * @return copy of the widget content
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public String getText() {
	checkWidget();
	return content.getTextRange(0, getCharCount());
}	
/**
 * Returns the widget content between the two offsets.
 * <p>
 *
 * @param start offset of the first character in the returned String
 * @param end offset of the last character in the returned String 
 * @return widget content starting at start and ending at end
 * @see #getTextRange(int,int)
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when start and/or end are outside the widget content</li> 
 * </ul>
 */
public String getText(int start, int end) {
	checkWidget();
	int contentLength = getCharCount();
	
	if (start < 0 || start >= contentLength || end < 0 || end >= contentLength || start > end) {
		SWT.error(SWT.ERROR_INVALID_RANGE);
	}	
	return content.getTextRange(start, end - start + 1);
}
/**
 * Returns the widget content starting at start for length characters.
 * <p>
 *
 * @param start offset of the first character in the returned String
 * @param length number of characters to return 
 * @return widget content starting at start and extending length characters.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when start and/or length are outside the widget content</li> 
 * </ul>
 */
public String getTextRange(int start, int length) {
	checkWidget();
	int contentLength = getCharCount();
	int end = start + length;
	
	if (start > end || start < 0 || end > contentLength) {
		SWT.error(SWT.ERROR_INVALID_RANGE);
	}	
	return content.getTextRange(start, length);
}
/**
 * Gets the text limit.  The text limit specifies the amount of text that the user 
 * can type into the widget.
 * <p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getTextLimit() {
	checkWidget();
	
	return textLimit;
}
/**
 * Gets the top index.  The top index is the index of the fully visible line that
 * is currently at the top of the widget.  The top index changes when the widget 
 * is scrolled. Indexing is zero based.
 * <p>
 *
 * @return the index of the top line
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getTopIndex() {
	checkWidget();
	int logicalTopIndex = topIndex;
	
	if (wordWrap) {
		int visualLineOffset = content.getOffsetAtLine(topIndex);
		logicalTopIndex = logicalContent.getLineAtOffset(visualLineOffset);
	}
	return logicalTopIndex;
}
/**
 * Gets the top pixel.  The top pixel is the pixel position of the line that is 
 * currently at the top of the widget.The text widget can be scrolled by pixels 
 * by dragging the scroll thumb so that a partial line may be displayed at the top 
 * the widget.  The top pixel changes when the widget is scrolled.  The top pixel 
 * does not include the widget trimming.
 * <p>
 *
 * @return pixel position of the top line
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public int getTopPixel() {
	checkWidget();
	return verticalScrollOffset;
}
/** 
 * Returns the vertical scroll increment.
 * <p>
 *
 * @return vertical scroll increment.
 */
int getVerticalIncrement() {
	return lineHeight;
}
/**
 * Returns the offset of the character after the word at the specified
 * offset.
 * <p>
 * There are two classes of words formed by a sequence of characters:
 * <ul>
 * <li>from 0-9 and A-z (ASCII 48-57 and 65-122)
 * <li>every other character except line breaks
 * </ul>
 * </p>
 * <p>
 * Space characters ' ' (ASCII 20) are special as they are treated as
 * part of the word leading up to the space character.  Line breaks are 
 * treated as one word.
 * </p>
 */
int getWordEnd(int offset) {
	int line = logicalContent.getLineAtOffset(offset);
	int lineOffset = logicalContent.getOffsetAtLine(line);
	String lineText = logicalContent.getLine(line);
	int lineLength = lineText.length();
	
	if (offset >= getCharCount()) {
		return offset;
	}
	if (offset == lineOffset + lineLength) {
		line++;
		offset = logicalContent.getOffsetAtLine(line);
	}
	else {
		offset -= lineOffset;
		char ch = lineText.charAt(offset);
		boolean letterOrDigit = Compatibility.isLetterOrDigit(ch);
		while (offset < lineLength - 1 && Compatibility.isLetterOrDigit(ch) == letterOrDigit) {
			offset++;
			ch = lineText.charAt(offset);
		}
		// skip over trailing whitespace
		while (offset < lineLength - 1 && Compatibility.isSpaceChar(ch)) {
			offset++;
			ch = lineText.charAt(offset);		
		}
		if (offset == lineLength - 1 && (Compatibility.isLetterOrDigit(ch) == letterOrDigit || Compatibility.isSpaceChar(ch))) {
			offset++;
		}
		offset += lineOffset;
	}
	return offset;
}
/**
 * Returns the offset of the character after the word at the specified
 * offset.
 * <p>
 * There are two classes of words formed by a sequence of characters:
 * <ul>
 * <li>from 0-9 and A-z (ASCII 48-57 and 65-122)
 * <li>every other character except line breaks
 * </ul>
 * </p>
 * <p>
 * Spaces are ignored and do not represent a word.  Line breaks are treated 
 * as one word.
 * </p>
 */
int getWordEndNoSpaces(int offset) {
	int line = logicalContent.getLineAtOffset(offset);
	int lineOffset = logicalContent.getOffsetAtLine(line);
	String lineText = logicalContent.getLine(line);
	int lineLength = lineText.length();
	
	if (offset >= getCharCount()) {
		return offset;
	}
	if (offset == lineOffset + lineLength) {
		line++;
		offset = logicalContent.getOffsetAtLine(line);
	}
	else {
		offset -= lineOffset;
		char ch = lineText.charAt(offset);
		boolean letterOrDigit = Compatibility.isLetterOrDigit(ch);
		
		while (offset < lineLength - 1 && Compatibility.isLetterOrDigit(ch) == letterOrDigit && Compatibility.isSpaceChar(ch) == false) {
			offset++;
			ch = lineText.charAt(offset);
		}
		if (offset == lineLength - 1 && Compatibility.isLetterOrDigit(ch) == letterOrDigit && Compatibility.isSpaceChar(ch) == false) {
			offset++;
		}
		offset += lineOffset;
	}
	return offset;
}
/**
 * Returns the start offset of the word at the specified offset.
 * There are two classes of words formed by a sequence of characters:
 * <p>
 * <ul>
 * <li>from 0-9 and A-z (ASCII 48-57 and 65-122)
 * <li>every other character except line breaks
 * </ul>
 * </p>
 * <p>
 * Space characters ' ' (ASCII 20) are special as they are treated as
 * part of the word leading up to the space character.  Line breaks are treated 
 * as one word.
 * </p>
 */
int getWordStart(int offset) {
	int line = logicalContent.getLineAtOffset(offset);
	int lineOffset = logicalContent.getOffsetAtLine(line);
	String lineText = logicalContent.getLine(line);
	
	if (offset <= 0) {
		return offset;
	}
	if (offset == lineOffset) {
		line--;
		lineText = logicalContent.getLine(line);
		offset = logicalContent.getOffsetAtLine(line) + lineText.length();
	}
	else {
		char ch;
		boolean letterOrDigit;
		
		offset -= lineOffset;
		// skip over trailing whitespace
		do {		
			offset--;
			ch = lineText.charAt(offset);
		} while (offset > 0 && Compatibility.isSpaceChar(ch));
		letterOrDigit = Compatibility.isLetterOrDigit(ch);
		while (offset > 0 && Compatibility.isLetterOrDigit(ch) == letterOrDigit && Compatibility.isSpaceChar(ch) == false) {
			offset--;
			ch = lineText.charAt(offset);
		}
		if (offset > 0 || Compatibility.isLetterOrDigit(ch) != letterOrDigit) {
			offset++;
		}
		offset += lineOffset;
	}
	return offset;
}
/**
 * Returns whether the widget wraps lines.
 * <p>
 *
 * @return true if widget wraps lines, false otherwise
 * @since 2.0
 */
public boolean getWordWrap() {
	checkWidget();
	return wordWrap;
}
/** 
 * Returns the x location of the character at the give offset in the line.
 * <b>NOTE:</b> Does not return correct values for true italic fonts (vs. slanted fonts).
 * <p>
 *
 * @return x location of the character at the given offset in the line.
 */
int getXAtOffset(String line, int lineIndex, int lineOffset) {
	int x;
	if (lineOffset == 0 && isBidi() == false) {
		x = leftMargin;
	}
	else {
		GC gc = new GC(this);		
		x = textWidth(line, lineIndex, Math.min(line.length(), lineOffset), gc) + leftMargin;
		gc.dispose();
		if (lineOffset > line.length()) {
			// offset is not on the line. return an x location one character 
			// after the line to indicate the line delimiter.
			x += lineEndSpaceWidth;
		}
	}
	return x - horizontalScrollOffset;
}
/** 
 * Inserts a string.  The old selection is replaced with the new text.  
 * <p>
 *
 * @param string the string
 * @see #replaceTextRange(int,int,String)
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when string is null</li>
 * </ul>
 */
public void insert(String string) {
	checkWidget();
	if (string == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	Point sel = getSelectionRange();
	replaceTextRange(sel.x, sel.y, string);
}
/**
 * Creates content change listeners and set the default content model.
 */
void installDefaultContent() {
	textChangeListener = new TextChangeListener() {
		public void textChanging(TextChangingEvent event) {
			handleTextChanging(event);
		}
		public void textChanged(TextChangedEvent event) {
			handleTextChanged(event);
		}
		public void textSet(TextChangedEvent event) {
			handleTextSet(event);
		}
	};
	logicalContent = content = new DefaultContent();
	content.addTextChangeListener(textChangeListener);
}
/**
 * Creates a default line style listener.
 * Used to store line background colors and styles.
 * Removed when the user sets a LineStyleListener.
 * <p>
 *
 * @see #addLineStyleListener
 */
void installDefaultLineStyler() {
	defaultLineStyler = new DefaultLineStyler(logicalContent);
	StyledTextListener typedListener = new StyledTextListener(defaultLineStyler);
	if (userLineStyle == false) {
		addListener(LineGetStyle, typedListener);
	}
	if (userLineBackground == false) {
		addListener(LineGetBackground, typedListener);
	}
}
/** 
 * Adds event listeners
 */
void installListeners() {
	ScrollBar verticalBar = getVerticalBar();
	ScrollBar horizontalBar = getHorizontalBar();
	
	addListener(SWT.Dispose, new Listener() {
		public void handleEvent(Event event) {
			handleDispose();
		}
	});
	addListener(SWT.KeyDown, new Listener() {
		public void handleEvent(Event event) {
			handleKeyDown(event);
		}
	});
	addListener(SWT.MouseDown, new Listener() {
		public void handleEvent(Event event) {
			handleMouseDown(event);
		}
	});
	addListener(SWT.MouseUp, new Listener() {
		public void handleEvent(Event event) {
			handleMouseUp(event);
		}
	});
	addListener(SWT.MouseDoubleClick, new Listener() {
		public void handleEvent(Event event) {
			handleMouseDoubleClick(event);
		}
	});
	addListener(SWT.MouseMove, new Listener() {
		public void handleEvent(Event event) {
			handleMouseMove(event);
		}
	});
	addListener(SWT.Paint, new Listener() {
		public void handleEvent(Event event) {
			handlePaint(event);
		}
	});
	addListener(SWT.Resize, new Listener() {
		public void handleEvent(Event event) {
			handleResize(event);
		}
	});
	addListener(SWT.Traverse, new Listener() {
		public void handleEvent(Event event) {
			handleTraverse(event);
		}
	});
	if (verticalBar != null) {
		verticalBar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				handleVerticalScroll(event);
			}
		});
	}
	if (horizontalBar != null) {
		horizontalBar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				handleHorizontalScroll(event);
			}
		});
	}
}
StyledTextContent internalGetContent() {
	return content;
}
int internalGetHorizontalPixel() {
	return horizontalScrollOffset;
}
int internalGetLastCaretDirection() {
	return lastCaretDirection;
}
LineCache internalGetLineCache() {
	return lineCache;
}
Point internalGetSelection() {
	return selection;
}
boolean internalGetWordWrap() {
	return wordWrap;
}
/**
 * Used by WordWrapCache to bypass StyledText.redraw which does
 * an unwanted cache reset.
 */
void internalRedraw() {
	super.redraw();
}
/** 
 * Redraws the specified text range.
 * <p>
 *
 * @param start offset of the first character to redraw
 * @param length number of characters to redraw
 * @param clearBackground true if the background should be cleared as 
 * 	part of the redraw operation.  If true, the entire redraw range will
 *  be cleared before anything is redrawn.  If the redraw range includes
 *	the last character of a line (i.e., the entire line is redrawn) the 
 * 	line is cleared all the way to the right border of the widget.
 *  The redraw operation will be faster and smoother if clearBackground is 
 * 	set to false.  Whether or not the flag can be set to false depends on 
 * 	the type of change that has taken place.  If font styles or background 
 * 	colors for the redraw range have changed, clearBackground should be 
 * 	set to true.  If only foreground colors have changed for the redraw 
 * 	range, clearBackground can be set to false. 
 */
void internalRedrawRange(int start, int length, boolean clearBackground) {
	int end = start + length;
	int firstLine = content.getLineAtOffset(start);
	int lastLine = content.getLineAtOffset(end);
	int offsetInFirstLine;
	int partialBottomIndex = getPartialBottomIndex();
	int partialTopIndex = verticalScrollOffset / lineHeight;
	// do nothing if redraw range is completely invisible	
	if (firstLine > partialBottomIndex || lastLine < partialTopIndex) {
		return;
	}
	// only redraw visible lines
	if (partialTopIndex > firstLine) {
		firstLine = partialTopIndex;
		offsetInFirstLine = 0;
	}
	else {
		offsetInFirstLine = start - content.getOffsetAtLine(firstLine);
	}
	if (partialBottomIndex + 1 < lastLine) {
		lastLine = partialBottomIndex + 1;	// + 1 to redraw whole bottom line, including line break
		end = content.getOffsetAtLine(lastLine);
	}
	// redraw first and last lines
	if (isBidi()) {
		redrawBidiLines(firstLine, offsetInFirstLine, lastLine, end, clearBackground);
	}	
	else {
		redrawLines(firstLine, offsetInFirstLine, lastLine, end, clearBackground);
	}
	// redraw entire center lines if redraw range includes more than two lines
	if (lastLine - firstLine > 1) {
		Rectangle clientArea = getClientArea();
		int redrawStopY = lastLine * lineHeight - verticalScrollOffset;		
		int redrawY = (firstLine + 1) * lineHeight - verticalScrollOffset;				
		draw(0, redrawY, clientArea.width, redrawStopY - redrawY, clearBackground);
	}
}
/**
 * Returns the widget text with style information encoded using RTF format
 * specification version 1.5.
 *
 * @return the widget text with style information encoded using RTF format
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
String getRtf(){
	checkWidget();
	RTFWriter rtfWriter = new RTFWriter(0, getCharCount());
	return getPlatformDelimitedText(rtfWriter);
}
/** 
 * Frees resources.
 */
void handleDispose() {
	clipboard.dispose();
	ibeamCursor.dispose();
	if (renderer != null) {
		renderer.dispose();
		renderer = null;
	}
	if (content != null) {
		content.removeTextChangeListener(textChangeListener);
	}	
	if (leftCaretBitmap != null) {
		leftCaretBitmap.dispose();
		leftCaretBitmap = null;
	}
	if (rightCaretBitmap != null) {
		rightCaretBitmap.dispose();
		rightCaretBitmap = null;
	}
	if (isBidi()) {
		StyledTextBidi.removeLanguageListener(this);
	}
}
/** 
 * Updates the caret location and selection if mouse button 1 has been 
 * pressed.
 */
void handleMouseDoubleClick(Event event) {
	if (event.button != 1 || doubleClickEnabled == false) {
		return;
	}
	event.y -= topMargin;
	mouseDoubleClick = true;
	caretOffset = getWordEndNoSpaces(caretOffset);
	resetSelection();
	caretOffset = getWordStart(caretOffset);
	caretLine = content.getLineAtOffset(caretOffset);
	showCaret();
	doMouseSelection();
}
/** 
 * Updates the caret location and selection if mouse button 1 has been 
 * pressed.
 */
void handleMouseDown(Event event) {
	boolean select = (event.stateMask & SWT.SHIFT) != 0;
	
	if (event.button != 1) {
		return;
	}
	mouseDoubleClick = false;
	event.y -= topMargin;
	if (isBidi()) {
		doBidiMouseLocationChange(event.x, event.y, select);
	}
	else {	
		doMouseLocationChange(event.x, event.y, select);
	}
}
/** 
 * Autoscrolling ends when the mouse button is released.
 */
void handleMouseUp(Event event) {
	event.y -= topMargin;
	endAutoScroll();
}
/** 
 * Updates the caret location and selection if mouse button 1 is pressed 
 * during the mouse move.
 */
void handleMouseMove(Event event) {
	if (mouseDoubleClick == true || (event.stateMask & SWT.BUTTON1) == 0) {
		return;
	}
	event.y -= topMargin;
	if (isBidi()) {
		doBidiMouseLocationChange(event.x, event.y, true);
	}
	else {	
		doMouseLocationChange(event.x, event.y, true);
	}
	doAutoScroll(event);
}
/** 
 * Scrolls the widget horizontally.
 */
void handleHorizontalScroll(Event event) {
	int scrollPixel = getHorizontalBar().getSelection() - horizontalScrollOffset;
	scrollHorizontal(scrollPixel);
}
/**
 * If a VerifyKey listener exists, verify that the key that was entered
 * should be processed.
 * <p>
 *
 * @param event keyboard event
 */
void handleKeyDown(Event event) {
	Event verifyEvent = new Event();
	
	verifyEvent.character = event.character;
	verifyEvent.keyCode = event.keyCode;
	verifyEvent.stateMask = event.stateMask;
	verifyEvent.doit = true;		
	notifyListeners(VerifyKey, verifyEvent);
	if (verifyEvent.doit == true) {
		handleKey(event);
	}
}
/**
 * If an action has been registered for the key stroke execute the action.
 * Otherwise, if a character has been entered treat it as new content.
 * <p>
 *
 * @param event keyboard event
 */
void handleKey(Event event) {
	int action;
	
	if (event.keyCode != 0) {
		action = getKeyBinding(event.keyCode | event.stateMask);
	}
	else {
		action = getKeyBinding(event.character | event.stateMask);
	}
	if (action == SWT.NULL) {
		// ignore anything below SPACE and ignore DEL
		if (event.character > 31 && 
			event.character != SWT.DEL && event.stateMask != SWT.ALT || 
		    event.character == SWT.CR || event.character == SWT.LF || 
		    event.character == TAB) {
			doContent(event.character);
		}
	}
	else {
		invokeAction(action);		
	}
}
/**
 * Renders the invalidated area specified in the paint event.
 * <p>
 *
 * @param event paint event
 */
void handlePaint(Event event) {
	int startLine = Math.max(0, (event.y - topMargin + verticalScrollOffset) / lineHeight);
	int paintYFromTopLine = (startLine - topIndex) * lineHeight;
	int topLineOffset = topIndex * lineHeight - verticalScrollOffset;
	int startY = paintYFromTopLine + topLineOffset;	// adjust y position for pixel based scrolling
	int renderHeight = event.y + event.height - startY;
	Rectangle clientArea = getClientArea();
	
	// Check if there is work to do. clientArea.width should never be 0
	// if we receive a paint event but we never want to try and create 
	// an Image with 0 width.
	if (clientArea.width == 0 || event.height == 0) {		
		return;
	}
	performPaint(event.gc, startLine, startY, renderHeight);	
}	
/**
 * Render the specified area.  Broken out as its own method to support
 * direct drawing.
 * <p>
 *
 * @param gc GC to render on 
 * @param startLine first line to render
 * @param startY y pixel location to start rendering at
 * @param renderHeight renderHeight widget area that needs to be filled with lines
 */
void performPaint(GC gc,int startLine,int startY, int renderHeight)	{
	int lineCount = content.getLineCount();
	int paintY = topMargin;	
	Rectangle clientArea = getClientArea();
	Color background = getBackground();
	Color foreground = getForeground();
	Font font = gc.getFont();
	FontData fontData = font.getFontData()[0];
	
	// Check if there is work to do. We never want to try and create 
	// an Image with 0 width or 0 height.
	if (clientArea.width == 0 || renderHeight == 0) {		
		return;
	}
	if (isSingleLine()) {
		lineCount = 1;
		if (startLine > 1) {
			startLine = 1;
		}
	}
	Image lineBuffer = new Image(getDisplay(), clientArea.width, renderHeight);
	GC lineGC = new GC(lineBuffer);	

	lineGC.setFont(font);
	lineGC.setForeground(foreground);
	lineGC.setBackground(background);
	for (int i = startLine; paintY < renderHeight && i < lineCount; i++, paintY += lineHeight) {
		String line = content.getLine(i);
		renderer.drawLine(line, i, paintY, lineGC, background, foreground, fontData, true);
	}
	if (paintY < renderHeight) {
		lineGC.setBackground(background);
		lineGC.setForeground(background);
		lineGC.fillRectangle(0, paintY, clientArea.width, renderHeight - paintY);
	}
	gc.drawImage(lineBuffer, 0, startY);
	lineGC.dispose();
	lineBuffer.dispose();
	clearMargin(gc, background, clientArea, renderHeight);
}
/**
 * Recalculates the scroll bars. Rewraps all lines when in word 
 * wrap mode.
 * <p>
 *
 * @param event resize event
 */
void handleResize(Event event) {
	int oldHeight = clientAreaHeight;
	int oldWidth = clientAreaWidth;
	clientAreaHeight = getClientArea().height;
	clientAreaWidth = getClientArea().width;
	if (wordWrap) {
	    if (oldWidth != clientAreaWidth) {	
	    	wordWrapResize(oldWidth);
	    }
	}
	else
	if (clientAreaHeight > oldHeight) {
		int lineCount = content.getLineCount();
		int oldBottomIndex = topIndex + oldHeight / lineHeight;
		int newItemCount = Compatibility.ceil(clientAreaHeight - oldHeight, lineHeight);
		
		oldBottomIndex = Math.min(oldBottomIndex, lineCount);
		newItemCount = Math.min(newItemCount, lineCount - oldBottomIndex);
		lineCache.calculate(oldBottomIndex, newItemCount);
	}	
	setScrollBars();
	claimBottomFreeSpace();
	claimRightFreeSpace();	
}
/**
 * Updates the caret position and selection and the scroll bars to reflect 
 * the content change.
 * <p>
 */
void handleTextChanged(TextChangedEvent event) {
	lineCache.textChanged(lastTextChangeStart, 
		lastTextChangeNewLineCount, 
		lastTextChangeReplaceLineCount,
		lastTextChangeNewCharCount,
		lastTextChangeReplaceCharCount);
	setScrollBars();
	// update selection/caret location after styles have been changed.
	// otherwise any text measuring could be incorrect
	// 
	// also, this needs to be done after all scrolling. Otherwise, 
	// selection redraw would be flushed during scroll which is wrong.
	// in some cases new text would be drawn in scroll source area even 
	// though the intent is to scroll it.
	// fixes 1GB93QT
	updateSelection(
		lastTextChangeStart, 
		lastTextChangeReplaceCharCount, 
		lastTextChangeNewCharCount);
		
	if (lastTextChangeReplaceLineCount > 0) {
		// Only check for unused space when lines are deleted.
		// Fixes 1GFL4LY
		// Scroll up so that empty lines below last text line are used.
		// Fixes 1GEYJM0
		claimBottomFreeSpace();
	}
	if (lastTextChangeReplaceCharCount > 0) {
		// fixes bug 8273
		claimRightFreeSpace();
	}
	// do direct drawing if the text change is confined to a single line.
	// optimization and fixes bug 13999. see also handleTextChanging.
	if (lastTextChangeNewLineCount == 0 && lastTextChangeReplaceLineCount == 0) {
		int startLine = content.getLineAtOffset(lastTextChangeStart);
		int startY = startLine * lineHeight - verticalScrollOffset;
		GC gc = new GC(this);
		Caret caret = getCaret();
		boolean caretVisible = caret.getVisible();
		
		caret.setVisible(false);
		performPaint(gc, startLine, startY, lineHeight);
		caret.setVisible(caretVisible);
		gc.dispose();
	}
}
/**
 * Updates the screen to reflect a pending content change.
 * <p>
 *
 * @param event.start the start offset of the change
 * @param event.newText text that is going to be inserted or empty String 
 *	if no text will be inserted
 * @param event.replaceCharCount length of text that is going to be replaced
 * @param event.newCharCount length of text that is going to be inserted
 * @param event.replaceLineCount number of lines that are going to be replaced
 * @param event.newLineCount number of new lines that are going to be inserted
 */
void handleTextChanging(TextChangingEvent event) {
	int firstLine;	
	int textChangeY;
	boolean isMultiLineChange = event.replaceLineCount > 0 || event.newLineCount > 0;
			
	if (event.replaceCharCount < 0) {
		event.start += event.replaceCharCount;
		event.replaceCharCount *= -1;
	}
	lastTextChangeStart = event.start;
	lastTextChangeNewLineCount = event.newLineCount;
	lastTextChangeNewCharCount = event.newCharCount;
	lastTextChangeReplaceLineCount = event.replaceLineCount;
	lastTextChangeReplaceCharCount = event.replaceCharCount;
	firstLine = content.getLineAtOffset(event.start);
	textChangeY = firstLine * lineHeight - verticalScrollOffset + topMargin;
	if (isMultiLineChange) {
		redrawMultiLineChange(textChangeY, event.newLineCount, event.replaceLineCount);
	}
	// notify default line styler about text change
	if (defaultLineStyler != null) {
		defaultLineStyler.textChanging(event);
	}
}
/**
 * Called when the widget content is set programatically, overwriting 
 * the old content. Resets the caret position, selection and scroll offsets. 
 * Recalculates the content width and scroll bars. Redraws the widget.
 * <p>
 *
 * @param event text change event. 
 */
void handleTextSet(TextChangedEvent event) {
	reset();
}
/**
 * Called when a traversal key is pressed.
 * Allow tab next traversal to occur when the widget is in single 
 * line mode. 
 * When in multi line mode we want to prevent the tab traversal 
 * and receive the tab key event instead.
 * <p>
 *
 * @param event the event
 */
void handleTraverse(Event event) {
	if (isSingleLine() && event.detail == SWT.TRAVERSE_TAB_NEXT) {
		event.doit = true;
	}
}
/** 
 * Scrolls the widget vertically.
 */
void handleVerticalScroll(Event event) {
	setVerticalScrollOffset(getVerticalBar().getSelection(), false);
}
/** 
 * Initializes the fonts used to render font styles.
 * Presently only regular and bold fonts are supported.
 */
void initializeRenderer() {
	if (renderer != null) {
		renderer.dispose();
	}
	renderer = new DisplayRenderer(
		getDisplay(), getFont(), isBidi(), leftMargin, this, tabLength);
	lineHeight = renderer.getLineHeight();
	lineEndSpaceWidth = renderer.getLineEndSpaceWidth();
}
/**
 * Executes the action.
 * <p>
 *
 * @param action one of the actions defined in ST.java
 */
public void invokeAction(int action) {
	checkWidget();	
	switch (action) {
		// Navigation
		case ST.LINE_UP:
			doLineUp();
			clearSelection(true);
			break;
		case ST.LINE_DOWN:
			doLineDown();
			clearSelection(true);
			break;
		case ST.LINE_START:
			doLineStart();
			clearSelection(true);
			break;
		case ST.LINE_END:
			doLineEnd();
			clearSelection(true);
			break;
		case ST.COLUMN_PREVIOUS:
			doCursorPrevious();
			clearSelection(true);
			break;
		case ST.COLUMN_NEXT:
			doCursorNext();
			clearSelection(true);
			break;
		case ST.PAGE_UP:
			doPageUp();
			clearSelection(true);
			break;
		case ST.PAGE_DOWN:
			doPageDown(false);
			clearSelection(true);
			break;
		case ST.WORD_PREVIOUS:
			doWordPrevious();
			clearSelection(true);
			break;
		case ST.WORD_NEXT:
			doWordNext();
			clearSelection(true);
			break;
		case ST.TEXT_START:
			doContentStart();
			clearSelection(true);
			break;
		case ST.TEXT_END:
			doContentEnd();
			clearSelection(true);
			break;
		case ST.WINDOW_START:
			doPageStart();
			clearSelection(true);
			break;
		case ST.WINDOW_END:
			doPageEnd();
			clearSelection(true);
			break;
		// Selection	
		case ST.SELECT_LINE_UP:
			doLineUp();
			doSelection(SWT.LEFT);
			break;
		case ST.SELECT_LINE_DOWN:
			doSelectionLineDown();
			// select first and then scroll to reduce flash when key 
			// repeat scrolls lots of lines
			doSelection(SWT.RIGHT);
			showCaret();
			break;
		case ST.SELECT_LINE_START:
			doLineStart();
			doSelection(SWT.LEFT);
			break;
		case ST.SELECT_LINE_END:
			doLineEnd();
			doSelection(SWT.RIGHT);
			break;
		case ST.SELECT_COLUMN_PREVIOUS:
			doSelectionCursorPrevious();
			doSelection(SWT.LEFT);
			break;
		case ST.SELECT_COLUMN_NEXT:
			doSelectionCursorNext();
			doSelection(SWT.RIGHT);
			break;
		case ST.SELECT_PAGE_UP:
			doPageUp();
			doSelection(SWT.LEFT);
			break;
		case ST.SELECT_PAGE_DOWN:
			doPageDown(true);
			break;
		case ST.SELECT_WORD_PREVIOUS:
			doSelectionWordPrevious();
			doSelection(SWT.LEFT);
			break;
		case ST.SELECT_WORD_NEXT:
			doSelectionWordNext();
			doSelection(SWT.RIGHT);
			break;
		case ST.SELECT_TEXT_START:
			doContentStart();
			doSelection(SWT.LEFT);
			break;
		case ST.SELECT_TEXT_END:
			doContentEnd();
			doSelection(SWT.RIGHT);
			break;
		case ST.SELECT_WINDOW_START:
			doPageStart();
			doSelection(SWT.LEFT);
			break;
		case ST.SELECT_WINDOW_END:
			doPageEnd();
			doSelection(SWT.RIGHT);
			break;
		// Modification			
		case ST.CUT:
			cut();
			break;
		case ST.COPY:
			copy();
			break;
		case ST.PASTE:
			paste();
			break;
		case ST.DELETE_PREVIOUS:
			doBackspace();
			break;
		case ST.DELETE_NEXT:
			doDelete();
			break;
		// Miscellaneous
		case ST.TOGGLE_OVERWRITE:
			overwrite = !overwrite;		// toggle insert/overwrite mode
			break;
	}
}
/**
 * Temporary until SWT provides this
 */
boolean isBidi() {
	return isBidi;
}
/**
 * Returns whether the given offset is inside a multi byte line delimiter.
 * Example: 
 * "Line1\r\n" isLineDelimiter(5) == false but isLineDelimiter(6) == true
 * 
 * @return true if the given offset is inside a multi byte line delimiter.
 * false if the given offset is before or after a line delimiter.
 */
boolean isLineDelimiter(int offset) {
	int line = content.getLineAtOffset(offset);
	int lineOffset = content.getOffsetAtLine(line);	
	int offsetInLine = offset - lineOffset;
	// offsetInLine will be greater than line length if the line 
	// delimiter is longer than one character and the offset is set
	// in between parts of the line delimiter.
	return offsetInLine > content.getLine(line).length();
}
/**
 * Returns whether or not the given lines are visible.
 * <p>
 *
 * @return true if any of the lines is visible
 * false if none of the lines is visible
 */
boolean isAreaVisible(int firstLine, int lastLine) {
	int partialBottomIndex = getPartialBottomIndex();
	int partialTopIndex = verticalScrollOffset / lineHeight;
	boolean notVisible = firstLine > partialBottomIndex || lastLine < partialTopIndex;
	return !notVisible;
}
/**
 * Returns whether or not the given styles will necessitate a redraw for the given start line.  
 * A redraw is necessary when font style changes after the start of a style will take place.  
 * This method assumes ranges is in order and non-overlapping.
 * <p>
 *
 * @return true if a redraw of the given line is necessary, false otherwise
 */
boolean isRedrawFirstLine(StyleRange[] ranges, int firstLine, int firstLineOffset) {
	int lineEnd = firstLineOffset + content.getLine(firstLine).length();
	for (int i=0; i<ranges.length; i++) {
		StyleRange range = ranges[i];
		// does the style start on the first line?
		if (range.start < lineEnd) {
			int rangeEnd = range.start + range.length;
			if (isStyleChanging(range, range.start, Math.min(rangeEnd, lineEnd))) return true;
		} else {
			return false;
		}
	}
	return false;
}
/**
 * Returns whether or not the given styles will necessitate a redraw for the given end line.  
 * A redraw is necessary when font style changes after the start of a style will take place.  
 * This method assumes ranges is in order and non-overlapping.
 * <p>
 *
 * @return true if a redraw of the last line is necessary, false otherwise
 */
boolean isRedrawLastLine(StyleRange[] ranges, int lastLine, int lastLineOffset) {
	for (int i = ranges.length - 1; i >= 0; i--) {
		StyleRange range = ranges[i];
		int rangeEnd = range.start + range.length;
		// does style range end on the last line?
		if (rangeEnd >= lastLineOffset) {
			if (isStyleChanging(range, Math.max(range.start, lastLineOffset), rangeEnd)) return true;
		} else {
			break;
		}
	}
	return false;
}
/**
 * Returns whether the widget can have only one line.
 * <p>
 *
 * @return true if widget can have only one line, false if widget can have 
 * 	multiple lines
 */
boolean isSingleLine() {
	return (getStyle() & SWT.SINGLE) != 0;
}
/** 
 * Returns whether the font style in the given style range is changing 
 * from SWT.NORMAL to SWT.BOLD or vice versa.
 * <p>
 *
 * @param range StyleRange to compare current font style with.
 * @param start offset of the first font style to compare 
 * @param end offset behind the last font style to compare
 * @return true if the font style is changing in the given style range,
 * 	false if the font style is not changing in the given style range.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
boolean isStyleChanging(StyleRange range, int start, int end) {
	checkWidget();
	StyleRange[] styles = defaultLineStyler.getStyleRangesFor(start, end - start);
	
	if (styles == null) {
		return (range.fontStyle != SWT.NORMAL);
	}
	for (int i = 0; i < styles.length; i++) {
		StyleRange newStyle = styles[i];
		if (newStyle.fontStyle != range.fontStyle) {
			return true;
		}
	}
	return false;
}
/**
 * Sends the specified verify event, replace/insert text as defined by 
 * the event and send a modify event.
 * <p>
 *
 * @param event	the text change event. 
 *	<ul>
 *	<li>event.start - the replace start offset</li>
 * 	<li>event.end - the replace end offset</li>
 * 	<li>event.text - the new text</li>
 *	</ul>
 * @param updateCaret whether or not he caret should be set behind
 *	the new text
 */
void modifyContent(Event event, boolean updateCaret) {
	event.doit = true;
	notifyListeners(SWT.Verify, event);
	if (event.doit) {
		StyledTextEvent styledTextEvent = null;
		int replacedLength = event.end - event.start;
		boolean isCharacterRemove = replacedLength == 1 && event.text.length() == 0;
		boolean isBackspace = event.start < caretOffset;
		boolean isDirectionBoundary = false;
				
		if (updateCaret && isBidi() && isCharacterRemove) {
			// set the keyboard language to the language of the deleted character.
			// determine direction boundary so that caret location can be updated 
			// properly.
			int line = content.getLineAtOffset(caretOffset);
			int lineStartOffset = content.getOffsetAtLine(line);		
			int offsetInLine = caretOffset - lineStartOffset;
			String lineText = content.getLine(line);
			GC gc = new GC(this);
			StyledTextBidi bidi = new StyledTextBidi(gc, lineText, getBidiSegments(lineStartOffset, lineText));			
			if (isBackspace) {
				if (offsetInLine > 0) {
					// the line start/end does not represent a direction boundary 
					// even if the previous/next line has a different direction.
					isDirectionBoundary = 
						offsetInLine < lineText.length() && 
						(bidi.isRightToLeft(offsetInLine) != bidi.isRightToLeft(offsetInLine - 1) || 
						 bidi.isLocalNumber(offsetInLine) != bidi.isLocalNumber(offsetInLine - 1));
					bidi.setKeyboardLanguage(offsetInLine - 1);
				}
			}
			else {
				if (offsetInLine < lineText.length()) {
					// the line start/end does not represent a direction boundary 
					// even if the previous/next line has a different direction.
					isDirectionBoundary = 
						offsetInLine > 0 && 
						(bidi.isRightToLeft(offsetInLine) != bidi.isRightToLeft(offsetInLine - 1) || 
						 bidi.isLocalNumber(offsetInLine) != bidi.isLocalNumber(offsetInLine - 1));
					bidi.setKeyboardLanguage(offsetInLine);
				}
			}
			gc.dispose();
		}						
		if (isListening(ExtendedModify)) {
			styledTextEvent = new StyledTextEvent(logicalContent);
			styledTextEvent.start = event.start;
			styledTextEvent.end = event.start + event.text.length();
			styledTextEvent.text = content.getTextRange(event.start, replacedLength);
		}
		content.replaceTextRange(event.start, replacedLength, event.text);
		// set the caret position prior to sending the modify event.
		// fixes 1GBB8NJ
		if (updateCaret) {		
			// always update the caret location. fixes 1G8FODP
			internalSetSelection(event.start + event.text.length(), 0, true);
			if (isBidi()) {
				// Update the caret direction so that the caret moves to the 
				// typed/deleted character. Fixes 1GJLQ16.
				if (isCharacterRemove) {
					updateBidiDirection(isBackspace, isDirectionBoundary);
				}
				else {
					lastCaretDirection = ST.COLUMN_NEXT;
				}
				showBidiCaret();
			}
			else {
				showCaret();
			}
		}	
		notifyListeners(SWT.Modify, event);		
		if (isListening(ExtendedModify)) {
			notifyListeners(ExtendedModify, styledTextEvent);
		}
	}
}
/** 
 * Replaces the selection with the clipboard text or insert the text at 
 * the current caret offset if there is no selection. 
 * If the widget has the SWT.SINGLE style and the clipboard text contains
 * more than one line, only the first line without line delimiters is 
 * inserted in the widget.
 * <p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void paste(){
	checkWidget();	
	TextTransfer transfer = TextTransfer.getInstance();
	String text;
	text = (String) clipboard.getContents(transfer);
	if (text != null && text.length() > 0) {
		Event event = new Event();
		event.start = selection.x;
		event.end = selection.y;
		event.text = getModelDelimitedText(text);
		sendKeyEvent(event);
	}
}
/** 
 * Prints the widget's text to the default printer.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void print() {
	checkWidget();
	Printer printer = new Printer();
	new Printing(this, printer).run();
	printer.dispose();
}
/** 
 * Returns a runnable that will print the widget's text
 * to the specified printer.
 * <p>
 * The runnable may be run in a non-UI thread.
 * </p>
 * 
 * @param printer the printer to print to
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when string is null</li>
 * </ul>
 */
public Runnable print(Printer printer) {
	checkWidget();
	if (printer == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	return new Printing(this, printer);
}
/**
 * Causes the entire bounds of the receiver to be marked
 * as needing to be redrawn. The next time a paint request
 * is processed, the control will be completely painted.
 * <p>
 * Recalculates the content width for all lines in the bounds.
 * When a <code>LineStyleListener</code> is used a redraw call 
 * is the only notification to the widget that styles have changed 
 * and that the content width may have changed.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Control#update
 */
public void redraw() {
	int itemCount;
	
	super.redraw();
	itemCount = getPartialBottomIndex() - topIndex + 1;
	lineCache.redrawReset(topIndex, itemCount, true);
	lineCache.calculate(topIndex, itemCount);
	setHorizontalScrollBar();
}
/**
 * Causes the rectangular area of the receiver specified by
 * the arguments to be marked as needing to be redrawn. 
 * The next time a paint request is processed, that area of
 * the receiver will be painted. If the <code>all</code> flag
 * is <code>true</code>, any children of the receiver which
 * intersect with the specified area will also paint their
 * intersecting areas. If the <code>all</code> flag is 
 * <code>false</code>, the children will not be painted.
 * <p>
 * Marks the content width of all lines in the specified rectangle
 * as unknown. Recalculates the content width of all visible lines.
 * When a <code>LineStyleListener</code> is used a redraw call 
 * is the only notification to the widget that styles have changed 
 * and that the content width may have changed.
 * </p>
 *
 * @param x the x coordinate of the area to draw
 * @param y the y coordinate of the area to draw
 * @param width the width of the area to draw
 * @param height the height of the area to draw
 * @param all <code>true</code> if children should redraw, and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Control#update
 */
public void redraw(int x, int y, int width, int height, boolean all) {
	super.redraw(x, y, width, height, all);
	if (height > 0) {
		int lineCount = content.getLineCount();
		int startLine = (getTopPixel() + y) / lineHeight;
		int endLine = startLine + Compatibility.ceil(height, lineHeight);
		int itemCount;
		
		// reset all lines in the redraw rectangle
		startLine = Math.min(startLine, lineCount);
		itemCount = Math.min(endLine, lineCount) - startLine;
		lineCache.reset(startLine, itemCount, true);
		// only calculate the visible lines
		itemCount = getPartialBottomIndex() - topIndex + 1;
		lineCache.calculate(topIndex, itemCount);
		setHorizontalScrollBar();
	}
}
/** 
 * Redraws a text range in the specified lines
 * <p>
 *
 * @param firstLine first line to redraw at the specified offset
 * @param offsetInFirstLine offset in firstLine to start redrawing
 * @param lastLine last line to redraw
 * @param endOffset offset in the last where redrawing should stop
 * @param clearBackground true=clear the background by invalidating
 *  the requested redraw range, false=draw the foreground directly
 *  without invalidating the redraw range.
 */
void redrawBidiLines(int firstLine, int offsetInFirstLine, int lastLine, int endOffset, boolean clearBackground) {
	int lineCount = lastLine - firstLine + 1;
	int redrawY = firstLine * lineHeight - verticalScrollOffset;
	int firstLineOffset = content.getOffsetAtLine(firstLine);
	String line = content.getLine(firstLine);
	GC gc = new GC(this);
	StyledTextBidi bidi = getStyledTextBidi(line, firstLineOffset, gc);
		
	bidi.redrawRange(
		this, offsetInFirstLine, 
		Math.min(line.length(), endOffset) - offsetInFirstLine, 
		leftMargin - horizontalScrollOffset, redrawY + topMargin, lineHeight);
	// redraw line break marker (either space or full client area width)
	// if redraw range extends over more than one line and background should be redrawn
	if (lastLine > firstLine && clearBackground) {
		int lineBreakWidth;		
		int lineBreakStartX = bidi.getTextWidth();
		// handle empty line case
		if (lineBreakStartX == leftMargin) {
			lineBreakStartX += XINSET;
		}
		lineBreakStartX = lineBreakStartX - horizontalScrollOffset;
		if ((getStyle() & SWT.FULL_SELECTION) != 0) {
			lineBreakWidth = getClientArea().width - lineBreakStartX;
		}
		else {
			lineBreakWidth = lineEndSpaceWidth;
		}
		draw(lineBreakStartX, redrawY, lineBreakWidth, lineHeight, clearBackground);
	}
	// redraw last line if more than one line needs redrawing 
	if (lineCount > 1) {
		int lastLineOffset = content.getOffsetAtLine(lastLine);
		int offsetInLastLine = endOffset - lastLineOffset;	
		// no redraw necessary if redraw offset is 0
		if (offsetInLastLine > 0) {				
			line = content.getLine(lastLine);
			redrawY = lastLine * lineHeight - verticalScrollOffset;		
			bidi = getStyledTextBidi(line, lastLineOffset, gc);
			bidi.redrawRange(
				this, 0, offsetInLastLine, 
				leftMargin - horizontalScrollOffset, 
				redrawY + topMargin, lineHeight);
		}
	}
	gc.dispose();
}
/** 
 * Redraw the given line.
 * <p>
 *
 * @param line index of the line to redraw
 * @param offset offset in line to start redrawing
 */
void redrawLine(int line, int offset) {
	int redrawX = 0;
	if (offset > 0) {
		String lineText = content.getLine(line);
		redrawX = getXAtOffset(lineText, line, offset);
	}
	int redrawY = line * lineHeight - verticalScrollOffset;
	super.redraw(
		redrawX + leftMargin, redrawY + topMargin, 
		getClientArea().width, lineHeight, true);
}
/** 
 * Redraws a text range in the specified lines
 * <p>
 *
 * @param firstLine first line to redraw at the specified offset
 * @param offsetInFirstLine offset in firstLine to start redrawing
 * @param lastLine last line to redraw
 * @param endOffset offset in the last where redrawing should stop
 * @param clearBackground true=clear the background by invalidating
 *  the requested redraw range. If the redraw range includes the 
 * 	last character of a line (i.e., the entire line is redrawn) the 
 * 	line is cleared all the way to the right border of the widget.
 *  false=draw the foreground directly without invalidating the 
 * 	redraw range.
 */
void redrawLines(int firstLine, int offsetInFirstLine, int lastLine, int endOffset, boolean clearBackground) {
	String line = content.getLine(firstLine);
	int lineCount = lastLine - firstLine + 1;
	int redrawX = getXAtOffset(line, firstLine, offsetInFirstLine) - leftMargin;
	int redrawStopX;
	int redrawY = firstLine * lineHeight - verticalScrollOffset;
	int firstLineOffset = content.getOffsetAtLine(firstLine);
	boolean fullLineRedraw = ((getStyle() & SWT.FULL_SELECTION) != 0 && lastLine > firstLine);

	// if redraw range includes last character on the first line, 
	// clear background to right widget border. fixes bug 19595.
	if (clearBackground && endOffset - firstLineOffset >= line.length()) {
		fullLineRedraw = true;
	}
	// calculate redraw stop location
	if (fullLineRedraw) {
		redrawStopX = getClientArea().width - leftMargin;
	}
	else {
		redrawStopX = getXAtOffset(line, firstLine, endOffset - firstLineOffset) - leftMargin;
	}
	draw(redrawX, redrawY, redrawStopX - redrawX, lineHeight, clearBackground);
	// redraw last line if more than one line needs redrawing 
	if (lineCount > 1) {
		int offsetInLastLine = endOffset - content.getOffsetAtLine(lastLine);	
		// no redraw necessary if redraw offset is 0
		if (offsetInLastLine > 0) {
			line = content.getLine(lastLine);
			// if redraw range includes last character on the last line, 
			// clear background to right widget border. fixes bug 19595.
			if (clearBackground && offsetInLastLine >= line.length()) {
				fullLineRedraw = true;
			}
			if (fullLineRedraw) {
				redrawStopX = getClientArea().width - leftMargin;
			}
			else {
				redrawStopX = getXAtOffset(line, lastLine, offsetInLastLine) - leftMargin;
			}
			redrawY = lastLine * lineHeight - verticalScrollOffset;
			draw(0, redrawY, redrawStopX, lineHeight, clearBackground);
		}
	}
}
/**
 * Fixes the widget to display a text change.
 * Bit blitting and redrawing is done as necessary.
 * <p>
 *
 * @param y y location of the text change
 * @param newLineCount number of new lines.
 * @param replacedLineCount number of replaced lines.
 */
void redrawMultiLineChange(int y, int newLineCount, int replacedLineCount) {
	Rectangle clientArea = getClientArea();
	int lineCount = newLineCount - replacedLineCount;
	int sourceY;
	int destinationY;
		
	if (lineCount > 0) {
		sourceY = Math.max(0, y + lineHeight);
		destinationY = sourceY + lineCount * lineHeight;
	} 
	else {
		destinationY = Math.max(0, y + lineHeight);
		sourceY = destinationY - lineCount * lineHeight;
	}	
	scroll(
		0, destinationY,			// destination x, y
		0, sourceY,					// source x, y
		clientArea.width, clientArea.height, true);
	// Always redrawing causes the bottom line to flash when a line is
	// deleted. This is because SWT merges the paint area of the scroll
	// with the paint area of the redraw call below.
	// To prevent this we could call update after the scroll. However,
	// adding update can cause even more flash if the client does other 
	// redraw/update calls (ie. for syntax highlighting).
	// We could also redraw only when a line has been added or when 
	// contents has been added to a line. This would require getting 
	// line index info from the content and is not worth the trouble
	// (the flash is only on the bottom line and minor).
	// Specifying the NO_MERGE_PAINTS style bit prevents the merged 
	// redraw but could cause flash/slowness elsewhere.
	if (y + lineHeight > 0 && y <= clientArea.height) {
		// redraw first changed line in case a line was split/joined
		super.redraw(0, y, clientArea.width, lineHeight, true);
	}
	if (newLineCount > 0) {
		int redrawStartY = y + lineHeight;
		int redrawHeight = newLineCount * lineHeight;
		
		if (redrawStartY + redrawHeight > 0 && redrawStartY <= clientArea.height) {
			// display new text
			super.redraw(0, redrawStartY, clientArea.width, redrawHeight, true);
		}
	}
}
/** 
 * Redraws the specified text range.
 * <p>
 *
 * @param start offset of the first character to redraw
 * @param length number of characters to redraw
 * @param clearBackground true if the background should be cleared as
 *  part of the redraw operation.  If true, the entire redraw range will
 *  be cleared before anything is redrawn.  If the redraw range includes
 *	the last character of a line (i.e., the entire line is redrawn) the 
 * 	line is cleared all the way to the right border of the widget.
 * 	The redraw operation will be faster and smoother if clearBackground 
 * 	is set to false.  Whether or not the flag can be set to false depends 
 * 	on the type of change that has taken place.  If font styles or 
 * 	background colors for the redraw range have changed, clearBackground 
 * 	should be set to true.  If only foreground colors have changed for 
 * 	the redraw range, clearBackground can be set to false. 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when start and/or end are outside the widget content</li> 
 * </ul>
 */
public void redrawRange(int start, int length, boolean clearBackground) {
	checkWidget();
	int end = start + length;
	int contentLength = content.getCharCount();
	int firstLine;
	int lastLine;
	
	if (start > end || start < 0 || end > contentLength) {
		SWT.error(SWT.ERROR_INVALID_RANGE);
	}	
	firstLine = content.getLineAtOffset(start);
	lastLine = content.getLineAtOffset(end);
	// reset all affected lines but let the redraw recalculate only 
	// those that are visible.
	lineCache.reset(firstLine, lastLine - firstLine + 1, true);
	internalRedrawRange(start, length, clearBackground);
}
/**
 * Removes the specified bidirectional segment listener.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 * @since 2.0
 */
public void removeBidiSegmentListener(BidiSegmentListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	removeListener(LineGetSegments, listener);	
}
/**
 * Removes the specified extended modify listener.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void removeExtendedModifyListener(ExtendedModifyListener extendedModifyListener) {
	checkWidget();
	if (extendedModifyListener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	removeListener(ExtendedModify, extendedModifyListener);	
}
/**
 * Removes the specified line background listener.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void removeLineBackgroundListener(LineBackgroundListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	removeListener(LineGetBackground, listener);	
	// use default line styler if last user line styler was removed.
	if (isListening(LineGetBackground) == false && userLineBackground) {
		StyledTextListener typedListener = new StyledTextListener(defaultLineStyler);
		addListener(LineGetBackground, typedListener);	
		userLineBackground = false;
	}
}
/**
 * Removes the specified line style listener.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void removeLineStyleListener(LineStyleListener listener) {
	checkWidget();
	if (listener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	removeListener(LineGetStyle, listener);	
	// use default line styler if last user line styler was removed. Fixes 1G7B1X2
	if (isListening(LineGetStyle) == false && userLineStyle) {
		StyledTextListener typedListener = new StyledTextListener(defaultLineStyler);
		addListener(LineGetStyle, typedListener);	
		userLineStyle = false;
	}
}
/**
 * Removes the specified modify listener.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void removeModifyListener(ModifyListener modifyListener) {
	checkWidget();
	if (modifyListener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	removeListener(SWT.Modify, modifyListener);	
}
/**
 * Removes the specified selection listener.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void removeSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	removeListener(SWT.Selection, listener);	
}
/**
 * Removes the specified verify listener.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void removeVerifyListener(VerifyListener verifyListener) {
	checkWidget();
	if (verifyListener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	removeListener(SWT.Verify, verifyListener);	
}
/**
 * Removes the specified key verify listener.
 * <p>
 *
 * @param listener the listener
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void removeVerifyKeyListener(VerifyKeyListener listener) {
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	removeListener(VerifyKey, listener);	
}
/** 
 * Replaces the styles in the given range with new styles.  This method
 * effectively deletes the styles in the given range and then adds the
 * the new styles. 
 * <p>
 * Should not be called if a LineStyleListener has been set since the 
 * listener maintains the styles.
 * </p>
 *
 * @param start offset of first character where styles will be replaced
 * @param length number of characters to replace. Use 0 to insert text
 * @param ranges StyleRange objects containing the new style information.
 * The ranges should not overlap and should be within the specified start 
 * and length. The style rendering is undefined if the ranges do overlap
 * or are ill-defined. Must not be null.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when either start or end is outside the valid range (0 <= offset <= getCharCount())</li> 
 *   <li>ERROR_NULL_ARGUMENT when string is null</li>
 * </ul>
 * @since 2.0
 */
public void replaceStyleRanges(int start, int length, StyleRange[] ranges) {
	checkWidget();
	if (userLineStyle) {
		return;
	}
 	if (ranges == null) {
 		SWT.error(SWT.ERROR_NULL_ARGUMENT);
 	}
 	if (ranges.length == 0) {
 		setStyleRange(new StyleRange(start, length, null, null));
 		return;
 	}
	int end = start + length;
	if (start > end || start < 0 || end > getCharCount()) {
		SWT.error(SWT.ERROR_INVALID_RANGE);
	}	
	
	int firstLine = content.getLineAtOffset(start);
	int lastLine = content.getLineAtOffset(end);

	// if the area is not visible, there is no need to redraw
	boolean redrawLines = isAreaVisible(firstLine, lastLine);

	if (!redrawLines) {
		defaultLineStyler.replaceStyleRanges(start, length, ranges);
		lineCache.reset(firstLine, lastLine - firstLine + 1, true);
	} else {
		boolean redrawFirstLine = false;
		boolean redrawLastLine = false;	
		// the first and last line needs to be redrawn completely if the 
		// font style is changing from SWT.NORMAL to something else or 
		// vice versa. fixes 1G7M5WE.
		int firstLineOffset = content.getOffsetAtLine(firstLine);
		if (isBidi()) {
			redrawFirstLine = true;
			redrawLastLine = true;
		} else {
			int firstLineEnd = firstLineOffset + content.getLine(firstLine).length();
			redrawFirstLine = isRedrawFirstLine(ranges, firstLine, firstLineOffset);
			// check if any bold styles will be cleared
			StyleRange clearRange = new StyleRange(firstLineOffset, firstLineEnd - firstLineOffset, null, null);
			redrawFirstLine = redrawFirstLine || isRedrawFirstLine(new StyleRange[] {clearRange}, firstLine, firstLineOffset);
			if (lastLine != firstLine) {
				int lastLineOffset = content.getOffsetAtLine(lastLine);
				int lastLineEnd = lastLineOffset + content.getLine(lastLine).length();
				redrawLastLine = isRedrawLastLine(ranges, lastLine, lastLineOffset);
				// check if any bold styles will be cleared
				clearRange = new StyleRange(lastLineOffset, lastLineEnd - lastLineOffset, null, null);
				redrawLastLine = redrawLastLine || isRedrawLastLine(new StyleRange[] {clearRange}, lastLine, lastLineOffset);
			}
		}
		defaultLineStyler.replaceStyleRanges(start, length, ranges);
		// reset all lines affected by the style change but let the redraw
		// recalculate only those that are visible.
		lineCache.reset(firstLine, lastLine - firstLine + 1, true);
		internalRedrawRange(start, length, true);
		if (redrawFirstLine) {
			redrawLine(firstLine, start - firstLineOffset);
		}
		if (redrawLastLine) {
			redrawLine(lastLine, 0);
		}
	}

	// make sure that the caret is positioned correctly.
	// caret location may change if font style changes.
	// fixes 1G8FODP
	setCaretLocation();
}
/**
 * Replaces the given text range with new text.
 * If the widget has the SWT.SINGLE style and "text" contains more than 
 * one line, only the first line is rendered but the text is stored 
 * unchanged. A subsequent call to getText will return the same text 
 * that was set. Note that only a single line of text should be set when 
 * the SWT.SINGLE style is used.
 * <p>
 * <b>NOTE:</b> During the replace operation the current selection is
 * changed as follows:
 * <ul>	
 * <li>selection before replaced text: selection unchanged
 * <li>selection after replaced text: adjust the selection so that same text 
 * remains selected
 * <li>selection intersects replaced text: selection is cleared and caret
 * is placed after inserted text
 * </ul>
 * </p>
 *
 * @param start offset of first character to replace
 * @param length number of characters to replace. Use 0 to insert text
 * @param text new text. May be empty to delete text.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when either start or end is outside the valid range (0 <= offset <= getCharCount())</li> 
 *   <li>ERROR_INVALID_ARGUMENT when either start or end is inside a multi byte line delimiter. 
 * 		Splitting a line delimiter for example by inserting text in between the CR and LF and deleting part of a line delimiter is not supported</li>  
 *   <li>ERROR_NULL_ARGUMENT when string is null</li>
 * </ul>
 */
public void replaceTextRange(int start, int length, String text) {
	checkWidget();
	int contentLength = getCharCount();
	int end = start + length;
	Event event = new Event();
	
	if (start > end || start < 0 || end > contentLength) {
		SWT.error(SWT.ERROR_INVALID_RANGE);
	}	
	if (text == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	event.start = start;
	event.end = end;
	event.text = text;
	modifyContent(event, false);
}
/**
 * Resets the caret position, selection and scroll offsets. Recalculate
 * the content width and scroll bars. Redraw the widget.
 */
void reset() {
	ScrollBar verticalBar = getVerticalBar();
	ScrollBar horizontalBar = getHorizontalBar();
	caretOffset = 0;
	caretLine = 0;
	topIndex = 0;
	verticalScrollOffset = 0;
	horizontalScrollOffset = 0;	
	resetSelection();
	// discard any styles that may have been set by creating a 
	// new default line styler
	if (defaultLineStyler != null) {
		removeLineBackgroundListener(defaultLineStyler);
		removeLineStyleListener(defaultLineStyler);
		installDefaultLineStyler();
	}	
	calculateContentWidth();
	if (verticalBar != null) {
		verticalBar.setSelection(0);
	}
	if (horizontalBar != null) {
		horizontalBar.setSelection(0);	
	}
	setScrollBars();
	setCaretLocation();
	super.redraw();
}
/**
 * Resets the selection.
 */
void resetSelection() {
	selection.x = selection.y = caretOffset;
	selectionAnchor = -1;
}
/**
 * Scrolls the widget horizontally.
 * <p>
 *
 * @param pixels number of pixels to scroll, > 0 = scroll left,
 * 	< 0 scroll right
 */
void scrollHorizontal(int pixels) {
	Rectangle clientArea;
	
	if (pixels == 0) {
		return;
	}
	clientArea = getClientArea();
	if (pixels > 0) {
		int sourceX = leftMargin + pixels;
		int scrollWidth = clientArea.width - sourceX - rightMargin;
		int scrollHeight = clientArea.height - topMargin - bottomMargin;
		scroll(
			leftMargin, topMargin, 						// destination x, y
			sourceX, topMargin,							// source x, y
			scrollWidth, scrollHeight, true);
		if (sourceX > scrollWidth) {
			// redraw from end of scrolled area to beginning of scroll 
			// invalidated area
			super.redraw(
				leftMargin + scrollWidth, topMargin, 
				pixels - scrollWidth, scrollHeight, true);
		}
	}
	else {
		int destinationX = leftMargin - pixels;
		int scrollWidth = clientArea.width - destinationX - rightMargin;
		int scrollHeight = clientArea.height - topMargin - bottomMargin;
		scroll(
			destinationX, topMargin,					// destination x, y
			leftMargin, topMargin,						// source x, y
			scrollWidth, scrollHeight, true);
		if (destinationX > scrollWidth) {
			// redraw from end of scroll invalidated area to scroll 
			// destination
			super.redraw(
				leftMargin + scrollWidth, topMargin, 
				-pixels - scrollWidth, scrollHeight, true);	
		}
	}
	horizontalScrollOffset += pixels;
	setCaretLocation();
}
/**
 * Scrolls the widget horizontally and adjust the horizontal scroll
 * bar to reflect the new horizontal offset..
 * <p>
 *
 * @param pixels number of pixels to scroll, > 0 = scroll left,
 * 	< 0 scroll right
 */
void scrollHorizontalBar(int pixels) {
	if (pixels == 0) {
		return;
	}
	ScrollBar horizontalBar = getHorizontalBar();
	if (horizontalBar != null) {
		horizontalBar.setSelection(horizontalScrollOffset + pixels);
	}
	scrollHorizontal(pixels);
}
/** 
 * Selects all the text.
 * <p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void selectAll() {
	checkWidget();
	setSelection(new Point(0, Math.max(getCharCount(),0)));
}
/**
 * Replaces/inserts text as defined by the event.
 * <p>
 *
 * @param event the text change event. 
 *	<ul>
 *	<li>event.start - the replace start offset</li>
 * 	<li>event.end - the replace end offset</li>
 * 	<li>event.text - the new text</li>
 *	</ul>
 */
void sendKeyEvent(Event event) {
	if (editable == false) {
		return;
	}
	modifyContent(event, true);
}
/**
 * Sends the specified selection event.
 */
void sendSelectionEvent() {
	Event event = new Event();
	event.x = selection.x;
	event.y = selection.y;
	notifyListeners(SWT.Selection, event);
}
/**
 * Sets whether the widget wraps lines.
 * This overrides the creation style bit SWT.WRAP.
 * <p>
 *
 * @param wrap true=widget wraps lines, false=widget does not wrap lines
 * @since 2.0
 */
public void setWordWrap(boolean wrap) {
	checkWidget();
	
	if (wrap != wordWrap) {
		ScrollBar horizontalBar = getHorizontalBar();
		
		wordWrap = wrap;
		if (wordWrap) {
		    logicalContent = content;
		    content = new WrappedContent(renderer, logicalContent);
		}
		else {
		    content = logicalContent;
		}
		calculateContentWidth();
		caretLine = content.getLineAtOffset(caretOffset);
	    horizontalScrollOffset = 0;
	    if (horizontalBar != null) {
			horizontalBar.setVisible(!wordWrap);		
	    }
		setScrollBars();
		setCaretLocation();
		super.redraw();		
	}
}
/**
 * Sets the caret location and scrolls the caret offset into view.
 */
void showBidiCaret() {
	int line = content.getLineAtOffset(caretOffset);
	int lineOffset = content.getOffsetAtLine(line);
	int offsetInLine = caretOffset - lineOffset;
	String lineText = content.getLine(line);
	int xAtOffset = 0;
	boolean scrolled = false;		
	GC gc = new GC(this);
	StyledTextBidi bidi = getStyledTextBidi(lineText, lineOffset, gc);
	// getXAtOffset, inlined for better performance
	xAtOffset = renderer.bidiTextWidth(lineText, 0, offsetInLine, 0, bidi) + leftMargin;
	if (offsetInLine > lineText.length()) {
		// offset is not on the line. return an x location one character 
		// after the line to indicate the line delimiter.
		xAtOffset += lineEndSpaceWidth;
	}
	xAtOffset -= horizontalScrollOffset;
	//
	scrolled = showLocation(xAtOffset, line);
	if (scrolled == false) {
		setBidiCaretLocation(bidi);
	}
	gc.dispose();
}
/**
 * Sets the receiver's caret.  Set the caret's height and location.
 * 
 * </p>
 * @param caret the new caret for the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setCaret(Caret caret) {
	checkWidget ();
	super.setCaret(caret);
	if (caret != null) {
		if (isBidi() == false) {
			caret.setSize(caret.getSize().x, lineHeight);
		}
		setCaretLocation();
		if (isBidi()) {
			setBidiKeyboardLanguage();	
		}
	}		
}
/**
 * @see org.eclipse.swt.widgets.Control#setBackground
 */
public void setBackground(Color color) {
	checkWidget();
	background = color;
	redraw();
}
/**
 * Moves the Caret to the current caret offset.
 * <p>
 * 
 * @param bidi StyledTextBidi object to use for measuring.
 * 	May be left null in which case a new object will be created.
 */
void setBidiCaretLocation(StyledTextBidi bidi) {
	Caret caret = getCaret();
	
	if (caret != null) {
		String lineText = content.getLine(caretLine);
		int lineStartOffset = content.getOffsetAtLine(caretLine);
		int offsetInLine = caretOffset - lineStartOffset;
		int caretX;
		GC gc = null;
		
		if (bidi == null) {
			gc = new GC(this);
			bidi = getStyledTextBidi(lineText, lineStartOffset, gc);
		}		
		if (lastCaretDirection == SWT.NULL) {
			caretX = bidi.getCaretPosition(offsetInLine) + leftMargin;
		} else {
			caretX = bidi.getCaretPosition(offsetInLine, lastCaretDirection) + leftMargin;
		}
		caretX = caretX - horizontalScrollOffset;
		if (StyledTextBidi.getKeyboardLanguageDirection() == SWT.RIGHT) {
			caretX -= (getCaretWidth() - 1);
		}
		createBidiCaret();
		caret.setLocation(
			caretX, 
			caretLine * lineHeight - verticalScrollOffset + topMargin);
		if (gc != null) {
			gc.dispose();
		}
	}
}
/**
 * Sets the BIDI coloring mode.  When true the BIDI text display
 * algorithm is applied to segments of text that are the same
 * color.
 *
 * @param mode the new coloring mode
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * <p>
 * @deprecated use BidiSegmentListener instead.
 * </p>
 */
public void setBidiColoring(boolean mode) {
	checkWidget();
	bidiColoring = mode;
}
/**
 * Switches the keyboard language according to the current editing 
 * position and cursor direction.
 */
void setBidiKeyboardLanguage() {
	int lineStartOffset = content.getOffsetAtLine(caretLine);
	int offsetInLine = caretOffset - lineStartOffset;
	String lineText = content.getLine(caretLine);
	GC gc = new GC(this);
	StyledTextBidi bidi;
	int lineLength = lineText.length();
	// Don't supply the bold styles/font since we don't want to measure anything
	bidi = new StyledTextBidi(gc, lineText, getBidiSegments(lineStartOffset, lineText));
	if (offsetInLine == 0) {
		bidi.setKeyboardLanguage(offsetInLine);
	}
	else
	if (offsetInLine >= lineLength) {
		offsetInLine = Math.min(offsetInLine, lineLength - 1);
		bidi.setKeyboardLanguage(offsetInLine);
	}
	else
	if (lastCaretDirection == ST.COLUMN_NEXT) {
		// continue with previous character type
		bidi.setKeyboardLanguage(offsetInLine - 1);
	} 
	else {
		bidi.setKeyboardLanguage(offsetInLine);
	}	
	gc.dispose();
}
/**
 * Moves the Caret to the current caret offset.
 * <p>
 * 
 * @param caretX the new x location of the caret.
 * 	passed in for better performance when it has already been 
 * 	calculated outside this method.
 * @param line index of the line the caret is on. Relative to 
 *	the first line in the document.
 */
void setCaretLocation(int caretX, int line) {
	if (isBidi()) {
		setBidiCaretLocation(null);
	}
	else {	
		Caret caret = getCaret();		
		if (caret != null) {
			caret.setLocation(
				caretX, 
				line * lineHeight - verticalScrollOffset + topMargin);
		}
	}
}
/**
 * Moves the Caret to the current caret offset.
 */
void setCaretLocation() {
	if (isBidi()) {
		setBidiCaretLocation(null);
	}
	else {	
		Caret caret = getCaret();		
		if (caret != null) {
			int lineStartOffset = content.getOffsetAtLine(caretLine);
			int caretX = getXAtOffset(
				content.getLine(caretLine), 
				caretLine, caretOffset - lineStartOffset);
			caret.setLocation(
				caretX, 
				caretLine * lineHeight - verticalScrollOffset + topMargin);
		}
	}
}
/**
 * Sets the caret offset.
 *
 * @param offset caret offset, relative to the first character in the text.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_ARGUMENT when either the start or the end of the selection range is inside a 
 * multi byte line delimiter (and thus neither clearly in front of or after the line delimiter)
 * </ul>
 */
public void setCaretOffset(int offset) {
	checkWidget();
	int length = getCharCount();
				
	if (length > 0 && offset != caretOffset) {
		if (offset < 0) {
			caretOffset = 0;
			caretLine = 0;
		}
		else
		if (offset > length) {
			caretOffset = length;
			caretLine = content.getLineCount() - 1;
		}
		else {
			if (isLineDelimiter(offset)) {
				// offset is inside a multi byte line delimiter. This is an 
				// illegal operation and an exception is thrown. Fixes 1GDKK3R
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
			caretOffset = offset;
			caretLine = content.getLineAtOffset(caretOffset);
		}
		// clear the selection if the caret is moved.
		// don't notify listeners about the selection change.
		clearSelection(false);
	}
	// always update the caret location. fixes 1G8FODP
	setCaretLocation();
	if (isBidi()) {
		setBidiKeyboardLanguage();	
	}
}	
/**
 * Copies the specified text range to the clipboard.  The text will be placed
 * in the clipboard in plain text format and RTF format.
 * <p>
 *
 * @param start start index of the text
 * @param length length of text to place in clipboard
 * 
 * @exception SWTError, see Clipboard.setContents
 * @see org.eclipse.swt.dnd.Clipboard.setContents
 */
void setClipboardContent(int start, int length) throws SWTError {
	RTFTransfer rtfTransfer = RTFTransfer.getInstance();
	TextTransfer plainTextTransfer = TextTransfer.getInstance();
	RTFWriter rtfWriter = new RTFWriter(start, length);
	TextWriter plainTextWriter = new TextWriter(start, length);
	String rtfText = getPlatformDelimitedText(rtfWriter);
	String plainText = getPlatformDelimitedText(plainTextWriter);

	clipboard.setContents(
		new String[]{rtfText, plainText}, 
		new Transfer[]{rtfTransfer, plainTextTransfer});
}
/**
 * Sets the content implementation to use for text storage.
 * <p>
 *
 * @param content StyledTextContent implementation to use for text storage.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 * </ul>
 */
public void setContent(StyledTextContent newContent) {
	checkWidget();	
	if (newContent == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	if (content != null) {
		content.removeTextChangeListener(textChangeListener);
	}	
	logicalContent = newContent;
	if (wordWrap) {
	    content = new WrappedContent(renderer, logicalContent);
	}
	else {
	    content = logicalContent;
	}
	content.addTextChangeListener(textChangeListener);
	reset();
}
/** 
 * Sets whether the widget implements double click mouse behavior.
 * </p>
 *
 * @param enable if true double clicking a word selects the word, if false
 * 	double clicks have the same effect as regular mouse clicks.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setDoubleClickEnabled(boolean enable) {
	checkWidget();
	doubleClickEnabled = enable;
}
/**
 * Sets whether the widget content can be edited.
 * </p>
 *
 * @param editable if true content can be edited, if false content can not be 
 * 	edited
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setEditable(boolean editable) {
	checkWidget();
	this.editable = editable;
}
/**
 * Sets a new font to render text with.
 * <p>
 * <b>NOTE:</b> Italic fonts are not supported unless they have no overhang
 * and the same baseline as regular fonts.
 * </p>
 *
 * @param font new font
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setFont(Font font) {
	checkWidget();
	int oldLineHeight = lineHeight;
	
	super.setFont(font);	
	initializeRenderer();
	// keep the same top line visible. fixes 5815
	if (lineHeight != oldLineHeight) {
		setVerticalScrollOffset(verticalScrollOffset * lineHeight / oldLineHeight, true);
		claimBottomFreeSpace();
	}
	calculateContentWidth();
	calculateScrollBars();
	if (isBidi()) {
		caretDirection = SWT.NULL;
		createCaretBitmaps();
		createBidiCaret();
	} 
	else {
		Caret caret = getCaret();
		if (caret != null) {
			caret.setSize(caret.getSize().x, lineHeight);
		}
	}
	// always set the caret location. Fixes 6685
	setCaretLocation();
	super.redraw();
}
/**
 * @see org.eclipse.swt.widgets.Control#setForeground
 */
public void setForeground(Color color) {
	checkWidget();
	foreground = color;
	redraw();
}
/** 
 * Sets the horizontal scroll offset relative to the start of the line.
 * Do nothing if there is no text set.
 * <p>
 * <b>NOTE:</b> The horizontal index is reset to 0 when new text is set in the 
 * widget.
 * </p>
 *
 * @param offset horizontal scroll offset relative to the start 
 * 	of the line, measured in character increments starting at 0, if 
 * 	equal to 0 the content is not scrolled, if > 0 = the content is scrolled.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setHorizontalIndex(int offset) {
	checkWidget();
	int clientAreaWidth = getClientArea().width;
	if (getCharCount() == 0) {
		return;
	}	
	if (offset < 0) {
		offset = 0;
	}
	offset *= getHorizontalIncrement();
	// allow any value if client area width is unknown or 0. 
	// offset will be checked in resize handler.
	// don't use isVisible since width is known even if widget 
	// is temporarily invisible
	if (clientAreaWidth > 0) {
		int width = lineCache.getWidth();
		// prevent scrolling if the content fits in the client area.
		// align end of longest line with right border of client area
		// if offset is out of range.
		if (offset > width - clientAreaWidth) {
			offset = Math.max(0, width - clientAreaWidth);
		}
	}
	scrollHorizontalBar(offset - horizontalScrollOffset);
}
/** 
 * Sets the horizontal pixel offset relative to the start of the line.
 * Do nothing if there is no text set.
 * <p>
 * <b>NOTE:</b> The horizontal pixel offset is reset to 0 when new text 
 * is set in the widget.
 * </p>
 *
 * @param pixel horizontal pixel offset relative to the start 
 * 	of the line.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @since 2.0
 */
public void setHorizontalPixel(int pixel) {
	checkWidget();
	int clientAreaWidth = getClientArea().width;
	if (getCharCount() == 0) {
		return;
	}	
	if (pixel < 0) {
		pixel = 0;
	}
	// allow any value if client area width is unknown or 0. 
	// offset will be checked in resize handler.
	// don't use isVisible since width is known even if widget 
	// is temporarily invisible
	if (clientAreaWidth > 0) {
		int width = lineCache.getWidth();
		// prevent scrolling if the content fits in the client area.
		// align end of longest line with right border of client area
		// if offset is out of range.
		if (pixel > width - clientAreaWidth) {
			pixel = Math.max(0, width - clientAreaWidth);
		}
	}
	scrollHorizontalBar(pixel - horizontalScrollOffset);
}
/**
 * Adjusts the maximum and the page size of the horizontal scroll bar 
 * to reflect content width changes.
 */
void setHorizontalScrollBar() {
	ScrollBar horizontalBar = getHorizontalBar();
	
	if (horizontalBar != null && horizontalBar.getVisible()) {
		final int INACTIVE = 1;
		Rectangle clientArea = getClientArea();
		// only set the real values if the scroll bar can be used 
		// (ie. because the thumb size is less than the scroll maximum)
		// avoids flashing on Motif, fixes 1G7RE1J and 1G5SE92
		if (clientArea.width < lineCache.getWidth()) {
			horizontalBar.setValues(
				horizontalBar.getSelection(),
				horizontalBar.getMinimum(),
				lineCache.getWidth(),							// maximum
				clientArea.width - leftMargin - rightMargin,	// thumb size
				horizontalBar.getIncrement(),
				clientArea.width - leftMargin - rightMargin);	// page size
		}
		else 
		if (horizontalBar.getThumb() != INACTIVE || horizontalBar.getMaximum() != INACTIVE) {
			horizontalBar.setValues(
				horizontalBar.getSelection(),
				horizontalBar.getMinimum(),
				INACTIVE,
				INACTIVE,
				horizontalBar.getIncrement(),
				INACTIVE);
		}
	}
}
/** 
 * Sets the background color of the specified lines.
 * The background color is drawn for the width of the widget. All
 * line background colors are discarded when setText is called.
 * The text background color if defined in a StyleRange overlays the 
 * line background color. Should not be called if a LineBackgroundListener 
 * has been set since the listener maintains the line backgrounds.
 * <p>
 * During text changes, when entire lines are inserted or removed, the line 
 * background colors that are associated with the lines after the change 
 * will "move" with their respective text.  For all other text changes, 
 * line background colors will remain unchanged. 
 * </p>
 * 
 * @param startLine first line the color is applied to, 0 based
 * @param lineCount number of lines the color applies to.
 * @param background line background color
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_ARGUMENT when the specified line range is invalid</li>
 * </ul>
 */
public void setLineBackground(int startLine, int lineCount, Color background) {
	checkWidget();
	int partialBottomIndex = getPartialBottomIndex();
	
	// this API can not be used if the client is providing the line background
	if (userLineBackground) {
		return;
	}
	if (startLine < 0 || startLine + lineCount > logicalContent.getLineCount()) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	} 
	defaultLineStyler.setLineBackground(startLine, lineCount, background);
	// do nothing if redraw range is completely invisible	
	if (startLine > partialBottomIndex || startLine + lineCount - 1 < topIndex) {
		return;
	}
	// only redraw visible lines
	if (startLine < topIndex) {
		lineCount -= topIndex - startLine;
		startLine = topIndex;
	}
	if (startLine + lineCount - 1 > partialBottomIndex) {
		lineCount = partialBottomIndex - startLine + 1;
	}
	startLine -= topIndex;
	super.redraw(
		leftMargin, startLine * lineHeight + topMargin, 
		getClientArea().width - leftMargin - rightMargin, lineCount * lineHeight, true);
}
/**
 * Adjusts the maximum and the page size of the scroll bars to 
 * reflect content width/length changes.
 */
void setScrollBars() {
	ScrollBar verticalBar = getVerticalBar();
	
	if (verticalBar != null) {
		Rectangle clientArea = getClientArea();
		final int INACTIVE = 1;
		int maximum = content.getLineCount() * getVerticalIncrement();
		
		// only set the real values if the scroll bar can be used 
		// (ie. because the thumb size is less than the scroll maximum)
		// avoids flashing on Motif, fixes 1G7RE1J and 1G5SE92
		if (clientArea.height < maximum) {
			verticalBar.setValues(
				verticalBar.getSelection(),
				verticalBar.getMinimum(),
				maximum,
				clientArea.height,				// thumb size
				verticalBar.getIncrement(),
				clientArea.height);				// page size
		}
		else
		if (verticalBar.getThumb() != INACTIVE || verticalBar.getMaximum() != INACTIVE) {
			verticalBar.setValues(
				verticalBar.getSelection(),
				verticalBar.getMinimum(),
				INACTIVE,
				INACTIVE,
				verticalBar.getIncrement(),
				INACTIVE);
		}		
	}
	setHorizontalScrollBar();
}
/** 
 * Sets the selection to the given position and scrolls it into view.  Equivalent to setSelection(start,start).
 * <p>
 *
 * @param start new caret position
 * @see #setSelection(int,int)
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when start is outside the widget content
 *   <li>ERROR_INVALID_ARGUMENT when either the start or the end of the selection range is inside a 
 * multi byte line delimiter (and thus neither clearly in front of or after the line delimiter)
 * </ul> 
 */
public void setSelection(int start) {
	setSelection(start, start);
}
/** 
 * Sets the selection and scrolls it into view.
 * <p>
 * Indexing is zero based.  Text selections are specified in terms of
 * caret positions.  In a text widget that contains N characters, there are 
 * N+1 caret positions, ranging from 0..N
 * </p>
 *
 * @param point x=selection start offset, y=selection end offset
 * @see #setSelection(int,int)
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_NULL_ARGUMENT when point is null</li>
 *   <li>ERROR_INVALID_RANGE when start and end is outside the widget content
 *   <li>ERROR_INVALID_ARGUMENT when either the start or the end of the selection range is inside a 
 * multi byte line delimiter (and thus neither clearly in front of or after the line delimiter)
 * </ul> 
 */
public void setSelection(Point point) {
	checkWidget();
	if (point == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);	
	setSelection(point.x, point.y);
}
/** 
 * Sets the selection and scrolls it into view.
 * <p>
 * Indexing is zero based.  Text selections are specified in terms of
 * caret positions.  In a text widget that contains N characters, there are 
 * N+1 caret positions, ranging from 0..N
 * </p>
 *
 * @param start selection start offset
 * @param end selection end offset
 * @see #setSelectionRange(int,int)
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when start and end is outside the widget content
 *   <li>ERROR_INVALID_ARGUMENT when either the start or the end of the selection range is inside a 
 * multi byte line delimiter (and thus neither clearly in front of or after the line delimiter)
 * </ul>
 */
public void setSelection(int start, int end) {
	checkWidget();
	int contentLength = getCharCount();
	
	if (start > end || start < 0 || end > contentLength) {
		SWT.error(SWT.ERROR_INVALID_RANGE);
	}
	if (isLineDelimiter(start) || isLineDelimiter(end)) {
		// the start offset or end offset of the selection range is inside a 
		// multi byte line delimiter. This is an illegal operation and an exception 
		// is thrown. Fixes 1GDKK3R
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}				
	internalSetSelection(start, end - start, false);
	// always update the caret location. fixes 1G8FODP
	setCaretLocation();
	if (isBidi()) {
		setBidiKeyboardLanguage();	
	}
	showSelection();
}
/** 
 * Sets the selection. The new selection may not be visible. Call showSelection to scroll 
 * the selection into view.
 * <p>
 *
 * @param start offset of the first selected character, start >= 0 must be true.
 * @param length number of characters to select, start <= start + length <= getCharCount() 
 * 	must be true.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when the range specified by start and length is outside the widget content
 *   <li>ERROR_INVALID_ARGUMENT when either the start or the end of the selection range is inside a 
 * multi byte line delimiter (and thus neither clearly in front of or after the line delimiter)
 * </ul>
 */
public void setSelectionRange(int start, int length) {
	checkWidget();
	int contentLength = getCharCount();
	int end = start + length;
	
	if (start > end || start < 0 || end > contentLength) {
		SWT.error(SWT.ERROR_INVALID_RANGE);
	}
	if (isLineDelimiter(start) || isLineDelimiter(end)) {
		// the start offset or end offset of the selection range is inside a 
		// multi byte line delimiter. This is an illegal operation and an exception 
		// is thrown. Fixes 1GDKK3R
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}					
	internalSetSelection(start, length, false);
	// always update the caret location. fixes 1G8FODP
	setCaretLocation();
	if (isBidi()) {
		setBidiKeyboardLanguage();	
	}
}
/** 
 * Sets the selection. 
 * The new selection may not be visible. Call showSelection to scroll 
 * the selection into view.
 * <p>
 *
 * @param start offset of the first selected character, start >= 0 must be true.
 * @param length number of characters to select, start <= start + length 
 * 	<= getCharCount() must be true.
 * @param sendEvent	a Selection event is sent when set to true and when 
 * 	the selection is reset.
 */
void internalSetSelection(int start, int length, boolean sendEvent) {
	int end = start + length;
	
	if (selection.x != start || selection.y != end) {
		clearSelection(sendEvent);
		selectionAnchor = selection.x = start;
		caretOffset = selection.y = end;
		caretLine = content.getLineAtOffset(caretOffset);
		if (length > 0) {
			internalRedrawRange(selection.x, selection.y - selection.x, true);
		}
	}
}
/** 
 * Adds the specified style. The new style overwrites existing styles for the
 * specified range.  Existing style ranges are adjusted if they partially 
 * overlap with the new style, To clear an individual style, call setStyleRange 
 * with a StyleRange that has null attributes. 
 * <p>
 * Should not be called if a LineStyleListener has been set since the 
 * listener maintains the styles.
 * </p>
 *
 * @param range StyleRange object containing the style information.
 * Overwrites the old style in the given range. May be null to delete
 * all styles.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_INVALID_RANGE when the style range is outside the valid range (> getCharCount())</li> 
 * </ul>
 */
public void setStyleRange(StyleRange range) {
	checkWidget();
	
	// this API can not be used if the client is providing the line styles
	if (userLineStyle) {
		return;
	}
 	// check the range, make sure it falls within the range of the
 	// text 
	if (range != null && range.start + range.length > content.getCharCount()) {
		SWT.error(SWT.ERROR_INVALID_RANGE);
	} 	
	if (range != null) {
		boolean redrawFirstLine = false;
		boolean redrawLastLine = false;
		int firstLine = content.getLineAtOffset(range.start);
		int lastLine = content.getLineAtOffset(range.start + range.length);

		// if the style is not visible, there is no need to redraw
		boolean redrawLines = isAreaVisible(firstLine, lastLine);

		if (!redrawLines) {
			defaultLineStyler.setStyleRange(range);
			lineCache.reset(firstLine, lastLine - firstLine + 1, true);
		} else {
			// the first and last line needs to be redrawn completely if the 
			// font style is changing from SWT.NORMAL to something else or 
			// vice versa. fixes 1G7M5WE.
			int firstLineOffset = content.getOffsetAtLine(firstLine);
			int lastLineOffset = content.getOffsetAtLine(lastLine);
			if (isBidi()) {
				redrawFirstLine = true;
				redrawLastLine = true;
			} else {
				redrawFirstLine = isRedrawFirstLine(new StyleRange[] {range}, firstLine, firstLineOffset);
				if (lastLine != firstLine) {
					redrawLastLine = isRedrawLastLine(new StyleRange[] {range}, lastLine, lastLineOffset);
				}
			}
			defaultLineStyler.setStyleRange(range);
			// reset all lines affected by the style change but let the redraw
			// recalculate only those that are visible.
			lineCache.reset(firstLine, lastLine - firstLine + 1, true);
			internalRedrawRange(range.start, range.length, true);
			if (redrawFirstLine) {
				redrawLine(firstLine, range.start - firstLineOffset);
			}
			if (redrawLastLine) {
				redrawLine(lastLine, 0);
			}
		}
	} else {
		// clearing all styles
		defaultLineStyler.setStyleRange(range);
		lineCache.reset(0, content.getLineCount(), false);
		redraw();
	}
	
	// make sure that the caret is positioned correctly.
	// caret location may change if font style changes.
	// fixes 1G8FODP
	setCaretLocation();
}
/** 
 * Sets styles to be used for rendering the widget content. All styles 
 * will be replaced with the given set of styles.
 * <p>
 * Should not be called if a LineStyleListener has been set since the 
 * listener maintains the styles.
 * </p>
 *
 * @param ranges StyleRange objects containing the style information.
 * The ranges should not overlap. The style rendering is undefined if 
 * the ranges do overlap. Must not be null.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when listener is null</li>
 *    <li>ERROR_INVALID_RANGE when the last of the style ranges is outside the valid range (> getCharCount())</li> 
 * </ul>
 */
public void setStyleRanges(StyleRange[] ranges) {
	checkWidget();
	// this API can not be used if the client is providing the line styles
	if (userLineStyle) {
		return;
	}
 	if (ranges == null) {
 		SWT.error(SWT.ERROR_NULL_ARGUMENT);
 	}
 	// check the last range, make sure it falls within the range of the
 	// current text 
 	if (ranges.length != 0) {
 		StyleRange last = ranges[ranges.length-1];
 		int lastEnd = last.start + last.length;
		int firstLine = content.getLineAtOffset(ranges[0].start);
		int lastLine;
		if (lastEnd > content.getCharCount()) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		} 	
		lastLine = content.getLineAtOffset(lastEnd);
		// reset all lines affected by the style change
		lineCache.reset(firstLine, lastLine - firstLine + 1, true);
 	}
 	else {
		// reset all lines
		lineCache.reset(0, content.getLineCount(), false);
 	}
	defaultLineStyler.setStyleRanges(ranges);
	redraw(); // should only redraw affected area to avoid flashing
	// make sure that the caret is positioned correctly.
	// caret location may change if font style changes.
	// fixes 1G8FODP
	setCaretLocation();
}
/** 
 * Sets the tab width. 
 * <p>
 *
 * @param tabs tab width measured in characters.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setTabs(int tabs) {
	checkWidget();	
	tabLength = tabs;
	renderer.setTabLength(tabLength);
	if (caretOffset > 0) {
		caretOffset = 0;
		caretLine = 0;
		if (isBidi()) {
			showBidiCaret();
		}
		else {
			showCaret();
		}
		clearSelection(false);
	}
	// reset all line widths when the tab width changes
	lineCache.reset(0, content.getLineCount(), false);
	redraw();
}
/** 
 * Sets the widget content. 
 * If the widget has the SWT.SINGLE style and "text" contains more than 
 * one line, only the first line is rendered but the text is stored 
 * unchanged. A subsequent call to getText will return the same text 
 * that was set.
 * <p>
 * <b>Note:</b> Only a single line of text should be set when the SWT.SINGLE 
 * style is used.
 * </p>
 *
 * @param text new widget content. Replaces existing content. Line styles 
 * 	that were set using StyledText API are discarded.  The
 * 	current selection is also discarded.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT when string is null</li>
 * </ul>
 */
public void setText(String text) {
	checkWidget();
	Event event = new Event();
	
	if (text == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	event.start = 0;
	event.end = getCharCount();
	event.text = text;
	event.doit = true;	
	notifyListeners(SWT.Verify, event);
	if (event.doit) {
		StyledTextEvent styledTextEvent = null;
		
		if (isListening(ExtendedModify)) {		
			styledTextEvent = new StyledTextEvent(logicalContent);
			styledTextEvent.start = event.start;
			styledTextEvent.end = event.start + event.text.length();
			styledTextEvent.text = content.getTextRange(event.start, event.end - event.start);
		}
		content.setText(event.text);
		notifyListeners(SWT.Modify, event);	
		if (styledTextEvent != null) {
			notifyListeners(ExtendedModify, styledTextEvent);
		}
	}
}
/**
 * Sets the text limit.
 * <p>
 * The text limit specifies the amount of text that
 * the user can type into the widget.
 * </p>
 *
 * @param limit the new text limit.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *   <li>ERROR_CANNOT_BE_ZERO when limit is 0</li>
 * </ul>
 */
public void setTextLimit(int limit) {
	checkWidget();
	if (limit == 0) {
		SWT.error(SWT.ERROR_CANNOT_BE_ZERO);
	}
	textLimit = limit;
}
/**
 * Sets the top index. Do nothing if there is no text set.
 * <p>
 * The top index is the index of the line that is currently at the top 
 * of the widget. The top index changes when the widget is scrolled.
 * Indexing starts from zero.
 * Note: The top index is reset to 0 when new text is set in the widget.
 * </p>
 *
 * @param index new top index. Must be between 0 and getLineCount() - 
 * 	visible lines per page. An out of range index will be adjusted accordingly.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setTopIndex(int topIndex) {
	checkWidget();
	int lineCount = logicalContent.getLineCount();
	int pageSize = Math.min(lineCount, getLineCountWhole());
	
	if (getCharCount() == 0) {
		return;
	}	
	if (topIndex < 0) {
		topIndex = 0;
	}
	else 
	if (topIndex > lineCount - pageSize) {
		topIndex = lineCount - pageSize;
	}
	if (wordWrap) {
		int logicalLineOffset = logicalContent.getOffsetAtLine(topIndex);
		topIndex = content.getLineAtOffset(logicalLineOffset);
	}
	setVerticalScrollOffset(topIndex * getVerticalIncrement(), true);
}
/**
 * Sets the top pixel offset. Do nothing if there is no text set.
 * <p>
 * The top pixel offset is the vertical pixel offset of the widget. The
 * widget is scrolled so that the given pixel position is at the top.
 * The top index is adjusted to the corresponding top line.
 * Note: The top pixel is reset to 0 when new text is set in the widget.
 * </p>
 *
 * @param pixel new top pixel offset. Must be between 0 and 
 * 	(getLineCount() - visible lines per page) / getLineHeight()). An out
 * 	of range offset will be adjusted accordingly.
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @since 2.0
 */
public void setTopPixel(int pixel) {
	checkWidget();
	int lineCount = logicalContent.getLineCount();
	int height = getClientArea().height;
	int maxTopPixel = Math.max(0, lineCount * getVerticalIncrement() - height);
	
	if (getCharCount() == 0) {
		return;
	}	
	if (pixel < 0) {
		pixel = 0;
	}
	else 
	if (pixel > maxTopPixel) {
		pixel = maxTopPixel;
	}
	setVerticalScrollOffset(pixel, true);
}
/**
 * Scrolls the widget vertically.
 * <p>
 *
 * @param pixelOffset the new vertical scroll offset
 * @param adjustScrollBar 
 * 	true= the scroll thumb will be moved to reflect the new scroll offset.
 * 	false = the scroll thumb will not be moved
 */
void setVerticalScrollOffset(int pixelOffset, boolean adjustScrollBar) {
	Rectangle clientArea;
	ScrollBar verticalBar = getVerticalBar();
	int verticalIncrement = getVerticalIncrement();
	
	if (pixelOffset == verticalScrollOffset) {
		return;
	}
	if (verticalBar != null && adjustScrollBar) {
		verticalBar.setSelection(pixelOffset);
	}
	clientArea = getClientArea();
	scroll(
		0, 0, 									// destination x, y
		0, pixelOffset - verticalScrollOffset,	// source x, y
		clientArea.width, clientArea.height, true);
	if (verticalIncrement != 0) {
		int oldTopIndex = topIndex;		
		topIndex = Compatibility.ceil(pixelOffset, verticalIncrement);
		if (topIndex != oldTopIndex) {
			lineCache.calculate(topIndex, getPartialBottomIndex() - topIndex + 1);
			setHorizontalScrollBar();
		}
	}
	verticalScrollOffset = pixelOffset;	
	setCaretLocation();
}
/**
 * Scrolls the specified location into view.
 * <p>
 * 
 * @param x the x coordinate that should be made visible.
 * @param line the line that should be made visible. Relative to the
 *	first line in the document.
 * @return 
 *	true=the widget was scrolled to make the specified location visible. 
 *	false=the specified location is already visible, the widget was 
 *	not scrolled. 	
 */
boolean showLocation(int x, int line) {
	int clientAreaWidth = getClientArea().width - leftMargin - rightMargin;
	int verticalIncrement = getVerticalIncrement();
	int horizontalIncrement = clientAreaWidth / 4;
	boolean scrolled = false;		
	
	if (x < leftMargin) {
		// always make 1/4 of a page visible
		x = Math.max(horizontalScrollOffset * -1, x - horizontalIncrement);	
		scrollHorizontalBar(x);
		scrolled = true;
	}
	else 
	if (x >= clientAreaWidth) {
		// always make 1/4 of a page visible
		x = Math.min(lineCache.getWidth() - horizontalScrollOffset, x + horizontalIncrement);
		scrollHorizontalBar(x - clientAreaWidth);
		scrolled = true;
	}
	if (line < topIndex) {
		setVerticalScrollOffset(line * verticalIncrement, true);
		scrolled = true;
	}
	else
	if (line > getBottomIndex()) {
		setVerticalScrollOffset((line + 1) * verticalIncrement - getClientArea().height, true);
		scrolled = true;
	}
	return scrolled;
}
/**
 * Sets the caret location and scrolls the caret offset into view.
 */
void showCaret() {
	int lineOffset = content.getOffsetAtLine(caretLine);
	String lineText = content.getLine(caretLine);
	int offsetInLine = caretOffset - lineOffset;
	int xAtOffset = getXAtOffset(lineText, caretLine, offsetInLine);	
	boolean scrolled = showLocation(xAtOffset, caretLine);
	
	if (scrolled == false) {
		setCaretLocation(xAtOffset, caretLine);
	}
	if (isBidi()) {
		setBidiKeyboardLanguage();
	}
}
/**
 * Scrolls the specified offset into view.
 * <p>
 *
 * @param offset offset that should be scolled into view
 */
void showOffset(int offset) {
	int line = content.getLineAtOffset(offset);
	int lineOffset = content.getOffsetAtLine(line);
	int offsetInLine = offset - lineOffset;
	String lineText = content.getLine(line);
	int xAtOffset = getXAtOffset(lineText, line, offsetInLine);
	
	showLocation(xAtOffset, line);	
}
/**
 * Scrolls the selection into view.
 * <p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void showSelection() {
	checkWidget();
	showOffset(selection.x);
	showOffset(selection.y);
}
/**
 * Returns the width of the specified text. Expand tabs to tab stops using
 * the widget tab width.
 * <p>
 *
 * @param line line to be measured.
 * @param lineIndex	index of the line relative to the first kine of the 
 * 	document
 * @param length number of characters to measure. Tabs are counted 
 * 	as one character in this parameter.
 * @param gc GC to use for measuring text
 * @return width of the text with tabs expanded to tab stops or 0 if the 
 * 	length is beyond the text length.
 */
int textWidth(String line, int lineIndex, int length, GC gc) {
	int lineOffset = content.getOffsetAtLine(lineIndex);
	int lineLength = line.length();
	int width;
	if (lineLength == 0 || length > lineLength) {
		return 0;
	}	
	if (isBidi()) {
		StyledTextBidi bidi = getStyledTextBidi(line, lineOffset, gc, null);
		width = renderer.bidiTextWidth(line, 0, length, 0, bidi);
	}
	else {
		StyledTextEvent event = renderer.getLineStyleData(lineOffset, line);
		StyleRange[] styles = null;
		if (event != null) {
			styles = renderer.filterLineStyles(event.styles);
		}
		width = renderer.textWidth(line, lineOffset, 0, length, styles, 0, gc, gc.getFont().getFontData()[0]);
	}
	return width;
}
/**
 * Updates the caret direction when a delete operation occured based on 
 * the type of the delete operation (next/previous character) and the 
 * caret location (at a direction boundary or inside a direction segment).
 * The intent is to place the caret at the visual location where a
 * character was deleted.
 * <p>
 * 
 * @param isBackspace true=the previous character was deleted, false=the 
 * 	character next to the caret location was deleted
 * @param isDirectionBoundary true=the caret is between a R2L and L2R segment,
 * 	false=the caret is within a direction segment
 */
void updateBidiDirection(boolean isBackspace, boolean isDirectionBoundary) {
	if (isDirectionBoundary) {
		if (isBackspace) {
			// Deleted previous character (backspace) at a direction boundary
			// Go to direction segment of deleted character
			lastCaretDirection = ST.COLUMN_NEXT;
		}
		else {
			// Deleted next character. Go to direction segment of deleted character
			lastCaretDirection = ST.COLUMN_PREVIOUS;
		}
	}
	else {
		if (isBackspace) {
			// Delete previous character inside direction segment (i.e., not at a direction boundary)
			lastCaretDirection = ST.COLUMN_PREVIOUS;
		}
		else {
			// Deleted next character.
			lastCaretDirection = ST.COLUMN_NEXT;
		}
	}
}
/**
 * Updates the selection and caret position depending on the text change.
 * If the selection intersects with the replaced text, the selection is 
 * reset and the caret moved to the end of the new text.
 * If the selection is behind the replaced text it is moved so that the
 * same text remains selected.  If the selection is before the replaced text 
 * it is left unchanged.
 * <p>
 *
 * @param startOffset offset of the text change
 * @param replacedLength length of text being replaced
 * @param newLength length of new text
 */
void updateSelection(int startOffset, int replacedLength, int newLength) {
	if (selection.y <= startOffset) {
		// selection ends before text change
		return;
	}
	if (selection.x < startOffset) {
		// clear selection fragment before text change
		internalRedrawRange(selection.x, startOffset - selection.x, true);
	}
	if (selection.y > startOffset + replacedLength && selection.x < startOffset + replacedLength) {
		// clear selection fragment after text change.
		// do this only when the selection is actually affected by the 
		// change. Selection is only affected if it intersects the change (1GDY217).
		int netNewLength = newLength - replacedLength;
		int redrawStart = startOffset + newLength;
		internalRedrawRange(redrawStart, selection.y + netNewLength - redrawStart, true);
	}
	if (selection.y > startOffset && selection.x < startOffset + replacedLength) {
		// selection intersects replaced text. set caret behind text change
		internalSetSelection(startOffset + newLength, 0, true);
		// always update the caret location. fixes 1G8FODP
		setCaretLocation();
	}
	else {
		// move selection to keep same text selected
		internalSetSelection(selection.x + newLength - replacedLength, selection.y - selection.x, true);
		// always update the caret location. fixes 1G8FODP
		setCaretLocation();
	}	
}
/**
 * Rewraps all lines and adjusts the top index so that the top 
 * line remains the same.
 * <p>
 * 
 * @param oldClientAreaWidth client area width before resize 
 * 	occurred
 */
void wordWrapResize(int oldClientAreaWidth) {
	String oldCaretLine = content.getLine(caretLine);
	String topLine = content.getLine(topIndex);	
	int topOffset = content.getOffsetAtLine(topIndex) + topLine.length();
	int newTopIndex;
	WrappedContent wrappedContent = (WrappedContent) content;
	// all lines are wrapped and no rewrap required if widget has already 
	// been visible, client area is now wider and visual (wrapped) line 
	// count equals logical line count.
	if (oldClientAreaWidth != 0 && clientAreaWidth > oldClientAreaWidth &&
		wrappedContent.getLineCount() == logicalContent.getLineCount()) {
		return;
	}
    wrappedContent.wrapLines();
    // has word wrap changed on the caret line? 
    if (caretLine >= content.getLineCount() || 
    	content.getLine(caretLine).equals(oldCaretLine) == false) {
    	// caret may now be on a different line
	    caretLine = content.getLineAtOffset(caretOffset);
	    setCaretLocation();
    }    
	// make sure top line remains the same
	// topOffset is the end of the top line. otherwise top index would be 
	// set to the preceeding visual line if top line is wrapped (because 
	// end of preceeding line == start of next line). fixes 8503.
    newTopIndex = content.getLineAtOffset(topOffset);
    if (newTopIndex != topIndex) {
    	ScrollBar verticalBar = getVerticalBar();
    	// adjust index and pixel offset manually instead of calling
    	// setVerticalScrollOffset because the widget does not actually need
    	// to be scrolled. causes flash otherwise.
    	verticalScrollOffset += (newTopIndex - topIndex) * getVerticalIncrement();
    	// verticalScrollOffset may become negative if first line was 
    	// partially visible and second line was top line. prevent this from 
    	// happening to fix 8503.
    	if (verticalScrollOffset < 0) {
    		verticalScrollOffset = 0;
    	}
    	topIndex = newTopIndex;
    	if (verticalBar != null) {
			verticalBar.setSelection(verticalScrollOffset);
    	}
    }
	// word wrap may have changed on one of the visible lines
    super.redraw();
}
}