package nablarch.core.log.basic;

import nablarch.core.log.Logger;
import nablarch.core.util.ObjectUtil;
import nablarch.core.util.annotation.Published;

/**
 * {@link LogWriter}の実装をサポートするクラス。<br>
 * <br>
 * このクラスでは、下記の機能を提供する。
 * <ul>
 * <li>{@link LogLevel}に応じた出力制御</li>
 * <li>{@link LogFormatter}を使用したログのフォーマット</li>
 * </ul>
 * 上記の機能は、プロパティファイルに設定を記述して使用する。<br>
 * プロパティファイルの記述ルールを下記に示す。
 * <dl>
 * <dt>writer.&lt;{@link LogWriter}の名称&gt;.level
 * <dd>{@link LogLevel}の名称。オプション。<br>
 *     {@link LogLevel}の名称を指定する。<br>
 *     ここで指定したレベル以上のログを全て出力する。
 *     指定がない場合はレベルに応じた出力制御を行わず、全てのレベルのログを出力する。
 *     
 * <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.className
 * <dd>{@link LogWriter}で使用する{@link LogFormatter}のクラス名。<br>
 *     {@link LogFormatter}を実装したクラスのFQCNを指定する。
 *     指定がない場合は{@link BasicLogFormatter}を使用する。
 *     
 * <dt>writer.&lt;{@link LogWriter}の名称&gt;.formatter.<プロパティ名>
 * <dd>{@link LogFormatter}毎のプロパティに設定する値。<br>
 *     設定内容は、使用する{@link LogFormatter}のJavadocを参照すること。
 * </dl>
 * 
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public abstract class LogWriterSupport implements LogWriter {
    
    /** 設定で指定された{@link LogWriter}の名称 */
    private String name;
    
    /** ログの出力制御の基準とする{@link LogLevel} */
    private LogLevel baseLevel;
    
    /** ログの出力制御の基準とする{@link LogLevel}の値 */
    private int baseLevelValue;
    
    /** {@link LogFormatter} */
    private LogFormatter formatter = new BasicLogFormatter();
    
    /**
     * {@inheritDoc}<br>
     * <br>
     * 設定を使用して{@link LogLevel}と{@link LogFormatter}を初期化する。
     */
    public void initialize(ObjectSettings settings) {
        
        name = settings.getName();
        
        String level = settings.getProp("level");
        if (level != null) {
            baseLevel = LogLevel.valueOf(level);
            baseLevelValue = baseLevel.getValue();
        }
        
        ObjectSettings formatterSettings = new ObjectSettings(settings.getLogSettings(), name + ".formatter");
        LogFormatter createdFormatter = createLogFormatter(formatterSettings);
        if (createdFormatter != null) {
            formatter = createdFormatter;
        }
        formatter.initialize(formatterSettings);
        
        onInitialize(settings);
    }
    
    /**
     * 設定を使用して{@link LogFormatter}を生成する。
     * @param settings {@link LogFormatter}の設定
     * @return 設定を使用して生成した{@link LogFormatter}。指定がない場合は<code>null</code>
     */
    protected LogFormatter createLogFormatter(ObjectSettings settings) {
        String className = settings.getProp("className");
        if (className == null || className.length() == 0) {
            return null;
        }
        return  (LogFormatter) ObjectUtil.createInstance(className);
    }
    
    /**
     * 初期処理を行う。<br>
     * ログの出力先に応じたリソースの確保などを実装する。<br>
     * デフォルト実装では何もしない。
     * @param settings {@link LogWriter}の設定内容
     */
    protected void onInitialize(ObjectSettings settings) {
    }
    
    /**
     * {@inheritDoc}
     */
    public void terminate() {
        onTerminate();
    }
    
    /**
     * 終了処理を行う。<br>
     * ログの出力先に応じて確保しているリソースの解放などを実装する。<br>
     * デフォルト実装では何もしない。
     */
    protected void onTerminate() {
    }
    
    /**
     * フォーマット済みのログを出力先に書き込む。<br>
     * <br>
     * 設定で{@link LogLevel}が指定されている場合は、有効なレベルの場合のみ{@link #onWrite(String)}メソッドを呼び出す。<br>
     * 有効なレベルのログでない場合は、何も処理しない。
     * 
     * @param context {@link LogContext}
     */
    public void write(LogContext context) {
        if (needsToWrite(context)) {
            onWrite(formatter.format(context));
        }
    }
    
    /**
     * 現在の設定から、指定されたログエントリを出力するか否かを返す。
     * @param context ログエントリオブジェクト
     * @return ログを出力する場合はtrue
     */
    public boolean needsToWrite(LogContext context) {
        return (baseLevel == null)
            || (baseLevelValue >= context.getLevel().getValue());
    }
    
    /**
     * フォーマット済みのログを出力先に書き込む。
     * @param formattedMessage フォーマット済みのログ
     */
    protected abstract void onWrite(String formattedMessage);
    
    /**
     * 設定情報を取得する。<br>
     * <br>
     * 設定情報のフォーマットを下記に示す。<br>
     * <br>
     * WRITER NAME        = [&lt;{@link LogWriter}の名称&gt;]<br>
     * WRITER CLASS       = [&lt;{@link LogWriter}のクラス名&gt;]<br>
     * FORMATTER CLASS    = [&lt;{@link LogFormatter}のクラス名&gt;]<br>
     * LEVEL              = [&lt;ログの出力制御の基準とする{@link LogLevel}&gt;]
     * 
     * @return 設定情報
     */
    protected String getSettings() {
        return new StringBuilder(256)
                    .append("\tWRITER NAME         = [").append(getName()).append("]").append(Logger.LS)
                    .append("\tWRITER CLASS        = [").append(getClass().getName()).append("]").append(Logger.LS)
                    .append("\tFORMATTER CLASS     = [").append(formatter.getClass().getName()).append("]").append(Logger.LS)
                    .append("\tLEVEL               = [").append(baseLevel == null ? null : baseLevel.name()).append("]").append(Logger.LS)
                    .toString();
    }

    /**
     * 設定で指定された{@link LogWriter}の名称を取得する。
     * @return 設定で指定された{@link LogWriter}の名称
     */
    protected String getName() {
        return name;
    }

    /**
     * {@link LogFormatter}を取得する。
     * @return {@link LogFormatter}
     */
    protected LogFormatter getFormatter() {
        return formatter;
    }
}
