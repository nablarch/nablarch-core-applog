package nablarch.core.log.basic;

/**
 * {@link LogPublisher}によって公開された{@link LogContext}を受け取るインタフェース。
 * @author Tanaka Tomoyuki
 */
public interface LogListener {

    /**
     * 公開された{@link LogContext}を受け取る。
     * @param context {@link LogContext}
     */
    void onWritten(LogContext context);
}
