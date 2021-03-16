package nablarch.core.log.app;

import nablarch.core.date.BusinessDateUtil;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import java.util.HashMap;
import java.util.Map;

/**
 * アプリケーション設定に関するメッセージをJSON形式でフォーマットするクラス。
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
 * </pre>
 *
 * @author Shuji Kitamura
 */
@Published(tag = "architect")
public class ApplicationSettingJsonLogFormatter extends ApplicationSettingLogFormatter {

    private final JsonLogFormatterSupport support = new JsonLogFormatterSupport(PROPS_PREFIX);

    /**
     * アプリケーション設定に関するログメッセージを生成する。
     * <p/>
     * ログ出力対象は、アプリケーション設定はプロパティファイル("classpath:app-log.properties")
     * に記載されている項目となる。<br>
     * システムプロパティ("nablarch.appLog.filePath")が指定されている場合は、
     * システムプロパティで指定されたパスを使用する。
     * @return 生成したアプリケーション設定ログ
     */
    @Override
    public String getAppSettingsLogMsg() {
        Map<String, Object> appSettings = new HashMap<String, Object>();
        appSettings.put("systemSettings", getSystemSettings());
        return support.getStructuredMessage(appSettings);
    }

    /**
     * アプリケーション設定及び業務日付に関するログメッセージを生成する。
     * <p/>
     * 業務日付は{@link BusinessDateUtil#getDate()}を利用して取得する。
     * @return 生成したアプリケーション設定ログ
     */
    @Override
    public String getAppSettingsWithDateLogMsg() {
        Map<String, Object> appSettings = new HashMap<String, Object>();
        appSettings.put("systemSettings", getSystemSettings());
        appSettings.put("businessDate", BusinessDateUtil.getDate());
        return support.getStructuredMessage(appSettings);
    }

    /**
     * システム設定の項目名を取得する。
     * @return システム設定の項目名
     */
    protected String getSystemSettingItems() {
        return AppLogUtil.getProps().get(PROPS_PREFIX + "systemSettingItems");
    }

    /**
     * システム設定を取得する。
     * @return システム設定
     */
    protected Map<String, Object> getSystemSettings() {
        String systemSettingItems = getSystemSettingItems();

        String[] strings = systemSettingItems.split(",");
        Map<String, Object> result = new HashMap<String, Object>();
        for (String str : strings) {
            String key = str.trim();
            if (StringUtil.isNullOrEmpty(key)) {
                continue;
            }
            result.put(key, SystemRepository.get(key));
        }
        return result;
    }
}
