package nablarch.core.log.app;

import nablarch.core.log.LogTestSupport;
import nablarch.core.log.basic.CustomJsonSerializationManager;
import nablarch.core.log.basic.JsonLogObjectBuilder;
import nablarch.core.text.json.BasicJsonSerializationManager;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

/**
 * {@link JsonLogFormatterSupport}のテストクラス。
 * @author Shuji Kitamura
 */
public class JsonLogFormatterSupportTest extends LogTestSupport {

    Map<String, String> props;

    @Before
    public void setup() {
        props = new HashMap<String, String>();
    }

    private JsonLogFormatterSupport createJsonLogFormatterSupport(JsonSerializationManager jsonSerializationManager) {
        JsonSerializationSettings settings
                = new JsonSerializationSettings(props, "xxxFormatter.", "filePath");
        return new JsonLogFormatterSupport(jsonSerializationManager, settings);
    }

    private JsonLogFormatterSupport createJsonLogFormatterSupport() {
        return createJsonLogFormatterSupport(new BasicJsonSerializationManager());
    }

    private class TestContext {
        private int getIntValue() {
            return 12345;
        }
        private String getStringValue() {
            return "test";
        }
    }

    /**
     * {@link JsonLogFormatterSupport#getStructuredMessage(Object)}のテスト
     * デフォルト設定
     */
    @Test
    public void testGetStructuredMessage() {
        JsonLogFormatterSupport support = createJsonLogFormatterSupport();

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("key1", 123);
        values.put("key2", 456);

        String message = support.getStructuredMessage(values);

        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("key1", 123)),
                withJsonPath("$", hasEntry("key2", 456)))));
    }

    /**
     * {@link JsonLogFormatterSupport#getStructuredMessage(Object)}のテスト
     * structuredMessagePrefixの指定
     */
    @Test
    public void testGetStructuredMessageWithOtherPrefix() {
        props.put("xxxFormatter.structuredMessagePrefix", "$$$");
        JsonLogFormatterSupport support = createJsonLogFormatterSupport();

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("key1", 123);
        values.put("key2", 456);

        String message = support.getStructuredMessage(values);

        assertThat(message.startsWith("$$$"), is(true));
        assertThat(message.substring("$$$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("key1", 123)),
                withJsonPath("$", hasEntry("key2", 456)))));
    }

    /**
     * {@link JsonLogFormatterSupport#getStructuredMessage(Object)}のテスト
     * jsonSerializationManagerClassNameの指定
     */
    @Test
    public void testGetStructuredMessageWithOtherManager() {

        // note CustomJsonSerializationManager クラスは、NumberToJsonSerializerが外されており、
        //      JavaのNumber型がObjectToJsonSerializerにて処理され、JSONのstringとして出力される。
        final CustomJsonSerializationManager serializationManager = new CustomJsonSerializationManager();
        JsonLogFormatterSupport support = createJsonLogFormatterSupport(serializationManager);

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("key1", 123);
        values.put("key2", 456);

        String message = support.getStructuredMessage(values);

        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("key1", "123")),
                withJsonPath("$", hasEntry("key2", "456")))));
    }

    /**
     * {@link JsonLogFormatterSupport#getStructuredMessage(List, Object)
     *  JsonLogFormatterSupport.getStructuredMessage(List, CTX)}のテスト
     */
    @Test
    public void testGetStructuredMessageFromContext() {
        JsonLogFormatterSupport support = createJsonLogFormatterSupport();

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("key1", 123);
        values.put("key2", 456);

        List<JsonLogObjectBuilder<TestContext>> targets
                = new ArrayList<JsonLogObjectBuilder<TestContext>>();
        targets.add(new JsonLogObjectBuilder<TestContext>() {
            @Override
            public void build(Map<String, Object> structuredObject, TestContext context) {
                structuredObject.put("intValue", context.getIntValue());
            }
        });
        targets.add(new JsonLogObjectBuilder<TestContext>() {
            @Override
            public void build(Map<String, Object> structuredObject, TestContext context) {
                structuredObject.put("stringValue", context.getStringValue());
            }
        });

        TestContext context = new TestContext();

        String message = support.getStructuredMessage(targets, context);

        assertThat(message.startsWith("$JSON$"), is(true));
        assertThat(message.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("intValue", 12345)),
                withJsonPath("$", hasEntry("stringValue", "test")))));
    }

    /**
     * シリアライズ時にIOExceptionがスローされた場合のテスト
     */
    @Test
    public void testSerializeError() {
        // note CustomJsonSerializationManagerはbooleanを処理する際に、必ずIOExceptionをスローする
        final CustomJsonSerializationManager serializationManager = new CustomJsonSerializationManager();
        final JsonLogFormatterSupport support = createJsonLogFormatterSupport(serializationManager);

        final Map<String, Object> structuredObject = new HashMap<String, Object>();
        structuredObject.put("key", true);

        RuntimeException e = assertThrows(RuntimeException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                support.getStructuredMessage(structuredObject);
            }
        });

        final Throwable cause = e.getCause();
        assertThat(cause, is(instanceOf(IOException.class)));
        assertThat(cause.getMessage(), is("error for test"));
    }

}
