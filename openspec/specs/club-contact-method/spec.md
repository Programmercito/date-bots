# club-contact-method Specification

## Purpose

Collect and validate at least one contact method from the user during friendship-club registration so matches can reach each other.

## Requirements

### Requirement: Contact Method Collection Step

After the profile photo is collected, the system MUST ask the user for a WhatsApp number or confirm their Telegram username before showing the registration preview.

#### Scenario: Registration reaches contact step

- GIVEN a user has just sent their profile photo
- WHEN the photo is accepted
- THEN the bot asks for a WhatsApp number or confirmation of the Telegram username
- AND the user's state becomes `club_register_contact`

### Requirement: Telegram Username Prefill

The system MUST prefill the Telegram username from the Telegram update when it is available, and allow the user to confirm or skip it.

#### Scenario: Telegram username is available

- GIVEN the Telegram update includes a username
- WHEN the contact step starts
- THEN the bot shows the username and asks the user to confirm or skip

### Requirement: WhatsApp Input Validation

The system MUST accept a free-text WhatsApp number and validate that it contains only digits, plus signs, hyphens, and spaces.

#### Scenario: Valid WhatsApp number

- GIVEN the user enters "+591 70012345"
- WHEN the input is validated
- THEN the WhatsApp number is accepted and stored

#### Scenario: Invalid WhatsApp number

- GIVEN the user enters "llámame"
- WHEN the input is validated
- THEN the bot asks for a valid WhatsApp number

### Requirement: At Least One Contact Method

The system MUST NOT allow the user to proceed to the preview until at least one of Telegram username or WhatsApp number is present.

#### Scenario: Both contact methods skipped

- GIVEN the user has no Telegram username
- AND the user skips the WhatsApp step
- WHEN the user tries to continue
- THEN the bot replies that at least one contact method is required

### Requirement: Contact Storage

The system MUST store the confirmed Telegram username in `profile.contact_username` and the validated WhatsApp number in `profile.whatsapp`.

#### Scenario: Contact methods stored

- GIVEN the user provides Telegram username "@ana" and WhatsApp "+59170012345"
- WHEN the user reaches the preview
- THEN `profile.contact_username` is "ana"
- AND `profile.whatsapp` is "+59170012345"
