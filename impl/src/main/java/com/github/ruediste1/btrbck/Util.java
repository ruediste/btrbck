package com.github.ruediste1.btrbck;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

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
}
