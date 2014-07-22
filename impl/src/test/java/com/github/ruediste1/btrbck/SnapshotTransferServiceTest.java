package com.github.ruediste1.btrbck;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.BackupStreamRepository;
import com.github.ruediste1.btrbck.dom.RemoteRepository;
import com.github.ruediste1.btrbck.dom.Snapshot;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.StreamRepository;
import com.github.ruediste1.btrbck.test.TestBase;

public class SnapshotTransferServiceTest extends TestBase {

	@Inject
	SnapshotTransferService transferService;

	@Inject
	StreamRepositoryService repositoryService;

	@Inject
	StreamService streamService;

	ApplicationStreamRepository repo1;
	BackupStreamRepository repo2;

	private interface ThreadOperation {
		void run(StreamRepository repo, String streamName, InputStream input,
				OutputStream output);
	}

	private class SshServiceTestDouble extends SshService {

		private StreamRepository remoteRepo;

		public SshServiceTestDouble(StreamRepository remoteRepo) {
			this.remoteRepo = remoteRepo;
		}

		@Override
		public SshConnection receiveSnapshots(RemoteRepository repoUnused,
				String remoteStreamName) throws IOException {
			return doInThread(remoteStreamName, new ThreadOperation() {

				@Override
				public void run(StreamRepository repo, String streamName,
						InputStream input, OutputStream output) {
					transferService.receiveSnapshots(repo, streamName, input,
							output);

				}
			});
		}

		@Override
		public SshConnection sendSnapshots(RemoteRepository repo,
				String remoteStreamName) throws IOException {
			return doInThread(remoteStreamName, new ThreadOperation() {

				@Override
				public void run(StreamRepository repo, String streamName,
						InputStream input, OutputStream output) {
					transferService.sendSnapshots(repo, streamName, input,
							output);

				}
			});
		}

		private SshConnection doInThread(final String remoteStreamName,
				final ThreadOperation operation) throws IOException {

			final PipedInputStream returnedInput = new PipedInputStream();
			final PipedInputStream threadInput = new PipedInputStream();
			final PipedOutputStream threadOutput = new PipedOutputStream(
					returnedInput);
			final PipedOutputStream returnedOutput = new PipedOutputStream(
					threadInput);

			ExecutorService exec = Executors.newSingleThreadExecutor();
			final Future<?> future = exec.submit(new Runnable() {

				@Override
				public void run() {
					try {

						operation.run(remoteRepo, remoteStreamName,
								threadInput, threadOutput);
					} catch (Throwable t) {
						System.err.println("Error in threaded operation");
						System.err.println(t.getMessage());
						t.printStackTrace();
						throw t;
					}
				}
			});
			return new SshConnection() {

				@Override
				public OutputStream getOutputStream() {
					return returnedOutput;
				}

				@Override
				public InputStream getInputStream() {
					return returnedInput;
				}

				@Override
				public void close() throws Exception {
					// returnedOutput.flush();
					// threadInput.close();
					future.get();
				}
			};
		}
	}

	@Before
	public void setup() throws Exception {
		repo1 = new ApplicationStreamRepository();
		repo1.rootDirectory = createTempDirectory();
		repositoryService.createRepository(repo1);

		repo2 = new BackupStreamRepository();
		repo2.rootDirectory = createTempDirectory();
		repositoryService.createRepository(repo2);

		transferService.sshService = new SshServiceTestDouble(repo2);
	}

	@After
	public void tearDown() {
		streamService.deleteStreams(repo1);
		repositoryService.deleteEmptyRepository(repo1);
		streamService.deleteStreams(repo2);
		repositoryService.deleteEmptyRepository(repo2);
	}

	@Test
	public void transferSnapshot() throws Exception {
		Stream stream = streamService.createStream(repo1, "test");
		Path testFile = repo1.getWorkingDirectory(stream).resolve("test.txt");
		Files.copy(new ByteArrayInputStream("Hello".getBytes("UTF-8")),
				testFile);
		Snapshot snapshot = streamService.takeSnapshot(stream);
		transferService.push(stream, null, "test2");

		Stream targetStream = streamService.readStream(repo2, "test2");
		assertThat(targetStream, not(nullValue()));
		Path snapshotDir = targetStream.getSnapshotsDir().resolve(
				snapshot.getSnapshotName());
		assertThat(Files.exists(snapshotDir), is(true));
		assertThat(Files.exists(snapshotDir.resolve("test.txt")), is(true));
	}
}
