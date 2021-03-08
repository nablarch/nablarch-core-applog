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
    @Override
    protected String formatForIncrement(long count) {
        return "$JSON${\"commitCount\":" + count + '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String formatForTerminate(long count) {
        return "$JSON${\"totalCommitCount\":" + count + '}';
    }
}
