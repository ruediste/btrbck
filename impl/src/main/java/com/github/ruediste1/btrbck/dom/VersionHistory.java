package com.github.ruediste1.btrbck.dom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class VersionHistory implements Serializable {
	private static final long serialVersionUID = 1L;

	public static class HistoryNode implements Comparable<HistoryNode> {
		public int snapshotNr;
		public final Set<HistoryNode> parents = new HashSet<>();
		public final Set<Integer> parentNrs = new HashSet<>();

		@Override
		public int compareTo(HistoryNode o) {
			return Integer.compare(snapshotNr, o.snapshotNr);
		}

	}

	final ArrayList<VersionHistoryEntry> entries = new ArrayList<>();

	public TreeMap<Integer, HistoryNode> calculateNodes() {
		TreeMap<Integer, HistoryNode> result = new TreeMap<>();

		// construct nodes
		HashSet<Integer> parents = new HashSet<>();
		int snapshotNr = 0;
		for (VersionHistoryEntry entry : entries) {
			if (entry instanceof SnapshotVersionHistoryEntry) {
				int count = ((SnapshotVersionHistoryEntry) entry).count;
				for (int i = 0; i < count; i++) {
					HistoryNode node = new HistoryNode();
					node.snapshotNr = snapshotNr++;
					if (!parents.isEmpty()) {
						node.parentNrs.addAll(parents);
						parents.clear();
					}
					result.put(node.snapshotNr, node);
					parents.add(node.snapshotNr);
				}
			} else if (entry instanceof RestoreVersionHistoryEntry) {
				parents.add(((RestoreVersionHistoryEntry) entry).restoredSnapshotNr);
			} else {
				throw new RuntimeException("Should not happen");
			}
		}

		// resolve parent nodes
		for (HistoryNode node : result.values()) {
			for (Integer nr : node.parentNrs) {
				node.parents.add(result.get(nr));
			}
		}
		return result;
	}

	public int getVersionCount() {
		int count = 0;
		for (VersionHistoryEntry entry : entries) {
			count += entry.getRepresentedSnapshotCount();
		}
		return count;
	}

	public VersionHistoryEntry getLastEntry() {
		if (entries.isEmpty()) {
			return null;
		}
		return entries.get(entries.size() - 1);
	}

	public void addRestore(UUID streamId, int restoredSnapshotNr) {
		RestoreVersionHistoryEntry entry = new RestoreVersionHistoryEntry(
				streamId);
		entry.restoredSnapshotNr = restoredSnapshotNr;
		entries.add(entry);
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
