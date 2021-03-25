package nablarch.core.log.app;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import nablarch.core.log.LogTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static mockit.internal.expectations.ActiveInvocations.minTimes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

/**
 * {@link PerformanceJsonLogFormatter}のテストクラス。
 * @author Shuji Kitamura
 */
public class PerformanceJsonLogFormatterTest extends LogTestSupport {

    @Mocked
    private PerformanceLogFormatter.PerformanceLogContext context;

    @Before
    public void setup() {
        System.clearProperty("performanceLogFormatter.targets");
    }

    /**
     * デフォルトの出力項目で正しくフォーマットされること。
     */
    @Test
    public void testFormat() {

        new Expectations() {{
            context.getPoint(); result = "point0001";
            context.getResult(); result = "success";
            context.getStartTime(); result = 100;
            context.getEndTime(); result = 300;
            context.getExecutionTime(); result = 200;
            context.getMaxMemory(); result = 1000000l;
            context.getStartFreeMemory(); result = 700000;
            context.getEndFreeMemory(); result = 670000;
            context.getStartUsedMemory(); result = 300000l;
            context.getEndUsedMemory(); result = 330000l;
        }};

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";

        formatter.start(point);
        String message = formatter.end(point, "success");

        new Verifications() {{
            context.setMaxMemory(anyLong); minTimes(1);
        }};

        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("point", "point0001")),
                withJsonPath("$", hasEntry("result", "success")),
                withJsonPath("$", hasEntry("startTime", 100)),
                withJsonPath("$", hasEntry("endTime", 300)),
                withJsonPath("$", hasEntry("executionTime", 200)),
                withJsonPath("$", hasEntry("maxMemory", 1000000)),
                withJsonPath("$", hasEntry("startFreeMemory", 700000)),
                withJsonPath("$", hasEntry("endFreeMemory", 670000)),
                withJsonPath("$", hasEntry("startUsedMemory", 300000)),
                withJsonPath("$", hasEntry("endUsedMemory", 330000)))));

        // note ログコンテキストをMockにしている為、setterの呼び出しを確認
        new Verifications() {{
            context.setPoint("point0001"); minTimes(1);
            context.setResult("success"); minTimes(1);
            context.setStartTime(anyLong); minTimes(1);
            context.setEndTime(anyLong); minTimes(1);
            context.setMaxMemory(anyLong); minTimes(1);
            context.setStartFreeMemory(anyLong); minTimes(1);
            context.setEndFreeMemory(anyLong); minTimes(1);
            context.setStartUsedMemory(anyLong); minTimes(1);
            context.setEndUsedMemory(anyLong); minTimes(1);
        }};
    }

    /**
     * 指定の出力項目で正しくフォーマットされること。
     */
    @Test
    public void testFormatWithTargets() {
        new Expectations() {{
            context.getPoint(); result = "point0001";
        }};

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

        // note nullが返るとキーごとスキップされるため、getterが呼び出しされていないこも確認
        new Verifications() {{
            context.getResult(); times = 0;
            context.getStartTime(); times = 0;
            context.getEndTime(); times = 0;
            context.getExecutionTime(); times = 0;
            context.getMaxMemory(); times = 0;
            context.getStartFreeMemory(); times = 0;
            context.getEndFreeMemory(); times = 0;
            context.getStartUsedMemory(); times = 0;
            context.getEndUsedMemory(); times = 0;
        }};
    }

    /**
     * メモリ関連の出力項目なしのときメモリのセットが行われないこと。
     */
    @Test
    public void testFormatWithoutMemory() {

        System.setProperty("performanceLogFormatter.targets",
                "point,result,startTime,endTime,executionTime");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        formatter.end(point, "success");

        new Verifications() {{
            context.setMaxMemory(anyLong); times = 0;
            context.setStartFreeMemory(anyLong); times = 0;
            context.setEndFreeMemory(anyLong); times = 0;
            context.setStartUsedMemory(anyLong); times = 0;
            context.setEndUsedMemory(anyLong); times = 0;
        }};

    }

    /**
     * 出力項目にmaxMemoryがあるときメモリのセットが行われること。
     */
    @Test
    public void testFormatWithMaxMemory() {

        System.setProperty("performanceLogFormatter.targets", "maxMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        new Verifications() {{
            context.setStartFreeMemory(anyLong); times = 1;
            context.setStartUsedMemory(anyLong); times = 1;
        }};

        formatter.end(point, "success");
        new Verifications() {{
            context.setMaxMemory(anyLong); minTimes(1);
            context.setEndFreeMemory(anyLong); times = 1;
            context.setEndUsedMemory(anyLong); times = 1;
        }};
    }

    /**
     * 出力項目にstartFreeMemoryがあるときメモリのセットが行われること。
     */
    @Test
    public void testFormatWithStartFreeMemory() {

        System.setProperty("performanceLogFormatter.targets", "startFreeMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        new Verifications() {{
            context.setStartFreeMemory(anyLong); times = 1;
            context.setStartUsedMemory(anyLong); times = 1;
        }};

        formatter.end(point, "success");
        new Verifications() {{
            context.setMaxMemory(anyLong); minTimes(1);
            context.setEndFreeMemory(anyLong); times = 1;
            context.setEndUsedMemory(anyLong); times = 1;
        }};
    }

    /**
     * 出力項目にendFreeMemoryがあるときメモリのセットが行われること。
     */
    @Test
    public void testFormatWithEndFreeMemory() {

        System.setProperty("performanceLogFormatter.targets", "endFreeMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        new Verifications() {{
            context.setStartFreeMemory(anyLong); times = 1;
            context.setStartUsedMemory(anyLong); times = 1;
        }};

        formatter.end(point, "success");
        new Verifications() {{
            context.setMaxMemory(anyLong); minTimes(1);
            context.setEndFreeMemory(anyLong); times = 1;
            context.setEndUsedMemory(anyLong); times = 1;
        }};
    }

    /**
     * 出力項目にstartUsedMemoryがあるときメモリのセットが行われること。
     */
    @Test
    public void testFormatWithStartUsedMemory() {

        System.setProperty("performanceLogFormatter.targets", "startUsedMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        new Verifications() {{
            context.setStartFreeMemory(anyLong); times = 1;
            context.setStartUsedMemory(anyLong); times = 1;
        }};

        formatter.end(point, "success");
        new Verifications() {{
            context.setMaxMemory(anyLong); minTimes(1);
            context.setEndFreeMemory(anyLong); times = 1;
            context.setEndUsedMemory(anyLong); times = 1;
        }};
    }

    /**
     * 出力項目にendUsedMemoryがあるときメモリのセットが行われること。
     */
    @Test
    public void testFormatWithEndUsedMemory() {

        System.setProperty("performanceLogFormatter.targets", "endUsedMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        new Verifications() {{
            context.setStartFreeMemory(anyLong); times = 1;
            context.setStartUsedMemory(anyLong); times = 1;
        }};

        formatter.end(point, "success");
        new Verifications() {{
            context.setMaxMemory(anyLong); minTimes(1);
            context.setEndFreeMemory(anyLong); times = 1;
            context.setEndUsedMemory(anyLong); times = 1;
        }};
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
