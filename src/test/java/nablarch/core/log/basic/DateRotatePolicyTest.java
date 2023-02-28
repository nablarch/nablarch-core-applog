package nablarch.core.log.basic;

import mockit.Deencapsulation;
import nablarch.core.log.Logger;
import nablarch.core.log.MockLogSettings;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.test.FixedBusinessDateProvider;
import nablarch.test.FixedSystemTimeProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    @Before
    public void setup() {
        final FixedSystemTimeProvider systemTimeProvider = new FixedSystemTimeProvider() {{
            setFixedDate("20161231235959999");
        }};

        final FixedBusinessDateProvider businessTimeProvider = new FixedBusinessDateProvider() {{
            setDefaultSegment("00");
            setFixedDate(new HashMap<String, String>() {{
                put("00", "20110101");
                put("000", "20110102");
            }});
        }};

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                Map<String, Object> repos = new HashMap<String, Object>();
                repos.put("systemTimeProvider", systemTimeProvider);
                repos.put("businessDateProvider", businessTimeProvider);
                return repos;
            }
        });
    }

    /** dateTypeがsystemの場合に、dateTypeが正しく設定されていること */
    @Test
    public void testSystemDateTypeInInitialize() {
        // dateTypeがSystemの場合
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        DateRotatePolicy.DateType actualDateType = Deencapsulation.getField(policy, "dateType");

        assertThat(actualDateType, is(DateRotatePolicy.DateType.SYSTEM));
    }

    /** dateTypeが設定されていない場合に、dateTypeにsystemが設定されていること */
    @Test
    public void testNoneDateTypeInInitialize() {
        // dateTypeが設定されていない場合
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        DateRotatePolicy.DateType actualDateType = Deencapsulation.getField(policy, "dateType");

        assertThat(actualDateType, is(DateRotatePolicy.DateType.SYSTEM));
    }

    /** dateTypeがbusinessの場合に、dateTypeが正しく設定されていること */
    @Test
    public void testBusinessDateTypeInInitialize() {
        // dateTypeがBusinessの場合
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "business");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        DateRotatePolicy.DateType actualDateType = Deencapsulation.getField(policy, "dateType");

        assertThat(actualDateType, is(DateRotatePolicy.DateType.BUSINESS));
    }

    /** dateTypeに不正な値が設定されている場合に、IllegalArgumentExceptionが発生すること */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDateTypeInInitialize() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "Invalid");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));
    }

    /** filePathが正しく設定されていること */
    @Test
    public void testFilePathInInitialize() {
        // dateTypeがSystemの場合
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // filePath
        String actualFilepath = Deencapsulation.getField(policy, "filePath");
        assertThat(actualFilepath, is("./log/app.log"));
    }

    /** segmentが正しく設定されていること */
    @Test
    public void testSegmentInitialize() {
        // dateTypeがSystemの場合
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");
        settings.put("appFile.segment", "000");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // filePath
        String actualSegment = Deencapsulation.getField(policy, "segment");
        assertThat(actualSegment, is("000"));
    }

    /** パスにファイルが存在かつシステム日付の場合、次回更新時刻をファイルの作成時刻から
     *  nextUpdateDateが正しく計算されていること */
    @Test
    public void testNextUpdateDateInInitialize() {

        File f = new File("./log/testNextUpdateDateInInitialize-app.log");
        if (f.exists()) {
            f.delete();
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            fail();
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/testNextUpdateDateInInitialize-app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        Date actualNextUpdateDate = Deencapsulation.getField(policy, "nextUpdateDate");

        // 次回更新予定日はファイル作成時刻の次の日
        Date date = new Date();
        Date expectedNextUpdateDate = null;
        Calendar cl = Calendar.getInstance();
        cl.setTime(date);
        // 時・分・秒・ミリ秒を0にする
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        // その後、日を+1する
        cl.add(Calendar.DATE, 1);
        expectedNextUpdateDate = cl.getTime();

        assertThat(actualNextUpdateDate, is(expectedNextUpdateDate));
    }

    /** 現在時刻<次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotateIfNoNeeded() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        //現在時刻<次回更新日
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date nextUpdateDate = null;
        try {
            nextUpdateDate = sdf.parse("20170101000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);
        boolean actual = policy.needsRotate("abcdeabcde",Charset.forName(System.getProperty("file.encoding")));

        assertThat(actual, is(false));
    }

    /** 業務日付の場合、現在時刻<次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testBusinessNeedsRotateIfNotNeeded() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "business");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        //現在時刻<次回更新日
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date nextUpdateDate = null;
        try {
            nextUpdateDate = sdf.parse("20110102000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);
        boolean actual = policy.needsRotate("abcdeabcde",Charset.forName(System.getProperty("file.encoding")));

        assertThat(actual, is(false));
    }

    /** 業務日付でセグメントが指定されている場合、現在日付 = 次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testBusinessSegmentNeedsRotateIfNeeded() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "business");
        settings.put("appFile.segment", "000");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        //現在時刻<次回更新日
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date nextUpdateDate = null;
        try {
            nextUpdateDate = sdf.parse("20110102000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);
        boolean actual = policy.needsRotate("abcdeabcde",Charset.forName(System.getProperty("file.encoding")));

        assertThat(actual, is(true));
    }

    /** 現在時刻>次回更新日の場合に、正しくrotateが必要かどうか判定を行えること */
    @Test
    public void testNeedsRotateIfNeeded() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));


        Date nextUpdateDate = null;
        // 現在時刻>次回更新日
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        try {
            nextUpdateDate = sdf.parse("20161230000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);
        boolean actual = policy.needsRotate("abcdeabcde",Charset.forName(System.getProperty("file.encoding")));

        assertThat(actual, is(true));
    }

    /** 正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideRotatedFilePath() {
        File f = new File("./log/app.log");
        if (f.exists()) {
            f.delete();
        }
        File rotatedFile = new File("./log/app.log.20161230.old");
        if (rotatedFile.exists()) {
            rotatedFile.delete();
        }
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // ファイルパスの確認
        Date nextUpdateDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        try {
            nextUpdateDate = sdf.parse("20161231000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is("./log/app.log.20161230.old"));
    }

    /** リネーム先のファイルが既に存在する場合に、正しくリネーム先のファイルパスが決定できること */
    @Test
    public void testDecideDupRotatedFilePath() {
        File f = new File("./log/app.log");
        if (f.exists()) {
            f.delete();
        }
        File dupf = new File("./log/app.log.20161230.old");
        if (!dupf.exists()) {
            try {
                dupf.createNewFile();
            } catch (IOException e) {
                fail();
            }
        }
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        // ファイルパスの確認
        Date nextUpdateDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        try {
            nextUpdateDate = sdf.parse("20161231000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);

        String actual = policy.decideRotatedFilePath();

        assertThat(actual, is("./log/app.log.20161231235959999.old"));
    }

    /** 正しくファイルがリネームされること */
    @Test
    public void testRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/testDateRotate-app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File f = new File("./log/testDateRotate-app.log");
        if (f.exists()) {
            f.delete();
        }
        File expected = new File("./log/testDateRotate-app.log.old");
        if (expected.exists()) {
            expected.delete();
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            fail();
        }

        policy.rotate("./log/testDateRotate-app.log.old");

        if (!expected.exists()) {
            fail();
        }
    }

    /** ローテーション時に、正しくnextUpdateDateが更新できること */
    @Test
    public void testNextUpdateDateInRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/testDateRotate-app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File f = new File("./log/testDateRotate-app.log");
        if (f.exists()) {
            f.delete();
        }
        File expected = new File("./log/testDateRotate-app.log.old");
        if (expected.exists()) {
            expected.delete();
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            fail();
        }

        policy.rotate("./log/testDateRotate-app.log.old");

        // nextUpdateDateの確認
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date actualNextUpdateDate = Deencapsulation.getField(policy, "nextUpdateDate");
        Date expectedNextUpdateDate = null;
        try {
            expectedNextUpdateDate = sdf.parse("20170101000000000");
        } catch (ParseException e) {
            fail();
        }

        assertThat(actualNextUpdateDate, is(expectedNextUpdateDate));
    }

    /** ファイルがリネームできない場合に、IllegalStateExceptionが発生すること */
    @Test(expected = IllegalStateException.class)
    public void testInvalidRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/testInvalidDateRotate-app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        File f = new File("./log/testInvalidDateRotate-app.log");
        if (f.exists()) {
            f.delete();
        }
        File expected = new File("./log/testInvalidDateRotate-app.log.old");
        if (expected.exists()) {
            expected.delete();
        }

        // fileが存在しない状態でリネームさせる
        policy.rotate("./log/testInvalidDateRotate-app.log.old");
    }

    /** 正しく設定情報が取得できること */
    @Test
    public void testGetSetting() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.encoding", "utf-8");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));

        Date nextUpdateDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        try {
            nextUpdateDate = sdf.parse("20170101000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);

        String actual = policy.getSettings();

        String expected = "\tDATE TYPE          = [" + "SYSTEM" + "]" + Logger.LS;

        assertThat(actual, is(expected));
    }

    /** パスにファイルが存在しない、かつシステム日付の場合に次回更新時刻を現在時刻から正しく計算できること */
    @Test
    public void testNextUpdateDateInSetUpAfterSystemRepositoryInitialized() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.encoding", "utf-8");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"));
        Deencapsulation.setField(policy, "nextUpdateDate", null);

        policy.setupAfterSystemRepositoryInitialized();
        Date actualNextUpdateDate = Deencapsulation.getField(policy, "nextUpdateDate");

        Date expectedNextUpdateDate = null;
        DateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try {
            expectedNextUpdateDate = sdf.parse("20170101");
        } catch (ParseException e) {
            fail();
        }

        assertThat(actualNextUpdateDate, is(expectedNextUpdateDate));
    }
}
