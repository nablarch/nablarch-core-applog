package nablarch.core.log.app;

import java.util.Collections;
import java.util.List;

/**
 * 複数の{@link CommitLogger}を組み合わせたロガークラス。
 * @author Tanaka Tomoyuki
 */
public class CompositeCommitLogger implements CommitLogger {
    private List<? extends CommitLogger> commitLoggerList = Collections.emptyList();

    @Override
    public void initialize() {
        for (CommitLogger commitLogger : commitLoggerList) {
            commitLogger.initialize();
        }
    }

    @Override
    public void increment(long count) {
        for (CommitLogger commitLogger : commitLoggerList) {
            commitLogger.increment(count);
        }
    }

    @Override
    public void terminate() {
        for (CommitLogger commitLogger : commitLoggerList) {
            commitLogger.terminate();
        }
    }

    /**
     * {@link CommitLogger}のリストを設定する。
     * @param commitLoggerList {@link CommitLogger}のリスト
     */
    public void setCommitLoggerList(List<? extends CommitLogger> commitLoggerList) {
        this.commitLoggerList = commitLoggerList;
    }
}
