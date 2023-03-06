package nablarch.core.log.basic;

import mockit.*;
import nablarch.core.log.LogTestSupport;
import nablarch.core.log.LogTestUtil;
import nablarch.core.log.MockLogSettings;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
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
    @Test
    public void testInvalidFilePath() {

        final Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./unknown/app.log");

        final FileLogWriter writer = new FileLogWriter();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                writer.initialize(
                        new ObjectSettings(new MockLogSettings(settings), "appFile"));
            }
        });

        assertThat(exception.getMessage(), CoreMatchers.is("failed to create java.io.Writer. file name = [./unknown/app.log], encoding = [UTF-8], buffer size =[8000]"));
    }

    /** 不正な文字エンコーディングが設定された場合は初期処理に失敗すること。 */
    @Test
    public void testInvalidEncoding() {

        final Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/invalid-encoding-app.log");
        settings.put("appFile.encoding", "UNKNOWN");
        settings.put("appFile.outputBufferSize", "8");

        final FileLogWriter writer = new FileLogWriter();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                writer.initialize(
                        new ObjectSettings(new MockLogSettings(settings), "appFile"));
            }
        });

        assertThat(exception.getMessage(), CoreMatchers.is("UNKNOWN"));
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

    /** ローテーションが不要な場合に、RotatePolicyのインターフェースが正しく呼び出されていること */
    @Test
    public void testRotatePolicyWhenNoRotation(@Capturing final RotatePolicy rotatePolicy) {
        LogTestUtil.cleanupLog("/testRotatePolicyWhenNoRotation-app.log");

        final String utf8 = "UTF-8";
        final String path = "./log/testRotatePolicyWhenNoRotation-app.log";
        final String message = "HelloWorld";

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.encoding", utf8);
        settings.put("appFile.outputBufferSize", "8");

        FileLogWriter writer = new FileLogWriter();
        final ObjectSettings objectSettings = new ObjectSettings(new MockLogSettings(settings), "appFile");

        writer.initialize(objectSettings);

        // initializeでは、initialize・onOpenFile・getSettingsが１回ずつ呼びされていることの確認
        new VerificationsInOrder() {
            {
                rotatePolicy.initialize(objectSettings);
                times = 1;
                rotatePolicy.onOpenFile(new File(path));
                times = 1;
                rotatePolicy.getSettings();
                times = 1;
            }
        };

        new Expectations() {
            {
                rotatePolicy.needsRotate(anyString, (Charset) any);
                result = false;
            }
        };

        writer.onWrite("HelloWorld");

        // onWriteでは、needsRotate・onWriteが１回ずつ呼びされていることの確認
        new VerificationsInOrder() {
            {
                rotatePolicy.needsRotate(message, Charset.forName(utf8));
                times = 1;
                rotatePolicy.onWrite(message, Charset.forName(utf8));
                times = 1;
            }
        };
    }

    /** ローテーションが必要な場合に、RotatePolicyのインターフェースが正しく呼び出されていること */
    @Test
    public void testRotatePolicyWhenRotation(@Capturing final RotatePolicy rotatePolicy) {
        LogTestUtil.cleanupLog("/testRotatePolicyWhenRotation-app.log");

        final String utf8 = "UTF-8";
        final String path = "./log/testRotatePolicyWhenRotation-app.log";
        final String rotatedFilePath = "rotatedFilePath";
        final String message = "HelloWorld";

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.encoding", utf8);
        settings.put("appFile.outputBufferSize", "8");

        FileLogWriter writer = new FileLogWriter();
        final ObjectSettings objectSettings = new ObjectSettings(new MockLogSettings(settings), "appFile");

        writer.initialize(objectSettings);

        // initializeでは、initialize・onOpenFile・getSettingsが１回ずつ呼びされていることの確認
        new VerificationsInOrder() {
            {
                rotatePolicy.initialize(objectSettings);
                times = 1;
                rotatePolicy.onOpenFile(new File(path));
                times = 1;
                rotatePolicy.getSettings();
                times = 1;
            }
        };

        new Expectations() {
            {
                rotatePolicy.needsRotate(anyString, (Charset) any);
                result = true;

                rotatePolicy.decideRotatedFilePath();
                result = rotatedFilePath;

                rotatePolicy.onOpenFile((File) any);
                rotatePolicy.getSettings();
            }
        };

        writer.onWrite(message);

        // onWriteではローテーションが必要な場合に、needsRotate・decideRotatedFilePath・onWrite
        // rotate・onOpenFile・getSettings・onWriteの順で実装クラスが呼びされていることの確認
        new VerificationsInOrder() {
            {
                rotatePolicy.needsRotate(message, Charset.forName(utf8));
                times = 1;

                rotatePolicy.decideRotatedFilePath();
                times = 1;
                rotatePolicy.rotate(rotatedFilePath);
                times = 1;
                rotatePolicy.onOpenFile(new File(path));
                times = 1;
                rotatePolicy.getSettings();
                times = 1;

                rotatePolicy.onWrite(anyString, Charset.forName(utf8));
                times = 2;
            }
        };
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

    private static final int MESSAGE_COUNT = 100;
}
