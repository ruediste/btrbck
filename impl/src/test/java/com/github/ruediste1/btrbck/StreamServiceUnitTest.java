package com.github.ruediste1.btrbck;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import com.github.ruediste1.btrbck.dom.Snapshot;

public class StreamServiceUnitTest {

    @SuppressWarnings("deprecation")
    @Test
    public void testIsSnapshotRequired() throws Exception {
        StreamService service = new StreamService();
        Period period = Period.days(2);
        Collection<Snapshot> snapshots = new ArrayList<>();
        snapshots.add(new Snapshot(0, new DateTime(2014, 1, 1, 0, 0), null));
        snapshots.add(new Snapshot(0, new DateTime(2014, 1, 2, 0, 0), null));
        snapshots.add(new Snapshot(0, new DateTime(2014, 1, 3, 0, 0), null));

        assertThat(service.isSnapshotRequired(
                new DateMidnight(2014, 1, 3).toInstant(), period, snapshots),
                is(false));
        assertThat(service.isSnapshotRequired(
                new DateMidnight(2014, 1, 4).toInstant(), period, snapshots),
                is(false));
        assertThat(service.isSnapshotRequired(
                new DateMidnight(2014, 1, 5).toInstant(), period, snapshots),
                is(true));
    }
}
