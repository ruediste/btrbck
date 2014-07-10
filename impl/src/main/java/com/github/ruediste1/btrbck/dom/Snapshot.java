package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

public class Snapshot {
	public int nr;
	public DateTime date;
	public Stream stream;

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
