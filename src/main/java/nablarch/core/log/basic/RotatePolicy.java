package nablarch.core.log.basic;

import java.io.File;
import java.nio.charset.Charset;

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
     * @param message ログファイルに書き込まれるメッセージ
     * @param charset 書き込み時に使用する文字エンコーディング
     * @return ローテーションが必要な場合はtrue
     */
    boolean needsRotate(String message, Charset charset);

    /**
     * ローテーション先のファイル名を決定する。
     * @return ローテーション先のファイル名
     */
    String decideRotatedFilePath();

    /**
     * ローテーションを行う。
     * @param rotatedFilePath ローテーション先のファイルパス
     */
    void rotate(String rotatedFilePath);

    /**
     * ログファイル読み込み時に出力する、ローテーションの設定情報を返す。<br>
     * @return 設定情報
     * @see FileLogWriter#getSettings()
     */
    String getSettings();

    /**
     * ログファイル書き込み時に発生するイベント。<br>
     * ファイルサイズによるローテーションなどを独自で実装したい場合に使用する。
     * @param message ログファイルに書き込まれるメッセージ
     * @param charset 書き込み時に使用する文字エンコーディング
     */
    void onWrite(String message, Charset charset);

    /**
     * ログファイル読み込み時に発生するイベント。<br>
     * ファイルサイズによるローテーションなどを独自で実装したい場合に使用する。
     * @param file 読み込まれたファイル
     */
    void onOpenFile(File file);
}
