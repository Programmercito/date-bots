package org.osbo.bots.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

class AgeCalculatorTest {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

    @Test
    void shouldReturnAgeForValidAdultBirthdate() {
        Clock clock = Clock.fixed(LocalDate.of(2026, 7, 26).atStartOfDay(LA_PAZ).toInstant(), LA_PAZ);
        LocalDate birthDate = LocalDate.of(2000, 3, 15);

        Integer age = AgeCalculator.calculateAge(birthDate, null, clock);

        assertThat(age).isEqualTo(26);
    }

    @Test
    void shouldReturnAgeWhenBirthdayNotYetReachedThisYear() {
        Clock clock = Clock.fixed(LocalDate.of(2026, 7, 26).atStartOfDay(LA_PAZ).toInstant(), LA_PAZ);
        LocalDate birthDate = LocalDate.of(2000, 12, 10);

        Integer age = AgeCalculator.calculateAge(birthDate, null, clock);

        assertThat(age).isEqualTo(25);
    }

    @Test
    void shouldReturnAgeOnBirthday() {
        Clock clock = Clock.fixed(LocalDate.of(2026, 7, 26).atStartOfDay(LA_PAZ).toInstant(), LA_PAZ);
        LocalDate birthDate = LocalDate.of(2000, 7, 26);

        Integer age = AgeCalculator.calculateAge(birthDate, null, clock);

        assertThat(age).isEqualTo(26);
    }

    @Test
    void shouldEvaluateCurrentDateInLaPazTimezone() {
        // UTC 2027-01-01T03:00:00Z is 2026-12-31T23:00 in La Paz.
        Clock clock = Clock.fixed(java.time.Instant.parse("2027-01-01T03:00:00Z"), LA_PAZ);
        LocalDate birthDate = LocalDate.of(2000, 12, 31);

        Integer age = AgeCalculator.calculateAge(birthDate, null, clock);

        // Birthday is today in La Paz, so the person has turned 26.
        assertThat(age).isEqualTo(26);
    }

    @Test
    void shouldFallbackToLegacyAgeWhenBirthDateIsNull() {
        Clock clock = Clock.fixed(LocalDate.of(2026, 7, 26).atStartOfDay(LA_PAZ).toInstant(), LA_PAZ);

        Integer age = AgeCalculator.calculateAge(null, 28, clock);

        assertThat(age).isEqualTo(28);
    }

    @Test
    void shouldReturnNullWhenBirthDateAndFallbackAgeAreNull() {
        Clock clock = Clock.fixed(LocalDate.of(2026, 7, 26).atStartOfDay(LA_PAZ).toInstant(), LA_PAZ);

        Integer age = AgeCalculator.calculateAge(null, null, clock);

        assertThat(age).isNull();
    }

    @Test
    void shouldParseValidUserDate() {
        LocalDate birthDate = AgeCalculator.parseUserDate("15/03/2000");

        assertThat(birthDate).isEqualTo(LocalDate.of(2000, 3, 15));
    }

    @Test
    void shouldThrowExceptionForInvalidUserDateFormat() {
        assertThatThrownBy(() -> AgeCalculator.parseUserDate("15-03-2000"))
                .isInstanceOf(DateTimeParseException.class);
    }

}
