package dev.infrai.logistics.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonCodec {
    private JsonCodec() {}

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(quote(entry.getKey().toString())).append(':').append(write(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> values) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : values) {
                if (!first) out.append(',');
                first = false;
                out.append(write(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    static Map<String, Object> readObject(String json) {
        Object value = new Parser(json).parse();
        if (!(value instanceof Map<?, ?> raw)) throw new IllegalArgumentException("Expected JSON object");
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(key.toString(), item));
        return result;
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('\"').toString();
    }

    private static final class Parser {
        private final String input;
        private int cursor;

        private Parser(String input) { this.input = input; }

        Object parse() {
            Object value = value();
            whitespace();
            if (cursor != input.length()) fail("Trailing content");
            return value;
        }

        private Object value() {
            whitespace();
            if (cursor >= input.length()) return fail("Missing value");
            return switch (input.charAt(cursor)) {
                case '{' -> object();
                case '[' -> array();
                case '\"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            cursor++;
            whitespace();
            if (take('}')) return map;
            do {
                whitespace();
                if (cursor >= input.length() || input.charAt(cursor) != '\"') fail("Expected key");
                String key = string();
                whitespace();
                if (!take(':')) fail("Expected colon");
                map.put(key, value());
                whitespace();
            } while (take(','));
            if (!take('}')) fail("Expected object end");
            return map;
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            cursor++;
            whitespace();
            if (take(']')) return list;
            do {
                list.add(value());
                whitespace();
            } while (take(','));
            if (!take(']')) fail("Expected array end");
            return list;
        }

        private String string() {
            StringBuilder out = new StringBuilder();
            cursor++;
            while (cursor < input.length()) {
                char c = input.charAt(cursor++);
                if (c == '\"') return out.toString();
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (cursor >= input.length()) fail("Incomplete escape");
                char escaped = input.charAt(cursor++);
                switch (escaped) {
                    case '\"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (cursor + 4 > input.length()) fail("Incomplete unicode escape");
                        out.append((char) Integer.parseInt(input.substring(cursor, cursor + 4), 16));
                        cursor += 4;
                    }
                    default -> fail("Unknown escape");
                }
            }
            return fail("Unclosed string");
        }

        private Object number() {
            int start = cursor;
            while (cursor < input.length() && "-+0123456789.eE".indexOf(input.charAt(cursor)) >= 0) cursor++;
            if (start == cursor) return fail("Expected value");
            String token = input.substring(start, cursor);
            return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.valueOf(token) : Long.valueOf(token);
        }

        private Object literal(String token, Object value) {
            if (!input.startsWith(token, cursor)) return fail("Invalid literal");
            cursor += token.length();
            return value;
        }

        private boolean take(char expected) {
            if (cursor < input.length() && input.charAt(cursor) == expected) {
                cursor++;
                return true;
            }
            return false;
        }

        private void whitespace() {
            while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) cursor++;
        }

        private <T> T fail(String message) {
            throw new IllegalArgumentException(message + " at character " + cursor);
        }
    }
}
