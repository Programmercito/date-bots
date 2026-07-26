# club-discovery Specification

## Purpose

Show approved profiles one at a time to a browsing user, allow like/skip/report actions, and keep the chat clean by editing the current message.

## Requirements

### Requirement: Approved Profile Required

The system MUST require an APPROVED profile before showing discovery profiles.

#### Scenario: Unapproved user sees guidance

- GIVEN user A has no APPROVED profile
- WHEN A sends `/ver_personas`
- THEN the bot replies that an approved profile is required

### Requirement: Filtered Profile Selection

The system MUST select the next approved profile matching the viewer's gender/orientation filters, excluding the viewer, already liked profiles, and blocked users.

#### Scenario: Male hetero sees female hetero first

- GIVEN a male hetero viewer and an approved female hetero target
- WHEN the viewer requests `/ver_personas`
- THEN the target is shown

### Requirement: Age Display

The system MUST display the calculated age from `birth_date` in the profile caption; if `birth_date` is absent it MUST fall back to the stored `age` column, and it MUST NOT show the raw birthdate.

#### Scenario: Caption shows calculated age

- GIVEN a target profile with birthdate `10/05/1998` and no stored age
- WHEN the viewer sees the profile
- THEN the caption contains `28 años` and does not contain `10/05/1998`

#### Scenario: Legacy profile without birthdate

- GIVEN a target profile with no birthdate and stored age 30
- WHEN the viewer sees the profile
- THEN the caption contains `30 años`

### Requirement: Single-Message UX

The system MUST edit the current message when the user likes or skips, and delete the previous message before sending the next profile.

#### Scenario: Like action edits the current message

- GIVEN the viewer is browsing a profile of Maria
- WHEN the viewer taps Like
- THEN the same message is edited to `Le diste like a Maria ❤️`

### Requirement: Report

The system SHOULD allow the viewer to report a profile and notify the admin.

#### Scenario: Profile reported

- GIVEN the viewer is browsing a profile
- WHEN the viewer taps Reportar
- THEN the message is edited to a confirmation
- AND a `ModerationMessage` of type `REPORT` is sent to `queue.moderation`
