package org.osbo.bots.jms.queue.receiver;

import java.time.OffsetDateTime;
import java.util.List;

import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.MessageSend;
import org.osbo.bots.model.entity.Message;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.services.MessageService;
import org.osbo.bots.model.services.UserService;
import org.osbo.bots.util.FechaActual;
import org.osbo.bots.util.Sleep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageCaption;
import com.pengrad.telegrambot.request.EditMessageMedia;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.MessagesResponse;
import com.pengrad.telegrambot.response.SendResponse;

@Component
public class ReceiverForSend {
    MessageService messageservice;
    UserService userService;
    ProfileRepository profileRepository;

    private TelegramBot telegramBot;

    ReceiverForSend(MessageService messageService, UserService userService, ProfileRepository profileRepository) {
        this.messageservice = messageService;
        this.userService = userService;
        this.profileRepository = profileRepository;
    }

    @Value("${telegram.token}")
    private String token;
    @Value("${telegram.channel}")
    private String chatidchannel;

    private TelegramBot getTelegramBot() {
        if (telegramBot == null) {
            telegramBot = new TelegramBot(token);
        }
        return telegramBot;
    }

    void setTelegramBotForTest(TelegramBot bot) {
        this.telegramBot = bot;
    }

    @JmsListener(destination = "queue.send", containerFactory = "myFactory")
    public void sendMessage(MessageSend message) {
        Sleep.sleep1seg();
        System.out.println("llegando a cola de envio");
        TelegramBot bot = getTelegramBot();
        String destinatario = message.getChatid();
        if ("channel".equals(message.getTipo())) {
            destinatario = chatidchannel;
        }
        if ("callback".equals(message.getTipo())) {
            bot.execute(new AnswerCallbackQuery(message.getCallbackQueryId()));
            return;
        }

        InlineKeyboardMarkup markup = buildMarkup(message.getButtons());

        if ("edit_text".equals(message.getTipo())) {
            EditMessageText edit = new EditMessageText(message.getChatid(), message.getMessageId(), message.getText());
            applyParseMode(edit, message.getParseMode());
            if (markup != null) {
                edit.replyMarkup(markup);
            }
            bot.execute(edit);
            return;
        }

        if ("edit_caption".equals(message.getTipo())) {
            EditMessageCaption edit = new EditMessageCaption(message.getChatid(), message.getMessageId());
            edit.caption(message.getText());
            applyParseMode(edit, message.getParseMode());
            if (markup != null) {
                edit.replyMarkup(markup);
            }
            bot.execute(edit);
            return;
        }

        if ("edit_photo_caption".equals(message.getTipo())) {
            InputMediaPhoto media = new InputMediaPhoto(message.getMedias()[0]).caption(message.getText());
            applyParseMode(media, message.getParseMode());
            EditMessageMedia edit = new EditMessageMedia(message.getChatid(), message.getMessageId(), media);
            if (markup != null) {
                edit.replyMarkup(markup);
            }
            if (!bot.execute(edit).isOk()) {
                handleBrokenDiscoveryPhoto(message, bot);
            }
            return;
        }

        if ("delete".equals(message.getTipo())) {
            bot.execute(new DeleteMessage(message.getChatid(), message.getMessageId()));
            return;
        }

        if ("media_group".equals(message.getTipo())) {
            handleMediaGroup(message, bot);
            return;
        }

        SendResponse response;
        boolean hasPhoto = message.getMedias() != null && message.getMedias().length > 0
                && message.getMedias()[0] != null && !message.getMedias()[0].isBlank();
        if (!hasPhoto) {
            SendMessage sendMessage = new SendMessage(destinatario, message.getText());
            sendMessage.disableNotification(message.isDisableNotification());
            applyParseMode(sendMessage, message.getParseMode());
            if (markup != null) {
                sendMessage.replyMarkup(markup);
            }
            response = bot.execute(sendMessage);
        } else {
            SendPhoto sendphoto = new SendPhoto(destinatario, message.getMedias()[0]);
            sendphoto.caption(message.getText());
            sendphoto.disableNotification(message.isDisableNotification());
            applyParseMode(sendphoto, message.getParseMode());
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
                List<List<Button>> adminButtons = List.of(
                        List.of(new Button("✅ Aprobar", "/aprobar_" + msg.getId(), null),
                                new Button("❌ Rechazar", "/rechazar_" + msg.getId(), null),
                                new Button("⛔ Bloquear", "/bloquear_" + msg.getUserid(), null)));
                InlineKeyboardMarkup adminMarkup = buildMarkup(adminButtons);

                if (message.getMedias() != null) {
                    EditMessageCaption edit = new EditMessageCaption(message.getChatid(), id);
                    edit.caption(adminText);
                    if (adminMarkup != null) {
                        edit.replyMarkup(adminMarkup);
                    }
                    bot.execute(edit);
                } else {
                    EditMessageText edit = new EditMessageText(message.getChatid(), id, adminText);
                    if (adminMarkup != null) {
                        edit.replyMarkup(adminMarkup);
                    }
                    bot.execute(edit);
                }

                messageservice.save(msg);
            } else if ("discovery_profile".equals(message.getTipo()) || "discovery_empty".equals(message.getTipo())
                    || "discovery_buttons".equals(message.getTipo())) {
                int id = response.message().messageId();
                User user = userService.findById(message.getChatid());
                if (user != null) {
                    user.setCurrentProfileMessageId(id);
                    userService.save(user);
                }
            }
            System.out.println("Mensaje enviado");
        } else {
            System.out.println("Error al enviar mensaje");
            if ("discovery_profile".equals(message.getTipo())) {
                handleBrokenDiscoveryPhoto(message, bot);
            } else {
                sendFallbackTextMessage(message, bot, destinatario);
            }
        }
    }

    private void handleMediaGroup(MessageSend message, TelegramBot bot) {
        String[] photoIds = message.getMedias();
        if (photoIds == null || photoIds.length == 0) {
            return;
        }
        InputMediaPhoto[] medias = new InputMediaPhoto[photoIds.length];
        for (int i = 0; i < photoIds.length; i++) {
            medias[i] = new InputMediaPhoto(photoIds[i]);
        }
        SendMediaGroup request = new SendMediaGroup(message.getChatid(), medias);
        MessagesResponse response = bot.execute(request);
        if (response.isOk() && response.messages() != null) {
            StringBuilder ids = new StringBuilder();
            for (com.pengrad.telegrambot.model.Message msg : response.messages()) {
                if (ids.length() > 0) ids.append("|");
                ids.append(msg.messageId());
            }
            User user = userService.findById(message.getChatid());
            if (user != null) {
                user.setMediaGroupMessageIds(ids.toString());
                userService.save(user);
            }
        } else if (!response.isOk()) {
            // Fall back: deactivate profile with broken photo
            String targetChatid = message.getTargetProfileChatid();
            if (targetChatid != null && !targetChatid.isBlank()) {
                Profile profile = profileRepository.findByChatid(targetChatid);
                if (profile != null) {
                    profile.setStatus("REJECTED");
                    profile.setUpdatedAt(OffsetDateTime.now().toString());
                    profileRepository.save(profile);
                    bot.execute(new SendMessage(targetChatid,
                            "Tu foto de perfil no pudo enviarse. Tu perfil fue desactivado del club. Si querés volver, escribí /club."));
                }
                bot.execute(new SendMessage(message.getChatid(),
                        "No se pudo mostrar un perfil. Continuá con /ver_personas."));
            }
        }
    }

    private void sendFallbackTextMessage(MessageSend message, TelegramBot bot) {
        sendFallbackTextMessage(message, bot, message.getChatid());
    }

    private void sendFallbackTextMessage(MessageSend message, TelegramBot bot, String chatid) {
        SendMessage fallback = new SendMessage(chatid, message.getText());
        if (message.getButtons() != null && !message.getButtons().isEmpty()) {
            fallback.replyMarkup(buildMarkup(message.getButtons()));
        }
        applyParseMode(fallback, message.getParseMode());
        bot.execute(fallback);
    }

    private void handleBrokenDiscoveryPhoto(MessageSend message, TelegramBot bot) {
        String targetChatid = message.getTargetProfileChatid();
        if (targetChatid == null || targetChatid.isBlank()) {
            return;
        }
        Profile profile = profileRepository.findByChatid(targetChatid);
        if (profile == null) {
            return;
        }
        profile.setStatus("REJECTED");
        profile.setUpdatedAt(OffsetDateTime.now().toString());
        profileRepository.save(profile);

        String ownerNotification = "Tu foto de perfil no pudo enviarse. Tu perfil fue desactivado del club. Si querés volver, escribí /club.";
        bot.execute(new SendMessage(targetChatid, ownerNotification));

        String viewerChatid = message.getChatid();
        if (viewerChatid != null && !viewerChatid.equals(targetChatid)) {
            bot.execute(new SendMessage(viewerChatid,
                    "No se pudo mostrar un perfil. Continuá con /ver_personas."));
        }
    }

    private void applyParseMode(SendMessage request, String parseMode) {
        if (parseMode == null) {
            return;
        }
        request.parseMode(ParseMode.valueOf(parseMode));
    }

    private void applyParseMode(SendPhoto request, String parseMode) {
        if (parseMode == null) {
            return;
        }
        request.parseMode(ParseMode.valueOf(parseMode));
    }

    private void applyParseMode(EditMessageText request, String parseMode) {
        if (parseMode == null) {
            return;
        }
        request.parseMode(ParseMode.valueOf(parseMode));
    }

    private void applyParseMode(EditMessageCaption request, String parseMode) {
        if (parseMode == null) {
            return;
        }
        request.parseMode(ParseMode.valueOf(parseMode));
    }

    private void applyParseMode(InputMediaPhoto media, String parseMode) {
        if (parseMode == null) {
            return;
        }
        media.parseMode(ParseMode.valueOf(parseMode));
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