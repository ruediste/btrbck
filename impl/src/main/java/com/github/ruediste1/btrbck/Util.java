package com.github.ruediste1.btrbck;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.io.ByteStreams;
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

	public static void send(String s, OutputStream output)
			throws UnsupportedEncodingException, IOException {
		ByteStreams.copy(new ByteArrayInputStream(s.getBytes("UTF-8")), output);
		output.flush();
	}

	/**
	 * Waits until a certain idicator is seen on the reader
	 */
	public static void waitFor(String indicator, InputStream input)
			throws IOException {
		InputStreamReader reader = new InputStreamReader(input, "UTF-8");
		char[] buf = new char[32];
		CyclicCharacterBuffer cbuf = new CyclicCharacterBuffer(
				indicator.length());
		while (true) {
			int count;
			count = reader.read(buf);
			if (count < 0) {
				break;
			}
			cbuf.append(buf, count);
			if (indicator.equals(cbuf.getTail())) {
				break;
			}
		}
	}

	public static void send(Object obj, OutputStream output) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(output);
		out.writeObject(obj);
		out.flush();
	}

	@SuppressWarnings("unchecked")
	public static <T> T read(Class<T> cls, InputStream input)
			throws ClassNotFoundException, IOException {
		ObjectInputStream in = new ObjectInputStream(input);
		return (T) in.readObject();
	}

	public static String readFully(InputStream input) throws IOException {
		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		ByteStreams.copy(input, tmp);
		return tmp.toString("UTF-8");
	}
}
