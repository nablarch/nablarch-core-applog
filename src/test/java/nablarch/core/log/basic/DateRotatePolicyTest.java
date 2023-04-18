package nablarch.core.log.basic;

import nablarch.core.log.LogTestUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.MockLogSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * {@link DateRotatePolicy}のテスト。
 *
 * @author Kotaro Taki
 */
@RunWith(Theories.class)
public class DateRotatePolicyTest {

    /**
     * 固定日付を返す、DateRotatePolicy継承クラス
     */
    private static class DateRotatePolicyForTest extends DateRotatePolicy {
        private Date currentDate;

        public DateRotatePolicyForTest(Date currentDate) {
            this.currentDate = currentDate;
        }

        @Override
        protected Date currentDate() {
            return currentDate;
        }

        private void setCurrentDate(Date currentDate) {
            this.currentDate = currentDate;
        }
    }

    private final Charset ignored = Charset.defaultCharset();
    private final String logFilePath = "./log/date-rotate-app.log";
    private ObjectSettings objectSettings;
    private final String message = "dummy-message";
    
    private Map<String, String> settings;

    @Before
    public void setup() {
        // 現在時刻から次回ローテーション日時を算出するため、既に存在する場合はファイルを削除する。
        LogTestUtil.cleanupLog(logFilePath);

        settings = new HashMap<String, String>();
        settings.put("appFile.filePath", logFilePath);
        objectSettings = new ObjectSettings(new MockLogSettings(settings), "appFile");
    }

