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
    @Value("${telegram.admin}")
    private String adminid;
    private String i;
    private String f;

    NqueueForSend queueForSend;

    public Horarios(NqueueForSend queueForSend) {
        this.queueForSend = queueForSend;
    }

    Horarios() {
        this.i = inicio.split(":")[0];
        this.f = fin.split(":")[0];
    }

    @Scheduled(cron = "0 0 ${i} * * *")
    public void inicio() {
        queueForSend.send(adminid, "El canal de amistad se ha abierto, escribe al bot para publicar !! @datebobot");
    }

    @Scheduled(cron = "0 0 ${f} * * *")
    public void fin() {
        queueForSend.send(adminid,
                "El canal de amistad se ha cerrado, no se podra publicar hasta el siguiente horario !! @datebobot");
    }

}
