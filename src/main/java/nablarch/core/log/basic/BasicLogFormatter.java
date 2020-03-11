package nablarch.core.log.basic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nablarch.core.log.DateItemSupport;
import nablarch.core.log.LogItem;
import nablarch.core.log.LogUtil;
import nablarch.core.log.Logger;
import nablarch.core.util.FileUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * {@link LogFormatter}の基本実装クラス。<br>
 * <br>
 * BasicLogFormatterクラスの特徴を下記に示す。<br>
 * <ul>
 * <li>ログに最低限必要な情報（日時、リクエストID、ユーザIDなど）を出力できる。</li>
 * <li>アプリケーションを起動しているプロセスを識別するために、システムプロパティで指定されたプロセス名をログに出力できる。</li>
 * <li>オブジェクトを指定してフィールド情報を出力できる。</li>
 * <li>例外オブジェクトを指定してスタックトレースを出力できる。</li>
 * <li>フォーマットを設定のみで変更することができる。</li>
 * </ul>
 * BasicLogFormatterは、プレースホルダを使用してフォーマットを指定する。
 * フォーマットに指定可能なプレースホルダの一覧を下記に示す。
 * <pre>
 * $date$
 *     このログ出力を要求した時点の日時。
 * $logLevel$
 *     このログ出力のログレベル。
 *     デフォルトはLogLevel列挙型の名称を文言に使用する。
 *     文言はプロパティファイルの設定で変更することができる。
 * $loggerName$
 *     このログ出力が対応するロガー設定の名称。
 *     このログ出力を呼び出した箇所に関わらず、プロパティファイル(log.properties)に記載したロガー名となる。
 * $runtimeLoggerName$
 *     実行時に、{@link nablarch.core.log.LoggerManager}からロガー取得に指定した名称。
 *     このログ出力を呼び出した際に{@link nablarch.core.log.LoggerManager#get(Class)}で指定したクラス名
 *     または{@link nablarch.core.log.LoggerManager#get(String)}で指定した名称となる。
 * $bootProcess$
 *     起動プロセスを識別する名前。
 *     起動プロセスは、システムプロパティ"nablarch.bootProcess"から取得する。
 *     指定がない場合はブランク。
 * $processingSystem$
 *     処理方式を識別する名前。
 *     処理方式は、プロパティファイル("nablarch.processingSystem")から取得する。
 *     指定がない場合はブランク。
 * $requestId$
 *     このログ出力を要求した時点のリクエストID。
 * $executionId$
 *     このログ出力を要求した時点の実行時ID
 * $userId$
 *     このログ出力を要求した時点のログインユーザのユーザID。
 * $message$
 *     このログ出力のメッセージ。
 *     指定がない場合はブランク。
 * $information$
 *     オプション情報に指定されたオブジェクトのフィールド情報。
 *     オブジェクトのフィールドに対して、Object#toString()メソッドを実行した結果を表示する。
 *     オプション情報に指定されたオブジェクトが基本データ型のラッパクラス、CharSequenceインタフェース、
 *     Dateクラスの場合は、オブジェクトに対してObject#toString()メソッドを実行した結果のみを表示する。
 *     オブジェクト情報の指定がない場合は表示しない。
 * $stackTrace$
 *     エラー情報に指定された例外オブジェクトのスタックトレース。
 *     エラー情報の指定がない場合は表示しない。
 * </pre>
 * フォーマット指定が無い場合に使用するフォーマットを下記に示す。
 * <br>
 * $date$ -$logLevel$- $loggerName$ [$executionId$]
 * boot_proc = [$bootProcess$] proc_sys = [$processingSystem$]
 * req_id = [$requestId$] usr_id = [$userId$] $message$$information$$stackTrace$
 * <br>
 * <br>
 * プロパティファイルの記述ルールを下記に示す。
 * <dl>
 *   <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.label.&lt;{@link LogLevel}の名称の小文字&gt;
 *   <dd>{@link LogLevel}に使用するラベル。オプション。<br>
 *       指定しなければ{@link LogLevel}の名称を使用する。
 *   <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.format
 *   <dd>フォーマット。オプション。
 *   <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.datePattern
 *   <dd>日時のフォーマットに使用するパターン。オプション。<br>
 *       指定しなければはyyyy-MM-dd HH:mm:ss.SSSを使用する。
 * </dl>
 * 
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class BasicLogFormatter implements LogFormatter {
    
    /** デフォルトの日時フォーマット */
    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    /** フォーマット指定が無い場合に使用するフォーマット */
    private static final String DEFAULT_FORMAT
        = "$date$ -$logLevel$- $loggerName$ [$executionId$]"
            + " boot_proc = [$bootProcess$] proc_sys = [$processingSystem$]"
            + " req_id = [$requestId$] usr_id = [$userId$]"
            + " $message$$information$$stackTrace$";
    
    /** フォーマット済みのログ出力項目 */
    private LogItem<LogContext>[] formattedLogItems;
    
    /**
     * {@inheritDoc}<br>
     * <br>
     * フォーマットとログレベルに使用するラベルを初期化する。
     */
    public void initialize(ObjectSettings settings) {
        String format = getFormat(settings);
        Map<String, LogItem<LogContext>> logItems = getLogItems(settings);
        formattedLogItems = LogUtil.createFormattedLogItems(logItems, format);
    }
    
    /**
     * フォーマット対象のログ出力項目を取得する。
     * @param settings LogFormatterの設定
     * @return フォーマット対象のログ出力項目
     */
    protected Map<String, LogItem<LogContext>> getLogItems(ObjectSettings settings) {
        Map<String, LogItem<LogContext>> logItemCandidates = new HashMap<String, LogItem<LogContext>>();
        logItemCandidates.put("$loggerName$", new LoggerNameItem());
        logItemCandidates.put("$runtimeLoggerName$", new RuntimeLoggerNameItem());
        logItemCandidates.put("$bootProcess$", new BootProcessItem());
        logItemCandidates.put("$processingSystem$", new ProcessingSystemItem(settings.getLogSettings().getProps().get("nablarch.processingSystem")));
        logItemCandidates.put("$requestId$", new RequestIdItem());
        logItemCandidates.put("$executionId$", new ExecutionIdItem());
        logItemCandidates.put("$userId$", new UserIdItem());
        logItemCandidates.put("$message$", new MessageItem());
        logItemCandidates.put("$information$", new InformationItem());
        logItemCandidates.put("$stackTrace$", new StackTraceItem());
        logItemCandidates.put("$date$", new DateItem(getDateFormat(settings)));
        logItemCandidates.put("$logLevel$", new LogLevelItem(getLogLevelLabelProvider(settings)));
        return logItemCandidates;
    }
    
    /**
     * 日時フォーマットを取得する。
     * @param settings LogFormatterの設定
     * @return 日時フォーマット
     */
    protected DateFormat getDateFormat(ObjectSettings settings) {
        String datePattern = settings.getProp("datePattern");
        return !StringUtil.isNullOrEmpty(datePattern) ?  new SimpleDateFormat(datePattern) : DEFAULT_DATE_FORMAT;
    }
    
    /**
     * LogLevelLabelProviderを取得する。
     * @param settings LogFormatterの設定
     * @return LogLevelLabelProvider
     */
    protected LogLevelLabelProvider getLogLevelLabelProvider(ObjectSettings settings) {
        return new LogLevelLabelProvider(settings);
    }
    
    /**
     * フォーマットを取得する。
     * @param settings LogFormatterの設定
     * @return フォーマット
     */
    protected String getFormat(ObjectSettings settings) {
        String format = settings.getProp("format");
        return !StringUtil.isNullOrEmpty(format) ? format : DEFAULT_FORMAT;
    }
    
    /**
     * {@inheritDoc}
     */
    public String format(LogContext context) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(LogUtil.formatMessage(formattedLogItems, context));
        String message = sb.toString();
        if (!message.endsWith(Logger.LS)) {
            message += Logger.LS;
        }
        return message;
    }

    /**
     * 出力日時を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class DateItem extends DateItemSupport<LogContext> {
        /**
         * コンストラクタ。
         * @param dateFormat 日時フォーマット
         */
        public DateItem(DateFormat dateFormat) {
            super(dateFormat);
        }
        /** {@inheritDoc} */
        protected Date getDate(LogContext context) {
            return context.getDate();
        }
    }

    /**
     * ログレベルを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class LogLevelItem implements LogItem<LogContext> {
        /** ログレベルを表す文言を提供するクラス */
        private LogLevelLabelProvider levelLabelProvider;
        /**
         * コンストラクタ。
         * @param levelLabelProvider LogLevelLabelProvider
         */
        public LogLevelItem(LogLevelLabelProvider levelLabelProvider) {
            this.levelLabelProvider = levelLabelProvider;
        }
        /**
         * LogLevelLabelProviderを使用してログレベルを取得する。<br>
         * @param context ログコンテキスト
         * @return ログレベル
         */
        public String get(LogContext context) { return levelLabelProvider.getLevelLabel(context.getLevel()); }
    }
    
    /**
     * ロガー名を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class LoggerNameItem implements LogItem<LogContext> {
        /**
         * ロガー名を取得する。
         * @param context ログコンテキスト
         * @return ロガー名
         */
        public String get(LogContext context) { return context.getLoggerName(); }
    }

    /**
     * 実行時ロガー名を取得するクラス。
     * @author Yutaka Kanayama
     */
    public static class RuntimeLoggerNameItem implements LogItem<LogContext> {
        /**
         * 実行時ロガー名を取得する。
         * @param context ログコンテキスト
         * @return 実行時ロガー名
         */
        public String get(LogContext context) { return context.getRuntimeLoggerName(); }
    }

    /**
     * 起動プロセスを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class BootProcessItem implements LogItem<LogContext> {
        /**
         * 起動プロセスを取得する。
         * @param context ログコンテキスト
         * @return 起動プロセス
         */
        public String get(LogContext context) {
            return LogUtil.getBootProcess();
        }
    }
    
    /**
     * 処理方式を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ProcessingSystemItem implements LogItem<LogContext> {
        /** 処理方式 */
        private String processingSystem;
        /**
         * コンストラクタ。
         * @param processingSystem 処理方式
         */
        public ProcessingSystemItem(String processingSystem) {
            this.processingSystem = processingSystem != null ? processingSystem : "";
        }
        /**
         * 処理方式を取得する。
         * @param context ログコンテキスト
         * @return 処理方式
         */
        public String get(LogContext context) {
            return processingSystem;
        }
    }

    /**
     * リクエストIDを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class RequestIdItem implements LogItem<LogContext> {
        /**
         * リクエストIDを取得する。
         * @param context ログコンテキスト
         * @return リクエストID
         */
        public String get(LogContext context) { return context.getRequestId(); }
    }
    
    /**
     * 実行時IDを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ExecutionIdItem implements LogItem<LogContext> {
        /**
         * 実行時IDを取得する。
         * @param context ログコンテキスト
         * @return 実行時ID
         */
        public String get(LogContext context) { return context.getExecutionId(); }
    }
    
    /**
     * ユーザIDを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class UserIdItem implements LogItem<LogContext> {
        /**
         * ユーザIDを取得する。
         * @param context ログコンテキスト
         * @return ユーザID
         */
        public String get(LogContext context) { return context.getUserId(); }
    }
    
    /**
     * メッセージを取得するクラス
     * @author Kiyohito Itoh
     */
    public static class MessageItem implements LogItem<LogContext> {
        /**
         * メッセージを取得する。
         * @param context ログコンテキスト
         * @return メッセージ
         */
        public String get(LogContext context) {
            String message = context.getMessage();
            return message != null ? message : "";
        }
    }
    
    /**
     * オプション情報に指定されたオブジェクトのフィールド情報を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class InformationItem implements LogItem<LogContext> {
        
        /**
         * オプション情報に指定されたオブジェクトのフィールド情報を取得する。
         * @param context ログコンテキスト
         * @return オプション情報に指定されたオブジェクトのフィールド情報
         */
        public String get(LogContext context) {
            Object[] options = context.getOptions();
            if (options == null || options.length == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder(512 * options.length);
            for (int i = 0; i < options.length; i++) {
                sb.append(Logger.LS);
                appendObjectInfo(sb, i, options[i]);
            }
            return sb.toString();
        }
        
        /**
         * フォーマット済みのオブジェクト情報を追加する。
         * @param sb フォーマット済みのオブジェクト情報を格納するバッファ
         * @param index インデックス
         * @param object オブジェクト
         */
        protected void appendObjectInfo(StringBuilder sb, int index, Object object) {
            sb.append("Object Information[").append(index).append("]: ");
            if (object == null) {
                sb.append("null");
                return;
            }
            Class<?> clazz = object.getClass();
            sb.append("Class Name = [").append(clazz.getName()).append("]").append(Logger.LS);
            if (!isValueObject(object)) {
                Field[] fields = clazz.getDeclaredFields();
                AccessibleObject.setAccessible(fields, true);
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    sb.append("\t").append(field.getName()).append(" = [");
                    try {
                        sb.append(field.get(object));
                    } catch (Exception e) {
                        sb.append("(unknown)");
                    }
                    sb.append("]").append(Logger.LS);
                }
            }
            sb.append("\ttoString() = [").append(object.toString()).append("]");
        }
        
        /**
         * 指定されたオブジェクトが基本データ型のラッパー、CharSequence型、Date型であるか判定する。
         * @param object オブジェクト
         * @return 基本データ型のラッパー、CharSequence型、Date型の場合は<code>true</code>
         */
        protected boolean isValueObject(Object object) {
            return object instanceof CharSequence
                || object instanceof Number
                || object instanceof Character
                || object instanceof Date;
        }
    }
    
    /**
     * エラー情報に指定された例外オブジェクトのスタックトレースを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class StackTraceItem implements LogItem<LogContext> {
        
        /**
         * エラー情報に指定された例外オブジェクトのスタックトレースを取得する。
         * @param context ログコンテキスト
         * @return エラー情報に指定された例外オブジェクトのスタックトレース
         */
        public String get(LogContext context) {
            Throwable error = context.getError();
            if (error == null) {
                return "";
            }
            return getStackTrace(error);
        }
        
        /**
         * フォーマット済みのスタックトレースを取得する。
         * @param error エラー情報
         * @return フォーマット済みのスタックトレース
         */
        protected String getStackTrace(Throwable error) {
            StringBuilder sb = new StringBuilder(512);
            sb.append(Logger.LS).append("Stack Trace Information : ").append(Logger.LS);
            StringWriter sw = null;
            PrintWriter pw = null;
            try {
                sw = new StringWriter();
                pw = new PrintWriter(sw);
                error.printStackTrace(pw);
                sb.append(sw.toString());
            } finally {
                FileUtil.closeQuietly(pw);
            }
            return sb.toString();
        }
    }
}
