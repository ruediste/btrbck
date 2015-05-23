package com.github.ruediste1.btrbck;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

public class BlockTransferServiceTest {

    @Test
    public void test() throws IOException, ClassNotFoundException {
        BlockTransferService service = new BlockTransferService();
        String in = "Hello World, I like it!";

        ByteArrayInputStream inStream = new ByteArrayInputStream(
                in.getBytes("UTF-8"));

        ByteArrayOutputStream outStream1 = new ByteArrayOutputStream();
        ByteArrayOutputStream outStream2 = new ByteArrayOutputStream();

        service.sendBlocks(inStream, outStream1, 3);
        service.readBlocks(new ByteArrayInputStream(outStream1.toByteArray()),
                outStream2);

        assertEquals(in, outStream2.toString("UTF-8"));
    }
}
