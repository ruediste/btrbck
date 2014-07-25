package com.github.ruediste1.btrbck.dom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

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

	/**
	 * Add a restore entry. If the last entry is already a restore for the same
	 * snapshot, do nothing.
	 */
	public void addRestore(UUID streamId, int restoredSnapshotNr) {
		VersionHistoryEntry lastEntry = getLastEntry();
		if (lastEntry instanceof RestoreVersionHistoryEntry) {
			RestoreVersionHistoryEntry lastRestoreEntry = (RestoreVersionHistoryEntry) lastEntry;
			if (lastRestoreEntry.streamId.equals(streamId)
					&& lastRestoreEntry.restoredSnapshotNr == restoredSnapshotNr) {
				// skip adding an entry
				return;
			}
		}
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

	private static class SnapshotIterator {
		PeekingIterator<VersionHistoryEntry> it;
		int remainingCount;

		public SnapshotIterator(VersionHistory history) {
			it = Iterators.peekingIterator(history.entries.iterator());
			advanceToNextEntry();
		}

		private void advanceToNextEntry() {
			// skip leading non-snapshot entries
			while (it.hasNext()) {
				if (!(it.peek() instanceof SnapshotVersionHistoryEntry)) {
					it.next();
				} else {
					break;
				}
			}

			if (it.hasNext()) {
				remainingCount = peek().count;
			}
		}

		private SnapshotVersionHistoryEntry peek() {
			SnapshotVersionHistoryEntry peek = (SnapshotVersionHistoryEntry) it
					.peek();
			return peek;
		}

		int remainingSameUuid() {
			return remainingCount;
		}

		UUID nextUuid() {
			return peek().streamId;
		}

		boolean hasNext() {
			return it.hasNext();
		}

		void advance(UUID id, int count) {
			if (!id.equals(nextUuid())) {
				throw new RuntimeException("id mismatch");
			}
			int toGo = count;
			if (remainingCount > count) {
				remainingCount -= count;
			} else {
				toGo -= remainingCount;
				it.next();
				advanceToNextEntry();
				if (hasNext() && id.equals(nextUuid())) {
					advance(id, toGo);
				}
			}
		}
	}

	public boolean isAncestorOf(VersionHistory child) {
		SnapshotIterator itParent = new SnapshotIterator(this);
		SnapshotIterator itChild = new SnapshotIterator(child);

		while (itParent.hasNext() && itChild.hasNext()) {
			if (!itParent.nextUuid().equals(itChild.nextUuid())) {
				// different id encountered, no ancestor-child relation
				return false;
			}
			int remaining = Math.min(itParent.remainingSameUuid(),
					itChild.remainingSameUuid());
			UUID id = itParent.nextUuid();
			itParent.advance(id, remaining);
			itChild.advance(id, remaining);
		}

		if (!itParent.hasNext()) {
			// we ate the whole parent, so the histories are equal or a
			// parent-child relationship
			return true;
		} else {
			return false;
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
