package com.github.ruediste1.btrbck;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.inject.Injector;

public class Util {
	private static Injector injector;

	public static void initializeLockFile(Path lockFile) throws IOException {
		FileOutputStream os = new FileOutputStream(lockFile.toFile());
		os.write("lockfile".getBytes("UTF-8"));
		os.close();
	}

	public static void setInjector(Injector injector) {
		Util.injector = injector;
	}

	public static <T> T get(Class<T> cls) {
		return injector.getInstance(cls);
	}

	public static void injectMembers(Object obj) {
		injector.injectMembers(obj);
	}

	public static void removeRecursive(Path path) {
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file,
						IOException exc) throws IOException {
					// try to delete the file anyway, even if its attributes
					// could not be read, since delete-only access is
					// theoretically possible
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir,
						IOException exc) throws IOException {
					if (exc == null) {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					} else {
						// directory iteration failed; propagate exception
						throw exc;
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("error while deleting directory "
					+ path.toAbsolutePath(), e);

		}
	}

	public static Set<String> getDirectoryNames(Path baseDirectory) {
		LinkedHashSet<String> result = new LinkedHashSet<>();
		try {
			for (Path p : Files.newDirectoryStream(baseDirectory)) {
				if (Files.isDirectory(p)) {
					String name = p.getFileName().toString();
					if (!result.add(name)) {
						throw new RuntimeException("duplicate directory: "
								+ name);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while reading directories", e);
		}
		return result;
	}
}
