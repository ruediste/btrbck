package com.github.ruediste1.btrbck.dom;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

/**
 * Describes the target of an ssh connection
 */
public class SshTarget {
	private Integer port;
	private String host;
	private String user;

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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!Strings.isNullOrEmpty(user)) {
			sb.append(user);
			sb.append("@");
		}
		sb.append(host);
		if (port != null) {
			sb.append(":");
			sb.append(port);
		}
		return sb.toString();
	}

	private SshTarget(SshTarget other) {
		host = other.host;
		port = other.port;
		user = other.user;
	}

	public Integer getPort() {
		return port;
	}

	public SshTarget withPort(Integer port) {
		SshTarget result = new SshTarget(this);
		result.port = port;
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

	public String getUser() {
		return user;
	}

	public SshTarget withUser(String user) {
		SshTarget result = new SshTarget(this);
		result.user = user;
		return result;
	}
}