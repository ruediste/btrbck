package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.github.ruediste1.btrbck.LockManager.Lock;
import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.BackupStreamRepository;
import com.github.ruediste1.btrbck.dom.StreamRepository;

/**
 * Provides operations on {@link StreamRepository}s
 */
@Singleton
public class StreamRepositoryService {

	@Inject
	LockManager lockManager;

	@Inject
	JAXBContext ctx;

	public Lock getStreamEnumerationLock(StreamRepository repository,
			boolean shared) throws IOException {
		return lockManager.getLock(repository.getStreamEnumerationLockFile(),
				shared);
	}

	@SuppressWarnings("unchecked")
	public <T extends StreamRepository> T createRepository(Class<T> clazz,
			Path location) throws IOException {
		T repository;
		if (clazz.equals(ApplicationStreamRepository.class)) {
			repository = (T) new ApplicationStreamRepository();
		} else if (clazz.equals(BackupStreamRepository.class)) {
			repository = (T) new BackupStreamRepository();
		} else {
			throw new RuntimeException("unknown repository type " + clazz);
		}
		repository.rootDirectory = location;

		// create directories
		Files.createDirectories(repository.rootDirectory);
		Files.createDirectories(repository.getBaseDirectory());

		// create lock files
		Util.initializeLockFile(repository.getStreamEnumerationLockFile());

		// create repository.xml
		try {
			ctx.createMarshaller().marshal(repository,
					repository.getRepositoryXmlFile().toFile());
		} catch (JAXBException e) {
			throw new RuntimeException(
					"Error while writing repository configuration", e);
		}
		// IputStream in = getClass().getClassLoader().getResourceAsStream(
		// "repository.template.xml");
		// Files.copy(in, repository.getRepositoryXmlFile());
		return repository;
	}

	/**
	 * Delete this empty repository
	 */
	public void deleteEmptyRepository(StreamRepository repository) {
		Util.removeRecursive(repository.rootDirectory);
	}

	public StreamRepository readRepository(Path rootDir) {
		try {
			{
				StreamRepository repo = new ApplicationStreamRepository();
				repo.rootDirectory = rootDir;
				if (Files.exists(repo.getRepositoryXmlFile())) {
					repo = (StreamRepository) ctx.createUnmarshaller()
							.unmarshal(repo.getRepositoryXmlFile().toFile());
					repo.rootDirectory = rootDir;
					return repo;
				}
			}
			{
				StreamRepository repo = new BackupStreamRepository();
				repo.rootDirectory = rootDir;
				if (Files.exists(repo.getRepositoryXmlFile())) {
					repo = (StreamRepository) ctx.createUnmarshaller()
							.unmarshal(repo.getRepositoryXmlFile().toFile());
					repo.rootDirectory = rootDir;
					return repo;
				}
			}
		} catch (JAXBException e) {
			throw new RuntimeException("Error while reading repository from "
					+ rootDir.toAbsolutePath(), e);

		}

		throw new DisplayException("No Repository found at "
				+ rootDir.toAbsolutePath());

	}
}
