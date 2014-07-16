package com.github.ruediste1.btrbck.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SshTarget {
	private Integer port;
	private String keyFile;
	private String host;
	private List<String> parameters = new ArrayList<>();

	public SshTarget() {
	}

	public SshTarget(String host) {
		this.host = host;
	}

	private SshTarget(SshTarget other) {
		host = other.host;
		port = other.port;
		keyFile = other.keyFile;
		parameters.addAll(other.parameters);
	}

	public Integer getPort() {
		return port;
	}

	public SshTarget withPort(Integer port) {
		SshTarget result = new SshTarget(this);
		result.port = port;
		return result;
	}

	public String getKeyFile() {
		return keyFile;
	}

	public SshTarget withKeyFile(String keyFile) {
		SshTarget result = new SshTarget(this);
		result.keyFile = keyFile;
		return result;
	}

	public String getHost() {
		return host;
	}

	public SshTarget withHost(String host) {
		SshTarget result = new SshTarget(this);
		result.host = host;
		return result;
	}

	public List<String> getParameters() {
		return Collections.unmodifiableList(parameters);
	}

	public SshTarget withParameter(String parameter) {
		SshTarget result = new SshTarget(this);
		result.parameters.add(parameter);
		return result;

	}
}