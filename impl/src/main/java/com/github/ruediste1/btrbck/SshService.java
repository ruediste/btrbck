package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Singleton;

import com.github.ruediste1.btrbck.dom.RemoteRepository;
import com.github.ruediste1.btrbck.dom.SshTarget;

@Singleton
public class SshService {

	private final class SshConnectionimpl implements SshConnection {
		private final Process process;

		private SshConnectionimpl(Process process) {
			this.process = process;
		}

		@Override
		public OutputStream getOutputStream() {
			return process.getOutputStream();
		}

		@Override
		public InputStream getInputStream() {
			return process.getInputStream();
		}

		@Override
		public void close() throws Exception {
			process.getInputStream().close();
			process.waitFor();
		}
	}

	public interface SshConnection {
		InputStream getInputStream();

		OutputStream getOutputStream();

		void close() throws Exception;
	}

	private ProcessBuilder processBuilder(SshTarget target, String... commands) {
		return processBuilder(target, Arrays.asList(commands));
	}

	private ProcessBuilder processBuilder(SshTarget target,
			List<String> commands) {
		// construct command
		LinkedList<String> list = new LinkedList<String>();
		list.add("ssh");

		// add keyfile
		if (target.getKeyFile() != null) {
			list.add("-i");
			list.add(target.getKeyFile().getAbsolutePath());
		}

		// add port
		if (target.getPort() != null) {
			list.add("-p");
			list.add(target.getPort().toString());
		}

		// add other parameters
		list.addAll(target.getParameters());

		// add host
		list.add(target.getHost());

		// add commands
		list.addAll(commands);

		return new ProcessBuilder().redirectError(Redirect.INHERIT).command(
				list);
	}

	public SshConnection sendSnapshots(RemoteRepository repo,
			String remoteStreamName) throws IOException {
		final Process process = processBuilder(repo.sshTarget, "btrbck",
				"sendSnapshots", repo.location, remoteStreamName).start();
		return new SshConnectionimpl(process);
	}

	public SshConnection receiveSnapshots(RemoteRepository repo,
			String remoteStreamName, boolean createRemoteIfNecessary)
			throws IOException {
		List<String> commands = Arrays.asList(new String[] { "btrbck",
				"receiveSnapshots" });
		if (createRemoteIfNecessary) {
			commands.add("-c");
		}
		commands.add(repo.location);
		commands.add(remoteStreamName);
		Process process = processBuilder(repo.sshTarget, commands).start();
		return new SshConnectionimpl(process);
	}
}
