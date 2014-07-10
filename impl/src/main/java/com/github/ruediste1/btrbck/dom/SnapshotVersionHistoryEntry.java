package com.github.ruediste1.btrbck.dom;

import java.util.UUID;

public class SnapshotVersionHistoryEntry extends VersionHistoryEntry {
	int count = 1;

	public SnapshotVersionHistoryEntry(UUID streamId) {
		super(streamId);
	}

	@Override
	public int getRepresentedSnapshotCount() {
		return count;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		SnapshotVersionHistoryEntry other = (SnapshotVersionHistoryEntry) obj;
		return count == other.count;
	}
}
