package nablarch.core.log.basic;

import nablarch.core.log.Logger;
import nablarch.core.util.StringUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ファイルサイズによるログのローテーションを行うクラス。<br>
 * 設定したファイルの最大サイズを超える場合にローテーションを行う。
 * ファイルの最大サイズが指定されていない場合は、ローテーションしない。<br>
 * <br>
 * プロパティファイルの記述ルールを下記に示す。<br>
 * <dl>
 *
 * <dt>maxFileSize</dt>
 * <dd>書き込み先ファイルの最大サイズ。オプション。<br>
 *     単位はキロバイト。1000バイトを1キロバイトと換算する。指定しなければ自動切替なし。<br>
 *     指定値が解析可能な整数値(Long.parseLong)でない場合は自動切替なし。<br>
 *     指定値が０以下の場合は自動切替なし。<br>
 *     古いログファイル名は、<通常のファイル名>.yyyyMMddHHmmssSSS.old。<br>
 *     このオプションは、rotatePolicyに{@link FileSizeRotatePolicy}が設定されているか、何も設定されていない場合に有効である。</dd>
 * </dl>
 * @author Kotaro Taki
 */
public class FileSizeRotatePolicy implements RotatePolicy {

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

    /** 書き込み時に使用する文字エンコーディング */
    private Charset charset;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ObjectSettings settings) {
        this.charset = charset;
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
    public void rotate(String rotatedFilePath) {
        if (!new File(filePath).renameTo(new File(rotatedFilePath))) {
            throw new IllegalStateException(
                    "renaming failed. File#renameTo returns false. src file = [" + filePath + "], dest file = [" + rotatedFilePath + "]");
        }
    }

    /**
     * {@inheritDoc}<br>
     * 設定したファイルの最大サイズを超える場合にtrueを返す。
     * ファイルの最大サイズが指定されていない場合はfalseを返す。
     */
    @Override
    public boolean needsRotate(String message, Charset charset) {
        if (maxFileSize <= 0) {
            return false;
        }

        return StringUtil.getBytes(message, charset).length + currentFileSize > maxFileSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String decideRotatedFilePath() {
        return filePath + "." + oldFileDateFormat.format(new Date()) + ".old";
    }

    /**
     * {@inheritDoc}<br>
     * {@link FileSizeRotatePolicy}では、読み込んだファイルサイズを現在のファイルサイズとして、インスタンス変数に保持する。
     */
    @Override
    public void onOpenFile(File file) {
        this.currentFileSize = file.length();
    }

    /**
     * {@inheritDoc}<br>
     * {@link FileSizeRotatePolicy}では、ファイルサイズに書き込むメッセージサイズを足すことで、現在のファイルサイズを更新する。
     */
    @Override
    public void onWrite(String message, Charset charset) {
        this.currentFileSize += StringUtil.getBytes(message, charset).length;
    }

    /**
     * {@inheritDoc}
     * 設定情報のフォーマットを下記に示す。<br>
     * <br>
     * FILE AUTO CHANGE   = [&lt;ログファイルを自動で切り替えるか否か。&gt;]<br>
     * MAX FILE SIZE      = [&lt;書き込み先ファイルの最大サイズ&gt;]<br>
     * CURRENT FILE SIZE  = [&lt;書き込み先ファイルの現在のサイズ&gt;]<br>
     *
     * @return 設定情報
     * @see FileLogWriter#getSettings()
     */
    @Override
    public String getSettings() {
        return "\tFILE AUTO CHANGE   = [" + (maxFileSize > 0) + "]" + Logger.LS
                + "\tMAX FILE SIZE      = [" + maxFileSize + "]" + Logger.LS
                + "\tCURRENT FILE SIZE  = [" + currentFileSize + "]" + Logger.LS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupIfNeeded() {

    }
}
