# Delta for club-mis-matches

## MODIFIED Requirements

### Requirement: Match Details

For each mutual match, the system MUST display the matched user's name, calculated age, city, and available contact methods. The list MUST NOT include the raw birthdate.

(Previously: For each mutual match, the system MUST display the matched user's name, age, city, and available contact methods.)

#### Scenario: Match entry shows contact info

- GIVEN A is matched with B
- AND B's profile has birthdate `20/04/1996`, city, Telegram username, and WhatsApp
- WHEN A views `/mis_matches`
- THEN the entry shows B's name, `29 años`, city, Telegram button, and WhatsApp button
- AND the entry does not show `20/04/1996`

#### Scenario: Legacy profile with stored age

- GIVEN B has no birthdate and stored age 31
- WHEN A views `/mis_matches`
- THEN the entry shows `31 años`
