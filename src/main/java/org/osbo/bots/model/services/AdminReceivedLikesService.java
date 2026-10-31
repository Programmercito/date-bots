package org.osbo.bots.model.services;

import java.util.ArrayList;
import java.util.List;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.model.entity.Like;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.repositories.LikeRepository;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.util.TelegramPresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AdminReceivedLikesService {

    public static final String COMMAND = "/likes_recibidos_admin";
    public static final String PAGE_PREFIX = "admin_likes_page_";
    private static final int PAGE_SIZE = 3;

    private final LikeRepository likeRepository;
    private final ProfileRepository profileRepository;
    private final NqueueForSend sender;
    private final String adminChatid;

    public AdminReceivedLikesService(LikeRepository likeRepository, ProfileRepository profileRepository,
            NqueueForSend sender, @Value("${telegram.admin}") String adminChatid) {
        this.likeRepository = likeRepository;
        this.profileRepository = profileRepository;
        this.sender = sender;
        this.adminChatid = adminChatid;
    }

    public boolean handle(String callerChatid, String text) {
        if (!COMMAND.equals(text) && (text == null || !text.startsWith(PAGE_PREFIX))) {
            return false;
        }
        if (!adminChatid.equals(callerChatid)) {
            sender.send(callerChatid, "🔒 Esta opción está disponible solo para administración.");
            return true;
        }

        int page = 0;
        if (!COMMAND.equals(text)) {
            String pageText = text.substring(PAGE_PREFIX.length());
            if (!pageText.matches("[0-9]{1,9}")) {
                sender.send(callerChatid, "No se pudo abrir esa página. Volvé a /likes_recibidos_admin.");
                return true;
            }
            page = Integer.parseInt(pageText);
        }
        showPage(page);
        return true;
    }

    private void showPage(int pageNumber) {
        Page<Like> likes = likeRepository.findByToChatidOrderByCreatedAtDescIdDesc(
                adminChatid, PageRequest.of(pageNumber, PAGE_SIZE));
        if (likes.isEmpty()) {
            sender.send(adminChatid, pageNumber == 0
                    ? "💌 Todavía no recibiste likes."
                    : "Esta página ya no está disponible. Volvé a /likes_recibidos_admin.");
            return;
        }

        StringBuilder text = new StringBuilder("💌 Likes que recibiste\n");
        text.append("📄 Página ").append(pageNumber + 1).append(" de ").append(likes.getTotalPages()).append("\n\n");
        for (Like like : likes) {
            Profile profile = profileRepository.findByChatid(like.getFromChatid());
            if (profile == null) {
                text.append("👤 Perfil ya no disponible\n\n");
                continue;
            }
            text.append("👤 ").append(displayValue(profile.getName()));
            Integer age = profile.getAge();
            if (age != null) {
                text.append(", ").append(age).append(" años");
            }
            text.append("\n");
            if (profile.getCity() != null && !profile.getCity().isBlank()) {
                text.append("📍 ").append(profile.getCity()).append("\n");
            }
            text.append(like.isMatched() ? "💕 Match mutuo\n" : "💌 Like recibido\n");
            String username = profile.getContactUsername();
            text.append("✈️ Telegram: ").append(username == null || username.isBlank()
                    ? "No disponible" : "@" + username).append("\n");
            text.append("📱 WhatsApp: ").append(displayValue(profile.getWhatsapp())).append("\n\n");
        }

        List<Button> navigation = new ArrayList<>();
        if (likes.hasPrevious()) {
            navigation.add(new Button("⬅️ Anterior", PAGE_PREFIX + (pageNumber - 1)));
        }
        if (likes.hasNext()) {
            navigation.add(new Button("Siguiente ➡️", PAGE_PREFIX + (pageNumber + 1)));
        }
        List<List<Button>> buttons = new ArrayList<>();
        if (!navigation.isEmpty()) {
            buttons.add(navigation);
        }
        buttons.add(List.of(new Button("🏠 Volver al inicio", "/start")));
        List<String> parts = TelegramPresentation.splitText(text.toString());
        for (int i = 0; i < parts.size() - 1; i++) {
            sender.send(adminChatid, parts.get(i));
        }
        sender.send(adminChatid, parts.get(parts.size() - 1), buttons);
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "No disponible" : value;
    }
}
