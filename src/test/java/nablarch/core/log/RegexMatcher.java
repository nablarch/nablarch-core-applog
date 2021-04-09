package nablarch.core.log;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.regex.Pattern;

/**
 * 正規表現のMatcherクラス。
 */
public class RegexMatcher extends TypeSafeMatcher<String> {

    /** 正規表現 */
    private final String regex;

    /** コンパイル済みの正規表現 */
    private final Pattern pattern;

    /**
     * 正規表現によるMatcherを生成する。
     * @param regex 正規表現
     * @return 正規表現によるMatcher
     */
    public static TypeSafeMatcher<String> matches(String regex) {
        return new RegexMatcher(regex);
    }

    /**
     * コンストラクタ。
     * @param regex 正規表現
     */
    public RegexMatcher(String regex) {
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void describeTo(Description description) {
        description.appendText("is matched ").appendValue(regex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matchesSafely(String object) {
        return object != null && pattern.matcher(object).matches();
    }
}
