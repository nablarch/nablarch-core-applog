package nablarch.core.log.app;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;

/**
 * コミットログ出力の基本実装クラス。
 * <p/>
 * {@link #setInterval(int)}で指定された間隔でコミットログを出力する。
 * ログ出力間隔の設定を省略した場合のデフォルト値は、500である。
 * コミットログは、以下のフォーマットで出力される。
 * <pre>
 * {@code
 *
 * COMMIT COUNT = [コミット件数]
 * TOTAL COMMIT COUNT = [総コミット件数]    # 本ログは、最後に１度のみ出力される。
 * }
 * </pre>
 *
 * @author hisaaki sioiri
 */
public class BasicCommitLogger implements CommitLogger {

    /** コミット件数 */
    private long commitCount;

    /** 総コミット件数 */
    private long totalCommitCount;

    /** コミットログ出力間隔。 */
    private int interval = 500;

    /** 初期化フラグ */
    private boolean initialized = false;

    /** コミットログを出力する際に使用するロガー */
    private static final Logger LOGGER = LoggerManager.get(
            BasicCommitLogger.class);

    /**
     * {@inheritDoc}
     * コミット件数及び、総コミット件数を初期化(0クリア)する。
     */
    public synchronized void initialize() {
        commitCount = 0;
        totalCommitCount = 0;
        initialized = true;
    }

    /**
     * コミット件数を加算する。
     *
     * コミット件数を加算した結果、ログ出力間隔を超えた場合にはコミットログの出力を行う。
     * 初期化が行われていない場合は、{@link IllegalStateException}を送出する。
     *
     * {@inheritDoc}
     * @throws IllegalStateException 本オブジェクトが初期化されていない場合
     */
    public synchronized void increment(long count) throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized object.");
        }
        commitCount += count;
        if (commitCount >= interval) {
            totalCommitCount += commitCount;
            commitCount = 0;
            LOGGER.logInfo(formatForIncrement(totalCommitCount));
        }
    }

    /**
     * {@link #increment}メソッドでログに出力する総コミット件数のメッセージをフォーマットする。
     * @param count 総コミット件数
     * @return フォーマットされたメッセージ
     */
    protected String formatForIncrement(long count) {
        return "COMMIT COUNT = [" + count + ']';
    }

    /**
     * {@inheritDoc}
     * 総コミット件数をログ出力する。
     * 初期化が行われていない場合は、何も行わない。
     */
    public synchronized void terminate() {
        if (!initialized) {
            return;
        }
        initialized = false;
        totalCommitCount += commitCount;
        LOGGER.logInfo(formatForTerminate(totalCommitCount));
    }

    /**
     * {@link #terminate}メソッドでログに出力する総コミット件数のメッセージをフォーマットする。
     * @param count 総コミット件数
     * @return フォーマットされたメッセージ
     */
    protected String formatForTerminate(long count) {
        return "TOTAL COMMIT COUNT = [" + count + ']';
    }

    /**
     * 間隔を設定する。
     *
     * @param interval 間隔
     */
    public void setInterval(int interval) {
        // この処理は、FindBugsでマルチスレッド環境での正確性の例外を抑制している。
        // これは、本メソッドはリポジトリからのインジェクションのみを想定しているためであり、
        // マルチスレッド時に使用されることは想定していないためである。
        this.interval = interval;
    }
}

