package org.osbo.bots.crons;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.springframework.test.util.ReflectionTestUtils;

class HorariosTest {

    private static final String CHANNEL_CHATID = "channel-123";

    private NqueueForSend sender;
    private Horarios horarios;

    @BeforeEach
    void setUp() {
        sender = mock(NqueueForSend.class);
        horarios = new Horarios(sender);
        ReflectionTestUtils.setField(horarios, "channel", CHANNEL_CHATID);
    }

    @Test
    void shouldSendOpeningAnnouncementWithMarkdown() {
        horarios.inicio();

        verify(sender).sendMarkdown(CHANNEL_CHATID,
                "🎉 ¡El canal de amistad se ha abierto! 🎉\n\n"
                        + "Escribe al bot @datebobot para publicar mensajes de texto, fotos, emojis y negritas. "
                        + "✨ Los mensajes son efímeros: duran 1 hora y luego se borran automáticamente.\n\n"
                        + "💡 *¿Querés conocer gente de forma más privada?* Unite al *Club de Amistad* 🤝 del bot y "
                        + "descubrí perfiles de personas que buscan lo mismo que vos. ¡Es una comunidad aparte "
                        + "dentro del canal! 🥰");
    }

    @Test
    void shouldSendClosingAnnouncementWithMarkdown() {
        horarios.fin();

        verify(sender).sendMarkdown(CHANNEL_CHATID,
                "⏳ ¡El canal de amistad se ha cerrado! ⏳\n\n"
                        + "No se podrá publicar hasta el siguiente horario, pero… ¡no te vas a quedar sin conocer gente! 🌙\n\n"
                        + "🤝 *Seguís pudiendo conocer personas en el Club de Amistad* de @datebobot. Entrá con /club "
                        + "y descubrí perfiles las 24 horas. ¡Nos vemos pronto! 😊");
    }

}
