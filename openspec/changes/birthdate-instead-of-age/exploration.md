## Exploration: birthdate-instead-of-age

### Current State

The friendship-club registration flow asks the user for an integer age (`¿Cuántos años tenés?`), stores it in `Profile.age`, and displays that integer everywhere a profile is rendered:

- `ClubRegistrationService` validates `age >= 18` in `handleAge` and stores `profile.setAge(age)`.
- `Profile` entity persists `age` as `Integer`.
- `ClubRegistrationService.buildProfilePreview()` shows `Edad: {age}`.
- `ClubDiscoveryService.buildProfileCaption()` shows `{name}, {age} años`.
- `LikeMatchService` shows age in match notifications and in the `/mis_matches` list.
- `ReceiverForModeration.buildAdminCaption()` shows `Edad: {age}` from the `ModerationMessage.age` field.
- `ModerationMessage` carries `age` as an `Integer`.
- `ClubRegistrationServiceTest` and `ClubDiscoveryServiceTest` set/assert `age` directly.

No birthdate is collected anywhere. The value is write-once during registration and never recomputed.

### Affected Areas

- `src/main/java/org/osbo/bots/model/entity/Profile.java` — add `birthDate` storage; remove or deprecate `age`.
- `src/main/java/org/osbo/bots/model/services/ClubRegistrationService.java` — replace `STATE_REGISTER_AGE`/`handleAge` with birthdate collection and age-from-birthdate validation.
- `src/main/java/org/osbo/bots/model/services/ClubDiscoveryService.java` — caption must call an age calculator instead of reading `profile.getAge()` directly.
- `src/main/java/org/osbo/bots/model/services/LikeMatchService.java` — match caption and `/mis_matches` list must show calculated age.
- `src/main/java/org/osbo/bots/jms/queue/pojos/ModerationMessage.java` — replace `age` with `birthDate` (or keep age computed).
- `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForModeration.java` — admin caption must show calculated age, not raw birthdate.
- `src/main/java/org/osbo/bots/model/services/ClubModerationService.java` — no direct age change, but profile preview/moderation paths may need helper.
- `src/test/java/org/osbo/bots/model/services/ClubRegistrationServiceTest.java` — replace age tests with birthdate parsing tests.
- `src/test/java/org/osbo/bots/model/services/ClubDiscoveryServiceTest.java` — set `birthDate` on test fixtures or stub the age helper.
- `docs/design-club-amistad.md` and `AGENTS.md` — update state machine and data model descriptions.
- SQLite schema — add `birth_date` column; decide what to do with existing `age` values.

### Approaches

1. **Store birth_date only, calculate age on every read**
   - Replace `Profile.age` with `Profile.birthDate` (ISO `yyyy-MM-dd`).
   - Add a domain helper `AgeCalculator` using `America/La_Paz` timezone.
   - All caption builders call the helper.
   - Pros: single source of truth; no stale ages; no privacy leak of birthdate.
   - Cons: touches every display site; existing profiles with `age` only need a migration strategy.
   - Effort: Medium.

2. **Store birth_date AND keep age as a denormalized cache**
   - Add `birthDate` to `Profile`, keep `age`.
   - Compute age at registration and store it.
   - Display sites continue reading `profile.getAge()`.
   - Pros: minimal change to discovery/match/moderation builders; backwards compatible.
   - Cons: duplicated data; age becomes stale after each birthday; violates the intent of collecting birthdate.
   - Effort: Low.

3. **Store birth_date, expose a computed `getAge()` method on `Profile`, keep `age` for legacy migration**
   - Add `birthDate` column; keep `age` nullable for existing profiles.
   - `Profile.getAge()` computes from `birthDate` when present, falls back to stored `age`.
   - Gradually backfill or require re-registration; eventually drop `age`.
   - Pros: pragmatic migration; display sites still call `getAge()`; new code is clean.
   - Cons: transitional data model; still need to update builders that currently read `age` directly.
   - Effort: Medium.

### Recommendation

Use **Approach 3** as the transition path:

- Add `birth_date` to `Profile` and the SQLite schema. Keep `age` nullable temporarily as a legacy fallback.
- Replace `getAge()` with a computed accessor that uses `birthDate` first and falls back to `age`.
- Create a small `AgeCalculator` utility using the `America/La_Paz` zone for Bolivia.
- Replace the registration `age` step with a `birthdate` step that asks for `DD/MM/AAAA` (Spanish copy), parses with `DateTimeFormatter`, validates the user is 18+, and stores the ISO birthdate.
- Update `ModerationMessage` to carry `birthDate` instead of `age`; compute age in `ReceiverForModeration` before displaying.
- Update all caption builders to call the computed accessor or the calculator.
- Add tests for valid/invalid dates, under-18 birthdates, and Bolivia timezone edge cases.
- Once all existing profiles are backfilled or re-registered, drop the `age` column.

### Risks

- **Existing data migration**: approved profiles currently have `age` but no `birth_date`. The fallback handles display, but the admin cannot see the true birthdate. A future re-registration or backfill is needed before removing the `age` column.
- **Invalid date formats**: users may type `05-06-1998`, `5/6/98`, or free text. Need a clear parse error and one-liner retry prompt.
- **Timezone edge cases**: a user whose birthday is today in Bolivia may be counted as under-18 if computed in UTC. The calculator must use `America/La_Paz`.
- **Under-18 boundary**: the calculation must reject anyone who has not yet turned 18 on the Bolivia local date.
- **Privacy**: birthdate must never appear in discovery, match, preview, or moderation UIs. Only the calculated age is shown.
- **Line count**: this change spans the entity, registration service, three caption builders, the JMS DTO, tests, and docs. It will likely exceed the 400-line review budget, so the implementation should be split into chained PRs (e.g., schema + registration, then display sites, then tests/docs).

### Ready for Proposal

Yes. The scope is clear, the affected files are identified, and the recommended approach balances a clean target model with a safe migration for existing profiles.
