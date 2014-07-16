package com.github.ruediste1.btrbck.test;

import javax.inject.Inject;

import org.junit.Before;

import com.github.ruediste1.btrbck.BtrfsService;
import com.github.ruediste1.btrbck.GuiceModule;
import com.github.ruediste1.btrbck.Util;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Base class for unit tests. Sets up guice and injects the test class
 * 
 */
public class TestBase {

	@Inject
	BtrfsService btrfsService;

	@Before
	final public void setUpTestBase() {
		Injector injector = Guice.createInjector(new GuiceModule());
		Util.setInjector(injector);
		Util.injectMembers(this);
		btrfsService.useSudo = true;
	}
}
