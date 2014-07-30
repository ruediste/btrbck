package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ruediste1.btrbck.SyncService.SendFileSpec;
import com.github.ruediste1.btrbck.dom.Snapshot;

/**
 * Encapsulates BTRFS operations
 */
@Singleton
public class BtrfsService {
	Logger log = LoggerFactory.getLogger(BtrfsService.class);

	private ThreadLocal<Boolean> useSudo = new ThreadLocal<>();

	public void setUseSudo(boolean value) {
		useSudo.set(value);
	}

	private boolean useSudo() {
		return Boolean.TRUE.equals(useSudo.get());
	}

	public void createSubVolume(Path subVolumeDir) {
		String path = subVolumeDir.toAbsolutePath().toString();
		try {
			{
				int exitValue = processBuilder("btrfs", "subvolume", "create",
						path).start().waitFor();
				if (exitValue != 0) {
					throw new IOException("exit code: " + exitValue);
				}
			}

			// determine the current user
			String userName;
			{
				Process process = new ProcessBuilder("whoami").redirectError(
						Redirect.INHERIT).start();
				int exitValue = process.waitFor();
				if (exitValue != 0) {
					throw new RuntimeException("whoami exited with "
							+ exitValue);
				}
				userName = Util.readFully(process.getInputStream());
				if (userName.endsWith("\n")) {
					userName = userName.substring(0,
							userName.length() - "\n".length());
				}
			}

			// change the owner of the subvolume
			{
				int exitValue = processBuilder("chown", userName + ":", path)
						.start().waitFor();
				if (exitValue != 0) {
					throw new RuntimeException("chown exited with " + exitValue);
				}
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
		if (useSudo()) {
			list.addFirst("sudo");
		}
		log.debug("created process builder: " + list);
		return new ProcessBuilder().redirectError(Redirect.INHERIT)
				.redirectOutput(Redirect.INHERIT).command(list);
	}

	public void deleteSubVolume(Path subVolumeDir) {
		String path = subVolumeDir.toAbsolutePath().toString();
		try {
			int retries = 0;
			while (true) {
				int exitValue = processBuilder("btrfs", "subvolume", "delete",
						path).start().waitFor();
				if (exitValue != 0) {
					log.debug("Delete: Exit code was " + exitValue);
				}
				if (exitValue != 1) {
					// quit retry loop if everything went well
					break;
				} else if (retries > 3) {
					// there was an error and no more retries, throw exception
					throw new IOException("exit code: " + exitValue);
				}

				retries++;
				Thread.sleep(retries * 100);
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error while deleting sub volume in "
					+ path, e);
		}

	}

	public void receive(Path destinationPath, Consumer<OutputStream> callback) {
		try {
			Process process = processBuilder("btrfs", "receive", "-e",
					destinationPath.toAbsolutePath().toString())
					.redirectOutput(Redirect.PIPE).start();
			callback.consume(process.getOutputStream());
			process.getOutputStream().close();
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
			Process process = processBuilder(args)
					.redirectOutput(Redirect.PIPE).start();
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

	/**
	 * Takes a read only snapshot of the source an puts it to the target
	 * 
	 * @param readonly
	 *            TODO
	 */
	public void takeSnapshot(Path sourceVolume, Path target, boolean readonly) {
		try {
			{
				LinkedList<String> list = new LinkedList<String>();
				list.addAll(Arrays.asList("btrfs", "subvolume", "snapshot"));
				if (readonly) {
					list.add("-r");
				}
				list.add(sourceVolume.toAbsolutePath().toString());
				list.add(target.toAbsolutePath().toString());

				int exitValue = processBuilder(list).start().waitFor();
				if (exitValue != 0) {
					log.debug("TakeSnapshot: Exit code was " + exitValue);
				}
				if (exitValue == 1) {
					throw new IOException("exit code: " + exitValue);
				}
			}

		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error while taking snapshot of  "
					+ sourceVolume.toAbsolutePath() + " to "
					+ target.toAbsolutePath(), e);
		}
	}
}
