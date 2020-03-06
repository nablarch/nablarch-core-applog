package nablarch.core.log.basic;

import nablarch.core.log.*;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * {@link BasicLoggerFactory}のテスト。
 * @author Kiyohito Itoh
 */
public class BasicLoggerFactoryTest extends LogTestSupport {
    
    private BasicLoggerFactory factory;
    
    @After
    public void tearDown() {
        if (factory != null) {
            factory.terminate();
        }
        super.tearDown();
    }

    /**
     * 設定不備に対応できること。
     */
    @Test
    public void testInvalidSettings() {
        
        // writerNames(使用する全てのログライタの名称)が指定されなかった場合
        
        LogSettings settings = new MockLogSettings("classpath:nablarch/core/log/basic/log-invalid-no-writers.properties");
        factory = new BasicLoggerFactory();
        try {
            factory.initialize(settings);
            fail("must be thrown the IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expected = "'writerNames' was not specified. file path = [classpath:nablarch/core/log/basic/log-invalid-no-writers.properties]";
            assertThat(e.getMessage(), is(expected));
        } finally {
            factory.terminate();
        }

        // availableLoggersNamesOrder(使用する全てのロガー設定の名称)が指定されなかった場合
        
        settings = new MockLogSettings("classpath:nablarch/core/log/basic/log-invalid-no-loggers.properties");
        factory = new BasicLoggerFactory();
        try {
            factory.initialize(settings);
            fail("must be thrown the IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expected = "'availableLoggersNamesOrder' was not specified. file path = [classpath:nablarch/core/log/basic/log-invalid-no-loggers.properties]";
            assertThat(e.getMessage(), is(expected));
        } finally {
            factory.terminate();
        }

        // ロガー設定で指定されたログライタが見つからない場合
        
        settings = new MockLogSettings("classpath:nablarch/core/log/basic/log-invalid-writer-not-found.properties");
        factory = new BasicLoggerFactory();
        try {
            factory.initialize(settings);
            fail("must be thrown the IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expected = "the writer was not found. "
                            + "file path = [classpath:nablarch/core/log/basic/log-invalid-writer-not-found.properties], "
                            + "logger name = [loggers.sql], writer name = [unknown]";
            assertThat(e.getMessage(), is(expected));
        } finally {
            factory.terminate();
        }
        
        // writerNames(使用する全てのログライタの名称)にブランクの名称が指定された場合
        
        settings = new MockLogSettings("classpath:nablarch/core/log/basic/log-invalid-writers-include-blank.properties");
        factory = new BasicLoggerFactory();
        try {
            factory.initialize(settings);
            fail("must be thrown the IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expected = "blank was included in the comma-separated value. "
                            + "file path = [classpath:nablarch/core/log/basic/log-invalid-writers-include-blank.properties], "
                            + "key = [writerNames]";
            assertThat(e.getMessage(), is(expected));
        } finally {
            factory.terminate();
        }
        
        // availableLoggersNamesOrder(使用する全てのロガー設定の名称)にブランクの名称が指定された場合
        
        settings = new MockLogSettings("classpath:nablarch/core/log/basic/log-invalid-loggers-include-blank.properties");
        factory = new BasicLoggerFactory();
        try {
            factory.initialize(settings);
            fail("must be thrown the IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expected = "blank was included in the comma-separated value. "
                            + "file path = [classpath:nablarch/core/log/basic/log-invalid-loggers-include-blank.properties], "
                            + "key = [availableLoggersNamesOrder]";
            assertThat(e.getMessage(), is(expected));
        } finally {
            factory.terminate();
        }

        // ロガー設定のログライタにブランクの名称が指定された場合
        
        settings = new MockLogSettings("classpath:nablarch/core/log/basic/log-invalid-logger-writers-include-blank.properties");
        factory = new BasicLoggerFactory();
        try {
            factory.initialize(settings);
            fail("must be thrown the IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expected = "blank was included in the comma-separated value. "
                            + "file path = [classpath:nablarch/core/log/basic/log-invalid-logger-writers-include-blank.properties], "
                            + "key = [loggers.monitoring.writerNames]";
            assertThat(e.getMessage(), is(expected));
        } finally {
            factory.terminate();
        }

        // availableLoggersNamesOrder(使用する全てのロガー設定の名称)とロガー設定が一致しない場合
        // 設定不備への対応。
        
        settings = new MockLogSettings("classpath:nablarch/core/log/basic/log-invalid-loggers-mismatch.properties");
        factory = new BasicLoggerFactory();
        try {
            factory.initialize(settings);
            fail("must be thrown the IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String expected = "availableLoggersNamesOrder and loggers.* was a mismatch. "
                            + "unused loggers.* should be comment out. "
                            + "availableLoggersNamesOrder = [[sql, monitoring, validation, root]]"
                            + ", unused loggers.* = [[access]]";
            assertThat(e.getMessage(), is(expected));
        } finally {
            factory.terminate();
        }
    }
    
    /**
     * 初期化せずに利用できないこと。
     */
    @Test(expected = IllegalStateException.class)
    public void testNotInitialized() {
        
        factory = new BasicLoggerFactory();
        
        // 初期化せずに終了処理を呼んでもエラーにならない。
        factory.terminate();
        
        // 初期化せずに利用すると例外がスローされる。
        factory.get(BasicLoggerFactoryTest.class.getName());
        fail("must be thrown IllegalStateException");
    }

    /**
     * マッチするロガーが見つからなくても利用できること。
     */
    @Test
    public void testNotMatchedLogger() throws Exception {
        
        File appFile = LogTestUtil.cleanupLog("/not-matched-logger-app.log");
        File debugFile = LogTestUtil.cleanupLog("/not-matched-logger-debug.log");        
        System.setOut(new PrintStream(new FileOutputStream("./log/not-matched-logger-debug.log", true)));

        LogSettings settings = new MockLogSettings(
                "classpath:nablarch/core/log/basic/log-not-matched-logger.properties");
        factory = new BasicLoggerFactory();
        factory.initialize(settings);
        
        Logger nullLogger = factory.get(BasicLoggerFactoryTest.class.getName());
        
        LogTestUtil.logForAllLevels(nullLogger);

        LogLevel[] enabled = new LogLevel[0];
        LogLevel[] disabled = LogLevel.values();
        LogTestUtil.assertLog(appFile, enabled, disabled);
        LogTestUtil.assertLog(debugFile, enabled, disabled);
    }
    
    /**
     * 書き込みで例外をスローするログライタがいても、他のログライタの書き込みは成功すること。
     */
    @Test
    public void testWriteWithException() throws Exception {
        
        File appFile = LogTestUtil.cleanupLog("/write-with-exception-app.log");
        File sqlFile = LogTestUtil.createFile("/write-with-exception-sql.log");
        File monitorFile = LogTestUtil.createFile("/write-with-exception-monitoring.log");
        File debugFile = LogTestUtil.createFile("/write-with-exception-debug.log");
        System.setErr(new PrintStream(new FileOutputStream("./log/write-with-exception-debug.log", true)));
        
        LogSettings settings = new MockLogSettings("classpath:nablarch/core/log/basic/log-write-with-exception.properties");
        factory = new BasicLoggerFactory();
        factory.initialize(settings);
        
        Logger monitorLogger = factory.get("MONITOR");
        assertNotNull(monitorLogger);
        
        LogTestUtil.logForAllLevels(monitorLogger);
        
        factory.terminate();
        
        LogLevel[] enabled = new LogLevel[] {LogLevel.FATAL, LogLevel.ERROR};
        LogLevel[] disabled = new LogLevel[] {LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG, LogLevel.TRACE};
        
        LogTestUtil.assertLog(appFile, enabled, disabled);
        boolean isOptionCheck = true;
        boolean isStackTraceCheck = false;
        LogTestUtil.assertLog(monitorFile, enabled, disabled, isOptionCheck, isStackTraceCheck);
        
        enabled = new LogLevel[0];
        disabled = LogLevel.values();
        LogTestUtil.assertLog(sqlFile, enabled, disabled);
        
        String debug = LogTestUtil.getLog(debugFile);
        assertTrue(debug.indexOf(ExceptionLogWriter.class.getName()) != -1);
    }
    
    /**
     * 初期処理完了後に設定情報が出力されること。
     */
    @Test
    public void testInitializedMessage() {
        
        File appFile = LogTestUtil.cleanupLog("/default-app.log");
        File sqlFile = LogTestUtil.createFile("/default-sql.log");
        File monitorFile = LogTestUtil.createFile("/default-monitoring.log");
        
        LogSettings settings = new MockLogSettings("classpath:nablarch/core/log/basic/log.properties");
        factory = new BasicLoggerFactory();
        factory.initialize(settings);
        
        factory.terminate();
        
        String appLog = LogTestUtil.getLog(appFile);

        //----------------------------------------------------------------------
        // 各ログファイルに対しての初期化ログの出力確認
        //----------------------------------------------------------------------
        Pattern pattern = Pattern.compile("nablarch.core.log.basic.BasicLoggerFactory.*initialized\\.");
        assertTrue(pattern.matcher(appLog).find());
        assertFalse(appLog.contains(
                "LOGGER = [sql] NAME REGEX = [SQL] LEVEL = [INFO]"));
        assertTrue(appLog.contains(
                "LOGGER = [monitoring] NAME REGEX = [MONITOR] LEVEL = [ERROR]"));
        assertTrue(appLog.contains(
                "LOGGER = [access] NAME REGEX = [tis\\.w8\\.web\\.handler\\.AccessLogHandler] LEVEL = [INFO]"));
        assertFalse(appLog.contains(
                "LOGGER = [validation] NAME REGEX = [tis\\.w8\\.core\\.validation.*] LEVEL = [DEBUG]"));
        assertTrue(appLog.contains(
                "LOGGER = [root] NAME REGEX = [.*] LEVEL = [WARN]"));

        String sqlLog = LogTestUtil.getLog(sqlFile);
        assertTrue(pattern.matcher(sqlLog).find());
        assertTrue(sqlLog.contains(
                "LOGGER = [sql] NAME REGEX = [SQL] LEVEL = [INFO]"));
        assertFalse(sqlLog.contains(
                "LOGGER = [monitoring] NAME REGEX = [MONITOR] LEVEL = [ERROR]"));
        assertFalse(sqlLog.contains(
                "LOGGER = [access] NAME REGEX = [tis\\.w8\\.web\\.handler\\.AccessLogHandler] LEVEL = [INFO]"));
        assertFalse(sqlLog.contains(
                "LOGGER = [validation] NAME REGEX = [tis\\.w8\\.core\\.validation.*] LEVEL = [DEBUG]"));
        assertFalse(sqlLog.contains(
                "LOGGER = [root] NAME REGEX = [.*] LEVEL = [WARN]"));
        
        String monitoringLog = LogTestUtil.getLog(monitorFile);
        assertFalse(monitoringLog.contains(
                "LOGGER = [sql] NAME REGEX = [SQL] LEVEL = [INFO]"));
        assertTrue(monitoringLog.contains(
                "LOGGER = [monitoring] NAME REGEX = [MONITOR] LEVEL = [ERROR]"));
        assertFalse(monitoringLog.contains(
                "LOGGER = [access] NAME REGEX = [tis\\.w8\\.web\\.handler\\.AccessLogHandler] LEVEL = [INFO]"));
        assertFalse(monitoringLog.contains(
                "LOGGER = [validation] NAME REGEX = [tis\\.w8\\.core\\.validation.*] LEVEL = [DEBUG]"));
        assertFalse(monitoringLog.contains(
                "LOGGER = [root] NAME REGEX = [.*] LEVEL = [WARN]"));
    }

    /**
     * 実行時ロガー名が出力されること。
     */
    @Test
    public void testRuntimeLoggerName() {

        File appFile = LogTestUtil.cleanupLog("/default-app.log");
        File sqlFile = LogTestUtil.createFile("/default-sql.log");
        File monitorFile = LogTestUtil.createFile("/default-monitoring.log");

        LogSettings settings = new MockLogSettings("classpath:nablarch/core/log/basic/log-with-rutime-logger-name.properties");
        factory = new BasicLoggerFactory();
        factory.initialize(settings);
        Logger logger = factory.get("TEST");
        Logger anotherLogger = factory.get("TEST2");
        logger.logWarn("実行時ロガー名がTESTで出力");
        anotherLogger.logWarn("実行時ロガー名がTEST2で出力");
        logger.logWarn("実行時ロガーがそれぞれ使えること");
        factory.terminate();

        String appLog = LogTestUtil.getLog(appFile);

        assertTrue(appLog.contains(
                "[root] [TEST] 実行時ロガー名がTESTで出力"));
        assertTrue(appLog.contains(
                "[root] [TEST2] 実行時ロガー名がTEST2で出力"));
        assertTrue(appLog.contains(
                "[root] [TEST] 実行時ロガーがそれぞれ使えること"));
    }

    /**
     * 2ファイルへ書き込みできる。
     */
    @Test
    public void testLoggingFor2Files() {
        
        File appFile = LogTestUtil.cleanupLog("/default-app.log");
        File sqlFile = LogTestUtil.createFile("/default-sql.log");
        File monitorFile = LogTestUtil.createFile("/default-monitoring.log");
        
        LogSettings settings = new MockLogSettings("classpath:nablarch/core/log/basic/log.properties");
        factory = new BasicLoggerFactory();
        factory.initialize(settings);
        
        Logger monitorLogger = factory.get("MONITOR");
        assertNotNull(monitorLogger);
        
        LogTestUtil.logForAllLevels(monitorLogger);
        
        factory.terminate();
        
        LogLevel[] enabled = new LogLevel[] {LogLevel.FATAL, LogLevel.ERROR};
        LogLevel[] disabled = new LogLevel[] {LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG, LogLevel.TRACE};
        
        LogTestUtil.assertLog(appFile, enabled, disabled);
        boolean isOptionCheck = true;
        boolean isStackTraceCheck = false;
        LogTestUtil.assertLog(monitorFile, enabled, disabled, isOptionCheck, isStackTraceCheck);
        
        enabled = new LogLevel[0];
        disabled = LogLevel.values();
        LogTestUtil.assertLog(sqlFile, enabled, disabled);
    }
    
    /**
     * システムプロパティで設定を上書きできる。
     */
    @Test
    public void testOverriding() {
        
        System.setProperty("writer.appFile.filePath", "./log/default-override.log");
        System.setProperty("loggers.monitoring.level", "DEBUG");
        
        File overrideFile = LogTestUtil.cleanupLog("/default-override.log");
        File sqlFile = LogTestUtil.createFile("/default-sql.log");
        File monitorFile = LogTestUtil.createFile("/default-monitoring.log");
        File appFile = LogTestUtil.createFile("/default-app.log");
        
        LogSettings settings = new MockLogSettings("classpath:nablarch/core/log/basic/log.properties");
        factory = new BasicLoggerFactory();
        factory.initialize(settings);
        
        Logger monitorLogger = factory.get("MONITOR");
        assertNotNull(monitorLogger);
        
        LogTestUtil.logForAllLevels(monitorLogger);
        
        factory.terminate();
        
        LogLevel[] enabled = new LogLevel[] {LogLevel.FATAL, LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG};
        LogLevel[] disabled = new LogLevel[] {LogLevel.TRACE};
        
        LogTestUtil.assertLog(overrideFile, enabled, disabled);

        boolean isOptionCheck = true;
        boolean isStackTraceCheck = false;
        LogTestUtil.assertLog(monitorFile, enabled, disabled, isOptionCheck, isStackTraceCheck);
        
        enabled = new LogLevel[0];
        disabled = LogLevel.values();
        LogTestUtil.assertLog(sqlFile, enabled, disabled);
        
        assertFalse(appFile.exists());
    }
    
    /**
     * 複数スレッドから利用できる。
     */
    @Test
    public void testMultiThreads() {
        
        File appFile = LogTestUtil.cleanupLog("/default-app.log");
        File sqlFile = LogTestUtil.createFile("/default-sql.log");
        File monitorFile = LogTestUtil.createFile("/default-monitoring.log");
        
        LogSettings settings = new MockLogSettings("classpath:nablarch/core/log/basic/log.properties");
        factory = new BasicLoggerFactory();
        factory.initialize(settings);
        
        Logger appLogger = factory.get(BasicLoggerFactoryTest.class.getName());
        Logger sqlLogger = factory.get("SQL");
        Logger monitorLogger = factory.get("MONITOR");
        
        int size = 30;
        Thread[] t = new Thread[size];
        Parallel[] parallels = new Parallel[size];
        for (int i = 0; i < size; i++) {
            parallels[i] = new Parallel(i, appLogger, sqlLogger, monitorLogger);
            t[i] = new Thread(parallels[i]);
        }
        
        long start = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            t[i].start();
        }
        
        for (int i = 0; i < size; i++) {
            try {
                t[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        factory.terminate();
        
        long total = System.currentTimeMillis() - start;
        System.out.println("TOTAL処理時間 : " + total);
        System.out.println("TOTAL処理時間 / (スレッド数 * 10メッセージ数) : " + (total / (size * MESSAGE_COUNT / 10)));
        for (int i = 0; i < size; i++) {
            System.out.println("  Thread毎の処理時間[" + i + "] : " + parallels[i].getTime());
        }
        
        String allMsg = LogTestUtil.getLog(appFile).concat(LogTestUtil.getLog(sqlFile)).concat(LogTestUtil.getLog(monitorFile));
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < MESSAGE_COUNT; j++) {
                assertTrue(allMsg.indexOf("メッセージ" + i + "-" + j) != -1);
            }
        }
    }

    /** 複数スレッドでログ出力するテストで使用するメッセージ数 */
    private static final int MESSAGE_COUNT = 100;
    
    /**
     * ログ出力を行う処理。
     * @author Kiyohito Itoh
     */
    private static class Parallel implements Runnable {
        
        private Logger appLogger;
        private Logger sqlLogger;
        private Logger monitorLogger;
        
        /** 処理時間 */
        private long time = -1;
        
        /** ログ出力を識別するID */
        private int id = -1;
        
        /**
         * コンストラクタ。
         * @param id ログ出力を識別するID
         */
        public Parallel(int id, Logger appLogger, Logger sqlLogger, Logger monitorLogger) {
            this.id = id;
            this.appLogger = appLogger;
            this.sqlLogger = sqlLogger;
            this.monitorLogger = monitorLogger;
        }
        
        /**
         * {@link BasicLoggerFactoryTest#MESSAGE_COUNT}で指定されて回数だけログ出力を行う。
         */
        public void run() {
            long start = System.currentTimeMillis();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String msg = "メッセージ" + id + "-" + i;
                if (id % 3 == 0) {
                    appLogger.logWarn(msg);
                } else if (id % 3 == 1) {
                    monitorLogger.logError(msg);
                } else {
                    sqlLogger.logInfo(msg);
                }
            }
            time = System.currentTimeMillis() - start;
            System.out.println(id + " stop");
        }
        
        /**
         * @return 処理時間
         */
        public long getTime() {
            return time;
        }
    }
}
