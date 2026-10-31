package org.osbo.bots.crons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.model.services.ClubStatisticsService;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

class HorariosTest {

    private static final String CHANNEL_CHATID = "channel-123";

    private NqueueForSend sender;
    private ClubStatisticsService statistics;
    private Horarios horarios;

    @BeforeEach
    void setUp() {
        sender = mock(NqueueForSend.class);
        statistics = mock(ClubStatisticsService.class);
        horarios = new Horarios(sender, statistics);
        ReflectionTestUtils.setField(horarios, "channel", CHANNEL_CHATID);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0 perfiles aprobados disponibles",
            "1, 1 perfil aprobado disponible",
            "42, 42 perfiles aprobados disponibles"
    })
    @SuppressWarnings("unchecked")
    void shouldShowCurrentCountAndBotButtonInOpeningAnnouncement(long count, String expected) {
        when(statistics.countDiscoverableProfiles()).thenReturn(count);

        horarios.inicio();

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttons = ArgumentCaptor.forClass(List.class);
        verify(sender).sendMarkdown(eq(CHANNEL_CHATID), message.capture(), buttons.capture());
        assertThat(message.getValue()).contains("canal de amistad está abierto", "una hora", expected, "/club");
        assertThat(buttons.getValue()).hasSize(1);
        assertThat(buttons.getValue().get(0)).singleElement().satisfies(button -> {
            assertThat(button.getText()).contains("Abrir el bot");
            assertThat(button.getUrl()).isEqualTo("https://t.me/datebobot");
            assertThat(button.getCallbackData()).isNull();
        });
    }

    @Test
    void shouldRefreshCountForClosingAnnouncement() {
        when(statistics.countDiscoverableProfiles()).thenReturn(2L, 3L);

        horarios.inicio();
        horarios.fin();

        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(sender, org.mockito.Mockito.times(2)).sendMarkdown(eq(CHANNEL_CHATID), messages.capture(),
                org.mockito.ArgumentMatchers.anyList());
        assertThat(messages.getAllValues().get(0)).contains("2 perfiles aprobados disponibles");
        assertThat(messages.getAllValues().get(1)).contains("canal de amistad está cerrado",
                "3 perfiles aprobados disponibles", "/club", "24 horas");
        verify(statistics, org.mockito.Mockito.times(2)).countDiscoverableProfiles();
    }

    @Test
    void shouldNotPublishAMisleadingCountWhenQueryFails() {
        when(statistics.countDiscoverableProfiles()).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(horarios::inicio).isInstanceOf(IllegalStateException.class);
        verify(sender, never()).sendMarkdown(anyString(), anyString(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldStillAnnounceOpeningWhenDatabaseIsUnavailable() {
        when(statistics.countDiscoverableProfiles())
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        horarios.inicio();

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMarkdown(eq(CHANNEL_CHATID), message.capture(), org.mockito.ArgumentMatchers.anyList());
        assertThat(message.getValue()).contains("canal de amistad está abierto",
                "El conteo de perfiles no está disponible en este momento");
        assertThat(message.getValue()).doesNotContain("Actualmente hay");
    }
}
