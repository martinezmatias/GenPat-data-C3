/*
 * Copyright (c) 2010 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
*/

package fitlibrary.flow;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import fitlibrary.table.Row;
import fitlibrary.table.RowOnParse;
import fitlibrary.traverse.DomainAdapter;
import fitlibrary.utility.TestResults;

@RunWith(JMock.class)
public class TestSetUpTearDown {
	Mockery context = new Mockery();
	SetUpTearDown setUpTearDown = new SetUpTearDown();
	TestInterface object = context.mock(TestInterface.class,"object");
	TestInterface sut = context.mock(TestInterface.class,"sut");
	TestResults testResults = new TestResults();
	Row table = new RowOnParse("1");
	
	@Test
	public void suiteSetUp() {
		context.checking(new Expectations() {{
			oneOf(object).suiteSetUp();
		}});
		setUpTearDown.callSuiteSetUp(object, table, testResults);
		assertThat(testResults.problems(),is(false));
	}
	@Test
	public void suiteSetUpWithException() {
		context.checking(new Expectations() {{
			oneOf(object).suiteSetUp(); will(throwException(new RuntimeException("error")));
		}});
		setUpTearDown.callSuiteSetUp(object, table, testResults);
		assertThat(testResults.problems(),is(true));
	}
	@Test
	public void suiteTearDown() {
		context.checking(new Expectations() {{
			oneOf(object).suiteTearDown();
		}});
		setUpTearDown.callSuiteTearDown(object,testResults);
		assertThat(testResults.problems(),is(false));
	}
	@Test
	public void suiteTearDownWithException() {
		context.checking(new Expectations() {{
			oneOf(object).suiteTearDown(); will(throwException(new RuntimeException("error")));
		}});
		setUpTearDown.callSuiteTearDown(object,testResults);
		assertThat(testResults.problems(),is(true));
	}
	@Test
	public void setUp() {
		context.checking(new Expectations() {{
			allowing(object).getSystemUnderTest(); will(returnValue(null));
			oneOf(object).setUp();
		}});
		setUpTearDown.callSetUpSutChain(object, table, testResults);
		assertThat(testResults.problems(),is(false));
	}
	@Test
	public void setUpWithSut() {
		context.checking(new Expectations() {{
			allowing(object).getSystemUnderTest(); will(returnValue(sut));
			oneOf(object).setUp();
			allowing(sut).getSystemUnderTest(); will(returnValue(null));
			oneOf(sut).setUp();
		}});
		setUpTearDown.callSetUpSutChain(object, table, testResults);
		assertThat(testResults.problems(),is(false));
	}
	@Test
	public void setUpWithException() {
		context.checking(new Expectations() {{
			allowing(object).getSystemUnderTest(); will(returnValue(sut));
			oneOf(object).setUp(); will(throwException(new RuntimeException("error")));
			allowing(sut).getSystemUnderTest(); will(returnValue(null));
			oneOf(sut).setUp();
		}});
		setUpTearDown.callSetUpSutChain(object, table, testResults);
		assertThat(testResults.problems(),is(true));
	}
	@Test
	public void setUpTearDown() {
		context.checking(new Expectations() {{
			allowing(object).getSystemUnderTest(); will(returnValue(null));
			oneOf(object).setUp();
			oneOf(object).tearDown();
		}});
		setUpTearDown.callSetUpSutChain(object, table, testResults);
		setUpTearDown.callTearDownSutChain(object, table, testResults);
		assertThat(testResults.problems(),is(false));
	}
	@Test
	public void setUpTearDownWithSut() {
		context.checking(new Expectations() {{
			allowing(object).getSystemUnderTest(); will(returnValue(sut));
			oneOf(object).setUp();
			allowing(sut).getSystemUnderTest(); will(returnValue(null));
			oneOf(sut).setUp();
			oneOf(object).tearDown();
			oneOf(sut).tearDown();
		}});
		setUpTearDown.callSetUpSutChain(object, table, testResults);
		setUpTearDown.callTearDownSutChain(object, table, testResults);
		assertThat(testResults.problems(),is(false));
	}
	@Test
	public void setUpTearDownWithException() {
		context.checking(new Expectations() {{
			allowing(object).getSystemUnderTest(); will(returnValue(null));
			oneOf(object).setUp();
			oneOf(object).tearDown(); will(throwException(new RuntimeException("error")));
		}});
		setUpTearDown.callSetUpSutChain(object, table, testResults);
		setUpTearDown.callTearDownSutChain(object, table, testResults);
		assertThat(testResults.problems(),is(true));
	}

	interface TestInterface extends DomainAdapter {
		void suiteSetUp();
		void suiteTearDown();
		void setUp();
		void tearDown();
	}
}
