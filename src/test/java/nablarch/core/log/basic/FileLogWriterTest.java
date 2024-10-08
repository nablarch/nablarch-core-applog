package nablarch.core.log.basic;

import nablarch.core.log.LogTestSupport;
import nablarch.core.log.LogTestUtil;
import nablarch.core.log.MockLogSettings;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
            public void run() {
                writer.initialize(
                        new ObjectSettings(new MockLogSettings(settings), "appFile"));
            }
        });

        assertThat(exception.getMessage(), is("failed to create java.io.Writer. file name = [./unknown/app.log], encoding = [UTF-8], buffer size =[8000]"));
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
            public void run() {
                writer.initialize(
                        new ObjectSettings(new MockLogSettings(settings), "appFile"));
            }
        });

        assertThat(exception.getMessage(), is("UNKNOWN"));
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
        settings.put("appFile.rotatePolicy", "nablarch.core.log.basic.RotatePolicyForTest");

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
        assertTrue(appLog.contains("ROTATE POLICY CLASS = [nablarch.core.log.basic.RotatePolicyForTest]"));
        assertTrue(appLog.contains("RotatePolicyForTest getSettings was called"));
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
        settings.put("appFile.rotatePolicy", "nablarch.core.log.basic.RotatePolicyForTest");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        writer.terminate();

        String appLog = LogTestUtil.getLog(appFile);
        assertEquals("", appLog);
    }

    /** initialize時に、RotatePolicyのインターフェースが正しく呼び出されていること */
    @Test
    public void testRotatePolicyWhenInitialize() {
        LogTestUtil.cleanupLog("/testRotatePolicyWhenNoRotation-app.log");

        final String utf8 = "UTF-8";
        final String path = "./log/testRotatePolicyWhenNoRotation-app.log";

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", path);
        settings.put("appFile.encoding", utf8);
        settings.put("appFile.outputBufferSize", "8");

        FileLogWriter writer = new FileLogWriter();
        final ObjectSettings objectSettings = new ObjectSettings(new MockLogSettings(settings), "appFile");
        
        try (final MockedConstruction<FileSizeRotatePolicy> mocked = mockConstruction(FileSizeRotatePolicy.class);) {
            writer.initialize(objectSettings);

            // initializeでは、initialize・onOpenFile・getSettings・onWriteが１回ずつ呼びされていることの確認
            final RotatePolicy rotatePolicy = mocked.constructed().get(0);

            final InOrder inOrder = inOrder(rotatePolicy);
            inOrder.verify(rotatePolicy).initialize(objectSettings);
            inOrder.verify(rotatePolicy).onOpenFile(new File(path));
            inOrder.verify(rotatePolicy).getSettings();
            inOrder.verify(rotatePolicy).onWrite(anyString(), eq(Charset.forName(utf8)));
            verifyNoMoreInteractions(rotatePolicy);
        }
    }

    /** ローテーションが不要な場合に、RotatePolicyのインターフェースが正しく呼び出されていること */
    @Test
    public void testRotatePolicyWhenNoRotation() {
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
        
        try (final MockedConstruction<FileSizeRotatePolicy> mocked = mockConstruction(FileSizeRotatePolicy.class)) {
            writer.initialize(objectSettings);
            
            // initialize の中で RotatePolicy のモックと行われたインタラクションをリセットする
            final RotatePolicy rotatePolicy = mocked.constructed().get(0);
            reset(rotatePolicy);
            when(rotatePolicy.needsRotate(message, Charset.forName(utf8))).thenReturn(false);
            
            writer.onWrite("HelloWorld");

            // onWriteでは、needsRotate・onWriteが１回ずつ呼びされていることの確認
            // その他のメソッドは呼ばれていないことの確認
            final InOrder inOrder = inOrder(rotatePolicy);
            
            inOrder.verify(rotatePolicy).needsRotate(message, Charset.forName(utf8));
            inOrder.verify(rotatePolicy).onWrite(message, Charset.forName(utf8));
            verifyNoMoreInteractions(rotatePolicy);
        }
    }

    /** ローテーションが必要な場合に、RotatePolicyのインターフェースが正しく呼び出されていること */
    @Test
    public void testRotatePolicyWhenRotation() {
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


        try (final MockedConstruction<FileSizeRotatePolicy> mocked = mockConstruction(FileSizeRotatePolicy.class)) {
            writer.initialize(objectSettings);
            
            // initialize の中で RotatePolicy のモックと行われたインタラクションをリセットする
            final RotatePolicy rotatePolicy = mocked.constructed().get(0);
            reset(rotatePolicy);
            
            when(rotatePolicy.needsRotate(message, Charset.forName(utf8))).thenReturn(true);
            when(rotatePolicy.decideRotatedFilePath()).thenReturn(rotatedFilePath);
            
            writer.onWrite(message);

            // onWriteではローテーションが必要な場合に、needsRotate・decideRotatedFilePath・onWrite
            // rotate・onOpenFile・getSettings・onWriteの順で実装クラスが呼びされていることの確認
            final InOrder inOrder = inOrder(rotatePolicy);

            inOrder.verify(rotatePolicy).needsRotate(message, Charset.forName(utf8));
            inOrder.verify(rotatePolicy).decideRotatedFilePath();
            inOrder.verify(rotatePolicy).onWrite(anyString(), eq(Charset.forName(utf8)));
            inOrder.verify(rotatePolicy).rotate(rotatedFilePath);
            inOrder.verify(rotatePolicy).onOpenFile(new File(path));
            inOrder.verify(rotatePolicy).getSettings();
            inOrder.verify(rotatePolicy, times(2)).onWrite(anyString(), eq(Charset.forName(utf8)));
            
            verifyNoMoreInteractions(rotatePolicy);
        }
    }

    /** 
     * INFOレベルより下のレベルで切り替えが発生した場合にINFOレベルのログが出ないこと。
     */
    @Test
    public void testSwitchedUnderInfoLevel() {

        File appFile = LogTestUtil.cleanupLog("/switched-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("appFile.filePath", "./log/switched-app.log");
        settings.put("appFile.encoding", "UTF-8");
        settings.put("appFile.outputBufferSize", "8");
        settings.put("appFile.rotatePolicy", "nablarch.core.log.basic.RotatePolicyForTest");
        settings.put("appFile.level", "WARN");
        settings.put("appFile.rotation", "always");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        writer.write(new LogContext(FQCN, LogLevel.WARN, "test",
                null));

        writer.terminate();
        
        File dir = appFile.getParentFile();

        FileFilter appFileFilter = new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.getName().contains("switched-app.log.");
            }
        };

        File[] files = dir.listFiles(appFileFilter);
        if (files == null) {
            fail();
        }

        assertTrue("1回はスイッチされていること", files.length >= 1);
        
        for (File file : files) {
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
        settings.put("appFile.rotatePolicy", "nablarch.core.log.basic.RotatePolicyForTest");

        FileLogWriter writer = new FileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "appFile"));

        String appLog = LogTestUtil.getLog(appFile);
        assertTrue(appLog.contains("initialized."));

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
            assertTrue(appLog.contains("[[[" + i + "]]]"));
        }

        writer.terminate();
        appLog = LogTestUtil.getLog(appFile);
        assertTrue(appLog.contains("terminated."));
    }
    
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
        settings.put("appFile.rotatePolicy", "nablarch.core.log.basic.RotatePolicyForTest");

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
        settings.put("appFile.rotatePolicy", "nablarch.core.log.basic.RotatePolicyForTest");

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
