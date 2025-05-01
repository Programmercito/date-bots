package org.osbo.bots.jms.queue.receiver;

import org.osbo.bots.jms.queue.pojos.MessageSend;
import org.osbo.bots.model.entity.Message;
import org.osbo.bots.model.services.MessageService;
import org.osbo.bots.util.FechaActual;
import org.osbo.bots.util.Sleep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.EditMessageCaption;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;

@Component
public class ReceiverForSend {
    MessageService messageservice;

    ReceiverForSend(MessageService messageService) {
        this.messageservice = messageService;
    }

    @Value("${telegram.token}")
    private String token;
    @Value("${telegram.channel}")
    private String chatidchannel;

    @JmsListener(destination = "queue.send", containerFactory = "myFactory")
    public void sendMessage(MessageSend message) {
        Sleep.sleep1seg();
        System.out.println("llegando a cola de envio");
        TelegramBot bot = new TelegramBot(token);
        String destinatario = message.getChatid();
        if ("channel".equals(message.getTipo())) {
            destinatario = chatidchannel;
        }
        SendResponse response;
        if (message.getMedias() == null) {
            response = bot.execute(new SendMessage(destinatario, message.getText()));
        } else {
            SendPhoto sendphoto = new SendPhoto(destinatario, message.getMedias()[0]);
            sendphoto.caption(message.getText());
            response = bot.execute(sendphoto);
        }
        if (response.isOk()) {
            if ("channel".equals(message.getTipo())) {
                int id = response.message().messageId();
                Message msg;
                if (message.getMsgid() != null) {
                    msg = messageservice.findById(message.getMsgid());
                    msg.setEstado("publicado");
                    msg.setExpiracion(FechaActual.obtenerFechaActualConHora());
                    msg.setMessageid(String.valueOf(id));
                } else {
                    msg = new Message();
                    msg.setId(String.valueOf(id));
                    msg.setMessageid(String.valueOf(id));
                    msg.setUserid(message.getChatid());
                    msg.setTexto(message.getText());
                    msg.setEstado("publicado");
                    msg.setExpiracion(FechaActual.obtenerFechaActualConHora());
                    if (message.getMedias() != null) {
                        msg.setMedia(message.getMedias()[0]);
                    }
                }
                messageservice.save(msg);
            } else if ("aprobacion".equals(message.getTipo())) {
                int id = response.message().messageId();
                String[] partes = message.getText().split("\\|");
                Message msg = new Message();
                msg.setId(String.valueOf(id) + "_" + partes[2]);
                msg.setMessageid(String.valueOf(id));
                msg.setUserid(partes[2]);
                msg.setTexto(partes[0] + "\nPuedes escribirle a :@" + partes[1]);
                msg.setEstado("pendiente");
                msg.setExpiracion(FechaActual.obtenerFechaActualConHora());
                String add = "\n✅/aprobar_" + msg.getId() + "\n❌/rechazar_" + msg.getId() + "\n⛔/bloquear_"
                        + msg.getUserid();
                if (message.getMedias() != null) {
                    msg.setMedia(message.getMedias()[0]);
                    EditMessageCaption edit = new EditMessageCaption(message.getChatid(), id);
                    edit.caption(msg.getTexto() + add);
                    bot.execute(edit);
                } else {
                    EditMessageText edit = new EditMessageText(message.getChatid(), id, msg.getTexto() + add);
                    bot.execute(edit);
                }

                messageservice.save(msg);
            }
            System.out.println("Mensaje enviado");
        } else {
            System.out.println("Error al enviar mensaje");
        }
    }
}