    private Date textToDate(String textDate) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        return format.parse(textDate);
    }

    /** 現在時刻<次回ローテーション日時の場合に、rotate不要と判定を行えること */
    @Test
    public void testNeedsRotateIfNoNeeded() throws ParseException {
        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-01 10:10:10.000"));
        policy.initialize(objectSettings);

        // 現在時刻の変更
        policy.setCurrentDate(textToDate("2018-01-01 23:59:59.999"));

        boolean actual = policy.needsRotate(message,
                ignored);

        assertThat(actual, is(false));
    }

    /** 現在時刻=次回ローテーション日時の場合に、rotate必要と判定を行えること */
    @Test
    public void testNeedsRotateIfCurrentDateEqualsNextRotateDateTime() throws ParseException {
        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-01 10:10:10.000"));
        policy.initialize(objectSettings);

        // 現在時刻の変更
        policy.setCurrentDate(textToDate("2018-01-02 00:00:00.000"));

        boolean actual = policy.needsRotate(message,
                ignored);

        assertThat(actual, is(true));
    }

    /** 現在時刻>次回ローテーション日時の場合に、rotate必要と判定を行えること */
    @Test
    public void testNeedsRotateIfNeeded() throws ParseException {
        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-01 10:10:10.000"));
        policy.initialize(objectSettings);

        // 現在時刻の変更
        policy.setCurrentDate(textToDate("2018-01-02 00:00:00.001"));

        boolean actual = policy.needsRotate(message, ignored);

        assertThat(actual, is(true));
    }

    /** パスにファイルが存在する場合、ファイルの最終更新日時をもとにrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsIfFileExists() throws ParseException {
        // ファイル更新時刻の設定
        newLogFileWithLastModified("2017-12-31 10:10:10.000");

        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-01 00:00:00.000"));
        policy.initialize(objectSettings);

        assertThat(policy.needsRotate(message, ignored), is(true));

        policy.setCurrentDate(textToDate("2017-12-31 23:59:59.999"));

        assertThat(policy.needsRotate(message, ignored), is(false));
    }

    /**
     * パスにファイルが存在し、rotateTimeが指定されている場合に次回ローテーション日時が正しく判定できていることのテスト。
     * <ul>
     *   <li>システム時刻がrotateTimeより前</li>
     *   <li>ログファイルの最終更新日付がシステム日付の前日</li>
     * </ul>
     */
    @Test
    public void testNeedsRotateSystemTimeBeforeThanRotateTimeAndLastModifiedIsYesterday() throws Exception {
        settings.put("appFile.rotateTime", "02:00:00");

        newLogFileWithLastModified("2018-01-01 10:00:00.000");

        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-02 01:00:00.000"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        assertThat(policy.needsRotate(message, ignored), is(false));

        policy.setCurrentDate(textToDate("2018-01-02 02:00:00.000"));

        assertThat(policy.needsRotate(message, ignored), is(true));
    }

    /**
     * パスにファイルが存在し、rotateTimeが指定されている場合に次回ローテーション日時が正しく判定できていることのテスト。
     * <ul>
     *   <li>システム時刻がrotateTimeより前</li>
     *   <li>ログファイルの最終更新日付がシステム日付と同日</li>
     * </ul>
     */
    @Test
    public void testNeedsRotateSystemTimeBeforeThanRotateTimeAndLastModifiedIsToday() throws Exception {
        settings.put("appFile.rotateTime", "02:00:00");

        newLogFileWithLastModified("2018-01-02 00:30:00.000");

        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-02 01:00:00.000"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        assertThat(policy.needsRotate(message, ignored), is(false));

        policy.setCurrentDate(textToDate("2018-01-02 02:00:00.000"));

        assertThat(policy.needsRotate(message, ignored), is(true));
    }

    /**
     * パスにファイルが存在し、rotateTimeが指定されている場合に次回ローテーション日時が正しく判定できていることのテスト。
     * <ul>
     *   <li>システム時刻がrotateTimeより後</li>
     *   <li>ログファイルの最終更新日付がシステム日付の前日</li>
     * </ul>
     */
    @Test
    public void testNeedsRotateSystemTimeAfterThanRotateTimeAndLastModifiedIsYesterday() throws Exception {
        settings.put("appFile.rotateTime", "02:00:00");

        newLogFileWithLastModified("2018-01-01 10:00:00.000");

        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-02 03:00:00.000"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        assertThat(policy.needsRotate(message, ignored), is(true));
    }

    /**
     * パスにファイルが存在し、rotateTimeが指定されている場合に次回ローテーション日時が正しく判定できていることのテスト。
     * <ul>
     *   <li>システム時刻がrotateTimeより後</li>
     *   <li>ログファイルの最終更新日付がシステム日付と同日</li>
     * </ul>
     */
    @Test
    public void testNeedsRotateSystemTimeAfterThanRotateTimeAndLastModifiedIsToday() throws Exception {
        settings.put("appFile.rotateTime", "02:00:00");

        newLogFileWithLastModified("2018-01-02 01:00:00.000");

        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-02 03:00:00.000"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        assertThat(policy.needsRotate(message, ignored), is(true));
    }

    /**
     * パスにファイルが存在し、rotateTimeが指定されている場合に次回ローテーション日時が正しく判定できていることのテスト。
     * <ul>
     *   <li>システム時刻がrotateTimeより前</li>
     *   <li>ログファイルの最終更新日付がシステム日付の2日前</li>
     * </ul>
     */
    @Test
    public void testNeedsRotateSystemTimeBeforeThanRotateTimeAndLastModifiedIs2DaysAgo() throws Exception {
        settings.put("appFile.rotateTime", "02:00:00");

        newLogFileWithLastModified("2017-12-31 12:00:00.000");

        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-02 01:00:00.000"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        assertThat(policy.needsRotate(message, ignored), is(true));
    }

    /**
     * パスにファイルが存在し、rotateTimeが指定されている場合に次回ローテーション日時が正しく判定できていることのテスト。
     * <ul>
     *   <li>システム時刻がrotateTimeより後</li>
     *   <li>ログファイルの最終更新日付がシステム日付の2日前</li>
     * </ul>
     */
    @Test
    public void testNeedsRotateSystemTimeAfterThanRotateTimeAndLastModifiedIs2DaysAgo() throws Exception {
        settings.put("appFile.rotateTime", "02:00:00");

        newLogFileWithLastModified("2017-12-31 12:00:00.000");

        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-02 03:00:00.000"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        assertThat(policy.needsRotate(message, ignored), is(true));
    }

    /** パスにファイルが存在する場合、ファイルの更新時刻が取得できない場合に例外が発生すること */
    @Test
    public void testLastModifiedReturnsZero() {
        final File file = newLogFile();
        
        // 更新時刻に 0 を設定することで更新時刻を取得できない状態を再現する
        assertTrue(file.setLastModified(0L));

        final DateRotatePolicy policy = new DateRotatePolicy();

        IllegalStateException exception = assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                policy.initialize(objectSettings);
            }
        });

        assertThat(exception.getMessage(), is("failed to read file. file name = [" + logFilePath + "]"));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() throws ParseException {
        String expectedPath = "./log/date-rotate-app.log.20180101101010123.old";

        DateRotatePolicy policy = new DateRotatePolicyForTest(textToDate("2018-01-01 10:10:10.123"));
        policy.initialize(objectSettings);

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is(expectedPath));
    }

    /** 正しくファイルがリネームされること */
    @Test
    public void testRotate() throws ParseException {
        DateRotatePolicy policy = new DateRotatePolicyForTest(textToDate("2018-01-01 10:10:10.000"));

        policy.initialize(objectSettings);
        File logFile = newLogFile();

        String expectedPath = "./log/testRotate-app.log.old";

        // ローテート前
        // リネーム前のファイルが存在すること
        assertThat(logFile.exists(), is(true));

        // リネーム後のファイルが存在しないこと
        File expectedFile = new File(expectedPath);
        assertThat(expectedFile.exists(), is(false));

        policy.rotate(expectedPath);

        // ローテート後
        // リネーム前のファイルが存在しないこと
        assertThat(logFile.exists(), is(false));

        // リネーム後のファイルが存在すること
        assertThat(expectedFile.exists(), is(true));
    }

    /** ローテーション時に、正しくnextRotateDateTimeが更新できること */
    @Test
    public void testNextRotateDateTimeInRotate() throws ParseException {
        DateRotatePolicyForTest policy = new DateRotatePolicyForTest(textToDate("2018-01-01 10:10:10.000"));
        policy.initialize(objectSettings);

        // ローテーションするファイルを作成
        newLogFile();

        String expectedPath = "./log/testNextRotateDateTimeInRotate-app.log.old";

        policy.setCurrentDate(textToDate("2018-01-02 13:59:59.000"));
        // 現在日時は2018年1月2日のため、次回ローテーション日時は2018年の1月3日になっている
        policy.rotate(expectedPath);

        // 正しくnextUpdateDateが更新できているかの確認
        policy.setCurrentDate(textToDate("2018-01-02 23:59:59.000"));

        boolean actual = policy.needsRotate(message, ignored);

        assertThat(actual, is(false));

        policy.setCurrentDate(textToDate("2018-01-03 00:00:00.000"));

        actual = policy.needsRotate(message, ignored);

        assertThat(actual, is(true));
    }

    /** ファイルがリネームできない場合に、IllegalStateExceptionが発生すること */
    @Test
    public void testInvalidRotate() {
        final DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(objectSettings);

        // fileが存在しない状態でリネームさせる
        final String rotatedFilePath = "./log/testInvalidDateRotate-app.log.old";
        IllegalStateException exception = assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                //filePathファイルが存在しない状態でリネームさせる
                policy.rotate(rotatedFilePath);
            }
        });
        assertThat(exception.getMessage(), is("renaming failed. File#renameTo returns false. src file = [" + logFilePath + "], dest file = [" + rotatedFilePath + "]"));
    }

    @DataPoints("normal")
    public static DateFixture[] testFixtures = {
            new DateFixture("12", "2018-01-01 12:00:00", "2018-01-01 10:10:10", "12:00:00")
            , new DateFixture("12:12", "2018-01-02 12:12:00", "2018-01-01 13:10:10", "12:12:00")
            , new DateFixture("12:12:12", "2018-01-02 12:12:12", "2018-01-01 13:10:10", "12:12:12")
            , new DateFixture("", "2018-01-02 00:00:00", "2018-01-01 13:10:10", "00:00:00")
    };

    @DataPoints("invalid")
    public static DateFixture[] InvalidTestFixtures = {
            new DateFixture("12:aiueo", null, "2018-01-01 10:10:10", null)
            , new DateFixture(":::::", null, "2018-01-01 10:10:10", null)
    };

    public static class DateFixture {
        private final String rotateTime;   // ローテーション時刻
        private final String expectedNextRotateDateTime;    //次回ローテーション日時
        private final String currentDate; // 現在時刻
        private final String expectedRotateTime; //フォーマット後のローテーション時刻

        public DateFixture(String rotateTime, String expectedNextRotateDateTime,
                           String currentDate, String formattedRotateTime) {
            this.rotateTime = rotateTime;
            this.expectedNextRotateDateTime = expectedNextRotateDateTime;
            this.currentDate = currentDate;
            this.expectedRotateTime = formattedRotateTime;
        }
    }

    /** 複数のrotateTimeのパターンで正しく設定情報が取得できること */
    @Theory
    public void testGetSetting(@FromDataPoints("normal") DateFixture dateFixture) throws ParseException {
        String path = "./log/testGetSetting.log";

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.encoding", "utf-8");
        if (!dateFixture.rotateTime.isEmpty()) {
            settings.put("appFile.rotateTime", dateFixture.rotateTime);
        }

        DateRotatePolicy policy = new DateRotatePolicyForTest(textToDate(dateFixture.currentDate + ".000"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actual = policy.getSettings();

        String expected = "\tROTATE TIME         = [" + dateFixture.expectedRotateTime + "]" + Logger.LS
                + "\tNEXT ROTATE DATE    = [" + dateFixture.expectedNextRotateDateTime + "]" + Logger.LS
                + "\tCURRENT DATE        = [" + dateFixture.currentDate + "]" + Logger.LS;

        assertThat(actual, is(expected));
    }

    /** 不正なrotateTimeを指定した場合に、例外が発生すること */
    @Theory
    public void testInvalidUpdateTime(@FromDataPoints("invalid") DateFixture dateFixture) throws ParseException {
        String path = "./log/testGetSetting.log";

        final Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.encoding", "utf-8");
        settings.put("appFile.rotateTime", dateFixture.rotateTime);

        final DateRotatePolicy policy = new DateRotatePolicyForTest(textToDate(dateFixture.currentDate + ".000"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));
            }
        });
        assertThat(exception.getMessage(), is("Invalid rotateTime"));
    }

    /**
     * 出力先のログファイルを空ファイルで生成する。
     */
    private File newLogFile() {
        try {
            final File file = new File(logFilePath);
            file.createNewFile();
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 出力先のログファイルを空ファイルで生成し、最終更新日時を指定された値に変更する。
     * @param lastModified 最終更新日時(yyyy-MM-dd HH:mm:ss.SSS)
     */
    private void newLogFileWithLastModified(String lastModified) {
        try {
            newLogFile().setLastModified(textToDate(lastModified).getTime());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
