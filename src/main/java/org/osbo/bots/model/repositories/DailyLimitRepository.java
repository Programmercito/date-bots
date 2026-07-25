package org.osbo.bots.model.repositories;

import org.osbo.bots.model.entity.DailyLimit;
import org.osbo.bots.model.entity.DailyLimitId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyLimitRepository extends JpaRepository<DailyLimit, DailyLimitId> {

}
