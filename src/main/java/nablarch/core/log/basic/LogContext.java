package nablarch.core.log.basic;

import java.util.Date;

import nablarch.core.ThreadContext;
import nablarch.core.util.annotation.Published;

/**
 * ログ出力に必要な情報を保持するクラス。
 * <br>
 * スレッド名、ユーザID、リクエストIDは、スレッドに紐付く値をクラスの内部で設定する。
 * 
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class LogContext {
    
    /** ロガー名 */
    private String loggerName;

    /**実行時ロガー名 */
    private String runtimeLoggerName;

    /** {@link LogLevel} */
    private LogLevel level;
    
    /** メッセージ */
    private String message;
    
    /** エラー情報 */
    private Throwable error;
    
    /** オプション情報 */
    private Object[] options;
    
    /** LogContext作成時点の日時 */
    private Date date;
    
    /** LogContext作成時点のユーザID */
    private String userId;
    
    /** LogContext作成時点のリクエストID */
    private String requestId;
    
    /** LogContext作成時点の実行時ID */
    private String executionId;
    
    /**
     * コンストラクタ。
     * @param loggerName ロガー名
     * @param level {@link LogLevel}
     * @param message メッセージ
     * @param error エラー情報(nullでも可)
     * @param options オプション情報(nullでも可)
     */
    public LogContext(String loggerName, LogLevel level, String message, Throwable error, Object... options) {
        this.loggerName = loggerName;
        this.level = level;
        this.message = message;
        this.error = error;
        this.options = options;
        this.date = new Date();
        this.userId = ThreadContext.getUserId();
        this.requestId = ThreadContext.getRequestId();
        this.executionId = ThreadContext.getExecutionId();
    }

    /**
     * 実行時ロガー名をコンストラクタ。
     * @param loggerName ロガー名
     * @param runtimeLoggerName 実行時ロガー名
     * @param level {@link LogLevel}
     * @param message メッセージ
     * @param error エラー情報(nullでも可)
     * @param options オプション情報(nullでも可)
     */
    public LogContext(String loggerName, String runtimeLoggerName, LogLevel level, String message, Throwable error, Object... options) {
        this.loggerName = loggerName;
        this.runtimeLoggerName = runtimeLoggerName;
        this.level = level;
        this.message = message;
        this.error = error;
        this.options = options;
        this.date = new Date();
        this.userId = ThreadContext.getUserId();
        this.requestId = ThreadContext.getRequestId();
        this.executionId = ThreadContext.getExecutionId();
    }

    /**
     * ロガー設定の名称を取得する。
     * @return ロガー設定の名称
     */
    public String getLoggerName() {
        return loggerName;
    }

    /**
     * 実行時にロガーを取得したときの名称を取得する。
     * @return 実行時にロガーを取得したときの名称
     */
    public String getRuntimeLoggerName() {
        return runtimeLoggerName;
    }

    /**
     * {@link LogLevel}を取得する。
     * @return {@link LogLevel}
     */
    public LogLevel getLevel() {
        return level;
    }

    /**
     * メッセージを取得する。
     * @return メッセージ
     */
    public String getMessage() {
        return message;
    }

    /**
     * エラー情報を取得する。
     * @return エラー情報
     */
    public Throwable getError() {
        return error;
    }

    /**
     * オプション情報を取得する。
     * @return オプション情報
     */
    public Object[] getOptions() {
        return options;
    }

    /**
     * LogContext作成時点の日時を取得する。
     * @return LogContext作成時点の日時
     */
    public Date getDate() {
        return date;
    }

    /**
     * LogContext作成時点のユーザIDを取得する。
     * @return LogContext作成時点のユーザID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * LogContext作成時点のリクエストIDを取得する。
     * @return LogContext作成時点のリクエストID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * LogContext作成時点の実行時IDを取得する。
     * @return LogContext作成時点の実行時ID
     */
    public String getExecutionId() {
        return executionId;
    }
}
