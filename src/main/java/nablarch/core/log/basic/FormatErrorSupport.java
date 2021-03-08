package nablarch.core.log.basic;

/**
 * ログのフォーマットエラー出力を処理するインターフェース。
 *
 * @author Shuji Kitamura
 */
public interface FormatErrorSupport {

    /**
     * フォーマットエラー内容を出力する。
     * @param message 出力するメッセージ
     */
    void outputFormatError(String message);

}
