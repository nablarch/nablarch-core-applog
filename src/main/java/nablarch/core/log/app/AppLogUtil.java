package nablarch.core.log.app;

import java.util.Map;

import nablarch.core.log.LogSettings;
import nablarch.core.log.LogUtil;
import nablarch.core.log.LogUtil.ObjectCreator;

/**
 * 各種ログ出力の実装を助けるユーティリティ。
 * @author Kiyohito Itoh
 */
public final class AppLogUtil {
    
    /** 隠蔽コンストラクタ */
    private AppLogUtil() {
    }
    
    /** {@link LogSettings}を生成する{@link ObjectCreator} */
    private static final ObjectCreator<LogSettings> LOG_SETTINGS_CREATOR = new ObjectCreator<LogSettings>() {
        public LogSettings create() {
            String filePath = getFilePath();
            return new LogSettings(filePath);
        }
    };
    
    /**
     * 各種ログ出力の設定情報を取得する。<br>
     * 設定情報はプロパティファイル("classpath:app-log.properties")から取得する。<br>
     * システムプロパティ("nablarch.appLog.filePath")が指定されている場合は、
     * システムプロパティで指定されたパスを使用する。
     * @return 各種ログ出力の設定情報
     */
    public static Map<String, String> getProps() {
        LogSettings settings = LogUtil.getObjectBoundToClassLoader(LOG_SETTINGS_CREATOR);
        return settings.getProps();
    }

    public static final String getFilePath() {
        return System.getProperty("nablarch.appLog.filePath", "classpath:app-log.properties");
    }
}
