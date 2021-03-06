package org.eclipse.swt.graphics;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved
 */

import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.*;
import java.util.Locale;

/**
 * Instances of this class describe operating system fonts.
 * Only the public API of this type is platform independent.
 * <p>
 * For platform-independent behaviour, use the get and set methods
 * corresponding to the following properties:
 * <dl>
 * <dt>height</dt><dd>the height of the font in points</dd>
 * <dt>name</dt><dd>the face name of the font, which may include the foundry</dd>
 * <dt>style</dt><dd>A bitwise combination of NORMAL, ITALIC and BOLD</dd>
 * </dl>
 * If extra, platform-dependent functionality is required:
 * <ul>
 * <li>On <em>Windows</em>, the data member of the <code>FontData</code>
 * corresponds to a Windows <code>LOGFONT</code> structure whose fields
 * may be retrieved and modified.</li>
 * <li>On <em>X</em>, the fields of the <code>FontData</code> correspond
 * to the entries in the font's XLFD name and may be retrieved and modified.
 * </ul>
 * Application code does <em>not</em> need to explicitly release the
 * resources managed by each instance when those instances are no longer
 * required, and thus no <code>dispose()</code> method is provided.
 *
 * @see Font
 */

public final class FontData {
	
	/**
	 * A Win32 LOGFONT struct
	 * (Warning: This field is platform dependent)
	 */
	public LOGFONT data;
	
	/**
	 * The height of the font data in points
	 * (Warning: This field is platform dependent)
	 */
	public int height;
	
	/**
	 * The locale of the font
	 * (Warning: This field is platform dependent)
	 */
	Locale locale;
	
/**	 
 * Constructs a new un-initialized font data.
 */
public FontData() {
	data = new LOGFONT();
	// We set the charset field so that
	// wildcard searching will work properly
	// out of the box
	data.lfCharSet = OS.DEFAULT_CHARSET;
	height = 12;
}

/**
 * Constructs a new font data given the Windows <code>LOGFONT</code>
 * that it should represent.
 * 
 * @param data the <code>LOGFONT</code> for the result
 */
FontData(LOGFONT data, int height) {
	this.data = data;
	this.height = height;
}

/**
 * Constructs a new FontData given a string representation
 * in the form generated by the <code>FontData.toString</code>
 * method.
 * <p>
 * Note that the representation varies between platforms,
 * and a FontData can only be created from a string that was 
 * generated on the same platform.
 * </p>
 *
 * @param string the string representation of a <code>FontData</code> (must not be null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the argument is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument does not represent a valid description</li>
 * </ul>
 *
 * @see #toString
 */
public FontData(String string) {
	if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	int start = 0;
	int end = string.indexOf('|');
	if (end == -1) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	String version1 = string.substring(start, end);
	
	start = end + 1;
	end = string.indexOf('|', start);
	if (end == -1) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	String name = string.substring(start, end);
	
	start = end + 1;
	end = string.indexOf('|', start);
	if (end == -1) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	int height = 0;
	try {
		height = Integer.parseInt(string.substring(start, end));
	} catch (NumberFormatException e) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}
	
