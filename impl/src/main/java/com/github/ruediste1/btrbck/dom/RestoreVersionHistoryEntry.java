package com.github.ruediste1.btrbck.dom;

import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Entry in a {@link VersionHistory} representing that a certain snapshot has
 * been restored. This is used to find optimal clone sources when transmitting
 * incremental snapshots.
 */
@XmlRootElement(name = "restore")
public class RestoreVersionHistoryEntry extends VersionHistoryEntry {
	private static final long serialVersionUID = 1L;

	@XmlAttribute
	public int restoredSnapshotNr;

	public RestoreVersionHistoryEntry() {

	}

	public RestoreVersionHistoryEntry(UUID streamId) {
		super(streamId);
	}

	@Override
	public int getRepresentedSnapshotCount() {
		return 0;
	}

	@Override
	public String toString() {
		return "(Restore: " + streamId + " restored: " + restoredSnapshotNr
				+ ")";
	}
}
