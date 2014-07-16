package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.LinkedList;

import javax.inject.Singleton;

import com.github.ruediste1.btrbck.dom.RemoteRepository;
import com.github.ruediste1.btrbck.dom.SshTarget;
import com.google.common.base.Strings;

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
		public void close() throws InterruptedException {
			process.waitFor();
		}
	}

	public interface SshConnection {
		InputStream getInputStream();

		OutputStream getOutputStream();

		void close() throws InterruptedException;
	}

	private ProcessBuilder processBuilder(SshTarget target, String... commands) {

		// construct command
		LinkedList<String> list = new LinkedList<String>();
		list.add("ssh");

		// add keyfile
		if (!Strings.isNullOrEmpty(target.getKeyFile())) {
			list.add("-i");
			list.add(target.getKeyFile());
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
		list.addAll(Arrays.asList(commands));

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
			String remoteStreamName) throws IOException {
		Process process = processBuilder(repo.sshTarget, "btrbck",
				"receiveSnapshots", repo.location, remoteStreamName).start();
		return new SshConnectionimpl(process);
	}
}
