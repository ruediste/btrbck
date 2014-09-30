package com.github.ruediste1.btrbck.cli;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.ruediste1.btrbck.DisplayException;
import com.github.ruediste1.btrbck.StreamRepositoryService;
import com.github.ruediste1.btrbck.StreamService;
import com.github.ruediste1.btrbck.Util;
import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.BackupStreamRepository;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.StreamRepository;
import com.github.ruediste1.btrbck.test.CliTestBase;

public class CliMainTest extends CliTestBase {

	@Inject
	StreamRepositoryService streamRepositoryService;

	@Inject
	StreamService streamService;

	List<Path> repoLocations = new ArrayList<>();

	private Path createRepoLocation() throws IOException {
		Path result = createTempDirectory();
		repoLocations.add(result);
		return result;
	}

	@Before
	public void setup() {}

	CliMain main() {
		CliMain main = new CliMain();
		Util.injectMembers(main);
		return main;
	}

	@After
	public void tearDown() {
		for (Path p : repoLocations) {
			try {
				StreamRepository repo = streamRepositoryService
						.readRepository(p);
				streamService.deleteStreams(repo);
				streamRepositoryService.deleteEmptyRepository(repo);
				Files.delete(p);
			}
			catch (Throwable t) {
				System.err.println("Error while removing repository: " + t);
			}
		}
	}

	@Test
	public void testCmdCreateBackupRepo() throws Exception {
		Path location = createRepoLocation();
		main().processCommand(
				new String[] { "-r", location.toAbsolutePath().toString(),
						"create" });
		StreamRepository repo = streamRepositoryService
				.readRepository(location);
		assertThat(repo, instanceOf(BackupStreamRepository.class));
	}

	@Test
	public void testCmdCreateApplicationRepo() throws Exception {
		Path location = createRepoLocation();
		main().processCommand(
				new String[] { "-r", location.toAbsolutePath().toString(),
						"-a", "create" });
		StreamRepository repo = streamRepositoryService
				.readRepository(location);
		assertThat(repo, instanceOf(ApplicationStreamRepository.class));
	}

	@Test
	public void testCmdDeleteRepo() throws Exception {
		Path location = createRepoLocation();
		ApplicationStreamRepository repo = streamRepositoryService
				.createRepository(ApplicationStreamRepository.class, location);
		streamService.createStream(repo, "test");

		assertThat(Files.exists(repo.getRepositoryXmlFile()), is(true));
		main().processCommand(
				new String[] { "-r", location.toAbsolutePath().toString(),
						"-sudo", "delete" });

		assertThat(Files.exists(repo.getRepositoryXmlFile()), is(false));
	}

	@Test
	public void testCmdDeleteStream() throws Exception {
		Path location = createRepoLocation();
		ApplicationStreamRepository repo = streamRepositoryService
				.createRepository(ApplicationStreamRepository.class, location);
		Stream stream = streamService.createStream(repo, "test");

		assertThat(Files.exists(repo.getRepositoryXmlFile()), is(true));
		assertThat(Files.exists(stream.getStreamMetaDirectory()), is(true));
		main().processCommand(
				new String[] { "-r", location.toAbsolutePath().toString(),
						"-sudo", "delete", "test" });

		assertThat(Files.exists(stream.getStreamMetaDirectory()), is(false));
		assertThat(Files.exists(repo.getRepositoryXmlFile()), is(true));
	}

	@Test
	public void testCmdSnapshotRestore() throws Exception {
		Path location = createRepoLocation();
		ApplicationStreamRepository repo = streamRepositoryService
				.createRepository(ApplicationStreamRepository.class, location);
		Stream stream = streamService.createStream(repo, "test");

		Path testFile = repo.getWorkingDirectory(stream).resolve("test.txt");

		// create test file
		Files.copy(new ByteArrayInputStream("Hello".getBytes("UTF-8")),
				testFile);

		// take snapshot
		main().processCommand(
				new String[] { "-r", location.toAbsolutePath().toString(),
						"snapshot", "test" });

		// delete file
		assertThat(Files.exists(testFile), is(true));
		Files.delete(testFile);
		assertThat(Files.exists(testFile), is(false));

		// restore snapshot with given number
		main().processCommand(
				new String[] { "-r", location.toAbsolutePath().toString(),
						"-sudo", "restore", "test", "0" });
		assertThat(Files.exists(testFile), is(true));

		// restore latest snapshot
		Files.delete(testFile);
		main().processCommand(
				new String[] { "-r", location.toAbsolutePath().toString(),
						"-sudo", "restore", "test" });
		assertThat(Files.exists(testFile), is(true));

		// restore latest snapshot on all streams
		Files.delete(testFile);
		main().processCommand(
				new String[] { "-r", location.toAbsolutePath().toString(),
						"-sudo", "restore" });
		assertThat(Files.exists(testFile), is(true));
	}

	@Test
	public void testCmdSnapshot() throws Exception {
		Path location = createRepoLocation();
		ApplicationStreamRepository repo = streamRepositoryService
				.createRepository(ApplicationStreamRepository.class, location);
		Stream stream = streamService.createStream(repo, "test");

		assertThat(streamService.getSnapshots(stream).isEmpty(), is(true));
		main().processCommand("-r", location.toAbsolutePath().toString(),
				"-sudo", "snapshot", "test");

		assertThat(streamService.getSnapshots(stream).isEmpty(), is(false));
		assertNull(streamService.getSnapshots(stream).get(0).senderStreamId);
	}

	@Test(expected = DisplayException.class)
	public void testCmdSnapshotInexistantStream() throws Exception {
		Path location = createRepoLocation();
		ApplicationStreamRepository repo = streamRepositoryService
				.createRepository(ApplicationStreamRepository.class, location);
		streamService.createStream(repo, "test");

		main().processCommand("-r", location.toAbsolutePath().toString(),
				"-sudo", "snapshot", "test123");
	}

}
