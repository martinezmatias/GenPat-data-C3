package org.eclipse.swt.graphics;

/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 1998, 2001  All Rights Reserved
 */
 
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.*;

public abstract class Device implements Drawable {
	
	/* Debugging */
	public static boolean DEBUG;
	boolean debug = DEBUG;
	boolean tracking = DEBUG;
	Error [] errors;
	Object [] objects;
	
	/* Palette */
	public int hPalette = 0;
	int [] colorRefCount;

	/* Font Enumeration */
	int nFonts = 256;
	LOGFONT [] logFonts;

	boolean disposed;
	
/**
 * Constructs a new instance of this class.
 * <p>
 * You must dispose the device when it is no longer required. 
 * </p>
 *
 * @param data the DeviceData which describes the receiver
 *
 * @see #create
 * @see #init
 * @see DeviceData
 */
public Device(DeviceData data) {
	create (data);
	init ();
	if (data != null) {
		debug = data.debug;
		tracking = data.tracking;
	}
	if (tracking) {
		errors = new Error [128];
		objects = new Object [128];
	}
}

/*
 * Temporary code.
 */	
static Device getDevice () {
	Device device = null;
	try {
		Class clazz = Class.forName ("org.eclipse.swt.widgets.Display");
		java.lang.reflect.Method method = clazz.getMethod("getCurrent", new Class[0]);
		device = (Device) method.invoke(clazz, new Object[0]);
		if (device == null) {
			method = clazz.getMethod("getDefault", new Class[0]);
			device = (Device)method.invoke(clazz, new Object[0]);
		}
	} catch (Throwable e) {};
	return device;
}

/**
 * Throws an <code>SWTException</code> if the receiver can not
 * be accessed by the caller. This may include both checks on
 * the state of the receiver and more generally on the entire
 * execution context. This method <em>should</em> be called by
 * device implementors to enforce the standard SWT invariants.
 * <p>
 * Currently, it is an error to invoke any method (other than
 * <code>isDisposed()</code> and <code>dispose()</code>) on a
 * device that has had its <code>dispose()</code> method called.
 * </p><p>
 * In future releases of SWT, there may be more or fewer error
 * checks and exceptions may be thrown for different reasons.
 * <p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
protected void checkDevice () {
	if (disposed) SWT.error(SWT.ERROR_DEVICE_DISPOSED);
}

/**
 * Creates the device in the operating system.  If the device
 * does not have a handle, this method may do nothing depending
 * on the device.
 * <p>
 * This method is called before <code>init</code>.
 * </p><p>
 * Subclasses are supposed to reimplement this method and not
 * call the <code>super</code> implementation.
 * </p>
 *
 * @param data the DeviceData which describes the receiver
 *
 * @see #init
 */
protected void create (DeviceData data) {
}

int computePixels(int height) {
	int hDC = internal_new_GC (null);
	/* Avoid using Math.round() */
	int pixels = -(int)((height * OS.GetDeviceCaps(hDC, OS.LOGPIXELSY) / 72.0f) + 0.5f);
	internal_dispose_GC (hDC, null);
	return pixels;
}

int computePoints(LOGFONT logFont) {
	int hDC = internal_new_GC (null);
	int logPixelsY = OS.GetDeviceCaps(hDC, OS.LOGPIXELSY);
	int pixels = 0; 
	if (logFont.lfHeight > 0) {
		/*
		 * Feature in Windows. If the lfHeight of the LOGFONT structure
		 * is positive, the lfHeight measures the height of the entire
		 * cell, including internal leading, in logical units. Since the
		 * height of a font in points does not include the internal leading,
		 * we must subtract the internal leading, which requires a TEXTMETRIC,
		 * which in turn requires font creation.
		 */
		int hFont = OS.CreateFontIndirect(logFont);
		int oldFont = OS.SelectObject(hDC, hFont);
		TEXTMETRIC lptm = new TEXTMETRIC();
		OS.GetTextMetrics(hDC, lptm);
		OS.SelectObject(hDC, oldFont);
		OS.DeleteObject(hFont);
		pixels = logFont.lfHeight - lptm.tmInternalLeading;
	} else {
		pixels = -logFont.lfHeight;
	}
	internal_dispose_GC (hDC, null);

	/* Avoid using Math.round() */
	return (int)((pixels * 72.0f / logPixelsY) + 0.5f);
}

/**
 * Destroys the device in the operating system and releases
 * the device's handle.  If the device does not have a handle,
 * this method may do nothing depending on the device.
 * <p>
 * This method is called after <code>release</code>.
 * </p><p>
 * Subclasses are supposed to reimplement this method and not
 * call the <code>super</code> implementation.
 * </p>
 *
 * @see #dispose
 * @see #release
 */
protected void destroy () {
}

/**
 * Disposes of the operating system resources associated with
 * the receiver. After this method has been invoked, the receiver
 * will answer <code>true</code> when sent the message
 * <code>isDisposed()</code>.
 *
 * @see #release
 * @see #destroy
 * @see #checkDevice
 */
public void dispose () {
	if (isDisposed()) return;
	checkDevice ();
	release ();
	destroy ();
	disposed = true;
	if (tracking) {
		objects = null;
		errors = null;
	}
}

void dispose_Object (Object object) {
	for (int i=0; i<objects.length; i++) {
		if (objects [i] == object) {
			objects [i] = null;
			errors [i] = null;
			return;
		}
	}
}

int EnumFontFamProc (int lpelfe, int lpntme, int FontType, int lParam) {
	boolean isScalable = (FontType & OS.RASTER_FONTTYPE) == 0;
	if ((lParam == 1) != isScalable) return 1;
	
	/* Add the log font to the list of log fonts */
	if (nFonts == logFonts.length) {
		LOGFONT [] newLogFonts = new LOGFONT [logFonts.length + 128];
		System.arraycopy (logFonts, 0, newLogFonts, 0, nFonts);
		logFonts = newLogFonts;
	}
	LOGFONT logFont = logFonts [nFonts];
	if (logFont == null) logFont = new LOGFONT ();
	OS.MoveMemory (logFont, lpelfe, LOGFONT.sizeof);
	logFonts [nFonts++] = logFont;
	return 1;
}

/**
 * Returns a rectangle describing the receiver's size and location.
 *
 * @return the bounding rectangle
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Rectangle getBounds () {
	checkDevice ();
	int hDC = internal_new_GC (null);
	int width = OS.GetDeviceCaps (hDC, OS.HORZRES);
	int height = OS.GetDeviceCaps (hDC, OS.VERTRES);
	internal_dispose_GC (hDC, null);
	return new Rectangle (0, 0, width, height);
}

/**
 * Returns a <code>DeviceData</code> based on the receiver.
 * Modifications made to this <code>DeviceData</code> will not
 * affect the receiver.
 *
 * @return a <code>DeviceData</code> containing the device's data and attributes
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see DeviceData
 */
public DeviceData getDeviceData () {
	checkDevice();
	DeviceData data = new DeviceData ();
	data.debug = debug;
	data.tracking = tracking;
	int count = 0, length = 0;
	if (tracking) length = objects.length;
	for (int i=0; i<length; i++) {
		if (objects [i] != null) count++;
	}
	int index = 0;
	data.objects = new Object [count];
	data.errors = new Error [count];
	for (int i=0; i<length; i++) {
		if (objects [i] != null) {
			data.objects [index] = objects [i];
			data.errors [index] = errors [i];
			index++;
		}
	}
	return data;
}

/**
 * Returns a rectangle which describes the area of the
 * receiver which is capable of displaying data.
 * 
 * @return the client area
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see #getBounds
 * @see #computeTrim
 */
public Rectangle getClientArea () {
	return getBounds ();
}

/**
 * Returns the bit depth of the screen, which is the number of
 * bits it takes to represent the number of unique colors that
 * the screen is currently capable of displaying. This number 
 * will typically be one of 1, 8, 15, 16, 24 or 32.
 *
 * @return the depth of the screen
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public int getDepth () {
	checkDevice ();
	int hDC = internal_new_GC (null);
	int bits = OS.GetDeviceCaps (hDC, OS.BITSPIXEL);
	int planes = OS.GetDeviceCaps (hDC, OS.PLANES);
	internal_dispose_GC (hDC, null);
	return bits * planes;
}

/**
 * Returns a point whose x coordinate is the horizontal
 * dots per inch of the display, and whose y coordinate
 * is the vertical dots per inch of the display.
 *
 * @return the horizontal and vertical DPI
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Point getDPI () {
	checkDevice ();
	int hDC = internal_new_GC (null);
	int dpiX = OS.GetDeviceCaps (hDC, OS.LOGPIXELSX);
	int dpiY = OS.GetDeviceCaps (hDC, OS.LOGPIXELSY);
	internal_dispose_GC (hDC, null);
	return new Point (dpiX, dpiY);
}

/**
 * Returns <code>FontData</code> objects which describe
 * the fonts which match the given arguments. If the
 * <code>faceName</code> is null, all fonts will be returned.
 *
 * @param faceName the name of the font to look for, or null
 * @param scalable true if scalable fonts should be returned.
 * @return the matching font data
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public FontData [] getFontList (String faceName, boolean scalable) {
	checkDevice ();
	
	/* Create the callback */
	Callback callback = new Callback (this, "EnumFontFamProc", 4);
	int lpEnumFontFamProc = callback.getAddress ();
		
