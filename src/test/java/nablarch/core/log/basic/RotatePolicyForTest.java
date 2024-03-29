package nablarch.core.log.basic;

import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * RotatePolicyの実装クラスのモック
 * ログが20回書き込まれるごとにローテーションする
 * */
public class RotatePolicyForTest implements RotatePolicy {

    private String logFilePath;
    private int logCounter;

    private  boolean isAlwaysRotation;

    @Override
    public void initialize(ObjectSettings settings) {
        logFilePath = settings.getRequiredProp("filePath");

        String rotation = settings.getProp("rotation");
        if ("always".equals(rotation)) {
            isAlwaysRotation = true;
        }
    }

    @Override
    public boolean needsRotate(String message, Charset charset) {
        logCounter++;

        if (isAlwaysRotation) {
            return true;
        }

        return logCounter % 20 == 0;
    }

    @Override
    public String decideRotatedFilePath() {
        DateFormat oldFileDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return logFilePath + "." + oldFileDateFormat.format(new Date()) + ".old";
    }

    @Override
    public void rotate(String rotatedFilePath) {
        if (!new File(logFilePath).renameTo(new File(rotatedFilePath))) {
            throw new IllegalStateException(
                    "renaming failed. File#renameTo returns false. src file = [" + logFilePath + "], dest file = [" + rotatedFilePath + "]");
        }
    }

    @Override
    public String getSettings() {
        return "RotatePolicyForTest getSettings was called";
    }

    @Override
    public void onWrite(String message, Charset charset) {

    }

    @Override
    public void onOpenFile(File file) {

    }
}
