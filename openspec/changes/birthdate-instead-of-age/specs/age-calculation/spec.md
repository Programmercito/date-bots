# age-calculation Specification

## Purpose

Compute a profile's age from a stored birthdate in the Bolivia (`America/La_Paz`) timezone, with a fallback to the legacy `age` column.

## Requirements

### Requirement: Birthdate-Based Age

The system MUST calculate age as the number of full years between the birthdate and the current date in `America/La_Paz`.

#### Scenario: Valid adult birthdate

- GIVEN a profile with birthdate `15/03/2000` and current date `26/07/2026` in `America/La_Paz`
- WHEN the age is requested
- THEN the returned age is 26

#### Scenario: Birthday not yet reached this year

- GIVEN a profile with birthdate `10/12/2000` and current date `26/07/2026` in `America/La_Paz`
- WHEN the age is requested
- THEN the returned age is 25

#### Scenario: Birthday today

- GIVEN a profile with birthdate `26/07/2000` and current date `26/07/2026` in `America/La_Paz`
- WHEN the age is requested
- THEN the returned age is 26

### Requirement: Bolivia Timezone

The system MUST evaluate the current date using `America/La_Paz` rather than the JVM default or UTC.

#### Scenario: New Year's boundary in La Paz

- GIVEN the UTC time is `2027-01-01T03:00:00Z` (La Paz time is `2026-12-31T23:00`)
- WHEN the age is requested for a birthdate `31/12/2000`
- THEN the returned age is 25

### Requirement: Legacy Age Fallback

The system MUST fall back to the stored `age` column when `birthDate` is null.

#### Scenario: Profile without birthdate

- GIVEN a profile with `age=28` and no `birthDate`
- WHEN the age is requested
- THEN the returned age is 28

### Requirement: Null Handling

The system MUST return null when both `birthDate` and `age` are absent.

#### Scenario: Missing birthdate and age

- GIVEN a profile with neither `birthDate` nor `age`
- WHEN the age is requested
- THEN the returned age is null

### Requirement: No Birthdate Exposure

The system MUST NOT expose the raw `birthDate` value in any UI caption, JMS DTO, or admin review.

#### Scenario: Caption uses age only

- GIVEN a profile with birthdate `05/05/1995`
- WHEN a profile caption is rendered
- THEN the caption shows `29 años` and does not contain `05/05/1995`
