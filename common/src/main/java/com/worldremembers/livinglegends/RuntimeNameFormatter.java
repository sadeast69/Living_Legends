package com.worldremembers.livinglegends;

public final class RuntimeNameFormatter {
    private static final int MAX_RUNTIME_NAME_LENGTH = 64;

    private RuntimeNameFormatter() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String trimmed = value.trim();
        StringBuilder result = new StringBuilder();
        boolean skipFormattingCode = false;
        for (int index = 0; index < trimmed.length() && result.length() < MAX_RUNTIME_NAME_LENGTH; index++) {
            char character = trimmed.charAt(index);
            if (skipFormattingCode) {
                skipFormattingCode = false;
                continue;
            }
            if (character == '\u00A7') {
                skipFormattingCode = true;
                continue;
            }
            if (character == '\n' || character == '\r' || Character.isISOControl(character)) {
                continue;
            }
            result.append(character);
        }
        return result.toString().trim();
    }

    public static String encodeNoteValue(String value) {
        String sanitized = sanitize(value);
        if (sanitized.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int index = 0; index < sanitized.length(); index++) {
            char character = sanitized.charAt(index);
            if (Character.isWhitespace(character)) {
                result.append("%20");
            } else if (character == '%') {
                result.append("%25");
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    public static String decodeNoteValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String trimmed = value.trim();
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < trimmed.length(); index++) {
            char character = trimmed.charAt(index);
            if (character == '%' && index + 2 < trimmed.length()) {
                int high = hexValue(trimmed.charAt(index + 1));
                int low = hexValue(trimmed.charAt(index + 2));
                if (high >= 0 && low >= 0) {
                    result.append((char) (high * 16 + low));
                    index += 2;
                    continue;
                }
            }
            // Earlier event notes used underscores because note values were whitespace-split.
            result.append(character == '_' ? ' ' : character);
        }
        return sanitize(result.toString());
    }

    private static int hexValue(char character) {
        if (character >= '0' && character <= '9') {
            return character - '0';
        }
        if (character >= 'a' && character <= 'f') {
            return character - 'a' + 10;
        }
        if (character >= 'A' && character <= 'F') {
            return character - 'A' + 10;
        }
        return -1;
    }
}
