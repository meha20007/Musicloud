package cloud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small JSON reader for the Swing client, which intentionally has no JSON dependency. */
public final class Json {

    private Json() {}

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.isFinished()) throw new IllegalArgumentException("Unexpected content after JSON value.");
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Object value) {
        return value instanceof List ? (List<Object>) value : null;
    }

    public static String string(Map<String, Object> object, String key) {
        Object value = object == null ? null : object.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static long number(Map<String, Object> object, String key) {
        Object value = object == null ? null : object.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); }
            catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            if (text == null) throw new IllegalArgumentException("JSON content is missing.");
            this.text = text;
        }

        private Object readValue() {
            skipWhitespace();
            if (isFinished()) throw new IllegalArgumentException("JSON content is empty.");
            char current = text.charAt(index);
            if (current == '{') return readObject();
            if (current == '[') return readArray();
            if (current == '"') return readString();
            if (current == 't') return readLiteral("true", Boolean.TRUE);
            if (current == 'f') return readLiteral("false", Boolean.FALSE);
            if (current == 'n') return readLiteral("null", null);
            return readNumber();
        }

        private Map<String, Object> readObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            index++;
            skipWhitespace();
            if (take('}')) return result;
            do {
                skipWhitespace();
                if (isFinished() || text.charAt(index) != '"') throw invalid();
                String key = readString();
                skipWhitespace();
                if (!take(':')) throw invalid();
                result.put(key, readValue());
                skipWhitespace();
                if (take('}')) return result;
                if (!take(',')) throw invalid();
            } while (true);
        }

        private List<Object> readArray() {
            List<Object> result = new ArrayList<>();
            index++;
            skipWhitespace();
            if (take(']')) return result;
            do {
                result.add(readValue());
                skipWhitespace();
                if (take(']')) return result;
                if (!take(',')) throw invalid();
            } while (true);
        }

        private String readString() {
            if (!take('"')) throw invalid();
            StringBuilder result = new StringBuilder();
            while (!isFinished()) {
                char current = text.charAt(index++);
                if (current == '"') return result.toString();
                if (current != '\\') {
                    if (current < 0x20) throw invalid();
                    result.append(current);
                    continue;
                }
                if (isFinished()) throw invalid();
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'u':
                        if (index + 4 > text.length()) throw invalid();
                        try {
                            result.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw invalid();
                        }
                        index += 4;
                        break;
                    default: throw invalid();
                }
            }
            throw invalid();
        }

        private Object readNumber() {
            int start = index;
            if (take('-')) { }
            while (!isFinished() && Character.isDigit(text.charAt(index))) index++;
            if (!isFinished() && text.charAt(index) == '.') {
                index++;
                while (!isFinished() && Character.isDigit(text.charAt(index))) index++;
            }
            if (!isFinished() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                index++;
                if (!isFinished() && (text.charAt(index) == '+' || text.charAt(index) == '-')) index++;
                while (!isFinished() && Character.isDigit(text.charAt(index))) index++;
            }
            if (start == index) throw invalid();
            String number = text.substring(start, index);
            try {
                if (number.contains(".") || number.contains("e") || number.contains("E")) {
                    return Double.valueOf(number);
                }
                return Long.valueOf(number);
            } catch (NumberFormatException ex) {
                throw invalid();
            }
        }

        private Object readLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) throw invalid();
            index += literal.length();
            return value;
        }

        private boolean take(char expected) {
            if (!isFinished() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (!isFinished() && Character.isWhitespace(text.charAt(index))) index++;
        }

        private boolean isFinished() { return index >= text.length(); }
        private IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid JSON response."); }
    }
}
