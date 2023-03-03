package nablarch.core.log.basic;

import nablarch.core.log.Logger;
import nablarch.core.log.MockLogSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

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
import static org.junit.Assert.*;

/**
 * {@link FileSizeRotatePolicy}のテスト。
 *
 * @author Kotaro Taki
 */
public class FileSizeRotatePolicyTest {

    @Before
    public  void setup() {
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File logFile = new File(logFilePath);
        if (logFile.exists()) {
            logFile.delete();
        }
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", logFilePath);
        settings.put("appFile.maxFileSize", "20");
        objectSettings = new ObjectSettings(new MockLogSettings(settings),"appFile");
    }
    private String logFilePath = "./log/file-size-rotate-app.log";
    private ObjectSettings objectSettings;

    /** 正しくファイルがリネームされること */
    @Test
    public void testRotate() throws IOException {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);

        File f = new File(logFilePath);
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
        final FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);
        new File(logFilePath).delete();

        policy.rotate("./log/testInvalidFileSizeRotate-app.log.old");
    }

    /** 正しくrotateが必要かどうか判定を行えること
     *  maxFileSizeが0KBのためfalseを返す */
    @Test
    public void testNeedsRotateMaxFileSizeZero() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", logFilePath);
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
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);

        // currentFileSizeを10KBに設定
        File f = new File(logFilePath);
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
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);

        // currentFileSizeを15KBに設定
        File f = new File(logFilePath);
        f.createNewFile();
        FileWriter filewriter = new FileWriter(f);
        String str = getAlphaNumericString(10 * 1000);
        filewriter.write(str);
        filewriter.close();

        policy.onOpenFile(f);

        policy.onWrite(getAlphaNumericString(5 * 1000), Charset.defaultCharset());

        assertThat(policy.needsRotate(getAlphaNumericString(10 * 1000), Charset.defaultCharset()), is(true));
    }

    /** 正しくrotateが必要かどうか判定を行えること
     *  maxFileSizeが不正な値、currentFileSizeが15バイトでmsgLengthが10バイト */
    @Test
    public void testIvalidMaxFileSizeNeedsRotate() throws IOException {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", logFilePath);
        settings.put("appFile.maxFileSize", "aiueo");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // currentFileSizeを15KBに設定
        File f = new File(logFilePath);
        f.createNewFile();
        FileWriter filewriter = new FileWriter(f);
        String str = getAlphaNumericString(15 * 1000);
        filewriter.write(str);
        filewriter.close();

        policy.onOpenFile(f);

        assertThat(policy.needsRotate(getAlphaNumericString(10 * 1000), Charset.defaultCharset()), is(false));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() throws ParseException {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);
        String actual = policy.decideRotatedFilePath();

        assertTrue(actual.startsWith(logFilePath) && actual.endsWith(".old"));

        DateFormat oldFileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String oldDate = actual.split("\\.")[3];

        oldFileDateFormat.parse(oldDate);
    }

    /** 正しく設定情報が取得できること */
    @Test
    public void testGetSetting() throws IOException {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);

        File file = new File(logFilePath);
        FileWriter filewriter = new FileWriter(file);

        filewriter.write("aiueo");

        filewriter.close();

        policy.onOpenFile(file);

        String actual = policy.getSettings();

        // MAX FILE SIZEはインスタンス変数にはバイトで保存されているため変換
        String expected = "\tFILE AUTO CHANGE    = [" + (20L > 0L) + "]" + Logger.LS
                + "\tMAX FILE SIZE       = [" + 20000L + "]" + Logger.LS
                + "\tCURRENT FILE SIZE   = [" + 5L + "]" + Logger.LS;
        ;

        assertThat(actual, is(expected));
    }
}
