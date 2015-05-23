package com.github.ruediste1.btrbck.dom;

import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;

/**
 * Entry in a {@link VersionHistory} representing that a certain snapshot has
 * been restored. This is used to find optimal clone sources when transmitting
 * incremental snapshots.
 */
@XmlRootElement(name = "restore")
public class RestoreVersionHistoryEntry extends VersionHistoryEntry {
    private static final long serialVersionUID = 1L;

    @XmlAttribute
    public int restoredSnapshotNr;

    public RestoreVersionHistoryEntry() {

    }

    public RestoreVersionHistoryEntry(UUID streamId) {
        super(streamId);
    }

    @Override
    public int getRepresentedSnapshotCount() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        RestoreVersionHistoryEntry other = (RestoreVersionHistoryEntry) obj;
        return streamId.equals(other.streamId)
                && restoredSnapshotNr == other.restoredSnapshotNr;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(streamId, restoredSnapshotNr);
    }

    @Override
    public String toString() {
        return "(Restore: " + streamId + " restored: " + restoredSnapshotNr
                + ")";
    }
}