	/* Initialize the instance variables */
	logFonts = new LOGFONT [nFonts];
	for (int i=0; i<logFonts.length; i++) {
		logFonts [i] = new LOGFONT ();
	}
	nFonts = 0;

	/* Enumerate */
	int offset = 0;
	int hDC = internal_new_GC (null);
	if (faceName == null) {	
		/* The user did not specify a face name, so they want all versions of all available face names */
		OS.EnumFontFamilies (hDC, null, lpEnumFontFamProc, scalable ? 1 : 0);
		
		/**
		 * For bitmapped fonts, EnumFontFamilies only enumerates once for each font, regardless
		 * of how many styles are available. If the user wants bitmapped fonts, enumerate on
		 * each face name now.
		 */
		offset = nFonts;
		for (int i=0; i<offset; i++) {
			LOGFONT lf = logFonts [i];
			/**
			 * Bug in Windows 98. When EnumFontFamiliesEx is called with a specified face name, it
			 * should enumerate for each available style of that font. Instead, it only enumerates
			 * once. The fix is to call EnumFontFamilies, which works as expected.
			 */
			byte [] lpFaceName = new byte [] {
				lf.lfFaceName0,  lf.lfFaceName1,  lf.lfFaceName2,  lf.lfFaceName3,
				lf.lfFaceName4,  lf.lfFaceName5,  lf.lfFaceName6,  lf.lfFaceName7,
				lf.lfFaceName8,  lf.lfFaceName9,  lf.lfFaceName10, lf.lfFaceName11,
				lf.lfFaceName12, lf.lfFaceName13, lf.lfFaceName14, lf.lfFaceName15,
				lf.lfFaceName16, lf.lfFaceName17, lf.lfFaceName18, lf.lfFaceName19,
				lf.lfFaceName20, lf.lfFaceName21, lf.lfFaceName22, lf.lfFaceName23,
				lf.lfFaceName24, lf.lfFaceName25, lf.lfFaceName26, lf.lfFaceName27,
				lf.lfFaceName28, lf.lfFaceName29, lf.lfFaceName30, lf.lfFaceName31,
			};
			OS.EnumFontFamilies (hDC, lpFaceName, lpEnumFontFamProc, scalable ? 1 : 0);
		}
	} else {
		byte [] lpFaceName = Converter.wcsToMbcs (0, faceName, true);
		/**
		 * Bug in Windows 98. When EnumFontFamiliesEx is called with a specified face name, it
		 * should enumerate for each available style of that font. Instead, it only enumerates
		 * once. The fix is to call EnumFontFamilies, which works as expected.
		 */
		OS.EnumFontFamilies (hDC, lpFaceName, lpEnumFontFamProc, scalable ? 1 : 0);
	}
	internal_dispose_GC (hDC, null);

