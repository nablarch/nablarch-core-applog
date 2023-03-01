package nablarch.core.log.basic;

import nablarch.core.log.Logger;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日次によるログのローテーションを行うクラス。<br>
 * <br>
 * プロパティファイルの記述ルールを下記に示す。<br>
 * 下記プロパティは、rotatePolicyに{@link DateRotatePolicy}が設定されている場合に有効である。
 * <dl>
 *
 * <dt>updateTime</dt>
 * <dd>更新時刻。オプション。<br>
 *     特定の時刻以降のログファイルをローテーションしたい場合に指定する。<br>
 *     時刻は24時間表記とする。デフォルトは0。</dd>
 * </dl>
 * @author Kotaro Taki
 */
public class DateRotatePolicy implements RotatePolicy {

    /** 書き込み先のファイルパス */
    private String filePath;

    /** ログファイル名と設定情報の出力に使用する日時フォーマット */
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");

    /** 重複したログファイル名に使用する日時フォーマット */
    private final DateFormat dupFileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    /** 次回ローテーション時刻 */
    private Date nextUpdateDate;

    /** 更新時刻 */
    private int updateTime;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ObjectSettings settings) {

        filePath = settings.getRequiredProp("filePath");

        File file = new File(filePath);

        String specificTime = settings.getProp("updateTime");
        if (specificTime != null) {
            try {
                this.updateTime = Integer.parseInt(specificTime);
            } catch (NumberFormatException e) {
                this.updateTime = 0;
            }
        }

        // ファイルが存在している場合、次回ローテーション時刻をファイルの更新時刻から算出する
        Date currentDate;
        if (file.exists()) {
            currentDate = new Date(file.lastModified());
        } else {
            currentDate = new Date();
        }
        nextUpdateDate = calcNextUpdateDate(currentDate);
    }

    /**
     * 次回ローテション時刻を計算する。
     * @param currentDate 現在日時
     * @return 次回ローテション時刻のCalendarオブジェクト
     */
    private Date calcNextUpdateDate(Date currentDate) {
        Calendar cl = Calendar.getInstance();
        cl.setTime(currentDate);
        // 時・分・秒・ミリ秒を0にする
        cl.set(Calendar.HOUR_OF_DAY, updateTime);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);

        // その後、日を+1する
        cl.add(Calendar.DATE, 1);

        return cl.getTime();
    }

    /**
     * {@inheritDoc}<br>
     * 現在時刻 > インスタンス変数として保持している次回ローテション時刻の場合、ローテーションが必要と判定する。<br>
     * それ以外の場合は、ローテーションが不要と判定する。
     */
    @Override
    public boolean needsRotate(String message, Charset charset) {

        Date currentDate = new Date();

        return currentDate.after(nextUpdateDate) || currentDate.equals(nextUpdateDate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String decideRotatedFilePath() {
        Calendar cl = Calendar.getInstance();
        cl.setTime(nextUpdateDate);
        cl.add(Calendar.DATE, -1);
        Date rotatedFileDate = cl.getTime();
        String rotatedFilePath = filePath + "." + dateFormat.format(rotatedFileDate) + ".old";

        File f = new File(rotatedFilePath);
        if (f.exists()) {
            rotatedFilePath = filePath + "." + dupFileDateFormat.format(new Date()) + ".old";
        }

        return rotatedFilePath;
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

        Date currentDate = new Date();
        nextUpdateDate = calcNextUpdateDate(currentDate);
    }

    /**
     * {@inheritDoc}
     * 設定情報のフォーマットを下記に示す。<br>
     * <br>
     * NEXT CHANGE DATE   = [&lt;次回更新時刻&gt;]<br>
     * CURRENT DATE       = [&lt;現在時刻&gt;]<br>
     * UPDATE TIME = [&lt;更新時刻&gt;]<br>
     *
     * @return 設定情報
     * @see FileLogWriter#getSettings()
     */
    @Override
    public String getSettings() {
        return "\tNEXT CHANGE DATE    = [" + dateFormat.format(nextUpdateDate) + "]" + Logger.LS
                + "\tCURRENT DATE     = [" + dateFormat.format(new Date()) + "]" + Logger.LS
                + "\tUPDATE TIME      = [" + updateTime + "]" + Logger.LS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onWrite(String message, Charset charset) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpenFile(File file) {

    }
}
