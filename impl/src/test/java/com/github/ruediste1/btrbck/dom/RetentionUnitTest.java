package com.github.ruediste1.btrbck.dom;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

public class RetentionUnitTest {

	@Test
	public void testRetentionTimes() throws Exception {
		Retention retention = new Retention();
		retention.period = Period.weeks(2);
		retention.snapshotsPerTimeUnit = 2;
		retention.timeUnit = TimeUnit.WEEK;

		Set<DateTime> times = retention.retentionTimes(new DateTime(2014, 3, 1,
				0, 0, 0));
		assertEquals(4, times.size());
		assertThat(
				times,
				containsInAnyOrder(new DateTime(2014, 2, 17, 0, 0, 0),
						new DateTime(2014, 2, 20, 12, 0, 0), new DateTime(2014,
								2, 24, 0, 0, 0), new DateTime(2014, 2, 27, 12,
								0, 0)));
	}

}
