# Proposal: Staging Temporary Profile Photos

## Intent

Fix two contradictions in the `/editar_perfil` photo step:

1. `ClubProfileEditService.handlePhoto()` appends every new photo to `profile.photoFileIds`, but the prompt tells the user "Las nuevas *reemplazarán* las actuales". The code does not match the copy.
2. Every photo is persisted immediately to the profile, so "Cancelar edición" cannot discard changes.

Introduce a temporary staging area on the `users` row. Edits accumulate there and are committed to the approved profile only when the user explicitly saves, making cancellation actually revert the photo changes.

## Scope

### In Scope
- Add `temp_photo_file_ids` to the `users` table/entity.
- Seed the temp field from the current profile photos when entering `club_edit_photo`.
- Append incoming photos to the temp field while in `club_edit_photo`, respecting `MAX_PHOTOS=10` and deduplicating by `file_id`.
- Support photo albums by iterating over the full `MessageUpdate.medias` array.
- Copy staged photos to `profile.photoFileIds` on "Listo, guardar" and clear the temp field.
- Clear the temp field on "Cancelar edición" without touching the profile.
- Update UI copy to describe staging behavior accurately.

### Out of Scope
- `/club` registration photo flow remains unchanged.
- No new payment, limits, or moderation rules.
- No migration script for existing `profile.photoFileIds`; seeding on entry handles current data.

## Capabilities

### New Capabilities
- `club-profile-edit`: Staging and committing profile photo changes during `/editar_perfil`.

### Modified Capabilities
- None.

## Approach

Store the working photo set in a new `User.tempPhotoFileIds` pipe-separated column. On entering the photo edit state, copy the approved profile's current photos into that field. Each incoming `MessageUpdate.medias` array is processed in order; every new valid `file_id` is appended to the temp list until `MAX_PHOTOS` is reached. The profile row is written only when the user taps "Listo, guardar", which copies the temp list to `profile.photoFileIds` and clears the temp field. "Cancelar edición" clears the temp field and returns to the edit menu without modifying the profile.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/org/osbo/bots/model/entity/User.java` | Modified | Add `tempPhotoFileIds` column. |
| `src/main/java/org/osbo/bots/model/services/ClubProfileEditService.java` | Modified | Stage photos in `User`, commit on done, clear on cancel, support album arrays. |
| `src/main/java/org/osbo/bots/jms/queue/pojos/MessageUpdate.java` | Unchanged | Already carries `String[] medias`; use all entries. |
| Database schema | Modified | Add nullable `users.temp_photo_file_ids` column. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Schema change on live SQLite | Low | Add nullable column; existing rows default to null and seed on first edit. |
| User cancels after sending photos | Low | Expected behavior; temp data is discarded. |
| Album messages arrive as multiple updates | Med | Process each `medias` array independently; dedupe by `file_id`. |

## Rollback Plan

1. Revert code changes in `User.java` and `ClubProfileEditService.java`.
2. Drop the `users.temp_photo_file_ids` column (or leave it nullable and unused).
3. Restore previous UI copy if needed.

## Dependencies

- None.

## Success Criteria

- [ ] Editing photos and tapping "Cancelar edición" leaves `profile.photoFileIds` unchanged.
- [ ] Editing photos and tapping "Listo, guardar" replaces `profile.photoFileIds` with the staged set.
- [ ] Sending a photo album in `club_edit_photo` appends all valid photos up to the limit.
- [ ] Duplicates are ignored and `MAX_PHOTOS` is enforced.
- [ ] Existing `/club` registration photo flow still works.
- [ ] `./mvnw test` passes.
