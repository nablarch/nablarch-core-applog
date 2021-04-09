package nablarch.core.log.app;

import nablarch.core.ThreadContext;
import nablarch.core.log.basic.JsonLogObjectBuilder;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 障害通知ログと障害解析ログのメッセージをJSON形式でフォーマットするクラス。
 * <p>
 * {@link FailureLogFormatter}では、フォーマットとして出力内容を設定するが、
 * 本クラスでは、 notificationTargets および、analysisTargets プロパティにて、
 * 出力項目を指定する。指定可能な出力項目は下記の通り。
 * <ul>
 * <li>failureCode: 障害コード</li>
 * <li>message: メッセージ</li>
 * <li>data: 処理対象データ</li>
 * <li>contact: 連絡先</li>
 * </ul>
 * </p>
 * @author Shuji Kitamura
 */
@Published(tag = "architect")
public class FailureJsonLogFormatter extends FailureLogFormatter {

    /** 障害コードの項目名 */
    private static final String TARGET_NAME_FAILURE_CODE = "failureCode";
    /** メッセージの項目名 */
    private static final String TARGET_NAME_MESSAGE = "message";
    /** 処理対象データの項目名 */
    private static final String TARGET_NAME_DATA = "data";
    /** 連絡先の項目名 */
    private static final String TARGET_NAME_CONTACT = "contact";

    /** 障害通知ログの出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_NOTIFICATION_TARGETS = PROPS_PREFIX + "notificationTargets";
    /** 障害解析ログの出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_ANALYSIS_TARGETS = PROPS_PREFIX + "analysisTargets";

    /** 出力項目のデフォルト値 */
    private static final String DEFAULT_TARGETS = "failureCode,message";

    /** 障害通知ログの出力項目 */
    private List<JsonLogObjectBuilder<FailureLogContext>> notificationStructuredTargets;
    /** 障害解析ログの出力項目 */
    private List<JsonLogObjectBuilder<FailureLogContext>> analysisStructuredTargets;

    /** 各種ログのJSONフォーマット支援オブジェクト */
    private JsonLogFormatterSupport support;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        Map<String, String> props = AppLogUtil.getProps();
        initializeFailureCodes(props);
        initializeMessage(props);
        initializeFormatterSupport(props, PROPS_PREFIX, AppLogUtil.getFilePath());
        initializeTargets(props);
    }

    /**
     * 各種ログのJSONフォーマット支援オブジェクトの初期化
     * @param props 各種ログ出力の設定情報
     */
    protected final void initializeFormatterSupport(Map<String, String> props, String prefix, String filePath) {
        support = new JsonLogFormatterSupport(
                new JsonSerializationSettings(props, prefix, filePath));
    }

    /**
     * 出力項目の初期化
     * @param props 各種ログ出力の設定情報
     */
    protected final void initializeTargets(Map<String, String> props) {
        notificationStructuredTargets = getStructuredTargets(props, PROPS_NOTIFICATION_TARGETS);
        analysisStructuredTargets = getStructuredTargets(props, PROPS_ANALYSIS_TARGETS);
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
        if (StringUtil.isNullOrEmpty(targetsStr)) {
            targetsStr = DEFAULT_TARGETS;
        }

        List<JsonLogObjectBuilder<FailureLogContext>> structuredTargets
                = new ArrayList<JsonLogObjectBuilder<FailureLogContext>>();

        String[] targets = targetsStr.split(",");
        Set<String> keys = new HashSet<String>(targets.length);
        for (String target: targets) {
            String key = target.trim();
            if (!StringUtil.isNullOrEmpty(key) && !keys.contains(key)) {
                keys.add(key);
                if (TARGET_NAME_FAILURE_CODE.equals(key)) { structuredTargets.add(new FailureCodeBuilder()); }
                else if (TARGET_NAME_MESSAGE.equals(key)) { structuredTargets.add(new MessageBuilder()); }
                else if (TARGET_NAME_DATA.equals(key)) { structuredTargets.add(new DataBuilder()); }
                else if (TARGET_NAME_CONTACT.equals(key)) {
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
     * {@inheritDoc}
     */
    @Override
    public String formatNotificationMessage(Throwable error, Object data, String failureCode, Object[] messageOptions) {
        return format(notificationStructuredTargets, error, data, failureCode, messageOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    public static class FailureCodeBuilder implements JsonLogObjectBuilder<FailureLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, FailureLogContext context) {
            structuredObject.put(TARGET_NAME_FAILURE_CODE, context.getFailureCode());
        }
    }

    /**
     * メッセージを処理するクラス。
     * @author Shuji Kitamura
     */
    public static class MessageBuilder implements JsonLogObjectBuilder<FailureLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, FailureLogContext context) {
            structuredObject.put(TARGET_NAME_MESSAGE, context.getMessage());
        }
    }

    /**
     * 処理対象データを処理するクラス。
     * @author Shuji Kitamura
     */
    public static class DataBuilder implements JsonLogObjectBuilder<FailureLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, FailureLogContext context) {
            structuredObject.put(TARGET_NAME_DATA, context.getData());
        }
    }

    /**
     * 連絡先を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class ContactBuilder implements JsonLogObjectBuilder<FailureLogContext> {

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
        @Override
        public void build(Map<String, Object> structuredObject, FailureLogContext context) {
            String requestId = ThreadContext.getRequestId();
            structuredObject.put(TARGET_NAME_CONTACT, findEntryValue(contacts, requestId));
        }
    }

}
