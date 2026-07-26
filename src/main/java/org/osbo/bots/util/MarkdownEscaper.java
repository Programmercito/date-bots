package org.osbo.bots.util;

/**
 * Escapes characters that have special meaning in Telegram Markdown mode.
 */
public final class MarkdownEscaper {

    private MarkdownEscaper() {
    }

    /**
     * Escapes *, _, [, ], (, ) and ` for Telegram Markdown parse mode.
     *
     * @param text the text to escape
     * @return the escaped text, or an empty string if the input is null/blank
     */
    public static String escape(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("`", "\\`");
    }

}
