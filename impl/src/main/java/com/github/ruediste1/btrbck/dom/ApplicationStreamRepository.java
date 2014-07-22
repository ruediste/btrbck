package com.github.ruediste1.btrbck.dom;

import java.nio.file.Path;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ApplicationStreamRepository extends StreamRepository {

	@Override
	public Path getBaseDirectory() {
		return rootDirectory.resolve(".backup");
	}

	public Path getWorkingDirectory(Stream stream) {
		return rootDirectory.resolve(stream.name);
	}
}
