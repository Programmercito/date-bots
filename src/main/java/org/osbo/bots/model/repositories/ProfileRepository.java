package org.osbo.bots.model.repositories;

import java.util.List;

import org.osbo.bots.model.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Profile findByChatid(String chatid);

    List<Profile> findByStatusAndCountryOrderByCreatedAtAsc(String status, String country);

    @Query("""
            SELECT COUNT(p) FROM Profile p JOIN User u ON p.chatid = u.chatid
            WHERE p.status = :status AND p.country = :country
              AND (u.estado IS NULL OR u.estado <> :blockedStatus)
            """)
    long countDiscoverableProfiles(@Param("status") String status, @Param("country") String country,
            @Param("blockedStatus") String blockedStatus);

}
