package com.github.ruediste1.btrbck.dom;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SshTarget {
	private Integer port;
	private File keyFile;
	private String host;
	private String user;
	private List<String> parameters = new ArrayList<>();

	public SshTarget() {
	}

	/**
	 * Create a new {@link SshTarget} by parsing the given string. the format is
	 * 
	 * <pre>
	 * {@code
	 * [<user>@]<host>[:<port>]
	 * }
	 * </pre>
	 */
	public static SshTarget parse(String s) {
		Pattern p = Pattern
				.compile("((?<user>[^@]*)@)?(?<host>[^:]+)(:(?<port>.*))?");
		Matcher matcher = p.matcher(s);
		if (!matcher.matches()) {
			return null;
		}
		SshTarget result = new SshTarget();
		result = result.withHost(matcher.group("host"));
		result = result.withUser(matcher.group("user"));
		String portString = matcher.group("port");
		if (portString != null) {
			result = result.withPort(Integer.parseInt(portString));
		}
		return result;
	}

	private SshTarget(SshTarget other) {
		host = other.host;
		port = other.port;
		keyFile = other.keyFile;
		user = other.user;
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

	public File getKeyFile() {
		return keyFile;
	}

	public SshTarget withKeyFile(File keyFile) {
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

	public String getUser() {
		return user;
	}

	public SshTarget withUser(String user) {
		SshTarget result = new SshTarget(this);
		result.user = user;
		return result;
	}
}