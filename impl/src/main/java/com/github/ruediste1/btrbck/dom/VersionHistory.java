package com.github.ruediste1.btrbck.dom;

import java.util.ArrayList;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class VersionHistory {
	final ArrayList<VersionHistoryEntry> entries = new ArrayList<>();

	public int getVersionCount() {
		int count = 0;
		for (VersionHistoryEntry entry : entries) {
			count += entry.getRepresentedSnapshotCount();
		}
		return count;
	}

	public SnapshotVersionHistoryEntry getLastSnapshotEntry() {
		for (int i = entries.size() - 1; i >= 0; i--) {
			VersionHistoryEntry entry = entries.get(i);
			if (entry instanceof SnapshotVersionHistoryEntry) {
				return (SnapshotVersionHistoryEntry) entry;
			}
		}
		return null;
	}

	public VersionHistoryEntry getLastEntry() {
		if (entries.isEmpty()) {
			return null;
		}
		return entries.get(entries.size() - 1);
	}

	public void addVersion(UUID streamId) {
		VersionHistoryEntry entry = getLastEntry();
		if (entry == null || (!entry.streamId.equals(streamId))
				|| (!(entry instanceof SnapshotVersionHistoryEntry))) {
			entries.add(new SnapshotVersionHistoryEntry(streamId));
		} else {
			((SnapshotVersionHistoryEntry) entry).count++;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		VersionHistory other = (VersionHistory) obj;
		return entries.equals(other.entries);
	}
}
