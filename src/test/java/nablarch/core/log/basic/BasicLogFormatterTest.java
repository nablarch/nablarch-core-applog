package nablarch.core.log.basic;

import nablarch.core.ThreadContext;
import nablarch.core.log.*;
import nablarch.core.log.app.OnMemoryLogWriter;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

/**
 * {@link BasicLogFormatter}のテスト。
 * @author Kiyohito Itoh
 */
public class BasicLogFormatterTest extends LogTestSupport {
    
    /**
     * カスタムのログ出力項目で正しくフォーマットできること。
     */
    @Test
    public void testCustomLogItem() throws Exception {
        
        System.setProperty("nablarch.bootProcess", "APP001");
        System.setProperty("nablarch.processingSystem", "1");
        
        ThreadContext.setUserId("0000000001");
        ThreadContext.setRequestId("USERS00302");
        ThreadContext.setExecutionId(LogUtil.generateExecutionId());
        
        Logger logger = LoggerManager.get(BasicLogFormatterTest.class);
        
        OnMemoryLogWriter.clear();
        
        logger.logInfo("test_message");
        
        List<String> messages = OnMemoryLogWriter.getMessages("writer.customLog");
        
        assertThat(messages.get(0), is("INFO ROO CUSTOM_PROCESS" + Logger.LS));
    }
    
