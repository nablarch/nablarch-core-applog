package nablarch.core.log.app;

import java.util.HashMap;
import java.util.Map;

import nablarch.core.date.BusinessDateUtil;
import nablarch.core.log.LogItem;
import nablarch.core.log.LogUtil;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * アプリケーション設定に関するログフォーマットを行うクラス。
 * <p/>
 * 主に、{@link nablarch.core.repository.SystemRepository}内の設定値をログ出力する際に使用する。
 * <p/>
 * ログ出力対象の設定値は、ログ設定ファイルに設定されたキー値によって決定される。
 * {@link SystemRepository}に格納されている値が、{@link String}以外のオブジェクトの場合には、文字列への変換({@code toString()})を行った結果の値をログに出力する。
 * <p/>
 * 以下に例を示す。
 * <pre>
 * ◆ログ設定ファイル
 * {@code
 * # 複数の設定値をログ出力したい場合には、以下のようにカンマ区切りで複数項目を列挙する。
 * applicationSettingLogFormatter.systemSettingItems = dbUser, dbUrl, threadCount
 * }
 * ◆ログ出力イメージ
 * dbUser = [scott]
 * dbUrl = [jdbc:oracle:thin:@localhost:1521:xe]
 * threadCount = [3]
 * </pre>
 *
 * @author hisaaki sioiri
 */
@Published(tag = "architect")
public class ApplicationSettingLogFormatter {

    /** プロパティ名のプレフィックス */
    public static final String PROPS_PREFIX = "applicationSettingLogFormatter.";

    /** アプリケーション設定のデフォルトフォーマット定義 */
    private static final String DEFAULT_APP_SETTINGS_FORMAT =
            "@@@@ APPLICATION SETTINGS @@@@"
                    + '\n' + '\t' + "system settings = {"
                    + "$systemSettings$"
                    + '\n' + '\t' + '}';

    /** アプリケーション設定と業務日付のデフォルトフォーマット定義 */
    private static final String DEFAULT_APP_SETTINGS_WITH_DATE_FORMAT =
            "@@@@ APPLICATION SETTINGS @@@@"
                    + '\n' + '\t' + "system settings = {"
                    + "$systemSettings$"
                    + '\n' + '\t' + '}'
                    + '\n' + '\t' + "business date = [$businessDate$]";

    /** アプリケーション設定の出力項目 */
    private Map<String, LogItem<ApplicationSettingLogContext>> appLogItem;

    /** アプリケーション設定及び業務日付の出力項目 */
    private Map<String, LogItem<ApplicationSettingLogContext>> appWithDateLogItem;

    /**
     * コンストラクタ。
     */
    public ApplicationSettingLogFormatter() {
        initialize();
    }

    /**
     * 初期化処理。
     */
    protected void initialize() {
        appLogItem = getAppSettingsLogItems();
        appWithDateLogItem = getAppSettingsWithDateLogItems();
    }

    /**
     * アプリケーション設定に関するログメッセージを生成する。
     * <p/>
     * {@link #getAppSettingsLogFormat}から取得したログフォーマットに従いログメッセージの生成を行う。
     * ログ出力対象は、アプリケーション設定はプロパティファイル("classpath:app-log.properties")
     * に記載されている項目となる。<br>
     * システムプロパティ("nablarch.appLog.filePath")が指定されている場合は、
     * システムプロパティで指定されたパスを使用する。
     *
     *
     *
     *
     * @return 生成したアプリケーション設定ログ
     */
    public String getAppSettingsLogMsg() {
        LogItem<ApplicationSettingLogContext>[] items = LogUtil
                .createFormattedLogItems(appLogItem, getAppSettingsLogFormat());
        return LogUtil.formatMessage(items, new ApplicationSettingLogContext());
    }

    /**
     * アプリケーション設定及び業務日付に関するログメッセージを生成する。
     * <p/>
     * {@link #getAppSettingsWithDateLogFormat}から取得したログフォーマットに従いログメッセージの生成を行う。
     * 業務日付は{@link BusinessDateUtil#getDate()}を利用して取得する。
     *
     * @return 生成したアプリケーション設定ログ
     */
    public String getAppSettingsWithDateLogMsg() {
        LogItem<ApplicationSettingLogContext>[] items = LogUtil
                .createFormattedLogItems(appWithDateLogItem,
                        getAppSettingsWithDateLogFormat());
        return LogUtil.formatMessage(items, new ApplicationSettingLogContext());
    }

