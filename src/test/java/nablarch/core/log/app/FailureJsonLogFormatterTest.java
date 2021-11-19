package nablarch.core.log.app;

import nablarch.core.ThreadContext;
import nablarch.core.log.LogTestSupport;
import nablarch.core.message.MockStringResourceHolder;
import nablarch.core.text.json.BasicJsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.text.json.JsonSerializer;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

/**
 * {@link FailureJsonLogFormatter}のテストクラス。
 * @author Shuji Kitamura
 */
public class FailureJsonLogFormatterTest extends LogTestSupport {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/core/log/app/message-resource-initialload-test.xml");

    private static final String[][] MESSAGES = {
            { "FW000001", "ja", "FW000001メッセージ{0}", "en","FW000001Message{0}"},
            { "FW000002", "ja", "FW000002メッセージ{0}", "en","FW000002Message{0}" },
            { "FW000003", "ja", "FW000003メッセージ{0}" , "en","FW000003Message{0}" },
            { "FW999999", "ja", "FW999999メッセージ{0}" , "en","FW999999Message{0}"},
            { "ZZ999999", "ja", "ZZ999999メッセージ{0}", "en","ZZ999999Message{0}" },
            { "AP000001", "ja","AP000001メッセージ{0}" , "en","AP000001Message{0}"},
            { "AP000002", "ja","AP000002メッセージ{0}", "en"," AP000002Message{0}" },
            { "AP000003", "ja","AP000003メッセージ{0}", "en","AP000003Message{0}" },
            { "failure.code.unknown", "ja","未知のエラー", "en","unknown error!!!" },
    };

    @Before
    public void setup() {
        repositoryResource.getComponentByType(MockStringResourceHolder.class).setMessages(MESSAGES);
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("param", new String[]{"10"});

        ThreadContext.clear();

        System.clearProperty("failureLogFormatter.notificationTargets");
        System.clearProperty("failureLogFormatter.analysisTargets");
        System.clearProperty("failureLogFormatter.contactFilePath");
    }

