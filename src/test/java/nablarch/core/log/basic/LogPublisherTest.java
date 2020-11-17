package nablarch.core.log.basic;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * {@link LogPublisher}の単体テスト。
 * @author Tanaka Tomoyuki
 */
public class LogPublisherTest {

    @Test
    public void testWrite() {
        TestLogListener listener1 = new TestLogListener();
        TestLogListener listener2 = new TestLogListener();
        LogPublisher.addListener(listener1);
        LogPublisher.addListener(listener2);

        LogPublisher sut = new LogPublisher();
        LogContext logContext = new LogContext("loggerName", LogLevel.INFO, "message", null);
        sut.write(logContext);

        assertThat(listener1.logContext, is(sameInstance(logContext)));
        assertThat(listener2.logContext, is(sameInstance(logContext)));
    }

    @Test
    public void testRemoveListener() {
        TestLogListener listener1 = new TestLogListener();
        TestLogListener listener2 = new TestLogListener();
        LogPublisher.addListener(listener1);
        LogPublisher.addListener(listener2);

        LogPublisher.removeListener(listener1);

        LogPublisher sut = new LogPublisher();
        LogContext logContext = new LogContext("loggerName", LogLevel.INFO, "message", null);
        sut.write(logContext);

        assertThat(listener1.logContext, is(nullValue()));
        assertThat(listener2.logContext, is(sameInstance(logContext)));
    }

    @Test
    public void testNoopMethods() {
        // 何もしないメソッドを実行しても何も起こらないことのテスト（テストの意味は薄いが、カバレッジを通すために実施している）
        TestLogListener listener = new TestLogListener();
        LogPublisher.addListener(listener);

        LogPublisher sut = new LogPublisher();
        sut.initialize(null);
        sut.terminate();

        assertThat(listener.logContext, is(nullValue()));
    }

    @After
    public void tearDown() {
        LogPublisher.removeAllListeners();
    }

    private static class TestLogListener implements LogListener {
        private LogContext logContext;

        @Override
        public void onWritten(LogContext context) {
            this.logContext = context;
        }
    }
}