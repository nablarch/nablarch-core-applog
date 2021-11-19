package nablarch.core.log.app;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.core.log.LogTestSupport;
import nablarch.core.log.RegexMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.lang.management.MemoryUsage;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;
import static nablarch.core.log.RegexMatcher.matches;
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
        System.clearProperty("performanceLogFormatter.datePattern");
    }

    /**
     * デフォルトの出力項目で正しくフォーマットされること。
     */
    @Test
    public void testFormat(@Mocked final PerformanceLogFormatter.PerformanceLogContext context) {

        new Expectations() {{
            context.getPoint(); result = "point0001";
            context.getResult(); result = "success";
            context.getStartTime(); result = toMilliseconds("2021-11-19 15:30:20.123");
            context.getEndTime(); result = toMilliseconds("2021-11-19 15:31:20.987");
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

        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("point", "point0001")),
                withJsonPath("$", hasEntry("result", "success")),
                withJsonPath("$", hasEntry("startTime", "2021-11-19 15:30:20.123")),
                withJsonPath("$", hasEntry("endTime", "2021-11-19 15:31:20.987")),
                withJsonPath("$", hasEntry("executionTime", 200)),
                withJsonPath("$", hasEntry("maxMemory", 1000000)),
                withJsonPath("$", hasEntry("startFreeMemory", 700000)),
                withJsonPath("$", hasEntry("endFreeMemory", 670000)),
                withJsonPath("$", hasEntry("startUsedMemory", 300000)),
                withJsonPath("$", hasEntry("endUsedMemory", 330000)))));
    }

    /**
     * {@code "yyyy-MM-dd HH:mm:ss.SSS"} 書式の日付文字列をミリ秒に変換する。
     * @param dateText 日付文字列
     * @return 日付のミリ秒
     */
    private static long toMilliseconds(String dateText) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            return format.parse(dateText).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
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
            withJsonPath("$.*", hasSize(1)),
            withJsonPath("$", hasEntry("point", "point0001"))
        )));
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
        String message = formatter.end(point, "success");

        assertThat(message.substring("$JSON$".length()), isJson(allOf(
            withoutJsonPath("$.maxMemory"),
            withoutJsonPath("$.startFreeMemory"),
            withoutJsonPath("$.endFreeMemory"),
            withoutJsonPath("$.startUsedMemory"),
            withoutJsonPath("$.endUsedMemory")
        )));
    }

    /**
     * 出力項目にmaxMemoryがあるときメモリの計測が行われログにmaxMemoryが出力されること。
     */
    @Test
    public void testFormatWithMaxMemory(@Mocked final MemoryUsage memoryUsage) {
        new Expectations() {{
            memoryUsage.getMax(); returns(2000L, 99L);
        }};

        System.setProperty("performanceLogFormatter.targets", "maxMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message.substring("$JSON$".length()), isJson(
            withJsonPath("$", hasEntry("maxMemory", 2000)))
        );
    }

    /**
     * 出力項目にstartFreeMemoryがあるときメモリの計測が行われログにstartFreeMemoryが出力されること。
     */
    @Test
    public void testFormatWithStartFreeMemory(@Mocked final MemoryUsage memoryUsage) {
        new Expectations() {{
            memoryUsage.getMax(); returns(2000L, 99L);
            memoryUsage.getUsed(); returns(1500L, 9L);
        }};

        System.setProperty("performanceLogFormatter.targets", "startFreeMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message.substring("$JSON$".length()), isJson(
            withJsonPath("$", hasEntry("startFreeMemory", 500)))
        );
    }

    /**
     * 出力項目にendFreeMemoryがあるときメモリの計測が行われログにendFreeMemoryが出力されること。
     */
    @Test
    public void testFormatWithEndFreeMemory(@Mocked final MemoryUsage memoryUsage) {
        new Expectations() {{
            memoryUsage.getMax(); returns(2000L, 99L);
            memoryUsage.getUsed(); returns(1500L, 9L);
        }};

        System.setProperty("performanceLogFormatter.targets", "endFreeMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message.substring("$JSON$".length()), isJson(
            withJsonPath("$", hasEntry("endFreeMemory", 90)))
        );
    }

    /**
     * 出力項目にstartUsedMemoryがあるときメモリの計測が行われログにstartUsedMemoryが出力されること。
     */
    @Test
    public void testFormatWithStartUsedMemory(@Mocked final MemoryUsage memoryUsage) {
        new Expectations() {{
            memoryUsage.getUsed(); returns(1500L, 9L);
        }};

        System.setProperty("performanceLogFormatter.targets", "startUsedMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message.substring("$JSON$".length()), isJson(
            withJsonPath("$", hasEntry("startUsedMemory", 1500)))
        );
    }

    /**
     * 出力項目にendUsedMemoryがあるときメモリの計測が行われログにendUsedMemoryが出力されること。
     */
    @Test
    public void testFormatWithEndUsedMemory(@Mocked final MemoryUsage memoryUsage) {
        new Expectations() {{
            memoryUsage.getUsed(); returns(1500L, 9L);
        }};

        System.setProperty("performanceLogFormatter.targets", "endUsedMemory");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";
        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message.substring("$JSON$".length()), isJson(
            withJsonPath("$", hasEntry("endUsedMemory", 9)))
        );
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

    /**
     * 日付フォーマットが指定できることをテスト。
     */
    @Test
    public void testDatePattern(@Mocked final PerformanceLogFormatter.PerformanceLogContext context) {
        System.setProperty("performanceLogFormatter.targets", "startTime,endTime");
        System.setProperty("performanceLogFormatter.datePattern", "yyyy/MM/dd HH:mm:ss");

        new Expectations() {{
            context.getStartTime(); result = toMilliseconds("2021-11-19 15:30:20.123");
            context.getEndTime(); result = toMilliseconds("2021-11-19 15:31:20.987");
        }};

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";

        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(2)),
                withJsonPath("$", hasEntry("startTime", "2021/11/19 15:30:20")),
                withJsonPath("$", hasEntry("endTime", "2021/11/19 15:31:20"))
        )));
    }
}
