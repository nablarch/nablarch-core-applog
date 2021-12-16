package nablarch.core.log.app;

import nablarch.core.log.basic.JsonLogObjectBuilder;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.util.FileUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 各種ログのJSONフォーマットを支援するクラスです。
 * @author Shuji Kitamura
 */
@Published(tag = "architect")
public class JsonLogFormatterSupport {

    /** messageを構造化されていることを示す接頭辞のプロパティ名 */
    private static final String PROPS_STRUCTURED_MESSAGE_PREFIX = "structuredMessagePrefix";

    /** messageを構造化されていることを示す接頭辞のデフォルト値 */
    static final String DEFAULT_STRUCTURED_MESSAGE_PREFIX = "$JSON$";

    /** Jsonのシリアライズに関する設定 */
    private final JsonSerializationSettings settings;

    /** Jsonのシリアライズに使用する管理クラス */
    private final JsonSerializationManager serializationManager;

    /**
     * コンストラクタ。
     * @param serializationManager {@link JsonSerializationManager}の実装オブジェクト
     * @param settings Jsonのシリアライズに関する設定
     */
    public JsonLogFormatterSupport(JsonSerializationManager serializationManager, JsonSerializationSettings settings) {
        this.settings = settings;
        this.serializationManager = serializationManager;
        this.serializationManager.initialize(settings);
    }

    /**
     * 各種ログの設定情報から構造化済みメッセージを示す接頭辞を取得する。
     * @return 構造化済みメッセージを示す接頭辞
     */
    private String getStructuredMessagePrefix() {
        String messagePrefix = settings.getProp(PROPS_STRUCTURED_MESSAGE_PREFIX);
        return !StringUtil.isNullOrEmpty(messagePrefix) ? messagePrefix : DEFAULT_STRUCTURED_MESSAGE_PREFIX;
    }

    /**
     * JSONのシリアライズを管理するクラスを取得します。
     * @return JSONのシリアライズを管理するクラス
     */
    private JsonSerializationManager getSerializationManager() {
        return serializationManager;
    }

    /**
     * ログコンテキストからシリアライズ用のオブジェクトを作成する。
     * @param builders 出力項目のビルダー
     * @param context ログコンテキスト
     * @return シリアライズ用のオブジェクト
     */
    private <CTX> Map<String, Object> createStructuredObject(List<JsonLogObjectBuilder<CTX>> builders, CTX context) {
        Map<String, Object> structuredObject = new HashMap<String, Object>();
        for (JsonLogObjectBuilder<CTX> builder: builders) {
            builder.build(structuredObject, context);
        }
        return structuredObject;
    }

    /**
     * 構造化されたmessageを生成する。
     * @param builders 出力項目のビルダー
     * @param context 構造化するコンテキスト
     * @return 生成したmessage
     */
    public <CTX> String getStructuredMessage(List<JsonLogObjectBuilder<CTX>> builders, CTX context) {
        Map<String, Object> o = createStructuredObject(builders, context);
        return getStructuredMessage(o);
    }

    /**
     * 構造化されたmessageを生成する。
     * @param o 構造化対象のオブジェクト
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
            throw new RuntimeException(e);
        } finally {
            FileUtil.closeQuietly(writer);
        }
    }

}
