package org.osbo.bots.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import java.util.List;

import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.LikeMessage;
import org.osbo.bots.jms.queue.pojos.MatchMessage;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.osbo.bots.model.entity.Like;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.LikeRepository;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.core.JmsTemplate;

class LikeMatchServiceTest {

    private static final String FROM_CHATID = "from-123";
    private static final String TO_CHATID = "to-456";

    private NqueueForSend sender;
    private ProfileRepository profileRepository;
    private UserRepository userRepository;
    private LikeRepository likeRepository;
    private JmsTemplate jmsTemplate;
    private LikeMatchService service;

    @BeforeEach
    void setUp() {
        sender = mock(NqueueForSend.class);
        profileRepository = mock(ProfileRepository.class);
        userRepository = mock(UserRepository.class);
        likeRepository = mock(LikeRepository.class);
        jmsTemplate = mock(JmsTemplate.class);
        service = new LikeMatchService(sender, profileRepository, userRepository, likeRepository, jmsTemplate);
    }

    @Test
    void shouldIgnoreSelfLike() {
        LikeMessage message = new LikeMessage(FROM_CHATID, FROM_CHATID);

        service.processLike(message);

        verifyNoInteractions(profileRepository, userRepository, likeRepository, sender, jmsTemplate);
    }

    @Test
    void shouldNotifyLikerWhenTargetProfileIsPaused() {
        LikeMessage message = new LikeMessage(FROM_CHATID, TO_CHATID);
        Profile profile = approvedProfile(TO_CHATID);
        profile.setStatus(LikeMatchService.STATUS_PAUSED);
        when(profileRepository.findByChatid(TO_CHATID)).thenReturn(profile);

        service.processLike(message);

        verify(sender).send(eq(FROM_CHATID), eq(LikeMatchService.MESSAGE_UNAVAILABLE_PROFILE));
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    void shouldNotifyLikerWhenTargetProfileIsRejected() {
        LikeMessage message = new LikeMessage(FROM_CHATID, TO_CHATID);
        Profile profile = approvedProfile(TO_CHATID);
        profile.setStatus(LikeMatchService.STATUS_REJECTED);
        when(profileRepository.findByChatid(TO_CHATID)).thenReturn(profile);

        service.processLike(message);

        verify(sender).send(eq(FROM_CHATID), eq(LikeMatchService.MESSAGE_UNAVAILABLE_PROFILE));
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    void shouldNotifyLikerWhenTargetUserIsBlocked() {
        LikeMessage message = new LikeMessage(FROM_CHATID, TO_CHATID);
        Profile profile = approvedProfile(TO_CHATID);
        when(profileRepository.findByChatid(TO_CHATID)).thenReturn(profile);
        when(userRepository.findById(TO_CHATID)).thenReturn(Optional.of(blockedUser(TO_CHATID)));

        service.processLike(message);

        verify(sender).send(eq(FROM_CHATID), eq(LikeMatchService.MESSAGE_UNAVAILABLE_PROFILE));
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    void shouldPersistLikeAndNotifyTargetWhenNotMutual() {
        LikeMessage message = new LikeMessage(FROM_CHATID, TO_CHATID);
        Profile profile = approvedProfile(TO_CHATID);
        when(profileRepository.findByChatid(TO_CHATID)).thenReturn(profile);
        when(userRepository.findById(TO_CHATID)).thenReturn(Optional.of(activeUser(TO_CHATID)));
        when(likeRepository.findByFromChatidAndToChatid(FROM_CHATID, TO_CHATID)).thenReturn(null);
        when(likeRepository.findByFromChatidAndToChatid(TO_CHATID, FROM_CHATID)).thenReturn(null);
        when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processLike(message);

        ArgumentCaptor<Like> likeCaptor = ArgumentCaptor.forClass(Like.class);
        verify(likeRepository).save(likeCaptor.capture());
        Like saved = likeCaptor.getValue();
        assertThat(saved.getFromChatid()).isEqualTo(FROM_CHATID);
        assertThat(saved.getToChatid()).isEqualTo(TO_CHATID);
        assertThat(saved.isMatched()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();

        verify(sender).send(eq(TO_CHATID), eq(LikeMatchService.MESSAGE_ANONYMOUS_LIKE));
        verify(jmsTemplate, never()).convertAndSend(eq("queue.match"), any(MatchMessage.class));
    }

    @Test
    void shouldPersistLikeAndSendMatchMessageWhenMutual() {
        LikeMessage message = new LikeMessage(FROM_CHATID, TO_CHATID);
        Profile profile = approvedProfile(TO_CHATID);
        when(profileRepository.findByChatid(TO_CHATID)).thenReturn(profile);
        when(userRepository.findById(TO_CHATID)).thenReturn(Optional.of(activeUser(TO_CHATID)));
        when(likeRepository.findByFromChatidAndToChatid(FROM_CHATID, TO_CHATID)).thenReturn(null);
        Like reverse = existingLike(TO_CHATID, FROM_CHATID);
        when(likeRepository.findByFromChatidAndToChatid(TO_CHATID, FROM_CHATID)).thenReturn(reverse);
        when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processLike(message);

        ArgumentCaptor<Like> likeCaptor = ArgumentCaptor.forClass(Like.class);
        verify(likeRepository, times(2)).save(likeCaptor.capture());
        assertThat(likeCaptor.getAllValues()).allMatch(Like::isMatched);

        ArgumentCaptor<MatchMessage> matchCaptor = ArgumentCaptor.forClass(MatchMessage.class);
        verify(jmsTemplate).convertAndSend(eq("queue.match"), matchCaptor.capture());
        MatchMessage matchMessage = matchCaptor.getValue();
        assertThat(matchMessage.getChatidA()).isEqualTo(FROM_CHATID);
        assertThat(matchMessage.getChatidB()).isEqualTo(TO_CHATID);

        verify(sender, never()).send(eq(TO_CHATID), anyString());
    }

    @Test
    void shouldIgnoreDuplicateLike() {
        LikeMessage message = new LikeMessage(FROM_CHATID, TO_CHATID);
        Profile profile = approvedProfile(TO_CHATID);
        when(profileRepository.findByChatid(TO_CHATID)).thenReturn(profile);
        when(userRepository.findById(TO_CHATID)).thenReturn(Optional.of(activeUser(TO_CHATID)));
        when(likeRepository.findByFromChatidAndToChatid(FROM_CHATID, TO_CHATID)).thenReturn(existingLike(FROM_CHATID, TO_CHATID));

        service.processLike(message);

        verify(likeRepository, never()).save(any(Like.class));
        verifyNoInteractions(sender, jmsTemplate);
    }

    @Test
    void shouldHandleUniqueConstraintViolationGracefully() {
        LikeMessage message = new LikeMessage(FROM_CHATID, TO_CHATID);
        Profile profile = approvedProfile(TO_CHATID);
        when(profileRepository.findByChatid(TO_CHATID)).thenReturn(profile);
        when(userRepository.findById(TO_CHATID)).thenReturn(Optional.of(activeUser(TO_CHATID)));
        when(likeRepository.findByFromChatidAndToChatid(FROM_CHATID, TO_CHATID)).thenReturn(null);
        when(likeRepository.save(any(Like.class))).thenThrow(new DataIntegrityViolationException("Duplicate like"));

        service.processLike(message);

        verify(sender, never()).send(eq(FROM_CHATID), anyString());
        verify(jmsTemplate, never()).convertAndSend(eq("queue.match"), any(MatchMessage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSendPhotoMatchNotificationWithFullProfileAndButtons() {
        Profile profileA = approvedProfile("A");
        profileA.setName("Ana");
        profileA.setContactUsername("ana");
        profileA.setWhatsapp("+591 70012345");
        profileA.setPhotoFileId("photo-A");
        Profile profileB = approvedProfile("B");
        profileB.setName("Bruno");
        profileB.setContactUsername("bruno");
        profileB.setWhatsapp("+591 70067890");
        profileB.setPhotoFileId("photo-B");
        when(profileRepository.findByChatid("A")).thenReturn(profileA);
        when(profileRepository.findByChatid("B")).thenReturn(profileB);

        service.notifyMatch(new MatchMessage("A", "B"));

        ArgumentCaptor<String> chatidCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> photoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttonsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sender, times(2)).send(chatidCaptor.capture(), textCaptor.capture(), eq("match_notification"),
                photoCaptor.capture(), eq((String) null), eq(false), buttonsCaptor.capture(), eq((String) null),
                eq("Markdown"));
        assertThat(chatidCaptor.getAllValues()).containsExactly("A", "B");
        assertThat(photoCaptor.getAllValues()).containsExactly("photo-B", "photo-A");
        String textForA = textCaptor.getAllValues().get(0);
        assertThat(textForA).contains("*Bruno*, 25 años");
        assertThat(textForA).doesNotContain("2001-03-15");
        List<List<Button>> buttonsForA = buttonsCaptor.getAllValues().get(0);
        assertThat(flattenButtons(buttonsForA)).anyMatch(b -> b.getText().contains("Telegram @bruno")
                && "https://t.me/bruno".equals(b.getUrl()));
        assertThat(flattenButtons(buttonsForA)).anyMatch(b -> b.getText().contains("WhatsApp")
                && "https://wa.me/59170067890".equals(b.getUrl()));
        assertThat(flattenButtons(buttonsForA)).anyMatch(b -> b.getText().contains("Reportar")
                && "club_match_report_B".equals(b.getCallbackData()));
    }

    @Test
    void shouldSendTextOnlyMatchNotificationWhenProfileHasNoPhoto() {
        Profile profileA = approvedProfile("A");
        profileA.setPhotoFileId(null);
        Profile profileB = approvedProfile("B");
        profileB.setPhotoFileId("photo-B");
        when(profileRepository.findByChatid("A")).thenReturn(profileA);
        when(profileRepository.findByChatid("B")).thenReturn(profileB);

        service.notifyMatch(new MatchMessage("A", "B"));

        verify(sender, times(1)).send(eq("A"), anyString(), eq("match_notification"), eq("photo-B"),
                eq((String) null), eq(false), any(List.class), eq((String) null), eq("Markdown"));
        verify(sender, times(1)).send(eq("B"), anyString(), eq("text"), eq((String) null),
                eq((String) null), eq(false), any(List.class), eq((String) null), eq("Markdown"));
    }

    @Test
    void shouldIgnoreNotifyMatchWhenEitherProfileIsMissing() {
        when(profileRepository.findByChatid("A")).thenReturn(null);
        when(profileRepository.findByChatid("B")).thenReturn(approvedProfile("B"));

        service.notifyMatch(new MatchMessage("A", "B"));

        verify(sender, never()).send(anyString(), anyString(), anyString(), any(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldListMatchesWithContactInfo() {
        Profile me = approvedProfile("me");
        Profile matchOne = approvedProfile("match-1");
        matchOne.setName("Carla");
        matchOne.setBirthDate(LocalDate.of(1996, 4, 20));
        matchOne.setAge(null);
        matchOne.setContactUsername("carla");
        matchOne.setWhatsapp("+591 70011111");
        Profile matchTwo = approvedProfile("match-2");
        matchTwo.setName("Diana");
        matchTwo.setBirthDate(LocalDate.of(1998, 6, 15));
        matchTwo.setAge(null);
        matchTwo.setContactUsername("diana");
        when(profileRepository.findByChatid("me")).thenReturn(me);
        Like likeOne = matchedLike("me", "match-1");
        Like likeTwo = matchedLike("match-2", "me");
        when(likeRepository.findByFromChatidOrToChatidAndMatchedTrue("me")).thenReturn(List.of(likeOne, likeTwo));
        when(profileRepository.findByChatid("match-1")).thenReturn(matchOne);
        when(profileRepository.findByChatid("match-2")).thenReturn(matchTwo);

        service.listMatches("me");

        ArgumentCaptor<String> chatidCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<List<Button>>> buttonsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sender).send(chatidCaptor.capture(), textCaptor.capture(), eq("text"), eq((String) null),
                eq((String) null), eq(false), buttonsCaptor.capture(), eq((String) null));
        assertThat(chatidCaptor.getValue()).isEqualTo("me");
        assertThat(textCaptor.getValue()).contains("Carla", "Diana", "Santa Cruz", "30 años", "28 años");
        assertThat(textCaptor.getValue()).doesNotContain("1996-04-20", "1998-06-15");
        List<List<Button>> buttons = buttonsCaptor.getValue();
        assertThat(flattenButtons(buttons)).anyMatch(b -> b.getText().contains("Telegram @carla")
                && "https://t.me/carla".equals(b.getUrl()));
        assertThat(flattenButtons(buttons)).anyMatch(b -> b.getText().contains("WhatsApp +591 70011111")
                && "https://wa.me/59170011111".equals(b.getUrl()));
    }

    @Test
    void shouldSendEmptyStateWhenNoMatches() {
        Profile me = approvedProfile("me");
        when(profileRepository.findByChatid("me")).thenReturn(me);
        when(likeRepository.findByFromChatidOrToChatidAndMatchedTrue("me")).thenReturn(List.of());

        service.listMatches("me");

        verify(sender).send(eq("me"), eq(LikeMatchService.MESSAGE_NO_MATCHES));
    }

    @Test
    void shouldRequireApprovedProfileForMatchesList() {
        when(profileRepository.findByChatid("me")).thenReturn(null);

        service.listMatches("me");

        verify(sender).send(eq("me"), eq(LikeMatchService.MESSAGE_MATCHES_REQUIRES_APPROVAL));
        verify(likeRepository, never()).findByFromChatidOrToChatidAndMatchedTrue(anyString());
    }

    @Test
    void shouldSendReportModerationMessageWhenReportingMatch() {
        Profile reported = approvedProfile("reported");
        when(profileRepository.findByChatid("reported")).thenReturn(reported);

        service.reportMatch("reporter", "reported");

        ArgumentCaptor<ModerationMessage> captor = ArgumentCaptor.forClass(ModerationMessage.class);
        verify(jmsTemplate).convertAndSend(eq("queue.moderation"), captor.capture());
        ModerationMessage message = captor.getValue();
        assertThat(message.getType()).isEqualTo(LikeMatchService.MODERATION_TYPE_REPORT);
        assertThat(message.getChatid()).isEqualTo("reported");
        assertThat(message.getReason()).isEqualTo(LikeMatchService.REASON_REPORT_FROM_MATCH);
        assertThat(message.getName()).isEqualTo(reported.getName());
        assertThat(message.getBirthDate()).isEqualTo("2001-03-15");
        assertThat(message.getAge()).isNull();
        verify(sender).send(eq("reporter"), eq(LikeMatchService.MESSAGE_REPORT_CONFIRMATION));
    }

    @Test
    void shouldSendReportWithoutProfileDetailsWhenProfileMissing() {
        when(profileRepository.findByChatid("reported")).thenReturn(null);

        service.reportMatch("reporter", "reported");

        ArgumentCaptor<ModerationMessage> captor = ArgumentCaptor.forClass(ModerationMessage.class);
        verify(jmsTemplate).convertAndSend(eq("queue.moderation"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(LikeMatchService.MODERATION_TYPE_REPORT);
        assertThat(captor.getValue().getChatid()).isEqualTo("reported");
        verify(sender).send(eq("reporter"), eq(LikeMatchService.MESSAGE_REPORT_CONFIRMATION));
    }

    private List<Button> flattenButtons(List<List<Button>> rows) {
        return rows.stream().flatMap(List::stream).toList();
    }

    private Like matchedLike(String fromChatid, String toChatid) {
        Like like = new Like();
        like.setFromChatid(fromChatid);
        like.setToChatid(toChatid);
        like.setMatched(true);
        return like;
    }

    private Profile approvedProfile(String chatid) {
        Profile profile = new Profile();
        profile.setChatid(chatid);
        profile.setName("Test");
        profile.setBirthDate(LocalDate.of(2001, 3, 15));
        profile.setAge(25);
        profile.setGender("MALE");
        profile.setOrientation("HETERO");
        profile.setCountry("BO");
        profile.setCity("Santa Cruz");
        profile.setStatus(LikeMatchService.STATUS_APPROVED);
        return profile;
    }

    private User activeUser(String chatid) {
        User user = new User();
        user.setChatid(chatid);
        user.setEstado("activo");
        return user;
    }

    private User blockedUser(String chatid) {
        User user = new User();
        user.setChatid(chatid);
        user.setEstado("bloqueado");
        return user;
    }

    private Like existingLike(String fromChatid, String toChatid) {
        Like like = new Like();
        like.setFromChatid(fromChatid);
        like.setToChatid(toChatid);
        like.setMatched(false);
        return like;
    }

}
