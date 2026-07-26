package org.osbo.bots.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ProfileTest {

    @Test
    void shouldCalculateAgeFromBirthDate() {
        Profile profile = new Profile();
        profile.setBirthDate(LocalDate.of(2000, 3, 15));

        assertThat(profile.getAge()).isEqualTo(26);
    }

    @Test
    void shouldFallbackToLegacyAgeWhenBirthDateIsNull() {
        Profile profile = new Profile();
        profile.setAge(28);

        assertThat(profile.getAge()).isEqualTo(28);
    }

    @Test
    void shouldPreferBirthDateOverLegacyAge() {
        Profile profile = new Profile();
        profile.setBirthDate(LocalDate.of(2000, 3, 15));
        profile.setAge(99);

        assertThat(profile.getAge()).isEqualTo(26);
    }

    @Test
    void shouldReturnNullWhenNeitherBirthDateNorAgeIsSet() {
        Profile profile = new Profile();

        assertThat(profile.getAge()).isNull();
    }

}
