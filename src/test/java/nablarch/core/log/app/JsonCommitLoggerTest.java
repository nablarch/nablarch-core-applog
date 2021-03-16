package nablarch.core.log.app;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link nablarch.core.log.app.JsonCommitLogger}のテストクラス。
 * @author Shuji Kitamura
 */
public class JsonCommitLoggerTest {

    /**
     * {@link JsonCommitLogger#formatForIncrement(long)}のテスト。
     */
    @Test
    public void testFormatForIncrement() {

        JsonCommitLogger logger = new JsonCommitLogger();

        assertThat(logger.formatForIncrement(5), is("$JSON${\"commitCount\":5}"));
    }

    /**
     * {@link JsonCommitLogger#formatForTerminate(long)}のテスト。
     */
    @Test
    public void testFormatForTerminate() {

        JsonCommitLogger logger = new JsonCommitLogger();

        assertThat(logger.formatForTerminate(10), is("$JSON${\"totalCommitCount\":10}"));
    }
}