	/* Create the fontData from the logfonts */
	int count = nFonts - offset;
	FontData [] result = new FontData [count];
	for (int i=0; i<count; i++) {
		LOGFONT logFont = logFonts [i+offset];
		result [i] = FontData.win32_new (logFont, computePoints(logFont));
	}
	
	/* Clean up */
	callback.dispose ();
	logFonts = null;
	return result;
}

/**
 * Returns the matching standard color for the given
 * constant, which should be one of the color constants
 * specified in class <code>SWT</code>. Any value other
 * than one of the SWT color constants which is passed
 * in will result in the color black. This color should
 * not be free'd because it was allocated by the system,
 * not the application.
 *
 * @param id the color constant
 * @return the matching color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see SWT
 */
public Color getSystemColor (int id) {
	checkDevice ();
	int pixel = 0x02000000;
	switch (id) {
		case SWT.COLOR_WHITE:				pixel = 0x02FFFFFF;  break;
		case SWT.COLOR_BLACK:				pixel = 0x02000000;  break;
		case SWT.COLOR_RED:					pixel = 0x020000FF;  break;
		case SWT.COLOR_DARK_RED:			pixel = 0x02000080;  break;
		case SWT.COLOR_GREEN:				pixel = 0x0200FF00;  break;
		case SWT.COLOR_DARK_GREEN:			pixel = 0x02008000;  break;
		case SWT.COLOR_YELLOW:				pixel = 0x0200FFFF;  break;
		case SWT.COLOR_DARK_YELLOW:			pixel = 0x02008080;  break;
		case SWT.COLOR_BLUE:				pixel = 0x02FF0000;  break;
		case SWT.COLOR_DARK_BLUE:			pixel = 0x02800000;  break;
		case SWT.COLOR_MAGENTA:				pixel = 0x02FF00FF;  break;
		case SWT.COLOR_DARK_MAGENTA:		pixel = 0x02800080;  break;
		case SWT.COLOR_CYAN:				pixel = 0x02FFFF00;  break;
		case SWT.COLOR_DARK_CYAN:			pixel = 0x02808000;  break;
		case SWT.COLOR_GRAY:				pixel = 0x02C0C0C0;  break;
		case SWT.COLOR_DARK_GRAY:			pixel = 0x02808080;  break;
	}
	return Color.win32_new (this, pixel);
}

