package com.github.ruediste1.btrbck.test;

import org.junit.Before;

import com.github.ruediste1.btrbck.GuiceModule;
import com.github.ruediste1.btrbck.Util;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestBase {

	@Before
	final public void setUpTestBase() {
		Injector injector = Guice.createInjector(new GuiceModule());
		Util.setInjector(injector);
		Util.injectMembers(this);
	}
}
