package nablarch.core.log.app;

import nablarch.core.log.LogTestSupport;
import nablarch.core.text.json.JsonSerializationManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * {@link JsonLogFormatterSupport}のテスト。
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
                "nablarch.core.log.app.CustomJsonSerializationManager");
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManagerClassName(), is("nablarch.core.log.app.CustomJsonSerializationManager"));
    }

    /**
     * {@link JsonLogFormatterSupport#getSerializationManagerClassName()}のテスト
     * デフォルト設定
     */
    @Test
    public void testGetSerializationManagerClassNameFromDefault() {
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManagerClassName(), is("nablarch.core.text.json.JsonSerializationManager"));
    }

    /**
     * {@link JsonLogFormatterSupport#getSerializationManager()}のテスト
     * プロパティ指定
     */
    @Test
    public void testgetSerializationManager() {
        System.setProperty("xxxFormatter.jsonSerializationManagerClassName",
                "nablarch.core.log.app.CustomJsonSerializationManager");
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManager(), is(instanceOf(CustomJsonSerializationManager.class)));
    }

    /**
     * {@link JsonLogFormatterSupport#getSerializationManager()}のテスト
     * デフォルト設定
     */
    @Test
    public void testgetSerializationManagerFromDefault() {
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManager(), is(instanceOf(JsonSerializationManager.class)));
    }

    /**
     * シリアライズ時にIOExceptionがスローされた場合のテスト
     */
    @Test
    public void testSerializeError() {
        System.setProperty("xxxFormatter.jsonSerializationManagerClassName",
                "nablarch.core.log.app.CustomJsonSerializationManager");
        JsonLogFormatterSupport support = new JsonLogFormatterSupport("xxxFormatter.", "default");
        assertThat(support.getSerializationManager(), is(instanceOf(CustomJsonSerializationManager.class)));

        assertThat(support.getStructuredMessage("test"), is("format error"));
    }



}
