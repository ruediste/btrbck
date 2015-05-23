package com.github.ruediste1.btrbck.dom;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

import com.github.ruediste1.btrbck.test.TestBase;

public class VersionHistoryXmlTest extends TestBase {

    @Inject
    JAXBContext ctx;
    private VersionHistory history;

    @Before
    public void setup() {
        history = new VersionHistory();

    }

    @Test
    public void testEmpty() throws JAXBException {
        checkRoundTrip(history);
    }

    @Test
    public void testSnapshotOnly() throws JAXBException {
        history.addVersion(UUID.randomUUID());
        checkRoundTrip(history);
    }

    @Test
    public void testRestoreOnly() throws JAXBException {
        history.addRestore(UUID.randomUUID(), 4);
        checkRoundTrip(history);
    }

    @Test
    public void testMixedOnly() throws JAXBException {
        history.addVersion(UUID.randomUUID());
        history.addRestore(UUID.randomUUID(), 4);
        checkRoundTrip(history);
    }

    private void checkRoundTrip(VersionHistory original) throws JAXBException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ctx.createMarshaller().marshal(original, out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        VersionHistory clone = (VersionHistory) ctx.createUnmarshaller()
                .unmarshal(in);
        assertThat(clone, equalTo(original));
    }
}
