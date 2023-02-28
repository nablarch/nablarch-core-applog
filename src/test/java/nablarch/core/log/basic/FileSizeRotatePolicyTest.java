package nablarch.core.log.basic;

import mockit.Deencapsulation;
import nablarch.core.log.Logger;
import nablarch.core.log.MockLogSettings;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link FileSizeRotatePolicy}のテスト。
 *
 * @author Kotaro Taki
 */
public class FileSizeRotatePolicyTest {

    /** 最大ファイルサイズが正しく設定されていること */
    @Test
    public void testMaxFileSize() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        // 単位はKB
        settings.put("appFile.maxFileSize", "10");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        long actualMaxFileSize = Deencapsulation.getField(policy, "maxFileSize");

        // インスタンス変数にはバイトで保存されているため変換
        assertThat(actualMaxFileSize, is(10L * 1000L));
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

    /** ファイルパスが正しく設定されていること */
    @Test
    public void testFilePath() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actualFilepath = Deencapsulation.getField(policy, "filePath");

        assertThat(actualFilepath, is("./log/app.log"));
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

        policy.rotate("./log/testFileSizeRotate-app.log.old");

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

        //filePathファイルが存在しない状態でリネームさせる
        policy.rotate("./log/testInvalidFileSizeRotate-app.log.old");
    }

    /** 正しくrotateが必要かどうか判定を行えること
     *  maxFileSizeが0バイトのためfalseを返す */
    @Test
    public void testNeedsRotateMaxFileSizeZero() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.maxFileSize", "0");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // maxFileSizeが0バイトのためfalseを返す
        assertThat(policy.needsRotate("abcde", Charset.forName(System.getProperty("file.encoding"))), is(false));
   }

    /** 正しくrotateが必要かどうか判定を行えること
     *  maxFileSizeが20バイトだが、currentFileSizeが10バイトでmsgLengthが5バイトのためrotate不要 */
    @Test
    public void testNeedsRotateIfNotNeeded() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.maxFileSize", "20");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // maxFileSizeが20バイトだが、currentFileSizeが10バイトでmsgLengthが5バイトのためrotate不要
        // 20 > 10+5 のためfalseを返す
        Deencapsulation.setField(policy, "maxFileSize", 20L);
        Deencapsulation.setField(policy, "currentFileSize", 10L);
        assertThat(policy.needsRotate("abcde", Charset.forName(System.getProperty("file.encoding"))), is(false));
    }

    /** 正しくrotateが必要かどうか判定を行えること
     *  maxFileSizeが20バイトだが、currentFileSizeが15バイトでmsgLengthが10バイト */
    @Test
    public void testNeedsRotateIfNeeded() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // maxFileSizeが20バイトだが、currentFileSizeが15バイトでmsgLengthが10バイト
        // 20 < 15+10 のためtrueを返す
        Deencapsulation.setField(policy, "maxFileSize", 20L);
        Deencapsulation.setField(policy, "currentFileSize", 15L);
        assertThat(policy.needsRotate("abcdeabcde", Charset.forName(System.getProperty("file.encoding"))), is(true));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        Deencapsulation.setField(policy, "filePath", "./log/app.log");
        String actual = policy.decideRotatedFilePath();

        assertTrue(actual.startsWith("./log/app.log.") && actual.endsWith(".old"));

        DateFormat oldFileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String oldDate = actual.split("\\.")[3];
        try {
            oldFileDateFormat.parse(oldDate);
        } catch (ParseException e) {
            fail();
        }
    }

    /** currentFileSizeが正しく設定されていること */
    @Test
    public void testOnOpenFile() {
        File file = new File("./log/app.log");
        if (file.exists()) {
            file.delete();
        }

        try{
            FileWriter filewriter = new FileWriter(file);

            filewriter.write("aiueo");

            filewriter.close();
        }catch(IOException e){
            fail();
        }

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.onOpenFile(file);

        long actual = Deencapsulation.getField(policy, "currentFileSize");

        assertThat(actual, is(5L));
    }

    /** currentFileSizeが正しく計算されていること */
    @Test
    public void testOnWrite() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();

        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));
        Deencapsulation.setField(policy, "currentFileSize", 10L);
        // 1バイトを加算する
        policy.onWrite("a", Charset.forName(System.getProperty("file.encoding")));

        long actual = Deencapsulation.getField(policy, "currentFileSize");

        assertThat(actual, is(11L));
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
