package com.github.ruediste1.btrbck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.Snapshot;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.test.TestBase;

public class StreamServiceTest extends TestBase {

	@Inject
	StreamService service;

	@Inject
	StreamRepositoryService repositoryService;

	ApplicationStreamRepository repository;

	@Before
	public void setUp() throws IOException {
		repository = repositoryService.createRepository(
				ApplicationStreamRepository.class, createTempDirectory());
	}

	@After
	public void tearDown() {
		service.deleteStreams(repository);
		repositoryService.deleteEmptyRepository(repository);
	}

	@Test
	public void testCreateAndReadStream() throws IOException {
		assertNull(service.readStream(repository, "test"));
		Stream stream = createStream("test");
		assertNotNull(stream.id);
		assertNotNull(stream.initialRetentionPeriod);
		assertNotNull(stream.versionHistory);
		assertTrue(Files.exists(stream.getSnapshotCreationLockFile()));
		assertTrue(Files.exists(stream.getSnapshotRemovalLockFile()));
		assertTrue(Files.exists(stream.getStreamConfigFile()));
		assertTrue(Files.exists(stream.getSnapshotsDir()));
		assertTrue(Files.exists(stream.getReceiveTempDir()));
		assertTrue(Files.exists(repository.getWorkingDirectory(stream)));

		// read repository back and compare
		Files.copy(stream.getStreamConfigFile(), System.out);
		Files.copy(stream.getVersionHistoryFile(), System.out);

		Stream readStream = service.readStream(repository, "test");
		assertNotNull(readStream);
		assertEquals(stream.name, readStream.name);
		assertEquals(stream.id, readStream.id);
		assertEquals(stream.versionHistory, readStream.versionHistory);
		assertEquals(stream.initialRetentionPeriod,
				readStream.initialRetentionPeriod);
		assertEquals(stream.streamRepository, readStream.streamRepository);
	}

	@Test
	public void testGetStreamNames() throws Exception {
		assertTrue(service.getStreamNames(repository).isEmpty());

		createStream("test");

		Set<String> streamNames = service.getStreamNames(repository);
		assertEquals(1, streamNames.size());
		assertTrue(streamNames.contains("test"));
	}

	@Test
	public void testDeleteStream() throws Exception {
		Stream stream = createStream("test");
		service.takeSnapshot(stream);

		assertFalse(service.getStreamNames(repository).isEmpty());
		assertTrue(Files.exists(stream.getStreamMetaDirectory()));
		assertTrue(Files.exists(repository.getWorkingDirectory(stream)));

		service.deleteStream(stream);

		assertTrue(service.getStreamNames(repository).isEmpty());
		assertFalse(Files.exists(stream.getStreamMetaDirectory()));
		assertFalse(Files.exists(repository.getWorkingDirectory(stream)));
	}

	private Stream createStream(String name) throws IOException {
		return service.createStream(repository, name);
	}

	@Test
	public void testDeleteStreams() throws Exception {
		Stream stream = createStream("test");
		assertFalse(service.getStreamNames(repository).isEmpty());
		assertTrue(Files.exists(stream.getStreamMetaDirectory()));
		assertTrue(Files.exists(repository.getWorkingDirectory(stream)));

		service.deleteStreams(repository);

		assertTrue(service.getStreamNames(repository).isEmpty());
		assertFalse(Files.exists(stream.getStreamMetaDirectory()));
		assertFalse(Files.exists(repository.getWorkingDirectory(stream)));
	}

	@Test
	public void testTakeAndDeleteSnapshot() throws Exception {
		Stream stream = createStream("test");
		// create test file
		{
			Path testFile = repository.getWorkingDirectory(stream).resolve(
					"test.txt");
			Files.copy(new ByteArrayInputStream("Hello".getBytes("UTF-8")),
					testFile);
		}
		assertTrue(service.getSnapshots(stream).isEmpty());

		Snapshot snapshot = service.takeSnapshot(stream);

		assertTrue(Files.exists(snapshot.getSnapshotDir()));
		assertTrue(Files.exists(snapshot.getSnapshotDir().resolve("test.txt")));
		assertFalse(service.getSnapshots(stream).isEmpty());

		service.deleteSnapshot(snapshot);
		assertFalse(Files.exists(snapshot.getSnapshotDir()));
		assertTrue(service.getSnapshots(stream).isEmpty());
	}

	@Test
	public void testRestoreSnapshot() throws Exception {
		Stream stream = createStream("test");
		// create test file

		Path testFile = repository.getWorkingDirectory(stream).resolve(
				"test.txt");
		Files.copy(new ByteArrayInputStream("Hello".getBytes("UTF-8")),
				testFile);

		Snapshot snapshot = service.takeSnapshot(stream);

		assertTrue(Files.exists(testFile));
		Files.delete(testFile);
		assertFalse(Files.exists(testFile));

		service.restoreSnapshot(snapshot);
		assertTrue(Files.exists(testFile));

		// test if file is writeable
		Files.delete(testFile);
	}
}
