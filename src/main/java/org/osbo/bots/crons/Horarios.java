package org.osbo.bots.crons;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Horarios {
    @Value("${telegram.horario.inicio}")
    private String inicio;
    @Value("${telegram.horario.fin}")
    private String fin;
    @Value("${telegram.channel}")
    private String channel;

    private final NqueueForSend queueForSend;

    public Horarios(NqueueForSend queueForSend) {
        this.queueForSend = queueForSend;
    }

    @Scheduled(cron = "0 0 #{T(java.lang.Integer).parseInt('${telegram.horario.inicio}'.split(':')[0])} * * *")
    public void inicio() {
        queueForSend.sendMarkdown(channel,
            "🎉 ¡El canal de amistad se ha abierto! 🎉\n\n"
            + "Escribe al bot @datebobot para publicar mensajes de texto, fotos, emojis y negritas. "
            + "✨ Los mensajes son efímeros: duran 1 hora y luego se borran automáticamente.\n\n"
            + "💡 *¿Querés conocer gente de forma más privada?* Unite al *Club de Amistad* 🤝 del bot y descubrí perfiles de personas que buscan lo mismo que vos. ¡Es una comunidad aparte dentro del canal! 🥰");
    }

    @Scheduled(cron = "0 0 #{T(java.lang.Integer).parseInt('${telegram.horario.fin}'.split(':')[0])} * * *")
    public void fin() {
        queueForSend.sendMarkdown(channel,
            "⏳ ¡El canal de amistad se ha cerrado! ⏳\n\n"
            + "No se podrá publicar hasta el siguiente horario, pero… ¡no te vas a quedar sin conocer gente! 🌙\n\n"
            + "🤝 *Seguís pudiendo conocer personas en el Club de Amistad* de @datebobot. Entrá con /club y descubrí perfiles las 24 horas. ¡Nos vemos pronto! 😊");
    }
}