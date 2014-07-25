package com.github.ruediste1.btrbck.dto;

import java.io.Serializable;

import com.github.ruediste1.btrbck.dom.VersionHistory;

public class SendFileListHeader implements Serializable {

	private static final long serialVersionUID = 1L;

	public VersionHistory targetVersionHistory;
	public byte[] streamConfiguration;
	public int count;
}
