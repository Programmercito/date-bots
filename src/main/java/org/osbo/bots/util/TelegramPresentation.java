package org.osbo.bots.util;

import java.util.ArrayList;
import java.util.List;

public final class TelegramPresentation {

    public static final int TEXT_LIMIT = 4096;
    public static final int CAPTION_LIMIT = 1024;

    private TelegramPresentation() {
    }

    public static String decorate(String text, String prefix, String suffix, int limit) {
        if ((long) prefix.length() + text.length() + suffix.length() > limit) {
            return text;
        }
        return prefix + text + suffix;
    }

    public static String channelPost(String text, boolean hasPhoto) {
        return decorate(text, "💌 Una nueva conexión\n\n",
                "\n\n✨ ¿Te gustaría conocer a esta persona?",
                hasPhoto ? CAPTION_LIMIT : TEXT_LIMIT);
    }

    public static List<String> splitText(String text) {
        List<String> parts = new ArrayList<>();
        for (int start = 0; start < text.length();) {
            int end = Math.min(start + TEXT_LIMIT, text.length());
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))
                    && Character.isLowSurrogate(text.charAt(end))) {
                end--;
            }
            parts.add(text.substring(start, end));
            start = end;
        }
        return parts;
    }
}
