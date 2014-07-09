package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;

public abstract class StreamRepository {
	public String name;

	/**
	 * The root directory of this repository. This is the directory containing
	 * the stream working directories for {@link ApplicationStreamRepository}s.
	 * For {@link BackupStreamRepository}s it is the same as
	 * {@link #getBaseDirectory()}.
	 */
	public Path rootDirectory;

	/**
	 * Return the directory containing the repository meta data
	 */
	public abstract Path getBaseDirectory();

	public Path getStreamRoot(Stream stream) {
		return getBaseDirectory().resolve(stream.name);
	}

	public Path getStreamEnumerationLockFile() {

		return getBaseDirectory().resolve(".streamEnumerationLock");
	}

	public Path getRepositoryXmlFile() {
		return getBaseDirectory().resolve("repository.xml");
	}
}
