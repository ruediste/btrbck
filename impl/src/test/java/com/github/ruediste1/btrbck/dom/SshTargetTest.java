package com.github.ruediste1.btrbck.dom;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SshTargetTest {

	@Test
	public void testParse() throws Exception {
		SshTarget target = SshTarget.parse("testHost");
		assertEquals("testHost", target.getHost());
		assertEquals(null, target.getPort());
		assertEquals(null, target.getUser());

		target = SshTarget.parse("foo@testHost");
		assertEquals("testHost", target.getHost());
		assertEquals(null, target.getPort());
		assertEquals("foo", target.getUser());

		target = SshTarget.parse("testHost:5000");
		assertEquals("testHost", target.getHost());
		assertEquals(5000, (int) target.getPort());
		assertEquals(null, target.getUser());

		target = SshTarget.parse("foo@testHost:5000");
		assertEquals("testHost", target.getHost());
		assertEquals(5000, (int) target.getPort());
		assertEquals("foo", target.getUser());
	}

}
