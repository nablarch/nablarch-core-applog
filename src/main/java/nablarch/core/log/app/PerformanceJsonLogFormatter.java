package nablarch.core.log.app;

import nablarch.core.log.basic.JsonLogObjectBuilder;
import nablarch.core.text.json.BasicJsonSerializationManager;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *パフォーマンスログのメッセージをJSON形式でフォーマットするクラス。
 * <p>
 * {@link PerformanceLogFormatter}では、フォーマットとして出力内容を設定するが、
 * 本クラスでは、 notificationTargets および、analysisTargets プロパティにて、
 * 出力項目を指定する。指定可能な出力項目は下記の通り。
 * <ul>
 * <li>point: ポイント</li>
 * <li>result: 処理結果</li>
 * <li>startTime: 開始日時</li>
 * <li>endTime: 終了日時</li>
 * <li>executionTime: 実行時間</li>
 * <li>maxMemory: 最大メモリ量</li>
 * <li>startFreeMemory: 開始時の空きメモリ量</li>
 * <li>endFreeMemory: 終了時の空きメモリ量</li>
 * <li>startUsedMemory: 開始時の使用メモリ量</li>
 * <li>endUsedMemory: 終了時の使用メモリ量</li>
 * </ul>
 * </p>
 * @author Shuji Kitamura
 */
@Published(tag = "architect")
public class PerformanceJsonLogFormatter extends PerformanceLogFormatter {

    /** ポイントの項目名 */
    private static final String TARGET_NAME_POINT = "point";
    /** 処理結果の項目名 */
    private static final String TARGET_NAME_RESULT = "result";
    /** 開始日時の項目名 */
    private static final String TARGET_NAME_START_TIME = "startTime";
    /** 終了日時の項目名 */
    private static final String TARGET_NAME_END_TIME = "endTime";
    /** 実行時間の項目名 */
    private static final String TARGET_NAME_EXECUTION_TIME = "executionTime";
    /** 最大メモリ量の項目名 */
    private static final String TARGET_NAME_MAX_MEMORY = "maxMemory";
    /** 開始時の空きメモリ量の項目名 */
    private static final String TARGET_NAME_START_FREE_MEMORY = "startFreeMemory";
    /** 終了時の空きメモリ量の項目名 */
    private static final String TARGET_NAME_END_FREE_MEMORY = "endFreeMemory";
    /** 開始時の使用メモリ量の項目名 */
    private static final String TARGET_NAME_START_USED_MEMORY = "startUsedMemory";
    /** 終了時の使用メモリ量の項目名 */
    private static final String TARGET_NAME_END_USED_MEMORY = "endUsedMemory";

    /** 出力項目のプロパティ名 */
    private static final String PROPS_TARGETS = PROPS_PREFIX + "targets";

    /** フォーマット指定が無い場合に使用する出力項目のデフォルト値 */
    private static final String DEFAULT_TARGETS = "point,result,startTime,endTime,"
            + "executionTime,maxMemory,startFreeMemory,startUsedMemory,endFreeMemory,endUsedMemory";

    /**
     * ターゲット名と {@link JsonLogObjectBuilder}の対応を定義したマップ。
     */
    private static final Map<String, JsonLogObjectBuilder<PerformanceLogContext>> TARGET_BUILDERS_MAP;

    static {
        Map<String, JsonLogObjectBuilder<PerformanceLogContext>> map = new HashMap<String, JsonLogObjectBuilder<PerformanceLogContext>>();
        map.put(TARGET_NAME_POINT, new PerformanceJsonLogFormatter.PointBuilder());
        map.put(TARGET_NAME_RESULT, new PerformanceJsonLogFormatter.ResultBuilder());
        map.put(TARGET_NAME_START_TIME, new PerformanceJsonLogFormatter.StartTimeBuilder());
        map.put(TARGET_NAME_END_TIME, new PerformanceJsonLogFormatter.EndTimeBuilder());
        map.put(TARGET_NAME_EXECUTION_TIME, new PerformanceJsonLogFormatter.ExecutionTimeBuilder());
        map.put(TARGET_NAME_MAX_MEMORY, new PerformanceJsonLogFormatter.MaxMemoryBuilder());
        map.put(TARGET_NAME_START_FREE_MEMORY, new PerformanceJsonLogFormatter.StartFreeMemoryBuilder());
        map.put(TARGET_NAME_END_FREE_MEMORY, new PerformanceJsonLogFormatter.EndFreeMemoryBuilder());
        map.put(TARGET_NAME_START_USED_MEMORY, new PerformanceJsonLogFormatter.StartUsedMemoryBuilder());
        map.put(TARGET_NAME_END_USED_MEMORY, new PerformanceJsonLogFormatter.EndUsedMemoryBuilder());

        TARGET_BUILDERS_MAP = Collections.unmodifiableMap(map);
    }

    /** ログ出力項目 */
    private List<JsonLogObjectBuilder<PerformanceLogContext>> structuredTargets;

