package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.github.ruediste1.btrbck.SshService.SshConnection;
import com.github.ruediste1.btrbck.SyncService.SendFileSpec;
import com.github.ruediste1.btrbck.dom.RemoteRepository;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.StreamRepository;
import com.github.ruediste1.btrbck.dto.SendFile;
import com.github.ruediste1.btrbck.dto.SendFileListHeader;
import com.github.ruediste1.btrbck.dto.StreamState;

/*
 * @startuml doc-files/pullSeq.png
 * Local -> Remote: open SSH connection
 * note right of Remote: sendSnapshots
 * activate Remote
 * Local <- Remote: send ready indicator
 * Local -> Remote: send available snapshots
 * Local <- Remote: send missing snapshots
 * deactivate Remote
 * @enduml
 */

/*
 * @startuml doc-files/pushSeq.png
 * Local -> Remote: open SSH connection
 * activate Remote
 * note right of Remote: receiveSnapshots
 * Local <- Remote: send ready indicator
 * Local -> Remote: send start command
 * Local <- Remote: send available snapshots
 * Local -> Remote: send missing snapshots
 * deactivate Remote
 * @enduml
 */

/**
 * Service managing the transfer of snapshots between repositories.
 * 
 * <p>
 * <strong> Pull Snapshots from Remote </strong> <br/>
 * </p>
 * <p>
 * <img src="doc-files/pullSeq.png"/>
 * </p>
 * 
 * <p>
 * <strong> Push Snapshots to Remote </strong> <br/>
 * </p>
 * <p>
 * <img src="doc-files/pushSeq.png"/>
 * </p>
 */
@Singleton
public class SnapshotTransferService {
	public static final String READY_INDICATOR = "BtrBck READY";
	public static final String START_COMMAND = "BtrBck START";

	@Inject
	SshService sshService;

	@Inject
	SyncService syncService;

	@Inject
	StreamService streamService;

	@Inject
	BtrfsService btrfsService;

	@Inject
	BlockTransferService blockTransferService;

	/**
	 * Send the {@link #READY_INDICATOR}, wait for the {@link #START_COMMAND},
	 * send the available snapshots and read the missing snapshots
	 * 
	 * <p>
	 * <img src="doc-files/pushSeq.png"/>
	 * </p>
	 */
	public void receiveSnapshots(StreamRepository repo, String streamName,
			InputStream input, OutputStream output,
			boolean createStreamIfNecessary) {
		try {
			// load or create stream
			Stream stream = streamService.tryReadStream(repo, streamName);
			boolean isNew = false;
			if (stream == null) {
				// stream does not exist, create
				if (!createStreamIfNecessary) {
					throw new DisplayException("stream " + streamName
							+ " does not exist in repository "
							+ repo.rootDirectory.toAbsolutePath());
				}
				stream = streamService.createStream(repo, streamName);
				isNew = true;
			}
			Util.send(READY_INDICATOR, output);
			Util.waitFor(START_COMMAND, input);

			// send available snapshots
			Util.send(syncService.calculateStreamState(stream, isNew), output);

			// receive missing snapshots
			receiveMissingSnapshots(stream, isNew, input);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (DisplayException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Error while receiving snapshots", e);

		}
	}

	/**
	 * Read the available snapshots from remote and send the missing snapshots
	 * <p>
	 * <img src="doc-files/pullSeq.png"/>
	 * </p>
	 */
	public void sendSnapshots(StreamRepository repo, String streamName,
			InputStream input, OutputStream output) {
		try {
			// read stream
			Stream stream = streamService.readStream(repo, streamName);

			// send ready
			Util.send(READY_INDICATOR, output);

			// read available snapshots
			StreamState streamState = Util.read(StreamState.class, input);

			// send missing snapshots
			sendMissingSnapshots(stream, streamState, output);

		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);

		} catch (Exception e) {
			throw new RuntimeException("Error while sending snapshots", e);

		}
	}

