package com.github.ruediste1.btrbck.dom;

import java.io.Serializable;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public abstract class VersionHistoryEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    @XmlAttribute
    public UUID streamId;

    public VersionHistoryEntry() {

    }

    public VersionHistoryEntry(UUID streamId) {
        this.streamId = streamId;
    }

    public abstract int getRepresentedSnapshotCount();
}
