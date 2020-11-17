package nablarch.core.log.basic;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 書き出されたログを、登録された{@link LogListener}に公開する{@link LogWriter}の実装クラス。
 * <p>
 * {@link LogWriter}のインスタンスは外部から取得できないため、
 * 公開対象の{@link LogListener}は{@code static}変数で保持している。
 * </p>
 * @author Tanaka Tomoyuki
 */
public class LogPublisher implements LogWriter {
    private static final CopyOnWriteArrayList<LogListener> LISTENERS = new CopyOnWriteArrayList<LogListener>();

    /**
     * 公開対象の{@link LogListener}を追加する。
     * @param listener {@link LogListener}
     */
    public static void addListener(LogListener listener) {
        LISTENERS.add(listener);
    }

    /**
     * 公開対象から指定した{@link LogListener}を削除する。
     * @param listener {@link LogListener}
     */
    public static void removeListener(LogListener listener) {
        LISTENERS.remove(listener);
    }

    /**
     * 登録されているすべての{@link LogListener}を削除する。
     */
    public static void removeAllListeners() {
        LISTENERS.clear();
    }

    @Override
    public void write(LogContext context) {
        for (LogListener listener : LISTENERS) {
            listener.onWritten(context);
        }
    }

    @Override
    public void initialize(ObjectSettings settings) {
        // noop
    }

    @Override
    public void terminate() {
        // noop
    }
}
