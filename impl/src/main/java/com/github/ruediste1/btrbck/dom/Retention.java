package com.github.ruediste1.btrbck.dom;

import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

/**
 * Describes a set of {@link Snapshot}s that should be retained.
 * 
 * <p>
 * The {@link #period} defines how far back from now the snapshots are to be
 * retained. During this time, {@link #snapshotsPerTimeUnit} snapshots should be
 * retained per {@link #timeUnit}. The snapshots should be evenly distributed
 * within the time unit.
 * </p>
 * 
 * <p>
 * The {@link Retention} defines a set of instants for which snapshots should
 * ideally be retained. Since there is usually no snapshot at this precise
 * instant, the next snapshot is taken.
 * </p>
 */
public class Retention {
	/**
	 * Defines how far back snapshots should be retained
	 */
	public Period period;

	/**
	 * {@link #snapshotsPerTimeUnit} {@link Snapshot}s per {@link #timeUnit}
	 * should be retained.
	 */
	public TimeUnit timeUnit;

	/**
	 * {@link #snapshotsPerTimeUnit} {@link Snapshot}s per {@link #timeUnit}
	 * should be retained.
	 */
	public int snapshotsPerTimeUnit;

	public Set<DateTime> retentionTimes(DateTime now) {
		HashSet<DateTime> result = new HashSet<>();
		Interval interval = new Interval(now.minus(period), now);
		DateTime current = timeUnit.truncate(interval.getStart());
		while (current.isBefore(now)) {
			DateTime endOfUnit = current.plus(timeUnit.getPeriod());
			long step = new Interval(current, endOfUnit).toDurationMillis()
					/ snapshotsPerTimeUnit;
			for (int i = 0; i < snapshotsPerTimeUnit; i++) {
				DateTime retentionTime = current.plus(i * step);
				if (interval.contains(retentionTime)) {
					result.add(retentionTime);
				}
			}
			current = endOfUnit;
		}
		return result;
	}
}
