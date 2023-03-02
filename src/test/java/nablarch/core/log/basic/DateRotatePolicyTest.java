package nablarch.core.log.basic;

import mockit.Mock;
import mockit.MockUp;
import nablarch.core.log.Logger;
import nablarch.core.log.MockLogSettings;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link DateRotatePolicy}のテスト。
 *
 * @author Kotaro Taki
 */
public class DateRotatePolicyTest {

    /**
     * Dateモック用クラス
     */
    private DateTimeMock dateTimeMock;

    private Charset ignored = Charset.defaultCharset();

    public static class DateTimeMock extends MockUp<System> {
        Date mockTime;

        public DateTimeMock(Date date) {
            mockTime = date;
        }

        public void SetCurrentTime(Date mockTime) {
            this.mockTime = mockTime;
        }

        @Mock
        public long currentTimeMillis() {
            return mockTime.getTime();
        }
    }

    /** 現在時刻<次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotateIfNoNeeded() {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/testNeedsRotateIfNoNeeded-app.log";
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        boolean actual = policy.needsRotate("abcdeabcde",
                ignored);

        assertThat(actual, is(false));
    }

    /** 現在時刻>次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotateIfNeeded() throws ParseException {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/testNeedsRotateIfNeeded-app.log";
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath",path);

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // 現在時刻の変更
        cl.clear();
        cl.set(2018, Calendar.JANUARY, 2, 23, 40, 28);
        dateTimeMock.SetCurrentTime(cl.getTime());

        boolean actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(true));
    }

    /** パスにファイルが存在する場合、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsIfFileExists() throws IOException {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/testNeedsIfFileExists-app.log";
        File f = new File(path);
        f.createNewFile();

        // ファイル更新時刻の設定
        cl.clear();
        cl.set(2017, Calendar.DECEMBER, 31, 10, 10, 10);
        f.setLastModified(cl.getTime().getTime());

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        boolean actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(true));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/testDecideRotatedFilePath-app.log";
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        String expectedPath = "./log/testDecideRotatedFilePath-app.log.201801010000.old";
        File expectedFile = new File(expectedPath);
        if (expectedFile.exists()) {
            expectedFile.delete();
        }
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is(expectedPath));
    }

    /** リネーム先のファイルが既に存在する場合に、正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideDupRotatedFilePath() throws IOException {
        String path = "./log/testDecideDupRotatedFilePath-app.log";
        String rotatedFilePath = "./log/testDecideDupRotatedFilePath-app.log.201801010000.old";
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        cl.set(Calendar.MILLISECOND, 000);
        dateTimeMock = new DateTimeMock(cl.getTime());

        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        File dupF = new File(rotatedFilePath);
        dupF.createNewFile();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is("./log/testDecideDupRotatedFilePath-app.log.20180101101010000.old"));
    }

    /** 正しくファイルがリネームされること */
    @Test
    public void testRotate() throws IOException {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/testDateRotate-app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        f.createNewFile();

        String expectedPath = "./log/testDateRotate-app.log.old";
        File expectedFile = new File(expectedPath);
        if (expectedFile.exists()) {
            expectedFile.delete();
        }

        policy.rotate(expectedPath);

        if (!expectedFile.exists()) {
            fail();
        }
    }

    /** ローテーション時に、正しくnextUpdateDateが更新できること */
    @Test
    public void testNextUpdateDateInRotate() throws IOException {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/testNextUpdateDate-app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // ローテーションするファイルを作成
        f.createNewFile();

        //ローテーション先にファイルがある場合は削除
        String expectedPath = "./log/testNextUpdateDate-app.log.old";
        File expectedFile = new File(expectedPath);
        if (expectedFile.exists()) {
            expectedFile.delete();
        }

        // rotateは2018年1月2日のため、次回更新時刻は2018年の1月3日になっている
        cl.clear();
        cl.set(2018, Calendar.JANUARY, 2, 23, 59, 59);
        dateTimeMock.SetCurrentTime(cl.getTime());
        policy.rotate(expectedPath);

        // 正しくnextUpdateDateが更新できているかの確認
        cl.clear();
        cl.set(2018, Calendar.JANUARY, 2, 23, 59, 59);
        dateTimeMock.SetCurrentTime(cl.getTime());

        boolean actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(false));

        cl.clear();
        cl.set(2018, Calendar.JANUARY, 3, 0, 0, 0);
        dateTimeMock.SetCurrentTime(cl.getTime());

        actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(true));
    }

    /** ファイルがリネームできない場合に、IllegalStateExceptionが発生すること */
    @Test(expected = IllegalStateException.class)
    public void testInvalidRotate() {
        String path = "./log/testInvalidDateRotate-app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        String expectedPath = "./log/testInvalidDateRotate-app.log.old";
        File expectedFile = new File(expectedPath);
        if (expectedFile.exists()) {
            expectedFile.delete();
        }

        // fileが存在しない状態でリネームさせる
        policy.rotate(expectedPath);
    }

    /** 正しく設定情報が取得できること */
    @Test
    public void testGetSetting() {
        String path = "./log/testGetSetting.log";
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.encoding", "utf-8");
        settings.put("appFile.updateTime", "aiueo");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actual = policy.getSettings();

        String expected = "\tNEXT CHANGE DATE    = [201801020000]" + Logger.LS
                + "\tCURRENT DATE        = [201801011010]" + Logger.LS
                + "\tUPDATE TIME         = [0]" + Logger.LS;
        ;

        assertThat(actual, is(expected));
    }
}