/**
 * Returns a reasonable font for applications to use.
 * On some platforms, this will match the "default font"
 * or "system font" if such can be found.  This font
 * should not be free'd because it was allocated by the
 * system, not the application.
 * <p>
 * Typically, applications which want the default look
 * should simply not set the font on the widgets they
 * create. Widgets are always created with the correct
 * default font for the class of user-interface component
 * they represent.
 * </p>
 *
 * @return a font
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Font getSystemFont () {
	checkDevice ();
	int hFont = OS.GetStockObject (OS.SYSTEM_FONT);
	return Font.win32_new (this, hFont);
}

/**
 * Returns <code>true</code> if the underlying window system prints out
 * warning messages on the console, and <code>setWarnings</code>
 * had previously been called with <code>true</code>.
 *
 * @return <code>true</code>if warnings are being handled, and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public boolean getWarnings () {
	checkDevice ();
	return false;
}

/**
 * Initializes any internal resources needed by the
 * device.
 * <p>
 * This method is called after <code>create</code>.
 * </p><p>
 * If subclasses reimplement this method, they must
 * call the <code>super</code> implementation.
 * </p>
 * 
 * @see #create
 */
protected void init () {
	
	/*
	 * If we're not on a device which supports palettes,
	 * don't create one.
	 */
	int hDC = internal_new_GC (null);
	int rc = OS.GetDeviceCaps (hDC, OS.RASTERCAPS);
	int bits = OS.GetDeviceCaps (hDC, OS.BITSPIXEL);
	int planes = OS.GetDeviceCaps (hDC, OS.PLANES);
	internal_dispose_GC (hDC, null);
	
	bits *= planes;
	if ((rc & OS.RC_PALETTE) == 0 || bits != 8) return;

	/*
	 * The following colors are listed in the Windows
	 * Programmer's Reference as the colors guaranteed
	 * to be in the default system palette.
	 */
	RGB [] rgbs = new RGB [] {
		new RGB (0,0,0),
		new RGB (0x80,0,0),
		new RGB (0,0x80,0),
		new RGB (0x80,0x80,0),
		new RGB (0,0,0x80),
		new RGB (0x80,0,0x80),
		new RGB (0,0x80,0x80),
		new RGB (0xC0,0xC0,0xC0),
		new RGB (0x80,0x80,0x80),
		new RGB (0xFF,0,0),
		new RGB (0,0xFF,0),
		new RGB (0xFF,0xFF,0),
		new RGB (0,0,0xFF),
		new RGB (0xFF,0,0xFF),
		new RGB (0,0xFF,0xFF),
		new RGB (0xFF,0xFF,0xFF),
	};
	
	/* 4 bytes header + 4 bytes per entry * 256 entries */
	byte [] logPalette = new byte [4 + 4 * 256];
	
	/* 2 bytes = special header */
	logPalette [0] = 0x00;
	logPalette [1] = 0x03;
	
	/* 2 bytes = number of colors, LSB first */
	logPalette [2] = 0;
	logPalette [3] = 1;
	
	/* Create the palette and reference counter */
	colorRefCount = new int [256];
	for (int i = 0; i < rgbs.length; i++) {
		colorRefCount [i] = 1;
		int offset = i * 4 + 4;
		logPalette [offset] = (byte) rgbs[i].red;
		logPalette [offset + 1] = (byte) rgbs[i].green;
		logPalette [offset + 2] = (byte) rgbs[i].blue;
	}
	hPalette = OS.CreatePalette (logPalette);
}

