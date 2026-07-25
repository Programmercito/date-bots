package org.osbo.bots.model.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.osbo.bots.model.entity.DailyLimit;
import org.osbo.bots.model.entity.DailyLimitId;
import org.osbo.bots.model.entity.Like;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.Report;
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

    @Test
    void shouldSaveAndFindProfile() {
        String chatid = randomChatid();
        Profile profile = new Profile();
        profile.setChatid(chatid);
        profile.setName("Test");
        profile.setAge(25);
        profile.setStatus("PENDING");

        Profile saved = profileRepository.save(profile);
        Profile found = profileRepository.findByChatid(chatid);

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("Test");
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

    private String randomChatid() {
        return UUID.randomUUID().toString();
    }

}
