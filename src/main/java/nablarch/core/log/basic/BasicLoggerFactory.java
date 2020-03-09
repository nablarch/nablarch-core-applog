package nablarch.core.log.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import nablarch.core.log.LogSettings;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerFactory;
import nablarch.core.util.ObjectUtil;

/**
 * {@link LoggerFactory}の基本実装クラス。<br>
 * <br>
 * フレームワーク実装の設定は、{@link nablarch.core.log.LoggerManager LoggerManager}が読み込むプロパティファイルに記述する。<br>
 * プロパティファイルの設定は、システムプロパティを使用して同じキー名に値を指定することで上書きすることができる。<br>
 * <br>
 * プロパティファイルの記述ルールを下記に示す。<br>
 * <dl>
 * <dt>writerNames
 * <dd>使用する全ての{@link LogWriter}の名称。必須。<br>
 *     複数指定する場合はカンマ区切り。<br>
 *     「”writer.” + &lt;ここで指定した{@link LogWriter}の名称&gt;」をキーのプレフィックスにして、{@link LogWriter}毎の設定を行う。
 *     
 * <dt>writer.&lt;{@link LogWriter}の名称&gt;.className
 * <dd>{@link LogWriter}のクラス名。必須。<br>
 *     {@link LogWriter}を実装したクラスのFQCNを指定する。
 *     
 * <dt>writer.&lt;{@link LogWriter}の名称&gt;.<プロパティ名>
 * <dd>{@link LogWriter}毎のプロパティに設定する値。<br>
 *     設定内容は、使用する{@link LogWriter}のJavadocを参照すること。
 *     
 * <dt>availableLoggersNamesOrder
 * <dd>使用する全ての{@link Logger}設定の名称。必須。<br>
 *     複数指定する場合はカンマ区切り。<br>
 *     「”loggers.” + &lt;ここで指定された{@link Logger}設定の名称&gt;」をキーのプレフィックスに使用して、{@link Logger}設定毎の設定を行う。
 *     
 * <dt>loggers.&lt;{@link Logger}設定の名称&gt;.nameRegex
 * <dd>{@link Logger}名とのマッチングに使用する正規表現。必須。<br>
 *     正規表現は、{@link Logger}設定の対象となる{@link Logger}を絞り込むために使用する。<br>
 *     {@link Logger}の取得時に指定された{@link Logger}名
 *     （つまり{@link nablarch.core.log.LoggerManager#get(String) LoggerManager#get}メソッドの引数に指定された{@link Logger}名）に対してマッチングを行う。
 *     
 * <dt>logger.&lt;{@link Logger}設定の名称&gt;.level
 * <dd>{@link LogLevel}の名称。必須。<br>
 *     {@link LogLevel}の名称を指定する。<br>
 *     ここで指定したレベル以上のログを全て出力する。
 *     
 * <dt>logger.&lt;{@link Logger}設定の名称&gt;.writerNames
 * <dd>{@link LogWriter}の名称。必須。<br>
 *     複数指定する場合はカンマ区切り。<br>
 *     ここで指定した全ての{@link LogWriter}に対してログの書き込みを行う。
 * </dl>
 * availableLoggersNamesOrderプロパティは、記述順に意味があるので注意すること。<br>
 * {@link Logger}の取得では、ログ出力を行うクラスが指定した{@link Logger}名に対して、
 * ここに記述した順番で{@link Logger}のマッチングを行い、最初にマッチした{@link Logger}を返す。<br>
 * そのため、availableLoggersNamesOrderプロパティは、より限定的な正規表現を指定した{@link Logger}から順に記述すること。<br>
 * <br>
 * 初期処理完了後に、各{@link LogWriter}に対して、出力されるログレベルの書き込みを行う。<br>
 * 初期処理完了後の出力例を下記に示す。
 * <pre>
 * 2010-09-14 15:26:32.345 nablarch.core.log.basic.BasicLoggerFactory INFO [main] user_id[null] request_id[null] initialized.
 *      NAME REGEX = [MONITOR] LEVEL = [ERROR]
 *      NAME REGEX = [tis\.w8\.web\.handler\.HttpAccessLogHandler] LEVEL = [INFO]
 *      NAME REGEX = [.*] LEVEL = [WARN]
 * </pre>
 * @author Kiyohito Itoh
 */
