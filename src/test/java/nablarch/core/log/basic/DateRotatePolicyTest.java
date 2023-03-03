package nablarch.core.log.basic;

import nablarch.core.log.Logger;
import nablarch.core.log.MockLogSettings;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
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
import static org.junit.Assert.fail;

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
    private static class FixedDateRotatePolicy extends DateRotatePolicy {
        private Date currentDate;

        public FixedDateRotatePolicy(Date currentDate) {
            this.currentDate = currentDate;
        }

        @Override
        protected Date currentDate() {
            return  currentDate;
        }

        private void setCurrentDate(Date currentDate) {
            this.currentDate = currentDate;
        }
    }

    private Charset ignored = Charset.defaultCharset();
    private String logFilePath = "./log/date-rotate-app.log";
    private ObjectSettings objectSettings;

    @Before
    public  void setup() {
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File logFile = new File(logFilePath);
        if (logFile.exists()) {
            logFile.delete();
        }
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", logFilePath);
        objectSettings = new ObjectSettings(new MockLogSettings(settings),"appFile");
    }

    private Date textToDate(String textDate) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return format.parse(textDate);
    }

    /** 現在時刻<次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotateIfNoNeeded() throws ParseException {
        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(objectSettings);

        boolean actual = policy.needsRotate("abcdeabcde",
                ignored);

        assertThat(actual, is(false));
    }

    /** 現在時刻>次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotateIfNeeded() throws ParseException {
        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(objectSettings);

        // 現在時刻の変更
        ((FixedDateRotatePolicy)policy).setCurrentDate(textToDate("2018-01-02 23:40:28"));

        boolean actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(true));
    }

    /** パスにファイルが存在する場合、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsIfFileExists() throws IOException, ParseException {
        File logFile = new File(logFilePath);
        logFile.createNewFile();

        // ファイル更新時刻の設定
        logFile.setLastModified(textToDate("2017-12-31 10:10:10").getTime());

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(objectSettings);

        boolean actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(true));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() throws ParseException {
        String expectedPath = "./log/date-rotate-app.log.20180102000000.old";
        File expectedFile = new File(expectedPath);
        if (expectedFile.exists()) {
            expectedFile.delete();
        }

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(objectSettings);

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is(expectedPath));
    }

    /** リネーム先のファイルが既に存在する場合に、正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideDupRotatedFilePath() throws IOException, ParseException {
        File dupLogFile = new File("./log/date-rotate-app.log.20180104000000.old");
        dupLogFile.createNewFile();

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-03 10:10:10"));
        policy.initialize(objectSettings);

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is("./log/date-rotate-app.log.20180103101010000.old"));
    }

    /** 正しくファイルがリネームされること */
    @Test
    public void testRotate() throws IOException, ParseException {
        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));

        policy.initialize(objectSettings);

        new File(logFilePath).createNewFile();

        String expectedPath = "./log/testRotate-app.log.old";
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
    public void testNextUpdateDateInRotate() throws IOException, ParseException {
        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(objectSettings);

        // ローテーションするファイルを作成
        new File(logFilePath).createNewFile();

        //ローテーション先にファイルがある場合は削除
        String expectedPath = "./log/testNextUpdateDateInRotate-app.log.old";
        File expectedFile = new File(expectedPath);
        if (expectedFile.exists()) {
            expectedFile.delete();
        }

        // rotateは2018年1月1日のため、次回更新時刻は2018年の1月2日になっている
        policy.rotate(expectedPath);

        // 正しくnextUpdateDateが更新できているかの確認
        ((FixedDateRotatePolicy)policy).setCurrentDate(textToDate("2018-01-01 23:59:59"));

        boolean actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(false));

        ((FixedDateRotatePolicy)policy).setCurrentDate(textToDate("2018-01-02 00:00:00"));

        actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(true));
    }

    /** ファイルがリネームできない場合に、IllegalStateExceptionが発生すること */
    @Test(expected = IllegalStateException.class)
    public void testInvalidRotate() {
        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(objectSettings);

        File f = new File(logFilePath);
        if (f.exists()) {
            f.delete();
        }

        // fileが存在しない状態でリネームさせる
        policy.rotate("./log/testInvalidDateRotate-app.log.old");
    }

    @DataPoints("normal")
    public static DateFixture[] testFixtures = {
            new DateFixture("12", "2018-01-02 12:00:00","2018-01-01 10:10:10")
            ,new DateFixture("12:12", "2018-01-02 12:12:00", "2018-01-01 10:10:10")
            ,new DateFixture("12:12:12", "2018-01-02 12:12:12", "2018-01-01 10:10:10")
    };

    @DataPoints("invalid")
    public static DateFixture[] InvalidTestFixtures = {
            new DateFixture("12:aiueo", null, "2018-01-01 10:10:10")
            ,new DateFixture(":::::", null, "2018-01-01 10:10:10")
    };

    @Ignore
    public static class DateFixture {
        private String updateTime;   // 更新時刻
        private String expectedNextUpdateTime;    //次回更新時刻
        private String currentDate; // 現在時刻

        public DateFixture(String updateTime,String expectedNextUpdateTime,
                String currentDate) {
            this.updateTime = updateTime;
            this.expectedNextUpdateTime = expectedNextUpdateTime;
            this.currentDate = currentDate;
        }
    }

    /** 正しく設定情報が取得できること */
    @Theory
    public void testGetSetting(@FromDataPoints("normal") DateFixture dateFixture) throws ParseException {
        String path = "./log/testGetSetting.log";
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.encoding", "utf-8");
        settings.put("appFile.updateTime", dateFixture.updateTime);

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate(dateFixture.currentDate));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actual = policy.getSettings();

        String expected = "\tNEXT CHANGE DATE    = ["+dateFixture.expectedNextUpdateTime +"]" + Logger.LS
                + "\tCURRENT DATE        = [2018-01-01 10:10:10]" + Logger.LS
                + "\tUPDATE TIME         = ["+dateFixture.updateTime+"]" + Logger.LS;
        ;

        assertThat(actual, is(expected));
    }

    /** 不正なUpdateTimeを指定した場合に、例外が発生すること */
    @Theory
    public void testInvalidUpdateTime(@FromDataPoints("invalid") DateFixture dateFixture) throws ParseException {
        String path = "./log/testGetSetting.log";

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.encoding", "utf-8");
        settings.put("appFile.updateTime", dateFixture.updateTime);

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate(dateFixture.currentDate));
        try{
            policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));
        }
        catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),is("Invalid updateTime"));
        }
    }
}
