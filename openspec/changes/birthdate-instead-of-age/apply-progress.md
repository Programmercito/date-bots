# Apply Progress: Birthdate instead of age

## Status

- **Mode**: Strict TDD
- **Total tasks**: 17
- **Completed**: 17
- **Pending**: 0
- **Verification**: `mvnw test` passed (84 tests)

## Phase 1: Foundation

- [x] 1.1 RED: Create `AgeCalculatorTest` with fixed-clock scenarios
- [x] 1.2 GREEN: Create `AgeCalculator`
- [x] 1.3 REFACTOR: Extract parsing and age helpers
- [x] 1.4 Modify `Profile` with `birthDate`, nullable `age`, computed `getAge()`

## Phase 2: Core Implementation

- [x] 2.1 Replace registration age step with `DD/MM/AAAA` birthdate step
- [x] 2.2 `ModerationMessage` carries `birthDate` (age kept as legacy fallback)
- [x] 2.3 Admin moderation computes age from `birthDate`

## Phase 3: Integration

- [x] 3.1 Discovery captions use `Profile.getAge()`; report messages carry `birthDate`
- [x] 3.2 Match and `/mis_matches` captions use `Profile.getAge()`; report messages carry `birthDate`
- [x] 3.3 `AGENTS.md` updated to list `club_register_birthdate`

## Phase 4: Testing

- [x] 4.1 `ClubRegistrationServiceTest` covers valid birthdate, under-18, invalid format, and 18th-birthday acceptance
- [x] 4.2 `ClubDiscoveryServiceTest` asserts captions show calculated age and no raw birthdate
- [x] 4.3 `LikeMatchServiceTest` asserts match and match-list captions show calculated age
- [x] 4.4 `ReceiverForModerationTest` already verified age-only admin caption
- [x] 4.5 `RepositoryTests` verifies `birthDate` round-trip

## Phase 5: Cleanup

- [x] 5.1 No remaining references to `club_register_age` / `STATE_REGISTER_AGE`
- [x] 5.2 Full test suite passes (84 tests)

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1-1.4 | `AgeCalculatorTest.java`, `ProfileTest.java` | Unit | 12/12 baseline | Written | Passed | 3+ cases | Clean |
| 2.1 | `ClubRegistrationServiceTest.java` | Unit | 12/12 baseline | Written | Passed | Valid/invalid/under-18/18th-birthday | Clean |
| 2.2 | `ClubRegistrationServiceTest.java` | Unit | N/A | Written | Passed | birthDate set in moderation message | Clean |
| 2.3 | `ReceiverForModerationTest.java` | Unit | 4/4 baseline | Written | Passed | birthDate/legacy age/invalid birthDate | Clean |
| 3.1 | `ClubDiscoveryServiceTest.java` | Unit | 14/14 baseline | Written | Passed | caption age + report birthDate | Clean |
| 3.2 | `LikeMatchServiceTest.java` | Unit | 16/16 baseline | Passed | Passed | match/match-list age + report birthDate | Clean |
| 3.3 | `AGENTS.md` | Docs | N/A | N/A | Updated | N/A | N/A |
| 4.1 | `ClubRegistrationServiceTest.java` | Unit | 12/12 baseline | Written | Passed | 4 birthdate cases | Clean |
| 4.2 | `ClubDiscoveryServiceTest.java` | Unit | 14/14 baseline | Written | Passed | birthdate/legacy age | Clean |
| 4.3 | `LikeMatchServiceTest.java` | Unit | 16/16 baseline | Written | Passed | match + list | Clean |
| 4.4 | `ReceiverForModerationTest.java` | Unit | 4/4 baseline | Written | Passed | birthDate/legacy age/invalid birthDate | Clean |
| 4.5 | `RepositoryTests.java` | Integration | 6/6 baseline | Written | Passed | birthDate round-trip | Clean |
| 5.1 | All files | N/A | N/A | N/A | Verified | N/A | N/A |
| 5.2 | Full suite | N/A | 79/79 baseline | N/A | 84/84 passed | N/A | N/A |

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `src/main/java/org/osbo/bots/util/AgeCalculator.java` | Created | Bolivia-timezone age calculation with fixed-clock overload |
| `src/main/java/org/osbo/bots/model/entity/Profile.java` | Modified | Added `birthDate`, kept `age` nullable, computed `getAge()` |
| `src/main/java/org/osbo/bots/model/services/ClubRegistrationService.java` | Modified | Birthdate registration step, sends `birthDate` in moderation |
| `src/main/java/org/osbo/bots/model/services/ClubDiscoveryService.java` | Modified | Report messages carry `birthDate` |
| `src/main/java/org/osbo/bots/model/services/LikeMatchService.java` | Modified | Report messages carry `birthDate` |
| `AGENTS.md` | Modified | `club_register_age` → `club_register_birthdate` |
| `src/test/java/org/osbo/bots/util/AgeCalculatorTest.java` | Created | Fixed-clock age and parsing tests |
| `src/test/java/org/osbo/bots/model/entity/ProfileTest.java` | Modified | Computed age fallback scenarios |
| `src/test/java/org/osbo/bots/model/services/ClubRegistrationServiceTest.java` | Modified | Birthdate step tests |
| `src/test/java/org/osbo/bots/model/services/ClubDiscoveryServiceTest.java` | Modified | Age-only caption and report birthDate tests |
| `src/test/java/org/osbo/bots/model/services/LikeMatchServiceTest.java` | Modified | Age-only captions and report birthDate tests |
| `src/test/java/org/osbo/bots/jms/queue/receiver/ReceiverForModerationTest.java` | Modified | Birthdate → age-only admin caption tests |
| `src/test/java/org/osbo/bots/model/repositories/RepositoryTests.java` | Modified | `birthDate` round-trip test |

## Deviations from Design

None — implementation matches design.

## Issues Found

None.

## Workload / PR Boundary

- Mode: `single-pr` with maintainer-approved `size:exception`
- Target branch: `dev`
- One PR contains all changes

## Next Steps

- Create branch `feat/birthdate-instead-of-age` from `dev`
- Commit changes as reviewable work units
- Push branch and open PR to `dev`
- Link PR to approved issue
