package com.github.ruediste1.btrbck.dto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains the information sent to the source in order to determine the clone
 * sources for the synchronization
 */
public class StreamState implements Serializable {

	private static final long serialVersionUID = 1L;

	public boolean isNewStream;
	public final Set<Integer> availableSnapshotNumbers = new HashSet<>();
}
