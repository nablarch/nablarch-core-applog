package nablarch.core.log.app;

/**
 * コミットログ出力のJson版実装クラス。
 *
 * @author Shuji Kitamura
 */
public class JsonCommitLogger extends BasicCommitLogger {

    /**
     * {@inheritDoc}
     */
    protected String formatForIncrement(long count) {
        return "$JSON${\"commitCount\":" + count + '}';
    }

    /**
     * {@inheritDoc}
     */
    protected String formatForTerminate(long count) {
        return "$JSON${\"totalCommitCount\":" + count + '}';
    }
}
