package nablarch.core.log.app;

import nablarch.core.log.LogTestSupport;
import nablarch.core.log.basic.CustomJsonSerializationManager;
import nablarch.core.text.json.JsonSerializationManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * {@link JsonLogFormatterSupport}のテストクラス。
 * @author Shuji Kitamura
 */
public class JsonLogFormatterSupportTest extends LogTestSupport {

    @Before
    public void setup() {
        System.clearProperty("xxxFormatter.targets");
        System.clearProperty("xxxFormatter.structuredMessagePrefix");
        System.clearProperty("xxxFormatter.jsonSerializationManagerClassName");
    }

    @After
    public void teardown() {
        System.clearProperty("xxxFormatter.targets");
        System.clearProperty("xxxFormatter.structuredMessagePrefix");
        System.clearProperty("xxxFormatter.jsonSerializationManagerClassName");
    }

    /**
     * {@link JsonLogFormatterSupport#getTargetsProperty()}のテスト
     */
    @Test
    public void testGetTargetsProperty() {
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getTargetsProperty(), is("xxxFormatter.targets"));
    }

    /**
     * {@link JsonLogFormatterSupport#getTargets()}のテスト
     * プロパティ指定
     */
    @Test
    public void testGetTargets() {
        System.setProperty("xxxFormatter.targets", "requestId,message");
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getTargets(), is("requestId,message"));
    }

    /**
     * {@link JsonLogFormatterSupport#getTargets()}のテスト
     * デフォルト設定
     */
    @Test
    public void testGetTargetsFromDefault() {
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getTargets(), is("default"));
    }

    /**
     * {@link JsonLogFormatterSupport#getStructuredMessagePrefix()}のテスト
     * プロパティ指定
     */
    @Test
    public void testGetStructuredMessagePrefix() {
        System.setProperty("xxxFormatter.structuredMessagePrefix", "$$$");
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getStructuredMessagePrefix(), is("$$$"));
    }

    /**
     * {@link JsonLogFormatterSupport#getStructuredMessagePrefix()}のテスト
     * デフォルト設定
     */
    @Test
    public void testGetStructuredMessagePrefixFromDefault() {
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getStructuredMessagePrefix(), is("$JSON$"));
    }

    /**
     * {@link JsonLogFormatterSupport#getSerializationManagerClassName()}のテスト
     * プロパティ指定
     */
    @Test
    public void testGetSerializationManagerClassName() {
        System.setProperty("xxxFormatter.jsonSerializationManagerClassName",
                "nablarch.core.log.basic.CustomJsonSerializationManager");
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManagerClassName(), is("nablarch.core.log.basic.CustomJsonSerializationManager"));
    }

    /**
     * {@link JsonLogFormatterSupport#getSerializationManagerClassName()}のテスト
     * デフォルト設定
     */
    @Test
    public void testGetSerializationManagerClassNameFromDefault() {
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManagerClassName(), is("nablarch.core.text.json.BasicJsonSerializationManager"));
    }

    /**
     * {@link JsonLogFormatterSupport#getSerializationManager()}のテスト
     * プロパティ指定
     */
    @Test
    public void testGetSerializationManager() {
        System.setProperty("xxxFormatter.jsonSerializationManagerClassName",
                "nablarch.core.log.basic.CustomJsonSerializationManager");
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManager(), is(instanceOf(CustomJsonSerializationManager.class)));
    }

    /**
     * {@link JsonLogFormatterSupport#getSerializationManager()}のテスト
     * デフォルト設定
     */
    @Test
    public void testGetSerializationManagerFromDefault() {
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManager(), is(instanceOf(JsonSerializationManager.class)));
    }

    /**
     * シリアライズ時にIOExceptionがスローされた場合のテスト
     */
    @Test
    public void testSerializeError() {
        System.setProperty("xxxFormatter.jsonSerializationManagerClassName",
                "nablarch.core.log.basic.CustomJsonSerializationManager");
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManager(), is(instanceOf(CustomJsonSerializationManager.class)));

        Map<String, Object> structuredObject = new HashMap<String, Object>();
        structuredObject.put("key", true);
        assertThat(support.getStructuredMessage(structuredObject), is("format error"));
    }



}
