package nablarch.core.log.app;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * {@link CompositeCommitLogger}の単体テスト。
 * @author Tanaka Tomoyuki
 */
public class CompositeCommitLoggerTest {
    private CompositeCommitLogger sut = new CompositeCommitLogger();
    private MockCommitLogger logger1 = new MockCommitLogger();
    private MockCommitLogger logger2 = new MockCommitLogger();
    private MockCommitLogger logger3 = new MockCommitLogger();

    @Before
    public void setup() {
        sut.setCommitLoggerList(Arrays.asList(logger1, logger2, logger3));
    }

    @Test
    public void testInitialize() {
        sut.initialize();

        assertThat(logger1.initialized, is(true));
        assertThat(logger2.initialized, is(true));
        assertThat(logger3.initialized, is(true));
    }

    @Test
    public void testIncrement() {
        sut.increment(99L);

        assertThat(logger1.count, is(99L));
        assertThat(logger2.count, is(99L));
        assertThat(logger3.count, is(99L));
    }

    @Test
    public void testTerminate() {
        sut.terminate();

        assertThat(logger1.terminated, is(true));
        assertThat(logger2.terminated, is(true));
        assertThat(logger3.terminated, is(true));
    }

    private static class MockCommitLogger implements CommitLogger {
        private boolean initialized;
        private long count;
        private boolean terminated;

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void increment(long count) {
            this.count = count;
        }

        @Override
        public void terminate() {
            terminated = true;
        }
    }
}