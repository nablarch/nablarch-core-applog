package nablarch.core.log.basic;

import nablarch.core.log.Logger;

import java.io.File;
import java.nio.charset.Charset;
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
     * 起動時にログファイルパスにログファイルが既に存在する場合は、ファイルの更新時刻から次回ローテーション時刻を算出する。
     * この初期化処理により、例えば2023年3月6日にログファイルに書き込み後アプリを停止。２日後にアプリを再起動する場合、
     * 起動時に本クラスが保持する次回ローテーション時刻は2023年3月7日 となる。
     * そのため起動後の初回ログ書き込み時にローテーションを行い、古いログファイル名は <ログファイルパス>.20230307000000.old となる。
     */
    @Override
    public void initialize(ObjectSettings settings) {

        String updateTime = settings.getProp("updateTime");

        String formattedUpdateTime = "00:00:00";
        if (updateTime != null) {
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
     * 次回ローテション時刻を計算する。<br>
     * <br>
     * 引数で渡された現在日時の時刻部分をupdateTimeに設定されている時刻とする。<br>
     * updateTimeが設定されていない場合は、00:00:00。<br>
     * 上記で算出された次回ローテション時刻を元に、以下の通り次回ローテション時刻を計算する。<br>
     * 1.次回ローテション時刻 >= 現在日時 の場合は、次回ローテーション時刻を返す。<br>
     * 2.次回ローテション時刻 < 現在日時 の場合は、次回ローテション時刻+1日を返す。
     * @param currentDate 現在日時
     * @return 次回ローテション時刻のDateオブジェクト
     */
    private Date calcNextUpdateDate(Date currentDate) {
        Calendar nextUpdateCalendar = Calendar.getInstance();
        nextUpdateCalendar.setTime(currentDate);

        Calendar nextUpdateTimeCalendar = Calendar.getInstance();
        nextUpdateTimeCalendar.setTime(nextUpdateTime);

        // 時・分・秒としてupdateTimeに指定された値を設定する
        nextUpdateCalendar.set(Calendar.HOUR_OF_DAY, nextUpdateTimeCalendar.get(Calendar.HOUR_OF_DAY));
        nextUpdateCalendar.set(Calendar.MINUTE, nextUpdateTimeCalendar.get(Calendar.MINUTE));
        nextUpdateCalendar.set(Calendar.SECOND, nextUpdateTimeCalendar.get(Calendar.SECOND));
        // ミリ秒を0にする
        nextUpdateCalendar.set(Calendar.MILLISECOND, 0);

        // 現在時刻の時分秒 > nextUpdateTimeの時分秒の場合は、日付に1を加える
        Date nextUpdateDate = nextUpdateCalendar.getTime();
        if (currentDate.getTime() > nextUpdateDate.getTime()) {
            nextUpdateCalendar.add(Calendar.DATE, 1);
        }

        return nextUpdateCalendar.getTime();
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
     * 古いログファイル名は、 <ログファイルパス>.yyyyMMddHHmmss.old のフォーマットで出力される。
     * 日時には、 本クラスが保持している次回ローテーション時刻が出力される。
     * もし、ローテーション先に同名のファイルが存在している場合、 <ログファイルパス>.yyyyMMddHHmmssSSS.old のフォーマット で出力される。
     * この時、日時にはローテーション実施時刻が出力される。
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
     * リネーム完了後に、 次回ローテション時刻を更新する。
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
