package nablarch.core.log.app;

import nablarch.core.ThreadContext;
import nablarch.core.log.DateItemSupport;
import nablarch.core.log.LogItem;
import nablarch.core.log.LogUtil;
import nablarch.core.util.annotation.Published;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * パフォーマンスログのメッセージをフォーマットするクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class PerformanceLogFormatter {
    
    /** デフォルトの日時フォーマット */
    private static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /** デフォルトのフォーマット */
    private static final String DEFAULT_FORMAT = "\n\tpoint = [$point$] result = [$result$]"
                                                  + "\n\tstart_time = [$startTime$] end_time = [$endTime$]"
                                                  + "\n\texecution_time = [$executionTime$]"
                                                  + "\n\tmax_memory = [$maxMemory$]"
                                                  + "\n\tstart_free_memory = [$startFreeMemory$] start_used_memory = [$startUsedMemory$]"
                                                  + "\n\tend_free_memory = [$endFreeMemory$] end_used_memory = [$endUsedMemory$]";
    
    /** プロパティ名のプレフィックス */
    public static final String PROPS_PREFIX = "performanceLogFormatter.";
    
    /** 出力対象のポイントを取得する際に使用するプロパティ名 */
    private static final String PROPS_TARGET_POINTS = PROPS_PREFIX + "targetPoints";
    
    /** 開始日時と終了日時のフォーマットに使用する日時パターンを取得する際に使用するプロパティ名 */
    private static final String PROPS_DATE_PATTERN = PROPS_PREFIX + "datePattern";
    
    /** フォーマットを取得する際に使用するプロパティ名 */
    private static final String PROPS_FORMAT = PROPS_PREFIX + "format";
    
    /** 出力対象のポイント */
    private Set<String> targetPoints;
    
    /** フォーマット済みのログ出力項目 */
    private LogItem<PerformanceLogContext>[] formattedLogItems;

    /** 出力対象にメモリ項目が含まれているか否か。 */
    private boolean containsMemoryItem;

    /** コンテキストマップ */
    private final ThreadLocal<Map<String, PerformanceLogContext>> contextMap = new ThreadLocal<Map<String, PerformanceLogContext>>() {
        @Override
        protected Map<String, PerformanceLogContext> initialValue() {
            return new HashMap<String, PerformanceLogContext>();
        }
    };
    
    /**
     * フォーマット済みのログ出力項目を初期化する。
     */
    public PerformanceLogFormatter() {
        initialize();
    }

    /**
     * 初期化
     */
    protected void initialize() {
        Map<String, String> props = AppLogUtil.getProps();
        initializeTargetPoints(props);
        initializeFormat(props);
    }

    /**
     * 出力対象のポイントを初期化
     * @param props 各種ログ出力の設定情報
     */
    protected void initializeTargetPoints(Map<String, String> props) {
        if (props.containsKey(PROPS_TARGET_POINTS)) {
            targetPoints = new HashSet<String>();
            for (String point : props.get(PROPS_TARGET_POINTS).split(",")) {
                targetPoints.add(point.trim());
            }
        } else {
            targetPoints = Collections.emptySet();
        }
    }

    /**
     * フォーマットの初期化
     * @param props 各種ログ出力の設定情報
     */
    protected void initializeFormat(Map<String, String> props) {
        DateFormat dateFormat = DEFAULT_DATE_FORMAT;
        if (props.containsKey(PROPS_DATE_PATTERN)) {
            dateFormat = new SimpleDateFormat(props.get(PROPS_DATE_PATTERN));
        }

        String format = DEFAULT_FORMAT;
        if (props.containsKey(PROPS_FORMAT)) {
            format = props.get(PROPS_FORMAT);
        }

        Map<String, LogItem<PerformanceLogContext>> logItems = getLogItems(dateFormat);
        formattedLogItems = LogUtil.createFormattedLogItems(logItems, format);
        setContainsMemoryItem(LogUtil.contains(formattedLogItems, MaxMemoryItem.class,
                StartFreeMemoryItem.class, EndFreeMemoryItem.class, StartUsedMemoryItem.class, EndUsedMemoryItem.class));
    }

    /**
     * 出力対象にメモリ項目が含まれているか否かを設定する。
     * @param containsMemoryItem 出力対象にメモリ項目が含まれているときtrue
     */
    protected void setContainsMemoryItem(boolean containsMemoryItem) {
        this.containsMemoryItem = containsMemoryItem;
    }

    /**
     * フォーマット対象のログ出力項目を取得する。
     * @param dateFormat 開始日時と終了日時のフォーマットに使用する日時フォーマット
     * @return フォーマット対象のログ出力項目
     */
    protected Map<String, LogItem<PerformanceLogContext>> getLogItems(DateFormat dateFormat) {
        Map<String, LogItem<PerformanceLogContext>> logItems = new HashMap<String, LogItem<PerformanceLogContext>>();
        logItems.put("$point$", new PointItem());
        logItems.put("$result$", new ResultItem());
        logItems.put("$startTime$", new StartTimeItem(dateFormat));
        logItems.put("$endTime$", new EndTimeItem(dateFormat));
        logItems.put("$executionTime$", new ExecutionTimeItem());
        logItems.put("$maxMemory$", new MaxMemoryItem());
        logItems.put("$startFreeMemory$", new StartFreeMemoryItem());
        logItems.put("$endFreeMemory$", new EndFreeMemoryItem());
        logItems.put("$startUsedMemory$", new StartUsedMemoryItem());
        logItems.put("$endUsedMemory$", new EndUsedMemoryItem());
        return logItems;
    }
    
    /**
     * 測定対象であるかを判定する。
     * @param point 測定対象を識別するID
     * @return 測定対象の場合はtrue
     */
    public boolean isTargetPoint(String point) {
        return targetPoints.contains(point);
    }
    
    /**
     * 測定を開始する。
     * @param point 測定対象を識別するID
     */
    public void start(String point) {
        
        String contextId = ThreadContext.getExecutionId() + point;
        PerformanceLogContext context = new PerformanceLogContext();
        contextMap.get().put(contextId, context);
        
        context.setPoint(point);
        
        if (containsMemoryItem) {
            MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemory = memory.getHeapMemoryUsage();
            long max = heapMemory.getMax();
            long used = heapMemory.getUsed();
            long free = max - used;
            context.setMaxMemory(max);
            context.setStartUsedMemory(used);
            context.setStartFreeMemory(free);
        }
        
        context.setStartTime(System.currentTimeMillis());
    }
    
    /**
     * 測定を終了し、パフォーマンスログのメッセージをフォーマットする。
     * @param point 測定対象を識別するID
     * @param result 処理結果を表す文字列
     * @return フォーマット済みのメッセージ
     */
    public String end(String point, String result) {
        
        String contextId = ThreadContext.getExecutionId() + point;
        Map<String, PerformanceLogContext> localMap = contextMap.get();
        PerformanceLogContext context = localMap.remove(contextId);
        if (localMap.isEmpty()) {
            contextMap.remove();
        }
        if (context == null) {
            throw new IllegalStateException(
                String.format("PerformanceLogContext was not found. point = [%s], execution id = [%s]",
                              point, ThreadContext.getExecutionId()));
        }
        
        context.setEndTime(System.currentTimeMillis());
        
        if (containsMemoryItem) {
            MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemory = memory.getHeapMemoryUsage();
            long max = heapMemory.getMax();
            long used = heapMemory.getUsed();
            long free = max - used;
            context.setEndUsedMemory(used);
            context.setEndFreeMemory(free);
        }
        
        context.setResult(result);
        
        return formatMessage(context);
    }

    /**
     * パフォーマンスログのメッセージをフォーマットする。
     * @param context パフォーマンスログのコンテキスト情報
     * @return フォーマット済みのメッセージ
     */
    protected String formatMessage(PerformanceLogContext context) {
        return LogUtil.formatMessage(formattedLogItems, context);
    }
    
    /**
     * パフォーマンスログのコンテキスト情報を保持するクラス。
     * @author Kiyohito Itoh
     */
    @Published(tag = "architect")
    public static class PerformanceLogContext {
        /** 測定対象を識別するID */
        private String point;
        /** 処理結果 */
        private String result;
        /** 開始日時 */
        private long startTime;
        /** 終了日時 */
        private long endTime;
        /** 最大メモリ量(開始時) */
        private long maxMemory;
        /** 空きメモリ量(開始時) */
        private long startFreeMemory;
        /** 空きメモリ量(終了時) */
        private long endFreeMemory;
        /** 使用メモリ量(開始時) */
        private long startUsedMemory;
        /** 使用メモリ量(終了時) */
        private long endUsedMemory;
        /**
         * 測定対象を識別するIDを取得する。
         * @return 測定対象を識別するID
         */
        public String getPoint() {
            return point;
        }
        /**
         * 測定対象を識別するIDを設定する。
         * @param point 測定対象を識別するID
         */
        public void setPoint(String point) {
            this.point = point;
        }
        /**
         * 処理結果を取得する。
         * @return 処理結果
         */
        public String getResult() {
            return result;
        }
        /**
         * 処理結果を設定する。
         * @param result 処理結果
         */
        public void setResult(String result) {
            this.result = result;
        }
        /**
         * 開始日時を取得する。
         * @return 開始日時
         */
        public long getStartTime() {
            return startTime;
        }
        /**
         * 開始日時を設定する。
         * @param startTime 開始日時
         */
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
        /**
         * 終了日時を取得する。
         * @return 終了日時
         */
        public long getEndTime() {
            return endTime;
        }
        /**
         * 終了日時を設定する。
         * @param endTime 終了日時
         */
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
        /**
         * 実行時間を取得する。
         * @return 実行時間
         */
        public long getExecutionTime() {
            return endTime - startTime;
        }
        /**
         * 最大メモリ量(開始時)を取得する。
         * @return 最大メモリ量(開始時)
         */
        public long getMaxMemory() {
            return maxMemory;
        }
        /**
         * 最大メモリ量(開始時)を設定する。
         * @param maxMemory 最大メモリ量(開始時)
         */
        public void setMaxMemory(long maxMemory) {
            this.maxMemory = maxMemory;
        }
        /**
         * 空きメモリ量(開始時)を取得する。
         * @return 空きメモリ量(開始時)
         */
        public long getStartFreeMemory() {
            return startFreeMemory;
        }
        /**
         * 空きメモリ量(開始時)を設定する。
         * @param startFreeMemory 空きメモリ量(開始時)
         */
        public void setStartFreeMemory(long startFreeMemory) {
            this.startFreeMemory = startFreeMemory;
        }
        /**
         * 空きメモリ量(終了時)を取得する。
         * @return 空きメモリ量(終了時)
         */
        public long getEndFreeMemory() {
            return endFreeMemory;
        }
        /**
         * 空きメモリ量(終了時)を設定する。
         * @param endFreeMemory 空きメモリ量(終了時)
         */
        public void setEndFreeMemory(long endFreeMemory) {
            this.endFreeMemory = endFreeMemory;
        }
        /**
         * 使用メモリ量(開始時)を取得する。
         * @return 使用メモリ量(開始時)
         */
        public long getStartUsedMemory() {
            return startUsedMemory;
        }
        /**
         * 使用メモリ量(開始時)を設定する。
         * @param startUsedMemory 使用メモリ量(開始時)
         */
        public void setStartUsedMemory(long startUsedMemory) {
            this.startUsedMemory = startUsedMemory;
        }
        /**
         * 使用メモリ量(終了時)を取得する。
         * @return 使用メモリ量(終了時)
         */
        public long getEndUsedMemory() {
            return endUsedMemory;
        }
        /**
         * 使用メモリ量(終了時)を設定する。
         * @param endUsedMemory 使用メモリ量(終了時)
         */
        public void setEndUsedMemory(long endUsedMemory) {
            this.endUsedMemory = endUsedMemory;
        }
    }
    /**
     * ポイントを取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class PointItem implements LogItem<PerformanceLogContext> {
        /**
         * ポイントを取得する。
         * @param context {@link PerformanceLogContext}
         * @return ポイント
         */
        public String get(PerformanceLogContext context) {
            return context.getPoint();
        }
    }
    /**
     * 処理結果を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ResultItem implements LogItem<PerformanceLogContext> {
        /**
         * 処理結果を取得する。
         * @param context {@link PerformanceLogContext}
         * @return 処理結果
         */
        public String get(PerformanceLogContext context) {
            return context.getResult();
        }
    }
    /**
     * 開始日時を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class StartTimeItem extends DateItemSupport<PerformanceLogContext> {
        /**
         * コンストラクタ。
         * @param dateFormat 日時フォーマット
         */
        public StartTimeItem(DateFormat dateFormat) {
            super(dateFormat);
        }
        /** {@inheritDoc} */
        protected Date getDate(PerformanceLogContext context) {
            return new Date(context.getStartTime());
        }
    }
    /**
     * 終了日時を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class EndTimeItem extends DateItemSupport<PerformanceLogContext> {
        /**
         * コンストラクタ。
         * @param dateFormat 日時フォーマット
         */
        public EndTimeItem(DateFormat dateFormat) {
            super(dateFormat);
        }
        /** {@inheritDoc} */
        protected Date getDate(PerformanceLogContext context) {
            return new Date(context.getEndTime());
        }
    }
    /**
     * 実行時間を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class ExecutionTimeItem implements LogItem<PerformanceLogContext> {
        /**
         * 実行時間を取得する。
         * @param context {@link PerformanceLogContext}
         * @return 実行時間
         */
        public String get(PerformanceLogContext context) {
            return String.valueOf(context.getExecutionTime());
        }
    }
    /**
     * 最大メモリ量を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class MaxMemoryItem implements LogItem<PerformanceLogContext> {
        /**
         * 最大メモリ量を取得する。
         * @param context {@link PerformanceLogContext}
         * @return 最大メモリ量
         */
        public String get(PerformanceLogContext context) {
            return String.valueOf(context.getMaxMemory());
        }
    }
    /**
     * 開始時の空きメモリ量を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class StartFreeMemoryItem implements LogItem<PerformanceLogContext> {
        /**
         * 開始時の空きメモリ量を取得する。
         * @param context {@link PerformanceLogContext}
         * @return 開始時の空きメモリ量
         */
        public String get(PerformanceLogContext context) {
            return String.valueOf(context.getStartFreeMemory());
        }
    }
    /**
     * 開始時の使用メモリ量を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class StartUsedMemoryItem implements LogItem<PerformanceLogContext> {
        /**
         * 開始時の使用メモリ量を取得する。
         * @param context {@link PerformanceLogContext}
         * @return 開始時の使用メモリ量
         */
        public String get(PerformanceLogContext context) {
            return String.valueOf(context.getStartUsedMemory());
        }
    }
    /**
     * 終了時の空きメモリ量を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class EndFreeMemoryItem implements LogItem<PerformanceLogContext> {
        /**
         * 終了時の空きメモリ量を取得する。
         * @param context {@link PerformanceLogContext}
         * @return 終了時の空きメモリ量
         */
        public String get(PerformanceLogContext context) {
            return String.valueOf(context.getEndFreeMemory());
        }
    }
    /**
     * 終了時の使用メモリ量を取得するクラス。
     * @author Kiyohito Itoh
     */
    public static class EndUsedMemoryItem implements LogItem<PerformanceLogContext> {
        /**
         * 終了時の使用メモリ量を取得する。
         * @param context {@link PerformanceLogContext}
         * @return 終了時の使用メモリ量
         */
        public String get(PerformanceLogContext context) {
            return String.valueOf(context.getEndUsedMemory());
        }
    }
}
