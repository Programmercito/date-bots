package org.osbo.bots.model.services;

import org.osbo.bots.model.repositories.ProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class ClubStatisticsService {

    private final ProfileRepository profileRepository;

    public ClubStatisticsService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public long countDiscoverableProfiles() {
        return profileRepository.countDiscoverableProfiles(ClubDiscoveryService.STATUS_APPROVED,
                ClubDiscoveryService.COUNTRY_BOLIVIA, ClubDiscoveryService.STATUS_BLOCKED);
    }
}
