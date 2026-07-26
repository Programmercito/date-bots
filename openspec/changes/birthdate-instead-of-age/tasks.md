# Tasks: Birthdate instead of age

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~450-650 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: AgeCalculator + Profile + registration; PR 2: Moderation + display sites; PR 3: Tests + docs |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | AgeCalculator + Profile schema + registration birthdate step | PR 1 | Base: main |
| 2 | ModerationMessage + ReceiverForModeration + discovery/match captions | PR 2 | Targets PR 1 branch |
| 3 | Remaining tests + AGENTS.md | PR 3 | Targets PR 2 branch |

## Phase 1: Foundation

- [x] 1.1 RED: Create `src/test/java/org/osbo/bots/util/AgeCalculatorTest.java` with fixed-clock tests for valid adult, birthday-not-yet-reached, birthday-today, La Paz timezone boundary, legacy fallback, and null handling.
- [x] 1.2 GREEN: Create `src/main/java/org/osbo/bots/util/AgeCalculator.java` with `calculateAge(LocalDate, Integer, Clock)` and `parseUserDate(String)` methods.
- [x] 1.3 REFACTOR: Extract private helpers in AgeCalculator if parsing and age math overlap.
- [x] 1.4 Modify `src/main/java/org/osbo/bots/model/entity/Profile.java` to add `birthDate` column, make `age` nullable, and replace Lombok-generated `getAge()` with a computed accessor.

## Phase 2: Core Implementation

- [ ] 2.1 Replace `STATE_REGISTER_AGE` and `handleAge` in `src/main/java/org/osbo/bots/model/services/ClubRegistrationService.java` with `STATE_REGISTER_BIRTHDATE` and `handleBirthdate`, parse `DD/MM/AAAA`, reject under 18, and store `birthDate`.
- [ ] 2.2 Update `src/main/java/org/osbo/bots/jms/queue/pojos/ModerationMessage.java` to add `String birthDate` while keeping `Integer age` as a legacy fallback.
- [ ] 2.3 Update `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForModeration.java` to compute age from `birthDate` for the admin caption, falling back to `age`.

## Phase 3: Integration

- [ ] 3.1 Update `src/main/java/org/osbo/bots/model/services/ClubDiscoveryService.java` to use `Profile.getAge()` in profile captions and set `birthDate` in report messages.
- [ ] 3.2 Update `src/main/java/org/osbo/bots/model/services/LikeMatchService.java` to use `Profile.getAge()` in match and `/mis_matches` captions and set `birthDate` in report messages.
- [ ] 3.3 Update `AGENTS.md` to replace the `club_register_age` state with `club_register_birthdate`.

## Phase 4: Testing

- [ ] 4.1 Update `src/test/java/org/osbo/bots/model/services/ClubRegistrationServiceTest.java` with birthdate parsing, under-18 rejection, invalid format retry, and state transition tests.
- [ ] 4.2 Update `src/test/java/org/osbo/bots/model/services/ClubDiscoveryServiceTest.java` to assert captions show calculated age and never contain raw birthdate.
- [ ] 4.3 Update `src/test/java/org/osbo/bots/model/services/LikeMatchServiceTest.java` to assert match and `/mis_matches` captions show calculated age.
- [ ] 4.4 Update `src/test/java/org/osbo/bots/jms/queue/receiver/ReceiverForModerationTest.java` to verify admin caption shows age, not raw birthdate.
- [ ] 4.5 Update `src/test/java/org/osbo/bots/model/repositories/RepositoryTests.java` to assert `birthDate` round-trips through JPA.

## Phase 5: Cleanup

- [ ] 5.1 Remove any remaining references to `club_register_age` in registration service and tests.
- [ ] 5.2 Run `.\mvnw test` on Windows and fix regressions.
