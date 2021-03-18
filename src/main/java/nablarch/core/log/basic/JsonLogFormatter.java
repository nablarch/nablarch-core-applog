package nablarch.core.log.basic;

import nablarch.core.log.LogUtil;
import nablarch.core.log.Logger;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.text.json.JsonSerializer;
import nablarch.core.text.json.RawJsonObjectMembers;
import nablarch.core.util.FileUtil;
import nablarch.core.util.ObjectUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * {@link LogFormatter}のJSON形式フォーマット実装クラス。<br>
 * <br>
 * BasicLogFormatterは、出力項目を指定してフォーマットを指定する。
 * 出力項目の一覧を下記に示す。
 * <pre>
 * date
 *     このログ出力を要求した時点の日時。
 * logLevel
 *     このログ出力のログレベル。
 *     デフォルトはLogLevel列挙型の名称を文言に使用する。
 *     文言はプロパティファイルの設定で変更することができる。
 * loggerName
 *     このログ出力が対応するロガー設定の名称。
 *     このログ出力を呼び出した箇所に関わらず、プロパティファイル(log.properties)に記載したロガー名となる。
 * runtimeLoggerName
 *     実行時に、{@link nablarch.core.log.LoggerManager}からロガー取得に指定した名称。
 *     このログ出力を呼び出した際に{@link nablarch.core.log.LoggerManager#get(Class)}で指定したクラス名
 *     または{@link nablarch.core.log.LoggerManager#get(String)}で指定した名称となる。
 * bootProcess
 *     起動プロセスを識別する名前。
 *     起動プロセスは、システムプロパティ"nablarch.bootProcess"から取得する。
 *     指定がない場合はブランク。
 * processingSystem
 *     処理方式を識別する名前。
 *     処理方式は、プロパティファイル("nablarch.processingSystem")から取得する。
 *     指定がない場合はブランク。
 * requestId
 *     このログ出力を要求した時点のリクエストID。
 * executionId
 *     このログ出力を要求した時点の実行時ID
 * userId
 *     このログ出力を要求した時点のログインユーザのユーザID。
 * message
 *     このログ出力のメッセージ。
 * payload
 *     オプション情報に指定されたオブジェクトのフィールド情報。<br>
 *     オブジェクトの型はMap&lt;String, Object&gt;でなければならない。<br>
 *     Mapのアイテムがルート階層に追加される。
 *     キーが重複した場合は、いずれか一つのみが出力される。
 * stackTrace
 *     エラー情報に指定された例外オブジェクトのスタックトレース。
 *     エラー情報の指定がない場合は表示しない。
 * </pre>
 * フォーマット指定が無い場合に使用する出力項目を下記に示す。
 * <br>
 * date,logLevel,loggerName,executionId,bootProcess,processingSystem,
 * requestId,userId,message,payload,stackTrace
 * <br>
 * <br>
 * プロパティファイルの記述ルールを下記に示す。<br>
 * <br>
 * <dl>
 *   <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.label.&lt;{@link LogLevel}の名称の小文字&gt;<dt/>
 *   <dd>{@link LogLevel}に使用するラベル。オプション。<br>
 *       指定しなければ{@link LogLevel}の名称を使用する。<dd/>
 *   <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.targets<dt/>
 *   <dd>出力項目。オプション。<dd/>
 *   <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.datePattern<dt/>
 *   <dd>日時のフォーマットに使用するパターン。オプション。<br>
 *       指定しなければyyyy-MM-dd HH:mm:ss.SSSを使用する。<dd/>
 *   <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.jsonSerializationManagerClassName<dt/>
 *   <dd>JSONへのシリアライズを管理するクラス。オプション。<br>
 *       指定しなければ{@link nablarch.core.text.json.BasicJsonSerializationManager}を使用する。<dd/>
 *   <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.structuredMessagePrefix<dt/>
 *   <dd>各種ログで使用される組み込み処理用の接頭区。オプション。<br>
 *       指定しなければ$JSON$を使用する。<dd/>
 * </dl>
 *
 * @author Shuji Kitamura
 */
@Published(tag = "architect")
public class JsonLogFormatter implements LogFormatter, FormatErrorSupport {

    /** 出力日時の項目名 */
    private static final String TARGET_NAME_DATE = "date";
    /** ログレベルの項目名 */
    private static final String TARGET_NAME_LOG_LEVEL = "logLevel";
    /** ロガー名の項目名 */
    private static final String TARGET_NAME_LOGGER_NAME = "loggerName";
    /** 実行時ロガー名の項目名 */
    private static final String TARGET_NAME_RUNTIME_LOGGER_NAME = "runtimeLoggerName";
    /** 起動プロセスの項目名 */
    private static final String TARGET_NAME_BOOT_PROCESS = "bootProcess";
    /** 処理方式の項目名 */
    private static final String TARGET_NAME_PROCESSING_SYSTEM = "processingSystem";
    /** リクエストIDの項目名 */
    private static final String TARGET_NAME_REQUEST_ID = "requestId";
    /** 実行時IDの項目名 */
    private static final String TARGET_NAME_EXECUTION_ID = "executionId";
    /** ユーザIDの項目名 */
    private static final String TARGET_NAME_USER_ID = "userId";
    /** メッセージの項目名 */
    private static final String TARGET_NAME_MESSAGE = "message";
    /** エラー情報に指定された例外オブジェクトのスタックトレーの項目名 */
    private static final String TARGET_NAME_STACK_TRACE = "stackTrace";
    /** オプション情報に指定されたオブジェクトの項目名 */
    private static final String TARGET_NAME_PAYLOAD = "payload";

    /** システムプロパティから処理方式を識別する文字列を取得する際に使用するキー */
    private static final String SYSTEM_PROP_PROCESSING_SYSTEM = "nablarch.processingSystem";

    /** 出力項目のプロパティ名 */
    private static final String PROPS_TARGETS = "targets";
    /** 出力項目のデフォルト値 */
    private static final String DEFAULT_TARGETS = "date,logLevel,loggerName,executionId"
            + ",bootProcess,processingSystem,requestId,userId,message,payload,stackTrace";

    /** messageを構造化されていることを示す接頭区のプロパティ名 */
    private static final String PROPS_STRUCTURED_MESSAGE_PREFIX = "structuredMessagePrefix";
    /** messageを構造化されていることを示す接頭区のデフォルト値 */
    private static final String DEFAULT_STRUCTURED_MESSAGE_PREFIX = "$JSON$";

    /** Jsonのシリアライズに使用する管理クラス名のプロパティ名 */
    private static final String PROPS_SERIALIZATION_MANAGER_CLASS_NAME = "jsonSerializationManagerClassName";
    /** Jsonのシリアライズに使用する管理クラス名のデフォルト値 */
    private static final String DEFAULT_SERIALIZATION_MANAGER_CLASS_NAME = "nablarch.core.text.json.BasicJsonSerializationManager";

    /** Jsonのシリアライズに使用する管理クラス */
    private JsonSerializationManager serializationManager;

    /** ログ出力項目 */
    private List<JsonLogObjectBuilder<LogContext>> structuredTargets;

    /**
     * {@inheritDoc}<br>
     * <br>
     * 出力項目を初期化する。
     */
    @Override
    public void initialize(ObjectSettings settings) {
        serializationManager = createSerializationManager(settings);
        structuredTargets = createStructuredTargets(settings);
    }

    /**
     * Jsonのシリアライズに使用する管理クラスを取得する。
     * @param settings LogFormatterの設定
     * @return Jsonのシリアライズに使用する管理クラス
     */
    protected JsonSerializationManager createSerializationManager(ObjectSettings settings) {
        String prefix = settings.getProp(PROPS_SERIALIZATION_MANAGER_CLASS_NAME);
        String className = !StringUtil.isNullOrEmpty(prefix) ? prefix : DEFAULT_SERIALIZATION_MANAGER_CLASS_NAME;
        JsonSerializationManager serializationManager = ObjectUtil.createInstance(className);
        JsonSerializationSettings jsonSettings = new JsonSerializationSettings(settings.getProps());
        serializationManager.initialize(jsonSettings);
        return serializationManager;
    }

    /**
     * ログ出力項目を取得する。
     * @param settings LogFormatterの設定
     * @return ログ出力項目
     */
    protected List<JsonLogObjectBuilder<LogContext>> createStructuredTargets(ObjectSettings settings) {

        List<JsonLogObjectBuilder<LogContext>> list = new ArrayList<JsonLogObjectBuilder<LogContext>>();

        String targetsStr = settings.getProp(PROPS_TARGETS);
        if (StringUtil.isNullOrEmpty(targetsStr)) targetsStr = DEFAULT_TARGETS;
        String[] targets = targetsStr.split(",");

        Set<String> keys = new HashSet<String>(targets.length);
        for (String target: targets) {
            String key = target.trim();
            if (!StringUtil.isNullOrEmpty(key) && !keys.contains(key)) {
                keys.add(key);
                if (TARGET_NAME_DATE.equals(key)) { list.add(new DateBuilder()); }
                else if (TARGET_NAME_LOG_LEVEL.equals(key)) { list.add(new LogLevelBuilder(getLogLevelLabelProvider(settings))); }
                else if (TARGET_NAME_LOGGER_NAME.equals(key)) { list.add(new LoggerNameBuilder()); }
                else if (TARGET_NAME_RUNTIME_LOGGER_NAME.equals(key)) { list.add(new RuntimeLoggerNameBuilder()); }
                else if (TARGET_NAME_BOOT_PROCESS.equals(key)) { list.add(new BootProcessBuilder()); }
                else if (TARGET_NAME_PROCESSING_SYSTEM.equals(key)) {
                    list.add(new ProcessingSystemBuilder(
                            settings.getLogSettings().getProps().get(SYSTEM_PROP_PROCESSING_SYSTEM))); }
                else if (TARGET_NAME_REQUEST_ID.equals(key)) { list.add(new RequestIdBuilder()); }
                else if (TARGET_NAME_EXECUTION_ID.equals(key)) { list.add(new ExecutionIdBuilder()); }
                else if (TARGET_NAME_USER_ID.equals(key)) { list.add(new UserIdBuilder()); }
                else if (TARGET_NAME_MESSAGE.equals(key)) { list.add(new MessageBuilder(getStructuredMessagePrefix(settings))); }
                else if (TARGET_NAME_STACK_TRACE.equals(key)) { list.add(new StackTraceBuilder()); }
                else if (TARGET_NAME_PAYLOAD.equals(key)) { list.add(new PayloadBuilder(this)); }
            }
        }
        return list;
    }

    /**
     * 構造化済みメッセージを示す接頭区を取得する。
     * @param settings LogFormatterの設定
     * @return 構造化済みメッセージを示す接頭区
     */
    public String getStructuredMessagePrefix(ObjectSettings settings) {
        String prefix = settings.getProp(PROPS_STRUCTURED_MESSAGE_PREFIX);
        return !StringUtil.isNullOrEmpty(prefix) ? prefix : DEFAULT_STRUCTURED_MESSAGE_PREFIX;
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
     * {@inheritDoc}
     */
    @Override
    public String format(LogContext context) {
        Map<String, Object> structuredObject = createStructuredObject(context);
        JsonSerializer serializer = serializationManager.getSerializer(structuredObject);
        StringWriter writer = new StringWriter();
        String message;
        try {
            serializer.serialize(writer, structuredObject);
            message = writer.toString();
        } catch (Exception e) {
            StringWriter sw;
            PrintWriter pw = null;
            try {
                sw = new StringWriter();
                pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                outputFormatError(sw.toString());
            } finally {
                FileUtil.closeQuietly(pw);
            }
            message = "\"log format error\"";
        }
        if (!message.endsWith(Logger.LS)) {
            message += Logger.LS;
        }
        return message;
    }

    /**
     * ログコンテキストからシリアライズ用のオブジェクトを作成する。
     * @param context ログコンテキスト
     * @return シリアライズ用のオブジェクト
     */
    protected Map<String, Object> createStructuredObject(LogContext context) {
        Map<String, Object> structuredObject = new HashMap<String, Object>();
        for (JsonLogObjectBuilder<LogContext> builder: structuredTargets) {
            builder.build(structuredObject, context);
        }
        return structuredObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void outputFormatError(String message) {
        System.err.println(message);
    }

    /**
     * 出力日時を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class DateBuilder implements JsonLogObjectBuilder<LogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            structuredObject.put(TARGET_NAME_DATE, context.getDate());
        }
    }

    /**
     * ログレベルを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class LogLevelBuilder implements JsonLogObjectBuilder<LogContext> {

        /** ログレベルを表す文言を提供するクラス */
        private final LogLevelLabelProvider levelLabelProvider;

        /**
         * コンストラクタ。
         * @param levelLabelProvider LogLevelLabelProvider
         */
        public LogLevelBuilder(LogLevelLabelProvider levelLabelProvider) {
            this.levelLabelProvider = levelLabelProvider;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            structuredObject.put(TARGET_NAME_LOG_LEVEL,
                levelLabelProvider.getLevelLabel(context.getLevel()));
        }
    }

    /**
     * ロガー名を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class LoggerNameBuilder implements JsonLogObjectBuilder<LogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            structuredObject.put(TARGET_NAME_LOGGER_NAME, context.getLoggerName());
        }
    }

    /**
     * 実行時ロガー名を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class RuntimeLoggerNameBuilder implements JsonLogObjectBuilder<LogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            structuredObject.put(TARGET_NAME_RUNTIME_LOGGER_NAME, context.getRuntimeLoggerName());
        }
    }

    /**
     * 起動プロセスを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class BootProcessBuilder implements JsonLogObjectBuilder<LogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            structuredObject.put(TARGET_NAME_BOOT_PROCESS, LogUtil.getBootProcess());
        }
    }

    /**
     * 処理方式を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ProcessingSystemBuilder implements JsonLogObjectBuilder<LogContext> {

        /** 処理方式 */
        private final String processingSystem;

        /**
         * コンストラクタ。
         * @param processingSystem 処理方式
         */
        public ProcessingSystemBuilder(String processingSystem) {
            this.processingSystem = processingSystem != null ? processingSystem : "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            structuredObject.put(TARGET_NAME_PROCESSING_SYSTEM, processingSystem);
        }
    }

    /**
     * リクエストIDを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class RequestIdBuilder implements JsonLogObjectBuilder<LogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            structuredObject.put(TARGET_NAME_REQUEST_ID, context.getRequestId());
        }
    }

    /**
     * 実行時IDを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ExecutionIdBuilder implements JsonLogObjectBuilder<LogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            structuredObject.put(TARGET_NAME_EXECUTION_ID, context.getExecutionId());
        }
    }

    /**
     * ユーザIDを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class UserIdBuilder implements JsonLogObjectBuilder<LogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            structuredObject.put(TARGET_NAME_USER_ID, context.getUserId());
        }
    }

    /**
     * メッセージを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MessageBuilder implements JsonLogObjectBuilder<LogContext> {

        /** 構造化済みメッセージであることを示す接頭区 */
        private final String structuredMessagePrefix;

        /**
         * コンストラクタ。
         * @param structuredMessagePrefix 構造化済みメッセージであることを示す接頭区
         */
        public MessageBuilder(String structuredMessagePrefix) {
            this.structuredMessagePrefix = structuredMessagePrefix;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            if (context.getMessage() == null) {
                structuredObject.put(TARGET_NAME_MESSAGE, null);
            } else if (context.getMessage().startsWith(structuredMessagePrefix)) {
                String message = context.getMessage();
                structuredObject.put(TARGET_NAME_MESSAGE,
                        new RawJsonObjectMembers(message.substring(structuredMessagePrefix.length() + 1, message.length() - 1)));
            } else {
                structuredObject.put(TARGET_NAME_MESSAGE, context.getMessage());
            }
        }
    }

    /**
     * エラー情報に指定された例外オブジェクトのスタックトレースを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class StackTraceBuilder implements JsonLogObjectBuilder<LogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            Throwable error = context.getError();
            if (error == null) {
                structuredObject.put(TARGET_NAME_STACK_TRACE, null);
            } else {
                StringWriter sw;
                PrintWriter pw = null;
                try {
                    sw = new StringWriter();
                    pw = new PrintWriter(sw);
                    error.printStackTrace(pw);
                    structuredObject.put(TARGET_NAME_STACK_TRACE, sw.toString());
                } finally {
                    FileUtil.closeQuietly(pw);
                }
            }
        }
    }

    /**
     * オプション情報に指定されたオブジェクトを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class PayloadBuilder implements JsonLogObjectBuilder<LogContext> {

        private final FormatErrorSupport errorSupport;

        /**
         * コンストラクタ
         */
        public PayloadBuilder(FormatErrorSupport errorSupport) {
            this.errorSupport = errorSupport;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, LogContext context) {
            if (context.getOptions() != null) {
                boolean isIllegalObject;
                for (Object option : context.getOptions()) {
                    isIllegalObject = false;
                    if (option instanceof Map) {
                        for(Map.Entry<?, ?> entry : ((Map<?, ?>)option).entrySet()) {
                            if (entry.getKey() instanceof String) {
                                structuredObject.put((String)entry.getKey(), entry.getValue());
                            } else {
                                isIllegalObject = true;
                            }
                        }
                    } else {
                        isIllegalObject = true;
                    }
                    if (isIllegalObject) {
                        errorSupport.outputFormatError("objects in options must be Map<String, Object>."
                                + "[" + (option == null ? "null" : option.toString()) + "]");
                    }
                }
            }
        }
    }

}
