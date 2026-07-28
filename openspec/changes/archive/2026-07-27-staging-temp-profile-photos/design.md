# Design: Staging Temporary Profile Photos

## Technical Approach

Introduce a working photo list on the `users` row. When an approved profile enters the photo-edit step, the current approved photos are copied into `users.temp_photo_file_ids`. Every incoming photo message (single or album) appends valid `file_id`s to that staged list, never to the approved `profiles` row. Only the explicit "Listo, guardar" action writes the staged list back to `profile.photo_file_ids`; "Cancelar edición", `/start`, or `/club` simply clears the staging column. This matches the spec scenarios and keeps the `profiles` row immutable during editing.

## Architecture Decisions

| Decision | Options | Tradeoffs | Chosen |
|---|---|---|---|
| Staging location | `users.temp_photo_file_ids` vs `profiles.draft_photo_file_ids` | Users row is already loaded/mutated by every step; profiles row should remain stable until commit. | `users` |
| Storage shape | Pipe-separated string vs one-to-many table | Existing `profile.photo_file_ids` uses pipe-separated; separate table adds schema complexity for a 10-item list. | Pipe-separated string |
| Schema migration | Hibernate `ddl-auto=update` vs manual script | Project has no Flyway/Liquibase; `ddl-auto=update` already manages schema. Manual scripts are risky on live SQLite. | `ddl-auto=update` |
| Helper methods | Entity methods vs utility class | Entity methods mirror `Profile.getPhotoList()`/`addPhoto()` and keep the logic discoverable. | Entity methods on `User` |
| Album handling | Loop all `MessageUpdate.medias` vs first only | Telegram albums arrive as a single update with multiple `file_id`s; looping all is required by spec. | Loop all entries in order |

## Data Flow

```
User taps "📷 Foto"
        │
        ▼
ClubProfileEditService.handleMenuSelection()
        │
        ├── copy profile.photoFileIds ──► user.tempPhotoFileIds
        └── send prompt with current count + "Listo, guardar" / "Cancelar"

User sends photo(s)
        │
        ▼
ClubProfileEditService.handlePhoto()
        │
        ├── for each medias[i]: user.addTempPhoto(fileId)
        ├── save user
        └── send count/feedback message

User taps ✅ Listo, guardar
        │
        ▼
ClubProfileEditService.handlePhotoDone()
        │
        ├── profile.photoFileIds = user.tempPhotoFileIds
        ├── profile.photoFileId  = first staged entry (or null)
        ├── user.tempPhotoFileIds = null
        └── saveAndReturnToMenu()

User taps ❌ Cancelar edición, /start, or /club
        │
        ▼
Clear user.tempPhotoFileIds and return without saving profile photos
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/org/osbo/bots/model/entity/User.java` | Modify | Add `tempPhotoFileIds` column and helper methods for list access, append, seed, clear, count. |
| `src/main/java/org/osbo/bots/model/services/ClubProfileEditService.java` | Modify | Seed staging on entering `club_edit_photo`; append full `update.getMedias()` array; commit on `CALLBACK_EDIT_PHOTO_DONE`; discard on cancel/`/start`/`/club`; update prompts. |
| `src/test/java/org/osbo/bots/model/services/ClubProfileEditServiceTest.java` | Modify | Add unit tests for staging seed, album append, deduplication, MAX_PHOTOS limit, commit, and discard paths. |

## Interfaces / Contracts

New helper API on `User`:

```java
public List<String> getTempPhotoList();
public boolean addTempPhoto(String fileId);
public void setTempPhotosFromProfile(Profile profile);
public void clearTempPhotos();
public int getTempPhotoCount();
```

Behavior contracts:
- `setTempPhotosFromProfile()` seeds from `profile.photoFileIds` falling back to `profile.photoFileId`; never mutates the profile.
- `addTempPhoto()` ignores null/blank values, deduplicates, enforces `MAX_PHOTOS`, and returns `true` if added.
- `clearTempPhotos()` sets `tempPhotoFileIds` to `null`.

Updated service contract for `ClubProfileEditService.handlePhoto()`:
- Input: user in `STATE_EDIT_PHOTO`, `MessageUpdate.medias` may contain one or more `file_id`s.
- Process: append each new valid `file_id` to `user.tempPhotoFileIds` in order until `MAX_PHOTOS` is reached.
- Output: send feedback (`"Foto N recibida ✅"`, limit message, or album partial-warning) and persist user only.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Staging seed from `photo_file_ids` and legacy `photo_file_id` | `ClubProfileEditServiceTest` with mocked repositories and sender |
| Unit | Album append, duplicate ignore, `MAX_PHOTOS` enforcement | Direct `User` helper + service-level tests |
| Unit | Commit (`CALLBACK_EDIT_PHOTO_DONE`) copies staged list to profile and clears temp | Service test verifying `profileRepository.save()` and user state |
| Unit | Discard (`CALLBACK_EDIT_CANCEL`, `/start`, `/club`) clears temp without touching profile | Service test asserting profile photos unchanged |
| Integration | Schema migration with `ddl-auto=update` | `@SpringBootTest` startup check that `users.temp_photo_file_ids` exists and is nullable |

No E2E tests are configured for this project.

## Migration / Rollout

No manual migration is required. The project uses `spring.jpa.hibernate.ddl-auto=update` against a local SQLite file. Adding a nullable `tempPhotoFileIds` column to `User.java` lets Hibernate create it automatically on startup. Existing rows default to `null`; the first entry into photo edit seeds the value from the approved profile.

Rollback:
1. Revert the two modified Java files.
2. Optionally drop the now-unused `users.temp_photo_file_ids` column (or leave it nullable and empty).

## Open Questions

None.
