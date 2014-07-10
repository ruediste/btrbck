package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;

import javax.inject.Singleton;

/**
 * Encapsulates BTRFS operations
 */
@Singleton
public class BtrfsService {

	public boolean useSudo;

	public void createSubVolume(Path workingDirectory) {
		String path = workingDirectory.toAbsolutePath().toString();
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
		if (useSudo) {
			list.addFirst("sudo");
		}
		return new ProcessBuilder().redirectError(Redirect.INHERIT)
				.redirectOutput(Redirect.INHERIT).command(list);
	}

	public void deleteSubVolume(Path workingDirectory) {
		String path = workingDirectory.toAbsolutePath().toString();
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

}
