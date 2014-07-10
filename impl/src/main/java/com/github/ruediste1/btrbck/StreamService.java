package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.joda.time.DateTime;
import org.joda.time.Period;

import com.github.ruediste1.btrbck.LockManager.Lock;
import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.Snapshot;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.StreamRepository;
import com.github.ruediste1.btrbck.dom.VersionHistory;

/**
 * Provides operations on {@link Stream}s
 * 
 */
@Singleton
public class StreamService {

	@Inject
	BtrfsService btrfsService;

	@Inject
	StreamRepositoryService streamRepositoryService;

	@Inject
	LockManager lockManager;

	@Inject
	JAXBContext ctx;

	public Lock getSnapshotRemovalLock(Stream stream, boolean shared)
			throws IOException {
		return lockManager.getLock(stream.getSnapshotRemovalLockFile(), shared);
	}

	public Lock getSnapshotCreationLock(Stream stream, boolean shared)
			throws IOException {
		return lockManager
				.getLock(stream.getSnapshotCreationLockFile(), shared);
	}

	public Stream readStream(StreamRepository repository, String name) {
		Stream s = new Stream();
		s.name = name;
		s.streamRepository = repository;
		if (!Files.isDirectory(s.getStreamMetaDirectory())) {
			return null;
		}

		Stream readStream;

		// read config file
		try {
			readStream = (Stream) ctx.createUnmarshaller().unmarshal(
					s.getStreamConfigFile().toFile());
		} catch (JAXBException e) {
			throw new RuntimeException(
					"Error while reading stream config file", e);
		}

		if (!Objects.equals(name, readStream.name)) {
			throw new DisplayException("Name " + readStream.name
					+ " does not equal the expected name of " + name);
		}
		readStream.streamRepository = repository;

		// read version history
		try {
			readStream.versionHistory = (VersionHistory) ctx
					.createUnmarshaller().unmarshal(
							s.getVersionHistoryFile().toFile());
		} catch (JAXBException e) {
			throw new RuntimeException("Error while reading version history", e);
		}

		return readStream;
	}

	public void createStream(Stream stream) throws IOException {
		Lock lock = streamRepositoryService.getStreamEnumerationLock(
				stream.streamRepository, false);

		if (readStream(stream.streamRepository, stream.name) != null) {
			throw new DisplayException("Stream " + stream.name
					+ " already exists");
		}

		Files.createDirectory(stream.getStreamMetaDirectory());
		Util.initializeLockFile(stream.getSnapshotCreationLockFile());
		Util.initializeLockFile(stream.getSnapshotRemovalLockFile());
		Files.createDirectory(stream.getSnapshotsDir());

		stream.id = UUID.randomUUID();
		stream.initialRetentionPeriod = Period.days(1);

		if (stream.streamRepository instanceof ApplicationStreamRepository) {
			Path workingDirectory = ((ApplicationStreamRepository) stream.streamRepository)
					.getWorkingDirectory(stream);
			btrfsService.createSubVolume(workingDirectory);
		}

		// write stream config
		try {
			ctx.createMarshaller().marshal(stream,
					stream.getStreamConfigFile().toFile());
		} catch (JAXBException e) {
			throw new RuntimeException("Error while writing stream", e);
		}

		// initialize version history
		stream.versionHistory = new VersionHistory();
		writeVersionHistory(stream);

		lock.release();
	}

	private void writeVersionHistory(Stream stream) {
		try {
			ctx.createMarshaller().marshal(stream.versionHistory,
					stream.getVersionHistoryFile().toFile());
		} catch (JAXBException e) {
			throw new RuntimeException("Error while writing stream", e);
		}
	}

	public Set<String> getStreamNames(StreamRepository repository) {
		return Util.getDirectoryNames(repository.getBaseDirectory());
	}

	public void deleteStream(Stream stream) {
		if (stream.streamRepository instanceof ApplicationStreamRepository) {
			Path workingDirectory = ((ApplicationStreamRepository) stream.streamRepository)
					.getWorkingDirectory(stream);
			btrfsService.deleteSubVolume(workingDirectory);
		}

		for (Snapshot snapshot : getSnapshots(stream)) {
			deleteSnapshot(snapshot);
		}

		Util.removeRecursive(stream.getStreamMetaDirectory());
	}

	public void deleteStreams(StreamRepository repository) {
		Set<String> streamNames = getStreamNames(repository);
		for (String name : streamNames) {
			Stream s = new Stream();
			s.name = name;
			s.streamRepository = repository;
			deleteStream(s);
		}
	}

	public List<Snapshot> getSnapshots(Stream stream) {
		ArrayList<Snapshot> result = new ArrayList<>();
		for (String name : Util.getDirectoryNames(stream.getSnapshotsDir())) {
			result.add(Snapshot.parse(stream, name));
		}
		return result;
	}

	public Snapshot takeSnapshot(Stream stream) {
		Snapshot snapshot = new Snapshot();
		snapshot.stream = stream;
		snapshot.date = new DateTime();
		snapshot.nr = stream.versionHistory.getVersionCount();

		stream.versionHistory.addVersion(stream.id);
		writeVersionHistory(stream);

		btrfsService.createSubVolume(snapshot.getSnapshotDir());
		return snapshot;
	}

	public void deleteSnapshot(Snapshot snapshot) {
		btrfsService.deleteSubVolume(snapshot.getSnapshotDir());
	}
}
