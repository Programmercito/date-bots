package org.osbo.bots.model.repositories;

import java.util.List;

import org.osbo.bots.model.entity.SkippedProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkippedProfileRepository extends JpaRepository<SkippedProfile, Long> {

    SkippedProfile findByFromChatidAndToChatid(String fromChatid, String toChatid);

    List<SkippedProfile> findByFromChatidAndExpiresAtMsAfter(String fromChatid, Long expiresAtMs);

}
