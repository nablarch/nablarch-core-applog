package nablarch.core.log.basic;

import nablarch.core.ThreadContext;
import nablarch.core.log.*;
import nablarch.core.log.app.OnMemoryLogWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

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
     * Json形式で正しくフォーマットできること。
     */
    @Test
    public void testFormat() {
        System.setProperty("nablarch.bootProcess", "APP001");
        System.setProperty("nablarch.processingSystem", "1");

        ThreadContext.setUserId("0000000001");
        ThreadContext.setRequestId("USERS00302");
        ThreadContext.setExecutionId(LogUtil.generateExecutionId());

        Logger logger = LoggerManager.get(JsonLogFormatterTest.class);

        OnMemoryLogWriter.clear();

        logger.logInfo("test_message");

        List<String> messages = OnMemoryLogWriter.getMessages("writer.jsonLog");

        assertThat(messages.get(0), isJson(allOf(
                withJsonPath("$", hasEntry("bootProcess", "APP001")),
                withJsonPath("$", hasEntry("logLevel", "INFO")),
                withJsonPath("$", hasEntry("loggerName", "ROO")))));
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
        Throwable error = null;
        try {
            errorMethod1();
        } catch (Exception e) {
            error = e;
        }

        Map<String, Object> payload1 = new HashMap<String, Object>();
        payload1.put("key1", "value1");

        Map<String, Object> payload2 = new HashMap<String, Object>();
        payload2.put("key2", 123);
        payload2.put("key3", true);

        LogLevel level = LogLevel.ERROR;

        LogContext context = new LogContext(loggerName, level, msg, error);

        String message = formatter.format(new LogContext(loggerName, runtimeLoggerName, level, msg, error, payload1, payload2));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasKey("date")),
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withJsonPath("$", hasEntry("loggerName", "TestLogger")),
                withoutJsonPath("$.runtimeLoggerName"),
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
     * 全項目の出力ができること。
     */
    @Test
    public void testFormatWithAllTargets() {
        System.setProperty("nablarch.bootProcess", "APP001");

        ThreadContext.setUserId("0000000001");
        ThreadContext.setRequestId("USERS00302");
        String executionId = LogUtil.generateExecutionId();
        ThreadContext.setExecutionId(executionId);

        LogFormatter formatter = new JsonLogFormatter();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("nablarch.processingSystem", "1");
        String targets = "date,logLevel,loggerName,runtimeLoggerName,bootProcess,processingSystem,"
                + "requestId,executionId,userId,message,stackTrace,payload";
        settings.put("formatter.targets", targets);
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));

        String loggerName = "TestLogger";
        String runtimeLoggerName = "TestLRuntimeLogger";
        String msg = "TestMessage";
        Throwable error = null;
        try {
            errorMethod1();
        } catch (Exception e) {
            error = e;
        }

        Map<String, Object> payload1 = new HashMap<String, Object>();
        payload1.put("key1", "value1");

        Map<String, Object> payload2 = new HashMap<String, Object>();
        payload2.put("key2", "value2");
        payload2.put("key3", "value3");

        LogLevel level = LogLevel.ERROR;

        LogContext context = new LogContext(loggerName, level, msg, error);

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
                withJsonPath("$", hasEntry("key2", "value2")),
                withJsonPath("$", hasEntry("key3", "value3")))));
    }

    private void errorMethod1() {
        errorMethod2();
    }

    private void errorMethod2() {
        throw new NullPointerException("error for test");
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
     * 不正なOptionsが無視されること。
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

        Map<Integer, Object> payload1 = new HashMap<Integer, Object>();
        payload1.put(1, 123);

        String payload2 = "test";

        Map<String, Object> payload3 = new HashMap<String, Object>();
        payload3.put("key", 123);

        String payload4 = null;

        LogLevel level = LogLevel.ERROR;

        String message = formatter.format(new LogContext(loggerName, level, msg, error,
                payload1, payload2, payload3, payload4));
        assertThat(message, isJson(allOf(
                withJsonPath("$", hasEntry("logLevel", "ERROR")),
                withJsonPath("$", hasEntry("message", "test")),
                withJsonPath("$", hasEntry("key", 123)))));
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
     * 構造化済みメッセージの接頭区の変更ができること。
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
     * フォーマットで例外がスローされても処理を止めないこと。
     */
    @Test
    public void testFormatWithError() {

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

        String message = formatter.format(new LogContext(loggerName, level, msg, error, payload));
        assertThat(message, is("\"log format error\"" + Logger.LS));
    }

}
