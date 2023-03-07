package nablarch.core.log.basic;

import nablarch.core.log.Logger;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日時でログのローテーションを行うクラス。<br>
 * <br>
 * プロパティファイルの記述ルールを下記に示す。
 * <dl>
 *
 * <dt>updateTime</dt>
 * <dd>更新時刻。オプション。<br>
 *     特定の時刻にログファイルをローテーションしたい場合に指定する。<br>
 *     時刻は、HH, HH:mm, HH:mm:ss のいずれかのフォーマットで指定する。デフォルトは00:00:00。</dd>
 * </dl>
 * @author Kotaro Taki
 */
public class DateRotatePolicy implements RotatePolicy {

    /** 書き込み先のファイルパス */
    private String logFilePath ;

    /** 次回ローテーション時刻 */
    private Date nextUpdateDate;

    /** プロパティファイルに設定された更新時刻から生成したDateオブジェクト */
    private  Date nextUpdateTime;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ObjectSettings settings) {

        String updateTime = settings.getProp("updateTime");

        String formattedUpdateTime;
        if (updateTime == null) {
            formattedUpdateTime = "00:00:00";
        } else {
            String[] splits = updateTime.split(":");
            if (splits.length >= 4 || splits.length == 0) {
                throw  new IllegalArgumentException("Invalid updateTime");
            }

            if (splits.length == 1) {
                formattedUpdateTime = updateTime+":00:00";
            } else if (splits.length == 2) {
                formattedUpdateTime = updateTime +":00";
            } else {
                formattedUpdateTime = updateTime;
            }
        }

        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        format.setLenient(false);
        try {
            nextUpdateTime = format.parse(formattedUpdateTime);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid updateTime", e);
        }

        // ファイルが存在している場合、次回ローテーション時刻をファイルの更新時刻から算出する
        logFilePath  = settings.getRequiredProp("filePath");
        File logFile = new File(logFilePath);
        Date currentDate;
        if (logFile.exists()) {
            long lastModifiedDate = logFile.lastModified();
            if (lastModifiedDate == 0) {
                throw  new IllegalStateException("failed to read file. file name = [" + logFilePath + "]");
            }
            currentDate = new Date(lastModifiedDate);
        } else {
            currentDate = currentDate();
        }
        nextUpdateDate = calcNextUpdateDate(currentDate);
    }

    /**
     * 次回ローテション時刻を計算する。
     * @param currentDate 現在日時
     * @return 次回ローテション時刻のCalendarオブジェクト
     */
    private Date calcNextUpdateDate(Date currentDate) {
        Calendar nextUpdateCalender = Calendar.getInstance();
        nextUpdateCalender.setTime(currentDate);
        // 時・分・秒としてupdateTimeに指定された値を設定する
        nextUpdateCalender.set(Calendar.HOUR_OF_DAY, nextUpdateTime.getHours());
        nextUpdateCalender.set(Calendar.MINUTE, nextUpdateTime.getMinutes());
        nextUpdateCalender.set(Calendar.SECOND, nextUpdateTime.getSeconds());
        // ミリ秒を0にする
        nextUpdateCalender.set(Calendar.MILLISECOND, 0);

        // 現在時刻の時分秒 > nextUpdateTimeの時分秒の場合は、日付に1を加える
        if (currentDate.getTime() > nextUpdateCalender.getTime().getTime()) {
            nextUpdateCalender.add(Calendar.DATE, 1);
        }

        return nextUpdateCalender.getTime();
    }

    /**
     * {@inheritDoc}<br>
     * 現在時刻 >= インスタンス変数として保持している次回ローテション時刻の場合、ローテーションが必要と判定する。<br>
     * それ以外の場合は、ローテーションが不要と判定する。
     */
    @Override
    public boolean needsRotate(String message, Charset charset) {

        Date currentDate =currentDate();

        return currentDate.getTime() >= nextUpdateDate.getTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String decideRotatedFilePath() {
        String rotatedFilePath = logFilePath  + "." + new SimpleDateFormat("yyyyMMddHHmmss").format(nextUpdateDate) + ".old";

        File rotatedFile = new File(rotatedFilePath);
        if (rotatedFile .exists()) {
            rotatedFilePath = logFilePath  + "." + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(currentDate()) + ".old";
        }

        return rotatedFilePath;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException ログファイルのリネームができない場合
     */
    @Override
    public void rotate(String rotatedFilePath) {
        if (!new File(logFilePath ).renameTo(new File(rotatedFilePath))) {
            throw new IllegalStateException(
                    "renaming failed. File#renameTo returns false. src file = [" + logFilePath  + "], dest file = [" + rotatedFilePath + "]");
        }

        nextUpdateDate = calcNextUpdateDate(currentDate());
    }

    /**
     * {@inheritDoc}
     * 設定情報のフォーマットを下記に示す。<br>
     * <pre>
     * {@code
     * NEXT CHANGE DATE    = [<次回更新時刻>]
     * CURRENT DATE        = [<現在時刻>]
     * UPDATE TIME         = [<更新時刻>]
     * }
     * </pre>
     * @return 設定情報
     * @see FileLogWriter#getSettings()
     */
    @Override
    public String getSettings() {
        SimpleDateFormat settingDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat updateTimeDateFormat = new SimpleDateFormat("HH:mm:ss");
        return "\tNEXT CHANGE DATE    = [" + settingDateFormat.format(nextUpdateDate) + "]" + Logger.LS
                + "\tCURRENT DATE        = [" + settingDateFormat.format(currentDate()) + "]" + Logger.LS
                + "\tUPDATE TIME         = [" + updateTimeDateFormat.format(nextUpdateTime) + "]" + Logger.LS;
    }

    /**
     * 現在日時を返す。
     * @return 現在日時
     */
    protected Date currentDate() {
        return new Date();
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
