package nablarch.core.log.basic;

import nablarch.core.date.BusinessDateUtil;
import nablarch.core.date.SystemTimeUtil;
import nablarch.core.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日時によるログのローテーションを行うクラス。<br>
 * ファイルの最大サイズが指定されている場合は、現在のファイルサイズにメッセージ長を加えた値が、
 * ファイルの最大サイズ以上になる場合は、ログのローテーションを行う。<br>
 * ファイルの最大サイズが指定されていない場合は、ローテーションをしない。
 * @author Kotaro Taki
 */
public class DateRotatePolicy implements RotatePolicy {

    /** 次回ローテーション実施時の、リネーム先のファイルパス */
    private String newFilePath;

    /** 書き込み先のファイルパス */
    private String filePath;

    /** 日時フォーマット */
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    /** ログ出力用の日時フォーマット */
    private final DateFormat logDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    /** 日付タイプ */
    private DateType dateType;

    /** 日付タイプ列挙型 */
    enum DateType {
        System,
        Business
    }

    /** 次回ローテーション日 */
    private Date nextUpdateDate;

    /** 日付の計算に使用するCalendar */
    private Calendar cl;

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException 不正なdateTypeが指定されている場合
     * @throws RuntimeException dateTypeがSystemで、既にログパスにファイルが存在する際にファイルの作成時刻が取得できない場合
     */
    @Override
    public void initialize(ObjectSettings settings) {

        filePath = settings.getRequiredProp("filePath");

        String dt = settings.getRequiredProp("dateType");
        if (dt == null || dt.equals("System")) {
            dateType = DateType.System;
        } else if (dt.equals("Business")) {
            dateType = DateType.Business;
        } else {
            throw new IllegalArgumentException("dateType was invalid");
        }

        File file = new File(filePath);

        Date currentDate;
        // ファイルが存在、かつシステム日付の場合
        // 次回更新時刻をファイルの作成時刻から算出する
        if (file.exists() && dateType == DateType.System) {
            final BasicFileAttributes attrs;
            try {
                attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read file attributes");
            }
            final FileTime fileTime = attrs.creationTime();
            currentDate = new Date(fileTime.toMillis());
        } else {
            currentDate = getCurrentDate();
        }

        cl = Calendar.getInstance();

        calcNextUpdateDate(currentDate);
        nextUpdateDate = cl.getTime();
    }

    /**
     * 現在日時を取得する。
     * @return 現在日時
     */
    private Date getCurrentDate() {
        if (dateType == DateType.System) {
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
     * 次回ローテション時刻を計算する。
     * @param currentDate 現在日時
     */
    private void calcNextUpdateDate(Date currentDate) {
        cl.setTime(currentDate);
        // 時・分・秒・ミリ秒を0にする
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);

        // その後、日を+1する
        cl.add(Calendar.DATE, 1);
    }

    /**
     * リネーム先のファイル名を決定するための、時刻を計算する。
     * @param nextUpdateDate 次回更新日時
     */
    private void calcNewFilePath(Date nextUpdateDate) {
        cl.setTime(nextUpdateDate);
        cl.add(Calendar.DATE, -1);
    }

    /**
     * {@inheritDoc}<br>
     * 現在時刻 > インスタンス変数として保持している次回ローテション時刻の場合、ローテーションが必要と判定する。<br>
     * ローテーションが必要場合は、併せてローテーション実施時のリネーム先のファイルパスと次回ローテション時刻を更新する。<br>
     * それ以外の場合は、ローテーションをしない。
     */
    @Override
    public boolean needsRotate(long msgLength) {
        Date currentDate = getCurrentDate();

        if (nextUpdateDate.after(currentDate)) {
            return false;
        } else {
            calcNewFilePath(nextUpdateDate);
            Date newFileDate = cl.getTime();
            newFilePath = filePath + "." + dateFormat.format(newFileDate) + ".old";

            calcNextUpdateDate(currentDate);
            nextUpdateDate = cl.getTime();

            return true;
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
     * {@inheritDoc}
     */
    @Override
    public String getNewFilePath() {
        return newFilePath;
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
     * NEXT CHANGE DATE   = [&lt;ログファイルの次回更新日&gt;]<br>
     * CURRENT DATE TIME  = [&lt;現在時刻&gt;]<br>
     *
     * @return 設定情報
     * @see LogWriterSupport#getSettings()
     */
    @Override
    public String getSettings() {
        return "\tFILE AUTO CHANGE   = [" + true + "]" + Logger.LS
                + "\tNEXT CHANGE DATE   = [" + dateFormat.format(nextUpdateDate) + "]" + Logger.LS
                + "\tCURRENT DATE TIME  = [" + logDateFormat.format(getCurrentDate()) + "]" + Logger.LS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onWrite(byte[] message) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRead(long currentFileSize) {

    }
}