public class BasicLoggerFactory implements LoggerFactory {
    
    /** 何も処理しない{@link Logger} */
    private static final Logger NULL_LOGGER = new BasicLogger("null");
    
    /** 設定で指定された全ての{@link Logger}定義 */
    private List<LoggerDefinition> loggerDefinitions;
    
    /** 設定で指定された全ての{@link LogWriter} */
    private Map<String, LogWriter> writers;
    
    /**
     * {@inheritDoc}<br>
     * <br>
     * ログ出力の設定に応じて、インスタンスの生成と初期化を行う。<br>
     * 設定に不備がある場合は{@link IllegalArgumentException}をスローする。<br>
     * <br>
     * 初期処理完了後に、各{@link LogWriter}に対して、出力されるログレベルの書き込みを行う。
     */
    public void initialize(LogSettings settings) {
        writers = createWriters(settings);
        loggerDefinitions = createLoggerDefinitions(settings);
        assertLoggerDefinitionMatching(settings);
        writeLoggerSettings();
    }
    
    /**
     * 使用可能なロガー設定と、全てのロガー設定が一致するか検証する。<br>
     * 一致しない場合は{@link IllegalArgumentException}を送出する。<br>
     * この検証は、設定ミスを防ぐために設けている。
     * @param settings ログ出力の設定
     */
    private void assertLoggerDefinitionMatching(LogSettings settings) {
        String prefix = "loggers";
        Set<String> eachLoggerNames = new HashSet<String>();
        for (Map.Entry<String, String> entry : settings.getProps().entrySet()) {
            String[] splitKey = entry.getKey().split("\\.");
            if (!prefix.equals(splitKey[0])) {
                continue;
            }
            eachLoggerNames.add(splitKey[1]);
        }
        List<String> defNames = new ArrayList<String>();
        for (LoggerDefinition def : loggerDefinitions) {
            String defName = def.getName();
            defNames.add(defName);
            eachLoggerNames.remove(defName);
        }
        if (!eachLoggerNames.isEmpty()) {
            throw new IllegalArgumentException(
                String.format(
                    "availableLoggersNamesOrder and loggers.* was a mismatch. unused loggers.* should be comment out."
                        + " availableLoggersNamesOrder = [%s], unused loggers.* = [%s]",
                    defNames, eachLoggerNames));
        }
    }
    
