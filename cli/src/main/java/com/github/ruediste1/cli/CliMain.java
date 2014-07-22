package com.github.ruediste1.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.github.ruediste1.btrbck.DisplayException;
import com.github.ruediste1.btrbck.StreamRepositoryService;
import com.github.ruediste1.btrbck.StreamService;
import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.BackupStreamRepository;
import com.github.ruediste1.btrbck.dom.StreamRepository;
import com.google.common.io.ByteStreams;

public class CliMain {
	@Option(name = "-r", usage = "the location of the stream repository to use")
	File repositoryLocation;

	@Option(name = "-c", usage = "if given, missing remote streams will be created during the push and the sync command")
	boolean createRemoteSnapshots;

	@Option(name = "-a", usage = "if given, the initialize command creates an application stream repository")
	boolean applicationRepository;

	@Argument(hidden = true)
	List<String> arguments = new ArrayList<>();

	@Inject
	StreamRepositoryService streamRepositoryService;

	@Inject
	StreamService streamService;

	public static void main(String... args) throws Exception {
		new CliMain().doMain(args);
	}

	private void doMain(String[] args) throws Exception {
		parseCmdLine(args);

		try {
			String command = arguments.get(0);
			if ("snapshot".equals(command)) {
				cmdSnapshot();
			} else if ("list".equals(command)) {
				cmdList();
			} else if ("push".equals(command)) {
				cmdPush();
			} else if ("pull".equals(command)) {
				cmdPush();
			} else if ("sync".equals(command)) {
				cmdSync();
			} else if ("prune".equals(command)) {
				cmdPrune();
			} else if ("create".equals(command)) {
				cmdCreate();
			} else if ("restore".equals(command)) {
				cmdRestore();
			}
		} catch (DisplayException e) {
			System.err.println("Error: " + e.getMessage());
			System.exit(1);
		}

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

	}

	private void cmdRestore() {
		// TODO Auto-generated method stub

	}

	private void cmdPrune() {
		// TODO Auto-generated method stub

	}

	private void cmdSync() {
		// TODO Auto-generated method stub

	}

	private void cmdPush() {
		// TODO Auto-generated method stub

	}

	private void cmdList() {
		// TODO Auto-generated method stub

	}

	private void cmdCreate() throws IOException {
		if (arguments.size() == 1) {
			// create repository
			StreamRepository repo;
			if (applicationRepository) {
				repo = new ApplicationStreamRepository();
			} else {
				repo = new BackupStreamRepository();
			}

			if (repositoryLocation != null) {
				repo.rootDirectory = repositoryLocation.toPath();
			} else {
				repo.rootDirectory = new File(".").toPath();
			}

			streamRepositoryService.createRepository(repo);

			System.out.println("Created repository in "
					+ repo.rootDirectory.toAbsolutePath());
		} else if (arguments.size() == 2) {
			// create stream
			String streamName = arguments.get(1);
			StreamRepository repo = readRepository();
			streamService.createStream(repo, streamName);
		} else {
			throw new DisplayException("too many arguments");
		}
	}

	private void cmdSnapshot() {
		// TODO Auto-generated method stub

	}

	private StreamRepository readRepository() {
		File path = repositoryLocation;
		if (path == null) {
			path = new File(".");
		}
		return streamRepositoryService.readRepository(path.toPath());
	}
}
