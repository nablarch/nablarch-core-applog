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
 * ファイルの最大サイズが指定されていない場合は、ローテーションしない。
 * <p>
 * プロパティファイルの記述ルールを下記に示す。<br>
 *   <dl>
 *     <dt>maxFileSize</dt>
 *     <dd>書き込み先ファイルの最大サイズ。オプション。<br>
 *       単位はキロバイト。1000バイトを1キロバイトと換算する。<br>
 *       指定値が解析可能な整数値(Long.parseLong)でない場合は自動切替なし。<br>
 *       指定値が０以下の場合は自動切替なし。</dd>
 *   </dl>
 * </p>
 * <p>
 *   ローテーション後のログファイル名は、 <ログファイルパス>.yyyyMMddHHmmssSSS.old となる。
 *   yyyyMMddHHmmssSSSはローテーション実施時刻。
 * </p>
 *
 * @author Kotaro Taki
 */
public class FileSizeRotatePolicy implements RotatePolicy {

    /** 書き込み先ファイルの最大サイズ */
    private long maxFileSize;

    /** 書き込み先のファイルパス */
    private String logFilePath;

    /** 書き込み先ファイルの現在のサイズ */
    private long currentFileSize;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ObjectSettings settings) {
        logFilePath = settings.getRequiredProp("filePath");

        try {
            maxFileSize = Long.parseLong(settings.getProp("maxFileSize")) * FileLogWriter.KB;
        } catch (NumberFormatException e) {
            maxFileSize = 0;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException ログファイルのリネームができない場合
     */
    @Override
    public void rotate(String rotatedFilePath) {
        if (!new File(logFilePath).renameTo(new File(rotatedFilePath))) {
            throw new IllegalStateException(
                    "renaming failed. File#renameTo returns false. src file = [" + logFilePath + "], dest file = [" + rotatedFilePath + "]");
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
     * 古いログファイル名は、 <ログファイルパス>.yyyyMMddHHmmssSSS.old のフォーマットで出力される。
     * 日時には、ローテーション実施時刻が出力される。
     */
    @Override
    public String decideRotatedFilePath() {
        DateFormat oldFileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return logFilePath + "." + oldFileDateFormat.format(new Date()) + ".old";
    }

    /**
     * {@inheritDoc}<br>
     * 読み込んだファイルサイズを現在のファイルサイズとして、インスタンス変数に保持する。
     */
    @Override
    public void onOpenFile(File file) {
        this.currentFileSize = file.length();
    }

    /**
     * {@inheritDoc}<br>
     * ファイルサイズに書き込むメッセージサイズを足すことで、現在のファイルサイズを更新する。
     */
    @Override
    public void onWrite(String message, Charset charset) {
        this.currentFileSize += StringUtil.getBytes(message, charset).length;
    }

    /**
     * {@inheritDoc}
     * 設定情報のフォーマットを下記に示す。<br>
     * <pre>
     * {@code
     * FILE AUTO CHANGE    = [<ログファイルを自動で切り替えるか否か。>]
     * MAX FILE SIZE       = [<書き込み先ファイルの最大サイズ>]
     * CURRENT FILE SIZE   = [<書き込み先ファイルの現在のサイズ>]
     * }
     * </pre>
     *
     * @return 設定情報
     * @see FileLogWriter#getSettings()
     */
    @Override
    public String getSettings() {
        return "\tFILE AUTO CHANGE    = [" + (maxFileSize > 0) + "]" + Logger.LS
                + "\tMAX FILE SIZE       = [" + maxFileSize + "]" + Logger.LS
                + "\tCURRENT FILE SIZE   = [" + currentFileSize + "]" + Logger.LS;
    }
}
