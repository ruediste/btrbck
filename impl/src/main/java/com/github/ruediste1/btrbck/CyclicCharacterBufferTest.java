package com.github.ruediste1.btrbck;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class CyclicCharacterBufferTest {

	CyclicCharacterBuffer buf;

	@Before
	public void stup() {
		buf = new CyclicCharacterBuffer(5);
	}

	@Test
	public void testSimple() {
		buf.append("abc");
		assertEquals("abc", buf.getTail());
	}

	@Test
	public void testSimpleWithCharsLength() {
		buf.append("abcdef".toCharArray(), 3);
		assertEquals("abc", buf.getTail());
	}

	@Test
	public void testAddEmpty() {
		buf.append("");
		assertEquals("", buf.getTail());
	}

	@Test
	public void testFillBuffer() {
		buf.append("abcde");
		assertEquals("abcde", buf.getTail());
	}

	@Test
	public void testOverflow() {
		buf.append("abcdef");
		assertEquals("bcdef", buf.getTail());
	}

	@Test
	public void testAddThenOverflow() {
		buf.append("abc");
		buf.append("def");
		assertEquals("bcdef", buf.getTail());
	}

	public void testAddThenOverflowJustFill() {
		buf.append("abc");
		buf.append("defghij");
		assertEquals("fghij", buf.getTail());
	}
}
