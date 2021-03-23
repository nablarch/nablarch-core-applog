package nablarch.core.log.app;

import nablarch.core.date.BusinessDateProvider;
import nablarch.core.log.LogItem;
import nablarch.core.log.LogTestSupport;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;

/**
 * {@link ApplicationSettingLogUtil}のテストクラス。
 *
 * @author hisaaki sioiri
 */
public class ApplicationSettingLogUtilTest extends LogTestSupport {

    /** テスト用のセットアップ処理 */
    @BeforeClass
    public static void setUpClass() {
        final BusinessDateProvider provider = new BusinessDateProvider() {
            public String getDate() {
                return "20111201";
            }

            public String getDate(String segment) {
                throw new UnsupportedOperationException("");
            }

            public Map<String, String> getAllDate() {
                throw new UnsupportedOperationException("");
            }

            public void setDate(String segment, String date) {
                throw new UnsupportedOperationException("");
            }
        };

        SystemRepository.load(new ObjectLoader() {
            public Map<String, Object> load() {
                Map<String, Object> loadData = new HashMap<String, Object>();
                loadData.put("businessDateProvider", provider);
                loadData.put("file.encoding", "utf-8");
                loadData.put("threadCount", "1000");
                loadData.put("key1", "name1");
                loadData.put("key2", "name2");
                loadData.put("key3", "name3\uD83D\uDE0E\uD83D\uDE0E");
                return loadData;
            }
        });
    }

    private void init() {
        System.setProperty("nablarch.appLog.filePath",
                "classpath:nablarch/core/log/app/empty-app-log.properties");
    }

    /**
     * {@link nablarch.core.log.app.ApplicationSettingLogUtil#getAppSettingsLogMsg()}
     * のテストケース。
     * <p/>
     * デフォルト設定での確認を行う。
     */
    @Test
    public void getAppSettingsLogMsg() {
        init();

        System.setProperty("applicationSettingLogFormatter.systemSettingItems",
                "file.encoding,key1, key3,   ,,,,null");

        String log = ApplicationSettingLogUtil.getAppSettingsLogMsg();
        System.out.println(
                "==================== actual log ====================\n" + log);

        assertThat(log, equalToIgnoringWhiteSpace("@@@@ APPLICATION SETTINGS @@@@\n"
                + "\tsystem settings = {\n"
                + "\t\tfile.encoding = [utf-8]\n"
                + "\t\tkey1 = [name1]\n"
                + "\t\tkey3 = [name3\uD83D\uDE0E\uD83D\uDE0E]\n"
                + "\t\tnull = [null]\n"         // リポジトリに存在しない値はnullとなる。
                + "\t}"));
    }

    /**
     * {@link nablarch.core.log.app.ApplicationSettingLogUtil#getAppSettingsLogMsg()}のテスト。
     * <p/>
     * フォーマットを任意の値に変更した場合。
     */
    @Test
    public void getAppSettingsLogOverrideFormat() {
        init();

        System.setProperty("applicationSettingLogFormatter.systemSettingItems",
                "file.encoding");
        System.setProperty("applicationSettingLogFormatter.appSettingFormat",
                "@@@@@@@@@@ log @@@@@@@@@@ systemSettings = [$systemSettings$]");

        String log = ApplicationSettingLogUtil.getAppSettingsLogMsg();
        System.out.println(
                "==================== actual log ====================\n" + log);

        assertThat(log, equalToIgnoringWhiteSpace("@@@@@@@@@@ log @@@@@@@@@@ systemSettings = [\n"
                + "\t\tfile.encoding = [utf-8]]"));

    }

    /**
     * {@link nablarch.core.log.app.ApplicationSettingLogUtil#getAppSettingsLogMsg()}のテスト。
     * <p/>
     * フォーマットを任意の値に変更した場合。
     * <p/>
     * ※「launcherLogFormatter.systemSettingItems」に空を設定した場合
     */
    @Test
    public void getAppSettingsLogOverrideFormat2() {
        init();

        System.setProperty("applicationSettingLogFormatter.appSettingFormat",
                "@@@@@@@@@@ log @@@@@@@@@@ systemSettings = [$systemSettings$]");

        String log = ApplicationSettingLogUtil.getAppSettingsLogMsg();
        System.out.println(
                "==================== actual log ====================\n" + log);

        assertThat(log, equalToIgnoringWhiteSpace(
                "@@@@@@@@@@ log @@@@@@@@@@ systemSettings = []"));
    }

