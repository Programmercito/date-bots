# Proposal: Birthdate instead of age

## Intent

Manual age input becomes stale after a year and requires users to remember to update it. Collecting birthdate once keeps the displayed age accurate forever without extra user action.

## Scope

### In Scope
- Replace the registration age step with a `DD/MM/AAAA` birthdate step.
- Add `birth_date` to `Profile` and compute age with Bolivia (`America/La_Paz`) timezone.
- Display calculated age in preview, discovery, match notifications, `/mis_matches`, and admin moderation.
- Never expose raw birthdate in any UI or JMS DTO.
- Keep `age` nullable as a legacy fallback until the column is removed.

### Out of Scope
- Backfilling or migrating existing profiles (none exist in production).
- Editing or pausing profile flows.
- Changes to likes, filters, or contact collection logic.

## Capabilities

### New Capabilities
- `age-calculation`: utility/service to compute age from a birthdate in `America/La_Paz` timezone.

### Modified Capabilities
- `club-registration`: replace the age step with birthdate collection and validation.
- `club-match-notification`: display calculated age in match messages.
- `club-mis-matches`: display calculated age in the match list.
- `club-moderation` (admin review): display calculated age only, never the raw birthdate.

## Approach

1. Add `birthDate` to `Profile` and the SQLite schema; keep `age` nullable as a transitional fallback.
2. Introduce `AgeCalculator` using `LocalDate` and `ZoneId.of("America/La_Paz")`.
3. Replace `Profile.getAge()` with a computed accessor that uses `birthDate` when present, otherwise falls back to the stored `age`.
4. Change the registration state from `club_register_age` to `club_register_birthdate`; parse `DD/MM/AAAA` with `DateTimeFormatter`, validate the user is 18+, and store the ISO birthdate.
5. Update `ModerationMessage` to carry `birthDate` instead of `age`; compute age in `ReceiverForModeration` before rendering the admin caption.
6. Update `ClubDiscoveryService`, `LikeMatchService`, and the registration preview to call the computed accessor.
7. Add tests for valid/invalid dates, under-18 birthdates, and timezone edge cases.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `Profile` | Modified | Add `birthDate`, keep `age` nullable, computed `getAge()`. |
| `ClubRegistrationService` | Modified | Replace `handleAge` with birthdate parsing and validation. |
| `ClubDiscoveryService` | Modified | Build caption from computed age. |
| `LikeMatchService` | Modified | Match and `/mis_matches` captions use computed age. |
| `ModerationMessage` | Modified | Replace `age` with `birthDate`. |
| `ReceiverForModeration` | Modified | Compute age for admin caption. |
| Tests/docs | Modified | Update registration/discovery tests and `AGENTS.md`. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Users enter dates in wrong formats | Medium | Strict `DD/MM/AAAA` parser with a clear retry prompt and example. |
| Timezone edge cases (birthday today) | Low | Calculate age in `America/La_Paz`, not UTC. |
| Privacy leak of birthdate | Low | Store ISO date internally; render only calculated age everywhere. |
| Change exceeds review budget | Medium | Split into chained PRs: schema + registration, then display sites, then tests/docs. |

## Rollback Plan

1. Revert the code changes to `Profile`, registration, and caption builders.
2. Keep the `age` column nullable so reverted code can write it again.
3. If `birth_date` column causes issues, drop it after the revert.

## Dependencies

None.

## Success Criteria

- [ ] Registration accepts `DD/MM/AAAA` and rejects anyone under 18.
- [ ] All profile displays show the calculated age, never the birthdate.
- [ ] Admin moderation shows only the calculated age.
- [ ] Existing tests pass and new birthdate tests are added.
