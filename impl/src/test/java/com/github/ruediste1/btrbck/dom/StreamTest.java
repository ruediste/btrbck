package com.github.ruediste1.btrbck.dom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.joda.time.Period;
import org.junit.Test;

import com.github.ruediste1.btrbck.test.TestBase;

public class StreamTest extends TestBase {

    @Inject
    JAXBContext ctx;

    @Test
    public void testXmlSerialization() throws JAXBException {
        Stream stream = new Stream();
        stream.initialRetentionPeriod = Period.days(3);
        {
            Retention retention = new Retention();
            retention.period = Period.days(2);
            retention.timeUnit = TimeUnit.HOUR;
            retention.snapshotsPerTimeUnit = 4;
            stream.retentions.add(retention);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ctx.createMarshaller().marshal(stream, out);
        Stream readStream = (Stream) ctx.createUnmarshaller().unmarshal(
                new ByteArrayInputStream(out.toByteArray()));

        assertThat(readStream.initialRetentionPeriod, is(Period.days(3)));

        assertThat(readStream.retentions.size(), is(1));
        Retention retention = readStream.retentions.get(0);
        assertThat(retention.period, is(Period.days(2)));
        assertThat(retention.timeUnit, is(TimeUnit.HOUR));
        assertThat(retention.snapshotsPerTimeUnit, is(4));

    }
}
