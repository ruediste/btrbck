package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockManager {
	private static final Logger log = LoggerFactory
			.getLogger(LockManager.class);

	private boolean failOnOpenLocks;

	public interface Lock {
		void release();
	}

	public Lock getLock(Path lockFile, boolean shared) throws IOException {
		final FileChannel f = FileChannel.open(lockFile,
				StandardOpenOption.WRITE);
		final FileLock fileLock = f.lock(0L, Long.MAX_VALUE, shared);

		return new Lock() {

			@Override
			public void release() {
				try {
					fileLock.release();
					f.close();
				} catch (IOException e) {
					throw new RuntimeException("error while releasing lock", e);
				}
			}

		};
	}

	private final ThreadLocal<Set<Lock>> locks = new ThreadLocal<>();

	public void failOnOpenLocks() {
		failOnOpenLocks = true;
	}

	public void enterLocks() {
		Set<Lock> lockSet = locks.get();
		if (lockSet == null) {
			return;
		}
		locks.set(new HashSet<Lock>());
	}

	public void leaveLocks() {
		Set<Lock> lockSet = locks.get();
		locks.set(null);
		if (!lockSet.isEmpty()) {
			releaseLocks(lockSet);

			if (failOnOpenLocks) {
				throw new RuntimeException("open locks detected");
			} else {
				log.error("Open locks detected");
			}
		}
	}

	private void releaseLocks(Set<Lock> lockSet) {
		// release open locks
		for (Lock l : lockSet) {
			try {
				l.release();
			} catch (Throwable t) {
				log.error("Error while releasing lock");
			}
		}
	}

	public void runWithLocks(Runnable runnable) {
		Set<Lock> oldLocks = locks.get();
		if (oldLocks != null) {
			runnable.run();
		} else {
			HashSet<Lock> lockSet = new HashSet<Lock>();
			try {
				locks.set(lockSet);
				runnable.run();
			} catch (Throwable t) {
				releaseLocks(lockSet);
			} finally {
				leaveLocks();
			}
		}
	}

}