    /**
     * アプリケーション設定ログのフォーマットを取得する。
     * <p/>
     * 設定ファイル(nablarch.core.log.app.AppLogUtil#getProps())にログフォーマットが指定されている場合は、
     * そのフォーマットを返却する。
     * 設定されていない場合には、デフォルトのフォーマットを使用する。
     *
     * @return 生成したフォーマット
     */
    protected String getAppSettingsLogFormat() {
        String overrideFormat = AppLogUtil.getProps().get(
                PROPS_PREFIX + "appSettingFormat");
        if (overrideFormat == null) {
            return DEFAULT_APP_SETTINGS_FORMAT;
        }
        return overrideFormat;
    }

    /**
     * アプリケーション設定用のログ出力項目を生成する。
     *
     * @return 生成したログ出力項目
     */
    protected Map<String, LogItem<ApplicationSettingLogContext>> getAppSettingsLogItems() {
        Map<String, LogItem<ApplicationSettingLogContext>> logItem
                = new HashMap<String, LogItem<ApplicationSettingLogContext>>();
        logItem.put("$systemSettings$", new SystemSettings());
        return logItem;
    }

    /**
     * アプリケーション設定及び業務日付ログ用のログフォーマットを取得する。
     * <p/>
     * 設定ファイル(nablarch.core.log.app.AppLogUtil#getProps())にログフォーマットが指定されている場合は、
     * そのフォーマットを返却する。
     * 設定されていない場合には、デフォルトのフォーマットを使用する。
     *
     * @return 生成したフォーマット
     */
    public String getAppSettingsWithDateLogFormat() {
        String overrideFormat = AppLogUtil.getProps().get(
                PROPS_PREFIX + "appSettingWithDateFormat");
        if (overrideFormat == null) {
            return DEFAULT_APP_SETTINGS_WITH_DATE_FORMAT;
        }
        return overrideFormat;
    }

    /**
     * アプリケーション設定及び日付出力用のログ出力項目を生成する。
     *
     * @return ログ出力項目
     */
    public Map<String, LogItem<ApplicationSettingLogContext>> getAppSettingsWithDateLogItems() {
        Map<String, LogItem<ApplicationSettingLogContext>> logItem
                = new HashMap<String, LogItem<ApplicationSettingLogContext>>();
        logItem.put("$businessDate$", new BusinessDate());
        logItem.put("$systemSettings$", new SystemSettings());
        return logItem;
    }

    /**
     * 業務日付を取得する。
     * <p/>
     * 業務日付は、{@link nablarch.core.date.BusinessDateUtil}から取得する。
     *
     * @author hisaaki sioiri
     */
    protected static class BusinessDate implements LogItem<ApplicationSettingLogContext> {

        /**
         * 業務日付を取得する。
         *
         * @param context ログの出力項目の取得に使用するコンテキスト
         * @return 業務日付
         */
        public String get(ApplicationSettingLogContext context) {
            return BusinessDateUtil.getDate();
        }
    }

    /**
     * システム設定値を取得する。
     * <p/>
     * ログ設定から取得したログ出力対象のシステム設定キーを元に、{@link nablarch.core.repository.SystemRepository#get(String)}から設定値を取得し、
     * メッセージを生成する。
     *
     * @author hisaaki sioiri
     */
    protected static class SystemSettings implements LogItem<ApplicationSettingLogContext> {

        /**
         * システム設定値を取得する。
         *
         * @param context ログの出力項目の取得に使用するコンテキスト
         * @return システム設定値を取得する。
         */
        public String get(ApplicationSettingLogContext context) {
            String systemSettingItems = AppLogUtil.getProps().get(
                    PROPS_PREFIX + "systemSettingItems");

            if (StringUtil.isNullOrEmpty(systemSettingItems)) {
                return "";
            }

            String[] strings = systemSettingItems.split(",");
            StringBuilder result = new StringBuilder();
            for (String str : strings) {
                String key = str.trim();
                if (StringUtil.isNullOrEmpty(key)) {
                    continue;
                }
                Object object = SystemRepository.get(key);
                result.append('\n');
                result.append("\t\t");
                result.append(key);
                result.append(" = [");
                result.append(object);
                result.append(']');
            }
            return result.toString();
        }
    }

    /**
     * アプリケーション設定ログを出力するために必要な情報を保持するクラス。
     * <p/>
     * デフォルト実装では、何も保持しない。
     *
     * @author hisaaki sioiri
     */
    protected static class ApplicationSettingLogContext {

    }
}

