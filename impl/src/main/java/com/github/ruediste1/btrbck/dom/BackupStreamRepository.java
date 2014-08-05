package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a {@link Stream} repository without working directory, suitable
 * for use on a backup server.
 */
@XmlRootElement
public class BackupStreamRepository extends StreamRepository {

	@Override
	public Path getBaseDirectory() {
		return rootDirectory;
	}

}
