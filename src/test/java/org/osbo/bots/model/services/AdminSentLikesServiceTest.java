package org.osbo.bots.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.model.entity.Like;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.repositories.LikeRepository;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class AdminSentLikesServiceTest {

    private static final String ADMIN = "admin-999";
    private static final String OTHER = "user-123";
    private LikeRepository likeRepository;
    private ProfileRepository profileRepository;
    private NqueueForSend sender;
    private AdminSentLikesService service;

    @BeforeEach
    void setUp() {
        likeRepository = mock(LikeRepository.class);
        profileRepository = mock(ProfileRepository.class);
        sender = mock(NqueueForSend.class);
        service = new AdminSentLikesService(likeRepository, profileRepository, sender, ADMIN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/likes_enviados_admin", "/likes_recibidos_admin",
            "admin_likes_page_0", "admin_likes_page_1"})
    void shouldRejectOtherUsersBeforeQueryingAnyContacts(String text) {
        assertThat(service.handle(OTHER, text)).isTrue();

        verifyNoInteractions(likeRepository, profileRepository);
        verify(sender).send(OTHER, "🔒 Esta opción está disponible solo para administración.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldShowOnlySentLikesIncludingUnmatchedAndMatchedContacts() {
        Like unmatched = like("alice", false);
        Like matched = like("bea", true);
        when(likeRepository.findByFromChatidOrderByCreatedAtDescIdDesc(ADMIN, PageRequest.of(0, 3)))
                .thenReturn(new PageImpl<>(List.of(unmatched, matched), PageRequest.of(0, 3), 2));
        Profile alice = profile("Alicia_*", "alice_user", "+59170012345");
        alice.setAge(25);
        alice.setCity("La Paz");
        when(profileRepository.findByChatid("alice")).thenReturn(alice);
        when(profileRepository.findByChatid("bea")).thenReturn(profile("Bea", null, null));

        assertThat(service.handle(ADMIN, AdminSentLikesService.COMMAND)).isTrue();

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttons = ArgumentCaptor.forClass(List.class);
        verify(sender).send(eq(ADMIN), message.capture(), buttons.capture());
        assertThat(message.getValue()).contains("Personas a las que diste like", "Alicia_*, 25 años", "📍 La Paz",
                "💌 Like enviado", "@alice_user", "+59170012345",
                "Bea", "💕 Match mutuo", "Telegram: No disponible", "WhatsApp: No disponible");
        assertThat(buttons.getValue().stream().flatMap(List::stream).map(Button::getCallbackData))
                .containsExactly("/start");
        verify(likeRepository).findByFromChatidOrderByCreatedAtDescIdDesc(ADMIN, PageRequest.of(0, 3));
        verify(profileRepository).findByChatid("alice");
        verify(profileRepository).findByChatid("bea");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNavigateSentLikesPagesWithoutAcceptingAnotherTarget() {
        Like like = like("alice", false);
        when(likeRepository.findByFromChatidOrderByCreatedAtDescIdDesc(ADMIN, PageRequest.of(1, 3)))
                .thenReturn(new PageImpl<>(List.of(like), PageRequest.of(1, 3), 7));
        when(profileRepository.findByChatid("alice")).thenReturn(profile("Alicia", "alice", null));

        assertThat(service.handle(ADMIN, "admin_likes_page_1")).isTrue();

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttons = ArgumentCaptor.forClass(List.class);
        verify(sender).send(eq(ADMIN), text.capture(), buttons.capture());
        assertThat(text.getValue()).contains("Página 2 de 3", "@alice");
        assertThat(buttons.getValue().stream().flatMap(List::stream).map(Button::getCallbackData))
                .containsExactly("admin_likes_page_0", "admin_likes_page_2", "/start");
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin_likes_page_", "admin_likes_page_-1", "admin_likes_page_9999999999",
            "admin_likes_page_1_other-user", "admin_likes_page_0_user-123"})
    void shouldRejectInvalidPageCallbacksWithoutQuerying(String text) {
        assertThat(service.handle(ADMIN, text)).isTrue();

        verifyNoInteractions(likeRepository, profileRepository);
        verify(sender).send(eq(ADMIN), org.mockito.ArgumentMatchers.contains("No se pudo abrir"));
    }

    @Test
    void shouldHandleNoLikesAndMissingProfiles() {
        when(likeRepository.findByFromChatidOrderByCreatedAtDescIdDesc(ADMIN, PageRequest.of(0, 3)))
                .thenReturn(Page.empty());
        assertThat(service.handle(ADMIN, AdminSentLikesService.COMMAND)).isTrue();
        verify(sender).send(ADMIN, "💌 Todavía no diste likes.");
        verifyNoInteractions(profileRepository);
    }

    @Test
    void shouldNotRevealIdentifiersForDeletedProfiles() {
        Like like = like("deleted-user", false);
        when(likeRepository.findByFromChatidOrderByCreatedAtDescIdDesc(ADMIN, PageRequest.of(0, 3)))
                .thenReturn(new PageImpl<>(List.of(like), PageRequest.of(0, 3), 1));

        service.handle(ADMIN, AdminSentLikesService.COMMAND);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(ADMIN), message.capture(), any(List.class));
        assertThat(message.getValue()).contains("Perfil ya no disponible").doesNotContain("deleted-user");
    }

    @Test
    void shouldSplitLongContactDetailsAndKeepNavigation() {
        Like like = like("alice", false);
        when(likeRepository.findByFromChatidOrderByCreatedAtDescIdDesc(ADMIN, PageRequest.of(0, 3)))
                .thenReturn(new PageImpl<>(List.of(like), PageRequest.of(0, 3), 1));
        when(profileRepository.findByChatid("alice")).thenReturn(profile("Alicia", "alice", "7".repeat(4090)));

        service.handle(ADMIN, AdminSentLikesService.COMMAND);

        ArgumentCaptor<String> first = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> last = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(ADMIN), first.capture());
        verify(sender).send(eq(ADMIN), last.capture(), any(List.class));
        verifyNoMoreInteractions(sender);
        assertThat(first.getValue().length()).isLessThanOrEqualTo(4096);
        assertThat(last.getValue().length()).isLessThanOrEqualTo(4096);
        assertThat(first.getValue() + last.getValue()).contains("@alice", "7".repeat(4090));
    }

    @Test
    void shouldIgnoreOtherCommands() {
        assertThat(service.handle(ADMIN, "/club")).isFalse();
        assertThat(service.handle(ADMIN, null)).isFalse();
        verifyNoInteractions(likeRepository, profileRepository, sender);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/likes_enviados_admin user-123", "/likes_recibidos_admin user-123"})
    void shouldNotAcceptAnExtraRecipientInCommands(String text) {
        assertThat(service.handle(ADMIN, text)).isFalse();
        verifyNoInteractions(likeRepository, profileRepository, sender);
    }

    @Test
    void shouldKeepOldMenuButtonsWorkingWithSentLikes() {
        when(likeRepository.findByFromChatidOrderByCreatedAtDescIdDesc(ADMIN, PageRequest.of(0, 3)))
                .thenReturn(Page.empty());

        assertThat(service.handle(ADMIN, "/likes_recibidos_admin")).isTrue();

        verify(sender).send(ADMIN, "💌 Todavía no diste likes.");
    }

    private Like like(String to, boolean matched) {
        Like like = new Like();
        like.setFromChatid(ADMIN);
        like.setToChatid(to);
        like.setMatched(matched);
        return like;
    }

    private Profile profile(String name, String username, String whatsapp) {
        Profile profile = new Profile();
        profile.setName(name);
        profile.setContactUsername(username);
        profile.setWhatsapp(whatsapp);
        return profile;
    }
}
