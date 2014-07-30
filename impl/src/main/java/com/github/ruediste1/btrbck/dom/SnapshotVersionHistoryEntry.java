package com.github.ruediste1.btrbck.dom;

import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;

@XmlRootElement(name = "snapshot")
public class SnapshotVersionHistoryEntry extends VersionHistoryEntry {
	private static final long serialVersionUID = 1L;

	@XmlAttribute
	public int count = 1;

	public SnapshotVersionHistoryEntry() {

	}

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
		return streamId.equals(other.streamId) && count == other.count;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(streamId, count);
	}

	@Override
	public String toString() {
		return "(Snapshot: " + streamId + " count: " + count + ")";
	}
}
