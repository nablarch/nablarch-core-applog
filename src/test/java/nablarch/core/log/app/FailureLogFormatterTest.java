package nablarch.core.log.app;

import nablarch.core.ThreadContext;
import nablarch.core.log.LogTestSupport;
import nablarch.core.message.MessageUtil;
import nablarch.core.message.MockStringResourceHolder;
import nablarch.core.util.DateUtil;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Kiyohito Itoh
 */
public class FailureLogFormatterTest extends LogTestSupport {

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
    public void classSetup() throws Exception {
        repositoryResource.getComponentByType(MockStringResourceHolder.class).setMessages(MESSAGES);
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("param", new String[]{"10"});

        ThreadContext.clear();
    }

    @After
    public void clearThreadContext() throws Exception {
        ThreadContext.clear();
    }

    /**
     * 設定でデフォルトの障害コードが指定されない場合に例外がスローされること。
     */
    @Test
    public void testDefaultFailureCodeRequired() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-required.properties");
        try {
            new FailureLogFormatter();
            fail("defaultFailureCode is required.");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("failureLogFormatter.defaultFailureCode was not specified. failureLogFormatter.defaultFailureCode is required."));
        }
    }

    /**
     * 設定でデフォルトのメッセージが指定されない場合に例外がスローされること。
     */
    @Test
    public void testDefaultMessageRequired() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-required.properties");
        System.setProperty("failureLogFormatter.defaultFailureCode", "AAAAAA");
        try {
            new FailureLogFormatter();
            fail("defaultMessage is required.");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("failureLogFormatter.defaultMessage was not specified. failureLogFormatter.defaultMessage is required."));
        }
    }

    /**
     * 設定でデフォルトのメッセージにブランクが設定された場合にフォーマットされること。
     */
    @Test
    public void testDefaultMessageBlank() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-required.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        System.setProperty("failureLogFormatter.defaultFailureCode", "UNKNOWN");
        System.setProperty("failureLogFormatter.defaultMessage", "");

        FailureLogFormatter formatter = new FailureLogFormatter();

        String failureCode = "UNKNOWN"; // not found in table

        String message = formatter.formatNotificationMessage(null, null, failureCode, null);
        assertThat(message, is("fail_code = [UNKNOWN] "));

        message = formatter.formatAnalysisMessage(null, null, failureCode, null);
        assertThat(message, is("fail_code = [UNKNOWN] "));
    }

    /**
     * デフォルトのメッセージが出力されること。
     */
    @Test
    public void testDefaultMessage() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-settings.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        System.setProperty("failureLogFormatter.defaultFailureCode", "UNKNOWN");

        FailureLogFormatter formatter = new FailureLogFormatter();

        String failureCode = "UNKNOWN"; // not found in table

        String message = formatter.formatNotificationMessage(null, null, failureCode, null);
        assertThat(message, is("fail_code:[UNKNOWN] [an unexpected exception occurred.] null"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, null);
        assertThat(message, is("fail_code<UNKNOWN> <an unexpected exception occurred.> null"));
    }

    /**
     * スレッドコンテキストの言語情報からメッセージが構築されること。
     * @throws Exception
     */
    @Test
    public void testGetMessageDependThreadContext() throws Exception {
        ThreadContext.setLanguage(Locale.ENGLISH);

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-default_bc.properties");
        final FailureLogFormatter sut = new FailureLogFormatter();
        final String message = sut.getMessage("failure.code.unknown", null, null);

        assertThat(message, is("unknown error!!!"));
    }

    /**
     * スレッドコンテキストが存在しない場合デフォルトの言語情報からメッセージが構築されること
     * @throws Exception
     */
    @Test
    public void testGetMessageNotDependThreadContext() throws Exception {
        ThreadContext.clear();

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-default_bc.properties");
        final FailureLogFormatter sut = new FailureLogFormatter();
        final String message = sut.getMessage("failure.code.unknown", null, null);

        assertThat(message, is("未知のエラー"));
    }

    /**
     * デフォルトのフォーマットで正しくフォーマットされること。
     */
    @Test
    public void testDefaultFormat() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-default.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        FailureLogFormatter formatter = new FailureLogFormatter();

        // テーブルに存在する障害コードを指定した場合

        String failureCode = "FW000001";

        String message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("fail_code = [FW000001] FW000001メッセージnotification"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("fail_code = [FW000001] FW000001メッセージerror"));

        // テーブルに存在する障害コードを指定した場合

        failureCode = "FW000003";

        message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("fail_code = [FW000003] FW000003メッセージnotification"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("fail_code = [FW000003] FW000003メッセージerror"));

        // テーブルに存在しない障害コードを指定した場合

        failureCode = "NOT_FOUND"; // not found in table

        Throwable error = new IllegalArgumentException("exception for notification.");
        message = formatter.formatNotificationMessage(error, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("fail_code = [NOT_FOUND] failed to get the message to output the failure log. failureCode = [" + failureCode + "]"));

        error = new IllegalArgumentException("exception for error.");
        message = formatter.formatAnalysisMessage(error, null, failureCode, new Object[] {"error"});
        assertThat(message, is("fail_code = [NOT_FOUND] failed to get the message to output the failure log. failureCode = [" + failureCode + "]"));

        // 障害コードにnullを指定した場合

        failureCode = null;

        message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("fail_code = [MSG99999] an unexpected exception occurred."));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("fail_code = [MSG99999] an unexpected exception occurred."));

        // すべてのパラメータにnullを指定した場合

        message = formatter.formatNotificationMessage(null, null, null, null);
        assertThat(message, is("fail_code = [MSG99999] an unexpected exception occurred."));

        message = formatter.formatAnalysisMessage(null, null, null, null);
        assertThat(message, is("fail_code = [MSG99999] an unexpected exception occurred."));
    }

    /**
     * フォーマットの出力項目を入れ替えた場合に正しくフォーマットされること。
     */
    @Test
    public void testFormatSettingsSwap() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-settings.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        FailureLogFormatter formatter = new FailureLogFormatter();

        String failureCode = "FW000001";

        String message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("fail_code:[FW000001] [FW000001Messagenotification] null"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("fail_code<FW000001> <FW000001Messageerror> null"));
    }

    /**
     * フォーマットの出力項目を減らした場合に正しくフォーマットされること。
     */
    @Test
    public void testFormatSettingsReduce() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-settings.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        System.setProperty("failureLogFormatter.notificationFormat", "[$failureCode$]");
        System.setProperty("failureLogFormatter.analysisFormat", "[$failureCode$:$message$]");

        FailureLogFormatter formatter = new FailureLogFormatter();

        String failureCode = "FW000001";

        String message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW000001]"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW000001:FW000001Messageerror]"));
    }

    /**
     * フォーマットに連絡先情報を入れた場合に正しくフォーマットされること。
     */
    @Test
    public void testContactSettings() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-settings.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        System.setProperty("failureLogFormatter.notificationFormat", "[$failureCode$] <$contact$>");
        System.setProperty("failureLogFormatter.analysisFormat", "[$failureCode$:$message$] <$contact$>");

        // リクエストIDがnullの場合

        ThreadContext.setRequestId(null);

        FailureLogFormatter formatter = new FailureLogFormatter();

        String failureCode = "FW000001";

        String message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW000001] <null>"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW000001:FW000001Messageerror] <null>"));

        // 連絡先情報が存在しないリクエストIDの場合

        ThreadContext.setRequestId("NOT_FOUND");

        message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW000001] <null>"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW000001:FW000001Messageerror] <null>"));

        // 連絡先情報が存在するリクエストIDの場合

        ThreadContext.setRequestId("R000001");

        message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW000001] <AAA001>"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW000001:FW000001Messageerror] <AAA001>"));

        // 連絡先情報が存在するリクエストIDの場合

        ThreadContext.setRequestId("R000002");

        message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW000001] <AAA002>"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW000001:FW000001Messageerror] <AAA002>"));

        // 連絡先情報が存在するリクエストIDの場合

        ThreadContext.setRequestId("GAA0002");

        message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW000001] <AAA005>"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW000001:FW000001Messageerror] <AAA005>"));

        // 連絡先情報が存在するリクエストIDの場合(前方一致)

        ThreadContext.setRequestId("GA00002");

        message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW000001] <XXXXXX>"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW000001:FW000001Messageerror] <XXXXXX>"));
    }

    /**
     * アプリケーションの障害コードが変更されること。
     */
    @Test
    public void testAppFailureCodeSettings() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-settings.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        System.setProperty("failureLogFormatter.notificationFormat", "[$failureCode$]$message$");
        System.setProperty("failureLogFormatter.analysisFormat", "[$failureCode$:$message$]");

        FailureLogFormatter formatter = new FailureLogFormatter();

        // プロパティファイルに存在する障害コードの場合

        String failureCode = "UM000001";

        String message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[AP000001]AP000001Messagenotification"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[AP000001:AP000001Messageerror]"));

        // プロパティファイルに存在する障害コードの場合

        failureCode = "UM000003";

        message = formatter.formatNotificationMessage(null, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[AP000003]AP000003Messagenotification"));

        message = formatter.formatAnalysisMessage(null, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[AP000003:AP000003Messageerror]"));

        // プロパティファイルに存在しない障害コードの場合

        failureCode = "NOT_FOUND";

        Throwable error = new IllegalArgumentException("exception for notification.");
        message = formatter.formatNotificationMessage(error, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[NOT_FOUND]failed to get the message to output the failure log. failureCode = [" + failureCode + "]"));

        error = new IllegalArgumentException("exception for error.");
        message = formatter.formatAnalysisMessage(error, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[NOT_FOUND:failed to get the message to output the failure log. failureCode = [" + failureCode + "]]"));
    }

    /**
     * フレームワークの障害コードが変更されること。
     * プロパティ「nablarch」が含まれる場合。
     */
    @Test
    public void testFwFailureCodeSettings() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-settings.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        System.setProperty("failureLogFormatter.notificationFormat", "[$failureCode$]$message$");
        System.setProperty("failureLogFormatter.analysisFormat", "[$failureCode$:$message$]");

        FailureLogFormatter formatter = new FailureLogFormatter();

        // プロパティファイルに存在するパッケージの場合

        Throwable error = null;
        try {
            MessageUtil.getStringResource("NOT_FOUND");
            fail("must throw exception.");
        } catch (Throwable e) {
            error = e;
        }

        String failureCode = null;

        String message = formatter.formatNotificationMessage(error, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW000001]FW000001Messagenotification"));

        message = formatter.formatAnalysisMessage(error, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW000001:FW000001Messageerror]"));

        // プロパティファイルに存在するパッケージの場合

        try {
            nablarch.dummy.ExceptionThrower.throwException();
            fail("must throw exception.");
        } catch (Throwable e) {
            error = new RuntimeException("nablarch.dummy error", e);
        }

        message = formatter.formatNotificationMessage(error, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW000003]FW000003Messagenotification"));

        message = formatter.formatAnalysisMessage(error, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW000003:FW000003Messageerror]"));

        // プロパティファイルに存在するパッケージの場合(前方一致)

        try {
            new ExceptionThrower();
            fail("must throw exception.");
        } catch (Throwable e) {
            error = e;
        }

        message = formatter.formatNotificationMessage(error, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[FW888888]failed to get the message to output the failure log. failureCode = [FW888888]"));

        message = formatter.formatAnalysisMessage(error, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[FW888888:failed to get the message to output the failure log. failureCode = [FW888888]]"));
    }

    /**
     * フレームワークの障害コードが変更されること。
     * プロパティ「nablarch」が含まれない場合。
     */
    @Test
    public void testNoFwFailureCode() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-settings.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        System.setProperty("failureLogFormatter.notificationFormat", "[$failureCode$]$message$");
        System.setProperty("failureLogFormatter.analysisFormat", "[$failureCode$:$message$]");
        System.setProperty("failureLogFormatter.fwFailureCodeFilePath", "classpath:nablarch/core/log/app/failure-log-fwFailCode2.properties");

        FailureLogFormatter formatter = new FailureLogFormatter();

        // プロパティファイルに存在しないパッケージの場合

        Throwable error = null;
        try {
            new ExceptionThrower();
            fail("must throw exception.");
        } catch (Throwable e) {
            error = e;
        }

        String failureCode = null;


        String message = formatter.formatNotificationMessage(error, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[MSG99999]an unexpected exception occurred."));

        message = formatter.formatAnalysisMessage(error, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[MSG99999:an unexpected exception occurred.]"));

        // 例外の発生元がフレームワークでない場合

        try {
            DateUtil.getDate("nablarch/dummy");
            fail("must throw exception.");
        } catch (Throwable e) {
            error = e;
        }

        message = formatter.formatNotificationMessage(error, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[MSG99999]an unexpected exception occurred."));

        message = formatter.formatAnalysisMessage(error, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[MSG99999:an unexpected exception occurred.]"));

        // スタックトレースが存在しない例外の場合

        error = new StackTraceZeroException();

        message = formatter.formatNotificationMessage(error, null, failureCode, new Object[] {"notification"});
        assertThat(message, is("[MSG99999]an unexpected exception occurred."));

        message = formatter.formatAnalysisMessage(error, null, failureCode, new Object[] {"error"});
        assertThat(message, is("[MSG99999:an unexpected exception occurred.]"));
    }

    @SuppressWarnings("serial")
    public static final class StackTraceZeroException extends RuntimeException {
        public StackTraceElement[] getStackTrace() {
            return new StackTraceElement[0];
        }

    }

    /**
     * デフォルト設定で処理対象データが正しくフォーマットされること。
     */
    @SuppressWarnings("serial")
    @Test
    public void testDataOutputForDefaultSettings() {

        System.setProperty("nablarch.appLog.filePath", "classpath:nablarch/core/log/app/app-log-settings.properties");
        ThreadContext.setLanguage(Locale.JAPANESE);

        FailureLogFormatter formatter = new FailureLogFormatter();

        String failureCode = "FW000001";
        Object data;

        // 処理対象データがマップの場合

        data = new TreeMap<String, String>() {
            {
                put("requestId", "REQ_TEST");
                put("executionId", "EXE_TEST");
                put("userId", "USR_TEST");
            }
        };
        String message = formatter.formatNotificationMessage(null, data, failureCode, new Object[] {"notification"});
        assertThat(message, is("fail_code:[FW000001] [FW000001Messagenotification] {executionId=EXE_TEST, requestId=REQ_TEST, userId=USR_TEST}"));

        message = formatter.formatAnalysisMessage(null, data, failureCode, new Object[] {"error"});
        assertThat(message, is("fail_code<FW000001> <FW000001Messageerror> {executionId=EXE_TEST, requestId=REQ_TEST, userId=USR_TEST}"));

        // 処理対象データがオブジェクトの場合
        data = new Entity();

        message = formatter.formatNotificationMessage(null, data, failureCode, new Object[] {"notification"});
        assertThat(message, is("fail_code:[FW000001] [FW000001Messagenotification] ENT_TEST"));

        message = formatter.formatAnalysisMessage(null, data, failureCode, new Object[] {"error"});
        assertThat(message, is("fail_code<FW000001> <FW000001Messageerror> ENT_TEST"));

        // 処理対象がリストの場合
        // 先頭要素が出力されること
        data = new ArrayList<Object>() {
            {
                for (int i = 0; i < 5; i++) {
                    final int index = i;
                    add(new TreeMap<String, String>() {
                        {
                            put("requestId", "REQ_TEST" + index);
                            put("executionId", "EXE_TEST" + index);
                            put("userId", "USR_TEST" + index);
                        }
                    });
                }
            }
        };

        message = formatter.formatNotificationMessage(null, data, failureCode, new Object[] {"notification"});
        assertThat(message, is("fail_code:[FW000001] [FW000001Messagenotification] "
                             + "[{executionId=EXE_TEST0, requestId=REQ_TEST0, userId=USR_TEST0}, {executionId=EXE_TEST1, requestId=REQ_TEST1, userId=USR_TEST1}, {executionId=EXE_TEST2, requestId=REQ_TEST2, userId=USR_TEST2}, {executionId=EXE_TEST3, requestId=REQ_TEST3, userId=USR_TEST3}, {executionId=EXE_TEST4, requestId=REQ_TEST4, userId=USR_TEST4}]"));

        message = formatter.formatAnalysisMessage(null, data, failureCode, new Object[] {"error"});
        assertThat(message, is("fail_code<FW000001> <FW000001Messageerror> "
                             + "[{executionId=EXE_TEST0, requestId=REQ_TEST0, userId=USR_TEST0}, {executionId=EXE_TEST1, requestId=REQ_TEST1, userId=USR_TEST1}, {executionId=EXE_TEST2, requestId=REQ_TEST2, userId=USR_TEST2}, {executionId=EXE_TEST3, requestId=REQ_TEST3, userId=USR_TEST3}, {executionId=EXE_TEST4, requestId=REQ_TEST4, userId=USR_TEST4}]"));
    }

    public static final class Entity {
        public String getRequestId() {
            return "REQ_ENT";
        }
        public String getExecutionId() {
            return "EXE_ENT";
        }
        public String getUserId() {
            return "USR_ENT";
        }
        public String toString() {
            return "ENT_TEST";
        }
    }

    /**
     * 例外を発生するクラス。
     * nablarchパッケージ配下にあり、fail_codeの設定がされていないパッケージから例外が出る場合のテストに使用する。
     */
    public static class ExceptionThrower {
        public ExceptionThrower() {
            throw new RuntimeException("test exception");
        }
    }
}
