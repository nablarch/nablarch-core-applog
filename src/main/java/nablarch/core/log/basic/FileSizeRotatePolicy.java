package nablarch.core.log.basic;

import nablarch.core.log.Logger;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ファイルサイズによるログのローテーションを行うクラス。<br>
 * ファイルの最大サイズが指定されている場合は、現在のファイルサイズにメッセージ長を加えた値が、
 * ファイルの最大サイズ以上になる場合は、ログのローテーションを行う。<br>
 * ファイルの最大サイズが指定されていない場合は、ローテーションをしない。
 * @author Kotaro Taki
 */
public class FileSizeRotatePolicy implements RotatePolicy {

    /** 次回ローテーション実施時の、リネーム先のファイルパス */
    private String newFilePath;

    /** 書き込み先ファイルの最大サイズ */
    private long maxFileSize;

    /** キロバイトを算出するための係数 */
    private static final int KB = 1000;

    /** 書き込み先のファイルパス */
    private String filePath;

    /** 書き込み先ファイルの現在のサイズ */
    private long currentFileSize;

    /** 古いログファイル名に使用する日時フォーマット */
    private final DateFormat oldFileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ObjectSettings settings) {
        filePath = settings.getRequiredProp("filePath");

        try {
            maxFileSize = Long.parseLong(settings.getProp("maxFileSize")) * KB;
        } catch (NumberFormatException e) {
            maxFileSize = 0;
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException ログファイルのリネームができない場合
     */
    @Override
    public void rotate() {
        if (!new File(filePath).renameTo(new File(newFilePath))) {
            throw new IllegalStateException(
                    "renaming failed. File#renameTo returns false. src file = [" + filePath + "], dest file = [" + newFilePath + "]");
        }
    }

    /**
     * {@inheritDoc}<br>
     * ファイルの最大サイズが指定されている場合は、現在のファイルサイズにメッセージ長を加えた値が、
     * ファイルの最大サイズ以上になる場合は、ローテーションが必要と判定する。<br>
     * ローテーションが必要場合は、併せてローテーション実施時のリネーム先のファイルパスを更新する。<br>
     * ファイルの最大サイズが指定されていない場合は、ローテーションをしない。
     */
    @Override
    public boolean needsRotate(long msgLength) {
        if (maxFileSize <= 0) {
            return false;
        }

        if (msgLength + currentFileSize <= maxFileSize) {
            return false;
        }

        return true;
    }

    @Override
    public String decideRotatedFilePath() {
         newFilePath = filePath + "." + oldFileDateFormat.format(new Date()) + ".old";
         return newFilePath;
    }

    /**
     * ログファイル読み込み時に発生するイベント。<br>
     * 読み込んだファイルサイズを現在のファイルサイズとして、インスタンス変数に保持する。
     * @param currentFileSize 読み込まれたファイルサイズ(KB)
     */
    @Override
    public void onRead(long currentFileSize) {
        this.currentFileSize = currentFileSize;
    }

    /**
     * ログファイル書き込み時に発生するイベント。<br>
     * ファイルサイズに書き込むメッセージサイズを足すことで、現在のファイルサイズを更新する。
     * @param message ログファイルに書き込まれるメッセージ
     */
    @Override
    public void onWrite(byte[] message) {
        this.currentFileSize += message.length;
    }

    /**
     * 設定情報を取得する。<br>
     * <br>
     * 設定情報のフォーマットを下記に示す。<br>
     * <br>
     * WRITER NAME        = [&lt;{@link LogWriter}の名称&gt;]<br>
     * WRITER CLASS       = [&lt;{@link LogWriter}のクラス名&gt;]<br>
     * FORMATTER CLASS    = [&lt;{@link LogFormatter}のクラス名&gt;]<br>
     * LEVEL              = [&lt;ログの出力制御の基準とする{@link LogLevel}&gt;]
     * FILE PATH          = [&lt;書き込み先のファイルパス&gt;]<br>
     * ENCODING           = [&lt;書き込み時に使用する文字エンコーディング&gt;]<br>
     * OUTPUT BUFFER SIZE = [&lt;出力バッファのサイズ&gt;]<br>
     * FILE AUTO CHANGE   = [&lt;ログファイルを自動で切り替えるか否か。&gt;]<br>
     * MAX FILE SIZE      = [&lt;書き込み先ファイルの最大サイズ&gt;]<br>
     * CURRENT FILE SIZE  = [&lt;書き込み先ファイルの現在のサイズ&gt;]<br>
     *
     * @return 設定情報
     * @see LogWriterSupport#getSettings()
     */
    @Override
    public String getSettings() {
        return "\tFILE AUTO CHANGE   = [" + (maxFileSize > 0) + "]" + Logger.LS
                + "\tMAX FILE SIZE      = [" + maxFileSize + "]" + Logger.LS
                + "\tCURRENT FILE SIZE  = [" + currentFileSize + "]" + Logger.LS;
    }

    @Override
    public void setupIfNeeded() {

    }
}
