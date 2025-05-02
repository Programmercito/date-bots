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
        queueForSend.send(channel, "El canal de amistad se ha abierto, escribe al bot para publicar !! @datebobot");
    }

    @Scheduled(cron = "0 0 #{T(java.lang.Integer).parseInt('${telegram.horario.fin}'.split(':')[0])} * * *")
    public void fin() {
        queueForSend.send(channel,
                "El canal de amistad se ha cerrado, no se podra publicar hasta el siguiente horario !! @datebobot mismos horarios");
    }
}