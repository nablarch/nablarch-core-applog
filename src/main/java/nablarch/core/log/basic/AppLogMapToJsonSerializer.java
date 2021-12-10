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
        boolean isFirst = true;
        writer.append(BEGIN_OBJECT);
        for (Map.Entry<?, ?> member : map.entrySet()) {
            Object memberName = member.getKey();
            if (memberName != null && memberNameSerializer.isTarget(memberName.getClass())) {
                Object memberValue = member.getValue();
                if (memberValue != null || !isIgnoreNullValueMember) {
                    if (memberValue instanceof RawJsonObjectMembers) {
                        RawJsonObjectMembers rawMembers = (RawJsonObjectMembers) memberValue;
                        if (!rawMembers.isJsonWhitespace()) {
                            if (!isFirst) {
                                writer.append(VALUE_SEPARATOR);
                            } else {
                                isFirst = false;
                            }
                        }
                        writer.append(rawMembers.getRawJsonText());
                    } else {
                        if (!isFirst) {
                            writer.append(VALUE_SEPARATOR);
                        } else {
                            isFirst = false;
                        }
                        memberNameSerializer.serialize(writer, memberName);
                        writer.append(NAME_SEPARATOR);
                        manager.getSerializer(memberValue).serialize(writer, memberValue);
                    }
                }
            }
        }
        writer.append(END_OBJECT);
    }

}
