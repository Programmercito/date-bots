package org.osbo.bots.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.AnalyticsMessage;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.LikeMessage;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.osbo.bots.model.entity.Like;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.Report;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.LikeRepository;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.ReportRepository;
import org.osbo.bots.model.repositories.UserRepository;
import org.osbo.bots.util.LookingForOption;
import org.springframework.jms.core.JmsTemplate;

class ClubDiscoveryServiceTest {

    private static final String VIEWER_CHATID = "viewer-123";
    private static final String TARGET_CHATID = "target-456";

    private NqueueForSend sender;
    private ProfileRepository profileRepository;
    private UserRepository userRepository;
    private LikeRepository likeRepository;
    private ReportRepository reportRepository;
    private JmsTemplate jmsTemplate;
    private ClubDiscoveryService service;

    @BeforeEach
    void setUp() {
        sender = mock(NqueueForSend.class);
        profileRepository = mock(ProfileRepository.class);
        userRepository = mock(UserRepository.class);
        likeRepository = mock(LikeRepository.class);
        reportRepository = mock(ReportRepository.class);
        jmsTemplate = mock(JmsTemplate.class);
        service = new ClubDiscoveryService(sender, profileRepository, userRepository, likeRepository,
                reportRepository, jmsTemplate);
    }

    @Test
    void shouldRejectViewPeopleWithoutProfile() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(null);

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        verify(sender).send(eq(VIEWER_CHATID), anyString());
        verify(profileRepository, never()).findByStatusAndCountryOrderByCreatedAtAsc(anyString(), anyString());
    }

    @Test
    void shouldRejectViewPeopleWhenProfileNotApproved() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile profile = approvedProfile(VIEWER_CHATID);
        profile.setStatus("PENDING");
        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(profile);

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        verify(sender).send(eq(VIEWER_CHATID), anyString());
        verify(profileRepository, never()).findByStatusAndCountryOrderByCreatedAtAsc(anyString(), anyString());
    }

    @Test
    void shouldShowNextApprovedProfile() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setGender(ClubDiscoveryService.GENDER_FEMALE);
        target.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);
        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA)).thenReturn(List.of(target));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(TARGET_CHATID)).thenReturn(Optional.of(activeUser(TARGET_CHATID)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean handled = service.handle(user, update);

        assertThat(handled).isTrue();
        assertThat(user.getComando()).isEqualTo(ClubDiscoveryService.STATE_BROWSING);
        assertThat(user.getCurrentProfileMessageId()).isNull();
        assertThat(user.getPreviousProfileMessageId()).isNull();
        verify(sender).send(eq(VIEWER_CHATID), anyString(), eq("discovery_profile"), eq(target.getPhotoFileId()),
                eq((String) null), eq(false), any(List.class), eq(target.getChatid()), eq("Markdown"));
        verify(jmsTemplate).convertAndSend(eq("queue.analytics"), any(AnalyticsMessage.class));
    }

    @Test
    void maleHeteroShouldSeeOnlyFemaleHetero() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        viewer.setGender(ClubDiscoveryService.GENDER_MALE);
        viewer.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        Profile femaleHetero = approvedProfile("female-hetero");
        femaleHetero.setGender(ClubDiscoveryService.GENDER_FEMALE);
        femaleHetero.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        Profile femaleBi = approvedProfile("female-bi");
        femaleBi.setGender(ClubDiscoveryService.GENDER_FEMALE);
        femaleBi.setOrientation(ClubDiscoveryService.ORIENTATION_BI);

        Profile maleHetero = approvedProfile("male-hetero");
        maleHetero.setGender(ClubDiscoveryService.GENDER_MALE);
        maleHetero.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA))
                .thenReturn(List.of(femaleHetero, femaleBi, maleHetero));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(anyString())).thenReturn(Optional.of(activeUser("any")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        ArgumentCaptor<String> chatidCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> photoCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(chatidCaptor.capture(), anyString(), eq("discovery_profile"), photoCaptor.capture(),
                eq((String) null), eq(false), any(List.class), eq(femaleHetero.getChatid()), eq("Markdown"));
        assertThat(chatidCaptor.getValue()).isEqualTo(VIEWER_CHATID);
        assertThat(photoCaptor.getValue()).isEqualTo(femaleHetero.getPhotoFileId());
    }

    @Test
    void shouldOnlyShowProfilesFromTheViewerCity() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        viewer.setCity("La Paz");

        Profile sameCityProfile = approvedProfile(TARGET_CHATID);
        sameCityProfile.setCity("La Paz");
        sameCityProfile.setGender(ClubDiscoveryService.GENDER_FEMALE);
        sameCityProfile.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        Profile otherCityProfile = approvedProfile("other-city");
        otherCityProfile.setCity("Santa Cruz");
        otherCityProfile.setGender(ClubDiscoveryService.GENDER_FEMALE);
        otherCityProfile.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA)).thenReturn(List.of(sameCityProfile, otherCityProfile));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(anyString())).thenReturn(Optional.of(activeUser("any")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        ArgumentCaptor<String> photoCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(VIEWER_CHATID), anyString(), eq("discovery_profile"), photoCaptor.capture(),
                eq((String) null), eq(false), any(List.class), eq(sameCityProfile.getChatid()), eq("Markdown"));
        assertThat(photoCaptor.getValue()).isEqualTo(sameCityProfile.getPhotoFileId());
    }

    @Test
    void femaleHeteroShouldSeeOnlyMaleHetero() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        viewer.setGender(ClubDiscoveryService.GENDER_FEMALE);
        viewer.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        Profile maleHetero = approvedProfile("male-hetero");
        maleHetero.setGender(ClubDiscoveryService.GENDER_MALE);
        maleHetero.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        Profile femaleHetero = approvedProfile("female-hetero");
        femaleHetero.setGender(ClubDiscoveryService.GENDER_FEMALE);
        femaleHetero.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA))
                .thenReturn(List.of(maleHetero, femaleHetero));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(anyString())).thenReturn(Optional.of(activeUser("any")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        ArgumentCaptor<String> photoCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(VIEWER_CHATID), anyString(), eq("discovery_profile"), photoCaptor.capture(),
                eq((String) null), eq(false), any(List.class), eq(maleHetero.getChatid()), eq("Markdown"));
        assertThat(photoCaptor.getValue()).isEqualTo(maleHetero.getPhotoFileId());
    }

    @Test
    void femaleBiShouldSeeMaleHeteroAndFemaleProfiles() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        viewer.setGender(ClubDiscoveryService.GENDER_FEMALE);
        viewer.setOrientation(ClubDiscoveryService.ORIENTATION_BI);

        Profile femaleHetero = approvedProfile("female-hetero");
        femaleHetero.setGender(ClubDiscoveryService.GENDER_FEMALE);
        femaleHetero.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        Profile femaleBi = approvedProfile("female-bi");
        femaleBi.setGender(ClubDiscoveryService.GENDER_FEMALE);
        femaleBi.setOrientation(ClubDiscoveryService.ORIENTATION_BI);

        Profile maleHetero = approvedProfile("male-hetero");
        maleHetero.setGender(ClubDiscoveryService.GENDER_MALE);
        maleHetero.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        Profile maleBi = approvedProfile("male-bi");
        maleBi.setGender(ClubDiscoveryService.GENDER_MALE);
        maleBi.setOrientation(ClubDiscoveryService.ORIENTATION_BI);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA))
                .thenReturn(List.of(femaleHetero, femaleBi, maleHetero, maleBi));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(anyString())).thenReturn(Optional.of(activeUser("any")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        ArgumentCaptor<String> photoCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(VIEWER_CHATID), anyString(), eq("discovery_profile"), photoCaptor.capture(),
                eq((String) null), eq(false), any(List.class), eq(femaleHetero.getChatid()), eq("Markdown"));
        assertThat(photoCaptor.getValue()).isEqualTo(femaleHetero.getPhotoFileId());
    }

    @Test
    void shouldExcludeAlreadyLikedProfiles() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setGender(ClubDiscoveryService.GENDER_FEMALE);
        target.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        Like previousLike = new Like();
        previousLike.setFromChatid(VIEWER_CHATID);
        previousLike.setToChatid(TARGET_CHATID);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA)).thenReturn(List.of(target));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of(previousLike));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).send(eq(VIEWER_CHATID), eq("No hay más personas por ahora."));
    }

    @Test
    void shouldExcludeBlockedUsers() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setGender(ClubDiscoveryService.GENDER_FEMALE);
        target.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA)).thenReturn(List.of(target));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(TARGET_CHATID)).thenReturn(Optional.of(blockedUser(TARGET_CHATID)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).send(eq(VIEWER_CHATID), eq("No hay más personas por ahora."));
    }

    @Test
    void shouldHandleNoMoreProfiles() {
        User user = newUser(ClubDiscoveryService.STATE_BROWSING);
        MessageUpdate update = newUpdate("club_next", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        user.setCurrentProfileMessageId(100);
        user.setPreviousProfileMessageId(99);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA)).thenReturn(List.of());
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(user.getComando()).isEqualTo("start");
        verify(sender).editCaption(VIEWER_CHATID, 100, "No hay más personas por ahora.", null);
        verify(sender, never()).deleteMessage(anyString(), anyInt());
    }

    @Test
    void shouldHandleLikeCallback() {
        User user = newUser(ClubDiscoveryService.STATE_BROWSING);
        user.setCurrentProfileMessageId(200);
        MessageUpdate update = newUpdate("club_like_" + TARGET_CHATID, null);
        update.setMessageId(200);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setName("María");

        when(profileRepository.findByChatid(TARGET_CHATID)).thenReturn(target);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(user.getCurrentProfileMessageId()).isEqualTo(200);
        verify(sender).editCaption(eq(VIEWER_CHATID), eq(200), eq("Le diste like a María ❤️"), any(List.class));
        verify(jmsTemplate).convertAndSend(eq("queue.like"), any(LikeMessage.class));
        verify(jmsTemplate).convertAndSend(eq("queue.analytics"), any(AnalyticsMessage.class));
    }

    @Test
    void shouldHandleSkipCallback() {
        User user = newUser(ClubDiscoveryService.STATE_BROWSING);
        user.setCurrentProfileMessageId(201);
        MessageUpdate update = newUpdate("club_skip_" + TARGET_CHATID, null);
        update.setMessageId(201);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setName("Laura");

        when(profileRepository.findByChatid(TARGET_CHATID)).thenReturn(target);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(user.getCurrentProfileMessageId()).isEqualTo(201);
        verify(sender).editCaption(eq(VIEWER_CHATID), eq(201), eq("Skippeaste a Laura 👋"), any(List.class));
        verify(jmsTemplate).convertAndSend(eq("queue.analytics"), any(AnalyticsMessage.class));
        verify(jmsTemplate, never()).convertAndSend(eq("queue.like"), any(LikeMessage.class));
    }

    @Test
    void shouldHandleReportCallback() {
        User user = newUser(ClubDiscoveryService.STATE_BROWSING);
        user.setCurrentProfileMessageId(202);
        MessageUpdate update = newUpdate("club_report_" + TARGET_CHATID, null);
        update.setMessageId(202);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setName("Reported");

        when(profileRepository.findByChatid(TARGET_CHATID)).thenReturn(target);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        assertThat(user.getCurrentProfileMessageId()).isEqualTo(202);
        verify(sender).editCaption(eq(VIEWER_CHATID), eq(202),
                eq("Perfil reportado. Los administradores lo revisarán."), any(List.class));
        verify(reportRepository).save(any(Report.class));
        ArgumentCaptor<ModerationMessage> moderationCaptor = ArgumentCaptor.forClass(ModerationMessage.class);
        verify(jmsTemplate).convertAndSend(eq("queue.moderation"), moderationCaptor.capture());
        assertThat(moderationCaptor.getValue().getBirthDate()).isEqualTo("2001-03-15");
        assertThat(moderationCaptor.getValue().getAge()).isNull();
    }

    @Test
    void shouldShowCalculatedAgeInCaptionWithoutRawBirthdate() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setGender(ClubDiscoveryService.GENDER_FEMALE);
        target.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);
        target.setBirthDate(LocalDate.of(1998, 5, 10));
        target.setAge(null);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA)).thenReturn(List.of(target));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(TARGET_CHATID)).thenReturn(Optional.of(activeUser(TARGET_CHATID)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        ArgumentCaptor<String> captionCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(VIEWER_CHATID), captionCaptor.capture(), eq("discovery_profile"), eq(target.getPhotoFileId()),
                eq((String) null), eq(false), any(List.class), eq(target.getChatid()), eq("Markdown"));
        assertThat(captionCaptor.getValue()).contains("28 años");
        assertThat(captionCaptor.getValue()).doesNotContain("10/05/1998", "1998-05-10");
    }

    @Test
    void shouldShowLegacyAgeWhenBirthDateMissing() {
        User user = newUser("start");
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setGender(ClubDiscoveryService.GENDER_FEMALE);
        target.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);
        target.setBirthDate(null);
        target.setAge(30);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA)).thenReturn(List.of(target));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(TARGET_CHATID)).thenReturn(Optional.of(activeUser(TARGET_CHATID)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        ArgumentCaptor<String> captionCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(VIEWER_CHATID), captionCaptor.capture(), eq("discovery_profile"), eq(target.getPhotoFileId()),
                eq((String) null), eq(false), any(List.class), eq(target.getChatid()), eq("Markdown"));
        assertThat(captionCaptor.getValue()).contains("30 años");
    }

    @Test
    void shouldReturnFalseForUnrelatedCallback() {
        User user = newUser(ClubDiscoveryService.STATE_BROWSING);
        MessageUpdate update = newUpdate("some_other_callback", null);

        boolean handled = service.handle(user, update);

        assertThat(handled).isFalse();
        verify(sender, never()).send(anyString(), anyString());
        verify(sender, never()).editCaption(anyString(), anyInt(), anyString());
    }

    @Test
    void shouldReplaceCurrentProfileMessageWhenShowingNext() {
        User user = newUser(ClubDiscoveryService.STATE_BROWSING);
        user.setCurrentProfileMessageId(200);
        user.setPreviousProfileMessageId(100);
        MessageUpdate update = newUpdate("club_next", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setGender(ClubDiscoveryService.GENDER_FEMALE);
        target.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA)).thenReturn(List.of(target));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(TARGET_CHATID)).thenReturn(Optional.of(activeUser(TARGET_CHATID)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        verify(sender).editPhotoCaptionMarkdown(eq(VIEWER_CHATID), eq(200), eq(target.getPhotoFileId()),
                anyString(), any(List.class), eq(target.getChatid()));
        verify(sender, never()).deleteMessage(anyString(), anyInt());
        assertThat(user.getCurrentProfileMessageId()).isEqualTo(200);
        assertThat(user.getPreviousProfileMessageId()).isEqualTo(100);
    }

    @Test
    void shouldDeleteExistingSwipeMessageWhenRestartingSwipeFromMenu() {
        // User was swiping (currentProfileMessageId set), went back to menu, now starts swiping again
        User user = newUser("start");
        user.setCurrentProfileMessageId(100);
        MessageUpdate update = newUpdate("/ver_personas", null);
        Profile viewer = approvedProfile(VIEWER_CHATID);
        Profile target = approvedProfile(TARGET_CHATID);
        target.setGender(ClubDiscoveryService.GENDER_FEMALE);
        target.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);

        when(profileRepository.findByChatid(VIEWER_CHATID)).thenReturn(viewer);
        when(profileRepository.findByStatusAndCountryOrderByCreatedAtAsc(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA)).thenReturn(List.of(target));
        when(likeRepository.findByFromChatid(VIEWER_CHATID)).thenReturn(List.of());
        when(userRepository.findById(TARGET_CHATID)).thenReturn(Optional.of(activeUser(TARGET_CHATID)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(user, update);

        // Old message from the previous swipe session must be deleted
        verify(sender).deleteMessage(VIEWER_CHATID, 100);
        assertThat(user.getCurrentProfileMessageId()).isNull();
        assertThat(user.getComando()).isEqualTo(ClubDiscoveryService.STATE_BROWSING);
        // And a new profile message must be sent
        verify(sender).send(eq(VIEWER_CHATID), anyString(), eq("discovery_profile"), anyString(),
                eq((String) null), eq(false), any(List.class), eq(target.getChatid()), eq("Markdown"));
    }

    private User newUser(String comando) {
        User user = new User();
        user.setChatid(VIEWER_CHATID);
        user.setComando(comando);
        return user;
    }

    private MessageUpdate newUpdate(String text, String user) {
        MessageUpdate update = new MessageUpdate();
        update.setChatid(VIEWER_CHATID);
        update.setText(text);
        update.setUser(user);
        return update;
    }

    private Profile approvedProfile(String chatid) {
        Profile profile = new Profile();
        profile.setChatid(chatid);
        profile.setName("Test");
        profile.setBirthDate(LocalDate.of(2001, 3, 15));
        profile.setAge(25);
        profile.setGender(ClubDiscoveryService.GENDER_MALE);
        profile.setOrientation(ClubDiscoveryService.ORIENTATION_HETERO);
        profile.setCountry(ClubDiscoveryService.COUNTRY_BOLIVIA);
        profile.setCity("Santa Cruz");
        profile.setDescription("Friendly");
        profile.setTastes("Music");
        profile.setTraits("Funny");
        profile.setLookingFor(LookingForOption.LOOKING_FOR_FRIENDSHIP);
        profile.setPhotoFileId("photo-" + chatid);
        profile.setStatus(ClubDiscoveryService.STATUS_APPROVED);
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

}
