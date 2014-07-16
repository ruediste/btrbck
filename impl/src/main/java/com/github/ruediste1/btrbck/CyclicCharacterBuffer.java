package com.github.ruediste1.btrbck;

public class CyclicCharacterBuffer {

	boolean filled;
	private int pos;
	private char[] buffer;

	public CyclicCharacterBuffer(int size) {
		buffer = new char[size];
	}

	public String getTail() {
		StringBuilder sb = new StringBuilder();
		if (filled) {
			sb.append(buffer, pos, buffer.length - pos);
		}
		sb.append(buffer, 0, pos);
		return sb.toString();
	}

	public void append(char[] chars, int charsLength) {
		int countToAdd = charsLength;
		if (countToAdd > buffer.length) {
			countToAdd = buffer.length;
		}
		int charsOffset = chars.length - countToAdd;

		// add the part from the current position to the end of the buffer
		int firstCount = countToAdd;
		int remaining = buffer.length - pos;
		if (firstCount > remaining) {
			firstCount = buffer.length - pos;
		}
		System.arraycopy(chars, charsOffset, buffer, pos, firstCount);
		pos += firstCount;

		// add the remaining characters
		if (countToAdd > firstCount) {
			remaining = countToAdd - firstCount;
			System.arraycopy(chars, charsOffset + firstCount, buffer, 0,
					remaining);
			pos = remaining;
			filled = true;
		}
	}

	public void append(String string) {
		append(string.toCharArray());
	}

	public void append(char[] buf) {
		append(buf, buf.length);
	}
}
