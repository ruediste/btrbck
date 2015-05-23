package com.github.ruediste1.btrbck;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.github.ruediste1.btrbck.SyncService.SendFileSpec;
import com.github.ruediste1.btrbck.dom.Snapshot;
import com.github.ruediste1.btrbck.dom.VersionHistory;
import com.github.ruediste1.btrbck.dto.StreamState.SnapshotEntry;

public class SyncServiceTest {

    private SyncService service;

    @Before
    public void setup() {

        service = new SyncService();
    }

    @Test
    public void testCalculateMissingSnapshots() throws Exception {

        ArrayList<Snapshot> snapshots = new ArrayList<>();
        snapshots.add(new Snapshot(1, null, null));
        Snapshot snapshot3 = new Snapshot(3, null, null);
        snapshots.add(snapshot3);

        Set<SnapshotEntry> snapshotNumbers = new HashSet<>();
        snapshotNumbers.add(new SnapshotEntry(1, null));
        snapshotNumbers.add(new SnapshotEntry(2, null));

        List<Snapshot> missing = service.calculateMissingSnapshots(snapshots,
                snapshotNumbers);

        assertThat(missing, hasSize(1));
        assertThat(missing, hasItem(snapshot3));
    }

    @Test
    public void testCalculateNextSnapshotNr() throws Exception {
        Snapshot snapshot = new Snapshot(3, null, null);
        TreeSet<Integer> available = new TreeSet<>();
        available.add(2);
        assertThat(service.calculateNextSnapshotNr(snapshot, available),
                is(nullValue()));
        available.add(3);
        assertThat(service.calculateNextSnapshotNr(snapshot, available),
                is(nullValue()));
        available.add(4);
        assertThat(service.calculateNextSnapshotNr(snapshot, available), is(4));
    }

    @Test
    public void testCalculateAncestorNrs() throws Exception {
        Snapshot snapshot = new Snapshot(3, null, null);
        VersionHistory versionHistory = new VersionHistory();
        UUID id = UUID.randomUUID();
        TreeSet<Integer> available = new TreeSet<>();

        versionHistory.addVersion(id);
        versionHistory.addVersion(id);
        versionHistory.addRestore(id, 0);
        versionHistory.addVersion(id);
        available.add(0);
        available.add(2);

        snapshot.nr = 0;
        Set<Integer> ancestors = service.calculateAncestorNrs(snapshot,
                versionHistory, available);
        assertThat(ancestors, empty());

        snapshot.nr = 1;
        ancestors = service.calculateAncestorNrs(snapshot, versionHistory,
                available);
        assertThat(ancestors, hasSize(1));
        assertThat(ancestors, hasItem(0));

        snapshot.nr = 2;
        ancestors = service.calculateAncestorNrs(snapshot, versionHistory,
                available);
        assertThat(ancestors, hasSize(1));
        assertThat(ancestors, hasItem(0));
    }

    @Test
    public void testDetermineSendFiles() {
        VersionHistory versionHistory = new VersionHistory();
        UUID id = UUID.randomUUID();
        Set<SnapshotEntry> targetSnapshots = new HashSet<>();
        TreeMap<Integer, Snapshot> sourceSnapshots = new TreeMap<>();

        // snapshot 0 only persent in source
        versionHistory.addVersion(id);
        sourceSnapshots.put(0, new Snapshot(0, null, null));

        // snapshot 1 only persent in target
        versionHistory.addVersion(id);
        targetSnapshots.add(new SnapshotEntry(1, null));

        // snapshot 2 present in both
        versionHistory.addVersion(id);
        sourceSnapshots.put(2, new Snapshot(2, null, null));
        targetSnapshots.add(new SnapshotEntry(2, null));

        List<SendFileSpec> specs = service.determineSendFiles(null,
                sourceSnapshots, versionHistory, targetSnapshots);
        assertThat(specs, hasSize(1));
        assertThat(specs.get(0).target.nr, is(0));
        assertThat(specs.get(0).cloneSources, hasSize(1));
        assertThat(specs.get(0).cloneSources.iterator().next().nr, is(2));
    }
}