    /**
     * {@link nablarch.core.log.app.ApplicationSettingLogUtil#getAppSettingsWithDateLogMsg()}
     * のテスト。
     */
    @Test
    public void getAppSettingsWithDateLogMsg() {
        init();
        System.setProperty("applicationSettingLogFormatter.systemSettingItems",
                "file.encoding, , , , ,                    , null-key,        key2");

        String log = ApplicationSettingLogUtil.getAppSettingsWithDateLogMsg();
        System.out.println(
                "==================== actual log ====================\n" + log);

        assertThat(log, equalToIgnoringWhiteSpace("@@@@ APPLICATION SETTINGS @@@@\n"
                + "\tsystem settings = {\n"
                + "\t\tfile.encoding = [utf-8]\n"
                + "\t\tnull-key = [null]\n"         // リポジトリに存在しない値はnullとなる。
                + "\t\tkey2 = [name2]\n"
                + "\t}\n"
                + "\tbusiness date = [20111201]"));

    }

    /**
     * {@link nablarch.core.log.app.ApplicationSettingLogUtil#getAppSettingsLogMsg()}のテスト。
     * <p/>
     * フォーマットを任意の値に変更した場合。
     */
    @Test
    public void getAppSettingsWithDateLogMsgOverrideFormat() {
        init();

        System.setProperty("applicationSettingLogFormatter.systemSettingItems",
                "file.encoding");
        System.setProperty("applicationSettingLogFormatter.appSettingWithDateFormat",
                "@@@@@@@@@@ log @@@@@@@@@@ systemSettings = [$systemSettings$] & date = [$businessDate$]");

        String log = ApplicationSettingLogUtil.getAppSettingsWithDateLogMsg();
        System.out.println(
                "==================== actual log ====================\n" + log);

        assertThat(log, equalToIgnoringWhiteSpace("@@@@@@@@@@ log @@@@@@@@@@ systemSettings = [\n"
                + "\t\tfile.encoding = [utf-8]] & date = [20111201]"));

    }

    /**
     * {@link nablarch.core.log.app.ApplicationSettingLogFormatter}を拡張した場合のテスト。
     * <p/>
     * 拡張したクラスのフォーマット、{@link LogItem}定義を使用してログフォーマットが行われていることを確認する。
     */
    @Test
    public void testOverrideFormatClass() {
        init();

        System.setProperty("applicationSettingLogFormatter.systemSettingItems",
                "key3");

        // フォーマットクラスを変更
        System.setProperty("applicationSettingLogFormatter.className",
                "nablarch.core.log.app.ApplicationSettingLogUtilTest$TestFormatter");

        assertThat(ApplicationSettingLogUtil.getAppSettingsLogMsg(),
                equalToIgnoringWhiteSpace(
                        "#### settings #### \n\t\tkey3 = [name3\uD83D\uDE0E\uD83D\uDE0E]:hogehogeSettings"));
        assertThat(ApplicationSettingLogUtil.getAppSettingsWithDateLogMsg(),
                equalToIgnoringWhiteSpace("#### date & settings ####"
                        + " 20111201:\n\t\tkey3 = [name3\uD83D\uDE0E\uD83D\uDE0E]:hogehogeSettings"));
    }

    /**
     * {@link nablarch.core.log.app.ApplicationSettingJsonLogFormatter}を拡張した場合のテスト。
     */
    @Test
    public void testApplicationSettingJsonLogFormatter() {
        init();

        System.setProperty("applicationSettingLogFormatter.systemSettingItems",
                "key3, ,");

        // フォーマットクラスを変更
        System.setProperty("applicationSettingLogFormatter.className",
                "nablarch.core.log.app.ApplicationSettingJsonLogFormatter");

        String message;

        message = ApplicationSettingLogUtil.getAppSettingsLogMsg();
        assertThat(message,
                is("$JSON${\"systemSettings\":{\"key3\":\"name3\uD83D\uDE0E\uD83D\uDE0E\"}}"));

        message = ApplicationSettingLogUtil.getAppSettingsWithDateLogMsg();
        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.systemSettings", hasEntry("key3", "name3\uD83D\uDE0E\uD83D\uDE0E")),
                withJsonPath("$", hasEntry("businessDate", "20111201")))));
    }

    public static class TestFormatter extends ApplicationSettingLogFormatter {

        @Override
        protected String getAppSettingsLogFormat() {
            return "#### settings #### $systemSettings$:$hoge$";
        }

        @Override
        protected Map<String, LogItem<ApplicationSettingLogContext>> getAppSettingsLogItems() {
            Map<String, LogItem<ApplicationSettingLogContext>> items = super
                    .getAppSettingsLogItems();
            items.put("$hoge$", new Hoge());
            return items;
        }

        @Override
        public String getAppSettingsWithDateLogFormat() {
            return "#### date & settings #### $date$:$systemSettings$:$hoge$";
        }

        @Override
        public Map<String, LogItem<ApplicationSettingLogContext>> getAppSettingsWithDateLogItems() {
            Map<String, LogItem<ApplicationSettingLogContext>> items = super
                    .getAppSettingsWithDateLogItems();
            items.put("$hoge$", new Hoge());
            items.put("$date$", new BusinessDate());
            return items;
        }

        private class Hoge implements LogItem<ApplicationSettingLogContext> {

            public String get(
                    ApplicationSettingLogContext context) {
                return "hogehogeSettings";
            }
        }
    }
}

