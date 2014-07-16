package com.github.ruediste1.btrbck;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.inject.Singleton;

import com.github.ruediste1.btrbck.dto.Block;
import com.google.common.io.ByteStreams;

@Singleton
public class BlockTransferService {

	/**
	 * Read blocks from the input and send the data to the output
	 */
	public void readBlocks(InputStream input, OutputStream output)
			throws IOException, ClassNotFoundException {
		boolean isFirst = true;
		while (true) {

			Block block = Util.read(Block.class, input);
			if (!isFirst && block.isFirst) {
				throw new RuntimeException("Unexpected block marked as first");
			}
			ByteStreams.copy(new ByteArrayInputStream(block.data), output);
			if (block.isLast) {
				break;
			}
		}
	}

	/**
	 * Read data from the input and send it as blocks to the output
	 */
	public void sendBlocks(InputStream input, OutputStream output, int blocksize)
			throws IOException {

		Block block = new Block();
		block.isFirst = true;
		block.isLast = false;
		byte[] buf = new byte[blocksize];

		while (true) {
			int result = input.read(buf);
			if (result < 0) {
				break;
			}
			block.data = Arrays.copyOf(buf, result);
			Util.send(block, output);
			block.isFirst = false;
		}
		block.isLast = true;
		block.data = new byte[] {};
		Util.send(block, output);
		// out.writeObject(block);
		// out.reset();
		// out.flush();
	}
}
