package nablarch.core.log.app;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.core.log.LogTestSupport;
import nablarch.core.text.json.BasicJsonSerializationManager;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.text.json.JsonSerializer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;
import java.io.Writer;
import java.lang.management.MemoryUsage;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
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
        System.clearProperty("performanceLogFormatter.structuredMessagePrefix");
    }

    /**
     * デフォルトの出力項目で正しくフォーマットされること。
     */
    @Test
    @Ignore("jacoco と jmockit が競合してエラーになるため")
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

        System.setProperty("performanceLogFormatter.targets", "maxMemory,point");

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

        System.setProperty("performanceLogFormatter.targets", "startFreeMemory,point");

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

        System.setProperty("performanceLogFormatter.targets", "endFreeMemory,point");

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

        System.setProperty("performanceLogFormatter.targets", "startUsedMemory,point");

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

        System.setProperty("performanceLogFormatter.targets", "endUsedMemory,point");

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
    @Ignore("jacoco と jmockit が競合してエラーになるため")
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

    /**
     * JSON文字列であることを示すプレフィックスを指定できることをテスト。
     */
    @Test
    public void testStructuredMessagePrefix() {
        System.setProperty("performanceLogFormatter.targets", "point");
        System.setProperty("performanceLogFormatter.structuredMessagePrefix", "@JSON@");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter();

        String point = "point0001";

        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message.startsWith("@JSON@"), is(true));
        assertThat(message.substring("@JSON@".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("point", "point0001"))
        )));
    }

    /**
     * {@link nablarch.core.text.json.JsonSerializationManager}を指定できることをテスト。
     */
    @Test
    public void testJsonSerializationManagerClassName() {
        System.setProperty("performanceLogFormatter.targets", "point");

        PerformanceLogFormatter formatter = new PerformanceJsonLogFormatter() {
            @Override
            protected JsonSerializationManager createSerializationManager(JsonSerializationSettings settings) {
                assertThat(settings.getProp("targets"), is("point"));
                return new MockJsonSerializationManager();
            }
        };

        String point = "point0001";

        formatter.start(point);
        String message = formatter.end(point, "success");

        assertThat(message, is("$JSON$mock serialization"));
    }

    /**
     * {@link nablarch.core.text.json.JsonSerializationManager}のモック。
     */
    public static class MockJsonSerializationManager extends BasicJsonSerializationManager {

        @Override
        public JsonSerializer getSerializer(Object value) {
            return new JsonSerializer() {
                @Override
                public void serialize(Writer writer, Object value) throws IOException {
                    writer.write("mock serialization");
                }

                @Override
                public void initialize(JsonSerializationSettings settings) {

                }

                @Override
                public boolean isTarget(Class<?> valueClass) {
                    return true;
                }
            };
        }
    }
}
