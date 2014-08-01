package com.github.ruediste1.btrbck.dom;

import static org.junit.Assert.assertEquals;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

public class TimeUnitUnitTest {

	DateTime now;

	@Before
	public void setup() {
		now = new DateTime(2014, 3, 3, 3, 3, 3, 3);
	}

	@Test
	public void testSecond() {
		assertEquals(new DateTime(2014, 3, 3, 3, 3, 3, 0),
				TimeUnit.SECOND.truncate(now));
	}

	@Test
	public void testMinute() {
		assertEquals(new DateTime(2014, 3, 3, 3, 3, 0, 0),
				TimeUnit.MINUTE.truncate(now));
	}

	@Test
	public void testHour() {
		assertEquals(new DateTime(2014, 3, 3, 3, 0, 0, 0),
				TimeUnit.HOUR.truncate(now));
	}

	@Test
	public void testDay() {
		assertEquals(new DateTime(2014, 3, 3, 0, 0, 0, 0),
				TimeUnit.DAY.truncate(now));
	}

	@Test
	public void testWeek() {
		assertEquals(new DateTime(2014, 3, 3, 0, 0, 0, 0),
				TimeUnit.WEEK.truncate(now.plusDays(2)));
	}

	@Test
	public void testYear() {
		assertEquals(new DateTime(2014, 1, 1, 0, 0, 0, 0),
				TimeUnit.YEAR.truncate(now));
	}

	@Test
	public void testDecade() {
		assertEquals(new DateTime(2010, 1, 1, 0, 0, 0, 0),
				TimeUnit.DECADE.truncate(now));
	}
}