	/**
	 * Open an ssh connection to the target, start the btrbck tool, wait for it
	 * to become ready, send the start signal, read the available snapshots and
	 * sent the missing snapshots to it.
	 * <p>
	 * <img src="doc-files/pushSeq.png"/>
	 * </p>
	 */
	public void push(Stream stream, RemoteRepository repo,
			String remoteStreamName, boolean createRemoteIfNecessary) {
		try {
			SshConnection process = sshService.receiveSnapshots(repo,
					remoteStreamName, createRemoteIfNecessary);
			InputStream input = process.getInputStream();
			OutputStream output = process.getOutputStream();

			// wait for the ready signal
			Util.waitFor(READY_INDICATOR, input);

			// send the start command
			Util.send(START_COMMAND, output);

			// read available snapshots
			StreamState streamState = Util.read(StreamState.class, input);

			// send missing snapshots
			sendMissingSnapshots(stream, streamState, output);

			process.close();
		} catch (DisplayException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);

		}
	}

	/**
	 * Open a ssh connection to the target, start the btrbck tool, send it the
	 * available snapshots and read the missing snapshots.
	 * 
	 * @param stream
	 *            the stream to pull into, or null if a new stream is to be
	 *            created
	 */
	public void pull(StreamRepository localRepo, String localStreamName,
			RemoteRepository remoteRepo, String remoteStreamName,
			boolean createStreamIfNecessary) {
		try {
			Stream stream = streamService.tryReadStream(localRepo,
					localStreamName);
			boolean isNewStream = false;
			if (stream == null) {
				// the stream was not found, a new one must be created
				if (!createStreamIfNecessary) {
					throw new DisplayException("Local stream "
							+ localStreamName + " was not found in repository "
							+ localRepo.rootDirectory.toAbsolutePath());
				}
				stream = streamService.createStream(localRepo, localStreamName);
				isNewStream = true;
			}

			SshConnection connection = sshService.sendSnapshots(remoteRepo,
					remoteStreamName);
			InputStream input = connection.getInputStream();

			// wait for the ready signal
			Util.waitFor(READY_INDICATOR, input);

			// send available snapshots
			StreamState streamState = syncService.calculateStreamState(stream,
					isNewStream);
			Util.send(streamState, connection.getOutputStream());

			// process incoming snapshots
			receiveMissingSnapshots(stream, isNewStream, input);

			connection.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Error while pulling from remote respository", e);

		}
	}

	void receiveMissingSnapshots(Stream stream, boolean isNew,
			final InputStream input) throws ClassNotFoundException, IOException {
		streamService.clearReceiveTempDir(stream);
		SendFileListHeader header = Util.read(SendFileListHeader.class, input);

		// if the stream is new
		if (isNew && header.streamConfiguration != null) {
			Files.write(stream.getStreamConfigFile(),
					header.streamConfiguration);
		}

		// check version histories for compatibility
		if (!stream.versionHistory.isAncestorOf(header.targetVersionHistory)) {
			throw new DisplayException(
					"the history of the target stream is not an ancestor of the source history");
		}

		stream.versionHistory = header.targetVersionHistory;
		streamService.writeVersionHistory(stream);

		for (int i = 0; i < header.count; i++) {
			SendFile sendFile = Util.read(SendFile.class, input);
			btrfsService.receive(stream.getReceiveTempDir(),
					new Consumer<OutputStream>() {

						@Override
						public void consume(OutputStream value) {
							try {
								blockTransferService.readBlocks(input, value);
								value.close();
							} catch (ClassNotFoundException | IOException e) {
								throw new RuntimeException(e);
							}
						}
					});

			// move to final destination
			Path tmpSnapshot = stream.getReceiveTempDir().resolve(
					sendFile.snapshotName);
			btrfsService.takeSnapshot(tmpSnapshot, stream.getSnapshotsDir(),
					true);
			btrfsService.deleteSubVolume(tmpSnapshot);
		}
	}

	void sendMissingSnapshots(Stream stream, StreamState streamState,
			final OutputStream output) throws IOException {
		List<SendFileSpec> sendFiles = syncService.determineSendFiles(stream,
				streamState);
		{
			SendFileListHeader header = new SendFileListHeader();
			header.count = sendFiles.size();
			header.targetVersionHistory = stream.versionHistory;
			if (streamState.isNewStream) {
				// if the stream is new, send the configuration
				header.streamConfiguration = Files.readAllBytes(stream
						.getStreamConfigFile());
			}
			Util.send(header, output);
		}
		for (SendFileSpec sendFile : sendFiles) {
			{
				SendFile s = new SendFile();
				s.snapshotName = sendFile.target.getSnapshotName();
				Util.send(s, output);
			}
			btrfsService.send(sendFile, new Consumer<InputStream>() {

				@Override
				public void consume(InputStream value) {
					try {
						blockTransferService.sendBlocks(value, output,
								1024 * 1024);
					} catch (IOException e) {
						throw new RuntimeException(
								"Error while sending snapshot", e);
					}
				}
			});
		}
	}

}
