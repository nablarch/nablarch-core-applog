package nablarch.core.log.app;

import nablarch.core.text.json.*;

import java.io.IOException;
import java.io.Writer;

public class CustomJsonSerializationManager extends JsonSerializationManager {

    protected void enlistSerializer(JsonSerializationSettings settings) {
        addSerializer(new ThrowIOExceptionSerializer());
    }

    public class ThrowIOExceptionSerializer implements JsonSerializer {

        public void initialize(JsonSerializationSettings settings) {
            // NOOP
        }

        public boolean isTarget(Class<?> valueClass) {
            return true;
        }

        public void serialize(Writer writer, Object value) throws IOException {
            throw new IOException("test");
        }

    }
}
