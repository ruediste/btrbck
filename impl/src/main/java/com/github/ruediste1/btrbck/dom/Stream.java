package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.Period;

@XmlRootElement
public class Stream {

	@XmlAttribute
	public UUID id;
	@XmlAttribute
	public String name;

	@XmlAttribute
	@XmlJavaTypeAdapter(PeriodAdapter.class)
	public Period initialRetentionPeriod;

	@XmlTransient
	public StreamRepository streamRepository;

	@XmlTransient
	public VersionHistory versionHistory;

	public Path getStreamConfigFile() {
		return getStreamMetaDirectory().resolve(name + ".xml");
	}

	public Path getSnapshotRemovalLockFile() {
		return getStreamMetaDirectory().resolve("snapshotRemovalLock");
	}

	public Path getSnapshotCreationLockFile() {
		return getStreamMetaDirectory().resolve("snapshotCreationLock");
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

}
