package nablarch.core.log.basic;

import nablarch.core.log.LogTestUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.MockLogSettings;
import nablarch.core.util.FileUtil;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * {@link FileSizeRotatePolicy}のテスト。
 * FileLogWriterが1000byteを1KBと計算しているため、本クラスでも1000byte=1KBとしてテストを行う。
 *
 * @author Kotaro Taki
 */
public class FileSizeRotatePolicyTest {

    @Before
    public  void setup() {
        LogTestUtil.cleanupLog(logFilePath);

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

        new File(logFilePath).createNewFile();

        String expectedPath = "./log/testFileSizeRotate-app.log.old";

        // ロテート前
        // リネーム前のファイルが存在すること
        File logFile = new File(logFilePath);
        assertThat(logFile.exists(), is(true));

        // リネーム後のファイルが存在しないこと
        File expectedFile = new File(expectedPath);
        assertThat(expectedFile.exists(), is(false));

        policy.rotate(expectedPath);

        // ロテート後
        // リネーム前のファイルが存在しないこと
        assertThat(logFile.exists(), is(false));

        // リネーム後のファイルが存在すること
        assertThat(expectedFile.exists(), is(true));
    }

    /** ファイルがリネームできない場合に、IllegalStateExceptionが発生すること */
    @Test
    public void testInvalidRotate() {
        final FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);

        final String rotatedFilePath = "./log/testInvalidFileSizeRotate-app.log.old";
        IllegalStateException exception = assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                //filePathファイルが存在しない状態でリネームさせる
                policy.rotate(rotatedFilePath);
            }
        });
        assertThat(exception.getMessage(), is("renaming failed. File#renameTo returns false. src file = [" + logFilePath + "], dest file = [" + rotatedFilePath + "]"));
    }

    /**
     * 正しくrotateが必要かどうか判定を行えること
     * maxFileSizeが0KBのためfalseを返す
     */
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

    /**
     * 正しくrotateが必要かどうか判定を行えること
     * maxFileSizeが-1KBのためfalseを返す
     */
    @Test
    public void testNeedsRotateNegativeMaxFileSize() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", logFilePath);
        settings.put("appFile.maxFileSize", "-1");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // maxFileSizeが-1KBのためfalseを返す
        assertThat(policy.needsRotate("abcde", Charset.defaultCharset()), is(false));
    }

    private File newFile(String path, int byteSize) throws IOException {
        String content = generateZeroPaddingString(byteSize);

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(path);
            fileWriter.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.closeQuietly(fileWriter);
        }

        return new File(path);
    }

    private String generateZeroPaddingString(int byteSize) {
        char[] content = new char[byteSize];
        Arrays.fill(content, '0');
        return new String(content);
    }

    /**
     * 正しくrotateが必要かどうか判定を行えること
     * maxFileSize 20KB > currentFileSize 19KB + msgLength 999byte のためrotate不要
     */
    @Test
    public void testNeedsRotateIfNotNeeded() throws IOException {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);

        // currentFileSizeを19KBに設定
        File logFile = newFile(logFilePath,19 * FileLogWriter.KB);

        policy.onOpenFile(logFile);

        assertThat(policy.needsRotate(generateZeroPaddingString(999), Charset.defaultCharset()), is(false));
    }

    /**
     * 正しくrotateが必要かどうか判定を行えること
     * maxFileSize 20KB = currentFileSize 19KB + msgLength 1KB のためrotate不要
     */
    @Test
    public void testNeedsRotateIfCurrentEqualsMaxFileSize() throws IOException {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);

        // currentFileSizeを19KBに設定
        File logFile = newFile(logFilePath,19 * FileLogWriter.KB);

        policy.onOpenFile(logFile);

        assertThat(policy.needsRotate(generateZeroPaddingString(1 * FileLogWriter.KB), Charset.defaultCharset()), is(false));
    }

    /**
     * 正しくrotateが必要かどうか判定を行えること
     * maxFileSize 20KB > currentFileSize 20KB + msgLength 1byte のためrotate必要
     */
    @Test
    public void testNeedsRotateIfNeeded() throws IOException {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);

        // currentFileSizeを15KBに設定
        File logFile = newFile(logFilePath,15 * FileLogWriter.KB);

        policy.onOpenFile(logFile);

        // currentFileSizeに5KBを加えて、20KBに設定
        policy.onWrite(generateZeroPaddingString(5 * FileLogWriter.KB), Charset.defaultCharset());

        assertThat(policy.needsRotate(generateZeroPaddingString(1), Charset.defaultCharset()), is(true));
    }

    /**
     * 正しくrotateが必要かどうか判定を行えること
     * maxFileSizeが不正な値のためrotate不要
     */
    @Test
    public void testNeedsRotateIfIvalidMaxFileSize() throws IOException {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", logFilePath);
        settings.put("appFile.maxFileSize", "aiueo");

        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // currentFileSizeを15000byteに設定
        File logFile = newFile(logFilePath,15*FileLogWriter.KB);

        policy.onOpenFile(logFile);

        assertThat(policy.needsRotate(generateZeroPaddingString(10 * FileLogWriter.KB), Charset.defaultCharset()), is(false));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() throws ParseException {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);
        String actual = policy.decideRotatedFilePath();

        // リネーム先ファイル名の末尾が.oldであるかどうかの確認
        assertThat(actual, allOf(
                startsWith(logFilePath),
                endsWith(".old")
        ));

        // ファイル名の日付を出力している部分をパースしてエラーにならないことを確認
        DateFormat oldFileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        oldFileDateFormat.setLenient(false);
        String oldDate = actual.split("\\.")[3];

        try {
            oldFileDateFormat.parse(oldDate);
        } catch (Exception e) {
            fail("パースできる形式で日付が出力されていない");
        }
    }

    /** 正しく設定情報が取得できること */
    @Test
    public void testGetSetting() throws IOException {
        FileSizeRotatePolicy policy = new FileSizeRotatePolicy();
        policy.initialize(objectSettings);

        File logFile = new File(logFilePath);
        FileWriter filewriter = new FileWriter(logFile);

        filewriter.write("aiueo");

        filewriter.close();

        policy.onOpenFile(logFile);

        String actual = policy.getSettings();

        // MAX FILE SIZEはインスタンス変数にはバイトで保存されているため変換
        String expected = "\tFILE AUTO CHANGE    = [true]" + Logger.LS
                + "\tMAX FILE SIZE       = [20000]" + Logger.LS
                + "\tCURRENT FILE SIZE   = [5]" + Logger.LS;
        ;

        assertThat(actual, is(expected));
    }
}
