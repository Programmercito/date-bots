# Tasks: phase-5-likes-and-matches

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 700-900 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: registration contact method; PR 2: like processing; PR 3: match notification + mis_matches |
| Delivery strategy | ask-always |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Contact method in registration | PR 1 | `Profile.whatsapp`, `LikeRepository` finder, registration tests |
| 2 | Like processing service + receiver | PR 2 | `LikeMatchService.processLike`, `ReceiverForLike`, tests |
| 3 | Match notification + mis_matches | PR 3 | `notifyMatch`, `listMatches`, `ReceiverForMatch`, `/mis_matches` |

## Phase 1: Foundation

- [x] 1.1 Add `whatsapp` field to `src/main/java/org/osbo/bots/model/entity/Profile.java`.
- [x] 1.2 Add `findByFromChatidOrToChatidAndMatchedTrue` to `src/main/java/org/osbo/bots/model/repositories/LikeRepository.java`.
- [x] 1.3 Add `club_register_contact` state in `src/main/java/org/osbo/bots/model/services/ClubRegistrationService.java` after photo collection.

## Phase 2: Core Implementation

- [x] 2.1 Create `src/main/java/org/osbo/bots/model/services/LikeMatchService.java` with `processLike`, `notifyMatch`, `listMatches`.
- [x] 2.2 Modify `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForLike.java` to delegate to `LikeMatchService.processLike`.
- [x] 2.3 Modify `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForMatch.java` to delegate to `LikeMatchService.notifyMatch`.
- [x] 2.4 Modify `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForProcess.java` to route `/mis_matches` to `LikeMatchService.listMatches`.

## Phase 3: Testing (Strict TDD)

- [x] 3.1 RED: write `src/test/java/org/osbo/bots/model/services/LikeMatchServiceTest.java` for persist/match/unavailable/duplicate like scenarios.
- [x] 3.2 GREEN: implement `LikeMatchService.processLike` to pass tests.
- [x] 3.3 RED: add match-notification and photo-fallback tests to `LikeMatchServiceTest.java`.
- [x] 3.4 GREEN: implement `LikeMatchService.notifyMatch` to pass tests.
- [x] 3.5 RED: add `/mis_matches` query and empty-state tests to `LikeMatchServiceTest.java`.
- [x] 3.6 GREEN: implement `LikeMatchService.listMatches` to pass tests.
- [x] 3.7 REFACTOR: extract common caption/button builders in `LikeMatchService`.
- [x] 3.8 RED: add contact-step validation tests to `src/test/java/org/osbo/bots/model/services/ClubRegistrationServiceTest.java`.
- [x] 3.9 GREEN: implement contact method validation in `ClubRegistrationService.java`.
- [x] 3.10 RED: write `src/test/java/org/osbo/bots/jms/queue/receiver/ReceiverForLikeTest.java`.
- [x] 3.11 GREEN: wire `ReceiverForLike` to `LikeMatchService`.
- [x] 3.12 RED: write `src/test/java/org/osbo/bots/jms/queue/receiver/ReceiverForMatchTest.java`.
- [x] 3.13 GREEN: wire `ReceiverForMatch` to `LikeMatchService`.

## Phase 4: Integration

- [x] 4.1 Implement `club_match_report_<chatid>` callback in `LikeMatchService` to send `REPORT` `ModerationMessage` to `queue.moderation`.
- [x] 4.2 Verify `src/main/java/org/osbo/bots/jms/queue/pojos/MatchMessage.java` carries both chat IDs and add fields if needed.
- [x] 4.3 Update `docs/design-club-amistad.md` contact-method and match sections.

## Phase 5: Cleanup

- [x] 5.1 Remove stub logging from `ReceiverForLike.java` and `ReceiverForMatch.java`.
- [x] 5.2 Run `.\mvnw test` and fix failures.
