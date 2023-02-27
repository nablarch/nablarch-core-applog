package nablarch.core.log.basic;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

import nablarch.core.log.Logger;
import nablarch.core.util.StringUtil;
import nablarch.core.util.ObjectUtil;

/**
 * ファイルにログを書き込むクラス。<br>
 * <br>
 * FileLogWriterクラスの特徴を下記に示す。<br>
 * <ul>
 * <li>ログフォーマッタを設定で指定できる。</li>
 * <li>設定されたクラスに従いログファイルのローテーションを行うことができる。</li>
 * <li>初期処理と終了処理、ログファイルの切り替え時に、書き込み先のログファイルにINFOレベルでメッセージを出力する。</li>
 * </ul>
 * 本クラスでは、ファイルへのログ書き込みに{@link java.io.BufferedOutputStream}を使用する。<br>
 * 出力バッファのサイズは設定で変更できる。<br>
 * 書き込み処理では、書き込み後にすぐにフラッシュし、書き込んだ内容をファイルに反映する。<br>
 * <br>
 * プロパティファイルの記述ルールを下記に示す。<br>
 * <dl>
 * <dt>filePath</dt>
 * <dd>書き込み先のファイルパス。必須。</dd>
 *
 * <dt>encoding</dt>
 * <dd>書き込み時に使用する文字エンコーディング。オプション。<br>
 *     指定しなければシステムプロパティ(file.encoding)から取得した文字エンコーディング。</dd>
 *
 * <dt>outputBufferSize</dt>
 * <dd>出力バッファのサイズ。オプション。<br>
 *     単位はキロバイト。1000バイトを1キロバイトと換算する。１以上を指定する。指定しなければ8KB。</dd>
 *
 * <dt>rotatePolicy</dt>
 * <dd>ファイルローテーション実行クラス。オプション。<br>
 *     {@link RotatePolicy}が実装されたクラスを指定する。<br>
 *     デフォルト値は{@link FileSizeRotatePolicy}</dd>
 *
 * <dt>maxFileSize</dt>
 * <dd>書き込み先ファイルの最大サイズ。オプション。<br>
 *     単位はキロバイト。1000バイトを1キロバイトと換算する。指定しなければ自動切替なし。<br>
 *     指定値が解析可能な整数値(Long.parseLong)でない場合は自動切替なし。<br>
 *     指定値が０以下の場合は自動切替なし。<br>
 *     古いログファイル名は、<通常のファイル名>.yyyyMMddHHmmssSSS.old。<br>
 *     このオプションは、rotatePolicyに{@link FileSizeRotatePolicy}が設定されているか、何も設定されていない場合に有効である。</dd>
 *
 * <dt>dateType</dt>
 * <dd>日付タイプ。オプション。<br>
 *     日付ごとのローテーション判定に必要な日付の種類を指定する。<br>
 *     システム日時を使用する場合はsystem、業務日付を使用する場合はbusinessを指定する。<br>
 *     デフォルトはsystem。<br>
 *     このオプションは、rotatePolicyに{@link DateRotatePolicy}が設定されている場合に有効である。</dd>
 * </dl>
 * 本クラスでは、初期処理と終了処理、ログファイルの切り替え時に、書き込み先のログファイルにINFOレベルでメッセージを出力する。
 *
 * @author Kiyohito Itoh
 */
public class FileLogWriter extends LogWriterSupport {

    /** FQCN */
    private static final String FQCN = FileLogWriter.class.getName();

    /** キロバイトを算出するための係数 */
    private static final int KB = 1000;

    /** 書き込み先のファイルパス */
    private String filePath;

    /** 書き込み時に使用する文字エンコーディング */
    private Charset charset;

    /** 出力バッファのサイズ */
    private int outputBufferSize;

    /** ファイルに書き込みを行う出力ストリーム */
    private OutputStream out;

    /** ファイルローテーションを行うためのインターフェース */
    private RotatePolicy rotatePolicy;

