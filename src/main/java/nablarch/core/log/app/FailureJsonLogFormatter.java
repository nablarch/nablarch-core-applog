package nablarch.core.log.app;

import nablarch.core.ThreadContext;
import nablarch.core.log.basic.JsonLogObjectBuilder;
import nablarch.core.util.StringUtil;

import java.util.*;

/**
 * 障害通知ログと障害解析ログのメッセージをJSON形式でフォーマットするクラス。
 * <pre>
 *   failureCode: 障害コード
 *   message: メッセージ
 *   data: 処理対象データ
 *   contact: 連絡先
 * </pre>
 * @author Shuji Kitamura
 */
public class FailureJsonLogFormatter extends FailureLogFormatter {

    /** 障害コードの項目名 */
    private static final String TARGET_NAME_FAILURE_CODE = "failureCode";
    /** メッセージの項目名 */
    private static final String TARGET_NAME_MESSAGE = "message";
    /** 処理対象データの項目名 */
    private static final String TARGET_NAME_DATA = "data";
    /** 連絡先の項目名 */
    private static final String TARGET_NAME_CONTACT = "contact";

    /** リクエスト処理開始時の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_NOTIFICATION_TARGETS = PROPS_PREFIX + "notificationTargets";
    /** hiddenパラメータ復号後の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_ANALYSIS_TARGETS = PROPS_PREFIX + "analysisTargets";

    /** フォーマット指定が無い場合に使用する出力項目のデフォルト値 */
    private static final String DEFAULT_TARGETS = "failureCode,message";

    /** ログ出力項目 */
    private List<JsonLogObjectBuilder<FailureLogContext>> notificationstructuredTargets;
    /** ログ出力項目 */
    private List<JsonLogObjectBuilder<FailureLogContext>> analysisStructuredTargets;

    /** 各種ログのJSONフォーマット支援オブジェクト */
    private JsonLogFormatterSupport support;

    /**
     * フォーマットの初期化
     * @param props 各種ログ出力の設定情報
     */
    protected void initializeFormat(Map<String, String> props) {
        support = new JsonLogFormatterSupport(PROPS_PREFIX);

        notificationstructuredTargets = getStructuredTargets(AppLogUtil.getProps(), PROPS_NOTIFICATION_TARGETS);
        analysisStructuredTargets = getStructuredTargets(AppLogUtil.getProps(), PROPS_ANALYSIS_TARGETS);
    }

    /**
     * ログ出力項目を取得する。
     * @param props 各種ログ出力の設定情報
     * @param targetsPropName 出力項目のプロパティ名
     * @return ログ出力項目
     */
    protected List<JsonLogObjectBuilder<FailureLogContext>> getStructuredTargets(
            Map<String, String> props, String targetsPropName) {

        String targetsStr = props.get(targetsPropName);
        if (StringUtil.isNullOrEmpty(targetsStr)) targetsStr = DEFAULT_TARGETS;

        List<JsonLogObjectBuilder<FailureLogContext>> structuredTargets
                = new ArrayList<JsonLogObjectBuilder<FailureLogContext>>();

        String[] targets = targetsStr.split(",");
        Set<String> keys = new HashSet<String>(targets.length);
        for (String target: targets) {
            String key = target.trim();
            if (!StringUtil.isNullOrEmpty(key) && !keys.contains(key)) {
                keys.add(key);
                if (TARGET_NAME_FAILURE_CODE.equals(target)) { structuredTargets.add(new FailureCodeBuilder()); }
                else if (TARGET_NAME_MESSAGE.equals(target)) { structuredTargets.add(new MessageBuilder()); }
                else if (TARGET_NAME_DATA.equals(target)) { structuredTargets.add(new DataBuilder()); }
                else if (TARGET_NAME_CONTACT.equals(target)) {
                    List<Map.Entry<String, String>> contactList = getContactList(props);
                    structuredTargets.add(new ContactBuilder(contactList));
                } else {
                    throw new IllegalArgumentException(
                            String.format("[%s] is unknown target. property name = [%s]", key, targetsPropName));
                }
            }
        }

        return structuredTargets;
    }

