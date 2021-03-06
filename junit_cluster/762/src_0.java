package org.junit.internal.matchers;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

/**
 * A matcher that delegates to throwableMatcher and in addition appends the
 * stacktrace of the actual item in case of a mismatch.
 */
public class StacktracePrintingMatcher<T extends Throwable> extends
		org.hamcrest.TypeSafeMatcher<T> {

	private final Matcher<T> fThrowableMatcher;

	private StacktracePrintingMatcher(Matcher<T> throwableMatcher) {
		fThrowableMatcher= throwableMatcher;
	}

	public void describeTo(Description description) {
		fThrowableMatcher.describeTo(description);
	}

	@Override
	protected boolean matchesSafely(T item) {
		return fThrowableMatcher.matches(item);
	}

	@Override
	protected void describeMismatchSafely(T item, Description description) {
		fThrowableMatcher.describeMismatch(item, description);
		description.appendText("\nStacktrace was: ");
		description.appendText(readStacktrace(item));
	}

	private String readStacktrace(Throwable throwable) {
		StringWriter stringWriter= new StringWriter();
		throwable.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.toString();
	}

	@Factory
	public static <T extends Throwable> Matcher<T> withStacktrace(
			Matcher<T> throwableMatcher) {
		return new StacktracePrintingMatcher<T>(throwableMatcher);
	}
}
