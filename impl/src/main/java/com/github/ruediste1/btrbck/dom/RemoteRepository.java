package com.github.ruediste1.btrbck.dom;

import com.github.ruediste1.btrbck.SnapshotTransferService;

/**
 * Defines a remote {@link StreamRepository}, used for
 * {@link SnapshotTransferService#pull(StreamRepository, String, RemoteRepository, String, boolean)
 * pulling} and
 * {@link SnapshotTransferService#push(Stream, RemoteRepository, String, boolean)
 * pushing} {@link Snapshot}s
 */
public class RemoteRepository {
    public SshTarget sshTarget;
    public String location;
}
