package nablarch.core.log.basic;

/**
 * フォーマットエラーを標準エラーに出力するクラス。
 *
 * @author Shuji Kitamura
 */
public class StandardErrorFormatErrorSupport implements FormatErrorSupport {

    /**
     * {@inheritDoc}
     */
    @Override
    // ログ出力できないときに最終手段として標準エラー出力に書き出すためのクラスのため、この警告は無視して問題ない
    @SuppressWarnings("squid:S106")
    public void outputFormatError(String message) {
        System.err.println(message);
    }
}
