package nablarch.core.log.app;

import nablarch.core.log.basic.JsonLogObjectBuilder;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.util.FileUtil;
import nablarch.core.util.ObjectUtil;
import nablarch.core.util.StringUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 各種ログのJSONフォーマットを支援するクラスです。
 * @author Shuji Kitamura
 */
public class JsonLogFormatterSupport {

    /** 出力項目のプロパティ名 */
    private static final String PROPS_TARGETS = "targets";

    /** messageを構造化されていることを示す接頭区のプロパティ名 */
    private static final String PROPS_STRUCTURED_MESSAGE_PREFIX = "structuredMessagePrefix";

    /** messageを構造化されていることを示す接頭区のデフォルト値 */
    private static final String DEFAULT_STRUCTURED_MESSAGE_PREFIX = "$JSON$";

    /** Jsonのシリアライズに使用する管理クラス名のプロパティ名 */
    private static final String PROPS_SERIALIZATION_MANAGER_CLASS_NAME = "jsonSerializationManagerClassName";

    /** Jsonのシリアライズに使用する管理クラス名のデフォルト値 */
    private static final String DEFAULT_SERIALIZATION_MANAGER_CLASS_NAME = "nablarch.core.text.json.JsonSerializationManager";

    /** 各種ログの設定内容から値と取得するのに使用するプロパティ名のプリフィックス */
    private final String prefix;

    /** デフォルトの出力項目 */
    private final String defaultTargets;

    /** Jsonのシリアライズに使用する管理クラス */
    private final JsonSerializationManager serializationManager;

    /**
     * コンストラクタ。
     * @param prefix 各種ログの設定内容から値と取得するのに使用するプロパティ名のプリフィックス、
     */
    public JsonLogFormatterSupport(String prefix) {
        this(prefix, null);
    }

    /**
     * コンストラクタ。
     * @param prefix 各種ログの設定内容から値と取得するのに使用するプロパティ名のプリフィックス、
     * @param defaultTargets デフォルトの出力項目。
     */
    public JsonLogFormatterSupport(String prefix, String defaultTargets) {
        this.prefix = prefix;
        this.defaultTargets = defaultTargets;
        this.serializationManager = ObjectUtil.createInstance(
                getSerializationManagerClassName());
        JsonSerializationSettings settings = new JsonSerializationSettings(AppLogUtil.getProps(), prefix, AppLogUtil.getFilePath());
        this.serializationManager.initialize(settings);
    }

    /**
     * 出力項目のプロパティ名を取得する。
     * @return 出力項目
     */
    public String getTargetsProperty() {
        return prefix + PROPS_TARGETS;
    }

    /**
     * 出力項目を取得する。
     * @return 出力項目
     */
    public String getTargets() {
        String targets = AppLogUtil.getProps().get(getTargetsProperty());
        return !StringUtil.isNullOrEmpty(targets) ? targets : defaultTargets;
    }

    /**
     * 各種ログの設定情報から構造化済みメッセージを示す接頭区を取得する。
     * @return 構造化済みメッセージを示す接頭区
     */
    public String getStructuredMessagePrefix() {
        String messagePrefix = AppLogUtil.getProps().get(prefix + PROPS_STRUCTURED_MESSAGE_PREFIX);
        return !StringUtil.isNullOrEmpty(messagePrefix) ? messagePrefix : DEFAULT_STRUCTURED_MESSAGE_PREFIX;
    }

    /**
     * Jsonのシリアライズに使用する管理クラス名を取得する。
     * @return Jsonのシリアライズに使用する管理クラス名
     */
    public String getSerializationManagerClassName() {
        String className = AppLogUtil.getProps().get(prefix + PROPS_SERIALIZATION_MANAGER_CLASS_NAME);
        return !StringUtil.isNullOrEmpty(className) ? className : DEFAULT_SERIALIZATION_MANAGER_CLASS_NAME;
    }

    /**
     * JSONのシリアライズを管理するクラスを取得します。
     * @return JSONのシリアライズを管理するクラス
     */
    public JsonSerializationManager getSerializationManager() {
        return serializationManager;
    }

    /**
     * ログコンテキストからシリアライズ用のオブジェクトを作成する。
     * @param targets 出力項目のビルダー
     * @param context ログコンテキスト
     * @return シリアライズ用のオブジェクト
     */
    public <CTX> Map<String, Object> createStructuredObject(List<JsonLogObjectBuilder<CTX>> targets, CTX context) {
        Map<String, Object> structuredObject = new HashMap<String, Object>();
        for (JsonLogObjectBuilder<CTX> builder: targets) {
            builder.build(structuredObject, context);
        }
        return structuredObject;
    }

    /**
     * 構造化されたmessageを生成する。
     * @return 生成したmessage
     */
    public <CTX> String getStructuredMessage(List<JsonLogObjectBuilder<CTX>> targets, CTX context) {
        Map<String, Object> o = createStructuredObject(targets, context);
        return getStructuredMessage(o);
    }

    /**
     * 構造化されたmessageを生成する。
     * @return 生成したmessage
     */
    public String getStructuredMessage(Object o) {
        StringWriter writer = null;
        try {
            writer = new StringWriter();
            writer.write(getStructuredMessagePrefix());
            getSerializationManager().getSerializer(o).serialize(writer, o);
            return writer.toString();
        } catch (IOException e) {
            // StringWriterはIOExceptionをスローしない
            e.printStackTrace();
        } finally {
            FileUtil.closeQuietly(writer);
        }
        return "format error";
    }

}
