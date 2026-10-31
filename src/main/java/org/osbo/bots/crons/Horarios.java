package org.osbo.bots.crons;

import java.util.List;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.model.services.ClubStatisticsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Horarios {
    @Value("${telegram.horario.inicio}")
    private String inicio;
    @Value("${telegram.horario.fin}")
    private String fin;
    @Value("${telegram.channel}")
    private String channel;

    private final NqueueForSend queueForSend;
    private final ClubStatisticsService clubStatisticsService;

    public Horarios(NqueueForSend queueForSend, ClubStatisticsService clubStatisticsService) {
        this.queueForSend = queueForSend;
        this.clubStatisticsService = clubStatisticsService;
    }

    @Scheduled(cron = "0 0 #{T(java.lang.Integer).parseInt('${telegram.horario.inicio}'.split(':')[0])} * * *")
    public void inicio() {
        queueForSend.sendMarkdown(channel,
            "🎉 *¡El canal de amistad está abierto!* 🇧🇴\n\n"
            + "💌 Escribí a @datebobot para compartir un mensaje o una foto.\n"
            + "⏰ Las publicaciones duran una hora y luego se borran automáticamente.\n\n"
            + clubPromotion(), clubButton());
    }

    @Scheduled(cron = "0 0 #{T(java.lang.Integer).parseInt('${telegram.horario.fin}'.split(':')[0])} * * *")
    public void fin() {
        queueForSend.sendMarkdown(channel,
            "🌙 *El canal de amistad está cerrado por ahora.*\n\n"
            + "Podrás volver a publicar en el próximo horario. 💌\n\n"
            + clubPromotion(), clubButton());
    }

    private String clubPromotion() {
        String countMessage;
        try {
            long count = clubStatisticsService.countDiscoverableProfiles();
            String label = count == 1 ? "perfil aprobado disponible" : "perfiles aprobados disponibles";
            countMessage = "👥 Actualmente hay *" + count + " " + label + "*.";
        } catch (DataAccessException e) {
            log.error("Unable to count discoverable club profiles", e);
            countMessage = "👥 El conteo de perfiles no está disponible en este momento.";
        }
        return "🤝 *¡El Club de Amistad te espera!*\n"
                + countMessage + "\n\n"
                + "✨ Descubrí nuevas conexiones en el club, disponible las 24 horas.\n"
                + "💬 Tocá el botón y escribí /club para unirte.";
    }

    private List<List<Button>> clubButton() {
        return List.of(List.of(new Button("🤝 Abrir el bot", null, "https://t.me/datebobot")));
    }
}