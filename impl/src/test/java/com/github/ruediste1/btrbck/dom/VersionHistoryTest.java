package com.github.ruediste1.btrbck.dom;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.TreeMap;
import java.util.UUID;

import org.junit.Test;

import com.github.ruediste1.btrbck.dom.VersionHistory.HistoryNode;

public class VersionHistoryTest {

	@Test
	public void testEquals() throws Exception {
		VersionHistory history1 = new VersionHistory();
		VersionHistory history2 = new VersionHistory();
		UUID id = UUID.randomUUID();

		assertEquals(history1, history2);
		history1.addVersion(id);

		assertNotEquals(history1, history2);

		history2.addVersion(id);
		assertEquals(history1, history2);

		history1.addVersion(id);
		id = UUID.randomUUID();
		history2.addVersion(id);

		assertNotEquals(history1, history2);
	}

	@Test
	public void testAddVersion() throws Exception {
		VersionHistory history = new VersionHistory();
		UUID id = UUID.randomUUID();

		assertTrue(history.entries.isEmpty());
		assertTrue(history.getVersionCount() == 0);

		history.addVersion(id);

		assertTrue(history.entries.size() == 1);
		assertTrue(history.entries.get(0).streamId == id);
		assertTrue(history.getVersionCount() == 1);

		history.addVersion(id);

		assertTrue(history.entries.size() == 1);
		assertTrue(history.entries.get(0).streamId == id);
		assertTrue(history.getVersionCount() == 2);

		id = UUID.randomUUID();

		history.addVersion(id);

		assertTrue(history.entries.size() == 2);
		assertTrue(history.entries.get(1).streamId == id);
		assertTrue(history.getVersionCount() == 3);

	}

	@Test
	public void testCalculateNodesSimple() throws Exception {
		VersionHistory history = new VersionHistory();
		UUID id = UUID.randomUUID();

		history.addVersion(id);
		history.addVersion(id);
		history.addVersion(id);

		TreeMap<Integer, HistoryNode> nodes = history.calculateNodes();
		assertThat(nodes.entrySet(), hasSize(3));
	}

	@Test
	public void testCalculateNodes() throws Exception {
		VersionHistory history = new VersionHistory();
		UUID id = UUID.randomUUID();

		history.addVersion(id);
		history.addRestore(id, 0);
		history.addVersion(id);
		history.addRestore(id, 0);
		history.addRestore(id, 1);
		history.addVersion(id);
		history.addVersion(id);

		TreeMap<Integer, HistoryNode> nodes = history.calculateNodes();

		assertThat(nodes.entrySet(), hasSize(4));

		assertThat(nodes.get(0).parents, is(empty()));
		assertThat(nodes.get(0).snapshotNr, is(0));
		assertThat(nodes.get(1).parents, hasSize(1));
		assertThat(nodes.get(1).parents, hasItem(nodes.get(0)));
		assertThat(nodes.get(2).parents, hasSize(2));
		assertThat(nodes.get(2).parents, hasItem(nodes.get(0)));
		assertThat(nodes.get(2).parents, hasItem(nodes.get(1)));
		assertThat(nodes.get(3).parents, hasSize(1));
		assertThat(nodes.get(3).parents, hasItem(nodes.get(2)));
		assertThat(nodes.get(3).snapshotNr, is(3));

	}

	@Test
	public void testIsAncestorOf() throws Exception {
		VersionHistory h1 = new VersionHistory();
		VersionHistory h2 = new VersionHistory();
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();

		assertTrue(h1.isAncestorOf(h2));
		assertTrue(h2.isAncestorOf(h1));

		h2.addVersion(id1);
		h2.addVersion(id1);
		assertTrue(h1.isAncestorOf(h2));
		assertFalse(h2.isAncestorOf(h1));

		h1.addVersion(id1);
		h1.addVersion(id1);
		assertTrue(h1.isAncestorOf(h2));
		assertTrue(h2.isAncestorOf(h1));

		h2.addVersion(id1);
		assertTrue(h1.isAncestorOf(h2));
		assertFalse(h2.isAncestorOf(h1));

		h1.addVersion(id2);
		h1.addVersion(id2);
		assertFalse(h1.isAncestorOf(h2));
		assertFalse(h2.isAncestorOf(h1));

	}
}
