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
     */
    @Override
    public void serialize(Writer writer, Object value) throws IOException {
        Map<?, ?> map = (Map<?, ?>) value;
        boolean first = true;
        writer.append(BEGIN_OBJECT);
        for (Map.Entry<?, ?> member : map.entrySet()) {
            if (isSkip(member)) {
                continue;
            }

            if (!first) {
                writer.append(VALUE_SEPARATOR);
            }

            Object memberValue = member.getValue();

            if (memberValue instanceof RawJsonObjectMembers) {
                RawJsonObjectMembers rawMembers = (RawJsonObjectMembers) memberValue;
                writer.append(rawMembers.getRawJsonText());
            } else {
                Object memberName = member.getKey();
                memberNameSerializer.serialize(writer, memberName);
                writer.append(NAME_SEPARATOR);
                manager.getSerializer(memberValue).serialize(writer, memberValue);
            }

            first = false;
        }
        writer.append(END_OBJECT);
    }

    /**
     * 指定されたメンバーが出力の条件を満たしていないことを判定する。
     * @param member 判定対象のメンバー
     * @return 出力の条件を満たしていない場合は true
     */
    private boolean isSkip(Map.Entry<?, ?> member) {
        return isNotSupportedMemberName(member.getKey())
                || isNotSupportedMemberValue(member.getValue())
                || isEmpty(member.getValue());
    }

    /**
     * メンバーの名前が出力サポート対象か判定する。
     * @param memberName メンバーの名前
     * @return 出力サポート対象の場合は true
     */
    private boolean isNotSupportedMemberName(Object memberName) {
        return memberName == null || !memberNameSerializer.isTarget(memberName.getClass());
    }

    /**
     * メンバーの値が出力がサポートされていない値かどうか判定する。
     * @param memberValue メンバーの値
     * @return 出力がサポートされていない値の場合は true
     */
    private boolean isNotSupportedMemberValue(Object memberValue) {
        return memberValue == null && isIgnoreNullValueMember;
    }

    /**
     * メンバーの値が空で出力が不要かどうかを判定する。
     * @param memberValue メンバーの値
     * @return 空で出力不要の場合は true
     */
    private boolean isEmpty(Object memberValue) {
        if (memberValue instanceof RawJsonObjectMembers) {
            return ((RawJsonObjectMembers) memberValue).isJsonWhitespace();
        } else {
            return false;
        }
    }
}
