package org.osbo.bots.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Calculates a person's age from a birthdate using the Bolivia timezone.
 */
public final class AgeCalculator {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter USER_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AgeCalculator() {
    }

    /**
     * Calculates the age from the birthdate, falling back to the legacy age if no
     * birthdate is available.
     *
     * @param birthDate    the birthdate in ISO format, may be null
     * @param fallbackAge  the legacy age value, may be null
     * @return the calculated age, or null if neither value is available
     */
    public static Integer calculateAge(LocalDate birthDate, Integer fallbackAge) {
        return calculateAge(birthDate, fallbackAge, Clock.system(LA_PAZ));
    }

    /**
     * Calculates the age from the birthdate using the provided clock, falling back
     * to the legacy age if no birthdate is available.
     *
     * @param birthDate    the birthdate, may be null
     * @param fallbackAge  the legacy age value, may be null
     * @param clock        the clock used to determine the current date
     * @return the calculated age, or null if neither value is available
     */
    public static Integer calculateAge(LocalDate birthDate, Integer fallbackAge, Clock clock) {
        if (birthDate == null) {
            return fallbackAge;
        }
        LocalDate today = LocalDate.now(clock);
        return Period.between(birthDate, today).getYears();
    }

    /**
     * Parses a user-entered date in {@code DD/MM/AAAA} format.
     *
     * @param input the user input
     * @return the parsed {@link LocalDate}
     * @throws DateTimeParseException if the input does not match the expected format
     */
    public static LocalDate parseUserDate(String input) {
        if (input == null) {
            throw new DateTimeParseException("Input is null", "", 0);
        }
        return LocalDate.parse(input.trim(), USER_DATE_FORMAT);
    }

}
