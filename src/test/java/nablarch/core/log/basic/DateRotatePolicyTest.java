package nablarch.core.log.basic;

import mockit.Deencapsulation;
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

    /** updateTimeが正しく設定されていること */
    @Test
    public void testUpDateTimeInInitialize() {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.updateTime", "3");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        int actualUpDateTime = Deencapsulation.getField(policy, "updateTime");

        assertThat(actualUpDateTime, is(3));
    }

    /** updateTimeに不正な値が指定された場合に0が設定されていること */
    @Test
    public void testInValidUpDateTimeInInitialize() {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.updateTime", "aiueo");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        int actualUpDateTime = Deencapsulation.getField(policy, "updateTime");

        assertThat(actualUpDateTime, is(0));
    }

    /** filePathが正しく設定されていること */
    @Test
    public void testFilePathInInitialize() {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actualFilepath = Deencapsulation.getField(policy, "filePath");
        assertThat(actualFilepath, is(path));
    }

    /** パスにファイルが存在しない場合、次回更新時刻が正しく計算されていること */
    @Test
    public void testNextUpdateDateInInitialize() throws ParseException {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/testNextUpdateDateInInitialize-app.log";
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.updateTime", "3");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        Date actualNextUpdateDate = Deencapsulation.getField(policy, "nextUpdateDate");

        cl.clear();
        cl.set(2018, Calendar.JANUARY, 2, 3, 0, 0);
        Date expectedDate = cl.getTime();

        assertThat(actualNextUpdateDate, is(expectedDate));
    }

    /** パスにファイルが存在する場合、次回更新時刻がファイルの更新時刻から正しく計算されていること */
    @Test
    public void testNextUpdateDateIfFileExistsInInitialize() throws IOException {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/testNextUpdateDateIfFileExistsInInitialize-app.log";
        File f = new File(path);
        f.createNewFile();

        // ファイル更新時刻の設定
        Date date = new Date();
        f.setLastModified(date.getTime());

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        Date actualNextUpdateDate = Deencapsulation.getField(policy, "nextUpdateDate");

        cl.clear();
        cl.set(2018, Calendar.JANUARY, 2, 0, 0, 0);
        Date expectedNextUpdateDate = cl.getTime();

        assertThat(actualNextUpdateDate, is(expectedNextUpdateDate));
    }

    /** 現在時刻<次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotateIfNoNeeded() {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/app.log";
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

        File f = new File("./log/app.log");
        if (f.exists()) {
            f.delete();
        }
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // 現在時刻の変更
        cl.clear();
        cl.set(2018, Calendar.JANUARY, 2, 23, 40, 28);
        dateTimeMock.SetCurrentTime(cl.getTime());

        boolean actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(true));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() {
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateTimeMock(cl.getTime());

        String path = "./log/app.log";
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        String expectedPath = "./log/app.log.201801010000.old";
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
        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        cl.set(Calendar.MILLISECOND, 000);
        dateTimeMock = new DateTimeMock(cl.getTime());

        File f = new File("./log/app.log");
        if (f.exists()) {
            f.delete();
        }

        File dupF = new File("./log/app.log.201801010000.old");
        dupF.createNewFile();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is("./log/app.log.20180101101010000.old"));
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

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File f = new File(path);
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

        String path = "./log/testDateRotate-app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File f = new File(path);
        f.createNewFile();

        String expectedPath = "./log/testDateRotate-app.log.old";
        File expectedFile = new File(expectedPath);
        if (expectedFile.exists()) {
            expectedFile.delete();
        }

        policy.rotate(expectedPath);

        // nextUpdateDateの確認
        Date actualNextUpdateDate = Deencapsulation.getField(policy, "nextUpdateDate");

        cl.clear();
        cl.set(2018, Calendar.JANUARY, 2, 0, 0, 0);
        Date expectedNextUpdateDate = cl.getTime();

        assertThat(actualNextUpdateDate, is(expectedNextUpdateDate));
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
        String path = "./log/app.log";
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
        settings.put("appFile.updateTime", "0");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actual = policy.getSettings();

        String expected = "\tNEXT CHANGE DATE   = [201801020000]" + Logger.LS
                + "\tCURRENT DATE    = [201801011010]" + Logger.LS
                + "\tUPDATE TIME     = [0]" + Logger.LS;
        ;

        assertThat(actual, is(expected));
    }
}
