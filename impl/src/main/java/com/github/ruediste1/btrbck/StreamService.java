package com.github.ruediste1.btrbck;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.chrono.ISOChronology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.Retention;
import com.github.ruediste1.btrbck.dom.Snapshot;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.StreamRepository;
import com.github.ruediste1.btrbck.dom.TimeUnit;
import com.github.ruediste1.btrbck.dom.VersionHistory;

/**
 * Provides operations on {@link Stream}s
 *
 */
@Singleton
public class StreamService {

	Logger log = LoggerFactory.getLogger(StreamService.class);

	@Inject
	BtrfsService btrfsService;

	@Inject
	StreamRepositoryService streamRepositoryService;

	@Inject
	JAXBContext ctx;

	public Stream readStream(StreamRepository repository, String name) {
		Stream result = tryReadStream(repository, name);
		if (result == null) {
			throw new DisplayException("Cannot read stream " + name
					+ " in repository "
					+ repository.rootDirectory.toAbsolutePath());
		}
		return result;
	}

	public Stream tryReadStream(StreamRepository repository, String name) {
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

		readStream.name = name;
		readStream.streamRepository = repository;

		// read id
		try {
			readStream.id = UUID.fromString(new String(Files
					.readAllBytes(readStream.getStreamUuidFile()), "UTF-8"));
		} catch (IOException e) {
			throw new RuntimeException("Error while reading stream id file", e);

		}
		// read version history
		try {
			File historyFile = s.getVersionHistoryFile().toFile();
			log.debug("reading version history from " + historyFile);
			readStream.versionHistory = (VersionHistory) ctx
					.createUnmarshaller().unmarshal(historyFile);
		} catch (JAXBException e) {
			throw new RuntimeException("Error while reading version history", e);
		}

		log.debug("read stream " + readStream + ", versionHistory: "
				+ readStream.versionHistory);
		return readStream;
	}

	public Stream createStream(StreamRepository streamRepository, String name)
			throws IOException {

		if (tryReadStream(streamRepository, name) != null) {
			throw new DisplayException("Stream " + name + " already exists");
		}

		Stream stream = new Stream();
		stream.streamRepository = streamRepository;
		stream.name = name;

		// temp setup
		{
			stream.initialRetentionPeriod = Period.days(1);

			Retention retention = new Retention();
			retention.period = Period.weeks(1);
			retention.timeUnit = TimeUnit.DAY;
			retention.snapshotsPerTimeUnit = 1;
			stream.retentions.add(retention);
		}

		Files.createDirectory(stream.getStreamMetaDirectory());
		Files.createDirectory(stream.getSnapshotsDir());
		Files.createDirectory(stream.getReceiveTempDir());

		stream.id = UUID.randomUUID();
		Files.write(stream.getStreamUuidFile(),
				stream.id.toString().getBytes("UTF-8"));

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

		return stream;
	}

