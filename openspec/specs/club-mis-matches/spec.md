# club-mis-matches Specification

## Purpose

Allow users to list their current mutual matches and view each match's profile summary and contact information.

## Requirements

### Requirement: Command Handler

The system MUST handle the `/mis_matches` command and the "Mis matches" inline button.

#### Scenario: User opens match list

- GIVEN user A has an APPROVED profile
- WHEN A sends `/mis_matches`
- THEN the bot replies with a list of A's mutual matches

### Requirement: Mutual Match Query

The system MUST query the `likes` table for rows where `matched=true` and the requesting user is either `from_chatid` or `to_chatid`, then collect the opposite chat IDs.

#### Scenario: List with two matches

- GIVEN A has mutual matches with B and C
- WHEN A requests `/mis_matches`
- THEN the response includes both B and C

### Requirement: Match Details

For each mutual match, the system MUST display the matched user's name, age, city, and available contact methods.

#### Scenario: Match entry shows contact info

- GIVEN A is matched with B
- AND B's profile has name, age, city, Telegram username, and WhatsApp
- WHEN A views `/mis_matches`
- THEN the entry shows B's name, age, city, Telegram button, and WhatsApp button

### Requirement: Empty State

If the user has no mutual matches, the system MUST send a message indicating there are no matches yet.

#### Scenario: No matches yet

- GIVEN A has no `matched=true` like rows
- WHEN A sends `/mis_matches`
- THEN the bot replies with a friendly message saying there are no matches

### Requirement: Restricted Access

The system SHOULD require an APPROVED profile before listing matches. If the user has no APPROVED profile, the system MUST invite them to register or wait for approval.

#### Scenario: Unapproved user tries listing matches

- GIVEN A has no APPROVED profile
- WHEN A sends `/mis_matches`
- THEN the bot replies that an approved profile is required
