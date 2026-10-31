package org.osbo.bots.jms.queue.receiver;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.model.entity.Message;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.services.AdminBroadcastService;
import org.osbo.bots.model.services.AdminReceivedLikesService;
import org.osbo.bots.model.services.ClubDiscoveryService;
import org.osbo.bots.model.services.ClubModerationService;
import org.osbo.bots.model.services.ClubProfileEditService;
import org.osbo.bots.model.services.ClubRegistrationService;
import org.osbo.bots.model.services.LikeMatchService;
import org.osbo.bots.model.services.MessageService;
import org.osbo.bots.model.services.UserService;
import org.osbo.bots.util.FechaActual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class ReceiverForProcess {
    private static final String CHANNEL_PUBLISHED_MESSAGE =
            "🎉 ¡Tu mensaje fue enviado al canal!\n\n"
            + "💌 Ojalá sea el comienzo de una linda conexión.\n"
            + "📢 Encontralo en https://t.me/amistadbo";

    NqueueForSend sender;
    UserService userService;
    MessageService messageService;
    ClubRegistrationService clubRegistrationService;
    ClubModerationService clubModerationService;
    ClubDiscoveryService clubDiscoveryService;
    ClubProfileEditService clubProfileEditService;
    LikeMatchService likeMatchService;
    AdminBroadcastService adminBroadcastService;
    AdminReceivedLikesService adminReceivedLikesService;

    @Value("${telegram.horario.inicio}")
    private String inicio;

    @Value("${telegram.horario.fin}")
    private String fin;

    @Value("${telegram.channel}")
    private String chatidchannel;

    @Value("${telegram.aprob}")
    private String aprob;

    @Value("${telegram.admin}")
    private String adminid;

    ReceiverForProcess(NqueueForSend sender, UserService userService, MessageService messageService,
            ClubRegistrationService clubRegistrationService, ClubModerationService clubModerationService,
            ClubDiscoveryService clubDiscoveryService, ClubProfileEditService clubProfileEditService,
            LikeMatchService likeMatchService, AdminBroadcastService adminBroadcastService,
            AdminReceivedLikesService adminReceivedLikesService) {
        this.messageService = messageService;
        this.userService = userService;
        this.sender = sender;
        this.clubRegistrationService = clubRegistrationService;
        this.clubModerationService = clubModerationService;
        this.clubDiscoveryService = clubDiscoveryService;
        this.clubProfileEditService = clubProfileEditService;
        this.likeMatchService = likeMatchService;
        this.adminBroadcastService = adminBroadcastService;
        this.adminReceivedLikesService = adminReceivedLikesService;
    }

    @JmsListener(destination = "queue.process", containerFactory = "myFactory")
    public void sendMessage(MessageUpdate update) {
        System.out.println(update.getText() + " " + update.getChatid() + " " + update.getUser());

        User user = userService.findById(update.getChatid());
        if (user == null) {
            user = new User();
            user.setChatid(update.getChatid());
            user.setUser(update.getUser());
            user.setFecha_registro(FechaActual.obtenerFechaActual());
            user.setEstado("activo");
            user.setComando("start");
            user = userService.save(user);
        }
        if (user.getEstado().equals("bloqueado")) {
            sender.send(update.getChatid(),
                    "Lo sentimos, no puedes usar el bot porque tu cuenta ha sido inactivada por denuncias de otros usuarios. hasta luego");
            return;
        }
        if ("/start".equals(update.getText())) {
            showStartMenu(user, update);
            userService.save(user);
            return;
        }
        if (handleModerationCommand(update)) {
            userService.save(user);
            return;
        }
        if (adminReceivedLikesService.handle(update.getChatid(), update.getText())) {
            userService.save(user);
            return;
        }
        if (adminBroadcastService.handle(update.getChatid(), update.getText())) {
            sender.send(update.getChatid(), "Mensaje enviado.");
            userService.save(user);
            return;
        }
        if (clubRegistrationService.handleProfileAvailability(user, update)) {
            userService.save(user);
            return;
        }
        if (clubProfileEditService.handle(user, update)) {
            userService.save(user);
            return;
        }
        if (clubRegistrationService.handle(user, update)) {
            userService.save(user);
            return;
        }
        if (clubDiscoveryService.handle(user, update)) {
            userService.save(user);
            return;
        }
        if ("/mis_matches".equals(update.getText())) {
            likeMatchService.listMatches(update.getChatid());
            user.setComando("start");
            userService.save(user);
            return;
        }
        if (update.getText() != null && update.getText().startsWith("club_match_report_")) {
            String reportedChatid = update.getText().substring("club_match_report_".length());
            likeMatchService.reportMatch(update.getChatid(), reportedChatid);
            user.setComando("start");
            userService.save(user);
            return;
        }
        if (update.getText() != null && (update.getText().startsWith("matches_page_")
                || "matches_clear".equals(update.getText()))) {
            if (likeMatchService.handleMatchListCallback(user, update)) {
                userService.save(user);
                return;
            }
        }
        if ("matches_back".equals(update.getText())) {
            if (update.getMessageId() != null) {
                sender.deleteMessage(update.getChatid(), update.getMessageId());
            }
            showStartMenu(user, update);
            userService.save(user);
            return;
        }
        if ("start".equals(user.getComando())) {
            if ("/publicar".equals(update.getText()) && update.getUser() != null
                    && messageService.existsMessageInLastHour(update.getChatid()) == 0) {
                List<List<Button>> buttons = Arrays.asList(
                        Arrays.asList(new Button("❌ Cancelar", "/cancelar")));
                sender.send(user.getChatid(),
                        "💌 ¡Compartí algo sobre vos!\n\n"
                        + "Escribí tu mensaje o enviá una foto con texto para el canal de amistad de Bolivia 🇧🇴.\n\n"
                        + "⏰ La publicación estará visible durante una hora.\n"
                        + "✉️ Tu usuario de Telegram se compartirá para que puedan escribirte.\n"
                        + "📱 Agregar otro medio de contacto es opcional.\n\n"
                        + "¿Cambiaste de idea? Tocá Cancelar. ✨",
                        buttons);
                user.setComando("publicar");
            } else if ("/publicar".equals(update.getText()) && update.getUser() != null
                    && messageService.existsMessageInLastHour(update.getChatid()) > 0) {
                sender.send(user.getChatid(),
                        "⏳ Ya publicaste durante la última hora. Esperá un poquito antes de compartir otro mensaje. 💌");
            } else if ("/publicar".equals(update.getText()) && update.getUser() == null) {
                sender.send(user.getChatid(),
                        "🔑 Para publicar, primero configurá un nombre de usuario en Telegram. Después volvé a intentarlo con /publicar. 💌");
            } else if ("/ver_canal".equals(update.getText())) {
                sender.send(user.getChatid(), "Nuestro canal es: https://t.me/amistadbo");
            } else if (update.getText().startsWith("/aprobar_") && adminid.equals(update.getChatid())) {
                String[] partes = update.getText().split("_");
                String idc = partes[1] + "_" + partes[2];
                Message msg = messageService.findById(idc);
                msg.setEstado("aprobado");
                sender.sendChannel(chatidchannel, msg.getTexto(), msg.getUsername(), msg.getMedia(), idc);
                messageService.save(msg);
                sender.send(update.getChatid(), "Mensaje aprobado con exito.");
                List<List<Button>> buttons = Arrays.asList(
                        Arrays.asList(new Button("✏️ Publicar de nuevo", "/publicar"),
                                new Button("📢 Ver canal", "/ver_canal")));
                sender.send(msg.getUserid(),
                        CHANNEL_PUBLISHED_MESSAGE,
                        true, buttons);

            } else if (update.getText().startsWith("/rechazar_") && adminid.equals(update.getChatid())) {
                String[] partes = update.getText().split("_");
                Message msg = messageService.findById(partes[1] + "_" + partes[2]);
                msg.setEstado("rechazado");
                messageService.save(msg);
                List<List<Button>> buttons = Arrays.asList(
                        Arrays.asList(new Button("✏️ Publicar de nuevo", "/publicar")));
                sender.send(msg.getUserid(),
                        "💌 Tu publicación no pudo ser aprobada. Revisá su contenido y, si querés, intentá nuevamente más tarde.",
                        buttons);
                sender.send(update.getChatid(), "Mensaje rechazado con exito.");
            } else {
                sender.send(user.getChatid(),
                        "🤔 No reconocí esa opción. Tocá /start para volver al menú y descubrir todo lo que podés hacer. ✨");
            }

        } else if ("publicar".equals(user.getComando())) {
            if ("/cancelar".equals(update.getText())) {
                List<List<Button>> buttons = Arrays.asList(
                        Arrays.asList(new Button("✏️ Publicar", "/publicar")));
                sender.send(user.getChatid(),
                        "🌷 Cancelamos tu publicación. Cuando quieras, podés intentarlo de nuevo con /publicar.",
                        buttons);
                user.setComando("start");
            } else {
                if (update.getUser() == null) {
                    sender.send(user.getChatid(),
                            "🔑 Para publicar necesitás configurar un nombre de usuario en Telegram. Después volvé a intentarlo. 💌");
                } else if (!validarTiempos()) {
                    sender.send(user.getChatid(),
                            "🌙 El canal está cerrado en este momento. Intentá nuevamente durante el horario de publicación. 💌");
                } else {
                    boolean apro = aprob.equals("true") ? true : false;
                    if (!apro) {
                        String media = update.getMedias() == null || update.getMedias().length == 0
                                ? null : update.getMedias()[0];
                        sender.sendChannel(update.getChatid(), update.getText(), update.getUser(), media);
                        List<List<Button>> buttons = Arrays.asList(
                                Arrays.asList(new Button("✏️ Publicar de nuevo", "/publicar"),
                                        new Button("📢 Ver canal", "/ver_canal")));
                        sender.send(user.getChatid(),
                                CHANNEL_PUBLISHED_MESSAGE,
                                true, buttons);
                    } else {
                        String media = update.getMedias() == null ? null : update.getMedias()[0];
                        sender.send(adminid, update.getText() + "|" + update.getUser() + "|" + update.getChatid(),
                                "aprobacion", media, null);
                        List<List<Button>> pendingButtons = Arrays.asList(
                                Arrays.asList(new Button("✏️ Publicar de nuevo", "/publicar")));
                        sender.send(user.getChatid(),
                                "💌 ¡Recibimos tu publicación!\n\nEl equipo la revisará y te avisaremos cuando sea aprobada. Gracias por tu paciencia. ✨",
                                true, pendingButtons);

                    }

                }
                user.setComando("start");

            }
        } else {
            sender.send(user.getChatid(),
                    "🤔 No reconocí esa opción. Tocá /start para volver al menú. ✨");
        }
        userService.save(user);
    }

    private boolean handleModerationCommand(MessageUpdate update) {
        String text = update.getText();
        if (text == null || !adminid.equals(update.getChatid())) {
            return false;
        }
        if (text.startsWith("/aprobar_perfil_")) {
            String targetChatid = text.substring("/aprobar_perfil_".length());
            return clubModerationService.approveProfile(update.getChatid(), targetChatid);
        }
        if (text.startsWith("/rechazar_perfil_")) {
            String targetChatid = text.substring("/rechazar_perfil_".length());
            return clubModerationService.rejectProfile(update.getChatid(), targetChatid);
        }
        if (text.startsWith("/bloquear_")) {
            String targetChatid = text.substring("/bloquear_".length());
            return clubModerationService.blockUser(update.getChatid(), targetChatid);
        }
        return false;
    }

    private void showStartMenu(User user, MessageUpdate update) {
        List<List<Button>> buttons = new ArrayList<>(Arrays.asList(
                Arrays.asList(new Button("✏️ Publicar", "/publicar"),
                        new Button("📢 Ver canal", "/ver_canal")),
                Arrays.asList(new Button("🤝 Entrar al club de amistad", "/club")),
                Arrays.asList(new Button("👥 Ver personas", "/ver_personas"),
                        new Button("💕 Mis matches", "/mis_matches")),
                Arrays.asList(new Button("✏️ Editar perfil", "/editar_perfil"))));
        if (adminid.equals(update.getChatid())) {
            buttons.add(List.of(new Button("💌 Likes que recibí", AdminReceivedLikesService.COMMAND)));
        }
        sender.send(update.getChatid(),
                "🌷 ¡Bienvenid@ a nuestra comunidad de amistad en Bolivia! 🇧🇴\n\n"
                + "💌 Compartí un mensaje en el canal para que otras personas puedan conocerte.\n"
                + "🤝 O entrá al club, armá tu perfil y descubrí nuevas conexiones.\n\n"
                + "✨ Elegí una opción para comenzar:",
                true, buttons);
        user.setComando("start");
    }

    private boolean validarTiempos() {
        java.time.LocalTime horaActual = java.time.LocalTime.now();
        java.time.LocalTime horaInicio = java.time.LocalTime.parse(inicio);
        java.time.LocalTime horaFin = java.time.LocalTime.parse(fin);
        if (horaFin.isAfter(horaInicio)) {
            return !horaActual.isBefore(horaInicio) && !horaActual.isAfter(horaFin);
        } else {
            // Rango que cruza medianoche
            return !horaActual.isBefore(horaInicio) || !horaActual.isAfter(horaFin);
        }
    }
}
