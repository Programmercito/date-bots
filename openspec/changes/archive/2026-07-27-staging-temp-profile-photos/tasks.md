# Tasks: Staging Temporary Profile Photos

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~280–350 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Staging temp photos + edit flow + tests | PR 1 | Single PR; main branch; tests and UI copy included |

## Phase 1: Foundation

- [x] 1.1 Add `tempPhotoFileIds` column to `src/main/java/org/osbo/bots/model/entity/User.java` mapped to `users.temp_photo_file_ids`.
- [x] 1.2 Add helper methods to `User.java`: `getTempPhotoList()`, `addTempPhoto(String fileId)`, `setTempPhotosFromProfile(Profile profile)`, `clearTempPhotos()`, `getTempPhotoCount()`.
- [x] 1.3 Verify `spring.jpa.hibernate.ddl-auto=update` in `src/main/resources/application.properties` will create the new nullable column.

## Phase 2: Core Implementation

- [x] 2.1 Modify `ClubProfileEditService.handleMenuSelection()` to seed `user.tempPhotoFileIds` from `profile.photoFileIds` (or `profile.photoFileId` fallback) when entering `club_edit_photo`.
- [x] 2.2 Update the `club_edit_photo` prompt text in `handleMenuSelection()` to explain staging, current count, `MAX_PHOTOS=10`, and that only "Listo, guardar" commits.
- [x] 2.3 Rewrite `ClubProfileEditService.handlePhoto()` to iterate `update.getMedias()` and append each valid `file_id` to `user.tempPhotoFileIds`, persisting `user` only.
- [x] 2.4 Enforce deduplication and `MAX_PHOTOS` limit in `handlePhoto()`, sending feedback for each photo, limit reached, or partial album ignored.
- [x] 2.5 Modify `ClubProfileEditService.handlePhotoDone()` to copy `user.tempPhotoFileIds` to `profile.photoFileIds` and `profile.photoFileId`, then clear the temp field.
- [x] 2.6 Ensure `saveAndReturnToMenu()` does not double-save or corrupt the committed `photoFileIds` during the photo-done path.

## Phase 3: Integration

- [x] 3.1 Update `cancelEdit()` in `ClubProfileEditService.java` to clear `user.tempPhotoFileIds` for `/start`, `/club`, and `CALLBACK_EDIT_CANCEL` without modifying the profile.
- [x] 3.2 Ensure `CALLBACK_EDIT_FINISH` from the edit menu clears any leftover `user.tempPhotoFileIds`.
- [x] 3.3 Add "Listo, guardar" button to the photo prompt so the user can explicitly commit staged photos.

## Phase 4: Testing

- [x] 4.1 Add unit tests for `User` helper methods: seed from profile, append, duplicate ignore, `MAX_PHOTOS` limit, and clear.
- [x] 4.2 Add service test for seeding temp photos from `profile.photoFileIds` and legacy single `photoFileId`.
- [x] 4.3 Add service test for album append, partial rejection when over limit, and per-photo feedback.
- [x] 4.4 Add service test for `CALLBACK_EDIT_PHOTO_DONE` committing staged photos and clearing temp state.
- [x] 4.5 Add service tests for cancel paths (`CALLBACK_EDIT_CANCEL`, `/start`, `/club`) verifying `profile.photoFileIds` unchanged and temp cleared.
- [x] 4.6 Add integration test verifying `users.temp_photo_file_ids` exists and is nullable on startup with `ddl-auto=update`.
- [x] 4.7 Run `./mvnw test` and fix regressions in existing registration and edit tests.

## Phase 5: Cleanup

- [x] 5.1 Remove dead code paths in `ClubProfileEditService.handlePhoto()` that previously mutated `profile.photoFileIds` directly.
- [x] 5.2 Confirm no changes are needed to `src/main/java/org/osbo/bots/model/services/ClubRegistrationService.java` because `/club` registration stays unchanged.

## Archive Status

- **Archived on**: 2026-07-27
- **Final task completion**: 17/17
- **Spec sync**: Created `openspec/specs/club-profile-edit/spec.md`
- **Verification**: 166/166 tests passed, no CRITICAL issues
- **Deviation from design**: None
