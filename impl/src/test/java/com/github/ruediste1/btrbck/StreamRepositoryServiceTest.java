package com.github.ruediste1.btrbck;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.BackupStreamRepository;
import com.github.ruediste1.btrbck.dom.StreamRepository;
import com.github.ruediste1.btrbck.test.TestBase;

public class StreamRepositoryServiceTest extends TestBase {

	@Inject
	StreamRepositoryService service;

	Path repoPath;

	@Before
	public void setup() {
	}

	@After
	public void tearDown() {

		if (repoPath != null) {
			Util.removeRecursive(repoPath);
			repoPath = null;
		}
	}

	@Test
	public void testCreateBackupRepository() throws IOException {
		testRepoCreate(new BackupStreamRepository());
	}

	@Test
	public void testCreateApplicationRepository() throws IOException {
		testRepoCreate(new ApplicationStreamRepository());
	}

	private Path testRepoCreate(StreamRepository repo) throws IOException {
		repoPath = createTempDirectory();
		repoPath.toFile().deleteOnExit();
		repo.rootDirectory = repoPath;
		service.createRepository(repo);
		assertTrue(Files.exists(repo.getRepositoryXmlFile()));
		return repoPath;
	}

	@Test
	public void testReadRepository() throws Exception {
		Path path = testRepoCreate(new BackupStreamRepository());
		StreamRepository repo = service.readRepository(path);
		assertThat(repo instanceof BackupStreamRepository, is(true));
		assertThat(repo.rootDirectory, equalTo(path));
	}
}
