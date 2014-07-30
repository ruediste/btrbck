package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement
public abstract class StreamRepository {

	/**
	 * The root directory of this repository. This is the directory containing
	 * the stream working directories for {@link ApplicationStreamRepository}s.
	 * For {@link BackupStreamRepository}s it is the same as
	 * {@link #getBaseDirectory()}.
	 */
	@XmlTransient
	public Path rootDirectory;

	/**
	 * Return the directory containing the repository meta data
	 */
	public abstract Path getBaseDirectory();

	public Path getStreamRoot(Stream stream) {
		return getBaseDirectory().resolve(stream.name);
	}

	public Path getRepositoryLockFile() {
		return getBaseDirectory().resolve("repositoryLock");
	}

	public Path getRepositoryXmlFile() {
		return getBaseDirectory().resolve("repository.xml");
	}

}
