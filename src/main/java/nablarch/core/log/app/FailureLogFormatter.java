package nablarch.core.log.app;

import nablarch.core.ThreadContext;
import nablarch.core.log.LogItem;
import nablarch.core.log.LogSettings;
import nablarch.core.log.LogUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.MessageUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 障害通知ログと障害解析ログのメッセージをフォーマットするクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class FailureLogFormatter {
    
    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(FailureLogFormatter.class);
    
    /** デフォルトのフォーマット */
    private static final String DEFAULT_FORMAT = "fail_code = [$failureCode$] $message$";
    
    /** フレームワークのパッケージ名のプレフィックス */
    private static final String FRAMEWORK_PACKAGE_PREFIX = "nablarch";
    
    /** プロパティ名のプレフィックス */
    public static final String PROPS_PREFIX = "failureLogFormatter.";
    
    /** デフォルトの障害コードを取得する際に使用するプロパティ名 */
    private static final String PROPS_DEFAULT_FAILURE_CODE = PROPS_PREFIX + "defaultFailureCode";
    
    /** デフォルトのメッセージを取得する際に使用するプロパティ名 */
    private static final String PROPS_DEFAULT_MESSAGE = PROPS_PREFIX + "defaultMessage";
    
    /** メッセージを取得する際に使用する言語 */
    private static final String PROPS_LANGUAGE = PROPS_PREFIX + "language";
    
    /** 障害通知ログのフォーマットを取得する際に使用するプロパティ名 */
    private static final String PROPS_NOTIFICATION_FORMAT = PROPS_PREFIX + "notificationFormat";
    
    /** 障害解析ログのフォーマットを取得する際に使用するプロパティ名 */
    private static final String PROPS_ANALYSIS_FORMAT = PROPS_PREFIX + "analysisFormat";
    
    /** 連絡先情報のファイルパスを取得する際に使用するプロパティ名 */
    private static final String PROPS_CONTACT_FILE_PATH = PROPS_PREFIX + "contactFilePath";
    
    /** アプリケーション用の障害コード変換情報のファイルパスを取得する際に使用するプロパティ名 */
    private static final String PROPS_APP_FAILURE_CODE_FILE_PATH = PROPS_PREFIX + "appFailureCodeFilePath";
    
    /** アプリケーション用のメッセージID変換情報のファイルパスを取得する際に使用するプロパティ名 */
    private static final String PROPS_APP_MESSAGE_ID_FILE_PATH = PROPS_PREFIX + "appMessageIdFilePath";
    
    /** フレームワーク用の障害コード変換情報のファイルパスを取得する際に使用するプロパティ名 */
    private static final String PROPS_FW_FAILURE_CODE_FILE_PATH = PROPS_PREFIX + "fwFailureCodeFilePath";
    
    /** フレームワーク用のメッセージID変換情報のファイルパスを取得する際に使用するプロパティ名 */
    private static final String PROPS_FW_MESSAGE_ID_FILE_PATH = PROPS_PREFIX + "fwMessageIdFilePath";
    
    /** デフォルトの障害コード */
    private String defaultFailureCode;
    
    /** デフォルトのメッセージ */
    private String defaultMessage;
    
    /** メッセージの言語 */
    private Locale locale;
    
    /** アプリケーション用の障害コード(キーはソース上で指定した障害コード) */
    private Map<String, String> appFailureCodes;
    
    /** フレームワーク用の障害コード(キーはパッケージ名) */
    private List<Map.Entry<String, String>> fwFailureCodes;
    
    /** 障害通知ログのフォーマット済みのログ出力項目 */
    private LogItem<FailureLogContext>[] notificationLogItems;
    
    /** 障害解析ログのフォーマット済みのログ出力項目 */
    private LogItem<FailureLogContext>[] analysisLogItems;
    
    /**
     * フォーマット済みのログ出力項目を初期化する。
     */
    public FailureLogFormatter() {
        initialize();
    }

    /**
     * 初期化
     */
    protected void initialize() {
        Map<String, String> props = AppLogUtil.getProps();
        initializeFailureCodes(props);
        initializeMessage(props);
        initializeFormat(props);
    }

    /**
     * 障害コードの初期化
     * @param props 各種ログ出力の設定情報
     */
    protected void initializeFailureCodes(Map<String, String> props) {
        defaultFailureCode = getDefaultFailureCode(props);
        appFailureCodes = getAppFailureCodes(props);
        fwFailureCodes = getFwFailureCodes(props);
    }

    /**
     * メッセージの初期化
     * @param props 各種ログ出力の設定情報
     */
    protected void initializeMessage(Map<String, String> props) {
        defaultMessage = getDefaultMessage(props);
        locale = props.containsKey(PROPS_LANGUAGE) ? new Locale(props.get(PROPS_LANGUAGE)) : null;
    }

    /**
     * フォーマットの初期化
     * @param props 各種ログ出力の設定情報
     */
    protected void initializeFormat(Map<String, String> props) {
        Map<String, LogItem<FailureLogContext>> logItems = getLogItems(props);
        notificationLogItems = LogUtil.createFormattedLogItems(logItems, getNotificationFormat(props));
        analysisLogItems = LogUtil.createFormattedLogItems(logItems, getAnalysisFormat(props));

        initContacts(props, notificationLogItems);
        initContacts(props, analysisLogItems);
    }

    /**
     * 指定されたフォーマット済みのログ出力項目に連絡先が含まれている場合は、連絡先を初期化する。
     * プロパティファイルのキー名の長さで降順にソートして返す。
     * @param props 各種ログ出力の設定情報
     * @param formattedLogItems フォーマット済みのログ出力項目
     */
    @SuppressWarnings("unchecked")
    private void initContacts(Map<String, String> props, LogItem<FailureLogContext>[] formattedLogItems) {
        LogItem<FailureLogContext> contactItem = LogUtil.findLogItem(formattedLogItems, ContactItem.class);
        if (contactItem != null) {
            List<Map.Entry<String, String>> contactList = getContactList(props);
            ((ContactItem) contactItem).setContacts(contactList);
        }
    }

    /**
     * 連絡先を取得する。
     * プロパティファイルのキー名の長さで降順にソートして返す。
     * @param props 各種ログ出力の設定情報
     * @return 連絡先
     */
    protected List<Map.Entry<String, String>> getContactList(Map<String, String> props) {
        Map<String, String> contactMap = getProps(props, PROPS_CONTACT_FILE_PATH, null);
        List<Map.Entry<String, String>> contactList = new ArrayList<Map.Entry<String, String>>(contactMap.size());
        contactList.addAll(contactMap.entrySet());
        Collections.sort(contactList, KEY_LENGTH_DESCENDING_COMPARATOR);
        return contactList;
    }
    
    /**
     * アプリケーション用の障害コード変換情報を取得する。
     * @param props 各種ログの設定情報
     * @return アプリケーション用の障害コード変換情報
     */
    protected Map<String, String> getAppFailureCodes(Map<String, String> props) {
        Map<String, String> appFailCodesMap = getProps(props, PROPS_APP_FAILURE_CODE_FILE_PATH, null);
        if (appFailCodesMap.isEmpty()) {
            // 「メッセージID」->「障害コード」変更に対して下位互換を保つための実装。
            appFailCodesMap = getProps(props, PROPS_APP_MESSAGE_ID_FILE_PATH, null);
        }
        return appFailCodesMap;
    }
    
    /**
     * フレームワーク用の障害コード変換情報を取得する。
     * プロパティファイルのキー名の長さで降順にソートして返す。
     * @param props 各種ログの設定情報
     * @return フレームワーク用の障害コード変換情報
     */
    protected List<Map.Entry<String, String>> getFwFailureCodes(Map<String, String> props) {
        
        Map<String, String> fwFailCodesMap = getProps(props, PROPS_FW_FAILURE_CODE_FILE_PATH, null);
        if (fwFailCodesMap.isEmpty()) {
            fwFailCodesMap = getProps(props, PROPS_FW_MESSAGE_ID_FILE_PATH, null);
        }
        
        List<Map.Entry<String, String>> fwMsgList = new ArrayList<Map.Entry<String, String>>(fwFailCodesMap.entrySet());
        List<Map.Entry<String, String>> listToRemove = new ArrayList<Map.Entry<String, String>>();
        for (Map.Entry<String, String> entry : fwMsgList) {
            if (!entry.getKey().startsWith(FRAMEWORK_PACKAGE_PREFIX)) {
                listToRemove.add(entry);
            }
        }
        fwMsgList.removeAll(listToRemove);
        
        if (fwMsgList.size() < 2) {
            return fwMsgList;
        }
        
        Collections.sort(fwMsgList, KEY_LENGTH_DESCENDING_COMPARATOR);
        
        return fwMsgList;
    }
    
    /** {@link Map.Entry}リストのキー文字列の長さの降順にソートする場合に使用する{@link Comparator} */
    private static final Comparator<Map.Entry<String, String>> KEY_LENGTH_DESCENDING_COMPARATOR
        = new Comparator<Map.Entry<String, String>>() {
            public int compare(Entry<String, String> o1, Entry<String, String> o2) {
                return o2.getKey().length() - o1.getKey().length();
            }
        };

    /**
     * デフォルトの障害コードを取得する。
     * @param props 各種ログの設定情報
     * @return デフォルトの障害コード
     */
    protected String getDefaultFailureCode(Map<String, String> props) {
        
        if (props.containsKey(PROPS_DEFAULT_FAILURE_CODE)) {
            return props.get(PROPS_DEFAULT_FAILURE_CODE);
        }
        
        throw new IllegalArgumentException(
            String.format("%s was not specified. %s is required.",
                          PROPS_DEFAULT_FAILURE_CODE, PROPS_DEFAULT_FAILURE_CODE));
    }
    
    /**
     * デフォルトのメッセージを取得する。
     * @param props 各種ログの設定情報
     * @return デフォルトのメッセージ
     */
    protected String getDefaultMessage(Map<String, String> props) {
        if (!props.containsKey(PROPS_DEFAULT_MESSAGE)) {
            throw new IllegalArgumentException(
                    String.format("%s was not specified. %s is required.", PROPS_DEFAULT_MESSAGE, PROPS_DEFAULT_MESSAGE));
        }
        return props.get(PROPS_DEFAULT_MESSAGE);
    }
    
    /**
     * 障害通知ログのフォーマットを取得する。
     * @param props 各種ログの設定情報
     * @return 障害通知ログのフォーマット
     */
    protected String getNotificationFormat(Map<String, String> props) {
        String format = DEFAULT_FORMAT;
        if (props.containsKey(PROPS_NOTIFICATION_FORMAT)) {
            format = props.get(PROPS_NOTIFICATION_FORMAT);
        }
        return format;
    }
    
    /**
     * 障害解析ログのフォーマットを取得する。
     * @param props 各種ログの設定情報
     * @return 障害解析ログのフォーマット
     */
    protected String getAnalysisFormat(Map<String, String> props) {
        String format = DEFAULT_FORMAT;
        if (props.containsKey(PROPS_ANALYSIS_FORMAT)) {
            format = props.get(PROPS_ANALYSIS_FORMAT);
        }
        return format;
    }
    
    /**
     * プロパティを取得する。
     * @param props 各種ログの設定情報
     * @param key プロパティのファイルパスを各種ログの設定情報から取得する際に使用するキー
     * @param defaultFilePath デフォルトのファイルパス
     * @return プロパティ
     */
    protected Map<String, String> getProps(Map<String, String> props, String key, String defaultFilePath) {
        String filePath = defaultFilePath;
        if (props.containsKey(key)) {
            filePath = props.get(key);
        }
        if (filePath == null) {
            return Collections.emptyMap();
        }
        return new LogSettings(filePath).getProps(); 
    }
    
    /**
     * フォーマット対象のログ出力項目を取得する。
     * <p/>
     * デフォルトで下記のプレースホルダを設定したログ出力項目を返す。
     * <pre>
     *   $failureCode$: 障害コード
     *   $messageId$: 障害コード(旧メッセージID。下位互換性のため)
     *   $message$: メッセージ
     *   $data$: 処理対象データ
     *   $contact$: 連絡先
     * </pre>
     * @param props 各種ログの設定情報
     * @return フォーマット対象のログ出力項目
     */
    protected Map<String, LogItem<FailureLogContext>> getLogItems(Map<String, String> props) {
        Map<String, LogItem<FailureLogContext>> logItems = new HashMap<String, LogItem<FailureLogContext>>();
        
        // 「メッセージID」->「障害コード」変更に対して下位互換を保つための実装。
        logItems.put("$messageId$", new FailureCodeItem());
        
        logItems.put("$failureCode$", new FailureCodeItem());
        logItems.put("$message$", new MessageItem());
        
        // 処理対象データ
        logItems.put("$data$", new DataItem());
        
        // 連絡先
        logItems.put("$contact$", new ContactItem());
        return logItems;
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
        return format(notificationLogItems, error, data, failureCode, messageOptions);
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
        return format(analysisLogItems, error, data, failureCode, messageOptions);
    }
    
    /**
     * 指定されたフォーマット済みのログ出力項目を使用してメッセージをフォーマットする。
     * <pre>
     * フォーマット対象の出力項目を下記に示す。
     * 障害コード
     * 障害コードから取得したメッセージ
     * 派生元実行時ID
     * </pre>
     * @param formattedLogItems フォーマット済みのログ出力項目
     * @param error エラー情報
     * @param data 処理対象データ
     * @param failureCode 障害コード
     * @param messageOptions 障害コードからメッセージを取得する際に使用するオプション情報
     * @return フォーマット後のメッセージ
     */
    protected String format(LogItem<FailureLogContext>[] formattedLogItems,
                             Throwable error, Object data,
                             String failureCode, Object[] messageOptions) {
        failureCode = getFailureCode(failureCode, error);
        FailureLogContext context = new FailureLogContext(failureCode, getMessage(failureCode, messageOptions, error), data);
        return LogUtil.formatMessage(formattedLogItems, context);
    }
    
    /**
     * 障害コードからメッセージを取得する。
     * <pre>
     * 障害コードがデフォルトの障害コードの場合は、デフォルトのメッセージを返す。
     * デフォルトのメッセージが指定されていない場合はブランクとなる。
     * 
     * メッセージ取得では、指定されたメッセージの言語を使用する。
     * メッセージの言語が指定されていない場合は、{@link ThreadContext#getLanguage()}を使用する。
     * 
     * メッセージ取得で例外が発生した場合は、下記の固定メッセージを返す。
     * 
     * "failed to get the message to output the failure log. failureCode = [障害コード]"
     * </pre>
     * @param failureCode 障害コード
     * @param options 障害コードからメッセージを取得する際に使用するオプション情報
     * @param error エラー情報
     * @return メッセージ
     * @see MessageUtil#createMessage(MessageLevel, String, Object...)
     */
    protected String getMessage(String failureCode, Object[] options, Throwable error) {
        if (defaultFailureCode.equals(failureCode)) {
            return defaultMessage;
        }
        try {
            Message message = MessageUtil.createMessage(MessageLevel.ERROR, failureCode, options);
            return message.formatMessage(getLocale());
            
        } catch (Throwable e) {
            LOGGER.logWarn("message not found. failureCode = [" + failureCode + "]", e);
            return "failed to get the message to output the failure log. failureCode = [" + failureCode + "]";
        }
    }

    /**
     * 言語情報を取得する。
     * @return 言語情報
     */
    private Locale getLocale() {
        if (locale != null) {
            return locale;
        }
        return ThreadContext.getLanguage() != null ? ThreadContext.getLanguage() : Locale.getDefault();
    }


    /**
     * ログ出力に使用する障害コードを取得する。
     * @param failureCode 出力依頼時に指定された障害コード
     * @param error エラー情報
     * @return ログ出力に使用する障害コード
     */
    protected String getFailureCode(String failureCode, Throwable error) {
        if (StringUtil.isNullOrEmpty(failureCode)) {
            failureCode = getFrameworkFailureCode(error);
        } else {
            if (appFailureCodes.containsKey(failureCode)) {
                failureCode = appFailureCodes.get(failureCode);
            }
        }
        return failureCode;
    }

    /**
     * フレームワーク用の障害コード変換情報から障害コードを取得する。
     * <pre>
     * 下記の場合はデフォルトの障害コードを返す。
     * ・エラー情報のスタックトレースのルート要素がフレームワークでない場合
     * ・フレームワーク用の障害コード変換情報から障害コードを取得できない場合
     * </pre>
     * @param error エラー情報
     * @return フレームワーク用の障害コード
     */
    protected String getFrameworkFailureCode(Throwable error) {
        String fwFailureCode = defaultFailureCode;
        StackTraceElement root = getRootExceptionPoint(error);
        if (root != null && root.getClassName().startsWith(FRAMEWORK_PACKAGE_PREFIX)) {
            String failureCode = findEntryValue(fwFailureCodes, root.getClassName());
            if (failureCode != null) {
                fwFailureCode = failureCode;
            }
        }
        return fwFailureCode;
    }
    
    /**
     * {@link Map.Entry}リストからキーのプレフィックス指定で値を検索する。
     * @param entries {@link Map.Entry}リスト
     * @param keyPrefix キーのプレフィックス
     * @return 値。見つからない場合はnull
     */
    static String findEntryValue(List<Map.Entry<String, String>> entries, String keyPrefix) {
        if (keyPrefix == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : entries) {
            if (keyPrefix.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * スタックトレースからルート要素を取得する。
     * @param error エラー情報
     * @return スタックトレースのルート要素。スタックトレースがない場合はnull
     */
    protected StackTraceElement getRootExceptionPoint(Throwable error) {
        if (error == null) {
            return null;
        }
        Throwable cause = error;
        while (true) {
            if (cause.getCause() == null) {
                break;
            }
            cause = cause.getCause();
        }
        StackTraceElement[] stackTrace = cause.getStackTrace();
        return stackTrace.length != 0 ? stackTrace[0] : null;
    }
    
    /**
     * 障害通知ログと障害解析ログの出力項目を保持するクラス。
     * @author Kiyohito Itoh
     */
    @Published(tag = "architect")
    public static class FailureLogContext {
        /** 障害コード */
        private String failureCode;
        /** メッセージ */
        private String message;
        /** データ */
        private Object data;
        /**
         * コンストラクタ。
         * @param failureCode 障害コード
         * @param message メッセージ
         * @param data データ
         */
        public FailureLogContext(String failureCode, String message, Object data) {
            this.failureCode = failureCode;
            this.message = message;
            this.data = data;
        }
        /**
         * 障害コードを取得する。
         * @return 障害コード
         */
        public String getFailureCode() {
            return failureCode;
        }
        /**
         * メッセージを取得する。
         * @return メッセージ
         */
        public String getMessage() {
            return message;
        }
        /**
         * データを取得する。
         * @return データ
         */
        public Object getData() {
            return data;
        }
    }
    
    /**
     * 障害コードを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class FailureCodeItem implements LogItem<FailureLogContext> {
        /**
         * 障害コードを取得する。
         * @param context {@link FailureLogContext}
         * @return 障害コード
         */
        public String get(FailureLogContext context) {
            return context.getFailureCode();
        }
    }

    /**
     * メッセージを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class MessageItem implements LogItem<FailureLogContext> {
        /**
         * メッセージを取得する。
         * @param context {@link FailureLogContext}
         * @return メッセージ
         */
        public String get(FailureLogContext context) {
            return context.getMessage();
        }
    }
    
    /**
     * 処理対象データを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class DataItem implements LogItem<FailureLogContext> {
        
        /**
         * 処理対象データを取得する。
         * @param context {@link FailureLogContext}
         * @return 処理対象データ
         */
        public String get(FailureLogContext context) {
            Object data = context.getData();
            return data != null ? data.toString() : null;
        }
    }
    
    /**
     * 連絡先を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ContactItem implements LogItem<FailureLogContext> {
        
        /** 連絡先 */
        private List<Map.Entry<String, String>> contacts;
        
        /**
         * 連絡先を設定する。
         * @param contacts 連絡先
         */
        public void setContacts(List<Map.Entry<String, String>> contacts) {
            this.contacts = contacts;
        }
        
        /**
         * 連絡先を取得する。
         * @param context {@link FailureLogContext}
         * @return 連絡先
         */
        public String get(FailureLogContext context) {
            String requestId = ThreadContext.getRequestId();
            return findEntryValue(contacts, requestId);
        }
    }
}
