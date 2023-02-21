package nablarch.core.log.basic;

import mockit.Deencapsulation;
import nablarch.core.log.Logger;
import nablarch.core.log.MockLogSettings;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link FileSizeRotatePolicy}のテスト。
 *
 * @author Kotaro Taki
 */
public class FileSizeRotatePolicyTest {

    /** 最大ファイルサイズとファイルパスが正しく設定されていること */
    @Test
    public void testInitialize() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        // 単位はKB
        settings.put("appFile.maxFileSize", "10");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        long actualMaxFileSize = Deencapsulation.getField(policy, "maxFileSize");
        String actualFilepath = Deencapsulation.getField(policy, "filePath");

        // インスタンス変数にはバイトで保存されているため変換
        assertThat(actualMaxFileSize, is(10L * 1000L));
        assertThat(actualFilepath, is("./log/app.log"));
    }

    /** 最大ファイルサイズに不正な値を指定した際にmaxFileSizeに0が設定されていること */
    @Test
    public void testInvalidMaxFileSize() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.maxFileSize", "invalid");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        long actualMaxFileSize = Deencapsulation.getField(policy, "maxFileSize");

        assertThat(actualMaxFileSize, is(0L));
    }

    /** 正しくファイルがリネームされること */
    @Test
    public void testRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/testFileSizeRotate-app.log");
        settings.put("appFile.maxFileSize", "20");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File f = new File("./log/testFileSizeRotate-app.log");
        if (f.exists()) {
            f.delete();
        }
        File expected = new File("./log/testFileSizeRotate-app.log.old");
        if (expected.exists()) {
            expected.delete();
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            fail();
        }

        Deencapsulation.setField(policy, "newFilePath", "./log/testFileSizeRotate-app.log.old");

        policy.rotate();

        if (!expected.exists()) {
            fail();
        }
    }

    /** ファイルがリネームできない場合に、IllegalStateExceptionが発生すること */
    @Test(expected = IllegalStateException.class)
    public void testInvalidRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/testInvalidFileSizeRotate-app.log");
        settings.put("appFile.maxFileSize", "20");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));
        File f = new File("./log/testInvalidFileSizeRotate-app.log");

        if (f.exists()) {
            f.delete();
        }

        Deencapsulation.setField(policy, "currentFileSize", 15 * 1000L);
        Deencapsulation.setField(policy, "newFilePath", "./log/testInvalidFileSizeRotate-app.log.old");

        //filePathファイルが存在しない状態でリネームさせる
        policy.rotate();
    }

    /** 正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // maxFileSizeが0バイトのためfalseを返す
        Deencapsulation.setField(policy, "maxFileSize", 0L);
        assertThat(policy.needsRotate(10L), is(false));

        // maxFileSizeが20バイトだが、currentFileSizeが10バイトでmsgLengthが5バイトのためrotate不要
        // 20 > 10+5 のためfalseを返す
        Deencapsulation.setField(policy, "maxFileSize", 20L);
        Deencapsulation.setField(policy, "currentFileSize", 10L);
        assertThat(policy.needsRotate(5L), is(false));

        // maxFileSizeが20バイトだが、currentFileSizeが15バイトでmsgLengthが10バイト
        // 20 < 15+10 のためtrueを返す
        Deencapsulation.setField(policy, "maxFileSize", 20L);
        Deencapsulation.setField(policy, "currentFileSize", 15L);
        assertThat(policy.needsRotate(10L), is(true));
    }

    /** currentFileSizeが正しく設定されていること */
    @Test
    public void testOnRead() {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.onRead(100L);

        long actual = Deencapsulation.getField(policy, "currentFileSize");

        assertThat(actual, is(100L));
    }

    /** currentFileSizeが正しく計算されていること */
    @Test
    public void testOnWrite() {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();

        Deencapsulation.setField(policy, "currentFileSize", 10L);
        // 1バイトを加算する
        policy.onWrite("a".getBytes());

        long actual = Deencapsulation.getField(policy, "currentFileSize");

        assertThat(actual, is(11L));
    }

    /** 正しくnewFilePathが取得できること */
    @Test
    public void testGetNewFilePath() {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        Deencapsulation.setField(policy, "newFilePath", "./log/app.log");
        String actual = policy.getNewFilePath();

        assertThat(actual, is("./log/app.log"));
    }

    /** 正しく設定情報が取得できること */
    @Test
    public void testGetSetting() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.encoding", "utf-8");
        // 単位はKB
        settings.put("appFile.maxFileSize", "20");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        Deencapsulation.setField(policy, "currentFileSize", 10000L);

        String actual = policy.getSettings();

        // MAX FILE SIZEはインスタンス変数にはバイトで保存されているため変換
        String expected = "\tFILE AUTO CHANGE   = [" + (20L > 0L) + "]" + Logger.LS
                + "\tMAX FILE SIZE      = [" + 20000L + "]" + Logger.LS
                + "\tCURRENT FILE SIZE  = [" + 10000L + "]" + Logger.LS;
        ;

        assertThat(actual, is(expected));
    }
}
