package com.github.ruediste1.btrbck.dom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.junit.Test;

import com.github.ruediste1.btrbck.test.TestBase;

public class StreamRepositoryTest extends TestBase {

	@Inject
	JAXBContext ctx;

	@Test
	public void testXmlSerialization() throws JAXBException {
		ApplicationStreamRepository repo = new ApplicationStreamRepository();

		{
			SyncConfiguration syncConfig = new SyncConfiguration();
			syncConfig.direction = SyncDirection.PULL;
			syncConfig.sshTarget = "foo";
			syncConfig.remoteRepoLocation = "bar";
			syncConfig.streamPatterns = "foobar";
			repo.syncConfigurations.add(syncConfig);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ctx.createMarshaller().marshal(repo, out);
		ApplicationStreamRepository readRepo = (ApplicationStreamRepository) ctx
				.createUnmarshaller().unmarshal(
						new ByteArrayInputStream(out.toByteArray()));

		assertThat(readRepo.syncConfigurations.size(), is(1));
		SyncConfiguration syncConfig = readRepo.syncConfigurations.get(0);
		assertThat(syncConfig.direction, is(SyncDirection.PULL));
		assertThat(syncConfig.sshTarget, is("foo"));
		assertThat(syncConfig.remoteRepoLocation, is("bar"));
		assertThat(syncConfig.streamPatterns, is("foobar"));
	}
}