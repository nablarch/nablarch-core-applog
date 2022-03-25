package nablarch.core.log.basic;

import java.util.Map;

/**
 * 構造化ログのオブジェクトを構築するインターフェース。
 * @param <CTX> オブジェクトのデータの取得に使用するコンテキストの型
 * @author Shuji Kitamura
 */
public interface JsonLogObjectBuilder<CTX> {

    /**
     * 構造化ログのオブジェクトを構築する。
     * @param structuredObject 構築先のオブジェクト
     * @param context ログコンテキスト
     */
    void build(Map<String, Object> structuredObject, CTX context);
}
