package nablarch.core.log.basic;

import nablarch.core.log.LogTestSupport;
import nablarch.core.log.LogTestUtil;
import nablarch.core.log.MockLogSettings;
import nablarch.core.message.MockStringResourceHolder;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
 * {@link SynchronousFileLogWriter}のテスト。
 * @author Masato Inoue
 */
public class SynchronousFileLogWriterTest extends LogTestSupport {

    private static final String FQCN = FileLogWriterTest.class.getName();

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
			"nablarch/core/log/app/message-resource-initialload-test.xml");

    private static final String[][] MESSAGES = {
        { "FW000001", "en","ロックの取得に失敗しました。ロックファイルが不正に開かれています。ロックファイルパス=[{0}]。"},
        { "FW000002", "en","ロックの取得に失敗しました。ロックファイルが生成できません。ロックファイルパス=[{0}]。" },
        { "FW000003", "en","ロックの解放に失敗しました。生成したロックファイルを削除できません。ロックファイルパス=[{0}]。" },
        { "FW000004", "en","ロック取得中に割り込みが発生しました。" },
        };

    @Before
    public void classSetup() throws Exception {
        repositoryResource.getComponentByType(MockStringResourceHolder.class).setMessages(MESSAGES);
    }

	@Override
    @After
    public void tearDown() {
        // ロックファイルが削除されず、他のテストが落ちるないように対処
        new File(System.getProperty("java.io.tmpdir") + "/test.lock").delete();
        new File("lock/test.lock").delete();
        new File("tmp/" + getClass().getName() + "/lock").delete();
        new File(System.getProperty("java.io.tmpdir") + "/test.lock???").delete();
        super.tearDown();
    }

    /**
     * 必須プロパティ「lockFilePath」が設定されていない場合に例外がスローされることのテスト。
     */
    @Test
    public void testNotSetLockFilePathProperty() {

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.level", "ERROR");
        settings.put("monitorFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "10");
        settings.put("monitorFile.maxFileSize", "50000");

        SynchronousFileLogWriter writer = new SynchronousFileLogWriter();

        try {
            writer.initialize(
                    new ObjectSettings(new MockLogSettings(settings), "monitorFile"));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("'lockFilePath' was not specified. file path = [null], name = [monitorFile]"));
        }
    }

    /**
     * 必須プロパティ「lockFilePath」が設定されていない場合に例外がスローされることのテスト。
     */
    @Test
    public void testLockFileAlreadyDirectoryExists() {

        String lockFilePath = "tmp/" + getClass().getName() + "/lock";
        File file = new File(lockFilePath);
        file.mkdirs();

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.level", "ERROR");
        settings.put("monitorFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "10");
        settings.put("monitorFile.maxFileSize", "50000");
        settings.put("monitorFile.lockFilePath", lockFilePath);

        SynchronousFileLogWriter writer = new SynchronousFileLogWriter();

        try {
            writer.initialize(
                    new ObjectSettings(new MockLogSettings(settings), "monitorFile"));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("lock file path is already exists of directory. lock file path =[" + lockFilePath + "]"));
        }
    }

    /**
     * 初期処理完了後に設定情報が出力されること。
     * <p/>
     * lockWaitTimeと、lockRetryIntervalがデフォルト値のパターン。
     */
    @Test
    public void testInitializedMessage() {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.level", "INFO");
        settings.put("monitorFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "10");
        settings.put("monitorFile.maxFileSize", "50000");
        settings.put("monitorFile.lockFilePath", "./lock/test.lock");

        SynchronousFileLogWriter writer = new SynchronousFileLogWriter();

        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        writer.terminate();

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.contains("initialized."));
        assertTrue(appLog.contains("WRITER NAME        = [monitorFile]"));
        assertTrue(appLog.contains(
                "WRITER CLASS       = [nablarch.core.log.basic.SynchronousFileLogWriter]")); // Lockable
        assertTrue(appLog.contains(
                "FORMATTER CLASS    = [" + MockLogFormatter.class.getName()
                        + "]"));
        assertTrue(appLog.contains("LEVEL              = [INFO]"));
        assertTrue(appLog.contains(
                "FILE PATH          = [./log/lock-app.log]"));
        assertTrue(appLog.contains("ENCODING           = [UTF-8]"));
        assertTrue(appLog.contains("OUTPUT BUFFER SIZE = [10000]"));
        assertTrue(appLog.contains("FILE AUTO CHANGE   = [true]"));
        assertTrue(appLog.contains("MAX FILE SIZE      = [50000000]"));
        assertTrue(appLog.contains("CURRENT FILE SIZE  = [0]"));
        assertTrue(appLog.contains("terminated."));
        assertThat(appLog, containsString("LOCK FILE PATH                      = [./lock/test.lock]"));
        assertTrue(appLog.contains("LOCK RETRY INTERVAL                 = [1]"));
        assertTrue(appLog.contains("LOCK WAIT TIME                      = [1800]"));
        assertTrue(appLog.contains("FAILURE CODE CREATE LOCK FILE       = [null]"));
        assertTrue(appLog.contains("FAILURE CODE RELEASE LOCK FILE      = [null]"));
        assertTrue(appLog.contains("FAILURE CODE FORCE DELETE LOCK FILE = [null]"));
        assertTrue(appLog.contains("FAILURE CODE INTERRUPT LOCK WAIT    = [null]"));
        assertTrue(appLog.contains("terminated."));
    }

    /**
     * 初期処理完了後に設定情報が出力されること。
     */
    @Test
    public void testInitializedMessageMaxValue() {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.level", "INFO");
        settings.put("monitorFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "10");
        settings.put("monitorFile.maxFileSize", "50000");
        settings.put("monitorFile.lockFilePath", "./lock/test.lock");
        settings.put("monitorFile.lockRetryInterval", String.valueOf(Long.MAX_VALUE));
        settings.put("monitorFile.lockWaitTime", String.valueOf(Long.MAX_VALUE));

        SynchronousFileLogWriter writer = new SynchronousFileLogWriter();

        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        writer.terminate();

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.contains("initialized."));
        assertTrue(appLog.contains("WRITER NAME        = [monitorFile]"));
        assertTrue(appLog.contains(
                "WRITER CLASS       = [nablarch.core.log.basic.SynchronousFileLogWriter]")); // Lockable
        assertTrue(appLog.contains(
                "FORMATTER CLASS    = [" + MockLogFormatter.class.getName()
                        + "]"));
        assertTrue(appLog.contains("LEVEL              = [INFO]"));
        assertTrue(appLog.contains(
                "FILE PATH          = [./log/lock-app.log]"));
        assertTrue(appLog.contains("ENCODING           = [UTF-8]"));
        assertTrue(appLog.contains("OUTPUT BUFFER SIZE = [10000]"));
        assertTrue(appLog.contains("FILE AUTO CHANGE   = [true]"));
        assertTrue(appLog.contains("MAX FILE SIZE      = [50000000]"));
        assertTrue(appLog.contains("CURRENT FILE SIZE  = [0]"));
        assertTrue(appLog.contains("terminated."));
        assertThat(appLog, containsString("LOCK FILE PATH                      = [./lock/test.lock]"));
        assertTrue(appLog.contains("LOCK RETRY INTERVAL                 = [" + String.valueOf(Long.MAX_VALUE) + "]"));
        assertTrue(appLog.contains("LOCK WAIT TIME                      = [" + String.valueOf(Long.MAX_VALUE) + "]"));
        assertTrue(appLog.contains("terminated."));
    }


    /**
     * lockRetryIntervalが0の場合に例外がスローされること。
     */
    @Test
    public void testInitializedMessageLockRetryIntervalUnder() {

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.level", "ERROR");
        settings.put("monitorFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("monitorFile.filePath", "/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "10");
        settings.put("monitorFile.maxFileSize", "50000");
        settings.put("monitorFile.lockFilePath", "./lock/test.lock");
        settings.put("monitorFile.lockRetryInterval", "0");
        settings.put("monitorFile.lockWaitTime", "1");

        SynchronousFileLogWriter writer = new SynchronousFileLogWriter();

        try {
            writer.initialize(
                    new ObjectSettings(new MockLogSettings(settings), "monitorFile"));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("invalid property was specified. 'lockRetryInterval' must be more than 1. value=[0]."));
        }
    }

    /**
     * lockRetryIntervalがLongの最大値以上の場合に例外がスローされること。
     */
    @Test
    public void testInitializedMessageLockRetryIntervalOver() {

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.level", "ERROR");
        settings.put("monitorFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("monitorFile.filePath", "initialized-message-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "10");
        settings.put("monitorFile.maxFileSize", "50000");
        settings.put("monitorFile.lockFilePath", "./lock/test.lock");
        settings.put("monitorFile.lockRetryInterval", String.valueOf(new BigDecimal("9223372036854775808")));
        settings.put("monitorFile.lockWaitTime", "1");

        SynchronousFileLogWriter writer = new SynchronousFileLogWriter();

        try {
            writer.initialize(
                    new ObjectSettings(new MockLogSettings(settings), "monitorFile"));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("invalid property was specified. 'lockRetryInterval' must be able to convert to Long. value=[9223372036854775808]."));
        }
    }

    /**
     * lockWaitTimeが-1の場合に例外がスローされること。
     */
    @Test
    public void testInitializedMessageLockWaitTimeUnder() {

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.level", "ERROR");
        settings.put("monitorFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "10");
        settings.put("monitorFile.maxFileSize", "50000");
        settings.put("monitorFile.lockFilePath", "./lock/test.lock");
        settings.put("monitorFile.lockRetryInterval", "1");
        settings.put("monitorFile.lockWaitTime", "-1");

        SynchronousFileLogWriter writer = new SynchronousFileLogWriter();

        try {
            writer.initialize(
                    new ObjectSettings(new MockLogSettings(settings), "monitorFile"));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("invalid property was specified. 'lockWaitTime' must be more than 0. value=[-1]."));
        }
    }

    /**
     * lockWaitTimeがLongの最大値以上の場合に例外がスローされること。
     */
    @Test
    public void testInitializedMessageLockWaitTimeOver() {

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.level", "ERROR");
        settings.put("monitorFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "10");
        settings.put("monitorFile.maxFileSize", "50000");
        settings.put("monitorFile.lockFilePath", "./lock/test.lock");
        settings.put("monitorFile.lockRetryInterval", "1");
        settings.put("monitorFile.lockWaitTime", String.valueOf(new BigDecimal("9223372036854775808")));

        SynchronousFileLogWriter writer = new SynchronousFileLogWriter();

        try {
            writer.initialize(
                    new ObjectSettings(new MockLogSettings(settings), "monitorFile"));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("invalid property was specified. 'lockWaitTime' must be able to convert to Long. value=[9223372036854775808]."));
        }
    }


    /**
     * 初期処理完了後に障害通知コードが出力されること。
     */
    @Test
    public void testInitializedNotificationCode() {


        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.level", "INFO");
        settings.put("monitorFile.formatter.className",
                MockLogFormatter.class.getName());
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "10");
        settings.put("monitorFile.maxFileSize", "50000");
        settings.put("monitorFile.lockFilePath", "./lock/test.lock");
        settings.put("monitorFile.failureCodeCreateLockFile", "FW000001");
        settings.put("monitorFile.failureCodeReleaseLockFile", "FW000002");
        settings.put("monitorFile.failureCodeForceDeleteLockFile", "FW000003");
        settings.put("monitorFile.failureCodeInterruptLockWait", "FW000004");

        SynchronousFileLogWriter writer = new SynchronousFileLogWriter();

        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        writer.terminate();

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.contains("initialized."));
        assertTrue(appLog.contains("WRITER NAME        = [monitorFile]"));
        assertTrue(appLog.contains(
                "WRITER CLASS       = [nablarch.core.log.basic.SynchronousFileLogWriter]")); // Lockable
        assertTrue(appLog.contains(
                "FORMATTER CLASS    = [" + MockLogFormatter.class.getName()
                        + "]"));
        assertTrue(appLog.contains("LEVEL              = [INFO]"));
        assertTrue(appLog.contains(
                "FILE PATH          = [./log/lock-app.log]"));
        assertTrue(appLog.contains("ENCODING           = [UTF-8]"));
        assertTrue(appLog.contains("OUTPUT BUFFER SIZE = [10000]"));
        assertTrue(appLog.contains("FILE AUTO CHANGE   = [true]"));
        assertTrue(appLog.contains("MAX FILE SIZE      = [50000000]"));
        assertTrue(appLog.contains("CURRENT FILE SIZE  = [0]"));
        assertTrue(appLog.contains("terminated."));
        assertThat(appLog, containsString("LOCK FILE PATH                      = [./lock/test.lock]"));
        assertTrue(appLog.contains("LOCK RETRY INTERVAL                 = [1]"));
        assertTrue(appLog.contains("LOCK WAIT TIME                      = [1800]"));
        assertTrue(appLog.contains("FAILURE CODE CREATE LOCK FILE       = [FW000001]"));
        assertTrue(appLog.contains("FAILURE CODE RELEASE LOCK FILE      = [FW000002]"));
        assertTrue(appLog.contains("FAILURE CODE FORCE DELETE LOCK FILE = [FW000003]"));
        assertTrue(appLog.contains("FAILURE CODE INTERRUPT LOCK WAIT    = [FW000004]"));
        assertTrue(appLog.contains("terminated."));
    }



    /**
     * ロックファイルの生成・削除が正しく行われることの確認テスト。
     * <p/>
     * リトライは行わないパターン。
     */
    @Test
    public void testNormal() throws Exception {

        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock";
        new File(lockFilePath).delete();

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);


        /*
         * 通常のFileLogWriterの挙動。ログ出力前・ログ出力後にロックファイルが存在しないことを確認。
         */
        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        MockLockableFileLogWriter writer = new MockLockableFileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        for (int i = 0; i < 3; i++) {
            assertFalse(new File(lockFilePath).exists()); // ロックファイルは存在しない！
            writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[" + i + "]]]",
                    null));
            assertFalse(new File(lockFilePath).exists()); // ロックファイルは存在しない！
        }
        writer.terminate();

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        for (int i = 0; i < 3; i++) {
            assertTrue(appLog.indexOf("[[[" + i + "]]]") != -1); // 3行書きだされることを確認
        }
        assertTrue(appLog.indexOf("[[[515]]]") == -1);

        assertThat(writer.retryCount, is(0));


        /*
         * ロックファイル削除メソッドを動作しないようにして、ログ出力後にロックファイルが存在することを確認。（念のためロックファイルを作成していることを確認）
         */
        monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        SynchronousFileLogWriter nonDeleteLockFileWriter = new SynchronousFileLogWriter() {
            @Override
            protected void releaseLock(String formattedMessage, LogContext context) {
                // nop
            }
        };

        nonDeleteLockFileWriter.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        for (int i = 0; i < 3; i++) {
            assertFalse(new File(lockFilePath).exists()); // ロックファイルは存在しない！
            nonDeleteLockFileWriter.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[" + i + "]]]",
                    null));
            assertTrue(new File(lockFilePath).exists()); // ロックファイルは存在する！
            new File(lockFilePath).delete();
        }
        nonDeleteLockFileWriter.terminate();

        new File(lockFilePath).delete();
    }


    /**
     * リトライを1回するテスト。
     * <p>
     * メインスレッド側の仕様：10ミリ秒待った後に処理を開始。再試行間隔は「50」ミリ秒。<br/>
     * 子スレッド側の仕様：即時に処理を開始。ロック取得後に10ミリ秒スリープしてから、ログを出力し、ロックを解除。
     * </p>
     * <p>
     * 想定動作：<br/>
     *  メインスレッドは以下のとおり動作する。<br/>
     *  ・1回目の実行時（約25ミリ秒経過時）には、子スレッドがロックしているので、リトライを行う。<br/>
     *  ・3回目の実行時（約60ミリ秒経過時）には、子スレッドのロックが解除されているので、ロックの取得に成功し、ログを出力する。
     * </p>
     */
    @Test
    public void testLockRetry1() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock";
        new File(lockFilePath).delete();

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);
        settings.put("monitorFile.lockRetryInterval", "50");
        settings.put("monitorFile.lockWaitTime", "500");

        MockLockableFileLogWriter writer = new MockLockableFileLogWriter();

        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        Thread childThread = new Thread(new ChildThread(monitorFile, lockFilePath, 50));
        childThread.start();
        Thread.sleep(25);
        
        assertTrue(new File(lockFilePath).exists()); // ロックファイルは存在する！
        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]",
                null));
        assertFalse(new File(lockFilePath).exists()); // ロックファイルは存在しない！

        writer.terminate();


        assertThat(writer.retryCount, is(1)); // リトライ回数は1回

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        assertTrue(appLog.indexOf("[[[parentLog]]]") != -1);
        assertTrue(appLog.indexOf("[[[childLog]]]") != -1);

        assertTrue(appLog.indexOf("[[[parentLog]]]") > appLog.indexOf("[[[childLog]]]")); // childLogが先に出力されている

        Thread.sleep(1000);
    }

    /**
     * リトライを3回するテスト。
     * <p>
     * メインスレッド側の仕様：10ミリ秒待った後に処理を開始。再試行間隔は「50」ミリ秒。<br/>
     * 子スレッド側の仕様：即時に処理を開始。ロック取得後に150ミリ秒スリープしてから、ログを出力し、ロックを解除。
     * </p>
     * <p>
     * 想定動作：<br/>
     *  メインスレッドは以下のとおり動作する。<br/>
     *  ・1回目の実行時（約30ミリ秒経過時）には、子スレッドがロックしているので、リトライを行う。<br/>
     *  ・2回目の実行時（約90ミリ秒経過時）には、子スレッドがロックしているので、リトライを行う。<br/>
     *  ・3回目の実行時（約140ミリ秒経過時）には、子スレッドがロックしているので、リトライを行う。<br/>
     *  ・3回目の実行時（約190ミリ秒経過時）には、子スレッドのロックが解除されているので、ロックの取得に成功し、ログを出力する。
     * </p>
     */
    @Test
    public void testLockRetry3() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock";
        new File(lockFilePath).delete();


        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);
        settings.put("monitorFile.lockRetryInterval", "50");
        settings.put("monitorFile.lockWaitTime", "500");

        MockLockableFileLogWriter writer = new MockLockableFileLogWriter();

        Thread childThread = new Thread(new ChildThread(monitorFile, lockFilePath, 150));
        childThread.start();

        Thread.sleep(30);
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        assertTrue(new File(lockFilePath).exists()); // ロックファイルは存在する！
        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]",
                null));
        assertFalse(new File(lockFilePath).exists()); // ロックファイルは存在しない！

        writer.terminate();


        assertThat(writer.retryCount, is(3)); // リトライ回数は3回

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        assertTrue(appLog.indexOf("[[[parentLog]]]") != -1);
        assertTrue(appLog.indexOf("[[[childLog]]]") != -1);

        assertTrue(appLog.indexOf("[[[parentLog]]]") > appLog.indexOf("[[[childLog]]]")); // childLogが先に出力されている

        Thread.sleep(1000);

    }

    /** テスト用のSynchronousFileLogWriterサブクラス */
    private class MockLockableFileLogWriter extends SynchronousFileLogWriter {

        public int retryCount = 0;

        @Override
        protected boolean waitLock(File file, String formattedMessage, LogContext context) {
            super.waitLock(file, formattedMessage, context);
            retryCount++;
            return true;
        }
    }

    /** テストのために使用する子スレッド */
    private class ChildThread implements Runnable {

        private String lockFilePath = null;
        private int sleepTimeAfterExecutionLockFileMethod = 0;
        private int retryCount = 0;
        private int lockRetryInterval = 3;
        private int lockWaitTime = 0;
        private String failureCodeInterruptLockWait = null;
        private boolean locked = false;

        public ChildThread(File monitorFile, String lockFilePath, int sleepTimeAfterExecutionLockFileMethod) {
            this.lockFilePath = lockFilePath;
            this.sleepTimeAfterExecutionLockFileMethod = sleepTimeAfterExecutionLockFileMethod;
        }

        public ChildThread(File monitorFile, String lockFilePath, int sleepTimeAfterExecutionLockFileMethod, int lockRetryInterval, int lockWaitTime, String failureCodeInterruptLockWait) {
            this.lockFilePath = lockFilePath;
            this.sleepTimeAfterExecutionLockFileMethod = sleepTimeAfterExecutionLockFileMethod;
            this.lockRetryInterval = lockRetryInterval;
            this.lockWaitTime = lockWaitTime;
            this.failureCodeInterruptLockWait = failureCodeInterruptLockWait;
        }

        public void run() {

            Map<String, String> settings = new HashMap<String, String>();
            settings.put("monitorFile.filePath", "./log/lock-app.log");
            settings.put("monitorFile.encoding", "UTF-8");
            settings.put("monitorFile.outputBufferSize", "8");
            settings.put("monitorFile.lockFilePath", lockFilePath);
            settings.put("monitorFile.lockRetryInterval", String.valueOf(lockRetryInterval));
            settings.put("monitorFile.lockWaitTime", String.valueOf(lockWaitTime));
            settings.put("monitorFile.failureCodeInterruptLockWait", failureCodeInterruptLockWait);


            /*
             * 通常のFileLogWriterの挙動。ログ出力前・ログ出力後にロックファイルが存在しないことを確認。
             */
            SynchronousFileLogWriter writer = new SynchronousFileLogWriter() {
                @Override
                protected boolean lockFile(String formattedMessage, LogContext context) {
                    assertTrue(super.lockFile(formattedMessage, context)); // ロックファイル作成に成功
                    assertTrue(new File(lockFilePath).exists()); // ロックファイルは存在する！
                    locked = true;
                    try {
                        Thread.sleep(sleepTimeAfterExecutionLockFileMethod); // 指定時間スリープ
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }

                @Override
                protected boolean waitLock(File file, String formattedMessage, LogContext context) {
                    super.waitLock(file, formattedMessage, context);
                    retryCount++;
                    return true;
                }
            };

            writer.initialize(
                    new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

            writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[childLog]]]",
                    null));

            writer.terminate();

        }

        public boolean isLocked() {
            return locked;
        }
    }

    /**
     * 再試行時間が0の場合に再試行が行われずロックファイルの強制削除に成功し、ロックの取得ができることのテスト。（※実際の運用でこのような値を設定することは考えられない）
     * <p>
     * メインスレッド側の仕様：子スレッドを立ち上げ10ミリ秒待った後に処理を開始。再試行間隔は「50」ミリ秒。<br/>
     * 子スレッド側の仕様：即時に処理を開始。ロック取得後に50ミリ秒スリープしてから、ログを出力し、ロックを解除。
     * </p>
     * <p>
     * 想定動作：<br/>
     *  メインスレッドは以下のとおり動作する。<br/>
     *  ・1回目の実行時（約10ミリ秒経過時）には、子スレッドがロックしていて、かつlockWaitTimeが0なので、強制的にファイル削除を行い、ログを出力する。
     * </p>
     */
    @Test
    public void testLockWaitTime0() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock";
        new File(lockFilePath).delete();

        Thread childThread = new Thread(new ChildThread(monitorFile, lockFilePath, 50));
        childThread.start();

        Thread.sleep(10);


        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);
        settings.put("monitorFile.lockRetryInterval", "50");
        settings.put("monitorFile.lockWaitTime", "0");

        MockLockableFileLogWriter writer = new MockLockableFileLogWriter();

        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        assertTrue(new File(lockFilePath).exists()); // ロックファイルは存在する！
        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]",
                null));
        assertFalse(new File(lockFilePath).exists()); // ロックファイルは存在しない！（存在するロックファイルが強制的に削除される）

        writer.terminate();


        Thread.sleep(50);

        assertThat(writer.retryCount, is(0)); // リトライ回数は0回

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        assertTrue(appLog.indexOf("[[[parentLog]]]") != -1);
        assertTrue(appLog.indexOf("[[[childLog]]]") != -1);

        assertTrue(appLog.indexOf("[[[parentLog]]]") < appLog.indexOf("[[[childLog]]]")); // parentLogが先に出力されている

    }

    /**
     * 再試行時間「150」を超えた場合にロックファイルの強制削除に成功し、ロックの取得ができることのテスト。（※実際の運用でこのような値を設定することは考えられない）
     * <p>
     * メインスレッド側の仕様：子スレッドを立ち上げ50ミリ秒待った後に処理を開始。再試行間隔は「60」ミリ秒。<br/>
     * 子スレッド側の仕様：即時に処理を開始。ロック取得後に300ミリ秒スリープしてから、ログを出力し、ロックを解除。
     * </p>
     * <p>
     * 想定動作：<br/>
     *  メインスレッドは以下のとおり動作する。<br/>
     *  ・1回目の実行時（約0ミリ秒経過時）には、子スレッドがロックしているので、リトライを行う。
     *  ・2回目の実行時（約60ミリ秒経過時）には、子スレッドがロックしているので、リトライを行う。
     *  ・3回目の実行時（約120ミリ秒経過時）には、子スレッドがロックしているので、リトライを行う。
     *  ・4回目の実行時（約180ミリ秒経過時）には、lockWaitTimeの150を超えるので、強制的にファイル削除を行い、ログを出力する。
     * </p>
     */
    @Test
    public void testLockWaitTime150() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock";
        new File(lockFilePath).delete();

        Thread childThread = new Thread(new ChildThread(monitorFile, lockFilePath, 300));
        childThread.start();

        Thread.sleep(50);


        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);
        settings.put("monitorFile.lockRetryInterval", "60");
        settings.put("monitorFile.lockWaitTime", "150");

        MockLockableFileLogWriter writer = new MockLockableFileLogWriter();

        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        assertTrue(new File(lockFilePath).exists()); // ロックファイルは存在する！
        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]",
                null));
        assertFalse(new File(lockFilePath).exists()); // ロックファイルは存在しない！（存在するロックファイルが強制的に削除される）

        writer.terminate();

        // 子スレッドの終了が250msだと終わらない場合を考慮して、10回は待つ
        for (int i = 0; i < 10; i++) {
            Thread.sleep(250); // 子スレッドが終了するのを待つ
            if (writer.retryCount >= 3) {
                break;
            }
        }

        assertThat(writer.retryCount, is(3)); // リトライ回数は3回

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        assertTrue(appLog.indexOf("[[[parentLog]]]") != -1);
        assertTrue(appLog.indexOf("[[[childLog]]]") != -1);

        assertTrue(appLog.indexOf("[[[parentLog]]]") < appLog.indexOf("[[[childLog]]]")); // parentLogが先に出力されている
    }



    /**
     * 再試行時間「0」を超えた場合にロックファイルの強制削除に失敗するテスト。
     * <p/>
     * {@link #testLockWaitTime150()}と同様の仕様でテストを行い、ファイル強制削除のタイミングでモックを使用して削除を失敗させる。
     */
    @Test
    public void testLockFileForceDeleteFailure() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock";
        new File(lockFilePath).delete();

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);
        settings.put("monitorFile.lockRetryInterval", "60");
        settings.put("monitorFile.lockWaitTime", "150");


        ChildThread child = new ChildThread(monitorFile, lockFilePath, 300);
        Thread childThread = new Thread(child);
        childThread.start();

        Thread.yield();

        // 子スレッドがロックファイルを握るのを待つ
        for (int i = 0; i < 100; i++) {
            Thread.sleep(50);
            if (child.isLocked()) {
                break;
            }
        }

        MockLockableFileLogWriter writer = new MockLockableFileLogWriter() {
            @Override
            protected boolean deleteLockFileExceedsLockWaitTime(File lockFile, String formattedMessage, LogContext context) {
                return super.deleteLockFileExceedsLockWaitTime(new File("./log/nonExist"), formattedMessage, context);
            }
        };

        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]",
                null));

        writer.terminate();

        // 子スレッドの終了が250msだと終わらない場合を考慮して、10回は待つ
        for (int i = 0; i < 10; i++) {
            Thread.sleep(250); // 子スレッドが終了するのを待つ
            if (writer.retryCount >= 3) {
                break;
            }
        }

        assertThat(writer.retryCount, is(3)); // リトライ回数は3回

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        assertTrue(appLog.indexOf("[[[parentLog]]]") != -1); // 強制的にログが出力されている
        assertTrue(appLog.indexOf("[[[childLog]]]") != -1);

        assertTrue(appLog.indexOf("[[[parentLog]]]") < appLog.indexOf("[[[childLog]]]")); // parentLogが先に出力されている

        assertTrue(appLog.indexOf("failed to delete lock file forcedly. lock file was opened illegally.") != -1); // ロックファイルが不正に開かれている旨のメッセージがログに出力されている
        assertTrue(appLog.indexOf("lock file path=[" + new File("./log/nonExist").getAbsolutePath() + "].") != -1);
    }


    /**
     * {@link #testLockFileForceDeleteFailure}テストの障害通知ログを設定するパターン。
     */
    @Test
    public void testLockFileForceDeleteFailureNotification() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock";
        new File(lockFilePath).delete();

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);
        settings.put("monitorFile.lockRetryInterval", "60");
        settings.put("monitorFile.lockWaitTime", "150");
        settings.put("monitorFile.failureCodeForceDeleteLockFile", "FW000001");

        ChildThread child = new ChildThread(monitorFile, lockFilePath, 300);
        Thread childThread = new Thread(child);
        childThread.start();

        Thread.yield();

        // 子スレッドがロックファイルを握るのを待つ
        for (int i = 0; i < 100; i++) {
            Thread.sleep(50);
            if (child.isLocked()) {
                break;
            }
        }

        MockLockableFileLogWriter writer = new MockLockableFileLogWriter() {
            @Override
            protected boolean deleteLockFileExceedsLockWaitTime(File lockFile, String formattedMessage, LogContext context) {
                return super.deleteLockFileExceedsLockWaitTime(new File("./log/nonExist"), formattedMessage, context);
            }
        };

        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]",
                null));

        writer.terminate();

        // 子スレッドの終了が250msだと終わらない場合を考慮して、10回は待つ
        for (int i = 0; i < 10; i++) {
            Thread.sleep(250); // 子スレッドが終了するのを待つ
            if (writer.retryCount >= 3) {
                break;
            }
        }

        assertThat(writer.retryCount, is(3)); // リトライ回数は3回

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        assertTrue(appLog.indexOf("[[[parentLog]]]") != -1); // 強制的にログが出力されている
        assertTrue(appLog.indexOf("[[[childLog]]]") != -1);

        assertTrue(appLog.indexOf("[[[parentLog]]]") < appLog.indexOf("[[[childLog]]]")); // parentLogが先に出力されている

        // 障害通知ログが出力されている
        assertTrue(appLog.indexOf("fail_code = [FW000001]") != -1);
        assertTrue(appLog.indexOf("ロックの取得に失敗しました。ロックファイルが不正に開かれています。") != -1);
        assertTrue(appLog.indexOf("ロックファイルパス=[" + new File("./log/nonExist").getAbsolutePath() + "]") != -1);
    }

    /**
     * スレッドがスリープしているときに、割り込みが発生した場合に、InterruptedExceptionをラップしたRuntimeException例外がスローされるテスト。
     */
    @Test
    public void testInterrupt() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock";
        new File(lockFilePath).delete();

        // ロックファイルを事前に作成しておき、子スレッドを500ミリ秒スリープさせる
        new File(lockFilePath).createNewFile();

        ChildThread childThreadInstance = new ChildThread(monitorFile, lockFilePath, 0, 500, 1000, null); // 500ミリ秒スリープ

        Thread childThread = new Thread(childThreadInstance);
        childThread.start();

        Thread.sleep(100);

        childThread.interrupt(); // 子スレッドに割り込み命令を投げる

        childThread.join();

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("[[[childLog]]]") != -1); // 強制的にログが出力されている

        assertTrue(appLog.indexOf("interrupted while waiting for lock retry.") != -1); // ロックファイルが生成出来なかった旨のメッセージがログに出力されている
    }


    /**
     * {@link #testInterrupt}テストの障害通知ログを設定するパターン。
     */
    @Test
    public void testInterruptNotification() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");
        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock";
        new File(lockFilePath).delete();

        // ロックファイルを事前に作成しておき、子スレッドを500ミリ秒スリープさせる
        new File(lockFilePath).createNewFile();

        ChildThread childThreadInstance = new ChildThread(monitorFile, lockFilePath, 0, 500, 1000, "FW000004"); // 500ミリ秒スリープ

        Thread childThread = new Thread(childThreadInstance);
        childThread.start();

        Thread.sleep(100);

        childThread.interrupt(); // 子スレッドに割り込み命令を投げる

        Thread.sleep(100);

        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("[[[childLog]]]") != -1); // 強制的にログが出力されている

        assertTrue(appLog.indexOf("fail_code = [FW000004] ") != -1); // 障害通知ログが出力されている
        assertTrue(appLog.indexOf("ロック取得中に割り込みが発生しました。") != -1);
    }

    /**
     * ロックファイルの生成に失敗しIOExceptionがスローされるテスト。
     */
    @Test
    public void testCreateNewFileFailed() throws Exception {

        // Windows環境でない場合は終了する
        if(!getOsName().contains("windows")){
            return;
        }

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");

        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock???";
        new File(lockFilePath).delete();

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);
        settings.put("monitorFile.failureCodeCreateLockFile", "FW000004");

        MockLockableFileLogWriter writer = new MockLockableFileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]", null));

        writer.terminate();


        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        assertTrue(appLog.indexOf("[[[parentLog]]]") != -1); // 強制的にログが出力されている

        assertTrue(appLog.indexOf("fail_code = [FW000004] ") != -1); // 障害通知ログが出力されている
        assertTrue(appLog.indexOf("ロック取得中に割り込みが発生しました。") != -1);
    }

    /**
     * {@link #testCreateNewFileFailed}テストの障害通知ログを設定するパターン。
     */
    @Test
    public void testCreateNewFileFailedNotification() throws Exception {

        // Windows環境でない場合は終了する
        if(!getOsName().contains("windows")){
            return;
        }

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");

        String lockFilePath = System.getProperty("java.io.tmpdir") + "/test.lock???";
        // java.io.tmpdirが返却する文字列のpath.sepの有無に依存しないようにFileオブジェクトのAPIを利用して正規化する。
        lockFilePath = new File(lockFilePath).getAbsolutePath();
        new File(lockFilePath).delete();

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);
        settings.put("monitorFile.failureCodeCreateLockFile", "FW000002");


        MockLockableFileLogWriter writer = new MockLockableFileLogWriter();
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]", null));

        writer.terminate();


        String appLog = LogTestUtil.getLog(monitorFile);
        assertThat(appLog, containsString("initialized."));
        assertThat(appLog, containsString("terminated."));
        assertThat("強制的にログが出力されている", appLog, containsString("[[[parentLog]]]"));

        assertThat("障害通知ログが出力されている", appLog, containsString("fail_code = [FW000002] "));
        assertThat(appLog, containsString("ロックの取得に失敗しました。ロックファイルが生成できません。"));
        assertThat(appLog, containsString("ロックファイルパス=[" + lockFilePath + "]"));
    }

    /**
     * 生成したロックファイルの削除に失敗するテスト。
     */
    @Test
    public void testLockFileReleaseFailed() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");

        final String lockFilePath = System.getProperty("java.io.tmpdir")
            + (System.getProperty("java.io.tmpdir").endsWith(File.separator) ? "":File.separator)
            + "test.lock";
        new File(lockFilePath).delete();

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);

        MockLockableFileLogWriter writer = new MockLockableFileLogWriter(){
            @Override
            protected void releaseLock(String formattedMessage,
                    LogContext context) {
                new File(lockFilePath).delete(); // FWが解放するまえに削除しておく
                super.releaseLock(formattedMessage, context);
            }
        };
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]", null));

        writer.terminate();


        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        assertTrue(appLog.indexOf("[[[parentLog]]]") != -1); // 強制的にログが出力されている

        assertTrue(appLog.indexOf("failed to delete lock file.") != -1); // ロックファイルが削除出来なかった旨のメッセージがログに出力されている
        assertThat(appLog, containsString("lock file path=[" + lockFilePath + "]."));

    }

    /**
     * {@link #testLockFileReleaseFailed}テストの障害通知ログを設定するパターン。
     */
    @Test
    public void testLockFileReleaseFailedNotification() throws Exception {

        File monitorFile = LogTestUtil.cleanupLog("/lock-app.log");

        final String lockFilePath = System.getProperty("java.io.tmpdir")
            + (System.getProperty("java.io.tmpdir").endsWith(File.separator) ? "":File.separator)
            + "test.lock";
        new File(lockFilePath).delete();

        Map<String, String> settings = new HashMap<String, String>();
        settings.put("monitorFile.filePath", "./log/lock-app.log");
        settings.put("monitorFile.encoding", "UTF-8");
        settings.put("monitorFile.outputBufferSize", "8");
        settings.put("monitorFile.lockFilePath", lockFilePath);
        settings.put("monitorFile.failureCodeReleaseLockFile", "FW000003");

        MockLockableFileLogWriter writer = new MockLockableFileLogWriter(){
            @Override
            protected void releaseLock(String formattedMessage,
                    LogContext context) {
                new File(lockFilePath).delete(); // FWが解放するまえに削除しておく
                super.releaseLock(formattedMessage, context);
            }
        };
        writer.initialize(
                new ObjectSettings(new MockLogSettings(settings), "monitorFile"));

        writer.write(new LogContext(FQCN, LogLevel.DEBUG, "[[[parentLog]]]", null));

        writer.terminate();


        String appLog = LogTestUtil.getLog(monitorFile);
        assertTrue(appLog.indexOf("initialized.") != -1);
        assertTrue(appLog.indexOf("terminated.") != -1);
        assertTrue(appLog.indexOf("[[[parentLog]]]") != -1); // 強制的にログが出力されている


        assertTrue(appLog.indexOf("fail_code = [FW000003] ") != -1); // 障害通知ログが出力されている
        assertTrue(appLog.indexOf("ロックの解放に失敗しました。生成したロックファイルを削除できません。") != -1);
        assertThat(appLog, containsString("ロックファイルパス=[" + lockFilePath + "]"));
    }



    /**
     * OS名を取得する。
     * @return OS名
     */
    private String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }

}
