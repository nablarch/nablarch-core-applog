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
import java.lang.reflect.Method;
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

    /** 書き込み時に使用する文字エンコーディング */
    private Charset charset =  Charset.forName(System.getProperty("file.encoding"));

    private FixedSystemTimeProvider systemTimeProvider = new FixedSystemTimeProvider() {{
        setFixedDate("20161231235959999");
    }};

    private FixedBusinessDateProvider businessTimeProvider = new FixedBusinessDateProvider() {{
        setDefaultSegment("00");
        setFixedDate(new HashMap<String, String>() {{
            put("00", "20110101");
        }});
    }};

    @Before
    public void setup() {
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

    /** dateTypeが正しく設定されていること
     *  1.dateTypeがsystemの場合
     *  2.dateTypeが設定されていない場合
     *  3.dateTypeがbusinessの場合 */
    @Test
    public void testDateTypeInInitialize() {
        // dateTypeがSystemの場合
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        DateRotatePolicy.DateType actualDateType = Deencapsulation.getField(policy, "dateType");

        assertThat(actualDateType, is(DateRotatePolicy.DateType.SYSTEM));

        // dateTypeが設定されていない場合
        settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");

        policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        actualDateType = Deencapsulation.getField(policy, "dateType");

        assertThat(actualDateType, is(DateRotatePolicy.DateType.SYSTEM));

        // dateTypeがBusinessの場合
        settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "business");

        policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        actualDateType = Deencapsulation.getField(policy, "dateType");

        assertThat(actualDateType, is(DateRotatePolicy.DateType.BUSINESS));
    }

    /** dateTypeに不正な値が設定されている場合に、IllegalArgumentExceptionが発生すること */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDateTypeInInitialize() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "Invalid");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);
    }

    /** filePathが正しく設定されていること */
    @Test
    public void testFilePathInInitialize() {
        // dateTypeがSystemの場合
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        // filePath
        String actualFilepath = Deencapsulation.getField(policy, "filePath");
        assertThat(actualFilepath, is("./log/app.log"));
    }

    /** ファイルが存在かつシステム日付の場合、次回更新時刻をファイルの作成時刻からnextUpdateDateが正しく計算されていること */
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
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

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

    /** 現在時刻が正しく取得できること
     *  1.dateTypeがSystemの場合
     *  2.dateTypeがBusinessの場合 */
    @Test
    public void testGetCurrentDate() {
        // dateTypeがSystemの場合
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        try {
            Method method = DateRotatePolicy.class.getDeclaredMethod("getCurrentDate");
            method.setAccessible(true);
            Date actualDate = (Date) method.invoke(policy);

            Date expectedDate = null;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            try {
                expectedDate = sdf.parse("20161231235959999");
            } catch (ParseException e) {
                fail();
            }

            assertThat(actualDate, is(expectedDate));
        } catch (Exception e) {
            fail();
        }

        //dateTypeがBusinessの場合
        settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "business");

        policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        try {
            Method method = DateRotatePolicy.class.getDeclaredMethod("getCurrentDate");
            method.setAccessible(true);
            Date actualDate = (Date) method.invoke(policy);

            Date expectedDate = null;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            try {
                expectedDate = sdf.parse("20110101000000000");
            } catch (ParseException e) {
                fail();
            }

            assertThat(actualDate, is(expectedDate));
        } catch (Exception e) {
            fail();
        }
    }

    /** 正しくrotateが必要かどうか判定を行えること
     * 1.現在時刻<次回更新日
     * 2.現在時刻>次回更新日。*/
    @Test
    public void testNeedsRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        //現在時刻<次回更新日
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date nextUpdateDate = null;
        try {
            nextUpdateDate = sdf.parse("20170101000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);
        boolean actual = policy.needsRotate("abcdeabcde");

        assertThat(actual, is(false));

        // 現在時刻>次回更新日
        try {
            nextUpdateDate = sdf.parse("20161230000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);
        actual = policy.needsRotate("abcdeabcde");

        assertThat(actual, is(true));
    }

    /** 正しくリネーム先のファイルパスが決定できること
     *  正しくnextUpdateDateが計算できること*/
    @Test
    public void testDecideRotatedFilePath() {
        File f = new File("./log/app.log");
        if (f.exists()) {
            f.delete();
        }
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

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

        // nextUpdateDateの確認
        Date actualNextUpdateDate = Deencapsulation.getField(policy, "nextUpdateDate");
        Date expectedNextUpdateDate = null;
        try {
            expectedNextUpdateDate = sdf.parse("20170101000000000");
        } catch (ParseException e) {
            fail();
        }

        assertThat(actualNextUpdateDate, is(expectedNextUpdateDate));
    }

    /** 正しくファイルがリネームされること */
    @Test
    public void testRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/testDateRotate-app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

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

    /** ファイルがリネームできない場合に、IllegalStateExceptionが発生すること */
    @Test(expected = IllegalStateException.class)
    public void testInvalidRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/testInvalidDateRotate-app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

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

    /** リネーム先のファイルが既に存在する場合に、正しくファイルがリネームされること */
    @Test
    public void testDuplicationRotate() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/testDuplicationRotate-app.log");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        File f = new File("./log/testDuplicationRotate-app.log");
        if (f.exists()) {
            f.delete();
        }
        File dupf = new File("./log/testDuplicationRotate-app.log.old");
        if (dupf.exists()) {
            dupf.delete();
        }

        File expected = new File("./log/testDuplicationRotate-app.log.20161231235959999.old");
        if (expected.exists()) {
            expected.delete();
        }

        try {
            f.createNewFile();
            dupf.createNewFile();
        } catch (IOException e) {
            fail();
        }

        policy.rotate("./log/testDuplicationRotate-app.log.old");

        if (!expected.exists()) {
            fail();
        }
    }

    /** 正しく設定情報が取得できること */
    @Test
    public void testGetSetting() {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.encoding", "utf-8");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        Date nextUpdateDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        try {
            nextUpdateDate = sdf.parse("20170101000000000");
        } catch (ParseException e) {
            fail();
        }
        Deencapsulation.setField(policy, "nextUpdateDate", nextUpdateDate);

        String actual = policy.getSettings();

        String expected = "\tFILE AUTO CHANGE   = [" + true + "]" + Logger.LS;

        assertThat(actual, is(expected));
    }

    /** 正しくnextUpdateDateが計算されること */
    @Test
    public void testSetupIfNeeded() {
        File f = new File("./log/app.log");
        if (f.exists()) {
            f.delete();
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/app.log");
        settings.put("appFile.encoding", "utf-8");
        settings.put("appFile.dateType", "system");

        DateRotatePolicy policy = new DateRotatePolicy();
        policy.initialize(new ObjectSettings(new MockLogSettings(settings), "appFile"),charset);

        policy.setupIfNeeded();
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
