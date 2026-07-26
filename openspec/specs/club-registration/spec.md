# club-registration Specification

## Purpose

Guide a user through creating a friendship-club profile, including collecting a valid contact method before submitting the profile for moderation.

## Requirements

### Requirement: Start Registration

The system MUST start a new INCOMPLETE profile when a user invokes `/club` or taps "Entrar al club de amistad" and no profile exists.

#### Scenario: New user starts registration

- GIVEN a user has no profile
- WHEN the user sends `/club`
- THEN an INCOMPLETE profile is created
- AND the bot asks for the user's name

### Requirement: Age Validation

The system MUST reject ages below 18 and ask for a valid age.

#### Scenario: Underage user is rejected

- GIVEN the user enters age 16
- WHEN the age is validated
- THEN the bot replies that the user must be at least 18
- AND the profile is not advanced

### Requirement: Allowed Combinations

The system MUST only allow the combinations male hetero, female hetero, and female bi; any other combination MUST be rejected and the INCOMPLETE profile MUST be deleted.

#### Scenario: Invalid combination is rejected

- GIVEN the user selects male and bi
- WHEN the orientation is validated
- THEN the bot explains the allowed combinations
- AND the INCOMPLETE profile is removed

### Requirement: Contact Method Step

After collecting the profile photo, the system MUST collect and validate at least one contact method (Telegram username or WhatsApp number) before showing the preview.

#### Scenario: User provides WhatsApp

- GIVEN the user has no Telegram username
- AND the user enters a valid WhatsApp number
- WHEN the contact step completes
- THEN the profile has the WhatsApp number stored
- AND the preview is shown

#### Scenario: No contact method provided

- GIVEN the user has no Telegram username
- AND the user skips the WhatsApp step
- WHEN the user tries to continue
- THEN the bot replies that at least one contact method is required

### Requirement: Preview and Confirmation

The system MUST display a preview of the complete profile and ask the user to confirm or edit before submission.

#### Scenario: User confirms profile

- GIVEN the user has completed all registration steps
- WHEN the user taps "Confirmar"
- THEN the profile status becomes PENDING
- AND a `ModerationMessage` is sent to `queue.moderation`