	start = end + 1;
	end = string.indexOf('|', start);
	if (end == -1) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	int style = 0;
	try {
		style = Integer.parseInt(string.substring(start, end));
	} catch (NumberFormatException e) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}

	start = end + 1;
	end = string.indexOf('|', start);
	data = new LOGFONT();
	data.lfCharSet = OS.DEFAULT_CHARSET;
	setName(name);
	setHeight(height);
	setStyle(style);
	if (end == -1) return;
	String platform = string.substring(start, end);

	start = end + 1;
	end = string.indexOf('|', start);
	if (end == -1) return;
	String version2 = string.substring(start, end);

	if (platform.equals("WINDOWS") && version2.equals("1")) {
		LOGFONT newData = new LOGFONT();
		try {
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfHeight = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfWidth = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfEscapement = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfOrientation = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfWeight = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfItalic = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfUnderline = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfStrikeOut = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfCharSet = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfOutPrecision = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfClipPrecision = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfQuality = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfPitchAndFamily = Byte.parseByte(string.substring(start, end));
			start = end + 1;
		} catch (NumberFormatException e) {
			setName(name);
			setHeight(height);
			setStyle(style);
			return;
		}
		char[] lfFaceName = new char[32];
		string.getChars(start, string.length(), lfFaceName, 0);
		newData.lfFaceName0 = lfFaceName[0];
		newData.lfFaceName1 = lfFaceName[1];
		newData.lfFaceName2 = lfFaceName[2];
		newData.lfFaceName3 = lfFaceName[3];
		newData.lfFaceName4 = lfFaceName[4];
		newData.lfFaceName5 = lfFaceName[5];
		newData.lfFaceName6 = lfFaceName[6];
		newData.lfFaceName7 = lfFaceName[7];
		newData.lfFaceName8 = lfFaceName[8];
		newData.lfFaceName9 = lfFaceName[9];
		newData.lfFaceName10 = lfFaceName[10];
		newData.lfFaceName11 = lfFaceName[11];
		newData.lfFaceName12 = lfFaceName[12];
		newData.lfFaceName13 = lfFaceName[13];
		newData.lfFaceName14 = lfFaceName[14];
		newData.lfFaceName15 = lfFaceName[15];
		newData.lfFaceName16 = lfFaceName[16];
		newData.lfFaceName17 = lfFaceName[17];
		newData.lfFaceName18 = lfFaceName[18];
		newData.lfFaceName19 = lfFaceName[19];
		newData.lfFaceName20 = lfFaceName[20];
		newData.lfFaceName21 = lfFaceName[21];
		newData.lfFaceName22 = lfFaceName[22];
		newData.lfFaceName23 = lfFaceName[23];
		newData.lfFaceName24 = lfFaceName[24];
		newData.lfFaceName25 = lfFaceName[25];
		newData.lfFaceName26 = lfFaceName[26];
		newData.lfFaceName27 = lfFaceName[27];
		newData.lfFaceName28 = lfFaceName[28];
		newData.lfFaceName29 = lfFaceName[29];
		newData.lfFaceName30 = lfFaceName[30];
		newData.lfFaceName31 = lfFaceName[31];
		data = newData;
	}
}

/**	 
 * Constructs a new font data given a font name,
 * the height of the desired font in points, 
 * and a font style.
 *
 * @param name the name of the font (must not be null)
 * @param height the font height in points
 * @param style a bit or combination of NORMAL, BOLD, ITALIC
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - when the font name is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the height is negative</li>
 * </ul>
 */
