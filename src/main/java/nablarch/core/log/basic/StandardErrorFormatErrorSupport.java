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
    public void outputFormatError(String message) {
        System.err.println(message);
    }
}
