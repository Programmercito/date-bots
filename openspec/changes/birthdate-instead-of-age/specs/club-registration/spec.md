# Delta for club-registration

## ADDED Requirements

### Requirement: Birthdate Privacy

The system MUST store the ISO birthdate in `profile.birth_date` and MUST NOT expose it in UI or JMS DTOs.

#### Scenario: Birthdate stored privately

- GIVEN a user enters `15/03/2000` during registration
- WHEN the profile is saved
- THEN `birth_date` is stored as `2000-03-15`
- AND the raw date is never sent in a Telegram message or `ModerationMessage`

### Requirement: Birthdate Format Guidance

The system MUST ask for `DD/MM/AAAA` and provide an example when the input is invalid.

#### Scenario: Invalid format receives example

- GIVEN a user is on the birthdate step and enters `03-15-2000`
- WHEN the input is parsed
- THEN the bot replies with a retry prompt including the example `15/03/2000`

## MODIFIED Requirements

### Requirement: Birthdate Collection and Validation

The system MUST replace the `club_register_age` step with `club_register_birthdate`, accept a `DD/MM/AAAA` string, parse it to an ISO birthdate, reject any birthdate that makes the user younger than 18, and persist the valid birthdate.

(Previously: The system MUST reject ages below 18 and ask for a valid age.)

#### Scenario: Valid adult birthdate accepted

- GIVEN the user is on the birthdate step
- AND the user enters `15/03/2000`
- WHEN the birthdate is validated against `America/La_Paz`
- THEN the profile stores `birth_date=2000-03-15`
- AND the state advances to `club_register_gender`

#### Scenario: Under-18 birthdate rejected

- GIVEN the user is on the birthdate step
- AND the user enters `15/03/2010`
- WHEN the birthdate is validated against `America/La_Paz`
- THEN the bot replies that the user must be at least 18
- AND the state remains `club_register_birthdate`

#### Scenario: Invalid birthdate format rejected

- GIVEN the user is on the birthdate step
- AND the user enters `15-03-2000`
- WHEN the birthdate is parsed
- THEN the bot replies with an example format `DD/MM/AAAA`
- AND the state remains `club_register_birthdate`

#### Scenario: Birthday today for an 18-year-old

- GIVEN the user is on the birthdate step
- AND the user enters `26/07/2008` (current date is 26/07/2026 in `America/La_Paz`)
- WHEN the birthdate is validated
- THEN the user is accepted as 18
- AND the state advances to `club_register_gender`

### Requirement: Preview and Confirmation

The system MUST display a preview of the complete profile, including the calculated age, and ask the user to confirm or edit before submission.

(Previously: The system MUST display a preview of the complete profile and ask the user to confirm or edit before submission.)

#### Scenario: User confirms profile

- GIVEN the user has completed all registration steps including birthdate
- WHEN the user taps "Confirmar"
- THEN the profile status becomes PENDING
- AND a `ModerationMessage` is sent to `queue.moderation`

## REMOVED Requirements

### Requirement: Age Validation

(Reason: Replaced by Birthdate Collection and Validation. Age is now derived from the persisted birthdate.)
(Migration: Update tests and UI strings that expect an age input prompt to expect a birthdate prompt and `DD/MM/AAAA` validation.)
