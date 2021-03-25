package nablarch.core.log.basic;

import nablarch.core.ThreadContext;
import nablarch.core.log.LogTestSupport;
import nablarch.core.log.LogUtil;
import nablarch.core.log.MockLogSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThrows;

/**
 * {@link JsonLogFormatter}のテストクラス。
 * @author Shuji Kitamura
 */
public class JsonLogFormatterTest extends LogTestSupport {

    @Before
    public void setup() {
        System.clearProperty("nablarch.bootProcess");
        System.clearProperty("nablarch.processingSystem");
        ThreadContext.clear();
    }

    @After
    public void teardown() {
        System.clearProperty("nablarch.bootProcess");
        System.clearProperty("nablarch.processingSystem");
        ThreadContext.clear();
    }

    /**
     * デフォルト設定で出力ができること。
     */
    @Test
    public void testFormatWithDefault() {
        System.setProperty("nablarch.bootProcess", "APP001");

        ThreadContext.setUserId("0000000001");
        ThreadContext.setRequestId("USERS00302");
        String executionId = LogUtil.generateExecutionId();
        ThreadContext.setExecutionId(executionId);

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("nablarch.processingSystem", "1");
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = "TestLogger";
        String runtimeLoggerName = "TestLRuntimeLogger";
        String msg = "TestMessage";
        Throwable error = new NullPointerException("error for test");

        Map<String, Object> payload1 = new HashMap<String, Object>();
        payload1.put("key1", "value1");

        Map<String, Object> payload2 = new HashMap<String, Object>();
        payload2.put("key2", 123);
        payload2.put("key3", true);

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, runtimeLoggerName, level, msg, error, payload1, payload2));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasKey("date")),
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withJsonPath("$", hasEntry("loggerName", "TestLogger")),
                withJsonPath("$", hasEntry("runtimeLoggerName", "TestLRuntimeLogger")),
                withJsonPath("$", hasEntry("bootProcess", "APP001")),
                withJsonPath("$", hasEntry("processingSystem", "1")),
                withJsonPath("$", hasEntry("requestId", "USERS00302")),
                withJsonPath("$", hasEntry("executionId", executionId)),
                withJsonPath("$", hasEntry("userId", "0000000001")),
                withJsonPath("$", hasEntry("message", "TestMessage")),
                withJsonPath("$", hasKey("stackTrace")),
                withJsonPath("$", hasEntry("key1", "value1")),
                withJsonPath("$", hasEntry("key2", 123)),
                withJsonPath("$", hasEntry("key3", true)))));
    }

    /**
     * 出力項目を指定して出力ができること。
     */
    @Test
    public void testFormatWithAllTargets() {

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        String targets = "logLevel,loggerName,message";
        settings.put("formatter.targets", targets);
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = "TestLogger";
        String msg = "TestMessage";
        Throwable error = null;

        Map<String, Object> payload1 = new HashMap<String, Object>();
        payload1.put("key1", "value1");

        Map<String, Object> payload2 = new HashMap<String, Object>();
        payload2.put("key2", "value2");
        payload2.put("key3", "value3");

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, level, msg, error, payload1, payload2));
        assertThat(message, isJson(allOf(
                withoutJsonPath("$.date"),
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withJsonPath("$", hasEntry("loggerName", "TestLogger")),
                withoutJsonPath("$.runtimeLoggerName"),
                withoutJsonPath("$.bootProcess"),
                withoutJsonPath("$.processingSystem"),
                withoutJsonPath("$.requestId"),
                withoutJsonPath("$.executionId"),
                withoutJsonPath("$.userId"),
                withJsonPath("$", hasEntry("message", "TestMessage")),
                withoutJsonPath("$.stackTrace"),
                withoutJsonPath("$.key1"),
                withoutJsonPath("$.key2"),
                withoutJsonPath("$.key3"))));
    }

    /**
     * 各項目でnullが指定されてもエラーにならないこと。
     */
    @Test
    public void testFormatWithNull() {

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        String targets = "date,logLevel,loggerName,runtimeLoggerName,bootProcess,processingSystem,"
                + "requestId,executionId,userId,message,stackTrace,payload";
        settings.put("formatter.targets", targets);
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = null;
        String msg = null;
        Throwable error = null;

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, level, msg, error, null));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasKey("date")),
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withoutJsonPath("$.loggerName"),
                withoutJsonPath("$.runtimeLoggerName"),
                withJsonPath("$", hasEntry("bootProcess", "")),
                withJsonPath("$", hasEntry("processingSystem", "")),
                withoutJsonPath("$.requestId"),
                withoutJsonPath("$.executionId"),
                withoutJsonPath("$.userId"),
                withoutJsonPath("$.message"),
                withoutJsonPath("$.stackTrace"),
                withoutJsonPath("$.payload"))));
    }

    /**
     * 不正なターゲットが無視されること。
     */
    @Test
    public void testFormatWithIllegalTargets() {

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("formatter.targets", "message ,, ,dummy,message");
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = "TestLogger";
        String msg = "TestMessage";
        Throwable error = null;

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, level, msg, error));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasEntry("message", "TestMessage")))));
    }

    /**
     * Map以外のOptionsが無視されること。
     */
    @Test
    public void testFormatWithIllegalOptions() {

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("formatter.targets", "logLevel,message,payload");
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = "TestLogger";
        String msg = "test";
        Throwable error = null;

        String payload1 = "test";

        Map<String, Object> payload2 = new HashMap<String, Object>();
        payload2.put("key", 123);

        String payload3 = null;

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, level, msg, error,
                payload1, payload2, payload3));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withJsonPath("$", hasEntry("message", "test")),
                withJsonPath("$", hasEntry("key", 123)))));
    }

    /**
     * キーがStringではないMapのEntryは無視されること。
     */
    @Test
    public void testFormatWithIllegalMapOptions() {

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("formatter.targets", "logLevel,message,payload");
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = "TestLogger";
        String msg = "test";
        Throwable error = null;

        Map<Integer, Object> payload1 = new HashMap<Integer, Object>();
        payload1.put(1, 12);

        Map<Object, Object> payload2 = new HashMap<Object, Object>();
        payload2.put("key", 34);
        payload2.put(2, 56);
        payload2.put(true, 78);
        payload2.put(null, 90);

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, level, msg, error,
                payload1, payload2));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withJsonPath("$", hasEntry("message", "test")),
                withJsonPath("$", hasEntry("key", 34)),
                withJsonPath("$", not(hasValue(12))),
                withJsonPath("$", not(hasValue(56))),
                withJsonPath("$", not(hasValue(78))),
                withJsonPath("$", not(hasValue(90))))));
    }

    /**
     * 構造化済みのメッセージが処理されること。
     */
    @Test
    public void testFormatWithStructuredMessage() {

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("formatter.targets", "logLevel,message");
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = "TestLogger";
        String msg = "$JSON${\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        Throwable error = null;

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, level, msg, error));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withJsonPath("$", hasEntry("key1", "value1")),
                withJsonPath("$", hasEntry("key2", "value2")),
                withJsonPath("$", hasEntry("key3", "value3")))));
    }

    /**
     * 構造化済みメッセージの接頭辞の変更ができること。
     */
    @Test
    public void testFormatWithOtherStructuredMessage() {

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("formatter.targets", "logLevel,message");
        settings.put("formatter.structuredMessagePrefix", "@JSON@");
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = "TestLogger";
        String msg = "@JSON@{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        Throwable error = null;

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, level, msg, error));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withJsonPath("$", hasEntry("key1", "value1")),
                withJsonPath("$", hasEntry("key2", "value2")),
                withJsonPath("$", hasEntry("key3", "value3")))));
    }

    /**
     * シリアライズ管理クラスの変更ができること。
     */
    @Test
    public void testFormatWithOtherManager() {

        // note CustomJsonSerializationManager クラスは、NumberToJsonSerializerが外されており、
        //      JavaのNumber型がObjectToJsonSerializerにて処理され、JSONのstringとして出力される。

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("formatter.targets", "logLevel,message,payload");
        settings.put("formatter.jsonSerializationManagerClassName", "nablarch.core.log.basic.CustomJsonSerializationManager");
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = "TestLogger";
        String msg = "test";
        Throwable error = null;

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("key", 123);

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, level, msg, error, payload));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withJsonPath("$", hasEntry("message", "test")),
                withJsonPath("$", hasEntry("key", "123")))));
    }

    /**
     * シリアライズ時にIOExceptionがスローされたときRuntimeExceptionとして再スローされること。
     */
    @Test
    public void testFormatWithError() {
        // note CustomJsonSerializationManagerはbooleanを処理する際に、必ずIOExceptionをスローする

        assertThrows(RuntimeException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                LogFormatter formatter = new JsonLogFormatter();
                Map<String, String> settings = new HashMap<String, String>();
                settings.put("formatter.targets", "logLevel,message,payload");
                settings.put("formatter.jsonSerializationManagerClassName", "nablarch.core.log.basic.CustomJsonSerializationManager");
                formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

                String loggerName = "TestLogger";
                String msg = "test";
                Throwable error = null;

                Map<String, Object> payload = new HashMap<String, Object>();
                payload.put("key", true);

                LogLevel level = LogLevel.ERROR;

                formatter.format(new LogContext(loggerName, level, msg, error, payload));
            }
        });
    }

}