    /**
     * 障害通知ログのメッセージをフォーマットする。
     * <pre>
     * フォーマット対象の出力項目を下記に示す。
     * 障害コード
     * 障害コードから取得したメッセージ
     * 派生元実行時ID
     * </pre>
     * @param error エラー情報
     * @param data 処理対象データ
     * @param failureCode 障害コード
     * @param messageOptions 障害コードからメッセージを取得する際に使用するオプション情報
     * @return フォーマット済みのメッセージ
     */
    public String formatNotificationMessage(Throwable error, Object data, String failureCode, Object[] messageOptions) {
        return format(notificationstructuredTargets, error, data, failureCode, messageOptions);
    }

    /**
     * 障害解析ログのメッセージをフォーマットする。
     * <pre>
     * フォーマット対象の出力項目を下記に示す。
     * 障害コード
     * 障害コードから取得したメッセージ
     * 派生元実行時ID
     * </pre>
     * @param error エラー情報
     * @param data 処理対象データ
     * @param failureCode 障害コード
     * @param messageOptions 障害コードからメッセージを取得する際に使用するオプション情報
     * @return フォーマット済みのメッセージ
     */
    public String formatAnalysisMessage(Throwable error, Object data, String failureCode, Object[] messageOptions) {
        return format(analysisStructuredTargets, error, data, failureCode, messageOptions);
    }

    /**
     * 指定されたフォーマット済みのログ出力項目を使用してメッセージをフォーマットする。
     * <pre>
     * フォーマット対象の出力項目を下記に示す。
     * 障害コード
     * 障害コードから取得したメッセージ
     * 派生元実行時ID
     * </pre>
     * @param structuredTargets ログ出力項目
     * @param error エラー情報
     * @param data 処理対象データ
     * @param failureCode 障害コード
     * @param messageOptions 障害コードからメッセージを取得する際に使用するオプション情報
     * @return フォーマット後のメッセージ
     */
    protected String format(List<JsonLogObjectBuilder<FailureLogContext>> structuredTargets,
                            Throwable error, Object data,
                            String failureCode, Object[] messageOptions) {
        failureCode = getFailureCode(failureCode, error);
        FailureLogContext context = new FailureLogContext(failureCode, getMessage(failureCode, messageOptions, error), data);
        return support.getStructuredMessage(structuredTargets, context);
    }

    /**
     * 障害コードを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class FailureCodeBuilder implements JsonLogObjectBuilder<FailureLogContext> {

        /**
         * {@inheritDoc}
         */
        public void build(Map<String, Object> structuredObject, FailureLogContext context) {
            structuredObject.put(TARGET_NAME_FAILURE_CODE, context.getFailureCode());
        }
    }

    /**
     * メッセージを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MessageBuilder implements JsonLogObjectBuilder<FailureLogContext> {

        /**
         * {@inheritDoc}
         */
        public void build(Map<String, Object> structuredObject, FailureLogContext context) {
            structuredObject.put(TARGET_NAME_MESSAGE, context.getMessage());
        }
    }

    /**
     * 処理対象データを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class DataBuilder implements JsonLogObjectBuilder<FailureLogContext> {

        /**
         * {@inheritDoc}
         */
        public void build(Map<String, Object> structuredObject, FailureLogContext context) {
            structuredObject.put(TARGET_NAME_DATA, context.getData());
        }
    }

    /**
     * 連絡先を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ContactBuilder implements JsonLogObjectBuilder<FailureLogContext> {

        /** 連絡先 */
        private final List<Map.Entry<String, String>> contacts;

        /**
         * 連絡先を設定する。
         * @param contacts 連絡先
         */
        public ContactBuilder(List<Map.Entry<String, String>> contacts) {
            this.contacts = contacts;
        }

        /**
         * {@inheritDoc}
         */
        public void build(Map<String, Object> structuredObject, FailureLogContext context) {
            String requestId = ThreadContext.getRequestId();
            structuredObject.put(TARGET_NAME_CONTACT, findEntryValue(contacts, requestId));
        }
    }

}
