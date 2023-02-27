package nablarch.core.log.basic;

/**
 * ログのローテーションを行うインタフェース。<br>
 * ログのローテーションの種類毎に本インタフェースの実装クラスを作成する。
 *
 * @author Kotaro Taki
 */
public interface RotatePolicy {

    /**
     * 初期処理を行う。
     * @param settings LogWriterの設定
     */
    void initialize(ObjectSettings settings);

    /**
     * ローテーションが必要かの判定を行う。
     * @param msgLength ログファイルに書き込まれるメッセージ長
     * @return ローテーションが必要な場合はtrue
     */
    boolean needsRotate(long msgLength);

    String decideRotatedFilePath();

    /**
     * ローテーションを行う。
     */
    void rotate();

    /**
     * ログファイル読み込み時に、出力する設定情報を返す。<br>
     * 特に何も設定しない場合、下記の設定情報が出力される<br>
     * <br>
     * WRITER NAME        = [&lt;{@link LogWriter}の名称&gt;]<br>
     * WRITER CLASS       = [&lt;{@link LogWriter}のクラス名&gt;]<br>
     * FORMATTER CLASS    = [&lt;{@link LogFormatter}のクラス名&gt;]<br>
     * LEVEL              = [&lt;ログの出力制御の基準とする{@link LogLevel}&gt;]
     * FILE PATH          = [&lt;書き込み先のファイルパス&gt;]<br>
     * ENCODING           = [&lt;書き込み時に使用する文字エンコーディング&gt;]<br>
     * OUTPUT BUFFER SIZE = [&lt;出力バッファのサイズ&gt;]<br>
     *
     * @return 設定情報
     * @see FileLogWriter#getSettings()
     */
    String getSettings();

    /**
     * ログファイル書き込み時に発生するイベント。<br>
     * ファイルサイズによるローテーションなどを独自で実装したい場合に使用する。
     * @param message ログファイルに書き込まれるメッセージ
     */
    void onWrite(byte[] message);

    /**
     * ログファイル読み込み時に発生するイベント。<br>
     * ファイルサイズによるローテーションなどを独自で実装したい場合に使用する。
     * @param currentFileSize 読み込まれたファイルサイズ(KB)
     */
    void onRead(long currentFileSize);
}
