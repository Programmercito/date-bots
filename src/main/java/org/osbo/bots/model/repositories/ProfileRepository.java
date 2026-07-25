package org.osbo.bots.model.repositories;

import java.util.List;

import org.osbo.bots.model.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Profile findByChatid(String chatid);

    List<Profile> findByStatusAndCountryOrderByCreatedAtAsc(String status, String country);

}
