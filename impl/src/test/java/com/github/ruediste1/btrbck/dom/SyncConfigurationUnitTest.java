package com.github.ruediste1.btrbck.dom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class SyncConfigurationUnitTest {

    @Test
    public void testIsSynced() throws Exception {
        SyncConfiguration config = new SyncConfiguration();
        config.streamPatterns = "";
        assertThat(config.isSynced("foo"), is(false));

        config.streamPatterns = "foo";
        assertThat(config.isSynced("foo"), is(true));
        assertThat(config.isSynced("bar"), is(false));

        config.streamPatterns = "f*o";
        assertThat(config.isSynced("foo"), is(true));
        assertThat(config.isSynced("bar"), is(false));

        config.streamPatterns = "-bar, foo";
        assertThat(config.isSynced("foo"), is(true));
        assertThat(config.isSynced("bar"), is(false));
    }

}
