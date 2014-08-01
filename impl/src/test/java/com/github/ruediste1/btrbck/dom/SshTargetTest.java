package com.github.ruediste1.btrbck.dom;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SshTargetTest {

	@Test
	public void testParseToString() throws Exception {
		SshTarget target = SshTarget.parse("testHost");
		assertEquals("testHost", target.getHost());
		assertEquals(null, target.getPort());
		assertEquals(null, target.getUser());
		assertEquals("testHost", target.toString());

		target = SshTarget.parse("foo@testHost");
		assertEquals("testHost", target.getHost());
		assertEquals(null, target.getPort());
		assertEquals("foo", target.getUser());
		assertEquals("foo@testHost", target.toString());

		target = SshTarget.parse("testHost:5000");
		assertEquals("testHost", target.getHost());
		assertEquals(5000, (int) target.getPort());
		assertEquals(null, target.getUser());
		assertEquals("testHost:5000", target.toString());

		target = SshTarget.parse("foo@testHost:5000");
		assertEquals("testHost", target.getHost());
		assertEquals(5000, (int) target.getPort());
		assertEquals("foo", target.getUser());
		assertEquals("foo@testHost:5000", target.toString());
	}

}