    /**
     * {@inheritDoc}
     * <p/>
     * プロパティファイルで指定された設定情報を取得し、ファイルへの書き込みを行う出力ストリームを初期化する。<br>
     * 初期処理完了後、INFOレベルで設定情報を出力する。
     */
    protected void onInitialize(ObjectSettings settings) {

        filePath = settings.getRequiredProp("filePath");

        String encoding = settings.getProp("encoding");
        if (encoding == null) {
            encoding = System.getProperty("file.encoding");
        }
        charset = Charset.forName(encoding);

        try {
            outputBufferSize = Integer.parseInt(settings.getProp("outputBufferSize")) * KB;
        } catch (NumberFormatException e) {
            outputBufferSize = 8 * KB;
        }

        String className = settings.getProp("rotatePolicy");
        if (className == null) {
            rotatePolicy = new FileSizeRotatePolicy();
        } else {
            rotatePolicy = ObjectUtil.createInstance(className);
        }

        rotatePolicy.initialize(settings, charset);

        initializeWriter("initialized.");
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
     * <br>
     * 追加で{@link RotatePolicy#getSettings()}によって得られた設定情報が出力される。<br>
     * @return 設定情報
     * @see LogWriterSupport#getSettings()
     */
    protected String getSettings() {
        return new StringBuilder(512)
                .append(super.getSettings())
                .append("\tFILE PATH          = [").append(filePath).append("]").append(Logger.LS)
                .append("\tENCODING           = [").append(charset.displayName()).append("]").append(Logger.LS)
                .append("\tOUTPUT BUFFER SIZE = [").append(outputBufferSize).append("]").append(Logger.LS)
                .append(rotatePolicy.getSettings())
                .toString();
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * 終了処理の前に、INFOレベルで終了メッセージを出力する。<br>
     * ファイルへの書き込みを行う出力ストリームをクローズする。
     */
    protected void onTerminate() {
        synchronized (this) {
            terminateWriter("terminated.");
        }
    }

    /**
     * {@inheritDoc}<br>
     * <br>
     * 設定情報に基づきログをファイルに書き込む。<br>
     * 書き込み後にすぐにフラッシュし、書き込んだ内容をファイルに反映する。<br>
     * <br>
     * IO例外が発生した場合は、IO例外をラップして{@link IllegalStateException}を送出する。
     */
    protected void onWrite(String formattedMessage) {
        rotatePolicy.setupIfNeeded();
        synchronized (this) {
            if (out == null) {
                throw new IllegalStateException(
                        String.format("failed to write for FileLogWriter has already terminated. name = [%s]", getName()));
            }
            renameFile(formattedMessage);
            try {
                write(formattedMessage);
            } catch (IOException e) {
                throw new IllegalStateException("failed to write. out name = [" + getName() + "]", e);
            }
        }
    }

    /**
     * ローテーションの種類毎にファイルをリネームする。<br>
     * ファイルをリネームする場合は、併せてファイルへの書き込みを行う出力ストリームを初期化する。
     * @param formattedMessage メッセージ
     */
    private void renameFile(String formattedMessage) {

        if (!rotatePolicy.needsRotate(formattedMessage)) {
            return;
        }

        String rotatedFilePath = rotatePolicy.decideRotatedFilePath();
        String message = "change [" + filePath + "] -> [" + rotatedFilePath + "]";
        terminateWriter(message);
        rotatePolicy.rotate(rotatedFilePath);
        initializeWriter(message);
    }

    /**
     * ファイルへの書き込みを行う出力ストリームと書き込み先ファイルの現在のサイズを初期化する。
     * @param message 初期処理完了後に書き込むメッセージ
     */
    private void initializeWriter(String message) {
        try {
            out = new BufferedOutputStream(new FileOutputStream(filePath, true), outputBufferSize);
            rotatePolicy.onOpenFile(new File(filePath));
            LogContext context = new LogContext(FQCN, LogLevel.INFO, message + Logger.LS + getSettings(), null);
            // 本来はメッセージを連結する前にメッセージ出力要否をチェックすべきだが、
            // 実行される回数が少なくパフォーマンスに与える影響が軽微と考えてあえてここでチェックする。
            if (needsToWrite(context)) {
                String formattedMessage = getFormatter().format(
                        context);
                write(formattedMessage);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("failed to create %s. file name = [%s], encoding = [%s], buffer size =[%s]",
                    Writer.class.getName(), filePath, charset.displayName(), outputBufferSize), e);
        }
    }

    /**
     * ファイルへの書き込みを行う出力ストリームの終了処理を行う。
     * @param message 終了処理の直前に書き込むメッセージ
     */
    private void terminateWriter(String message) {
        try {
            LogContext context = new LogContext(FQCN, LogLevel.INFO, message, null);
            // 本来はメッセージを連結する前にメッセージ出力要否をチェックすべきだが、
            // 実行される回数が少なくパフォーマンスに与える影響が軽微と考えてあえてここでチェックする。
            if (needsToWrite(context)) {
                String formattedMessage = getFormatter().format(context);
                write(formattedMessage);
            }
            out.close();
            out = null;
        } catch (IOException e) {
            throw new IllegalStateException("termination failed. out name = [" + getName() + "]", e);
        }
    }

    /**
     * メッセージの書き込みを行いフラッシュする。
     * @param message メッセージ
     * @throws IOException IO例外
     */
    private void write(String message) throws IOException {
        rotatePolicy.onWrite(message);
        out.write(StringUtil.getBytes(message, charset));
        out.flush();
    }
}
