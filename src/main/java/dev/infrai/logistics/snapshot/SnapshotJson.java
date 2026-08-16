package dev.infrai.logistics.snapshot;

import java.util.Iterator;
import java.util.Map;

final class SnapshotJson {
    private SnapshotJson() {}

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return "\"" + escape(text) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                out.append(write(entry.getKey().toString())).append(':').append(write(entry.getValue()));
                if (iterator.hasNext()) out.append(',');
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder out = new StringBuilder("[");
            Iterator<?> iterator = items.iterator();
            while (iterator.hasNext()) {
                out.append(write(iterator.next()));
                if (iterator.hasNext()) out.append(',');
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported snapshot value: " + value.getClass().getName());
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
