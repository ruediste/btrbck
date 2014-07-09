package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;

public class BackupStreamRepository extends StreamRepository {

	@Override
	public Path getBaseDirectory() {
		return rootDirectory;
	}

}
