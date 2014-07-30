package com.github.ruediste1.btrbck.cli;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.ruediste1.btrbck.BtrfsService;
import com.github.ruediste1.btrbck.DisplayException;
import com.github.ruediste1.btrbck.GuiceModule;
import com.github.ruediste1.btrbck.SnapshotTransferService;
import com.github.ruediste1.btrbck.SshService;
import com.github.ruediste1.btrbck.StreamRepositoryService;
import com.github.ruediste1.btrbck.StreamService;
import com.github.ruediste1.btrbck.Util;
import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.BackupStreamRepository;
import com.github.ruediste1.btrbck.dom.RemoteRepository;
import com.github.ruediste1.btrbck.dom.Snapshot;
import com.github.ruediste1.btrbck.dom.SshTarget;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.StreamRepository;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class CliMain {
	Logger log = LoggerFactory.getLogger(CliMain.class);

	@Option(name = "-r", usage = "the location of the stream repository to use")
	File repositoryLocation;

	@Option(name = "-c", usage = "if given, missing target streams will be created during the push, pull and the sync command")
	boolean createTargetStreams;

	@Option(name = "-a", usage = "if given, the initialize command creates an application stream repository")
	boolean applicationRepository;

	@Option(name = "-sudo", usage = "if given, use sudo to execute local btrfs commands")
	boolean sudoLocalBtrfs;

	@Option(name = "-strace", usage = "if given, use strace for certain commands. The output will be logged to strace.XXX.log files")
	boolean useStrace;

	@Option(name = "-sudoRemoteBtrbck", usage = "if given, use sudo to execute remote btrbck commands")
	boolean sudoRemoteBtrbck;

	@Option(name = "-sudoRemoteBtrfs", usage = "if given, use sudo to execute remote btrfs commands")
	boolean sudoRemoteBtrfs;

	@Option(name = "-i", usage = "the key file to be used by ssh")
	File keyFile;

	@Option(name = "-v", usage = "show verbose output")
	boolean verbose;

	@Argument(hidden = true)
	List<String> arguments = new ArrayList<>();

	@Inject
	StreamRepositoryService streamRepositoryService;

	@Inject
	StreamService streamService;

	@Inject
	SnapshotTransferService streamTransferService;

	@Inject
	BtrfsService btrfsService;

	@Inject
	SshService sshService;

	private FileLock repositoryLock;

	public static void main(String... args) throws Exception {
		new CliMain().doMain(args);
	}

	private void doMain(String[] args) throws Exception {
		byte[] buf = new byte[4];
		new Random().nextBytes(buf);
		MDC.put("id", BaseEncoding.base16().encode(buf));

		Injector injector = Guice.createInjector(new GuiceModule());
		Util.setInjector(injector);
		Util.injectMembers(this);

		try {
			processCommand(args);
		} catch (DisplayException e) {
			System.err.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}

	void processCommand(String... args) throws IOException {
		try {
			parseCmdLine(args);

			log.debug("args: " + Arrays.toString(args));
			log.debug("Arguments: " + arguments);

			String command = arguments.get(0);
			if ("snapshot".equals(command)) {
				cmdSnapshot();
			} else if ("list".equals(command)) {
				cmdList();
			} else if ("push".equals(command)) {
				cmdPush();
			} else if ("pull".equals(command)) {
				cmdPull();
			} else if ("process".equals(command)) {
				cmdProcess();
			} else if ("prune".equals(command)) {
				cmdPrune();
			} else if ("create".equals(command)) {
				cmdCreate();
			} else if ("delete".equals(command)) {
				cmdDelete();
			} else if ("restore".equals(command)) {
				cmdRestore();
			} else if ("receiveSnapshots".equals(command)) {
				cmdReceiveSnapshots();
			} else if ("sendSnapshots".equals(command)) {
				cmdSendSnapshots();
			} else if ("lock".equals(command)) {
				cmdLock();
			} else {
				throw new DisplayException("Unknown command " + command);
			}
		} finally {
			if (repositoryLock != null) {
				repositoryLock.release();
			}
		}
	}

	private void cmdLock() {
		readAndLockRepository();
		Console console = System.console();
		if (console != null) {
			console.printf("Repository locked. Press enter to unlock.\n");
			console.readLine();
			console.printf("Repository unlocked.\n");
		}
	}

	private void cmdSendSnapshots() {
		if (arguments.size() != 2) {
			throw new DisplayException("Usage: sendSnapshots <streamName>");
		}
		StreamRepository repo = readAndLockRepository();
		streamTransferService.sendSnapshots(repo, arguments.get(1), System.in,
				System.out);
	}

	private void cmdReceiveSnapshots() {
		if (arguments.size() != 2) {
			throw new DisplayException("Usage: receiveSnapshots <streamName>");
		}
		StreamRepository repo = readAndLockRepository();
		streamTransferService.receiveSnapshots(repo, arguments.get(1),
				System.in, System.out, createTargetStreams);
	}

	private void parseCmdLine(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);

		// if you have a wider console, you could increase the value;
		// here 80 is also the default
		parser.setUsageWidth(80);

		try {
			// parse the arguments.
			parser.parseArgument(args);

			if (arguments.isEmpty()) {
				throw new CmdLineException(parser, "No command given");
			}
		} catch (CmdLineException e) {
			// if there's a problem in the command line,
			// you'll get this exception. this will report
			// an error message.
			System.err.println("Error: " + e.getMessage());

			try {
				ByteStreams.copy(getClass().getResourceAsStream("usage.txt"),
						System.err);
			} catch (IOException e1) {
				throw new RuntimeException("Error while printing usage", e1);
			}

			System.err.println("\n\nOptions: ");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			System.exit(1);
		}

		// initialize sudoConfig
		btrfsService.setUseSudo(sudoLocalBtrfs);
		btrfsService.setUseStrace(useStrace);
		sshService.setSudoRemoteBtrbck(sudoRemoteBtrbck);
		sshService.setSudoRemoteBtrfs(sudoRemoteBtrfs);

		// configure loglevel
		if (verbose) {
			org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
			sshService.setVerboseRemote(verbose);
		}
	}

	private void cmdPrune() {
		// TODO Auto-generated method stub

	}

	private void cmdProcess() {
		// TODO Auto-generated method stub

	}

	private void cmdPush() {
		if (arguments.size() < 4) {
			throw new DisplayException("Not enought arguments");
		}

		if (arguments.size() > 5) {
			throw new DisplayException("Too many arguments");
		}

		String streamName = arguments.get(1);
		SshTarget sshTarget = SshTarget.parse(arguments.get(2)).withKeyFile(
				keyFile);

		String remoteStreamName = streamName;
		if (arguments.size() == 5) {
			remoteStreamName = arguments.get(4);
		}

		RemoteRepository remoteRepo = new RemoteRepository();
		remoteRepo.location = arguments.get(3);
		remoteRepo.sshTarget = sshTarget;

		StreamRepository repo = readAndLockRepository();
		Stream stream = streamService.readStream(repo, streamName);

		streamTransferService.push(stream, remoteRepo, remoteStreamName,
				createTargetStreams);
	}

	private void cmdPull() {
		if (arguments.size() < 4) {
			throw new DisplayException("Not enought arguments");
		}

		if (arguments.size() > 5) {
			throw new DisplayException("Too many arguments");
		}

		SshTarget sshTarget = SshTarget.parse(arguments.get(1)).withKeyFile(
				keyFile);
		String remoteRepoPath = arguments.get(2);

		String remoteStreamName = arguments.get(3);

		String streamName = remoteStreamName;
		if (arguments.size() == 5) {
			streamName = arguments.get(4);
		}

		RemoteRepository remoteRepo = new RemoteRepository();
		remoteRepo.location = remoteRepoPath;
		remoteRepo.sshTarget = sshTarget;

		StreamRepository repo = readAndLockRepository();

		streamTransferService.pull(repo, streamName, remoteRepo,
				remoteStreamName, createTargetStreams);

	}

	private void cmdList() {
		if (arguments.size() == 1) {
			// list streams in repository
			StreamRepository repo = readAndLockRepository();
			System.out.println("Streams in repository "
					+ repo.rootDirectory.toAbsolutePath() + ":");
			for (String name : streamService.getStreamNames(repo)) {
				System.out.println(name);
			}
		} else if (arguments.size() == 2) {
			StreamRepository repo = readAndLockRepository();
			Stream stream = streamService.readStream(repo, arguments.get(1));
			TreeMap<Integer, Snapshot> snapshots = streamService
					.getSnapshots(stream);
			System.out.println("Snapshots in stream " + stream.name
					+ " in repository " + repo.rootDirectory.toAbsolutePath()
					+ ":");
			for (Snapshot s : snapshots.values()) {
				System.out.println(s.getSnapshotName());
			}
		} else {
			throw new DisplayException("too many arguments");
		}

	}

	private void cmdCreate() throws IOException {
		if (arguments.size() == 1) {
			// create repository
			Path location;
			if (repositoryLocation != null) {
				location = repositoryLocation.toPath();
			} else {
				location = Paths.get("");
			}

			StreamRepository repo;
			if (applicationRepository) {
				repo = streamRepositoryService.createRepository(
						ApplicationStreamRepository.class, location);
			} else {
				repo = streamRepositoryService.createRepository(
						BackupStreamRepository.class, location);
			}

			System.out.println("Created repository in "
					+ repo.rootDirectory.toAbsolutePath());
		} else if (arguments.size() == 2) {
			// create stream
			String streamName = arguments.get(1);
			StreamRepository repo = readAndLockRepository();
			streamService.createStream(repo, streamName);
		} else {
			throw new DisplayException("too many arguments");
		}
	}

	private void cmdDelete() {
		if (arguments.size() == 1) {
			StreamRepository repo = readAndLockRepository();
			// delete repository
			streamService.deleteStreams(repo);
			streamRepositoryService.deleteEmptyRepository(repo);
		} else if (arguments.size() == 2) {
			StreamRepository repo = readAndLockRepository();
			streamService.deleteStream(repo, arguments.get(1));
		} else {
			throw new DisplayException("too many arguments");
		}

	}

	private void cmdSnapshot() {
		if (arguments.size() == 1) {
			StreamRepository repo = readAndLockRepository();
			for (String name : streamService.getStreamNames(repo)) {
				Stream stream = streamService.readStream(repo, name);
				streamService.takeSnapshot(stream);
			}
		} else if (arguments.size() == 2) {
			StreamRepository repo = readAndLockRepository();
			String streamName = arguments.get(1);
			Stream stream = streamService.readStream(repo, streamName);
			streamService.takeSnapshot(stream);
		} else {
			throw new DisplayException("too many arguments");
		}
	}

	private void cmdRestore() {
		if (arguments.size() == 1) {
			// restore the latest snapshot of all streams
			StreamRepository repo = readAndLockRepository();
			for (String name : streamService.getStreamNames(repo)) {
				Stream stream = streamService.readStream(repo, name);
				streamService.restoreLatestSnapshot(stream);
			}
		} else if (arguments.size() == 2) {
			// restore the latest snapshot of a single stream
			StreamRepository repo = readAndLockRepository();
			Stream stream = streamService.readStream(repo, arguments.get(1));
			streamService.restoreLatestSnapshot(stream);
		} else if (arguments.size() == 3) {
			// restore a specific snapshot of a single stream
			StreamRepository repo = readAndLockRepository();
			Stream stream = streamService.readStream(repo, arguments.get(1));
			int snapshotNr = Integer.parseInt(arguments.get(2));
			streamService.restoreSnapshot(stream, snapshotNr);
		} else {
			throw new DisplayException("too many arguments");
		}

	}

	private StreamRepository readAndLockRepository() {
		File path = repositoryLocation;
		if (path == null) {
			path = Paths.get("").toFile();
		}
		StreamRepository repo = streamRepositoryService.readRepository(path
				.toPath());
		FileChannel f;
		try {
			f = FileChannel.open(repo.getRepositoryLockFile(),
					StandardOpenOption.WRITE);
			repositoryLock = f.lock(0L, Long.MAX_VALUE, false);
		} catch (IOException e) {
			throw new RuntimeException("Error while locking repository", e);
		}
		return repo;
	}
}
