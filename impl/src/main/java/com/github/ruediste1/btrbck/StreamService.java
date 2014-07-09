package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.nio.file.Files;

import javax.inject.Inject;

import com.github.ruediste1.btrbck.LockManager.Lock;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.StreamRepository;

/**
 * Provides operations on {@link Stream}s
 * 
 */
public class StreamService {

	@Inject
	StreamRepositoryService streamRepositoryService;

	@Inject
	LockManager lockManager;

	public Lock getSnapshotRemovalLock(Stream stream, boolean shared)
			throws IOException {
		return lockManager.getLock(stream.getSnapshotRemovalLockFile(), shared);
	}

	public Lock getSnapshotCreationLock(Stream stream, boolean shared)
			throws IOException {
		return lockManager
				.getLock(stream.getSnapshotCreationLockFile(), shared);
	}

	public Stream getStream(StreamRepository repository, String name) {
		Stream s = new Stream();
		s.name = name;
		s.streamRepository = repository;
		if (!Files.isDirectory(s.getRootDirectory())) {
			return null;
		}

		return s;
	}

	public void createStream(Stream stream) throws IOException {
		Lock lock = streamRepositoryService.getStreamEnumerationLock(
				stream.streamRepository, true);

		if (getStream(stream.streamRepository, stream.name) != null) {
			throw new DisplayException("Stream " + stream.name
					+ " already exists");
		}

		Files.createDirectory(stream.getRootDirectory());
		lock.release();
	}

}
