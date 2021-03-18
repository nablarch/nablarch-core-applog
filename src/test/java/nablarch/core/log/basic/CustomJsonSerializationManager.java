package nablarch.core.log.basic;

import nablarch.core.log.Logger;
import nablarch.core.text.json.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

/**
 * テスト用にカスタマイズしたシリアライズ管理クラス。
 * Number型を文字列として処理、Map末尾に改行付与、Boolean型は例外になる。
 *
 * @author Shuji Kitamura
 */
public class CustomJsonSerializationManager extends BasicJsonSerializationManager {

    @Override
    protected List<JsonSerializer> createSerializers(JsonSerializationSettings settings) {
        return Arrays.asList(
                new StringToJsonSerializer(),
                new DateToJsonSerializer(this),
                new CustomMapToJsonSerializer(this),
                new ListToJsonSerializer(this),
                new ArrayToJsonSerializer(this),
                new CustomBooleanToJsonSerializer(),
                new CalendarToJsonSerializer(this),
                new LocalDateTimeToJsonSerializer(this));
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
