package com.github.ruediste1.btrbck.dom;

import java.io.Serializable;
import java.util.UUID;

public abstract class VersionHistoryEntry implements Serializable {
	private static final long serialVersionUID = 1L;

	UUID streamId;

	public VersionHistoryEntry(UUID streamId) {
		this.streamId = streamId;
	}

	public abstract int getRepresentedSnapshotCount();
}
