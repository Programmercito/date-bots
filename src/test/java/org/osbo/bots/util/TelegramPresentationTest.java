package org.osbo.bots.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramPresentationTest {

    @Test
    void shouldDecorateChannelTextWithoutChangingUserContent() {
        String text = "Hola *amigos* 😊\nBusco conocer gente | en La Paz";

        assertThat(TelegramPresentation.channelPost(text, false))
                .startsWith("💌 Una nueva conexión\n\n")
                .contains(text)
                .endsWith("✨ ¿Te gustaría conocer a esta persona?");
    }

    @Test
    void shouldKeepLongPhotoCaptionsUnchanged() {
        String text = "a".repeat(TelegramPresentation.CAPTION_LIMIT);

        assertThat(TelegramPresentation.channelPost(text, true)).isEqualTo(text);
    }

    @Test
    void shouldKeepLongTextMessagesUnchanged() {
        String text = "a".repeat(TelegramPresentation.TEXT_LIMIT);

        assertThat(TelegramPresentation.channelPost(text, false)).isEqualTo(text);
    }

    @Test
    void shouldDecorateOnlyWhenTheEntireMessageFits() {
        assertThat(TelegramPresentation.decorate("🌷", "A", "B", 4)).isEqualTo("A🌷B");
        assertThat(TelegramPresentation.decorate("🌷", "A", "B", 3)).isEqualTo("🌷");
    }
}
