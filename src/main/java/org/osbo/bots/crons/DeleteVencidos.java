package org.osbo.bots.crons;

import org.osbo.bots.model.services.MessageService;
import org.osbo.bots.util.Sleep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class DeleteVencidos {
    private final MessageService messageService;

    @Value("${telegram.token}")
    private String token;
    @Value("${telegram.channel}")
    private String chatidchannel;

    public DeleteVencidos(MessageService messageService) {
        this.messageService = messageService;
    }

    @Scheduled(fixedRate = 60000)
    public void execute() {
        log.info("Ejecutando tarea programada para eliminar mensajes vencidos.");
        messageService.vencidos().forEach(m -> {
            Sleep.sleep1seg();
            System.out.println("empezando a borrar eliminados");
            TelegramBot bot = new TelegramBot(token);
            DeleteMessage delete = new DeleteMessage(chatidchannel, Integer.parseInt(m.getMessageid()));
            bot.execute(delete);
            m.setEstado("terminado");
            messageService.save(m);
            SendMessage send = new SendMessage(m.getUserid(),
                    "El mensaje ha sido eliminado por el bot, ya que ha pasado su tiempo de expiracion.");
            bot.execute(send);
        });
    }

}
