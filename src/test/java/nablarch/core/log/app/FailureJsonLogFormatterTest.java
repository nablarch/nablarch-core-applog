package nablarch.core.log.app;

import nablarch.core.ThreadContext;
import nablarch.core.log.LogTestSupport;
import nablarch.core.message.MockStringResourceHolder;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
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
                withoutJsonPath("$.failureCode"),
                withJsonPath("$", hasEntry("message", "FW000001Messagenotification")),
                withoutJsonPath("$.data"),
                withJsonPath("$", hasEntry("contact", "AAA001")))));
    }

    /**
     * 出力項目を指定した障害解析ログがフォーマットされること。
     */
    @Test
    public void testFormatAnalysisMessageWithTargets() {
        System.setProperty("failureLogFormatter.analysisTargets", "failureCode ,message,data,,");

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

        String message = formatter.formatAnalysisMessage(null, data, failureCode, new Object[] {"error"});
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("failureCode", "FW000001")),
                withJsonPath("$", hasEntry("message", "FW000001Messageerror")),
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
}
