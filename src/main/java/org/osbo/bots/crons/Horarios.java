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
        queueForSend.send(channel, 
            "🎉✨ ¡CANAL DE AMISTAD ABIERTO! ✨🎉\n\n"
            + "💫 ¡Es hora de conocer personas increíbles! 💫\n\n"
            + "👉 Escribe al bot @datebobot para publicar tu mensaje y conectar con nuevos amigos 👈\n\n"
            + "⏱️ Recuerda que los mensajes son efímeros y duran solo 1 hora ⏱️\n\n"
            + "🌟 ¡No pierdas esta oportunidad de hacer nuevas conexiones! 🌟\n\n"
            + "¡Anímate y comparte tu mensaje ahora mismo! 🤩💬❤️");
    }

    @Scheduled(cron = "0 0 #{T(java.lang.Integer).parseInt('${telegram.horario.fin}'.split(':')[0])} * * *")
    public void fin() {
        queueForSend.send(channel, 
            "⏰ ¡CANAL DE AMISTAD CERRADO POR HOY! ⏰\n\n"
            + "😴 El periodo de publicación ha terminado, pero no te preocupes... ¡volveremos pronto! 😴\n\n"
            + "💭 Mientras tanto, puedes chatear con las personas que ya conociste 💭\n\n"
            + "👉 Recuerda seguir a @datebobot para no perderte cuando se abra de nuevo 👈\n\n"
            + "📝 Los mensajes actuales se borrarán automáticamente en una hora 📝\n\n"
            + "✨ ¡Gracias por ser parte de nuestra comunidad! ¡Hasta pronto! ✨❤️🌙");
    }
}