package nablarch.core.log.basic;

import mockit.Mock;
import mockit.MockUp;
import nablarch.core.log.LogTestSupport;
import nablarch.core.log.LogTestUtil;
import nablarch.core.log.MockLogSettings;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * {@link FileLogWriter}のテスト。
 *
 * @author Kiyohito Itoh
 */
public class FileLogWriterTest extends LogTestSupport {

    private static final String FQCN = FileLogWriterTest.class.getName();

    /** ファイルパスが存在しない場合は初期処理に失敗すること。 */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFilePath() {

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./unknown/app.log");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));
        writer.terminate();
    }

    /** 不正な文字エンコーディングが設定された場合は初期処理に失敗すること。 */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidEncoding() {

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/invalid-encoding-app.log");
        settings.put("appFile.encoding", "UNKNOWN");
        settings.put("appFile.outputBufferSize", "8");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        writer.terminate();
    }

    /** 最大ファイルサイズに0以下を指定 */
    @Test
    public void testMaxFileSizeLeZero() {
        //----------------------------------------------------------------------
        // 0を指定した場合（エラーにならずに正常にログ出力が行えること。)
        //----------------------------------------------------------------------
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/invalid-encoding-app.log");
        settings.put("appFile.encoding", "utf-8");
        settings.put("appFile.maxFileSize", "0");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        writer.terminate();

        //----------------------------------------------------------------------
        // -1を指定した場合（エラーにならずに正常にログ出力が行えること。)
        //----------------------------------------------------------------------
        Map<String, String> settings2 = new HashMap<String, String>();
        settings2.put("appFile.filePath", "./log/invalid-encoding-app.log");
        settings2.put("appFile.encoding", "utf-8");
        settings2.put("appFile.maxFileSize", "-1");

        FileLogWriter writer2 = new FileLogWriter();
        writer2.initialize(
                new ObjectSettings(new MockLogSettings(settings2), "appFile"));

        writer2.terminate();
    }

    /**
     * 初期処理完了後に設定情報が出力されること。
     * <br/>
     * カレントファイルサイズが0バイトの場合
     */
    @Test
    public void testInitializedMessage() {

        File appFile = LogTestUtil.cleanupLog("/initialized-message-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.level", "INFO");
        settings.put("appFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("appFile.filePath", "./log/initialized-message-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "10");
        settings.put("appFile.maxFileSize", "50000");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        writer.terminate();

        String appLog = LogTestUtil.getLog(appFile);
        assertTrue(appLog.contains("initialized."));
        assertTrue(appLog.contains("WRITER NAME         = [appFile]"));
        assertTrue(appLog.contains(
                "WRITER CLASS        = [nablarch.core.log.basic.FileLogWriter]"));
        assertTrue(appLog.contains(
                "FORMATTER CLASS     = [" + MockLogFormatter.class.getName()
                        + "]"));
        assertTrue(appLog.contains("LEVEL               = [INFO]"));
        assertTrue(appLog.contains(
                "FILE PATH           = [./log/initialized-message-app.log]"));
        assertTrue(appLog.contains("ENCODING            = [UTF-8]"));
        assertTrue(appLog.contains("OUTPUT BUFFER SIZE  = [10000]"));
        assertTrue(appLog.contains("FILE AUTO CHANGE    = [true]"));
        assertTrue(appLog.contains("MAX FILE SIZE       = [50000000]"));
        assertTrue(appLog.contains("CURRENT FILE SIZE   = [0]"));
        assertTrue(appLog.contains("terminated."));
    }

