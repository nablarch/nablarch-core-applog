package nablarch.core.log.basic;

import nablarch.core.log.Logger;
import nablarch.core.text.json.*;

import java.io.IOException;
import java.io.Writer;

/**
 * テスト用にカスタマイズしたシリアライズ管理クラス。
 * Number型を文字列として処理、Map末尾に改行付与、Boolean型は例外になる。
 *
 * @author Shuji Kitamura
 */
public class CustomJsonSerializationManager extends JsonSerializationManager {

    protected void enlistSerializer(JsonSerializationSettings settings) {
        addSerializer(new StringToJsonSerializer());
        addSerializer(new DateToJsonSerializer());
        addSerializer(new CustomMapToJsonSerializer(this));
        addSerializer(new ListToJsonSerializer(this));
        addSerializer(new ArrayToJsonSerializer(this));
        addSerializer(new CustomBooleanToJsonSerializer());
        addSerializer(new LocalDateTimeToJsonSerializer());
    }

    public class CustomMapToJsonSerializer extends MapToJsonSerializer {

        public CustomMapToJsonSerializer(JsonSerializationManager manager) {
            super(manager);
        }

        public void serialize(Writer writer, Object value) throws IOException {
            super.serialize(writer, value);
            writer.append(Logger.LS);
        }
    }

    public class CustomBooleanToJsonSerializer extends BooleanToJsonSerializer {

        public void serialize(Writer writer, Object value) throws IOException {
            throw new IllegalArgumentException("error for test");
        }
    }
}
