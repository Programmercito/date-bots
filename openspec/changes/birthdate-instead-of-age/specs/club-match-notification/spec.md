# Delta for club-match-notification

## MODIFIED Requirements

### Requirement: Profile Details

The match message MUST include the matched user's name, calculated age, gender, orientation, city, description, tastes, traits, and what they are looking for. The message MUST NOT include the raw birthdate.

(Previously: The match message MUST include the matched user's name, age, gender, orientation, city, description, tastes, traits, and what they are looking for.)

#### Scenario: Match message contains full profile

- GIVEN B has a complete APPROVED profile with birthdate `15/06/1998`
- WHEN A receives the match notification about B
- THEN the message contains B's name, `27 años`, city, and all profile fields
- AND the message does not contain `15/06/1998`

#### Scenario: Legacy profile with stored age

- GIVEN B has no birthdate and stored age 28
- WHEN A receives the match notification about B
- THEN the message contains `28 años`
