package org.osbo.bots.model.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.osbo.bots.model.repositories.ProfileRepository;

class ClubStatisticsServiceTest {

    @Test
    void shouldCountProfilesUsingDiscoveryEligibility() {
        ProfileRepository repository = mock(ProfileRepository.class);
        when(repository.countDiscoverableProfiles("APPROVED", "BO", "bloqueado")).thenReturn(12L);
        ClubStatisticsService service = new ClubStatisticsService(repository);

        assertThat(service.countDiscoverableProfiles()).isEqualTo(12);
        verify(repository).countDiscoverableProfiles("APPROVED", "BO", "bloqueado");
    }
}
