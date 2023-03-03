package nablarch.core.log.basic;

import nablarch.core.log.Logger;
import nablarch.core.log.MockLogSettings;
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


    /** 現在時刻<次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotateIfNoNeeded() throws ParseException {
        String path = "./log/testNeedsRotateIfNoNeeded-app.log";
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        boolean actual = policy.needsRotate("abcdeabcde",
                ignored);

        assertThat(actual, is(false));
    }

    private Date textToDate(String textDate) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return format.parse(textDate);
    }

    /** 現在時刻>次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotateIfNeeded() throws ParseException {
        String path = "./log/testNeedsRotateIfNeeded-app.log";
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath",path);

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // 現在時刻の変更
        ((FixedDateRotatePolicy)policy).setCurrentDate(textToDate("2018-01-02 23:40:28"));

        boolean actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(true));
    }

    /** パスにファイルが存在する場合、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsIfFileExists() throws IOException, ParseException {
        String path = "./log/testNeedsIfFileExists-app.log";
        File f = new File(path);
        f.createNewFile();

        // ファイル更新時刻の設定
        f.setLastModified(textToDate("2017-12-31 10:10:10").getTime());

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        boolean actual = policy.needsRotate("abcdeabcde", ignored);

        assertThat(actual, is(true));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() throws ParseException {
        String path = "./log/testDecideRotatedFilePath-app.log";
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        String expectedPath = "./log/testDecideRotatedFilePath-app.log.20180102000000.old";
        File expectedFile = new File(expectedPath);
        if (expectedFile.exists()) {
            expectedFile.delete();
        }
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is(expectedPath));
    }

    /** リネーム先のファイルが既に存在する場合に、正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideDupRotatedFilePath() throws IOException, ParseException {
        String path = "./log/testDecideDupRotatedFilePath-app.log";
        String rotatedFilePath = "./log/testDecideDupRotatedFilePath-app.log.20180102000000.old";

        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        File dupF = new File(rotatedFilePath);
        dupF.createNewFile();
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is("./log/testDecideDupRotatedFilePath-app.log.20180101101010000.old"));
    }

    /** 正しくファイルがリネームされること */
    @Test
    public void testRotate() throws IOException, ParseException {
        String path = "./log/testDateRotate-app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);

        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));

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
    public void testNextUpdateDateInRotate() throws IOException, ParseException {
        String path = "./log/testNextUpdateDate-app.log";
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        // 現在時刻から次回更新時刻を算出するため、既に存在する場合はファイルを削除する。
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }

        DateRotatePolicy policy = new FixedDateRotatePolicy(textToDate("2018-01-01 10:10:10"));
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // ローテーションするファイルを作成
        f.createNewFile();

        //ローテーション先にファイルがある場合は削除
        String expectedPath = "./log/testNextUpdateDate-app.log.old";
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
