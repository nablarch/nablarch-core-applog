package nablarch.core.log.app;

import nablarch.core.ThreadContext;
import nablarch.core.log.LogTestSupport;
import nablarch.core.log.Logger;
import nablarch.core.message.MockStringResourceHolder;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Kiyohito Itoh
 */
public class FailureLogUtilTest extends LogTestSupport {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
			"nablarch/core/log/app/message-resource-initialload-test.xml");

    private static final String[][] MESSAGES = {
        { "FW000001", "ja", "FW000001メッセージ{0}", "en","FW000001Message{0}"},
        { "FW000002", "ja", "FW000002メッセージ{0}", "en","FW000002Message{0}" },
        { "FW000003", "ja", "FW000003メッセージ{0}:\uD83D\uDE0E\uD83D\uDE0E" , "en","FW000003Message{0}" },
        { "FW000004", "ja", "FW000004メッセージ{0}", "en","FW000004Message{0}"},
        { "FW000005", "ja", "FW000005メッセージ{0}", "en","FW000005Message{0}" },
        { "FW000006", "ja", "FW000006メッセージ{0}" , "en","FW000006Message{0}" },
        { "FW000007", "ja", "FW000007メッセージ{0}" , "en","FW000007Message{0}" },
        { "FW999999", "ja", "FW999999メッセージ{0}" , "en","FW999999Message{0}"},
        { "ZZ999999", "ja", "ZZ999999メッセージ{0}", "en","ZZ999999Message{0}" },
        { "AP000001", "ja","AP000001メッセージ{0}" , "en","AP000001Message{0}"},
        { "AP000002", "ja","AP000002メッセージ{0}", "en"," AP000002Message{0}" },
        { "AP000003", "ja","AP000003メッセージ{0}", "en","AP000003Message{0}" },
        };

    @BeforeClass
    public static void classSetup() throws Exception {
        repositoryResource.getComponentByType(MockStringResourceHolder.class).setMessages(MESSAGES);
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("param", new String[]{"10"});
    }
    @AfterClass
	public static void classDown() {
	}

    /**
     * 設定不備の場合に例外がスローされること。
     */
    @Test
    public void testInitialize() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-invalid.properties");

        try {
            FailureLogUtil.initialize();
            fail("must throw exception.");
        } catch (Throwable e) {
            assertThat(e.getCause().getMessage(), is("invalid failureLogFormatter.className"));
        }
    }

    /**
     * デフォルト設定で正しく出力されること。
     */
    @Test
    public void testDefault() {

        OnMemoryLogWriter.clear();

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-default.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        Object data = null;

        // FATAL
        FailureLogUtil.logFatal(new IllegalArgumentException("fatal_full_error"), data, "FW000001", new Object[] {"fatal_full_messageOption"}, new Object[] {"fatal_full_logOption"});
        FailureLogUtil.logFatal(new IllegalArgumentException("fatal_short_error"), data, "FW000002", "fatal_short_messageOption");
        FailureLogUtil.logFatal(data, "FW000003", "fatal_min_messageOption");

        // ERROR
        FailureLogUtil.logError(new IllegalArgumentException("error_full_error"), data, "FW000001", new Object[] {"error_full_messageOption"}, new Object[] {"error_full_logOption"});
        FailureLogUtil.logError(new IllegalArgumentException("error_short_error"), data, "FW000002", "error_short_messageOption");
        FailureLogUtil.logError(data, "FW000003", "error_min_messageOption-\uD83D\uDE0E\uD83D\uDE0E");

        // WARN
        FailureLogUtil.logWarn(new IllegalArgumentException("warn_short_error"), data, "FW000002", "warn_short_messageOption");

        List<String> monitorFile = OnMemoryLogWriter.getMessages("writer.monitorLog");
        List<String> appFile = OnMemoryLogWriter.getMessages("writer.appLog");

        // fatal_full

        int index = 0;

        assertThat(monitorFile.get(index), is("FATAL fail_code = [FW000001] FW000001メッセージfatal_full_messageOption" + Logger.LS));
        assertThat(appFile.get(index), containsString("FATAL ROO fail_code = [FW000001] FW000001メッセージfatal_full_messageOption"));

        // fatal_short

        index++;

        assertThat(monitorFile.get(index), is("FATAL fail_code = [FW000002] FW000002メッセージfatal_short_messageOption" + Logger.LS));
        assertThat(appFile.get(index), containsString("FATAL ROO fail_code = [FW000002] FW000002メッセージfatal_short_messageOption"));

        // fatal_min

        index++;

        assertThat(monitorFile.get(index), is("FATAL fail_code = [FW000003] FW000003メッセージfatal_min_messageOption:\uD83D\uDE0E\uD83D\uDE0E" + Logger.LS));
        assertThat(appFile.get(index), containsString("FATAL ROO fail_code = [FW000003] FW000003メッセージfatal_min_messageOption"));

        // error_full

        index++;

        assertThat(monitorFile.get(index), is("ERROR fail_code = [FW000001] FW000001メッセージerror_full_messageOption" + Logger.LS));
        assertThat(appFile.get(index), containsString("ERROR ROO fail_code = [FW000001] FW000001メッセージerror_full_messageOption"));

        // error_short

        index++;

        assertThat(monitorFile.get(index), is("ERROR fail_code = [FW000002] FW000002メッセージerror_short_messageOption" + Logger.LS));
        assertThat(appFile.get(index), containsString("ERROR ROO fail_code = [FW000002] FW000002メッセージerror_short_messageOption"));

        // error_min

        index++;

        assertThat(monitorFile.get(index), is("ERROR fail_code = [FW000003] FW000003メッセージerror_min_messageOption-\uD83D\uDE0E\uD83D\uDE0E:\uD83D\uDE0E\uD83D\uDE0E" + Logger.LS));
        assertThat(appFile.get(index), containsString("ERROR ROO fail_code = [FW000003] FW000003メッセージerror_min_messageOption"));

        // warn_short

        index++;

        assertThat(monitorFile.size(), is(index)); // WARNレベルは障害通知ログが出力されない。
        assertThat(appFile.get(index), containsString("WARN ROO fail_code = [FW000002] FW000002メッセージwarn_short_messageOption"));
    }

    /**
     * カスタムのフォーマッタで正しく出力できること。
     */
    @Test
    public void testCustomFailureLogFormatter() {

        OnMemoryLogWriter.clear();

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-custom.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        FailureLogUtil.logFatal(new IllegalArgumentException("fatal_full_error"), null, "FW000001", new Object[] {"fatal_full_messageOption"}, new Object[] {"fatal_full_logOption"});

        List<String> monitorFile = OnMemoryLogWriter.getMessages("writer.monitorLog");
        List<String> appFile = OnMemoryLogWriter.getMessages("writer.appLog");

        assertThat(monitorFile.get(0), is("FATAL [CustomFailureLogFormatter]fail_code = [FW000001] FW000001メッセージfatal_full_messageOption" + Logger.LS));
        assertThat(appFile.get(0), containsString("FATAL ROO [CustomFailureLogFormatter]fail_code = [FW000001] FW000001メッセージfatal_full_messageOption"));
    }

    /**
     * カスタムの処理対象データが正しくフォーマットされること。
     */
    @SuppressWarnings("serial")
    @Test
    public void testDataForCustomSettings() {

        OnMemoryLogWriter.clear();

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-settings-data.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        String failureCode = "FW000001";
        Object data = new TreeMap<String, String>() {
            {
                put("password", "pass");
                put("password1", "pass1");
                put("name", "hoge");
                put("age", "20");
            }
        };

        FailureLogUtil.logFatal(new IllegalArgumentException("error occurred."), data, failureCode, new Object[] {"fatal_full_messageOption"}, new Object[] {"fatal_full_logOption"});

        List<String> monitorFile = OnMemoryLogWriter.getMessages("writer.monitorLog");
        List<String> appFile = OnMemoryLogWriter.getMessages("writer.appLog");

        assertThat(monitorFile.get(0), is("FATAL fail_code:[FW000001] [FW000001メッセージfatal_full_messageOption] {age=20, name=hoge, password=*****, password1=*****}" + Logger.LS));
        assertThat(appFile.get(0), containsString("FATAL ROO fail_code<FW000001> <FW000001メッセージfatal_full_messageOption> {age=20, name=hoge, password=*****, password1=*****}"));
    }
}
