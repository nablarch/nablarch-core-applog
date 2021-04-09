package nablarch.core.log.basic;

import nablarch.core.text.json.ArrayToJsonSerializer;
import nablarch.core.text.json.BooleanToJsonSerializer;
import nablarch.core.text.json.DateToJsonSerializer;
import nablarch.core.text.json.JsonSerializer;
import nablarch.core.text.json.ListToJsonSerializer;
import nablarch.core.text.json.LocalDateTimeToJsonSerializer;
import nablarch.core.text.json.NumberToJsonSerializer;
import nablarch.core.text.json.StringToJsonSerializer;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeTrue;

/**
 * {@link AppLogJsonSerializationManager}のテストクラス
 *
 * @author Shuji Kitamura
 */
public class AppLogJsonSerializationManagerTest {

    AppLogJsonSerializationManager manager;

    @Before
    public void setup() {
        manager = new AppLogJsonSerializationManager();
    }

    private boolean isRunningOnJava8OrHigher() {
        try {
            Class.forName("java.time.LocalDateTime");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Test
    public void オブジェクトに応じたシリアライザの取得ができること() throws Exception {
        manager.initialize();

        Object value = "test";
        JsonSerializer serializer = manager.getSerializer(value);
        assertThat(serializer, is(instanceOf(StringToJsonSerializer.class)));

        value = new Date();
        serializer = manager.getSerializer(value);
        assertThat(serializer, is(instanceOf(DateToJsonSerializer.class)));

        value = new HashMap<String, Object>();
        serializer = manager.getSerializer(value);
        assertThat(serializer, is(instanceOf(AppLogMapToJsonSerializer.class)));

        value = new ArrayList<String>();
        serializer = manager.getSerializer(value);
        assertThat(serializer, is(instanceOf(ListToJsonSerializer.class)));

        value = new int[0];
        serializer = manager.getSerializer(value);
        assertThat(serializer, is(instanceOf(ArrayToJsonSerializer.class)));

        value = 123;
        serializer = manager.getSerializer(value);
        assertThat(serializer, is(instanceOf(NumberToJsonSerializer.class)));

        value = true;
        serializer = manager.getSerializer(value);
        assertThat(serializer, is(instanceOf(BooleanToJsonSerializer.class)));
    }

    @Test
    public void Java8以降でLocalDateTimeシリアライザの取得ができること() throws Exception {
        assumeTrue(isRunningOnJava8OrHigher());

        AppLogJsonSerializationManager manager = new AppLogJsonSerializationManager();
        manager.initialize();

        Class<?> clazz = Class.forName("java.time.LocalDateTime");
        Method method = clazz.getDeclaredMethod("now");
        Object value = method.invoke(null);

        JsonSerializer serializer = manager.getSerializer(value);
        assertThat(serializer, is(instanceOf(LocalDateTimeToJsonSerializer.class)));
    }

}
