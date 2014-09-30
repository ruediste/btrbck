package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Represents a snapshot of the working directory of a {@link Stream}.
 */
public class Snapshot {
	public static final Comparator<Snapshot> COMPARATOR_NR = new Comparator<Snapshot>() {

		@Override
		public int compare(Snapshot o1, Snapshot o2) {
			return Integer.compare(o1.nr, o2.nr);
		}
	};

	/**
	 * Number of the snapshot. The next snapshot number is determined by the
	 * length of the {@link VersionHistory} of the {@link Stream}
	 */
	public int nr;

	/**
	 * Instant the snapshot has been taken
	 */
	public DateTime date;

	/**
	 * {@link Stream} the snapshot belongs to
	 */
	public Stream stream;

	public UUID senderStreamId;

	public Snapshot() {}

	public Snapshot(int nr, DateTime date, Stream stream) {
		super();
		this.nr = nr;
		this.date = date;
		this.stream = stream;
	}

	public Path getSnapshotDir() {
		return stream.getSnapshotsDir().resolve(getSnapshotName());
	}

	public String getSnapshotName() {
		return String.format("%d_%s", nr,
				date.toString(ISODateTimeFormat.dateTime()));
	}

	public static Snapshot parse(Stream stream, String name) {
		Snapshot snapshot = new Snapshot();
		snapshot.stream = stream;
		int idx = name.indexOf("_");
		snapshot.date = ISODateTimeFormat.dateTimeParser().withOffsetParsed()
				.parseDateTime(name.substring(idx + 1));
		snapshot.nr = Integer.parseInt(name.substring(0, idx));
		return snapshot;
	}
}