	public void writeVersionHistory(Stream stream) {
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

	public void deleteStream(Path repoLocation, String name) {
		deleteStream(readStream(
				streamRepositoryService.readRepository(repoLocation), name));
	}

	public void deleteStream(StreamRepository repo, String name) {
		deleteStream(readStream(repo, name));
	}

	public void deleteStream(Stream stream) {
		if (stream.streamRepository instanceof ApplicationStreamRepository) {
			Path workingDirectory = ((ApplicationStreamRepository) stream.streamRepository)
					.getWorkingDirectory(stream);
			btrfsService.deleteSubVolume(workingDirectory);
		}

		for (Snapshot snapshot : getSnapshots(stream).values()) {
			deleteSnapshot(snapshot);
		}

		clearReceiveTempDir(stream);

		Util.removeRecursive(stream.getStreamMetaDirectory(), true);
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

	/**
	 * Return the snapshots in the stream, sorted by their number
	 */
	public TreeMap<Integer, Snapshot> getSnapshots(Stream stream) {
		TreeMap<Integer, Snapshot> result = new TreeMap<>();
		for (String name : Util.getDirectoryNames(stream.getSnapshotsDir())) {
			Snapshot snapshot = Snapshot.parse(stream, name);
			result.put(snapshot.nr, snapshot);
		}
		return result;
	}

	public Snapshot takeSnapshot(Stream stream) {
		if (!(stream.streamRepository instanceof ApplicationStreamRepository)) {
			throw new DisplayException(
					"Cannot take a snapshot of the working directory of stream "
							+ stream.name
							+ " in non-application stream repository "
							+ stream.streamRepository.rootDirectory
									.toAbsolutePath());
		}
		ApplicationStreamRepository repo = (ApplicationStreamRepository) stream.streamRepository;

		Snapshot snapshot = new Snapshot();
		snapshot.stream = stream;
		snapshot.date = new DateTime();
		snapshot.nr = stream.versionHistory.getVersionCount();

		stream.versionHistory.addVersion(stream.id);
		writeVersionHistory(stream);

		btrfsService.takeSnapshot(repo.getWorkingDirectory(stream),
				snapshot.getSnapshotDir(), true);
		return snapshot;
	}

	public void restoreLatestSnapshot(Stream stream) {
		TreeMap<Integer, Snapshot> snapshots = getSnapshots(stream);
		if (snapshots.isEmpty()) {
			throw new DisplayException(
					"Cannot restore latest snapshot. Stream " + stream.name
							+ " does not contain any snapshots");
		}
		restoreSnapshot(stream, Collections.max(snapshots.keySet()));
	}

	public void restoreSnapshot(Stream stream, int snapshotNr) {
		TreeMap<Integer, Snapshot> snapshots = getSnapshots(stream);
		Snapshot snapshot = snapshots.get(snapshotNr);
		if (snapshot == null) {
			throw new DisplayException("Cannot restore snapshot. Stream "
					+ stream.name + " does not contain snapshot number "
					+ snapshotNr);
		}
		restoreSnapshot(snapshot);
	}

	/**
	 * Restore a snapshot.
	 *
	 * The following list outlines the steps taken:
	 * <ol>
	 * <li>delete working directory</li>
	 * <li>update version file</li>
	 * <li>restore working directory</li>
	 * </ol>
	 *
	 * If the process is aborted at any stage (power loss), the command can
	 * simply be executed again.
	 */
	public void restoreSnapshot(Snapshot snapshot) {
		Stream stream = snapshot.stream;
		ApplicationStreamRepository repo = (ApplicationStreamRepository) stream.streamRepository;

		btrfsService.deleteSubVolume(repo.getWorkingDirectory(stream));
		stream.versionHistory.addRestore(stream.id, snapshot.nr);
		writeVersionHistory(stream);
		btrfsService.takeSnapshot(snapshot.getSnapshotDir(),
				repo.getWorkingDirectory(stream), false);
	}

	public void deleteSnapshot(Snapshot snapshot) {
		btrfsService.deleteSubVolume(snapshot.getSnapshotDir());
	}

	public void clearReceiveTempDir(Stream stream) {
		for (String name : Util.getDirectoryNames(stream.getReceiveTempDir())) {
			btrfsService.deleteSubVolume(stream.getReceiveTempDir().resolve(
					name));
		}
	}

	/**
	 * Determine if a new snapshot is required, given the current time
	 */
	public void takeSnapshotIfRequired(Stream stream, Instant now) {
		if (isSnapshotRequired(now, stream.snapshotInterval,
				getSnapshots(stream).values())) {
			takeSnapshot(stream);
		}
	}

	boolean isSnapshotRequired(Instant now, Period snapshotInterval,
			Collection<Snapshot> snapshots) {
		if (snapshotInterval == null) {
			return false;
		}
		DateTime latest = null;
		for (Snapshot s : snapshots) {
			if (latest == null || latest.isBefore(s.date)) {
				latest = s.date;
			}
		}

		log.debug("Latest snapshot date: " + latest + " interval: "
				+ snapshotInterval + " now: " + now);

		boolean snapshotRequired = true;
		if (latest != null) {
			if (latest.plus(snapshotInterval).isAfter(now)) {
				snapshotRequired = false;
			}
		}
		return snapshotRequired;
	}

	public void pruneSnapshots(Stream stream) {
		DateTime now = new DateTime(ISOChronology.getInstanceUTC());
		TreeMap<DateTime, Boolean> keepSnapshot = new TreeMap<>();
		HashMap<DateTime, Snapshot> snapshotMap = new HashMap<>();
		Interval initialRetentionInterval = stream
				.getInitialRetentionInterval(now);

		// fill maps
		for (Snapshot s : getSnapshots(stream).values()) {
			keepSnapshot.put(s.date, s.date.isAfter(now)
					|| initialRetentionInterval.contains(s.date));
			snapshotMap.put(s.date, s);
		}

		// process retentions
		for (Retention r : stream.retentions) {
			for (DateTime time : r.retentionTimes(now)) {
				DateTime key = keepSnapshot.ceilingKey(time);
				if (key != null) {
					keepSnapshot.put(key, true);
				}
			}
		}

		// delete streams which are not to be retained
		for (Entry<DateTime, Boolean> entry : keepSnapshot.entrySet()) {
			if (!entry.getValue()) {
				deleteSnapshot(snapshotMap.get(entry.getKey()));
			}
		}
	}
}
