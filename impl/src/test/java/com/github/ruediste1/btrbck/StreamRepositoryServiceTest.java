package com.github.ruediste1.btrbck;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;

import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.BackupStreamRepository;
import com.github.ruediste1.btrbck.dom.StreamRepository;

public class StreamRepositoryServiceTest {

	private StreamRepositoryService service;

	@Before
	public void setup() {
		service = new StreamRepositoryService();
	}

	@Test
	public void testInitializeBackupRepository() throws IOException {
		testRepoInit(new BackupStreamRepository(), "backupTestRepo");
	}

	@Test
	public void testInitializeApplicationRepository() throws IOException {
		testRepoInit(new ApplicationStreamRepository(), "applicationTestRepo");
	}

	private void testRepoInit(StreamRepository repo, String name)
			throws IOException {
		Path temp = Files.createTempDirectory("btrbck");
		temp.toFile().deleteOnExit();
		repo.name = name;
		repo.rootDirectory = temp;
		service.initializeRepository(repo);
		assertTrue(Files.exists(repo.getRepositoryXmlFile()));
	}
}
