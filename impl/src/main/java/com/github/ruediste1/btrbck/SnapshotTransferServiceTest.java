package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.nio.file.Files;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.BackupStreamRepository;
import com.github.ruediste1.btrbck.dom.RemoteRepository;
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

	private class SshServiceTestDouble extends SshService {

		private StreamRepository remoteRepo;

		public SshServiceTestDouble(StreamRepository remoteRepo) {
			this.remoteRepo = remoteRepo;
		}

		@Override
		public SshConnection receiveSnapshots(RemoteRepository repoUnused,
				String remoteStreamName) throws IOException {

			Stream stream = streamService.readStream(remoteRepo,
					remoteStreamName);
			transferService.receiveSnapshots(stream, null, null);
			// TODO Auto-generated method stub
			return super.receiveSnapshots(repo, remoteStreamName);
		}
	}

	@Before
	public void setup() throws Exception {
		repo1 = new ApplicationStreamRepository();
		repo1.rootDirectory = Files.createTempDirectory("btrbck");
		repositoryService.initializeRepository(repo1);

		repo2 = new BackupStreamRepository();
		repo2.rootDirectory = Files.createTempDirectory("btrbck");
		repositoryService.initializeRepository(repo2);
	}

	@Test
	public void transferSnapshot() {
	}
}