public FontData(String name, int height, int style) {
	if (name == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	data = new LOGFONT();
	setName(name);
	setHeight(height);
	setStyle(style);
	// We set the charset field so that
	// wildcard searching will work properly
	// out of the box
	data.lfCharSet = OS.DEFAULT_CHARSET;
}

/**
 * Compares the argument to the receiver, and returns true
 * if they represent the <em>same</em> object using a class
 * specific comparison.
 *
 * @param object the object to compare with this object
 * @return <code>true</code> if the object is the same as this object and <code>false</code> otherwise
 *
 * @see #hashCode
 */
public boolean equals (Object object) {
	if (object == this) return true;
	if (!(object instanceof FontData)) return false;
	LOGFONT lf = ((FontData)object).data;
	return data.lfCharSet == lf.lfCharSet && 
		data.lfHeight == lf.lfHeight &&
		data.lfWidth == lf.lfWidth &&
		data.lfEscapement == lf.lfEscapement &&
		data.lfOrientation == lf.lfOrientation &&
		data.lfWeight == lf.lfWeight &&
		data.lfItalic == lf.lfItalic &&
		data.lfUnderline == lf.lfUnderline &&
		data.lfStrikeOut == lf.lfStrikeOut &&
		data.lfCharSet == lf.lfCharSet &&
		data.lfOutPrecision == lf.lfOutPrecision &&
		data.lfClipPrecision == lf.lfClipPrecision &&
		data.lfQuality == lf.lfQuality &&
		data.lfPitchAndFamily == lf.lfPitchAndFamily &&
		data.lfFaceName0 == lf.lfFaceName0 &&
		data.lfFaceName1 == lf.lfFaceName1 &&
		data.lfFaceName2 == lf.lfFaceName2 &&
		data.lfFaceName3 == lf.lfFaceName3 &&
		data.lfFaceName4 == lf.lfFaceName4 &&
		data.lfFaceName5 == lf.lfFaceName5 &&
		data.lfFaceName6 == lf.lfFaceName6 &&
		data.lfFaceName7 == lf.lfFaceName7 &&
		data.lfFaceName8 == lf.lfFaceName8 &&
		data.lfFaceName9 == lf.lfFaceName9 &&
		data.lfFaceName10 == lf.lfFaceName10 &&
		data.lfFaceName11 == lf.lfFaceName11 &&
		data.lfFaceName12 == lf.lfFaceName12 &&
		data.lfFaceName13 == lf.lfFaceName13 &&
		data.lfFaceName14 == lf.lfFaceName14 &&
		data.lfFaceName15 == lf.lfFaceName15 &&
		data.lfFaceName16 == lf.lfFaceName16 &&
		data.lfFaceName17 == lf.lfFaceName17 &&
		data.lfFaceName18 == lf.lfFaceName18 &&
		data.lfFaceName19 == lf.lfFaceName19 &&
		data.lfFaceName20 == lf.lfFaceName20 &&
		data.lfFaceName21 == lf.lfFaceName21 &&
		data.lfFaceName22 == lf.lfFaceName22 &&
		data.lfFaceName23 == lf.lfFaceName23 &&
		data.lfFaceName24 == lf.lfFaceName24 &&
		data.lfFaceName25 == lf.lfFaceName25 &&
		data.lfFaceName26 == lf.lfFaceName26 &&
		data.lfFaceName27 == lf.lfFaceName27 &&
		data.lfFaceName28 == lf.lfFaceName28 &&
		data.lfFaceName29 == lf.lfFaceName29 &&
		data.lfFaceName30 == lf.lfFaceName30 &&
		data.lfFaceName31 == lf.lfFaceName31;
}

int EnumLocalesProc(int lpLocaleString) {
	
	/* Get the locale ID */
	int length = 8;
	TCHAR buffer = new TCHAR(0, length);
	OS.MoveMemory(buffer, lpLocaleString, length);
	int lcid = Integer.parseInt(buffer.toString(0, buffer.strlen ()), 16);

	/* Check the language */
	int size = OS.GetLocaleInfo(lcid, OS.LOCALE_SISO639LANGNAME, buffer, length);
	String lang = buffer.toString(0, size - 1);
	if (!locale.getLanguage().equals(lang)) return 1;

	/* Check the country */
	String javaCountry = locale.getCountry();
	if (javaCountry.length() != 0) {
		size = OS.GetLocaleInfo(lcid, OS.LOCALE_SISO3166CTRYNAME, buffer, length);
		String country = buffer.toString(0, size - 1);
		if (!javaCountry.equals(country)) return 1;
	}

	/* Get the charset */
	size = OS.GetLocaleInfo(lcid, OS.LOCALE_IDEFAULTANSICODEPAGE, buffer, length);
	int cp = Integer.parseInt(buffer.toString(0, size - 1));
	int [] lpCs = new int[8];
	OS.TranslateCharsetInfo(cp, lpCs, OS.TCI_SRCCODEPAGE);
	data.lfCharSet = (byte)lpCs[0];

	return 0;
}

/**
 * Returns the height of the receiver in points.
 *
 * @return the height of this FontData
 *
 * @see #setHeight
 */
public int getHeight() {
	return height;
}

/**
 * Returns the name of the receiver.
 * On platforms that support font foundries, the return value will
 * be the foundry followed by a dash ("-") followed by the face name.
 *
 * @return the name of this <code>FontData</code>
 *
 * @see #setName
 */
public String getName() {
	char[] chars = {
		data.lfFaceName0,  data.lfFaceName1,  data.lfFaceName2,  data.lfFaceName3,
		data.lfFaceName4,  data.lfFaceName5,  data.lfFaceName6,  data.lfFaceName7,
		data.lfFaceName8,  data.lfFaceName9,  data.lfFaceName10, data.lfFaceName11,
		data.lfFaceName12, data.lfFaceName13, data.lfFaceName14, data.lfFaceName15,
		data.lfFaceName16, data.lfFaceName17, data.lfFaceName18, data.lfFaceName19,
		data.lfFaceName20, data.lfFaceName21, data.lfFaceName22, data.lfFaceName23,
		data.lfFaceName24, data.lfFaceName25, data.lfFaceName26, data.lfFaceName27,
		data.lfFaceName28, data.lfFaceName29, data.lfFaceName30, data.lfFaceName31,
	};
	int index = 0;
	while (index < chars.length) {
		if (chars [index] == 0) break;
		index++;
	}
	return new String (chars, 0, index);
}

/**
 * Returns the style of the receiver which is a bitwise OR of 
 * one or more of the <code>SWT</code> constants NORMAL, BOLD
 * and ITALIC.
 *
 * @return the style of this <code>FontData</code>
 * 
 * @see #setStyle
 */
public int getStyle() {
	int style = SWT.NORMAL;
	if (data.lfWeight == 700) style |= SWT.BOLD;
	if (data.lfItalic != 0) style |= SWT.ITALIC;
	return style;
}

/**
 * Returns an integer hash code for the receiver. Any two 
 * objects which return <code>true</code> when passed to 
 * <code>equals</code> must return the same value for this
 * method.
 *
 * @return the receiver's hash
 *
 * @see #equals
 */
public int hashCode () {
	return data.lfCharSet ^ data.lfHeight ^ data.lfWidth ^ data.lfEscapement ^
		data.lfOrientation ^ data.lfWeight ^ data.lfItalic ^data.lfUnderline ^
		data.lfStrikeOut ^ data.lfCharSet ^ data.lfOutPrecision ^
		data.lfClipPrecision ^ data.lfQuality ^ data.lfPitchAndFamily ^
		data.lfFaceName0 ^ data.lfFaceName1 ^ data.lfFaceName2 ^
		data.lfFaceName3 ^ data.lfFaceName4 ^ data.lfFaceName5 ^
		data.lfFaceName6 ^ data.lfFaceName7 ^ data.lfFaceName8 ^
		data.lfFaceName9 ^ data.lfFaceName10 ^ data.lfFaceName11 ^
		data.lfFaceName12 ^ data.lfFaceName13 ^ data.lfFaceName14 ^
		data.lfFaceName15 ^ data.lfFaceName16 ^ data.lfFaceName17 ^
		data.lfFaceName18 ^ data.lfFaceName19 ^ data.lfFaceName20 ^
		data.lfFaceName21 ^ data.lfFaceName22 ^ data.lfFaceName23 ^
		data.lfFaceName24 ^ data.lfFaceName25 ^ data.lfFaceName26 ^
		data.lfFaceName27 ^ data.lfFaceName28 ^ data.lfFaceName29 ^
		data.lfFaceName30 ^	data.lfFaceName31;
}

/**
 * Sets the height of the receiver. The parameter is
 * specified in terms of points, where a point is one
 * seventy-second of an inch.
 *
 * @param height the height of the <code>FontData</code>
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the height is negative</li>
 * </ul>
 * 
 * @see #getHeight
 */
public void setHeight(int height) {
	if (height < 0) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	this.height = height;
}

/**
 * Sets the locale of the receiver.
 * <p>
 * The locale determines which platform character set this
 * font is going to use. Widgets and graphics operations that
 * use this font will convert UNICODE strings to the platform
 * character set of the specified locale.
 * </p>
 * <p>
 * On platforms which there are multiple character sets for a
 * given language/country locale, the variant portion of the
 * locale will determine the character set.
 * </p>
 * 
 * @param locale the Locale of the <code>FontData</code>
 */
public void setLocale(Locale locale) {
	this.locale = locale;
	if (locale == null) {
		data.lfCharSet = OS.DEFAULT_CHARSET;
	} else {
		Callback callback = new Callback (this, "EnumLocalesProc", 1);
		int lpEnumLocalesProc = callback.getAddress ();	
		OS.EnumSystemLocales(lpEnumLocalesProc, OS.LCID_SUPPORTED);
		callback.dispose ();
	}
}

/**
 * Sets the name of the receiver.
 * <p>
 * Some platforms support font foundries. On these platforms, the name
 * of the font specified in setName() may have one of the following forms:
 * <ol>
 * <li>a face name (for example, "courier")</li>
 * <li>a foundry followed by a dash ("-") followed by a face name (for example, "adobe-courier")</li>
 * </ol>
 * In either case, the name returned from getName() will include the
 * foundry.
 * </p>
 * <p>
 * On platforms that do not support font foundries, only the face name
 * (for example, "courier") is used in <code>setName()</code> and 
 * <code>getName()</code>.
 * </p>
 *
 * @param name the name of the font data (must not be null)
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - when the font name is null</li>
 * </ul>
 *
 * @see #getName
 */
public void setName(String name) {
	if (name == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	char [] chars = new char [32];

	/* The field lfFaceName must be NULL terminated */
	int length = name.length();
	name.getChars (0, length <= 31 ? length : 31, chars, 0);
	data.lfFaceName0 = chars[0];
	data.lfFaceName1 = chars[1];
	data.lfFaceName2 = chars[2];
	data.lfFaceName3 = chars[3];
	data.lfFaceName4 = chars[4];
	data.lfFaceName5 = chars[5];
	data.lfFaceName6 = chars[6];
	data.lfFaceName7 = chars[7];
	data.lfFaceName8 = chars[8];
	data.lfFaceName9 = chars[9];
	data.lfFaceName10 = chars[10];
	data.lfFaceName11 = chars[11];
	data.lfFaceName12 = chars[12];
	data.lfFaceName13 = chars[13];
	data.lfFaceName14 = chars[14];
	data.lfFaceName15 = chars[15];
	data.lfFaceName16 = chars[16];
	data.lfFaceName17 = chars[17];
	data.lfFaceName18 = chars[18];
	data.lfFaceName19 = chars[19];
	data.lfFaceName20 = chars[20];
	data.lfFaceName21 = chars[21];
	data.lfFaceName22 = chars[22];
	data.lfFaceName23 = chars[23];
	data.lfFaceName24 = chars[24];
	data.lfFaceName25 = chars[25];
	data.lfFaceName26 = chars[26];
	data.lfFaceName27 = chars[27];
	data.lfFaceName28 = chars[28];
	data.lfFaceName29 = chars[29];
	data.lfFaceName30 = chars[30];
	data.lfFaceName31 = chars[31];
}

/**
 * Sets the style of the receiver to the argument which must
 * be a bitwise OR of one or more of the <code>SWT</code> 
 * constants NORMAL, BOLD and ITALIC.
 *
 * @param style the new style for this <code>FontData</code>
 *
 * @see #getStyle
 */
public void setStyle(int style) {
	if ((style & SWT.BOLD) == SWT.BOLD) {
		data.lfWeight = 700;
	} else {
		data.lfWeight = 0;
	}
	if ((style & SWT.ITALIC) == SWT.ITALIC) {
		data.lfItalic = 1;
	} else {
		data.lfItalic = 0;
	}
}

/**
 * Returns a string representation of the receiver which is suitable
 * for constructing an equivalent instance using the 
 * <code>FontData(String)</code> constructor.
 *
 * @return a string representation of the FontData
 *
 * @see FontData
 */
public String toString() {
	StringBuffer buffer = new StringBuffer();
	buffer.append("1|");
	buffer.append(getName());
	buffer.append("|");
	buffer.append(getHeight());
	buffer.append("|");
	buffer.append(getStyle());
	buffer.append("|");
	buffer.append("WINDOWS|1|");	
	buffer.append(data.lfHeight);
	buffer.append("|");
	buffer.append(data.lfWidth);    
	buffer.append("|");
	buffer.append(data.lfEscapement); 
	buffer.append("|");
	buffer.append(data.lfOrientation);    
	buffer.append("|");
	buffer.append(data.lfWeight);    
	buffer.append("|");
	buffer.append(data.lfItalic);    
	buffer.append("|");
	buffer.append(data.lfUnderline); 
	buffer.append("|");
	buffer.append(data.lfStrikeOut);    
	buffer.append("|");
	buffer.append(data.lfCharSet);    
	buffer.append("|");
	buffer.append(data.lfOutPrecision); 
	buffer.append("|");
	buffer.append(data.lfClipPrecision);    
	buffer.append("|");
	buffer.append(data.lfQuality);    
	buffer.append("|");
	buffer.append(data.lfPitchAndFamily);
	buffer.append("|");
	char[] faceName = {
		data.lfFaceName0,  data.lfFaceName1,  data.lfFaceName2,  data.lfFaceName3,
		data.lfFaceName4,  data.lfFaceName5,  data.lfFaceName6,  data.lfFaceName7,
		data.lfFaceName8,  data.lfFaceName9,  data.lfFaceName10, data.lfFaceName11,
		data.lfFaceName12, data.lfFaceName13, data.lfFaceName14, data.lfFaceName15,
		data.lfFaceName16, data.lfFaceName17, data.lfFaceName18, data.lfFaceName19,
		data.lfFaceName20, data.lfFaceName21, data.lfFaceName22, data.lfFaceName23,
		data.lfFaceName24, data.lfFaceName25, data.lfFaceName26, data.lfFaceName27,
		data.lfFaceName28, data.lfFaceName29, data.lfFaceName30, data.lfFaceName31,
	};
	int i = 0;
	while (i < faceName.length && faceName[i] != 0) {
		buffer.append(faceName[i++]);
	}
	return buffer.toString();
}

/**	 
 * Invokes platform specific functionality to allocate a new font data.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>FontData</code>. It is marked public only so that
 * it can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param data the <code>LOGFONT</code> for the font data
 * 
 * @private
 */
public static FontData win32_new(LOGFONT data, int height) {
	return new FontData(data, height);
}

}
