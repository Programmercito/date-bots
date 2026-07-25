package org.osbo.bots.model.repositories;

import java.util.List;

import org.osbo.bots.model.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Like findByFromChatidAndToChatid(String fromChatid, String toChatid);

    List<Like> findByFromChatid(String fromChatid);

}
