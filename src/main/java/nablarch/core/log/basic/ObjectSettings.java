package nablarch.core.log.basic;

import java.util.HashMap;
import java.util.Map;

import nablarch.core.log.LogSettings;
import nablarch.core.util.annotation.Published;

/**
 * ログ出力機能の設定からオブジェクトに対する設定を抜き出して保持するクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class ObjectSettings {
    
    /** ログ出力の設定 */
    private LogSettings settings;
    
    /** プロパティファイル内で指定されるオブジェクトの名称 */
    private String name;

    /** オブジェクトに対する設定 */
    private Map<String, String> props;
    
    /**
     * コンストラクタ。
     * @param settings ログ出力の設定
     * @param name プロパティファイル内で指定されるオブジェクトの名称
     */
    public ObjectSettings(LogSettings settings, String name) {
        this.settings = settings;
        this.name = name;
        props = getSettingsByPrefix(settings, name + ".");
    }

    /**
     * ログ出力の設定を取得する。
     * @return ログ出力の設定内容
     */
    protected LogSettings getLogSettings() {
        return settings;
    }
    
    /**
     * ログ出力の設定を読み込む際に使用したファイルパスを取得する。
     * @return ログ出力の設定を読み込む際に使用したファイルパス
     */
    protected String getFilePath() {
        return settings.getFilePath();
    }
    
    /**
     * プロパティファイル内で指定されるオブジェクトの名称を取得する。
     * @return プロパティファイル内で指定されるオブジェクトの名称
     */
    protected String getName() {
        return name;
    }

    /**
     * オブジェクトに対する設定を取得する。
     * @return オブジェクトに対する設定
     */
    Map<String, String> getProps() {
        return props;
    }
    
    /**
     * 指定されたプレフィックスにマッチする設定を取得する。
     * @param settings ログ出力の設定
     * @param prefix プレフィックス
     * @return プレフィックスにマッチする設定
     */
    protected Map<String, String> getSettingsByPrefix(LogSettings settings, String prefix) {
        Map<String, String> settingsForPrefix = new HashMap<String, String>();
        for (Map.Entry<String, String> prop : settings.getProps().entrySet()) {
            if (prop.getKey().startsWith(prefix)) {
                settingsForPrefix.put(prop.getKey(), prop.getValue());
            }
        }
        return settingsForPrefix;
    }
    

    /**
     * 必須でないプロパティを取得する。
     * @param propName オブジェクトに対するプロパティ名
     * @return プロパティに設定された値。プロパティが存在しない場合は<code>null</code>
     */
    public String getProp(String propName) {
        return getProps().get(name + "." + propName);
    }
    
    /**
     * 必須プロパティを取得する。
     * @param propName オブジェクトに対するプロパティ名
     * @return プロパティに設定された値
     * @throws IllegalArgumentException プロパティが存在しない場合
     */
    public String getRequiredProp(String propName) throws IllegalArgumentException {
        String propValue = getProp(propName);
        if (propValue == null || propValue.length() == 0) {
            throw new IllegalArgumentException(
                "'" + propName + "' was not specified. file path = [" + getFilePath() + "], name = [" + getName() + "]");
        }
        return propValue;
    }
}
