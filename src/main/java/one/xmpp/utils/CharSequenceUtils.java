package one.xmpp.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

public class CharSequenceUtils {

    static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    public static final Comparator<CharSequence> COMPARATOR = new Comparator<CharSequence>() {
        @Override
        public int compare(CharSequence o1, CharSequence o2) {
            if (o1 == null || o2 == null) {
                throw new NullPointerException();
            }

            return CharSequenceUtils.compare(o1, o2);
        }
    };

    /**
     * Charset decoder for UTF-8
     */
    private static final ThreadLocal<CharsetDecoder> UTF8_DECODER = new ThreadLocal<CharsetDecoder>() {
        @Override
        protected CharsetDecoder initialValue() {
            return CHARSET_UTF8.newDecoder();
        }
    };

    public static int compare(CharSequence sequence1, CharSequence sequence2) {
        int len1 = sequence1.length();
        int len2 = sequence2.length();
        int n = Math.min(len1, len2);

        int k = 0;
        while (k < n) {
            char c1 = sequence1.charAt(k);
            char c2 = sequence2.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    public static CharSequence concatenate(final CharSequence... sequences) {
        int targetLength = 0;
        for (int i = 0; i < sequences.length; i++) {
            targetLength += sequences[i] != null ? sequences[i].length() : 4;
        }
        StringBuffer stringBuffer = new StringBuffer(targetLength);
        for (int i = 0; i < sequences.length; i++) {
            stringBuffer.append(sequences[i] != null ? sequences[i] : "null");
        }
        return stringBuffer;
    }

    public static CharSequence decodeFromUtf8(ByteBuffer byteBuffer) throws CharacterCodingException {
        final CharsetDecoder charsetDecoder = UTF8_DECODER.get();
        charsetDecoder.reset();

        final ByteBuffer duplicate = byteBuffer.duplicate();
        return charsetDecoder.decode(duplicate);
    }

    public static CharSequence decodeFromUtf8Safe(byte[] bytes) {
        return decodeFromUtf8Safe(ByteBuffer.wrap(bytes));
    }

    public static CharSequence decodeFromUtf8Safe(ByteBuffer byteBuffer) {
        try {
            final CharsetDecoder charsetDecoder = UTF8_DECODER.get();
            charsetDecoder.reset();

            final ByteBuffer duplicate = byteBuffer.duplicate();
            return charsetDecoder.decode(duplicate);
        } catch (Exception exc) {
            return "<error>";
        }
    }

    public static ByteBuffer encodeToUtf8(CharSequence data) {
        return CHARSET_UTF8.encode(CharBuffer.wrap(data));
    }

    public static byte[] encodeToUtf8ByteArray(CharSequence data) {
        ByteBuffer byteBuffer = CHARSET_UTF8.encode(CharBuffer.wrap(data));
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit());
    }

    public static boolean equals(CharSequence sequence1, CharSequence sequence2) {
        if (sequence1 == null || sequence2 == null) {
            return sequence1 == null && sequence2 == null;
        }
        final int length = sequence1.length();
        if (length != sequence2.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (sequence1.charAt(i) != sequence2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static CharSequence escapeXml(CharSequence data, boolean inAttributeValue) {
        if (data == null) {
            return null;
        }

        if (!needXmlEscape(data, inAttributeValue)) {
            return data;
        }

        StringBuilder stringBuilder = new StringBuilder((int) (data.length() * 1.1f));
        escapeXml(stringBuilder, data, inAttributeValue);
        return stringBuilder;
    }

    public static void escapeXml(StringBuilder stringBuilder, CharSequence data, boolean inAttributeValue) {
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);

            if (c < 32) {

                switch (c) {
                case 0x09:
                    if (inAttributeValue) {
                        stringBuilder.append("&#x09;");
                    } else {
                        stringBuilder.append(c);
                    }
                    break;
                case 0x0A:
                    if (inAttributeValue) {
                        stringBuilder.append("&#x0A;");
                    } else {
                        stringBuilder.append(c);
                    }
                    break;
                case 0x0D:
                    if (inAttributeValue) {
                        stringBuilder.append("&#x0D;");
                    } else {
                        stringBuilder.append(c);
                    }
                    break;
                default:
                    stringBuilder.append('?');
                }

            } else {

                switch (c) {
                case '&':
                    stringBuilder.append("&amp;");
                    break;
                case '\'':
                    stringBuilder.append("&apos;");
                    break;
                case '<':
                    stringBuilder.append("&lt;");
                    break;
                case '>':
                    stringBuilder.append("&gt;");
                    break;
                case '"':
                    stringBuilder.append("&quot;");
                    break;
                default:
                    stringBuilder.append(c);
                    break;
                }

            }
        }
    }

    public static boolean isEmptyTrimmed(CharSequence data) {
        if (data == null || data.length() == 0) {
            return true;
        }

        for (int i = 0; i < data.length(); i++) {
            if (!Character.isWhitespace(data.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean needXmlEscape(CharSequence data, boolean inAttributeValue) {
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);

            if (c == 0x09 || c == 0x0A || c == 0x0D) {
                if (inAttributeValue) {
                    return true;
                }
            } else if (c < 32) {
                // will be replaced with '?'
                return true;
            } else if (c == '&' || c == '\'' || c == '<' || c == '>' || c == '\"') {
                return true;
            }
        }

        return false;
    }

    public static boolean startsWith(CharSequence sequence, CharSequence prefix) {
        if (sequence == null) {
            throw new NullArgumentException("sequence");
        }
        if (prefix == null) {
            throw new NullArgumentException("prefix");
        }

        final int length = prefix.length();

        if (length > sequence.length()) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (sequence.charAt(i) != prefix.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    public static CharSequence substringAfter(CharSequence sequence, char c) {
        final int length = sequence.length();

        for (int i = 0; i < length; i++) {
            if (sequence.charAt(i) == c) {
                if (i == length - 1) {
                    return StringUtils.EMPTY;
                }

                return sequence.subSequence(i + 1, length);
            }
        }

        return sequence;
    }

    public static CharSequence substringBefore(CharSequence sequence, char c) {
        final int length = sequence.length();

        for (int i = 0; i < length; i++) {
            if (sequence.charAt(i) == c) {
                return sequence.subSequence(0, i);
            }
        }

        return StringUtils.EMPTY;
    }

    private static void unescapeAmpSequence(StringBuilder builder, StringBuilder escapeSequence) {

        if (escapeSequence.length() < 2) {
            throw new IllegalArgumentException("Unknown escape sequence: " + escapeSequence);
        }

        switch (escapeSequence.charAt(0)) {

        case 'a':
            if (escapeSequence.length() == 3 && escapeSequence.charAt(1) == 'm' && escapeSequence.charAt(2) == 'p') {
                builder.append('&');
                return;
            }
            if (escapeSequence.length() == 4 && escapeSequence.charAt(1) == 'p' && escapeSequence.charAt(2) == 'o'
                    && escapeSequence.charAt(3) == 's') {
                builder.append('\'');
                return;
            }
            break;
        case 'g':
            if (escapeSequence.length() == 2 && escapeSequence.charAt(1) == 't') {
                builder.append('>');
                return;
            }
            break;
        case 'l':
            if (escapeSequence.length() == 2 && escapeSequence.charAt(1) == 't') {
                builder.append('<');
                return;
            }
            break;
        case 'q':
            if (escapeSequence.length() == 4 && escapeSequence.charAt(1) == 'u' && escapeSequence.charAt(2) == 'o'
                    && escapeSequence.charAt(3) == 't') {
                builder.append('"');
                return;
            }
            break;
        case '#':

            if (escapeSequence.charAt(1) == 'x') {
                // hex
                int value = 0;
                for (int i = 2; i < escapeSequence.length(); i++) {
                    value <<= 4;
                    final char c = escapeSequence.charAt(i);
                    if ('0' <= c && c <= '9') {
                        value = value + (c - '0');
                    } else if ('a' <= c && c <= 'f') {
                        value = value + (c - 'a' + 10);
                    } else if ('A' <= c && c <= 'F') {
                        value = value + (c - 'A' + 10);
                    } else {
                        throw new IllegalArgumentException("Unknown escape sequence: " + escapeSequence);
                    }
                }
                builder.append((char) value);
            } else {
                // decimal
                int value = 0;
                for (int i = 1; i < escapeSequence.length(); i++) {
                    value *= 10;
                    final char c = escapeSequence.charAt(i);
                    if ('0' <= c && c <= '9') {
                        value = value + (c - '0');
                    } else {
                        throw new IllegalArgumentException("Unknown escape sequence: " + escapeSequence);
                    }
                }
                builder.append((char) value);
            }

            return;
        }

        throw new IllegalArgumentException("Unknown escape sequence: " + escapeSequence);
    }

    public static CharSequence unescapeXml(CharSequence charSequence) throws IllegalArgumentException {
        StringBuilder escapeSequence = new StringBuilder();
        StringBuilder builder = new StringBuilder(charSequence.length());
        i: for (int i = 0; i < charSequence.length(); i++) {

            final char currentChar = charSequence.charAt(i);
            if (currentChar == '&') {
                // begin escape sequence
                if (escapeSequence.length() != 0) {
                    throw new IllegalArgumentException("Another escape sequence started inside existed");
                }

                escapeSequence = new StringBuilder();
                for (int k = i + 1; k < charSequence.length(); k++) {
                    final char escapeSequenceChar = charSequence.charAt(k);
                    if (escapeSequenceChar == ';') {
                        // end of sequence
                        unescapeAmpSequence(builder, escapeSequence);
                        escapeSequence.setLength(0);
                        i = k;
                        continue i;
                    }
                    escapeSequence.append(escapeSequenceChar);
                }

                throw new IllegalArgumentException("Escape sequence not finished until the end of string");
            }

            builder.append(currentChar);
        }
        return builder;
    }
}
