package nablarch.core.log.app;

import nablarch.core.date.BusinessDateProvider;
import nablarch.core.log.LogTestSupport;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.text.json.BasicJsonSerializationManager;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.text.json.JsonSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThrows;

/**
 * {@link ApplicationSettingJsonLogFormatter}の単体テスト。
 *
 * @author Tanaka Tomoyuki
 */
public class ApplicationSettingJsonLogFormatterTest extends LogTestSupport {
    /** テストで使用するプロパティファイルの共通プレフィックス */
    private static final String TEST_PROPERTIES_FILE_PREFIX = "classpath:nablarch/core/log/app/ApplicationSettingJsonLogFormatterTest/";

    private ApplicationSettingJsonLogFormatter sut;

    @Before
    public void before() {
        SystemRepository.clear();
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                Map<String, Object> components = new HashMap<String, Object>();

                components.put("foo", "FOO");
                components.put("bar", "BAR");
                components.put("businessDateProvider", new MockBusinessDateProvider("20210101"));

                return components;
            }
        });
        System.setProperty("nablarch.appLog.filePath", TEST_PROPERTIES_FILE_PREFIX + "app-log.properties");
    }

    @Test
    public void getAppSettingsLogMsgの先頭がJSONテキストであることを表すプレフィックスで始まること() {
        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsLogMsg(), startsWith("$JSON$"));
    }

    @Test
    public void getAppSettingsLogMsgのデフォルトの出力項目のテスト() {
        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsLogMsg().substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(1)),
            withJsonPath("$", hasKey("systemSettings"))
        )));
    }

    @Test
    public void systemSettingItemsが指定されていない場合_systemSettingsは空のオブジェクトで出力されることをテスト() {
        System.clearProperty("nablarch.appLog.filePath");

        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsLogMsg().substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(1)),
            withJsonPath("$", hasKey("systemSettings")),
            withJsonPath("$.systemSettings.*", hasSize(0))
        )));
    }

    @Test
    public void getAppSettingsLogMsgでtargetを指定した場合のテスト() {
        System.setProperty("applicationSettingLogFormatter.appSettingTargets", "systemSettings,,businessDate,businessDate");

        sut = new ApplicationSettingJsonLogFormatter();
        assertThat(sut.getAppSettingsLogMsg().substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasKey("systemSettings")),
            withJsonPath("$", hasKey("businessDate"))
        )));
    }

    @Test
    public void getAppSettingsLogMsgでのsystemSettingsの値のテスト() {
        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsLogMsg().substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.systemSettings", hasEntry("foo", "FOO")),
            withJsonPath("$.systemSettings", hasEntry("bar", "BAR"))
        )));
    }

    @Test
    public void getAppSettingsLogMsgでのbusinessDateの値のテスト() {
        System.setProperty("applicationSettingLogFormatter.appSettingTargets", "businessDate");

        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsLogMsg().substring("$JSON$".length()), isJson(
            withJsonPath("$", hasEntry("businessDate", "20210101")))
        );
    }

    @Test
    public void appSettingTargetsに不明なtargetが指定された場合はエラー() {
        System.setProperty("applicationSettingLogFormatter.appSettingTargets", "systemSettings,unknown");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                new ApplicationSettingJsonLogFormatter();
            }
        });

        assertThat(exception.getMessage(), is("[unknown] is unknown target. property name = [applicationSettingLogFormatter.appSettingTargets]"));
    }

    @Test
    public void getAppSettingsWithDateLogMsgの先頭がJSONテキストであることを表すプレフィックスで始まること() {
        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsWithDateLogMsg(), startsWith("$JSON$"));
    }

    @Test
    public void getAppSettingsWithDateLogMsgのデフォルトの出力項目のテスト() {
        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsWithDateLogMsg().substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasKey("systemSettings")),
            withJsonPath("$", hasKey("businessDate"))
        )));
    }

    @Test
    public void getAppSettingsWithDateLogMsgでtargetを指定した場合のテスト() {
        System.setProperty("applicationSettingLogFormatter.appSettingWithDateTargets", "businessDate");

        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsWithDateLogMsg().substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasKey("businessDate"))
        )));
    }

    @Test
    public void getAppSettingsWithDateLogMsgでのsystemSettingsの値のテスト() {
        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsWithDateLogMsg().substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.systemSettings", hasEntry("foo", "FOO")),
            withJsonPath("$.systemSettings", hasEntry("bar", "BAR"))
        )));
    }

    @Test
    public void getAppSettingsWithDateLogMsgでのbusinessDateの値のテスト() {
        sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsWithDateLogMsg().substring("$JSON$".length()), isJson(
            withJsonPath("$", hasEntry("businessDate", "20210101"))
        ));
    }

    @Test
    public void appSettingWithDateTargetsに不明なtargetが指定された場合はエラー() {
        System.setProperty("applicationSettingLogFormatter.appSettingWithDateTargets", "businessDate,unknown");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                new ApplicationSettingJsonLogFormatter();
            }
        });

        assertThat(exception.getMessage(), is("[unknown] is unknown target. property name = [applicationSettingLogFormatter.appSettingWithDateTargets]"));
    }

    /**
     * {@code structuredMessagePrefix}の指定ができることのテスト。
     */
    @Test
    public void testStructuredMessagePrefix() {
        System.setProperty("applicationSettingLogFormatter.structuredMessagePrefix", "@JSON@");

        final ApplicationSettingJsonLogFormatter sut = new ApplicationSettingJsonLogFormatter();

        assertThat(sut.getAppSettingsLogMsg(), startsWith("@JSON@"));
        assertThat(sut.getAppSettingsWithDateLogMsg(), startsWith("@JSON@"));
    }

    @Test
    public void JsonSerializationManagerが設定できることをテスト() {
        System.setProperty("applicationSettingLogFormatter.appSettingTargets", "businessDate,systemSettings");
        System.setProperty("applicationSettingLogFormatter.appSettingWithDateTargets", "businessDate");

        final ApplicationSettingJsonLogFormatter sut = new ApplicationSettingJsonLogFormatter() {
            @Override
            protected JsonSerializationManager createSerializationManager(JsonSerializationSettings settings) {
                assertThat(settings.getProp("appSettingTargets"), is("businessDate,systemSettings"));
                assertThat(settings.getProp("appSettingWithDateTargets"), is("businessDate"));
                return new MockJsonSerializationManager();
            }
        };

        assertThat(sut.getAppSettingsLogMsg(), is("$JSON$mock serialization"));
        assertThat(sut.getAppSettingsWithDateLogMsg(), is("$JSON$mock serialization"));
    }

    private static class MockJsonSerializationManager extends BasicJsonSerializationManager {

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

    @After
    public void after() {
        SystemRepository.clear();
    }

    /**
     * コンストラクタで指定された日付文字列を返すだけのテスト用{@link BusinessDateProvider}。
     */
    private static class MockBusinessDateProvider implements BusinessDateProvider {

        private final String date;

        private MockBusinessDateProvider(String date) {
            this.date = date;
        }

        @Override
        public String getDate() {
            return date;
        }

        @Override
        public String getDate(String segment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, String> getAllDate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDate(String segment, String date) {
            throw new UnsupportedOperationException();
        }
    }
}