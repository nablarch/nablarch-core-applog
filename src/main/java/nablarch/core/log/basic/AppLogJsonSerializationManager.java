package nablarch.core.log.basic;

import nablarch.core.text.json.ArrayToJsonSerializer;
import nablarch.core.text.json.BasicJsonSerializationManager;
import nablarch.core.text.json.BooleanToJsonSerializer;
import nablarch.core.text.json.CalendarToJsonSerializer;
import nablarch.core.text.json.DateToJsonSerializer;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.text.json.JsonSerializer;
import nablarch.core.text.json.ListToJsonSerializer;
import nablarch.core.text.json.LocalDateTimeToJsonSerializer;
import nablarch.core.text.json.NumberToJsonSerializer;
import nablarch.core.text.json.StringToJsonSerializer;
import nablarch.core.util.annotation.Published;

import java.util.Arrays;
import java.util.List;

/**
 * 各種ログのJSON形式による出力に対応した{@link JsonSerializationManager}の実装クラス。
 * @author Shuji Kitamura
 */
@Published(tag = "architect")
public class AppLogJsonSerializationManager extends BasicJsonSerializationManager {

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<JsonSerializer> createSerializers(JsonSerializationSettings settings) {
        return Arrays.asList(
                new StringToJsonSerializer(),
                new DateToJsonSerializer(this),
                new AppLogMapToJsonSerializer(this),
                new ListToJsonSerializer(this),
                new ArrayToJsonSerializer(this),
                new NumberToJsonSerializer(this),
                new BooleanToJsonSerializer(),
                new CalendarToJsonSerializer(this),
                new LocalDateTimeToJsonSerializer(this));
    }

}
