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
 * ログ書き込み時の現在日時 >= 保持している次回ローテーション日時の場合、ローテーションを行う。<br>
 * <p>
 * プロパティファイルの記述ルールを下記に示す。
 *   <dl>
 *     <dt>rotateTime</dt>
 *     <dd>ローテーション時刻。オプション。<br>
 *       特定の時刻にログファイルをローテーションしたい場合に指定する。<br>
 *       時刻は、HH, HH:mm, HH:mm:ss のいずれかのフォーマットで指定する。デフォルトは00:00:00。</dd>
 *   </dl>
 * </p>
 * <p>
 *   次回ローテーション日時は、システム起動時とローテーション時に算出する。
 *   計算方法は以下の通り。
 * </p>
 * <p>
 *   まず、次回ローテーション日時を決めるための基準日時を決定する。
 *   基準日時は、システム起動時かつログファイルが既に存在する場合とそれ以外の場合で以下の2通りの日時を採用する。
 *   <ul>
 *     <li>システム起動時かつログファイルが存在する場合 → ログファイルの最終更新日時</li>
 *     <li>それ以外の場合 → システム日時</li>
 *   </ul>
 * </p>
 * <p>
 *   次に、基準日時の時刻と rotateTime の時刻を比較して、次回ローテーション日時の日付を決定する。
 *   <ul>
 *     <li>基準日時の時刻 <= rotateTime → システム日付</li>
 *     <li>rotateTime < 基準日時の時刻 → システム日付 + 1日</li>
 *   </ul>
 * </p>
 * <p>
 *   この日付に rotateTime の時刻を設定したものを、システム起動時の次回ローテーション日時とする。
 * </p>
 * <p>
 *   例えば、rotateTimeに 12:00:00 が設定されており、基準日時が 2023-03-25 11:59:59 の場合、
 *   次回ローテーション日時は 2023-03-25 12:00:00 となる。<br>
 *   rotateTimeに 12:00:00 が設定されており、基準日時が 2023-03-25 12:00:01 の場合、
 *   次回ローテーション日時は 2023-03-26 12:00:00 となる。
 * </p>
 * <p>
 *   ローテーション後のログファイル名は、 <ログファイルパス>.yyyyMMddHHmmssSSS.old となる。
 *   yyyyMMddHHmmssSSSにはローテーション実施時刻が出力される。
 * </p>
 *
 * @author Kotaro Taki
 */
public class DateRotatePolicy implements RotatePolicy {

    /** 書き込み先のファイルパス */
    private String logFilePath;

    /** 次回ローテーション日時 */
    private Date nextRotateDateTime;

    /** プロパティファイルに設定された更新時刻から生成したDateオブジェクト */
    private Date nextRotateTime;

    /**
     * {@inheritDoc}
     * 起動時にログファイルパスにログファイルが既に存在する場合は、ファイルの更新時刻から次回ローテーション日時を算出する。
     * この初期化処理により、例えば2023年3月6日にログファイルに書き込み後アプリを停止。２日後にアプリを再起動する場合、
     * 起動時に本クラスが保持する次回ローテーション日時は2023年3月7日 となる。
     */
    @Override
    public void initialize(ObjectSettings settings) {

        String rotateTime = settings.getProp("rotateTime");

        String formattedRotateTime = "00:00:00";
        if (rotateTime != null) {
            String[] timeFields = rotateTime.split(":");
            if (timeFields.length >= 4 || timeFields.length == 0) {
                throw new IllegalArgumentException("Invalid rotateTime");
            }

            if (timeFields.length == 1) {
                formattedRotateTime = rotateTime + ":00:00";
            } else if (timeFields.length == 2) {
                formattedRotateTime = rotateTime + ":00";
            } else {
                formattedRotateTime = rotateTime;
            }
        }

        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        format.setLenient(false);
        try {
            nextRotateTime = format.parse(formattedRotateTime);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid rotateTime", e);
        }

        // ファイルが存在している場合、次回ローテーション日時をファイルの更新時刻から算出する
        logFilePath = settings.getRequiredProp("filePath");
        File logFile = new File(logFilePath);
        Date currentDate;
        if (logFile.exists()) {
            long lastModifiedDate = logFile.lastModified();
            if (lastModifiedDate == 0) {
                throw new IllegalStateException("failed to read file. file name = [" + logFilePath + "]");
            }
            currentDate = new Date(lastModifiedDate);
        } else {
            currentDate = currentDate();
        }
        nextRotateDateTime = calcNextRotateDateTime(currentDate);
    }

