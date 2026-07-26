package org.osbo.bots.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.osbo.bots.jms.queue.pojos.Button;

class LookingForOptionTest {

    @Test
    void shouldTranslateAllStoredCodesToSpanishLabels() {
        assertThat(LookingForOption.translate(LookingForOption.LOOKING_FOR_FRIENDSHIP)).isEqualTo("Amistad");
        assertThat(LookingForOption.translate(LookingForOption.LOOKING_FOR_RELATIONSHIP)).isEqualTo("Relación");
        assertThat(LookingForOption.translate(LookingForOption.LOOKING_FOR_ONLINE_RELATIONSHIP))
                .isEqualTo("Relación online");
        assertThat(LookingForOption.translate(LookingForOption.LOOKING_FOR_CASUAL)).isEqualTo("Algo casual");
        assertThat(LookingForOption.translate(LookingForOption.LOOKING_FOR_SUGAR_DADDY)).isEqualTo("Sugar");
        assertThat(LookingForOption.translate(LookingForOption.LOOKING_FOR_SERIOUS_RELATIONSHIP))
                .isEqualTo("Relación seria");
        assertThat(LookingForOption.translate(LookingForOption.LOOKING_FOR_MARRIAGE)).isEqualTo("Matrimonio");
        assertThat(LookingForOption.translate(LookingForOption.LOOKING_FOR_LOVERS)).isEqualTo("Novios");
        assertThat(LookingForOption.translate(LookingForOption.LOOKING_FOR_INFORMAL_RELATIONSHIP))
                .isEqualTo("Relación informal");
    }

    @Test
    void shouldReturnUnknownCodeAsIs() {
        assertThat(LookingForOption.translate("UNKNOWN_CODE")).isEqualTo("UNKNOWN_CODE");
    }

    @Test
    void shouldReturnNullForNullCode() {
        assertThat(LookingForOption.translate(null)).isNull();
    }

    @Test
    void shouldMapCallbacksToStoredCodes() {
        assertThat(LookingForOption.fromCallback(LookingForOption.CALLBACK_LOOKING_FOR_FRIENDSHIP))
                .isEqualTo(LookingForOption.LOOKING_FOR_FRIENDSHIP);
        assertThat(LookingForOption.fromCallback(LookingForOption.CALLBACK_LOOKING_FOR_CASUAL))
                .isEqualTo(LookingForOption.LOOKING_FOR_CASUAL);
        assertThat(LookingForOption.fromCallback(LookingForOption.CALLBACK_LOOKING_FOR_MARRIAGE))
                .isEqualTo(LookingForOption.LOOKING_FOR_MARRIAGE);
    }

    @Test
    void shouldReturnNullForUnknownCallback() {
        assertThat(LookingForOption.fromCallback("invalid_callback")).isNull();
    }

    @Test
    void shouldProvideButtonRowsWithSpanishLabels() {
        List<List<Button>> rows = LookingForOption.getButtonRows();

        assertThat(rows).isNotEmpty();
        List<String> labels = rows.stream().flatMap(List::stream).map(Button::getText).toList();
        assertThat(labels).contains("Amistad", "Relación", "Relación online", "Algo casual", "Sugar",
                "Relación seria", "Matrimonio", "Novios", "Relación informal");
    }

    @Test
    void shouldProvideButtonRowsWithStableCallbacks() {
        List<List<Button>> rows = LookingForOption.getButtonRows();

        List<String> callbacks = rows.stream().flatMap(List::stream).map(Button::getCallbackData).toList();
        assertThat(callbacks).allMatch(callback -> callback.startsWith("club_looking_for_"));
    }

}
