package nablarch.core.log.app;

/**
 * コミットログ出力のJson版実装クラス。
 *
 * @author Shuji Kitamura
 */
public class JsonCommitLogger extends BasicCommitLogger {

    /** messageを構造化されていることを示す接頭辞 */
    private String structuredMessagePrefix = JsonLogFormatterSupport.DEFAULT_STRUCTURED_MESSAGE_PREFIX;

    /**
     * messageを構造化されていることを示す接頭辞を設定する。
     * @param structuredMessagePrefix messageを構造化されていることを示す接頭辞
     */
    public void setStructuredMessagePrefix(String structuredMessagePrefix) {
        this.structuredMessagePrefix = structuredMessagePrefix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String formatForIncrement(long count) {
        StringBuilder message = new StringBuilder();
        message.append(structuredMessagePrefix);
        message.append("{\"commitCount\":");
        message.append(count);
        message.append('}');
        return message.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String formatForTerminate(long count) {
        StringBuilder message = new StringBuilder();
        message.append(structuredMessagePrefix);
        message.append("{\"totalCommitCount\":");
        message.append(count);
        message.append('}');
        return message.toString();
    }
}
