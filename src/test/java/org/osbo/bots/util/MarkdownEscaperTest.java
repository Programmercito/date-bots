package org.osbo.bots.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownEscaperTest {

    @Test
    void shouldEscapeSpecialCharacters() {
        assertThat(MarkdownEscaper.escape("*bold*_test_[link](url)"))
                .isEqualTo("\\*bold\\*\\_test\\_\\[link\\]\\(url\\)");
    }

    @Test
    void shouldReturnEmptyStringForNull() {
        assertThat(MarkdownEscaper.escape(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyStringForBlank() {
        assertThat(MarkdownEscaper.escape("   ")).isEmpty();
    }

    @Test
    void shouldNotChangePlainText() {
        assertThat(MarkdownEscaper.escape("Hola mundo")).isEqualTo("Hola mundo");
    }

}
