# club-moderation Specification

## Purpose

Present new profiles and reports to an admin for review, and compute the displayed age from the stored birthdate without exposing it.

## Requirements

### Requirement: Admin Notification

The system MUST consume a `ModerationMessage` from `queue.moderation` and send a caption with profile details and action buttons to the admin.

#### Scenario: New profile reaches admin

- GIVEN a `ModerationMessage` of type `NEW_PROFILE` for a complete profile
- WHEN the consumer processes it
- THEN the admin receives a caption and approve/reject/block buttons

### Requirement: Birthdate-Based Age Display

The system MUST compute the displayed age from the birthdate carried in the `ModerationMessage` and MUST NOT display the raw birthdate.

#### Scenario: Admin sees calculated age

- GIVEN a `ModerationMessage` with birthdate `12/09/1998`
- WHEN the admin caption is built
- THEN the caption contains `Edad: 27`
- AND the caption does not contain `12/09/1998`

#### Scenario: Legacy profile uses stored age

- GIVEN a `ModerationMessage` with no birthdate and age=28
- WHEN the admin caption is built
- THEN the caption contains `Edad: 28`

### Requirement: ModerationMessage Contract

The `ModerationMessage` DTO SHOULD carry `birthDate` instead of `age` for new profiles, while keeping `age` as an optional fallback for backward compatibility.

#### Scenario: New profile message carries birthdate

- GIVEN a submitted profile with birthdate `2000-03-15`
- WHEN the `ModerationMessage` is created
- THEN the message contains `birthDate=2000-03-15`
- AND the message does not contain `age`

### Requirement: Report Notifications

The system SHOULD include profile details for reported profiles when available.

#### Scenario: Reported profile moderation

- GIVEN a `ModerationMessage` of type `REPORT`
- WHEN the consumer processes it
- THEN the admin receives a caption with the reported profile details and action buttons