/**	 
 * Invokes platform specific functionality to allocate a new GC handle.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Device</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param data the platform specific GC data 
 * @return the platform specific GC handle
 *
 * @private
 */
public abstract int internal_new_GC (GCData data);

/**	 
 * Invokes platform specific functionality to dispose a GC handle.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Device</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param handle the platform specific GC handle
 * @param data the platform specific GC data 
 *
 * @private
 */
public abstract void internal_dispose_GC (int hDC, GCData data);

/**
 * Returns <code>true</code> if the device has been disposed,
 * and <code>false</code> otherwise.
 * <p>
 * This method gets the dispose state for the device.
 * When a device has been disposed, it is an error to
 * invoke any other method using the device.
 *
 * @return <code>true</code> when the device is disposed and <code>false</code> otherwise
 */
public boolean isDisposed () {
	return disposed;
}

void new_Object (Object object) {
	for (int i=0; i<objects.length; i++) {
		if (objects [i] == null) {
			objects [i] = object;
			errors [i] = new Error ();
			return;
		}
	}
	Object [] newObjects = new Object [objects.length + 128];
	System.arraycopy (objects, 0, newObjects, 0, objects.length);
	newObjects [objects.length] = object;
	objects = newObjects;
	Error [] newErrors = new Error [errors.length + 128];
	System.arraycopy (errors, 0, newErrors, 0, errors.length);
	newErrors [errors.length] = new Error ();
	errors = newErrors;
}

/**
 * Releases any internal resources back to the operating
 * system and clears all fields except the device handle.
 * <p>
 * When a device is destroyed, resources that were acquired
 * on behalf of the programmer need to be returned to the
 * operating system.  For example, if the device allocated a
 * font to be used as the system font, this font would be
 * freed in <code>release</code>.  Also,to assist the garbage
 * collector and minimize the amount of memory that is not
 * reclaimed when the programmer keeps a reference to a
 * disposed device, all fields except the handle are zero'd.
 * The handle is needed by <code>destroy</code>.
 * </p>
 * This method is called before <code>destroy</code>.
 * </p><p>
 * If subclasses reimplement this method, they must
 * call the <code>super</code> implementation.
 * </p>
 *
 * @see #dispose
 * @see #destroy
 */
protected void release () {
	if (hPalette != 0) OS.DeleteObject (hPalette);
	hPalette = 0;
	colorRefCount = null;
	logFonts = null;
	nFonts = 0;
}

/**
 * If the underlying window system supports printing warning messages
 * to the console, setting warnings to <code>true</code> prevents these
 * messages from being printed. If the argument is <code>false</code>
 * message printing is not blocked.
 *
 * @param warnings <code>true</code>if warnings should be handled, and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public void setWarnings (boolean warnings) {
	checkDevice ();
}

}