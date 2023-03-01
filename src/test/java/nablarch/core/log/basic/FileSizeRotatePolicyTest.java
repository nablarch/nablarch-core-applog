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
        String path = "./log/app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actualFilepath = Deencapsulation.getField(policy, "filePath");

        assertThat(actualFilepath, is(path));
    }

    /** 正しくファイルがリネームされること */
    @Test
    public void testRotate() throws IOException {
        String path = "./log/testFileSizeRotate-app.log";

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.maxFileSize", "20");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File f = new File(path);
        f.createNewFile();

        String expectedPath = "./log/testFileSizeRotate-app.log.old";
        File expected = new File(expectedPath);
        if (expected.exists()) {
            expected.delete();
        }

        policy.rotate(expectedPath);

        if (!expected.exists()) {
            fail();
        }
    }

    /** ファイルがリネームできない場合に、IllegalStateExceptionが発生すること */
    @Test(expected = IllegalStateException.class)
    public void testInvalidRotate() {
        String path = "./log/testInvalidFileSizeRotate-app.log";

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.maxFileSize", "20");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));
        File f = new File(path);

        if (f.exists()) {
            f.delete();
        }

        //filePathファイルが存在しない状態でリネームさせる
        policy.rotate("./log/testInvalidFileSizeRotate-app.log.old");
    }

    /** 正しくrotateが必要かどうか判定を行えること
     *  maxFileSizeが0KBのためfalseを返す */
    @Test
    public void testNeedsRotateMaxFileSizeZero() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.maxFileSize", "0");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // maxFileSizeが0バイトのためfalseを返す
        assertThat(policy.needsRotate("abcde", Charset.defaultCharset()), is(false));
    }

    /** 指定したバイト数分のランダムな文字列を生成する */
    private String getAlphaNumericString(int n) {
        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index
                    = (int) (alphaNumericString.length()
                    * Math.random());
            sb.append(alphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }

    /** 正しくrotateが必要かどうか判定を行えること
     *  maxFileSizeが20KBだが、currentFileSizeが10KBでmsgLengthが5KBのためrotate不要 */
    @Test
    public void testNeedsRotateIfNotNeeded() throws IOException {
        String path = "./log/app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.maxFileSize", "20");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // currentFileSizeを10KBに設定
        File f = new File(path);
        f.createNewFile();

        FileWriter filewriter = new FileWriter(f);
        String str = getAlphaNumericString(10 * 1000);
        filewriter.write(str);
        filewriter.close();

        policy.onOpenFile(f);

        assertThat(policy.needsRotate(getAlphaNumericString(5 * 1000), Charset.defaultCharset()), is(false));
    }

    /** 正しくrotateが必要かどうか判定を行えること
     *  maxFileSizeが20バイトだが、currentFileSizeが15バイトでmsgLengthが10バイト */
    @Test
    public void testNeedsRotateIfNeeded() throws IOException {
        String path = "./log/app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.maxFileSize", "20");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // currentFileSizeを15KBに設定
        File f = new File(path);
        f.createNewFile();
        FileWriter filewriter = new FileWriter(f);
        String str = getAlphaNumericString(15 * 1000);
        filewriter.write(str);
        filewriter.close();

        policy.onOpenFile(f);

        assertThat(policy.needsRotate(getAlphaNumericString(10 * 1000), Charset.defaultCharset()), is(true));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() throws ParseException {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        String path = "./log/app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.maxFileSize", "20");

        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));
        String actual = policy.decideRotatedFilePath();

        assertTrue(actual.startsWith("./log/app.log.") && actual.endsWith(".old"));

        DateFormat oldFileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String oldDate = actual.split("\\.")[3];

        oldFileDateFormat.parse(oldDate);
    }

    /** currentFileSizeが正しく設定されていること */
    @Test
    public void testOnOpenFile() throws IOException {
        File file = new File("./log/app.log");
        if (file.exists()) {
            file.delete();
        }

        FileWriter filewriter = new FileWriter(file);

        filewriter.write("aiueo");

        filewriter.close();

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.onOpenFile(file);

        long actual = Deencapsulation.getField(policy, "currentFileSize");

        assertThat(actual, is(5L));
    }

    /** currentFileSizeが正しく計算されていること */
    @Test
    public void testOnWrite() throws IOException {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();

        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File file = new File("./log/app.log");
        if (file.exists()) {
            file.delete();
        }

        FileWriter filewriter = new FileWriter(file);

        filewriter.write("aiueo");

        filewriter.close();

        policy.onOpenFile(file);

        // 1バイトを加算する
        policy.onWrite("a", Charset.defaultCharset());

        long actual = Deencapsulation.getField(policy, "currentFileSize");

        assertThat(actual, is(6L));
    }

    /** 正しく設定情報が取得できること */
    @Test
    public void testGetSetting() throws IOException {
        String path = "./log/app.log";

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.encoding", "utf-8");
        // 単位はKB
        settings.put("appFile.maxFileSize", "20");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }

        FileWriter filewriter = new FileWriter(file);

        filewriter.write("aiueo");

        filewriter.close();

        policy.onOpenFile(file);

        String actual = policy.getSettings();

        // MAX FILE SIZEはインスタンス変数にはバイトで保存されているため変換
        String expected = "\tFILE AUTO CHANGE   = [" + (20L > 0L) + "]" + Logger.LS
                + "\tMAX FILE SIZE      = [" + 20000L + "]" + Logger.LS
                + "\tCURRENT FILE SIZE  = [" + 5L + "]" + Logger.LS;
        ;

        assertThat(actual, is(expected));
    }
}
