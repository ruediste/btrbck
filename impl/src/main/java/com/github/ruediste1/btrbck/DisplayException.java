package com.github.ruediste1.btrbck;

/**
 * An error with a message which can safely be shown to the user
 */
public class DisplayException extends RuntimeException {

	public DisplayException(String message) {
		super(message);
	}
}
