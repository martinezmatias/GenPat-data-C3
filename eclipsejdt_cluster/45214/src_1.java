package org.eclipse.jdt.internal.compiler.util;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.io.*;

public class Util {
	/* Bundle containing messages */
	protected static ResourceBundle bundle;
	private final static String bundleName = "org.eclipse.jdt.internal.compiler.util.messages"; //$NON-NLS-1$
	static {
		relocalize();
	}
/**
 * Lookup the message with the given ID in this catalog and bind its
 * substitution locations with the given strings.
 */
public static String bind(String id, String binding1, String binding2) {
	return bind(id, new String[] {binding1, binding2});
}
/**
 * Lookup the message with the given ID in this catalog and bind its
 * substitution locations with the given string.
 */
public static String bind(String id, String binding) {
	return bind(id, new String[] {binding});
}
/**
 * Lookup the message with the given ID in this catalog and bind its
 * substitution locations with the given string values.
 */
public static String bind(String id, String[] bindings) {
	if (id == null)
		return "No message available"; //$NON-NLS-1$
	String message = null;
	try {
		message = bundle.getString(id);
	} catch (MissingResourceException e) {
		// If we got an exception looking for the message, fail gracefully by just returning
		// the id we were looking for.  In most cases this is semi-informative so is not too bad.
		return "Missing message: " + id + " in: " + bundleName; //$NON-NLS-2$ //$NON-NLS-1$
	}
	if (bindings == null)
		return message;
	int length = message.length();
	int start = -1;
	int end = length;
	StringBuffer output = new StringBuffer(80);
	while (true) {
		if ((end = message.indexOf('{', start)) > -1) {
			output.append(message.substring(start + 1, end));
			if ((start = message.indexOf('}', end)) > -1) {
				int index = -1;
				try {
					index = Integer.parseInt(message.substring(end + 1, start));
					output.append(bindings[index]);
				} catch (NumberFormatException nfe) {
					output.append(message.substring(end + 1, start + 1));
				} catch (ArrayIndexOutOfBoundsException e) {
					output.append("{missing " + Integer.toString(index) + "}"); //$NON-NLS-2$ //$NON-NLS-1$
				}
			} else {
				output.append(message.substring(end, length));
				break;
			}
		} else {
			output.append(message.substring(start + 1, length));
			break;
		}
	}
	return output.toString();
}
/**
 * Lookup the message with the given ID in this catalog 
 */
public static String bind(String id) {
	return bind(id, (String[])null);
}
/**
 * Creates a NLS catalog for the given locale.
 */
public static void relocalize() {
	bundle = ResourceBundle.getBundle(bundleName, Locale.getDefault());
}
/**
 * Returns the given bytes as a char array.
 */
public static char[] bytesToChar(byte[] bytes) throws IOException {

	return getInputStreamAsCharArray(new ByteArrayInputStream(bytes));

}
/**
 * Returns the given input stream's contents as a byte array.
 * Closes the stream before returning;
 * @throws IOException if a problem occured reading the stream.
 */
public static byte[] getInputStreamAsByteArray(InputStream stream) throws IOException {
	byte[] contents = new byte[0];
	try {
		int contentsLength = 0;
		int bytesRead = -1;
		do {
			int available= stream.available();
			
			// resize contents if needed
			if (contentsLength + available > contents.length) {
				System.arraycopy(contents, 0, contents = new byte[contentsLength + available], 0, contentsLength);
			}

			// read as many bytes as possible
			bytesRead = stream.read(contents, contentsLength, available);
			
			if (bytesRead > 0) {
				// remember length of contents
				contentsLength += bytesRead;
			}
		} while (bytesRead > 0);
		
		// resize contents if necessary
		if (contentsLength < contents.length) {
			System.arraycopy(contents, 0, contents = new byte[contentsLength], 0, contentsLength);
		}
	} finally {
		try {
			stream.close();
		} catch (IOException e) {
		}
	}
	return contents;
}
/**
 * Returns the given input stream's contents as a character array.
 * Closes the stream before returning;
 * @throws IOException if a problem occured reading the stream.
 */
public static char[] getInputStreamAsCharArray(InputStream stream) throws IOException {
	InputStreamReader reader= null;
	reader= new InputStreamReader(stream);
	char[] contents = new char[0];
	try {
		int contentsLength = 0;
		int charsRead = -1;
		do {
			int available= stream.available();
			
			// resize contents if needed
			if (contentsLength + available > contents.length) {
				System.arraycopy(contents, 0, contents = new char[contentsLength + available], 0, contentsLength);
			}

			// read as many chars as possible
			charsRead = reader.read(contents, contentsLength, available);
			
			if (charsRead > 0) {
				// remember length of contents
				contentsLength += charsRead;
			}
		} while (charsRead > 0);
		
		// resize contents if necessary
		if (contentsLength < contents.length) {
			System.arraycopy(contents, 0, contents = new char[contentsLength], 0, contentsLength);
		}
	} finally {
		try {
			reader.close();
		} catch (IOException e) {
		}
	}
	return contents;
}
public static void main(String[] arg){
	System.out.println(bind("test")); //$NON-NLS-1$
}
}