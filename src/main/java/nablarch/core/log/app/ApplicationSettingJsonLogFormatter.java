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
 * 基本的な仕様については、継承元クラスの{@link ApplicationSettingLogFormatter}を参照。
 * <p/>
 * @author Shuji Kitamura
 */
@Published(tag = "architect")
public class ApplicationSettingJsonLogFormatter extends ApplicationSettingLogFormatter {

    /** 各種ログのJSONフォーマット支援オブジェクト */
    private final JsonLogFormatterSupport support = new JsonLogFormatterSupport(PROPS_PREFIX);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAppSettingsLogMsg() {
        Map<String, Object> appSettings = new HashMap<String, Object>();
        appSettings.put("systemSettings", getSystemSettings());
        return support.getStructuredMessage(appSettings);
    }

    /**
     * {@inheritDoc}
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