    /**
     * デフォルトのフォーマットで出力できること。
     */
    @Test
    public void testDefaultFormat() throws Exception {
        
        System.setProperty("nablarch.bootProcess", "APP001");
        
        ThreadContext.setUserId("0000000001");
        ThreadContext.setRequestId("USERS00302");
        ThreadContext.setExecutionId(LogUtil.generateExecutionId());
        
        User user = new User(null, "山田太郎", 28);
        String userId = null;
        String name = "山田花子";
        long price = 2000000;
        char c = 'K';
        Date date = new SimpleDateFormat("yyyyMMdd").parse("20110214");
        
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("nablarch.processingSystem", "1");
        LogFormatter formatter = new BasicLogFormatter();
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));
        
        for (LogLevel level : LogLevel.values()) {
            
            String message = formatter.format(new LogContext("root", level, "想定していない例外が発生しました。\uD83D\uDE0E",
                                                             new IllegalArgumentException("userId was null."),
                                                             user, userId, name, price, c, date));
            
            String[] splitMsgs = message.split(" |" + Logger.LS);
            int index = 0;
            
            assertNotNull(new SimpleDateFormat("yyyy-MM-dd").parse(splitMsgs[index++]));
            assertNotNull(new SimpleDateFormat("HH:mm:ss.SSS").parse(splitMsgs[index++]));
            assertThat(splitMsgs[index++], is("-" + level.name() + "-"));
            assertThat(splitMsgs[index++], is("root"));
            assertTrue(Pattern.matches("^\\[.*\\]$", splitMsgs[index++]));
            assertThat(splitMsgs[index++], is("boot_proc"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[APP001]"));
            assertThat(splitMsgs[index++], is("proc_sys"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[1]"));
            assertThat(splitMsgs[index++], is("req_id"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[USERS00302]"));
            assertThat(splitMsgs[index++], is("usr_id"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[0000000001]"));
            assertThat(splitMsgs[index++], is("想定していない例外が発生しました。\uD83D\uDE0E"));

            assertThat(message, containsString("Object Information[0]: Class Name = [nablarch.core.log.basic.User]"));
            assertThat(message, containsString("id = [null]"));
            assertThat(message, containsString("name = [山田太郎]"));
            assertThat(message, containsString("age = [28]"));
            assertThat(message, containsString("Object Information[1]: null"));
            assertThat(message, containsString("Object Information[2]: Class Name = [java.lang.String]"));
            assertThat(message, containsString("toString() = [山田花子]"));
            assertThat(message, containsString("Object Information[3]: Class Name = [java.lang.Long]"));
            assertThat(message, containsString("toString() = [2000000]"));
            assertThat(message, containsString("Object Information[4]: Class Name = [java.lang.Character]"));
            assertThat(message, containsString("toString() = [K]"));
            assertThat(message, containsString("Object Information[5]: Class Name = [java.util.Date]"));
            assertThat(message, containsString("Stack Trace Information :"));
            assertThat(message, containsString("java.lang.IllegalArgumentException: userId was null."));
        }
        
        System.getProperties().remove("nablarch.bootProcess");
        
        settings = new HashMap<String, String>();
        formatter = new BasicLogFormatter();
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));
        
        for (LogLevel level : LogLevel.values()) {
            
            String message = formatter.format(new LogContext("root", level, "想定していない例外が発生しました。",
                                                             new IllegalArgumentException("userId was null."),
                                                             user, userId, name, price));
            String[] splitMsgs = message.split(" |" + Logger.LS);
            int index = 0;
            assertNotNull(new SimpleDateFormat("yyyy-MM-dd").parse(splitMsgs[index++]));
            assertNotNull(new SimpleDateFormat("HH:mm:ss.SSS").parse(splitMsgs[index++]));
            assertThat(splitMsgs[index++], is("-" + level.name() + "-"));
            assertThat(splitMsgs[index++], is("root"));
            assertTrue(Pattern.matches("^\\[.*\\]$", splitMsgs[index++]));
            assertThat(splitMsgs[index++], is("boot_proc"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[]"));
            assertThat(splitMsgs[index++], is("proc_sys"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[]"));
            assertThat(splitMsgs[index++], is("req_id"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[USERS00302]"));
            assertThat(splitMsgs[index++], is("usr_id"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[0000000001]"));
            assertThat(splitMsgs[index++], is("想定していない例外が発生しました。"));
            
            assertThat(message, containsString("Object Information[0]: Class Name = [nablarch.core.log.basic.User]"));
            assertThat(message, containsString("id = [null]"));
            assertThat(message, containsString("name = [山田太郎]"));
            assertThat(message, containsString("age = [28]"));
            assertThat(message, containsString("Object Information[1]: null"));
            assertThat(message, containsString("Object Information[2]: Class Name = [java.lang.String]"));
            assertThat(message, containsString("toString() = [山田花子]"));
            assertThat(message, containsString("Object Information[3]: Class Name = [java.lang.Long]"));
            assertThat(message, containsString("toString() = [2000000]"));
            assertThat(message, containsString("Stack Trace Information :"));
            assertThat(message, containsString("java.lang.IllegalArgumentException: userId was null."));
        }
    }
    
    /**
     * null指定でもエラーにならないこと。
     */
    @Test
    public void testNull() throws Exception {
        
        ThreadContext.setUserId(null);
        ThreadContext.setRequestId(null);
        ThreadContext.setExecutionId(null);
        
        Map<String, String> settings = new HashMap<String, String>();
        
        LogFormatter formatter = new BasicLogFormatter();
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));
        
        // all arguments are null.
        
        User user = null;
        String userId = null;
        String name = null;
        Long price = null;
        
        String loggerName = null;
        String msg = null;
        Throwable error = null;
        
        LogLevel level = LogLevel.ERROR;
        
        String message = formatter.format(new LogContext(loggerName, level, msg, error, user, userId, name, price));
        
        String[] splitMsgs = message.split(" |" + Logger.LS);
        int index = 0;
        
        assertNotNull(new SimpleDateFormat("yyyy-MM-dd").parse(splitMsgs[index++]));
        assertNotNull(new SimpleDateFormat("HH:mm:ss.SSS").parse(splitMsgs[index++]));
        assertThat(splitMsgs[index++], is("-" + level.name() + "-"));
        assertThat(splitMsgs[index++], is("null"));
        assertThat(splitMsgs[index++], is("[null]"));
        assertThat(splitMsgs[index++], is("boot_proc"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[]"));
        assertThat(splitMsgs[index++], is("proc_sys"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[]"));
        assertThat(splitMsgs[index++], is("req_id"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[null]"));
        assertThat(splitMsgs[index++], is("usr_id"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[null]"));
        
        splitMsgs = message.split(Logger.LS);
        index = 1;
        
        assertThat(splitMsgs[index++].trim(), is("Object Information[0]: null"));
        assertThat(splitMsgs[index++].trim(), is("Object Information[1]: null"));
        assertThat(splitMsgs[index++].trim(), is("Object Information[2]: null"));
        assertThat(splitMsgs[index++].trim(), is("Object Information[3]: null"));
        
        assertThat(message, not(containsString("Stack Trace Information :")));
        
        // options is null.
        
        Object[] options = null;
        message = formatter.format(new LogContext(loggerName, level, msg, error, options));
        
        splitMsgs = message.split(" |" + Logger.LS);
        index = 0;
        
        assertNotNull(new SimpleDateFormat("yyyy-MM-dd").parse(splitMsgs[index++]));
        assertNotNull(new SimpleDateFormat("HH:mm:ss.SSS").parse(splitMsgs[index++]));
        assertThat(splitMsgs[index++], is("-" + level.name() + "-"));
        assertThat(splitMsgs[index++], is("null"));
        assertThat(splitMsgs[index++], is("[null]"));
        assertThat(splitMsgs[index++], is("boot_proc"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[]"));
        assertThat(splitMsgs[index++], is("proc_sys"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[]"));
        assertThat(splitMsgs[index++], is("req_id"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[null]"));
        assertThat(splitMsgs[index++], is("usr_id"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[null]"));
        
        splitMsgs = message.split(Logger.LS);
        index = 1;
        
        assertThat(message, not(containsString("Object Information")));
        assertThat(message, not(containsString("Stack Trace Information :")));
        
        // options is empty.
        
        options = new Object[0];
        message = formatter.format(new LogContext(loggerName, level, msg, error, options));
        
        splitMsgs = message.split(" |" + Logger.LS);
        index = 0;
        
        assertNotNull(new SimpleDateFormat("yyyy-MM-dd").parse(splitMsgs[index++]));
        assertNotNull(new SimpleDateFormat("HH:mm:ss.SSS").parse(splitMsgs[index++]));
        assertThat(splitMsgs[index++], is("-" + level.name() + "-"));
        assertThat(splitMsgs[index++], is("null"));
        assertThat(splitMsgs[index++], is("[null]"));
        assertThat(splitMsgs[index++], is("boot_proc"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[]"));
        assertThat(splitMsgs[index++], is("proc_sys"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[]"));
        assertThat(splitMsgs[index++], is("req_id"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[null]"));
        assertThat(splitMsgs[index++], is("usr_id"));
        assertThat(splitMsgs[index++], is("="));
        assertThat(splitMsgs[index++], is("[null]"));
        
        splitMsgs = message.split(Logger.LS);
        index = 1;
        
        assertThat(message, not(containsString("Object Information")));
        assertThat(message, not(containsString("Stack Trace Information :")));
    }

    /**
     * ログレベルのログ出力文言を変更できること。
     */
    @Test
    public void testLevelLabelSettings() throws Exception {
        
        System.setProperty("nablarch.bootProcess", "APP001");
        
        ThreadContext.setUserId("0000000001");
        ThreadContext.setRequestId("USERS00302");
        ThreadContext.setExecutionId(LogUtil.generateExecutionId());
        
        User user = new User(null, "山田太郎", 28);
        String userId = null;
        String name = "山田花子";
        long price = 2000000;
        
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("formatter.label.fatal", "F");
        settings.put("formatter.label.error", "E");
        settings.put("formatter.label.warn", "W");
        settings.put("formatter.label.info", "I");
        settings.put("formatter.label.debug", "D");
        settings.put("formatter.label.trace", "T");
        settings.put("nablarch.processingSystem", "1");
        
        LogFormatter formatter = new BasicLogFormatter();
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));
        
        for (LogLevel level : LogLevel.values()) {
            
            String message = formatter.format(new LogContext("root", level, "想定していない例外が発生しました。",
                                                             new IllegalArgumentException("userId was null."),
                                                             user, userId, name, price));
            
            String[] splitMsgs = message.split(" |" + Logger.LS);
            int index = 0;
            
            assertNotNull(new SimpleDateFormat("yyyy-MM-dd").parse(splitMsgs[index++]));
            assertNotNull(new SimpleDateFormat("HH:mm:ss.SSS").parse(splitMsgs[index++]));
            assertThat(splitMsgs[index++], is("-" + level.name().substring(0, 1) + "-"));
            assertThat(splitMsgs[index++], is("root"));
            assertTrue(Pattern.matches("^\\[.*\\]$", splitMsgs[index++]));
            assertThat(splitMsgs[index++], is("boot_proc"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[APP001]"));
            assertThat(splitMsgs[index++], is("proc_sys"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[1]"));
            assertThat(splitMsgs[index++], is("req_id"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[USERS00302]"));
            assertThat(splitMsgs[index++], is("usr_id"));
            assertThat(splitMsgs[index++], is("="));
            assertThat(splitMsgs[index++], is("[0000000001]"));
            assertThat(splitMsgs[index++], is("想定していない例外が発生しました。"));
            
            splitMsgs = message.split(Logger.LS);
            index = 1;
            
            assertThat(message, containsString("Object Information[0]: Class Name = [nablarch.core.log.basic.User]"));
            assertThat(message, containsString("id = [null]"));
            assertThat(message, containsString("name = [山田太郎]"));
            assertThat(message, containsString("age = [28]"));
            assertThat(message, containsString("Object Information[1]: null"));
            assertThat(message, containsString("Object Information[2]: Class Name = [java.lang.String]"));
            assertThat(message, containsString("toString() = [山田花子]"));
            assertThat(message, containsString("Object Information[3]: Class Name = [java.lang.Long]"));
            assertThat(message, containsString("toString() = [2000000]"));
            assertThat(message, containsString("Stack Trace Information :"));
            assertThat(message, containsString("java.lang.IllegalArgumentException: userId was null."));
        }
    }
    
    /**
     * フォーマット指定して出力できること。
     */
    @Test
    public void testFormatSettings() throws Exception {
        
        System.setProperty("nablarch.bootProcess", "APP001");
        
        ThreadContext.setUserId("0000000001");
        ThreadContext.setRequestId("USERS00302");
        ThreadContext.setExecutionId(LogUtil.generateExecutionId());
        
        User user = new User(null, "山田太郎", 28);
        String userId = null;
        String name = "山田花子";
        long price = 2000000;
        
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("formatter.format", "$date$ <$logLevel$> $loggerName$ $runtimeLoggerName$ [$executionId$]"
                                         + " boot_proc : [$bootProcess$] proc_sys : [$processingSystem$]"
                                         + " req_id : [$requestId$] usr_id : [$userId$]"
                                         + " $message$$stackTrace$$information$");
        settings.put("formatter.datePattern", "yyyy/MM/dd HH-mm-ss[SSS]");
        settings.put("nablarch.processingSystem", "1");
        
        LogFormatter formatter = new BasicLogFormatter();
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));
        
        for (LogLevel level : LogLevel.values()) {
            
            String message = formatter.format(new LogContext("root", "nablarch.core.log.basic.BasicLogFormatterTest", level, "想定していない例外が発生しました。",
                                                             new IllegalArgumentException("userId was null."),
                                                             user, userId, name, price));
            
            String[] splitMsgs = message.split(" |" + Logger.LS);
            int index = 0;
            
            assertNotNull(new SimpleDateFormat("yyyy/MM/dd").parse(splitMsgs[index++]));
            assertNotNull(new SimpleDateFormat("HH-mm-ss[SSS]").parse(splitMsgs[index++]));
            assertThat(splitMsgs[index++], is("<" + level.name() + ">"));
            assertThat(splitMsgs[index++], is("root"));
            assertThat(splitMsgs[index++], is("nablarch.core.log.basic.BasicLogFormatterTest"));
            assertTrue(Pattern.matches("^\\[.*\\]$", splitMsgs[index++]));
            assertThat(splitMsgs[index++], is("boot_proc"));
            assertThat(splitMsgs[index++], is(":"));
            assertThat(splitMsgs[index++], is("[APP001]"));
            assertThat(splitMsgs[index++], is("proc_sys"));
            assertThat(splitMsgs[index++], is(":"));
            assertThat(splitMsgs[index++], is("[1]"));
            assertThat(splitMsgs[index++], is("req_id"));
            assertThat(splitMsgs[index++], is(":"));
            assertThat(splitMsgs[index++], is("[USERS00302]"));
            assertThat(splitMsgs[index++], is("usr_id"));
            assertThat(splitMsgs[index++], is(":"));
            assertThat(splitMsgs[index++], is("[0000000001]"));
            assertThat(splitMsgs[index++], is("想定していない例外が発生しました。"));
            
            assertThat(message, containsString("Stack Trace Information :"));
            assertThat(message, containsString("java.lang.IllegalArgumentException: userId was null."));
            
            assertThat(message, containsString("Object Information[0]: Class Name = [nablarch.core.log.basic.User]"));
            assertThat(message, containsString("id = [null]"));
            assertThat(message, containsString("name = [山田太郎]"));
            assertThat(message, containsString("age = [28]"));
            assertThat(message, containsString("Object Information[1]: null"));
            assertThat(message, containsString("Object Information[2]: Class Name = [java.lang.String]"));
            assertThat(message, containsString("toString() = [山田花子]"));
            assertThat(message, containsString("Object Information[3]: Class Name = [java.lang.Long]"));
            assertThat(message, containsString("toString() = [2000000]"));
        }
    }

    /**
     * 末尾にLFが追加されること。
     */
    @Test
    public void testEndWithLineSeparator() throws Exception {
        
        System.setProperty("nablarch.bootProcess", "APP001");
        
        ThreadContext.setUserId("0000000001");
        ThreadContext.setRequestId("USERS00302");
        ThreadContext.setExecutionId(LogUtil.generateExecutionId());
        
        Map<String, String> settings = new HashMap<String, String>();
        settings.put("formatter.format", "<$logLevel$> $loggerName$ $message$");
        settings.put("formatter.datePattern", "yyyy/MM/dd HH-mm-ss[SSS]");
        settings.put("nablarch.processingSystem", "1");
        
        LogFormatter formatter = new BasicLogFormatter();
        formatter.initialize(new ObjectSettings(new MockLogSettings(settings), "formatter"));
        
        LogLevel level = LogLevel.TRACE;
        String message = formatter.format(new LogContext("root", level, "想定していない例外が発生しました。",
                                                         new IllegalArgumentException("userId was null.")));
        
        assertThat(message, is("<TRACE> root 想定していない例外が発生しました。" + Logger.LS));
    }
}