    /**
     * デフォルト設定でフォーマットされること。
     */
    @Test
    public void testFormat() {
        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-default.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        FailureLogFormatter formatter = new FailureJsonLogFormatter();

        String failureCode = "FW000001";
        Object data = new TreeMap<String, String>() {
            {
                put("requestId", "REQ_TEST");
                put("executionId", "EXE_TEST");
                put("userId", "USR_TEST");
            }
        };

        String message = formatter.formatNotificationMessage(null, data, failureCode, new Object[] {"notification"});
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("failureCode", "FW000001")),
                withJsonPath("$", hasEntry("message", "FW000001メッセージnotification")),
                withoutJsonPath("$.data"),
                withoutJsonPath("$.contact"))));

        message = formatter.formatAnalysisMessage(null, data, failureCode, new Object[] {"error"});
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("failureCode", "FW000001")),
                withJsonPath("$", hasEntry("message", "FW000001メッセージerror")),
                withoutJsonPath("$.data"),
                withoutJsonPath("$.contact"))));
    }

    /**
     * 出力項目を指定した障害通知ログがフォーマットされること。
     */
    @Test
    public void testFormatNotificationWithTargets() {
        System.setProperty("failureLogFormatter.notificationTargets", "message , ,message,contact");
        System.setProperty("failureLogFormatter.contactFilePath", "classpath:nablarch/core/log/app/failure-log-contact.properties");
        System.setProperty("failureLogFormatter.appFailureCodeFilePath", "classpath:nablarch/core/log/app/failure-log-appFailCode.properties");

        FailureLogFormatter formatter = new FailureJsonLogFormatter();

        String failureCode = "UM000001";
        Object data = new TreeMap<String, String>() {
            {
                put("requestId", "REQ_TEST");
                put("executionId", "EXE_TEST");
                put("userId", "USR_TEST");
            }
        };

        ThreadContext.setRequestId("R000001");

        String message = formatter.formatNotificationMessage(null, data, failureCode, new Object[] {"notification"});
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withoutJsonPath("$.failureCode"),
                withJsonPath("$", hasEntry("message", "AP000001Messagenotification")),
                withoutJsonPath("$.data"),
                withJsonPath("$", hasEntry("contact", "AAA001")))));
    }

    /**
     * 出力項目を指定した障害解析ログがフォーマットされること。
     */
    @Test
    public void testFormatAnalysisMessageWithTargets() {
        System.setProperty("failureLogFormatter.analysisTargets", "failureCode ,message,data,,");
        System.setProperty("failureLogFormatter.appFailureCodeFilePath", "classpath:nablarch/core/log/app/failure-log-appFailCode.properties");

        FailureLogFormatter formatter = new FailureJsonLogFormatter();

        String failureCode = "UM000001";
        Object data = new TreeMap<String, String>() {
            {
                put("requestId", "REQ_TEST");
                put("executionId", "EXE_TEST");
                put("userId", "USR_TEST");
            }
        };

        ThreadContext.setRequestId("R000001");

        String message = formatter.formatAnalysisMessage(null, data, failureCode, new Object[] {"error"});
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("failureCode", "AP000001")),
                withJsonPath("$", hasEntry("message", "AP000001Messageerror")),
                withJsonPath("$.data", hasEntry("requestId", "REQ_TEST")),
                withJsonPath("$.data", hasEntry("executionId", "EXE_TEST")),
                withJsonPath("$.data", hasEntry("userId", "USR_TEST")),
                withoutJsonPath("$.contact"))));
    }

    /**
     * 全ての出力項目がフォーマットされること。
     */
    @Test
    public void testFormatWithFullTargets() {
        System.setProperty("failureLogFormatter.notificationTargets", "failureCode,message,data,contact");
        System.setProperty("failureLogFormatter.analysisTargets", "failureCode,message,data,contact");
        System.setProperty("failureLogFormatter.contactFilePath", "classpath:nablarch/core/log/app/failure-log-contact.properties");

        FailureLogFormatter formatter = new FailureJsonLogFormatter();

        String failureCode = "FW000001";
        Object data = new TreeMap<String, String>() {
            {
                put("requestId", "REQ_TEST");
                put("executionId", "EXE_TEST");
                put("userId", "USR_TEST");
            }
        };

        ThreadContext.setRequestId("R000001");

        String message = formatter.formatNotificationMessage(null, data, failureCode, new Object[] {"notification"});
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("failureCode", "FW000001")),
                withJsonPath("$", hasEntry("message", "FW000001Messagenotification")),
                withJsonPath("$.data", hasEntry("requestId", "REQ_TEST")),
                withJsonPath("$.data", hasEntry("executionId", "EXE_TEST")),
                withJsonPath("$.data", hasEntry("userId", "USR_TEST")),
                withJsonPath("$", hasEntry("contact", "AAA001")))));

        message = formatter.formatAnalysisMessage(null, data, failureCode, new Object[] {"error"});
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("failureCode", "FW000001")),
                withJsonPath("$", hasEntry("message", "FW000001Messageerror")),
                withJsonPath("$.data", hasEntry("requestId", "REQ_TEST")),
                withJsonPath("$.data", hasEntry("executionId", "EXE_TEST")),
                withJsonPath("$.data", hasEntry("userId", "USR_TEST")),
                withJsonPath("$", hasEntry("contact", "AAA001")))));
    }

    /**
     * 不正なターゲットのテスト。
     */
    @Test
    public void testIllegalTargets() {
        System.setProperty("failureLogFormatter.notificationTargets", "failureCode,message,dummy,contact");

        Exception e = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                FailureLogFormatter formatter = new FailureJsonLogFormatter();
            }
        });

        assertThat(e.getMessage(), is("[dummy] is unknown target. property name = [failureLogFormatter.notificationTargets]"));
    }

    /**
     * スレッドコンテキストの言語情報からメッセージが構築されること。
     */
    @Test
    public void testGetMessageDependThreadContext() {
        System.setProperty("failureLogFormatter.notificationTargets", "message");
        System.setProperty("failureLogFormatter.analysisTargets", "message");
        ThreadContext.setLanguage(Locale.ENGLISH);

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-default_bc.properties");
        final FailureJsonLogFormatter sut = new FailureJsonLogFormatter();

        final String analysisMessage =
            sut.formatAnalysisMessage(null, null, "failure.code.unknown", new Object[]{"error"});

        assertThat(analysisMessage.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(1)),
            withJsonPath("$", hasEntry("message", "unknown error!!!"))
        )));

        final String notificationMessage =
            sut.formatNotificationMessage(null, null, "failure.code.unknown", new Object[]{"error"});

        assertThat(notificationMessage.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(1)),
            withJsonPath("$", hasEntry("message", "unknown error!!!"))
        )));
    }

    /**
     * スレッドコンテキストが存在しない場合デフォルトの言語情報からメッセージが構築されること
     */
    @Test
    public void testGetMessageNotDependThreadContext() {
        System.setProperty("failureLogFormatter.notificationTargets", "message");
        System.setProperty("failureLogFormatter.analysisTargets", "message");
        ThreadContext.clear();

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-default_bc.properties");
        final FailureJsonLogFormatter sut = new FailureJsonLogFormatter();

        final String analysisMessage =
            sut.formatAnalysisMessage(null, null, "failure.code.unknown", new Object[]{"error"});

        assertThat(analysisMessage.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(1)),
            withJsonPath("$", hasEntry("message", "未知のエラー"))
        )));

        final String notificationMessage =
            sut.formatNotificationMessage(null, null, "failure.code.unknown", new Object[]{"error"});

        assertThat(notificationMessage.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(1)),
            withJsonPath("$", hasEntry("message", "未知のエラー"))
        )));
    }

    /**
     * {@code fwFailureCodeFilePath}の指定ができることのテスト。
     */
    @Test
    public void testFwFailureCodeFilePath() {
        System.setProperty("failureLogFormatter.notificationTargets", "failureCode");
        System.setProperty("failureLogFormatter.analysisTargets", "failureCode");
        System.setProperty("failureLogFormatter.fwFailureCodeFilePath", "classpath:nablarch/core/log/app/failure-log-fwFailCode3.properties");

        final FailureJsonLogFormatter sut = new FailureJsonLogFormatter();

        final Exception exception = new Exception();

        final String analysisMessage =
                sut.formatAnalysisMessage(exception, null, null, new Object[]{"error"});

        assertThat(analysisMessage.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("failureCode", "FW000001"))
        )));

        final String notificationMessage =
                sut.formatNotificationMessage(exception, null, null, new Object[]{"error"});

        assertThat(notificationMessage.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("failureCode", "FW000001"))
        )));
    }

    /**
     * {@code fwMessageIdFilePath}の指定ができることのテスト。
     */
    @Test
    public void testFwMessageIdFilePath() {
        System.setProperty("failureLogFormatter.notificationTargets", "failureCode");
        System.setProperty("failureLogFormatter.analysisTargets", "failureCode");
        System.setProperty("failureLogFormatter.fwMessageIdFilePath", "classpath:nablarch/core/log/app/failure-log-fwFailCode3.properties");

        final FailureJsonLogFormatter sut = new FailureJsonLogFormatter();

        final Exception exception = new Exception();

        final String analysisMessage =
                sut.formatAnalysisMessage(exception, null, null, new Object[]{"error"});

        assertThat(analysisMessage.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("failureCode", "FW000001"))
        )));

        final String notificationMessage =
                sut.formatNotificationMessage(exception, null, null, new Object[]{"error"});

        assertThat(notificationMessage.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("failureCode", "FW000001"))
        )));
    }

    /**
     * {@code appMessageIdFilePath}の指定ができることのテスト。
     */
    @Test
    public void testAppMessageIdFilePath() {
        System.setProperty("failureLogFormatter.notificationTargets", "failureCode");
        System.setProperty("failureLogFormatter.analysisTargets", "failureCode");
        System.setProperty("failureLogFormatter.appMessageIdFilePath", "classpath:nablarch/core/log/app/failure-log-appFailCode.properties");

        final FailureJsonLogFormatter sut = new FailureJsonLogFormatter();

        final Exception exception = new Exception();

        final String analysisMessage =
                sut.formatAnalysisMessage(exception, null, "UM000002", new Object[]{"error"});

        assertThat(analysisMessage.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("failureCode", "AP000002"))
        )));

        final String notificationMessage =
                sut.formatNotificationMessage(exception, null, "UM000003", new Object[]{"error"});

        assertThat(notificationMessage.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("failureCode", "AP000003"))
        )));
    }

    /**
     * {@code structuredMessagePrefix}の指定ができることのテスト。
     */
    @Test
    public void testStructuredMessagePrefix() {
        System.setProperty("failureLogFormatter.notificationTargets", "failureCode");
        System.setProperty("failureLogFormatter.analysisTargets", "failureCode");
        System.setProperty("failureLogFormatter.structuredMessagePrefix", "@JSON@");

        final FailureJsonLogFormatter sut = new FailureJsonLogFormatter();

        final String analysisMessage =
                sut.formatAnalysisMessage(null, null, "E000001", new Object[]{"error"});

        assertThat(analysisMessage, startsWith("@JSON@"));

        final String notificationMessage =
                sut.formatNotificationMessage(null, null, "E000001", new Object[]{"error"});

        assertThat(notificationMessage, startsWith("@JSON@"));
    }

    /**
     * {@code jsonSerializationManagerClassName}の指定ができることをテスト。
     */
    @Test
    public void testJsonSerializationManagerClassName() {
        System.setProperty("failureLogFormatter.notificationTargets", "failureCode");
        System.setProperty("failureLogFormatter.analysisTargets", "failureCode");
        System.setProperty("failureLogFormatter.jsonSerializationManagerClassName", MockJsonSerializationManager.class.getName());

        final FailureJsonLogFormatter sut = new FailureJsonLogFormatter();

        final String analysisMessage =
                sut.formatAnalysisMessage(null, null, "E000001", new Object[]{"error"});

        assertThat(analysisMessage, is("$JSON$mock serialization"));

        final String notificationMessage =
                sut.formatNotificationMessage(null, null, "E000001", new Object[]{"error"});

        assertThat(notificationMessage, is("$JSON$mock serialization"));
    }

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
                    return false;
                }
            };
        }
    }
}
