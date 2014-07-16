package com.github.ruediste1.btrbck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.github.ruediste1.btrbck.dom.Snapshot;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.VersionHistory;
import com.github.ruediste1.btrbck.dom.VersionHistory.HistoryNode;
import com.github.ruediste1.btrbck.dto.StreamState;

@Singleton
public class SyncService {

	@Inject
	StreamService streamService;

	public static class SendFileSpec {
		List<Snapshot> cloneSources = new ArrayList<>();
		Snapshot target;
	}

	/**
	 * Create a {@link StreamState} for a stream
	 */
	public StreamState calculateStreamState(Stream stream) {
		StreamState result = new StreamState();
		result.versionHistory = stream.versionHistory;
		result.availableSnapshotNumbers.addAll(streamService.getSnapshots(
				stream).keySet());
		return result;
	}

	/**
	 * Determine the files (and thus snaphots) to be sent in order to
	 * synchronize a stream in the stateOfTarget to the sourceStream.
	 */
	public List<SendFileSpec> determineSendFiles(Stream sourceStream,
			StreamState stateOfTarget) {
		TreeMap<Integer, Snapshot> sourceSnapshots = streamService
				.getSnapshots(sourceStream);
		VersionHistory versionHistory = sourceStream.versionHistory;

		return determineSendFiles(sourceSnapshots, versionHistory,
				stateOfTarget.availableSnapshotNumbers);
	}

	List<SendFileSpec> determineSendFiles(
			TreeMap<Integer, Snapshot> sourceSnapshots,
			VersionHistory versionHistory, Set<Integer> targetSnapshotNrs) {
		// calculate the snapshots which are present in the source but missing
		// on the target
		List<Snapshot> missingSnapshots = calculateMissingSnapshots(
				sourceSnapshots.values(), targetSnapshotNrs);

		TreeSet<Integer> availableCloneSources = new TreeSet<>();
		for (int nr : targetSnapshotNrs) {
			if (sourceSnapshots.containsKey(nr)) {
				availableCloneSources.add(nr);
			}
		}

		// calculate the SendFileSpecs for the snapshots missing on the target
		ArrayList<SendFileSpec> result = new ArrayList<>();
		for (Snapshot snapshot : missingSnapshots) {
			SendFileSpec spec = new SendFileSpec();
			spec.target = snapshot;

			// consider the next snapshot as clone source
			{
				Integer nextNr = calculateNextSnapshotNr(snapshot,
						availableCloneSources);
				if (nextNr != null) {
					spec.cloneSources.add(sourceSnapshots.get(nextNr));
				}
			}

			// consider ancestors
			{
				Set<Integer> ancestors = calculateAncestorNrs(snapshot,
						versionHistory, availableCloneSources);
				for (Integer ancestorNr : ancestors) {
					spec.cloneSources.add(sourceSnapshots.get(ancestorNr));
				}
			}
			result.add(spec);
			availableCloneSources.add(snapshot.nr);
		}

		return result;
	}

	/**
	 * Determines the ancestors of the given snapshot which are available on the
	 * target
	 */
	Set<Integer> calculateAncestorNrs(Snapshot snapshot,
			VersionHistory versionHistory,
			TreeSet<Integer> availableCloneSources) {
		Set<Integer> result = new HashSet<>();
		TreeMap<Integer, HistoryNode> nodes = versionHistory.calculateNodes();
		HistoryNode node = nodes.get(snapshot.nr);

		fillAvailableAncestors(node, result, availableCloneSources);
		return result;
	}

	private void fillAvailableAncestors(HistoryNode node, Set<Integer> result,
			TreeSet<Integer> availableCloneSources) {
		for (HistoryNode parent : node.parents) {
			if (availableCloneSources.contains(parent.snapshotNr)) {
				result.add(parent.snapshotNr);
			} else {
				fillAvailableAncestors(parent, result, availableCloneSources);
			}
		}
	}

	/**
	 * Calculate the next available snapshot after the given snapshot, or null
	 * if none is available
	 */
	Integer calculateNextSnapshotNr(Snapshot snapshot,
			TreeSet<Integer> availableCloneSources) {
		return availableCloneSources.higher(snapshot.nr);
	}

	List<Snapshot> calculateMissingSnapshots(
			Collection<Snapshot> sourceSnapshots, Set<Integer> targetSnapshotNrs) {
		List<Snapshot> result = new ArrayList<>();
		for (Snapshot s : sourceSnapshots) {
			if (!targetSnapshotNrs.contains(s.nr)) {
				result.add(s);
			}
		}
		return result;
	}
}
