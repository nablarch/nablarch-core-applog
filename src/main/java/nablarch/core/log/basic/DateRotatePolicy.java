package nablarch.core.log.basic;

import nablarch.core.date.BusinessDateUtil;
import nablarch.core.date.SystemTimeUtil;
import nablarch.core.log.Logger;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日次によるログのローテーションを行うクラス。<br>
 * <br>
 * プロパティファイルの記述ルールを下記に示す。<br>
 * <dl>
 *
 * <dt>dateType</dt>
 * <dd>日付タイプ。オプション。<br>
 *     日付ごとのローテーション判定に必要な日付の種類を指定する。<br>
 *     システム日付を使用する場合はsystem、業務日付を使用する場合はbusinessを指定する。<br>
 *     デフォルトはsystem。<br>
 *     このオプションは、rotatePolicyに{@link DateRotatePolicy}が設定されている場合に有効である。</dd>
 * </dl>
 * @author Kotaro Taki
 */
public class DateRotatePolicy implements RotatePolicy {

    /** 書き込み先のファイルパス */
    private String filePath;

    /** 日時フォーマット */
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    /** 重複したログファイル名に使用する日時フォーマット */
    private final DateFormat dupFileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    /** 日付タイプ */
    private DateType dateType;

    /** 日付タイプ列挙型 */
    enum DateType {
        SYSTEM,
        BUSINESS
    }

    /** 次回ローテーション日 */
    private Date nextUpdateDate;

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException 不正なdateTypeが指定されている場合
     * @throws RuntimeException dateTypeがSystemで、既にログパスにファイルが存在する際にファイルの作成時刻が取得できない場合
     */
    @Override
    public void initialize(ObjectSettings settings) {

        filePath = settings.getRequiredProp("filePath");

        String dt = settings.getProp("dateType");
        if (dt == null){
            dt = "system";
        }

        if (dt.equals("system")) {
            dateType = DateType.SYSTEM;
        } else if (dt.equals("business")) {
            dateType = DateType.BUSINESS;
        } else {
            throw new IllegalArgumentException("dateType was invalid");
        }

        File file = new File(filePath);

        // ファイルが存在、かつシステム日付の場合
        // 次回ローテーション日をファイルの更新時刻から算出する
        if (file.exists() && dateType == DateType.SYSTEM) {
            Date currentDate = new Date(file.lastModified());
            nextUpdateDate = calcNextUpdateDate(currentDate);
        }
    }

    /**
     * 現在日時を取得する。
     * @return 現在日時
     */
    private Date getCurrentDate() {
        if (dateType == DateType.SYSTEM) {
            return SystemTimeUtil.getDate();
        } else {
            String currentDateSt = BusinessDateUtil.getDate();
            try {
                return dateFormat.parse(currentDateSt);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 次回ローテション日を計算する。
     * @param currentDate 現在日時
     * @return 次回ローテション日のCalendarオブジェクト
     */
    private Date calcNextUpdateDate(Date currentDate) {
        Calendar cl = Calendar.getInstance();
        cl.setTime(currentDate);
        // 時・分・秒・ミリ秒を0にする
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);

        // その後、日を+1する
        cl.add(Calendar.DATE, 1);

        return cl.getTime();
    }

    /**
     * {@inheritDoc}<br>
     * 現在時刻 > インスタンス変数として保持している次回ローテション日の場合、ローテーションが必要と判定する。<br>
     * それ以外の場合は、ローテーションが不要と判定する。
     */
    @Override
    public boolean needsRotate(String message, Charset charset) {

        Date currentDate = getCurrentDate();

        return !nextUpdateDate.after(currentDate);
    }

    /**
     * {@inheritDoc}
     * {@link DateRotatePolicy}では、併せて次回ローテーション日を更新する。
     */
    @Override
    public String decideRotatedFilePath() {
        Calendar cl = Calendar.getInstance();
        cl.setTime(nextUpdateDate);
        cl.add(Calendar.DATE, -1);
        Date rotatedFileDate = cl.getTime();
        String rotatedFilePath = filePath + "." + dateFormat.format(rotatedFileDate) + ".old";

        Date currentDate = getCurrentDate();
        nextUpdateDate = calcNextUpdateDate(currentDate);

        return rotatedFilePath;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException ログファイルのリネームができない場合
     */
    @Override
    public void rotate(String rotatedFilePath) {
        File f = new File(rotatedFilePath);
        if (f.exists()) {
            rotatedFilePath = filePath + "." + dupFileDateFormat.format(getCurrentDate()) + ".old";
        }

        if (!new File(filePath).renameTo(new File(rotatedFilePath))) {
            throw new IllegalStateException(
                    "renaming failed. File#renameTo returns false. src file = [" + filePath + "], dest file = [" + rotatedFilePath + "]");
        }
    }

    /**
     * {@inheritDoc}<br>
     * 設定情報のフォーマットを下記に示す。<br>
     * <br>
     * FILE AUTO CHANGE   = [&lt;ログファイルを自動で切り替えるか否か。&gt;]<br>
     *
     * @return 設定情報
     * @see FileLogWriter#getSettings()
     */
    @Override
    public String getSettings() {
        return "\tFILE AUTO CHANGE   = [" + true + "]" + Logger.LS;
    }

    /**
     * {@inheritDoc}<br>
     * {@link DateRotatePolicy}では、次回ローテーション日が設定されていない場合に次回ローテーション日を計算する。
     */
    @Override
    public void setupIfNeeded() {
        if (nextUpdateDate == null) {
            Date currentDate = getCurrentDate();

            nextUpdateDate = calcNextUpdateDate(currentDate);
        }
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
