package com.github.ruediste1.btrbck.dom;

import java.util.UUID;

public abstract class VersionHistoryEntry {
	UUID streamId;

	public VersionHistoryEntry(UUID streamId) {
		this.streamId = streamId;
	}

	public abstract int getRepresentedSnapshotCount();
}
