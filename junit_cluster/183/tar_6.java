package org.junit.tests.experimental.parallel;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.Before;

public class ParallelMethodTest {
	private static volatile Thread fOne= null;
	private static volatile Thread fTwo= null;
	
	public static class Example {
		@Test public void one() {
			fOne= Thread.currentThread();
		}
		@Test public void two() {
			fTwo= Thread.currentThread();
		}
	}
	
	@Before public void cleanupReferences() {
		fOne= null;
		fTwo= null;
	}
	
	@Test public void testsRunInParallel() {
		Result result= JUnitCore.runClasses(ParallelComputer.methods(),
				Example.class);
		assertTrue(result.wasSuccessful());
		assertNotNull(fOne);
		assertNotNull(fTwo);
		assertThat(fOne, is(not(fTwo)));
	}
}
