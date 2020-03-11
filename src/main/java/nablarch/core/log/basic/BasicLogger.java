package nablarch.core.log.basic;

import nablarch.core.log.Logger;


/**
 * {@link Logger}の基本実装クラス。
 * 
 * @author Kiyohito Itoh
 */
public class BasicLogger implements Logger {

    /** ロガー名 */
    private String name;

    /** 実行時のロガー名 */
    private String runtimeName;
    
    /** ログの出力制御の基準とする{@link LogLevel} */
    private LogLevel baseLevel;
    
    /** {@link LogWriter} */
    private LogWriter[] writers;
    
    /** FATALレベルのログ出力が有効か否か。 */
    private boolean fatalEnabled;
    
    /** ERRORレベルのログ出力が有効か否か。 */
    private boolean errorEnabled;
    
    /** WARNレベルのログ出力が有効か否か。 */
    private boolean warnEnabled;
    
    /** INFOレベルのログ出力が有効か否か。 */
    private boolean infoEnabled;
    
    /** DEBUGレベルのログ出力が有効か否か。 */
    private boolean debugEnabled;
    
    /** TRACEレベルのログ出力が有効か否か。 */
    private boolean traceEnabled;
    
    /**
     * コンストラクタ。
     * @param name ロガー名
     * @param baseLevel ログの出力制御の基準とする{@link LogLevel}
     * @param writers {@link LogWriter}
     */
    BasicLogger(String name, LogLevel baseLevel, LogWriter[] writers) {
        this.name = name;
        this.baseLevel = baseLevel;
        this.writers = writers;
        initializeLogLevelEnabled();
    }
    /**
     * {@link BasicLogger}をコピーし実行時ロガー名を付与するコンストラクタ。
     * @param src コピー元{@link BasicLogger}
     * @param runtimeName 実行時ロガー名
     */
    BasicLogger(BasicLogger src, String runtimeName) {
        this(src.name, src.baseLevel, src.writers);
        this.runtimeName = runtimeName;
    }

    /**
     * ロガー定義が存在しないロガー名が指定された場合に、
     * 何もしない{@link Logger}を生成するためのコンストラクタ。
     * @param name ロガー名
     */
    BasicLogger(String name) {
        this.name = name;
        this.baseLevel = null;
        this.writers = new LogWriter[0];
    }
    
    /**
     * 全ての{@link LogLevel}に対するログ出力の有効／無効を初期化する。
     */
    private void initializeLogLevelEnabled() {
        int baseLevelValue = baseLevel.getValue();
        fatalEnabled = true;
        errorEnabled = LogLevel.ERROR.getValue() <= baseLevelValue;
        warnEnabled = LogLevel.WARN.getValue() <= baseLevelValue;
        infoEnabled = LogLevel.INFO.getValue() <= baseLevelValue;
        debugEnabled = LogLevel.DEBUG.getValue() <= baseLevelValue;
        traceEnabled = LogLevel.TRACE.getValue() <= baseLevelValue;
    }
    
    /** {@inheritDoc} */
    public boolean isFatalEnabled() {
        return fatalEnabled;
    }
    
    /** {@inheritDoc} */
    public void logFatal(String message, Object... options) {
        if (fatalEnabled) {
            log(LogLevel.FATAL, message, null, options);
        }
    }
    
    /** {@inheritDoc} */
    public void logFatal(String message, Throwable error, Object... options) {
        if (fatalEnabled) {
            log(LogLevel.FATAL, message, error, options);
        }
    }
    
    /** {@inheritDoc} */
    public boolean isErrorEnabled() {
        return errorEnabled;
    }
    
    /** {@inheritDoc} */
    public void logError(String message, Object... options) {
        if (errorEnabled) {
            log(LogLevel.ERROR, message, null, options);
        }
    }
    
    /** {@inheritDoc} */
    public void logError(String message, Throwable error, Object... options) {
        if (errorEnabled) {
            log(LogLevel.ERROR, message, error, options);
        }
    }
    
    /** {@inheritDoc} */
    public boolean isWarnEnabled() {
        return warnEnabled;
    }
    
    /** {@inheritDoc} */
    public void logWarn(String message, Object... options) {
        if (warnEnabled) {
            log(LogLevel.WARN, message, null, options);
        }
    }
    
    /** {@inheritDoc} */
    public void logWarn(String message, Throwable error, Object... options) {
        if (warnEnabled) {
            log(LogLevel.WARN, message, error, options);
        }
    }
    
    /** {@inheritDoc} */
    public boolean isInfoEnabled() {
        return infoEnabled;
    }
    
    /** {@inheritDoc} */
    public void logInfo(String message, Object... options) {
        if (infoEnabled) {
            log(LogLevel.INFO, message, null, options);
        }
    }
    
    /** {@inheritDoc} */
    public void logInfo(String message, Throwable error, Object... options) {
        if (infoEnabled) {
            log(LogLevel.INFO, message, error, options);
        }
    }
    
    /** {@inheritDoc} */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /** {@inheritDoc} */
    public void logDebug(String message, Object... options) {
        if (debugEnabled) {
            log(LogLevel.DEBUG, message, null, options);
        }
    }
    
    /** {@inheritDoc} */
    public void logDebug(String message, Throwable error, Object... options) {
        if (debugEnabled) {
            log(LogLevel.DEBUG, message, error, options);
        }
    }
    
    /** {@inheritDoc} */
    public boolean isTraceEnabled() {
        return traceEnabled;
    }
    
    /** {@inheritDoc} */
    public void logTrace(String message, Object... options) {
        if (traceEnabled) {
            log(LogLevel.TRACE, message, null, options);
        }
    }
    
    /** {@inheritDoc} */
    public void logTrace(String message, Throwable error, Object... options) {
        if (traceEnabled) {
            log(LogLevel.TRACE, message, error, options);
        }
    }

    /**
     * 指定された{@link LogLevel}でログを出力する。<br>
     * <br>
     * {@link LogWriter}の書き込み処理で例外が発生した場合は、発生した例外をキャッチし、標準エラーにスタックトレースを出力する。<br>
     * 発生した例外の再スローは行わない。
     * 
     * @param level {@link LogLevel}
     * @param message メッセージ
     * @param error エラー情報(nullでも可)
     * @param options オプション情報(nullでも可)
     */
    private void log(LogLevel level, String message, Throwable error, Object... options) {
        LogContext context = new LogContext(name, runtimeName,level, message, error, options);
        for (LogWriter writer : writers) {
            try {
                writer.write(context);
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }
}