    /**
     * 次回ローテーション日時を計算する。<br>
     * <br>
     * 引数で渡された現在日時の時刻部分をrotateTimeに設定されている時刻とする。<br>
     * rotateTimeが設定されていない場合は、00:00:00。<br>
     * 上記で算出された次回ローテーション日時を元に、以下の通り次回ローテーション日時を計算する。<br>
     * 1.次回ローテーション日時 >= 現在日時 の場合は、次回ローテーション日時を返す。<br>
     * 2.次回ローテーション日時 < 現在日時 の場合は、次回ローテーション日時+1日を返す。
     *
     * @param currentDate 現在日時
     * @return 次回ローテーション日時のDateオブジェクト
     */
    private Date calcNextRotateDateTime(Date currentDate) {
        Calendar nextRotateDateTimeCalendar = Calendar.getInstance();
        nextRotateDateTimeCalendar.setTime(currentDate);

        Calendar nextRotateHHmmssCalendar = Calendar.getInstance();
        nextRotateHHmmssCalendar.setTime(nextRotateTime);

        // 時・分・秒としてrotateTimeに指定された値を設定する
        nextRotateDateTimeCalendar.set(Calendar.HOUR_OF_DAY, nextRotateHHmmssCalendar.get(Calendar.HOUR_OF_DAY));
        nextRotateDateTimeCalendar.set(Calendar.MINUTE, nextRotateHHmmssCalendar.get(Calendar.MINUTE));
        nextRotateDateTimeCalendar.set(Calendar.SECOND, nextRotateHHmmssCalendar.get(Calendar.SECOND));
        // ミリ秒を0にする
        nextRotateDateTimeCalendar.set(Calendar.MILLISECOND, 0);

        // 現在時刻の時分秒 > nextRotateTimeの時分秒の場合は、日付に1を加える
        Date nextRotateDate = nextRotateDateTimeCalendar.getTime();
        if (currentDate.getTime() > nextRotateDate.getTime()) {
            nextRotateDateTimeCalendar.add(Calendar.DATE, 1);
        }

        return nextRotateDateTimeCalendar.getTime();
    }

    /**
     * {@inheritDoc}<br>
     * 現在時刻 >= 次回ローテーション日時の場合、ローテーションが必要と判定する。<br>
     * それ以外の場合は、ローテーションが不要と判定する。
     */
    @Override
    public boolean needsRotate(String message, Charset charset) {

        Date currentDate = currentDate();

        return currentDate.getTime() >= nextRotateDateTime.getTime();
    }

    /**
     * {@inheritDoc}
     * 古いログファイル名は、 <ログファイルパス>.yyyyMMddHHmmssSSS.old のフォーマットで出力される。
     * 日時には、ローテーション実施時刻が出力される。
     */
    @Override
    public String decideRotatedFilePath() {
        return logFilePath + "." + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(currentDate()) + ".old";
    }

    /**
     * {@inheritDoc}
     * リネーム完了後に、 次回ローテーション日時を更新する。
     *
     * @throws IllegalStateException ログファイルのリネームができない場合
     */
    @Override
    public void rotate(String rotatedFilePath) {
        if (!new File(logFilePath).renameTo(new File(rotatedFilePath))) {
            throw new IllegalStateException(
                    "renaming failed. File#renameTo returns false. src file = [" + logFilePath + "], dest file = [" + rotatedFilePath + "]");
        }

        nextRotateDateTime = calcNextRotateDateTime(currentDate());
    }

    /**
     * {@inheritDoc}
     * 設定情報のフォーマットを下記に示す。<br>
     * <pre>
     * {@code
     * NEXT ROTATE DATE    = [<次回ローテーション日時>]
     * CURRENT DATE        = [<現在時刻>]
     * }
     * </pre>
     *
     * @return 設定情報
     * @see FileLogWriter#getSettings()
     */
    @Override
    public String getSettings() {
        SimpleDateFormat settingDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat rotateHHmmssFormat = new SimpleDateFormat("HH:mm:ss");
        return "\tNEXT ROTATE DATE    = [" + settingDateFormat.format(nextRotateDateTime) + "]" + Logger.LS
                + "\tCURRENT DATE        = [" + settingDateFormat.format(currentDate()) + "]" + Logger.LS;
    }

    /**
     * 現在日時を返す。
     *
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
