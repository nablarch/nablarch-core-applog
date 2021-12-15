package nablarch.core.log.basic;

import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.MapToJsonSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * applog用に拡張したMapをJSONにシリアライズするクラス。
 * <p>
 * このクラスは、Mapの値に{@link RawJsonObjectMembers}が有る場合をサポートするように
 * {@link MapToJsonSerializer}を拡張している。
 * </p>
 * @author Shuji Kitamura
 */
public class AppLogMapToJsonSerializer extends MapToJsonSerializer {

    /**
     * コンストラクタ。
     * @param manager シリアライズ管理クラス
     */
    public AppLogMapToJsonSerializer(JsonSerializationManager manager) {
        super(manager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * メンバーの値が {@link RawJsonObjectMembers} の場合に独自の出力処理を行い、
     * それ以外の値の場合は従来の出力処理を呼び出している。
     * </p>
     *
     * @param writer 出力先の Writer
     * @param member 出力するメンバー
     * @throws IOException 出力時にエラーが発生した場合
     */
    @Override
    protected void writeMember(Writer writer, Map.Entry<?, ?> member) throws IOException {
        Object memberValue = member.getValue();

        if (memberValue instanceof RawJsonObjectMembers) {
            RawJsonObjectMembers rawMembers = (RawJsonObjectMembers) memberValue;
            writer.append(rawMembers.getRawJsonText());
        } else {
            super.writeMember(writer, member);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 従来の条件に加えて、メンバーの値がホワイトスペースのみの {@link RawJsonObjectMembers}
     * である場合も追加している。
     * </p>
     * @param member 判定対象のメンバー
     * @return 出力の条件を満たしていない場合は true
     */
    protected boolean isSkip(Map.Entry<?, ?> member) {
        return super.isSkip(member) || isJsonWhitespace(member.getValue());
    }

    /**
     * メンバーの値が JSON の空白文字だけであるかどうかを判定する。
     * @param memberValue メンバーの値
     * @return JSON の空白文字だけである場合は true
     */
    protected boolean isJsonWhitespace(Object memberValue) {
        if (memberValue instanceof RawJsonObjectMembers) {
            return ((RawJsonObjectMembers) memberValue).isJsonWhitespace();
        } else {
            return false;
        }
    }
}
