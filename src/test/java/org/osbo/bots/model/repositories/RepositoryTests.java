package org.osbo.bots.model.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.osbo.bots.model.entity.DailyLimit;
import org.osbo.bots.model.entity.DailyLimitId;
import org.osbo.bots.model.entity.Like;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.Report;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.entity.UserPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTests {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private DailyLimitRepository dailyLimitRepository;

    @Autowired
    private UserPlanRepository userPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindProfile() {
        String chatid = randomChatid();
        Profile profile = new Profile();
        profile.setChatid(chatid);
        profile.setName("Test");
        profile.setAge(25);
        profile.setStatus("PENDING");
        profile.setWhatsapp("+591 70012345");

        Profile saved = profileRepository.save(profile);
        Profile found = profileRepository.findByChatid(chatid);

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("Test");
        assertThat(found.getWhatsapp()).isEqualTo("+591 70012345");
    }

    @Test
    void shouldSaveAndFindProfileWithBirthDate() {
        String chatid = randomChatid();
        Profile profile = new Profile();
        profile.setChatid(chatid);
        profile.setName("Test");
        profile.setBirthDate(LocalDate.of(2000, 3, 15));
        profile.setAge(25);
        profile.setStatus("PENDING");

        Profile saved = profileRepository.save(profile);
        Profile found = profileRepository.findByChatid(chatid);

        assertThat(found).isNotNull();
        assertThat(found.getBirthDate()).isEqualTo(LocalDate.of(2000, 3, 15));
        assertThat(found.getAge()).isEqualTo(26);
    }

    @Test
    void shouldSaveAndFindLike() {
        String fromChatid = randomChatid();
        String toChatid = randomChatid();
        Like like = new Like();
        like.setFromChatid(fromChatid);
        like.setToChatid(toChatid);

        Like saved = likeRepository.save(like);
        Like found = likeRepository.findById(saved.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.isMatched()).isFalse();
    }

    @Test
    void shouldFindLikesByFromChatidOrToChatidAndMatchedTrue() {
        String a = randomChatid();
        String b = randomChatid();
        String c = randomChatid();
        String d = randomChatid();

        Like fromAtoB = new Like();
        fromAtoB.setFromChatid(a);
        fromAtoB.setToChatid(b);
        fromAtoB.setMatched(true);
        likeRepository.save(fromAtoB);

        Like fromCtoA = new Like();
        fromCtoA.setFromChatid(c);
        fromCtoA.setToChatid(a);
        fromCtoA.setMatched(true);
        likeRepository.save(fromCtoA);

        Like fromDtoB = new Like();
        fromDtoB.setFromChatid(d);
        fromDtoB.setToChatid(b);
        fromDtoB.setMatched(false);
        likeRepository.save(fromDtoB);

        List<Like> matches = likeRepository.findByFromChatidOrToChatidAndMatchedTrue(a);

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .allMatch(like -> like.isMatched()
                        && (a.equals(like.getFromChatid()) || a.equals(like.getToChatid())));
    }

    @Test
    void shouldSaveAndFindReport() {
        Report report = new Report();
        report.setReporterChatid(randomChatid());
        report.setReportedChatid(randomChatid());
        report.setReason("spam");
        report.setStatus("OPEN");

        Report saved = reportRepository.save(report);
        Report found = reportRepository.findById(saved.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void shouldSaveAndFindDailyLimit() {
        DailyLimit limit = new DailyLimit();
        String chatid = randomChatid();
        String date = "2026-07-25";
        limit.setChatid(chatid);
        limit.setDate(date);
        limit.setLikesUsed(3);
        limit.setViewsUsed(5);

        DailyLimit saved = dailyLimitRepository.save(limit);
        DailyLimit found = dailyLimitRepository.findById(new DailyLimitId(chatid, date)).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getLikesUsed()).isEqualTo(3);
        assertThat(found.getViewsUsed()).isEqualTo(5);
    }

    @Test
    void shouldSaveAndFindUserPlan() {
        String chatid = randomChatid();
        UserPlan plan = new UserPlan();
        plan.setChatid(chatid);
        plan.setPlan("PREMIUM");

        UserPlan saved = userPlanRepository.save(plan);
        UserPlan found = userPlanRepository.findById(saved.getChatid()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getPlan()).isEqualTo("PREMIUM");
    }

    @Test
    void shouldSaveAndFindUserWithNullTempPhotoFileIds() {
        String chatid = randomChatid();
        User user = new User();
        user.setChatid(chatid);
        user.setComando("start");

        User saved = userRepository.save(user);
        User found = userRepository.findById(saved.getChatid()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getTempPhotoFileIds()).isNull();
    }

    @Test
    void shouldSaveAndFindUserWithTempPhotoFileIds() {
        String chatid = randomChatid();
        User user = new User();
        user.setChatid(chatid);
        user.setComando("club_edit_photo");
        user.setTempPhotoFileIds("A|B|C");

        User saved = userRepository.save(user);
        User found = userRepository.findById(saved.getChatid()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getTempPhotoList()).containsExactly("A", "B", "C");
    }

    private String randomChatid() {
        return UUID.randomUUID().toString();
    }

}
