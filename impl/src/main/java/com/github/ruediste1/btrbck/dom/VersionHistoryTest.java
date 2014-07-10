package com.github.ruediste1.btrbck.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

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
}