    /** 各種ログのJSONフォーマット支援オブジェクト */
    private JsonLogFormatterSupport support;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        Map<String, String> props = AppLogUtil.getProps();
        initializeTargetPoints(props);
        initializeFormatterSupport(props, PROPS_PREFIX, AppLogUtil.getFilePath());
        initializeTargets(props);
    }

    /**
     * 各種ログのJSONフォーマット支援オブジェクトの初期化
     * @param props 各種ログ出力の設定情報
     */
    protected final void initializeFormatterSupport(Map<String, String> props, String prefix, String filePath) {
        JsonSerializationSettings settings = new JsonSerializationSettings(props, prefix, filePath);
        JsonSerializationManager serializationManager = createSerializationManager(settings);
        support = new JsonLogFormatterSupport(serializationManager, settings);
    }

    /**
     * 変換処理に使用する{@link JsonSerializationManager}を生成する。
     * @param settings 各種ログ出力の設定情報
     * @return {@link JsonSerializationManager}
     */
    protected JsonSerializationManager createSerializationManager(JsonSerializationSettings settings) {
        return new BasicJsonSerializationManager();
    }

    /**
     * 出力項目の初期化
     * @param props 各種ログ出力の設定情報
     */
    protected final void initializeTargets(Map<String, String> props) {
        structuredTargets = getStructuredTargets(props);
    }

    /**
     * ログ出力項目を取得する。
     * @param props 各種ログ出力の設定情報
     * @return ログ出力項目
     */
    protected List<JsonLogObjectBuilder<PerformanceLogContext>> getStructuredTargets(
            Map<String, String> props) {

        String targetsStr = props.get(PROPS_TARGETS);
        if (StringUtil.isNullOrEmpty(targetsStr)) {
            targetsStr = DEFAULT_TARGETS;
        }

        List<JsonLogObjectBuilder<PerformanceLogContext>> targetBuilders
                = new ArrayList<JsonLogObjectBuilder<PerformanceLogContext>>();

        String[] targets = targetsStr.split(",");
        Set<String> keys = new HashSet<String>(targets.length);
        for (String target: targets) {
            String key = target.trim();
            if (!StringUtil.isNullOrEmpty(key) && !keys.contains(key)) {
                keys.add(key);

                if (!TARGET_BUILDERS_MAP.containsKey(key)) {
                    throw new IllegalArgumentException(
                            String.format("[%s] is unknown target. property name = [%s]", key, PROPS_TARGETS));
                }

                targetBuilders.add(TARGET_BUILDERS_MAP.get(key));
            }
        }

        boolean hasMemoryItem = containsAnyOfMemoryTarget(keys);
        setContainsMemoryItem(hasMemoryItem);

        return targetBuilders;
    }

    /**
     * メモリ関係のターゲットが1つ以上含まれているかどうか判定する。
     * @param targets ターゲットのセット
     * @return メモリ関係のターゲットが1つ以上含まれている場合は true
     */
    private boolean containsAnyOfMemoryTarget(Set<String> targets) {
        for (String target : targets) {
            if (isAnyOfMemoryTarget(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定された target がメモリに関するものかどうか判定する。
     * @param target 判定対象の target
     * @return メモリ関係の target の場合は true
     */
    private boolean isAnyOfMemoryTarget(String target) {
        return TARGET_NAME_MAX_MEMORY.equals(target)
                || TARGET_NAME_START_FREE_MEMORY.equals(target)
                || TARGET_NAME_END_FREE_MEMORY.equals(target)
                || TARGET_NAME_START_USED_MEMORY.equals(target)
                || TARGET_NAME_END_USED_MEMORY.equals(target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String formatMessage(PerformanceLogContext context) {
        return support.getStructuredMessage(structuredTargets, context);
    }

    /**
     * ポイントを処理するクラス。
     * @author Shuji Kitamura
     */
    public static class PointBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_POINT, context.getPoint());
        }
    }

    /**
     * 処理結果を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class ResultBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_RESULT, context.getResult());
        }
    }

    /**
     * 開始日時を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class StartTimeBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_START_TIME, new Date(context.getStartTime()));
        }
    }

    /**
     * 終了日時を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class EndTimeBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_END_TIME, new Date(context.getEndTime()));
        }
    }

    /**
     * 実行時間を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class ExecutionTimeBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_EXECUTION_TIME, context.getExecutionTime());
        }
    }

    /**
     * 最大メモリ量を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class MaxMemoryBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_MAX_MEMORY, context.getMaxMemory());
        }
    }

    /**
     * 開始時の空きメモリ量を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class StartFreeMemoryBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_START_FREE_MEMORY, context.getStartFreeMemory());
        }
    }

    /**
     * 終了時の空きメモリ量を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class EndFreeMemoryBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_END_FREE_MEMORY, context.getEndFreeMemory());
        }
    }

    /**
     * 開始時の使用メモリ量を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class StartUsedMemoryBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_START_USED_MEMORY, context.getStartUsedMemory());
        }
    }

    /**
     * 終了時の使用メモリ量を処理するクラス。
     * @author Shuji Kitamura
     */
    public static class EndUsedMemoryBuilder implements JsonLogObjectBuilder<PerformanceLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, PerformanceLogContext context) {
            structuredObject.put(TARGET_NAME_END_USED_MEMORY, context.getEndUsedMemory());
        }
    }

}
