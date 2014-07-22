package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BackupStreamRepository extends StreamRepository {

	@Override
	public Path getBaseDirectory() {
		return rootDirectory;
	}

}
