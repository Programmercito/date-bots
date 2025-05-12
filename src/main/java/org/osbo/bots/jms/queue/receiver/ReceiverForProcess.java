package org.osbo.bots.jms.queue.receiver;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.model.entity.Message;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.services.MessageService;
import org.osbo.bots.model.services.UserService;
import org.osbo.bots.util.CustomProperties;
import org.osbo.bots.util.FechaActual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class ReceiverForProcess {
    NqueueForSend sender;
    UserService userService;
    MessageService messageService;

    @Value("${telegram.horario.inicio}")
    private String inicio;

    @Value("${telegram.horario.fin}")
    private String fin;

    @Value("${telegram.channel}")
    private String chatidchannel;

    @Value("${telegram.admin}")
    private String adminid;

    ReceiverForProcess(NqueueForSend sender, UserService userService, MessageService messageService) {
        this.messageService = messageService;
        this.userService = userService;
        this.sender = sender;

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
        } else {
            user.setUser(update.getUser());
            userService.save(user);
        }
        if (user.getEstado().equals("bloqueado")) {
            sender.send(update.getChatid(),
                    "Lo sentimos, no puedes usar el bot porque tu cuenta ha sido inactivada por denuncias de otros usuarios. hasta luego");
            return;
        }
        if ("start".equals(user.getComando())) {
            if ("/start".equals(update.getText())) {
                sender.send(update.getChatid(),
                        "¡Hola! 😃✨ ¡Bienvenido/a al bot de amistad! 💖 Este chat es exclusivo para personas de Bolivia 🇧🇴. Aquí puedes conocer personas increíbles y hacer nuevos amigos. Si quieres compartir un mensaje en nuestro canal de amistad, solo escribe /publicar. ¡Atrévete a dar el primer paso y vive nuevas experiencias! 💬🤗🎉🥰, nuestro canal es : https://t.me/amistadbo");
                user.setComando("start");
            } else if ("/publicar".equals(update.getText()) && update.getUser() != null
                    && messageService.existsMessageInLastHour(update.getChatid()) == 0) {
                sender.send(user.getChatid(),
                        "¡Genial! 🎉🥳✨ Escribe el mensaje que te gustaría compartir en el canal de amistad. Recuerda que este chat es solo para bolivianos 🇧🇴. Tu mensaje estará visible durante una hora ⏰ y tu usuario de Telegram será compartido automáticamente para que otros puedan contactarte 🤝💌. Si quieres, puedes cambiar tu usuario de Telegram desde la app antes de publicar. No es necesario incluir otro medio de comunicación, pero si lo deseas, puedes agregar tu número de celular 📱☎️ u otro medio en el mensaje. ¡Esta es tu oportunidad para encontrar nuevas amistades! 🌟💫 Si cambias de opinión, puedes escribir /cancelar. ¡Estamos emocionados de leerte! 😄🙌🎈");
                user.setComando("publicar");
            } else if ("/publicar".equals(update.getText()) && update.getUser() != null
                    && messageService.existsMessageInLastHour(update.getChatid()) > 0) {
                sender.send(user.getChatid(),
                        "¡Ups! 😅🚫 Ya has publicado un mensaje en la última hora. Por favor, espera un poco más antes de volver a publicar. ¡No te desanimes! Tu oportunidad de hacer nuevos amigos llegará pronto. 💖🤞🌟✨ ");
            } else if ("/publicar".equals(update.getText()) && update.getUser() == null) {
                sender.send(user.getChatid(),
                        "¡Oops! 😅🚫 Necesitas configurar un nombre de usuario en Telegram para poder publicar. ¡Es súper fácil! 🤩 Solo ve a la app de Telegram ➡️ Configuración ⚙️ y crea tu @usuario. ¡Así todos podrán encontrarte! 🔍✨ Una vez configurado, regresa y comparte tu mensaje. ¡Estamos deseando conocer nuevos amigos contigo! 💫💪🌈 Si prefieres no hacerlo ahora, escribe /cancelar. ¡Te esperamos con los brazos abiertos! 🤗💝");
                user.setComando("publicar");
            } else if (update.getText().startsWith("/aprobar_") && adminid.equals(update.getChatid())) {
                String[] partes = update.getText().split("_");
                String idc = partes[1] + "_" + partes[2];
                Message msg = messageService.findById(idc);
                msg.setEstado("aprobado");
                sender.sendChannel(chatidchannel, msg.getTexto(), null, msg.getMedia(), idc);
                messageService.save(msg);
                sender.send(update.getChatid(), "¡Perfecto! ✅ Mensaje aprobado y publicado con éxito. 🌟 ¡Gracias por mantener nuestra comunidad activa y segura! 🛡️😊");
                sender.send(msg.getUserid(),
                        "¡GENIAL! 🎊🎉🥳 Tu mensaje acaba de ser publicado en nuestro canal de amistad. 📣 ¡Es tu momento de brillar y conocer personas maravillosas! ✨👫👭👬 Esperamos que vivas experiencias increíbles y hagas conexiones especiales. 💖 Si quieres volver a publicar en otro momento, solo escribe /publicar. ¡La aventura de hacer nuevos amigos te espera! 🚀🌈🤩\n\n¡Visita ahora nuestro canal y mira tu mensaje! 👉 https://t.me/amistadbo 👈");

            } else if (update.getText().startsWith("/admin") && adminid.equals(update.getChatid())) {
                String aprob = CustomProperties.getProperty("telegram.aprob");
                boolean apro = aprob.equals("true") ? true : false;
                if (apro) {
                    sender.send(update.getChatid(), "¡Configuración actualizada! ⚙️✨ Has cambiado el modo de aprobación a MODERADO 🛡️🔍. Ahora todos los mensajes pasarán por tu revisión antes de ser publicados. ¡Control total activado! 💯🔐");
                    sender.send(chatidchannel, "⚠️ AVISO IMPORTANTE DE LA ADMINISTRACIÓN ⚠️\n\n¡El modo de fotos ha sido DESACTIVADO temporalmente! 🚫📸\n\nPor ahora, solo se permiten mensajes de texto. 📝💬\n\nRecuerda seguir nuestras normas comunitarias para mantener un espacio amigable y respetuoso para todos. 🤝❤️\n\n¡Gracias por ser parte de nuestra comunidad! 🌟😊");
                    CustomProperties.setProperty("telegram.aprob", "false");
                    CustomProperties.save();
                } else {
                    sender.send(update.getChatid(), "¡Genial! 🎉 Se ha cambiado el modo de aprobación a directo ✅. ¡Los mensajes se publicarán instantáneamente! 🚀");
                    sender.send(chatidchannel, "¡BUENAS NOTICIAS! 🤩 Se ha ACTIVADO el modo de fotos permitidas 📸✨. ¡Ahora puedes enviar tu mensaje junto con una foto y será publicada en el canal! 🌟 Recuerda que debes cumplir con las normas de la comunidad 📝👍");
                    CustomProperties.setProperty("telegram.aprob", "true");
                    CustomProperties.save();
                }

            } else if (update.getText().startsWith("/rechazar_") && adminid.equals(update.getChatid())) {
                String[] partes = update.getText().split("_");
                Message msg = messageService.findById(partes[1] + "_" + partes[2]);
                msg.setEstado("rechazado");
                messageService.save(msg);
                sender.send(msg.getUserid(),
                        "Lo sentimos 😔, tu mensaje ha sido rechazado por los administradores. ⚠️ Ten cuidado con lo que solicitas o podrías ser bloqueado. 🚫 ¡Puedes volver a intentarlo más tarde con un mensaje apropiado! 💪🔄");
                sender.send(update.getChatid(), "¡Mensaje rechazado con éxito! 🛑✅");
            } else if (update.getText().startsWith("/bloquear_") && adminid.equals(update.getChatid())) {
                String[] partes = update.getText().split("_");
                User us = userService.findById(partes[1]);
                us.setEstado("bloqueado");
                userService.save(us);
                sender.send(update.getChatid(), "Usuario bloqueado con exito.");
            } else {
                sender.send(user.getChatid(),
                        "Comando no reconocido 😅❓. Puedes escribir /start para comenzar o /publicar para compartir un mensaje en el canal de amistad. Recuerda que este chat es solo para bolivianos 🇧🇴. ¡Anímate a participar y haz nuevos amigos! ✨🙌🎊💖");
            }

        } else if ("publicar".equals(user.getComando())) {
            if ("/cancelar".equals(update.getText())) {
                sender.send(user.getChatid(),
                        "¡No hay problema! 😊👍 Tu publicación ha sido cancelada. Si quieres intentarlo de nuevo, solo escribe /publicar. ¡Estamos aquí para ayudarte a conectar con nuevas personas y vivir momentos geniales! 🌈🤩🎉💬");
                user.setComando("start");
            } else {
                if (!validarTiempos()) {
                    sender.send(user.getChatid(),
                            "¡Ups! 😅⏳ En este momento no es posible publicar mensajes. Por favor, intenta más tarde. ¡No te desanimes, tu oportunidad de hacer nuevos amigos llegará pronto! 💖🤞🌟✨");
                } else {
                    String aprob = CustomProperties.getProperty("telegram.aprob");
                    boolean apro = aprob.equals("true") ? true : false;
                    if (!apro) {
                        // String media = update.getMedias() == null ? null : update.getMedias()[0];
                        sender.sendChannel(update.getChatid(), update.getText(), update.getUser(), null);
                        sender.send(user.getChatid(),
                                "¡Listo! 🎊🙌🥳 Tu mensaje ha sido publicado en el canal de amistad. ¡Esperamos que encuentres personas increíbles y vivas nuevas experiencias! Si quieres volver a publicar, solo escribe /publicar. ¡Suerte y que la amistad te acompañe! 🥰🌟💬💖\n\nPuedes ver tu mensaje y los de otros en nuestro canal: https://t.me/amistadbo");
                    } else {
                        String media = update.getMedias() == null ? null : update.getMedias()[0];
                        sender.send(adminid, update.getText() + "|" + update.getUser() + "|" + update.getChatid(),
                                "aprobacion", media, null);
                        sender.send(user.getChatid(),
                                "Tu mensaje ha sido enviado a los administradores para su revisión. Te avisaremos cuando sea aprobado. ¡Gracias por tu paciencia! 🙏😊✨ Si quieres volver a publicar, solo escribe /publicar. ¡Estamos aquí para ayudarte a conectar con nuevas personas y vivir momentos geniales! 🌈🤩🎉💬");

                    }

                }
                user.setComando("start");

            }
        } else {
            sender.send(user.getChatid(),
                    "Comando no reconocido 😅❓. Puedes escribir /start para comenzar o /publicar para compartir un mensaje en el canal de amistad. ¡Anímate a participar y haz nuevos amigos! ✨🙌🎊💖");
        }
        userService.save(user);
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
