package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.github.ruediste1.btrbck.SshService.SshConnection;
import com.github.ruediste1.btrbck.SyncService.SendFileSpec;
import com.github.ruediste1.btrbck.dom.RemoteRepository;
import com.github.ruediste1.btrbck.dom.Snapshot;
import com.github.ruediste1.btrbck.dom.Stream;
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
	public void receiveSnapshots(Stream stream, InputStream input,
			OutputStream output) {
		try {
			Util.send(READY_INDICATOR, output);
			Util.waitFor(START_COMMAND, input);

			// send available snapshots
			Util.send(syncService.calculateStreamState(stream), output);

			// receive missing snapshots
			receiveMissingSnapshots(stream, input);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);

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
	public void sendSnapshots(Stream stream, InputStream input,
			OutputStream output) {
		try {
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
			String remoteStreamName) {
		try {
			Process process = sshService.receiveSnapshots(repo,
					remoteStreamName);
			InputStream input = process.getInputStream();
			OutputStream output = process.getOutputStream();

			// wait for the ready signal
			Util.waitFor(READY_INDICATOR, input);

			// send the start command
			Util.send(START_COMMAND, output);

			// read available snapshots
			StreamState streamState = Util.read(StreamState.class, input);

			// send missing snapshots
			sendMissingSnapshots(null, streamState, output);

			process.waitFor();
		} catch (Exception e) {
			throw new RuntimeException(e);

		}
	}

	/**
	 * Open a ssh connection to the target, start the btrbck tool, send it the
	 * available snapshots and read the missing snapshots.
	 */
	public void pull(Stream stream, RemoteRepository repo,
			String remoteStreamName) {
		try {
			SshConnection connection = sshService.sendSnapshots(repo,
					remoteStreamName);
			InputStream input = connection.getInputStream();

			// wait for the ready signal
			Util.waitFor(READY_INDICATOR, input);

			// send available snapshots
			Util.send(syncService.calculateStreamState(stream),
					connection.getOutputStream());

			// process incoming snapshots
			receiveMissingSnapshots(stream, input);

			connection.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Error while pulling from remote respository", e);

		}
	}

	void receiveMissingSnapshots(Stream stream, final InputStream input)
			throws ClassNotFoundException, IOException {
		streamService.clearReceiveTempDir(stream);
		SendFileListHeader header = Util.read(SendFileListHeader.class, input);
		for (int i = 0; i < header.count; i++) {
			SendFile sendFile = Util.read(SendFile.class, input);
			btrfsService.receive(stream.getReceiveTempDir(),
					new Consumer<OutputStream>() {

						@Override
						public void consume(OutputStream value) {
							try {
								blockTransferService.readBlocks(input, value);
							} catch (ClassNotFoundException | IOException e) {
								throw new RuntimeException(e);
							}
						}
					});
			Snapshot snapshot = Snapshot.parse(stream, sendFile.snapshotName);

			// move to final destination
			Files.move(stream.getReceiveTempDir()
					.resolve(sendFile.snapshotName), snapshot.getSnapshotDir());
		}
	}

	void sendMissingSnapshots(Stream stream, StreamState streamState,
			final OutputStream output) {
		for (SendFileSpec sendFile : syncService.determineSendFiles(stream,
				streamState)) {
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
