package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;

import javax.inject.Singleton;

import com.github.ruediste1.btrbck.SyncService.SendFileSpec;
import com.github.ruediste1.btrbck.dom.Snapshot;

/**
 * Encapsulates BTRFS operations
 */
@Singleton
public class BtrfsService {

	public boolean useSudo;

	public void createSubVolume(Path subVolumeDir) {
		String path = subVolumeDir.toAbsolutePath().toString();
		try {
			int exitValue;
			exitValue = processBuilder("btrfs", "subvolume", "create", path)
					.start().waitFor();
			if (exitValue != 0) {
				throw new IOException("exit code: " + exitValue);
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error while creating sub volume in "
					+ path, e);
		}
	}

	private ProcessBuilder processBuilder(String... commands) {
		LinkedList<String> list = new LinkedList<String>(
				Arrays.asList(commands));
		return processBuilder(list);
	}

	private ProcessBuilder processBuilder(LinkedList<String> list) {
		if (useSudo) {
			list.addFirst("sudo");
		}
		return new ProcessBuilder().redirectError(Redirect.INHERIT)
				.redirectOutput(Redirect.INHERIT).command(list);
	}

	public void deleteSubVolume(Path subVolumeDir) {
		String path = subVolumeDir.toAbsolutePath().toString();
		try {
			int exitValue;
			exitValue = processBuilder("btrfs", "subvolume", "delete", path)
					.start().waitFor();
			if (exitValue != 0) {
				throw new IOException("exit code: " + exitValue);
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error while deleting sub volume in "
					+ path, e);
		}

	}

	public void receive(Path destinationPath, Consumer<OutputStream> callback) {
		try {
			Process process = processBuilder("btrfs", "receive")
					.directory(destinationPath.toFile())
					.redirectOutput(Redirect.PIPE).start();
			callback.consume(process.getOutputStream());
			int exitValue = process.waitFor();
			if (exitValue != 0) {
				throw new IOException("exit code: " + exitValue);
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(
					"Error while receiving snapshot sub volume in "
							+ destinationPath, e);
		}
	}

	public void send(SendFileSpec sendFile, Consumer<InputStream> callback) {
		try {
			LinkedList<String> args = new LinkedList<>();
			args.addAll(Arrays.asList("btrfs", "send"));
			for (Snapshot s : sendFile.cloneSources) {
				args.add("-c");
				args.add(s.getSnapshotDir().toAbsolutePath().toString());
			}
			args.add(sendFile.target.getSnapshotDir().toAbsolutePath()
					.toString());
			Process process = processBuilder(args).redirectInput(Redirect.PIPE)
					.start();
			callback.consume(process.getInputStream());
			int exitValue = process.waitFor();
			if (exitValue != 0) {
				throw new IOException("exit code: " + exitValue);
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(
					"Error while receiving snapshot sub volume in " + sendFile,
					e);
		}
	}
}
