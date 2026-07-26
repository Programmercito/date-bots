# club-match-notification Specification

## Purpose

Send a rich mutual-match notification to both users when a mutual like is detected, including profile details, contact buttons, and a report button.

## Requirements

### Requirement: Rich Match Message

The system MUST build and send a rich match message to both users when a `MatchMessage` is consumed from `queue.match`.

#### Scenario: Mutual match notifies both users

- GIVEN users A and B have mutually liked each other
- WHEN a `MatchMessage` arrives on `queue.match`
- THEN A receives a match message about B
- AND B receives a match message about A

### Requirement: Profile Details

The match message MUST include the matched user's name, age, gender, orientation, city, description, tastes, traits, and what they are looking for.

#### Scenario: Match message contains full profile

- GIVEN B has a complete APPROVED profile
- WHEN A receives the match notification about B
- THEN the message contains B's name, age, city, and all profile fields

### Requirement: Profile Photo

The match message SHOULD include the matched user's profile photo when the `file_id` is valid.

#### Scenario: Match message with photo

- GIVEN B's profile has a valid `photo_file_id`
- WHEN A receives the match notification
- THEN the message is sent as a photo with the profile details as caption

### Requirement: Photo Fallback

If the photo cannot be sent because the `file_id` is invalid, the system MUST fall back to a text-only message and MUST NOT change the profile status.

#### Scenario: Broken photo in match notification

- GIVEN B's `photo_file_id` is no longer valid
- WHEN A receives the match notification
- THEN A receives the profile details as a plain text message
- AND B's profile status remains APPROVED

### Requirement: Contact Buttons

The match message MUST include buttons for contacting the matched user via Telegram username and/or WhatsApp when those contact methods are present.

#### Scenario: Match with both contact methods

- GIVEN B has a Telegram username and a WhatsApp number
- WHEN A receives the match notification
- THEN A sees a Telegram button linking to B's Telegram
- AND A sees a WhatsApp button linking to B's WhatsApp chat

### Requirement: Report Button

The match message MUST include a "Reportar" button that submits the matched profile to `queue.moderation`.

#### Scenario: Reporting a match

- GIVEN A receives a match notification about B
- WHEN A taps the "Reportar" button
- THEN a `ModerationMessage` of type `REPORT` is sent to `queue.moderation`
