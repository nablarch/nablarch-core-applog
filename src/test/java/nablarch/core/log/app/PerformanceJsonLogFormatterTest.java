package nablarch.core.log.app;

import nablarch.core.log.LogTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

/**
 * {@link PerformanceJsonLogFormatter}のテストクラス。
 * @author Shuji Kitamura
 */
public class PerformanceJsonLogFormatterTest extends LogTestSupport {

    @Before
    public void setup() {
        System.clearProperty("performanceLogFormatter.targets");
    }

    @After
    public void teardown() {
        System.clearProperty("performanceLogFormatter.targets");
    }

    /**
     * デフォルトの出力項目で正しくフォーマットされること。
     */
    @Test
    public void testFormat() {

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";

        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("point", "point0001")),
                withJsonPath("$", hasEntry("result", "success")),
                withJsonPath("$", hasKey("startTime")),
                withJsonPath("$", hasKey("endTime")),
                withJsonPath("$", hasKey("executionTime")),
                withJsonPath("$", hasKey("maxMemory")),
                withJsonPath("$", hasKey("startFreeMemory")),
                withJsonPath("$", hasKey("endFreeMemory")),
                withJsonPath("$", hasKey("startUsedMemory")),
                withJsonPath("$", hasKey("endUsedMemory")))));
    }

    /**
     * 指定の出力項目で正しくフォーマットされること。
     */
    @Test
    public void testFormatWithTargets() {
        System.setProperty("performanceLogFormatter.targets", "point ,, point");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";

        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("point", "point0001")),
                withoutJsonPath("$.result"),
                withoutJsonPath("$.startTime"),
                withoutJsonPath("$.endTime"),
                withoutJsonPath("$.executionTime"),
                withoutJsonPath("$.maxMemory"),
                withoutJsonPath("$.startFreeMemory"),
                withoutJsonPath("$.endFreeMemory"),
                withoutJsonPath("$.startUsedMemory"),
                withoutJsonPath("$.endUsedMemory"))));
    }

    /**
     * 不正なターゲットのテスト。
     */
    @Test
    public void testIllegalTargets() {
        System.setProperty("performanceLogFormatter.targets", "executionTime,dummy");

        Exception e = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                new PerformanceJsonLogFormatter();
            }
        });

        assertThat(e.getMessage(), is("[dummy] is unknown target. property name = [performanceLogFormatter.targets]"));
    }
}
