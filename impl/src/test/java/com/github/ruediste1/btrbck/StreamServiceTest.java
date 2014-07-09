package com.github.ruediste1.btrbck;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import com.github.ruediste1.btrbck.dom.ApplicationStreamRepository;
import com.github.ruediste1.btrbck.dom.Stream;
import com.github.ruediste1.btrbck.dom.StreamRepository;
import com.github.ruediste1.btrbck.test.TestBase;

public class StreamServiceTest extends TestBase {

	@Inject
	StreamService service;

	@Inject
	StreamRepositoryService repositoryService;

	StreamRepository repository;

	@Before
	public void setUp() throws IOException {
		repository = new ApplicationStreamRepository();
		repository.rootDirectory = Files.createTempDirectory("btrbck");
		repositoryService.initializeRepository(repository);
	}

	@Test
	public void testCreateStream() throws IOException {
		Stream stream = new Stream();
		stream.name = "test";
		stream.streamRepository = repository;
		assertNull(service.getStream(repository, "test"));
		service.createStream(stream);
		assertNotNull(service.getStream(repository, "test"));
		assertNotNull(stream.id);
	}

}
