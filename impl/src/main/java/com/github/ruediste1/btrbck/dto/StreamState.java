package com.github.ruediste1.btrbck.dto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.github.ruediste1.btrbck.dom.Snapshot;
import com.google.common.base.Objects;

/**
 * Contains the information sent to the source in order to determine the clone
 * sources for the synchronization
 */
public class StreamState implements Serializable {

    private static final long serialVersionUID = 1L;

    public boolean isNewStream;

    public static class SnapshotEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        public int snapshotNr;
        public UUID senderStreamId;

        public SnapshotEntry(Snapshot sn) {
            snapshotNr = sn.nr;
            senderStreamId = sn.senderStreamId;
        }

        public SnapshotEntry(int snapshotNr, UUID senderStreamId) {
            super();
            this.snapshotNr = snapshotNr;
            this.senderStreamId = senderStreamId;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("snapshotNr", snapshotNr)
                    .add("senderStreamId", senderStreamId).toString();
        }
    }

    public final Set<SnapshotEntry> availableSnapshots = new HashSet<>();

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("isNewStream", isNewStream)
                .add("availableSnapshots", availableSnapshots).toString();
    }

    public Set<Integer> availableSnapshotNumbers() {
        HashSet<Integer> result = new HashSet<>();
        for (SnapshotEntry e : availableSnapshots) {
            result.add(e.snapshotNr);
        }
        return result;
    }
}
