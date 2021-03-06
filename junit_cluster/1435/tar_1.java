/**
 * 
 */
package org.junit.internal.runners.links;

import org.junit.internal.runners.model.EachTestNotifier;

public class Notifying extends NotificationStrategy {
	private final Link fNext;

	public Notifying(Link next) {
		fNext= next;
	}

	@Override
	public void run(EachTestNotifier context) {
		context.fireTestStarted();
		try {
			fNext.run();
		} catch (Throwable e) {
			context.addFailure(e);
		} finally {
			context.fireTestFinished();
		}
	}
}