    /**
     * {@link LogWriter}毎に、自身に設定されているロガー設定を出力する。<br>
     * 設定情報のフォーマットを下記に示す。<br>
     * <br>
     * LOGGER = [&lt;{@link Logger}名&gt;] NAME REGEX = [&lt;{@link Logger}名に対するマッチングに使用する正規表現&gt;] LEVEL = [&lt;ログの出力制御の基準とする{@link LogLevel}&gt;]
     */
    private void writeLoggerSettings() {
        for (LogWriter writer : writers.values()) {
            StringBuilder sb = new StringBuilder(512);
            sb.append("initialized.").append(Logger.LS);
            for (LoggerDefinition definition : loggerDefinitions) {
                if (definition.getWriters().contains(writer)) {
                    sb.append("\tLOGGER = [").append(definition.getName()).append("]")
                      .append(" NAME REGEX = [").append(definition.getNameRegex()).append("]")
                      .append(" LEVEL = [").append(definition.getBaseLevel().name()).append("]")
                      .append(Logger.LS);
                }
            }
            String name = BasicLoggerFactory.class.getName();
            try {
                writer.write(new LogContext(name, LogLevel.INFO, sb.toString(), null));
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }
    
    /**
     * 設定で指定された全ての{@link LogWriter}の生成と初期化を行う。
     * @param settings ログ出力の設定内容
     * @return 設定で指定された全ての{@link LogWriter}
     */
    private Map<String, LogWriter> createWriters(LogSettings settings) {
        String[] writerNames = settings.getRequiredProp("writerNames").split(",");
        Map<String, LogWriter> writers = new HashMap<String, LogWriter>((int) (writerNames.length * 1.5));
        for (String splitName : writerNames) {
            String writerName = splitName.trim();
            if (writerName.length() == 0) {
                throw new IllegalArgumentException(
                    String.format("blank was included in the comma-separated value. file path = [%s], key = [writerNames]", settings.getFilePath()));
            }
            writers.put(writerName, createLogWriter(new ObjectSettings(settings, "writer." + writerName)));
        }
        return writers;
    }
    
    /**
     * 設定で指定された全ての{@link Logger}定義を生成する。
     * @param settings ログ出力の設定内容
     * @return 設定で指定された全ての{@link Logger}定義
     */
    private List<LoggerDefinition> createLoggerDefinitions(LogSettings settings) {
        String[] loggerNames = settings.getRequiredProp("availableLoggersNamesOrder").split(",");
        List<LoggerDefinition> loggers = new ArrayList<LoggerDefinition>(loggerNames.length);
        for (String splitName : loggerNames) {
            String loggerName = splitName.trim();
            if (loggerName.length() == 0) {
                throw new IllegalArgumentException(
                    String.format("blank was included in the comma-separated value. file path = [%s], key = [availableLoggersNamesOrder]",
                                  settings.getFilePath()));
            }
            loggers.add(createLoggerDefinition(loggerName, new ObjectSettings(settings, "loggers." + loggerName)));
        }
        return loggers;
    }
    
    /**
     * {@inheritDoc}<br>
     * <br>
     * 全ての{@link LogWriter}の終了処理を行う。<br>
     * {@link LogWriter}の終了処理で例外が発生した場合は、発生した例外をキャッチし、標準エラーにスタックトレースを出力する。<br>
     * 発生した例外の再スローは行わない。
     */
    public void terminate() {
        if (loggerDefinitions != null) {
            loggerDefinitions.clear();
        }
        if (writers != null) {
            LogWriter[] tmpWriters = writers.values().toArray(new LogWriter[writers.values().size()]);
            writers.clear();
            for (LogWriter writer : tmpWriters) {
                try {
                    writer.terminate();
                } catch (Throwable t) {
                    t.printStackTrace(System.err);
                }
            }
        }
    }
    
    /**
     * {@inheritDoc}<br>
     * <br>
     * availableLoggersNamesOrderプロパティで指定された順番に{@link Logger}名のマッチングを行い、最初にマッチした{@link Logger}を返す。<br>
     * マッチする{@link Logger}が見つからない場合は、何もしない{@link Logger}を返す。
     */
    public Logger get(String name) {
        if (loggerDefinitions == null) {
            throw new IllegalStateException("not initialized.");
        }
        for (LoggerDefinition loggerDefinition : loggerDefinitions) {
            if (loggerDefinition.matches(name)) {
                return loggerDefinition.getLogger(name);
            }
        }
        return NULL_LOGGER;
    }
    
    /**
     * 設定を使用して{@link LogWriter}を生成する。
     * @param settings {@link LogWriter}の設定
     * @return 設定を使用して生成した{@link LogWriter}
     */
    private LogWriter createLogWriter(ObjectSettings settings) {
        LogWriter writer = ObjectUtil.createInstance(settings.getRequiredProp("className"));
        writer.initialize(settings);
        return writer;
    }
    
    /**
     * 設定を使用して{@link Logger}定義を生成する。
     * @param name ロガー設定の名称
     * @param settings {@link Logger}定義の設定
     * @return 設定を使用して生成した{@link Logger}定義
     */
    private LoggerDefinition createLoggerDefinition(String name, ObjectSettings settings) {
        return new LoggerDefinition(name,
                                     settings.getRequiredProp("nameRegex"),
                                     LogLevel.valueOf(settings.getRequiredProp("level")),
                                     getLogWriters(settings));
    }
    
    /**
     * {@link Logger}定義に指定された{@link LogWriter}を取得する。
     * @param settings {@link Logger}定義の設定
     * @return {@link Logger}に指定された{@link LogWriter}
     */
    private List<LogWriter> getLogWriters(ObjectSettings settings) {
        String[] writerNames = settings.getRequiredProp("writerNames").split(",");
        List<LogWriter> specifiedWriters = new ArrayList<LogWriter>(writerNames.length);
        for (String splitName : writerNames) {
            String writerName = splitName.trim();
            if (writerName.length() == 0) {
                throw new IllegalArgumentException(
                    String.format("blank was included in the comma-separated value. file path = [%s], key = [%s]",
                                  settings.getFilePath(), settings.getName() + ".writerNames"));
            }
            LogWriter writer = writers.get(writerName);
            if (writer == null) {
                throw new IllegalArgumentException(
                    String.format("the writer was not found. file path = [%s], logger name = [%s], writer name = [%s]",
                                  settings.getFilePath(), settings.getName(), writerName));
            }
            specifiedWriters.add(writer);
        }
        return specifiedWriters;
    }
    
    /**
     * {@link Logger}定義を保持するクラス。
     * @author Kiyohito Itoh
     */
    private static final class LoggerDefinition {
        
        /** ロガー名 */
        private String name;

        /** {@link Logger}名に対するマッチングに使用する正規表現 */
        private String nameRegex;
        
        /** {@link Logger}名に対するマッチングに使用するパターン */
        private Pattern pattern;
        
        /** ログの出力制御の基準とする{@link LogLevel} */
        private LogLevel baseLevel;
        
        /** ログの出力先となる{@link LogWriter} */
        private List<LogWriter> writers;
        
        /** {@link Logger} */
        private Logger logger;
        
        /**
         * コンストラクタ。
         * @param name ロガー設定の名称
         * @param nameRegex {@link Logger}名に対するマッチングに使用する正規表現
         * @param baseLevel ログの出力制御の基準とする{@link LogLevel}
         * @param writers ログの出力先となる{@link LogWriter}
         */
        private LoggerDefinition(String name, String nameRegex, LogLevel baseLevel, List<LogWriter> writers) {
            this.name = name;
            this.nameRegex = nameRegex;
            this.pattern = Pattern.compile(nameRegex);
            this.baseLevel = baseLevel;
            this.writers = writers;
            logger = new BasicLogger(name, baseLevel, writers.toArray(new LogWriter[writers.size()]));
        }
        
        /**
         * この{@link Logger}定義が指定された{@link Logger}名にマッチするか否かを判定する。
         * @param loggerName {@link Logger}名
         * @return 指定された{@link Logger}名にマッチする場合は<code>true</code>、マッチしない場合は<code>false</code>
         */
        private boolean matches(String loggerName) {
            return pattern.matcher(loggerName).matches();
        }
        
        /**
         * この{@link Logger}定義を使用して生成した{@link Logger}を取得する。
         * @return {@link Logger}
         */
        private Logger getLogger() {
            return logger;
        }

        /**
         * この{@link Logger}定義を使用して生成した{@link Logger}を取得する。
         * @param runtimeName ロガーに付与する実行時名称
         * @return {@link Logger}
         */
        private Logger getLogger(String runtimeName) {
            return new BasicLogger((BasicLogger) logger, runtimeName);
        }

        /**
         * ログの出力先となる{@link LogWriter}を取得する。
         * @return ログの出力先となる{@link LogWriter}
         */
        private List<LogWriter> getWriters() {
            return writers;
        }
        
        /**
         * ロガー設定の名称を取得する。
         * @return ロガー設定の名称
         */
        private String getName() {
            return name;
        }
        
        /**
         * {@link Logger}名に対するマッチングに使用する正規表現を取得する。
         * @return {@link Logger}名に対するマッチングに使用する正規表現
         */
        private String getNameRegex() {
            return nameRegex;
        }
        
        /**
         * ログの出力制御の基準とする{@link LogLevel}を取得する。
         * @return ログの出力制御の基準とする{@link LogLevel}
         */
        private LogLevel getBaseLevel() {
            return baseLevel;
        }
    }
}
