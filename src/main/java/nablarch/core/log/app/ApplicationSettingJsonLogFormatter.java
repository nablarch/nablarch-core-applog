package nablarch.core.log.app;

import nablarch.core.date.BusinessDateUtil;
import nablarch.core.log.basic.JsonLogObjectBuilder;
import nablarch.core.repository.SystemRepository;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * アプリケーション設定に関するメッセージをJSON形式でフォーマットするクラス。
 * <p/>
 * 基本的な仕様については、継承元クラスの{@link ApplicationSettingLogFormatter}を参照。
 * <p/>
 * @author Shuji Kitamura
 */
@Published(tag = "architect")
public class ApplicationSettingJsonLogFormatter extends ApplicationSettingLogFormatter {
    /** {@link #getAppSettingsLogMsg()}のターゲットを設定するプロパティの名前 */
    private static final String PROPS_APP_SETTINGS_TARGETS = PROPS_PREFIX + "appSettingTargets";
    /** {@link #getAppSettingsWithDateLogMsg()}のターゲットを設定するプロパティの名前 */
    private static final String PROPS_APP_SETTINGS_WITH_DATE_TARGETS = PROPS_PREFIX + "appSettingWithDateTargets";

    /** {@link #getAppSettingsLogMsg()}のデフォルトターゲット */
    private static final String DEFAULT_TARGETS_APP_SETTINGS = "systemSettings";
    /** {@link #getAppSettingsWithDateLogMsg()}のデフォルトターゲット */
    private static final String DEFAULT_TARGETS_APP_SETTINGS_WITH_DATE = "systemSettings,businessDate";

    /** システム設定値の項目名 */
    private static final String TARGET_NAME_SYSTEM_SETTING = "systemSettings";
    /** 業務日付の項目名 */
    private static final String TARGET_NAME_BUSINESS_DATE = "businessDate";

    /** {@link #getAppSettingsLogMsg()}の項目取得オブジェクトのリスト */
    private List<JsonLogObjectBuilder<ApplicationSettingLogContext>> appSettingsTargets;
    /** {@link #getAppSettingsWithDateLogMsg()}の項目取得オブジェクトのリスト */
    private List<JsonLogObjectBuilder<ApplicationSettingLogContext>> appSettingsWithDateTargets;

    /** 各種ログのJSONフォーマット支援オブジェクト */
    private final JsonLogFormatterSupport support
            = new JsonLogFormatterSupport(
                    new JsonSerializationSettings(AppLogUtil.getProps(), PROPS_PREFIX, AppLogUtil.getFilePath()));

    @Override
    protected void initialize() {
        this.appSettingsTargets
            = getStructuredTargets(PROPS_APP_SETTINGS_TARGETS, DEFAULT_TARGETS_APP_SETTINGS);
        this.appSettingsWithDateTargets
            = getStructuredTargets(PROPS_APP_SETTINGS_WITH_DATE_TARGETS, DEFAULT_TARGETS_APP_SETTINGS_WITH_DATE);
    }

    /**
     * ログ出力項目を取得する。
     * @param propName ターゲットを取得するためのプロパティ名
     * @param defaultTargets ターゲットの設定値が取得できない場合に使用するデフォルト値
     * @return ログ出力項目
     */
    protected List<JsonLogObjectBuilder<ApplicationSettingLogContext>> getStructuredTargets(String propName, String defaultTargets) {
        String targetsConfiguration = AppLogUtil.getProps().get(propName);
        String targets;
        if (StringUtil.isNullOrEmpty(targetsConfiguration)) {
            targets = defaultTargets;
        } else {
            targets = targetsConfiguration;
        }

        List<JsonLogObjectBuilder<ApplicationSettingLogContext>> structuredTargets
                = new ArrayList<JsonLogObjectBuilder<ApplicationSettingLogContext>>();

        String[] tokens = targets.split(",");
        Set<String> processedTargets = new HashSet<String>(tokens.length);
        for (String token : tokens) {
            String target = token.trim();
            if (!StringUtil.isNullOrEmpty(target) && !processedTargets.contains(target)) {
                processedTargets.add(target);

                if (TARGET_NAME_SYSTEM_SETTING.equals(target)) {
                    structuredTargets.add(new SystemSettingsBuilder());
                } else if (TARGET_NAME_BUSINESS_DATE.equals(target)) {
                    structuredTargets.add(new BusinessDateBuilder());
                } else {
                    throw new IllegalArgumentException(
                        String.format("[%s] is unknown target. property name = [%s]", target, propName)
                    );
                }
            }
        }

        return structuredTargets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAppSettingsLogMsg() {
        return support.getStructuredMessage(appSettingsTargets, new ApplicationSettingLogContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAppSettingsWithDateLogMsg() {
        return support.getStructuredMessage(appSettingsWithDateTargets, new ApplicationSettingLogContext());
    }

    /**
     * システム設定値を取得するクラス。
     */
    public static class SystemSettingsBuilder implements JsonLogObjectBuilder<ApplicationSettingLogContext> {

        @Override
        public void build(Map<String, Object> structuredObject, ApplicationSettingLogContext context) {
            String systemSettingItems = AppLogUtil.getProps().get(PROPS_PREFIX + "systemSettingItems");

            String[] strings = systemSettingItems.split(",");
            Map<String, Object> systemSettings = new HashMap<String, Object>();
            for (String str : strings) {
                String key = str.trim();
                if (!StringUtil.isNullOrEmpty(key)) {
                    systemSettings.put(key, SystemRepository.get(key));
                }
            }

            structuredObject.put("systemSettings", systemSettings);
        }
    }

    /**
     * 業務日付を取得するクラス。
     */
    public static class BusinessDateBuilder implements JsonLogObjectBuilder<ApplicationSettingLogContext> {

        @Override
        public void build(Map<String, Object> structuredObject, ApplicationSettingLogContext context) {
            structuredObject.put("businessDate", BusinessDateUtil.getDate());
        }
    }
}
