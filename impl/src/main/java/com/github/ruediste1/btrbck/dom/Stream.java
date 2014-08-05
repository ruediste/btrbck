package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import com.google.common.base.Objects;

/**
 * Represents a stream of snaphots. Streams live in a {@link StreamRepository}.
 */
@XmlRootElement
public class Stream {

	/**
	 * Unique ID of the stream
	 */
	@XmlTransient
	public UUID id;

	/**
	 * Name of the stream
	 */
	@XmlTransient
	public String name;

	/**
	 * Period during which all snapshots are retained
	 */
	@XmlAttribute
	@XmlJavaTypeAdapter(PeriodAdapter.class)
	public Period initialRetentionPeriod;

	/**
	 * {@link StreamRepository} this stream is part of
	 */
	@XmlTransient
	public StreamRepository streamRepository;

	/**
	 * History of {@link Snapshot}s and restore operations which were made in
	 * this stream
	 */
	@XmlTransient
	public VersionHistory versionHistory;

	/**
	 * The interval at which to create automatic snaphots
	 */
	@XmlAttribute
	@XmlJavaTypeAdapter(PeriodAdapter.class)
	public Period snapshotInterval;

	@XmlElementRef
	public final ArrayList<Retention> retentions = new ArrayList<>();

	public Path getStreamConfigFile() {
		return getStreamMetaDirectory().resolve(name + ".xml");
	}

	public Path getStreamUuidFile() {
		return getStreamMetaDirectory().resolve(name + ".id");
	}

	public Path getStreamMetaDirectory() {
		return streamRepository.getBaseDirectory().resolve(name);
	}

	public Path getSnapshotsDir() {
		return getStreamMetaDirectory().resolve("snapshots");
	}

	public Path getReceiveTempDir() {
		return getStreamMetaDirectory().resolve("receiveTmp");
	}

	public Path getVersionHistoryFile() {
		return getStreamMetaDirectory().resolve("versions.xml");
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("id", id).add("name", name)
				.add("repo", streamRepository.rootDirectory.toAbsolutePath())
				.toString();
	}

	public Interval getInitialRetentionInterval(DateTime now) {
		return new Interval(initialRetentionPeriod, now);

	}

}