    /**
     * 初期処理完了後に設定情報が出力されること。
     * <br/>
     * カレントファイルサイズが1バイトの場合
     */
    @Test
    public void testInitializedMessage2() throws IOException {

        File appFile = LogTestUtil.cleanupLog("/initialized-message-app.log");
        FileWriter fileWriter = new FileWriter(appFile);
        try {
            fileWriter.write("1");
            fileWriter.flush();
        } finally {
            fileWriter.close();
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.level", "INFO");
        settings.put("appFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("appFile.filePath", "./log/initialized-message-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "10");
        settings.put("appFile.maxFileSize", "50000");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        writer.terminate();

        String appLog = LogTestUtil.getLog(appFile);
        assertTrue(appLog.contains("initialized."));
        assertTrue(appLog.contains("WRITER NAME        = [appFile]"));
        assertTrue(appLog.contains(
                "WRITER CLASS       = [nablarch.core.log.basic.FileLogWriter]"));
        assertTrue(appLog.contains(
                "FORMATTER CLASS    = [" + MockLogFormatter.class.getName()
                        + "]"));
        assertTrue(appLog.contains("LEVEL              = [INFO]"));
        assertTrue(appLog.contains(
                "FILE PATH          = [./log/initialized-message-app.log]"));
        assertTrue(appLog.contains("ENCODING           = [UTF-8]"));
        assertTrue(appLog.contains("OUTPUT BUFFER SIZE = [10000]"));
        assertTrue(appLog.contains("FILE AUTO CHANGE   = [true]"));
        assertTrue(appLog.contains("MAX FILE SIZE      = [50000000]"));
        assertTrue(appLog.contains("CURRENT FILE SIZE  = [1]"));
        assertTrue(appLog.contains("terminated."));
    }

    /**
     * INFOレベル以下だと初期処理完了後に設定情報が出力されないこと。
     */
    @Test
    public void testInitializedMessageUnderInfoLevel() {

        File appFile = LogTestUtil.cleanupLog("/initialized-message-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.level", "WARN");
        settings.put("appFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("appFile.filePath", "./log/initialized-message-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "10");
        settings.put("appFile.maxFileSize", "50000");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        writer.terminate();

        String appLog = LogTestUtil.getLog(appFile);
        assertEquals("", appLog);
    }

    /** ファイルが切り替わらないこと。 */
    @Test
    public void testNotSwitched() {

        File appFile = LogTestUtil.cleanupLog("/not-switched-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/not-switched-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "8");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        for (int i = 0; i < 515; i++) {
            writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[" + i + "]]]",
                    null));
        }

        writer.terminate();

        String appLog = LogTestUtil.getLog(appFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        for (int i = 0; i < 515; i++) {
            assertTrue(appLog.indexOf("[[[" + i + "]]]") != -1);
        }
        assertTrue(appLog.indexOf("[[[515]]]") == -1);
    }

    /** ファイルが自動で切り替わること。 */
    @Test
    public void testSwitched() {

        File appFile = LogTestUtil.cleanupLog("/switched-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/switched-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "8");
        settings.put("appFile.maxFileSize", "10");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        for (int i = 0; i < 515; i++) {
            if (i % 50 == 0) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[" + i + "]]]",
                    null));
        }

        writer.terminate();

        StringBuilder sb = new StringBuilder(50 * 1000);
        File dir = appFile.getParentFile();
        assertTrue(dir.listFiles().length > 1);

        for (File file : dir.listFiles()) {
            if (!file.getName().startsWith("switched-")) {
                continue;
            }
            assertTrue(file.length() < (11 * 1000));
            String log = LogTestUtil.getLog(file);
            assertTrue(log.indexOf(
                    "] change [./log/switched-app.log] -> [./log/switched-app.log.")
                    != -1);
            sb.append(log);
        }

        String appLog = sb.toString();
        for (int i = 0; i < 515; i++) {
            assertTrue(appLog.indexOf("[[[" + i + "]]]") != -1);
        }
        assertTrue(appLog.indexOf("[[[515]]]") == -1);
    }

    /**
     * Dateモック用クラス
     */
    private DateRotatePolicyTest.DateTimeMock dateTimeMock;

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

    /** 日次ローテーションでファイルが自動で切り替わること。*/
    @Test
    public void testDateSwitched() throws ParseException {
        File appFile = LogTestUtil.cleanupLog("/switched-app.log");

        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateRotatePolicyTest.DateTimeMock(cl.getTime());

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/switched-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "8");
        settings.put("appFile.rotatePolicy", "nablarch.core.log.basic.DateRotatePolicy");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String[] dateArray = new String[]{"20180102235059999","20180103115159999","20180104205259999"};
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        for (String date: dateArray) {
            writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[" + date + "]]]",
                    null));
            dateTimeMock.SetCurrentTime(sdf.parse(date));
        }
        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "finished", null));

        writer.terminate();

        // ファイルの存在確認
        StringBuilder sb = new StringBuilder(50 * 1000);
        File dir = appFile.getParentFile();
        assertTrue(dir.listFiles().length >= 3);

        String[] actualDateArray = new String[]{"201801010000","201801020000","201801030000"};
        for (String date: actualDateArray) {
            File f = new File("./log/switched-app.log"+"."+date+".old");
            if (!f.exists()) {
                fail();
            }
        }

        // ファイルの内容確認
        for (File file : dir.listFiles()) {
            if (!file.getName().startsWith("switched-")) {
                continue;
            }
            String log = LogTestUtil.getLog(file);
            assertTrue(log.indexOf(
                    "] change [./log/switched-app.log] -> [./log/switched-app.log.")
                    != -1);
            sb.append(log);
        }

        String appLog = sb.toString();

        for (int i = 0; i < dateArray.length; i++) {
            assertTrue(appLog.indexOf("[[[" + dateArray[i] + "]]]") != -1);
        }
    }

    /** 
     * INFOレベルより下のレベルで切り替えが発生した場合にINFOレベルのログが出ないこと。
     */
    @Test
    public void testSwitchedUnderInfoLevel() throws Throwable {

        File appFile = LogTestUtil.cleanupLog("/switched-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/switched-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "8");
        settings.put("appFile.maxFileSize", "2");
        settings.put("appFile.level", "WARN");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));
        
        for (int i = 0; i < 15; i++) {
            writer.write(new LogContext(FQCN, LogLevel.WARN, "test",
                    null));
        }

        writer.terminate();
        
        File dir = appFile.getParentFile();

        FileFilter appFileFilter = new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.getName().contains("switched-app.log.");
            }
        };
        assertTrue("1回はスイッチされていること", dir.listFiles(appFileFilter).length >= 1);
        
        for (File file : dir.listFiles(appFileFilter)) {
            String log = LogTestUtil.getLog(file);
            assertFalse(log.contains(
                    "] change [./log/switched-app.log] -> [./log/switched-app.log."));
            assertFalse(log.contains("terminated."));

            assertFalse(log.contains("initialized."));
        }
        
        String log = LogTestUtil.getLog(appFile);
        assertFalse(log.contains("terminated."));

        assertFalse(log.contains("initialized."));
        
    }


    /**
     * 書き込みの度にフラッシュされていること。
     */
    @Test
    public void testFlush() {

        File appFile = LogTestUtil.cleanupLog("/flush-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/flush-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "50");
        settings.put("appFile.maxFileSize", "50");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String appLog = LogTestUtil.getLog(appFile);
        assertTrue(appLog.indexOf("initialized.") != -1);

        for (int i = 0; i < 1030; i++) {
            if (i % 250 == 0) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[" + i + "]]]",
                    null));
            appLog = LogTestUtil.getLog(appFile);
            assertTrue(appLog.indexOf("[[[" + i + "]]]") != -1);
        }

        writer.terminate();
        appLog = LogTestUtil.getLog(appFile);
        assertTrue(appLog.indexOf("terminated.") != -1);
    }

    /**
     * リソース解放後(terminated呼び出し後)にログ出力を行った場合は、エラーとなること。
     */
    /**
     * 終了処理の後に書き込み処理が呼ばれた場合に例外がスローされること。
     */
    @Test
    public void testWriteAfterTerminate() {

        LogTestUtil.cleanupLog("/write-after-terminate.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/switched-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "8");
        settings.put("appFile.maxFileSize", "10");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        writer.terminate();

        try {
            writer.write(new LogContext(FQCN, LogLevel.DEBUG, "message", null));
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(),
                    is("failed to write for FileLogWriter has already terminated. name = [appFile]"));
        }
    }

    /**
     * マルチスレッドでファイル切り替えが発生する状況で正しく動作すること。
     */
    @Test
    public void testMultiThreads() {

        LogTestUtil.cleanupLog("/multi-threads-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/multi-threads-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.maxFileSize", "10");

        final FileLogWriter writer = new FileLogWriter();

        try {
            writer.initialize(new ObjectSettings(new MockLogSettings(settings),
                    "appFile"));

            int size = 50;
            Thread[] threads = new Thread[size];
            for (int i = 0; i < size; i++) {
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        for (int i = 0; i < MESSAGE_COUNT; i++) {
                            try {
                                Thread.sleep(100L);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            writer.write(new LogContext(FQCN, LogLevel.DEBUG,
                                    "[[[" + i + "]]]", null));
                        }
                    }
                });
            }
            for (int i = 0; i < size; i++) {
                threads[i].start();
            }

            for (int i = 0; i < size; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            writer.terminate();
        }
    }

    /**
     * マルチスレッドでファイル切り替えが発生する状況で正しく動作すること。
     */
    @Test
    public void testDateRotateMultiThreads() {
        LogTestUtil.cleanupLog("/multi-threads-app.log");

        Calendar cl = Calendar.getInstance();
        cl.set(2018, Calendar.JANUARY, 1, 10, 10, 10);
        dateTimeMock = new DateRotatePolicyTest.DateTimeMock(cl.getTime());

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/multi-threads-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.rotatePolicy", "nablarch.core.log.basic.DateRotatePolicy");

        final FileLogWriter writer = new FileLogWriter();

        try {
            writer.initialize(new ObjectSettings(new MockLogSettings(settings),
                    "appFile"));

            int size = 50;

            Thread[] threads = new Thread[size];
            for (int i = 0; i < size; i++) {
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        for (int i = 0; i < MESSAGE_COUNT; i++) {
                            try {
                                Thread.sleep(100L);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            writer.write(new LogContext(FQCN, LogLevel.DEBUG,
                                    "[[[" + i + "]]]", null));

                            if (i % 10 ==0) {
                                Date currentDate = new Date();
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(currentDate);
                                calendar.add(Calendar.DATE, 1);
                                dateTimeMock.SetCurrentTime(calendar.getTime());
                            }
                        }
                    }
                });
            }
            for (int i = 0; i < size; i++) {
                threads[i].start();
            }

            for (int i = 0; i < size; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            writer.terminate();
        }
    }

    private static final int MESSAGE_COUNT = 100;
}
