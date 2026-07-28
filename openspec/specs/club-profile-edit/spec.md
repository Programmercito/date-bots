# club-profile-edit Specification

## Purpose

Define the `/editar_perfil` photo-editing flow using a temporary staging area on the `users` row. Changes are committed to the approved profile only when the user taps *Listo, guardar*; cancelled changes are discarded.

## Requirements

### Requirement: Staging column

The system MUST add a nullable `users.temp_photo_file_ids` column for the working photo list.

#### Scenario: Default column

- GIVEN a new or migrated user row
- THEN `temp_photo_file_ids` is null

### Requirement: Seed staging on entry

When the user enters `club_edit_photo`, the system SHALL copy `profile.photo_file_ids` into `users.temp_photo_file_ids` before showing the prompt.

#### Scenario: Profile with photos

- GIVEN an approved profile with `photo_file_ids` = `A|B`
- WHEN the user enters photo edit
- THEN `users.temp_photo_file_ids` = `A|B`
- AND `profile.photo_file_ids` remains `A|B`

#### Scenario: Profile without photos

- GIVEN an approved profile with no photos
- WHEN the user enters photo edit
- THEN `users.temp_photo_file_ids` is empty
- AND `profile.photo_file_ids` remains unchanged

### Requirement: Append incoming photos

While the user is in `club_edit_photo`, the system SHALL append each valid `file_id` from `MessageUpdate.medias` to the staged list, in order, up to `MAX_PHOTOS=10`. The system MUST NOT store duplicates or modify the `profiles` row until commit.

#### Scenario: Single photo

- GIVEN `users.temp_photo_file_ids` = `A`
- WHEN the user sends photo `B`
- THEN `users.temp_photo_file_ids` = `A|B`
- AND `profile.photo_file_ids` remains `A`

#### Scenario: Album larger than remaining slots

- GIVEN `users.temp_photo_file_ids` = `A|B|C|D|E|F|G|H`
- WHEN the user sends a 5-photo album
- THEN only the first 2 new photos are appended
- AND the rest are ignored
- AND the bot reports the limit

#### Scenario: Duplicate ignored

- GIVEN `users.temp_photo_file_ids` = `A|B`
- WHEN the user sends photo `A`
- THEN `users.temp_photo_file_ids` remains `A|B`

#### Scenario: Non-photo message ignored

- GIVEN the user is in `club_edit_photo`
- WHEN the user sends text
- THEN the staged list is unchanged
- AND the bot asks for a photo or shows done/cancel options

### Requirement: Commit staged photos

When the user taps `CALLBACK_EDIT_PHOTO_DONE`, the system SHALL copy `users.temp_photo_file_ids` into `profile.photo_file_ids`, set `profile.photo_file_id` to the first staged entry, clear `users.temp_photo_file_ids`, and return to the edit menu.

#### Scenario: Save replaces photos

- GIVEN `users.temp_photo_file_ids` = `A|B|C`
- WHEN the user taps `CALLBACK_EDIT_PHOTO_DONE`
- THEN `profile.photo_file_ids` = `A|B|C`
- AND `profile.photo_file_id` = `A`
- AND `users.temp_photo_file_ids` is cleared

#### Scenario: Save with no staged photos

- GIVEN `users.temp_photo_file_ids` is empty
- WHEN the user taps `CALLBACK_EDIT_PHOTO_DONE`
- THEN `profile.photo_file_ids` is cleared
- AND `profile.photo_file_id` is cleared
- AND `users.temp_photo_file_ids` is cleared

### Requirement: Discard staged photos

When the user taps `CALLBACK_EDIT_CANCEL`, or issues `/start` or `/club`, the system SHALL clear `users.temp_photo_file_ids` without touching `profile.photo_file_ids`.

#### Scenario: Cancel discards changes

- GIVEN `users.temp_photo_file_ids` = `A|B|C`
- WHEN the user taps `CALLBACK_EDIT_CANCEL`
- THEN `profile.photo_file_ids` remains unchanged
- AND `users.temp_photo_file_ids` is cleared

### Requirement: Backward compatibility

The system MUST keep existing `profile.photo_file_ids` semantics for discovery, moderation, and match notifications. Profiles with only `profile.photo_file_id` MUST remain discoverable.

#### Scenario: Single-photo legacy profile

- GIVEN a profile with `photo_file_id` = `A` and null `photo_file_ids`
- WHEN the user enters photo edit
- THEN `users.temp_photo_file_ids` is seeded as `A`
- AND discovery still shows `A`

### Requirement: Accurate UI copy

The system SHALL update the `club_edit_photo` prompt so it explains that new photos are staged, the limit is `MAX_PHOTOS=10`, and only *Listo, guardar* saves them.

#### Scenario: Prompt text

- GIVEN the user enters photo edit
- WHEN the bot sends the prompt
- THEN it states photos are staged and only *Listo, guardar* commits them

### Requirement: Maximum photo limit

The system SHALL enforce `MAX_PHOTOS=10` while appending to `users.temp_photo_file_ids`.

#### Scenario: Limit reached

- GIVEN the staged list already has 10 photos
- WHEN the user sends another photo
- THEN it is ignored
- AND the bot informs the user
