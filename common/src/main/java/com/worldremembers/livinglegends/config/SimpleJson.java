package com.worldremembers.livinglegends.config;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleJson {
    private SimpleJson() {
    }

    public static Map<String, Object> parseObject(Reader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[2048];
        int read;

        while ((read = reader.read(buffer)) >= 0) {
            content.append(buffer, 0, read);
        }

        Object parsed = new Parser(content.toString()).parse();

        if (parsed instanceof Map<?, ?> map) {
            return castObjectMap(map);
        }

        throw new IllegalArgumentException("Config root must be a JSON object");
    }

    public static Map<String, Object> parseObjectWithComments(Reader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[2048];
        int read;

        while ((read = reader.read(buffer)) >= 0) {
            content.append(buffer, 0, read);
        }

        Object parsed = new Parser(stripComments(content.toString())).parse();

        if (parsed instanceof Map<?, ?> map) {
            return castObjectMap(map);
        }

        throw new IllegalArgumentException("Config root must be a JSON object");
    }

    public static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value, 0);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castObjectMap(Map<?, ?> map) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("JSON object keys must be strings");
            }

            values.put(key, entry.getValue());
        }

        return values;
    }

    private static void writeValue(StringBuilder builder, Object value, int indent) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String stringValue) {
            writeString(builder, stringValue);
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(builder, map, indent);
        } else if (value instanceof Iterable<?> iterable) {
            writeArray(builder, iterable, indent);
        } else {
            writeString(builder, String.valueOf(value));
        }
    }

    private static void writeObject(StringBuilder builder, Map<?, ?> map, int indent) {
        builder.append('{');

        if (!map.isEmpty()) {
            boolean first = true;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }

                builder.append('\n');
                indent(builder, indent + 2);
                writeString(builder, String.valueOf(entry.getKey()));
                builder.append(": ");
                writeValue(builder, entry.getValue(), indent + 2);
                first = false;
            }

            builder.append('\n');
            indent(builder, indent);
        }

        builder.append('}');
    }

    private static void writeArray(StringBuilder builder, Iterable<?> values, int indent) {
        builder.append('[');
        boolean first = true;

        for (Object value : values) {
            if (!first) {
                builder.append(',');
            }

            builder.append('\n');
            indent(builder, indent + 2);
            writeValue(builder, value, indent + 2);
            first = false;
        }

        if (!first) {
            builder.append('\n');
            indent(builder, indent);
        }

        builder.append(']');
    }

    private static void writeString(StringBuilder builder, String value) {
        builder.append('"');

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }

        builder.append('"');
    }

    private static void indent(StringBuilder builder, int indent) {
        builder.append(" ".repeat(Math.max(0, indent)));
    }

    private static String stripComments(String input) {
        StringBuilder output = new StringBuilder();
        boolean inString = false;
        boolean escaping = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            char next = index + 1 < input.length() ? input.charAt(index + 1) : '\0';

            if (lineComment) {
                if (current == '\n' || current == '\r') {
                    lineComment = false;
                    output.append(current);
                }
                continue;
            }

            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    index++;
                } else if (current == '\n' || current == '\r') {
                    output.append(current);
                }
                continue;
            }

            if (inString) {
                output.append(current);
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                output.append(current);
                continue;
            }
            if (current == '/' && next == '/') {
                lineComment = true;
                index++;
                continue;
            }
            if (current == '/' && next == '*') {
                blockComment = true;
                index++;
                continue;
            }
            output.append(current);
        }
        return output.toString();
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input;
        }

        private Object parse() {
            skipWhitespace();

            if (isAtEnd()) {
                throw error("JSON is empty");
            }

            Object value = parseValue();
            skipWhitespace();

            if (!isAtEnd()) {
                throw error("Unexpected trailing content");
            }

            return value;
        }

        private Object parseValue() {
            skipWhitespace();

            if (isAtEnd()) {
                throw error("Expected JSON value");
            }

            char current = input.charAt(index);

            return switch (current) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (current == '-' || Character.isDigit(current)) {
                        yield parseNumber();
                    }

                    throw error("Unexpected character '" + current + "'");
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> values = new LinkedHashMap<>();
            skipWhitespace();

            if (peek('}')) {
                index++;
                return values;
            }

            while (true) {
                skipWhitespace();

                if (!peek('"')) {
                    throw error("Expected object key string");
                }

                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                values.put(key, value);
                skipWhitespace();

                if (peek('}')) {
                    index++;
                    return values;
                }

                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipWhitespace();

            if (peek(']')) {
                index++;
                return values;
            }

            while (true) {
                values.add(parseValue());
                skipWhitespace();

                if (peek(']')) {
                    index++;
                    return values;
                }

                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();

            while (!isAtEnd()) {
                char current = input.charAt(index++);

                if (current == '"') {
                    return value.toString();
                }

                if (current != '\\') {
                    value.append(current);
                    continue;
                }

                if (isAtEnd()) {
                    throw error("Unterminated escape sequence");
                }

                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(parseUnicodeEscape());
                    default -> throw error("Unsupported escape sequence '\\" + escaped + "'");
                }
            }

            throw error("Unterminated string");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > input.length()) {
                throw error("Incomplete unicode escape");
            }

            String hex = input.substring(index, index + 4);
            index += 4;

            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid unicode escape");
            }
        }

        private Object parseNumber() {
            int start = index;

            if (peek('-')) {
                index++;
            }

            readDigits();

            if (peek('.')) {
                index++;
                readDigits();
            }

            if (peek('e') || peek('E')) {
                index++;

                if (peek('+') || peek('-')) {
                    index++;
                }

                readDigits();
            }

            String raw = input.substring(start, index);

            try {
                if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                    return Double.parseDouble(raw);
                }

                return Long.parseLong(raw);
            } catch (NumberFormatException exception) {
                throw error("Invalid number '" + raw + "'");
            }
        }

        private void readDigits() {
            int start = index;

            while (!isAtEnd() && Character.isDigit(input.charAt(index))) {
                index++;
            }

            if (index == start) {
                throw error("Expected digit");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!input.startsWith(literal, index)) {
                throw error("Expected '" + literal + "'");
            }

            index += literal.length();
            return value;
        }

        private void expect(char expected) {
            skipWhitespace();

            if (!peek(expected)) {
                throw error("Expected '" + expected + "'");
            }

            index++;
        }

        private boolean peek(char expected) {
            return !isAtEnd() && input.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (!isAtEnd()) {
                char current = input.charAt(index);

                if (!Character.isWhitespace(current)) {
                    return;
                }

                index++;
            }
        }

        private boolean isAtEnd() {
            return index >= input.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }
    }
}
