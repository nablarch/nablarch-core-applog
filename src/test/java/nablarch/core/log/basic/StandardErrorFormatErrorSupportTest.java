package nablarch.core.log.basic;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link StandardErrorFormatErrorSupport} の単体テスト。
 *
 * @author Tanaka Tomoyuki
 */
public class StandardErrorFormatErrorSupportTest {

    /**
     * 標準出力にメッセージが書き出されることをテスト。
     * @throws Exception エラーが発生した場合
     */
    @Test
    public void test() throws Exception {
        final PrintStream originalErr = System.err;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer));

        try {
            final StandardErrorFormatErrorSupport sut = new StandardErrorFormatErrorSupport();
            sut.outputFormatError("error message");

            final String message = buffer.toString("UTF-8");

            assertThat(message, is("error message" + System.getProperty("line.separator")));
        } finally {
            System.setErr(originalErr);
        }
    }
}
