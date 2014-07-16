package com.github.ruediste1.btrbck;

import java.util.List;

import javax.inject.Inject;

import com.github.ruediste1.btrbck.dom.Snapshot;
import com.github.ruediste1.btrbck.dom.SnapshotHandle;
import com.github.ruediste1.btrbck.dom.StreamHandle;
import com.github.ruediste1.btrbck.dom.StreamRepositoryHandle;

/**
 * Provides high level backup operations
 */
public class BackupService {
	@Inject
	SyncService syncService;

	@Inject
	StreamService streamService;

	@Inject
	BtrfsService btrfsService;

	@Inject
	BlockTransferService blockTransferService;

	public void createStream(StreamHandle streamHandle) {

	}

	public void deleteStream(StreamHandle streamHandle) {

	}

	public void renameStream(StreamHandle streamHandle) {

	}

	public void snapshot(StreamHandle streamHandle) {

	}

	/**
	 * 
	 */
	public void sendSnapshots() {

	}

	public void pruneSnapshots(StreamHandle streamHandle) {

	}

	public void restoreWorkingDirectory(SnapshotHandle snapshotHandle) {

	}

	public List<String> readStreamNames(StreamRepositoryHandle repositoryHandle) {
		return null;
	}

	public List<Snapshot> readSnapshots(StreamRepositoryHandle repositoryHandle) {
		return null;
	}
}
