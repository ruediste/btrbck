package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.inject.Inject;

import com.github.ruediste1.btrbck.LockManager.Lock;
import com.github.ruediste1.btrbck.dom.StreamRepository;

/**
 * Provides operations on {@link StreamRepository}s
 */
public class StreamRepositoryService {

	@Inject
	LockManager lockManager;

	public Lock getStreamEnumerationLock(StreamRepository repository,
			boolean shared) throws IOException {
		return lockManager.getLock(repository.getStreamEnumerationLockFile(),
				shared);
	}

	public void initializeRepository(StreamRepository repository)
			throws IOException {
		// create directories
		Files.createDirectories(repository.rootDirectory);
		Files.createDirectories(repository.getBaseDirectory());

		// create lock files
		Util.initializeLockFile(repository.getStreamEnumerationLockFile());

		// create repository.xml
		InputStream in = getClass().getClassLoader().getResourceAsStream(
				"repository.template.xml");
		Files.copy(in, repository.getRepositoryXmlFile());
	}

}
