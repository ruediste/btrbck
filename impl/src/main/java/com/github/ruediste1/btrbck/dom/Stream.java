package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;
import java.util.UUID;

public class Stream {
	public UUID id;
	public String name;
	public Duration initialRetentionTime;
	public StreamRepository streamRepository;

	public Path getSnapshotRemovalLockFile() {
		return getRootDirectory().resolve("snapshotRemovalLock");
	}

	public Path getSnapshotCreationLockFile() {
		return getRootDirectory().resolve("snapshotCreationLock");
	}

	public Path getRootDirectory() {
		return streamRepository.getBaseDirectory().resolve(name);
	}

}
