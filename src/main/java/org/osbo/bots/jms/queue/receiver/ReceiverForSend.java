package org.osbo.bots.jms.queue.receiver;

import java.util.List;

import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.MessageSend;
import org.osbo.bots.model.entity.Message;
import org.osbo.bots.model.services.MessageService;
import org.osbo.bots.util.FechaActual;
import org.osbo.bots.util.Sleep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
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
        if ("callback".equals(message.getTipo())) {
            bot.execute(new AnswerCallbackQuery(message.getCallbackQueryId()));
            return;
        }

        InlineKeyboardMarkup markup = buildMarkup(message.getButtons());
        SendResponse response;
        if (message.getMedias() == null) {
            SendMessage sendMessage = new SendMessage(destinatario, message.getText());
            sendMessage.disableNotification(message.isDisableNotification());
            if (markup != null) {
                sendMessage.replyMarkup(markup);
            }
            response = bot.execute(sendMessage);
        } else {
            SendPhoto sendphoto = new SendPhoto(destinatario, message.getMedias()[0]);
            sendphoto.caption(message.getText());
            sendphoto.disableNotification(message.isDisableNotification());
            if (markup != null) {
                sendphoto.replyMarkup(markup);
            }
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
                msg.setUsername(partes[1]);
                msg.setTexto(partes[0]);
                msg.setEstado("pendiente");
                msg.setExpiracion(FechaActual.obtenerFechaActualConHora());
                if (message.getMedias() != null) {
                    msg.setMedia(message.getMedias()[0]);
                }

                String adminText = partes[0] + "\nDe: @" + partes[1];
                List<List<Button>> buttons = List.of(
                        List.of(new Button("✅ Aprobar", "/aprobar_" + msg.getId(), null),
                                new Button("❌ Rechazar", "/rechazar_" + msg.getId(), null),
                                new Button("⛔ Bloquear", "/bloquear_" + msg.getUserid(), null)));
                InlineKeyboardMarkup markup = buildMarkup(buttons);

                if (message.getMedias() != null) {
                    EditMessageCaption edit = new EditMessageCaption(message.getChatid(), id);
                    edit.caption(adminText);
                    if (markup != null) {
                        edit.replyMarkup(markup);
                    }
                    bot.execute(edit);
                } else {
                    EditMessageText edit = new EditMessageText(message.getChatid(), id, adminText);
                    if (markup != null) {
                        edit.replyMarkup(markup);
                    }
                    bot.execute(edit);
                }

                messageservice.save(msg);
            }
            System.out.println("Mensaje enviado");
        } else {
            System.out.println("Error al enviar mensaje");
        }
    }

    private InlineKeyboardMarkup buildMarkup(List<List<Button>> buttons) {
        if (buttons == null || buttons.isEmpty()) {
            return null;
        }
        InlineKeyboardButton[][] keyboard = new InlineKeyboardButton[buttons.size()][];
        for (int i = 0; i < buttons.size(); i++) {
            List<Button> row = buttons.get(i);
            keyboard[i] = new InlineKeyboardButton[row.size()];
            for (int j = 0; j < row.size(); j++) {
                Button button = row.get(j);
                InlineKeyboardButton inlineButton = new InlineKeyboardButton(button.getText());
                if (button.getUrl() != null) {
                    inlineButton.url(button.getUrl());
                } else {
                    inlineButton.callbackData(button.getCallbackData());
                }
                keyboard[i][j] = inlineButton;
            }
        }
        return new InlineKeyboardMarkup(keyboard);
    }
}