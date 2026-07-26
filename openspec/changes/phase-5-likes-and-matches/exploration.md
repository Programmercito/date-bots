## Exploration: phase-5-likes-and-matches

### Current State

Phase 5 (likes and mutual matches) is currently stubbed out:

- `ReceiverForLike` only logs the incoming `LikeMessage` and does nothing else.
- `ReceiverForMatch` only logs the incoming `MatchMessage` and does nothing else.
- `ClubDiscoveryService.handleLike()` already edits the current discovery message to confirm the like, then enqueues a `LikeMessage` on `queue.like` and tracks an analytics event.
- `Like` entity already has the required fields (`fromChatid`, `toChatid`, `matched`, `createdAt`) and a unique constraint on `(from_chatid, to_chatid)`.
- `LikeRepository` exposes `findByFromChatidAndToChatid` and `findByFromChatid`, which is sufficient for mutual-match detection.
- `MatchMessage` is a plain pair of chat IDs (`chatidA`, `chatidB`).
- `ReceiverForSend` already handles broken `file_id` during discovery by marking the target profile as `REJECTED` and notifying the owner; this is unrelated to match notification and should not be affected.

The discovery side of the flow is complete. The missing piece is the asynchronous processing of the like queue and the match notification queue.

### Affected Areas

- `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForLike.java` — needs to persist the like, detect the reverse like, and enqueue a `MatchMessage` when both likes exist.
- `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForMatch.java` — needs to look up both profiles and notify both users with their contact usernames.
- `src/main/java/org/osbo/bots/model/services/` — a new `LikeMatchService` (or similar) is the natural place for the business logic, following the existing `ClubDiscoveryService` / `ClubModerationService` pattern.
- `src/main/java/org/osbo/bots/model/repositories/LikeRepository.java` — may need an additional finder for the reverse like (`findByFromChatidAndToChatid` already exists, so no change is required unless we want to add `findByToChatid` for the "who liked me" feature in Phase 6).
- `src/main/java/org/osbo/bots/jms/queue/pojos/LikeMessage.java` and `MatchMessage.java` — already sufficient; no change needed.
- `src/main/java/org/osbo/bots/jms/queue/enqueue/NqueueForSend.java` — already provides the needed send methods; no change needed.
- `src/test/java/.../jms/queue/receiver/` and `src/test/java/.../model/services/` — will need new unit tests for `ReceiverForLike`, `ReceiverForMatch`, and the new service.
- `docs/design-club-amistad.md` sections 8.3 and 8.4 — describe the intended behavior; implementation should align with them.

### Approaches

1. **Fat consumers** — Put all logic inside `ReceiverForLike` and `ReceiverForMatch`.
   - Pros: Fewer new classes, quick to implement.
   - Cons: Violates the project convention that business logic lives in services and `ReceiverForProcess` (and by extension queue consumers) act as dispatchers. Harder to unit test because JMS listeners need Spring context or heavy mocking.
   - Effort: Low

2. **Thin consumers + dedicated service** — Add a `LikeMatchService` (or `ClubLikeService`) that owns the like persistence, mutual-match detection, and match-message construction. `ReceiverForLike` and `ReceiverForMatch` delegate to it and only handle queue plumbing.
   - Pros: Matches the existing `ClubDiscoveryService` / `ClubModerationService` architecture, keeps queue consumers simple, easy to test with plain JUnit + Mockito, and keeps transactions short and explicit.
   - Cons: One additional service class and one additional test class.
   - Effort: Medium

3. **Service split by direction** — Separate `LikeProcessingService` for `queue.like` and `MatchNotificationService` for `queue.match`.
   - Pros: Very small, focused classes.
   - Cons: The like processing and match notification are tightly coupled (one produces the message the other consumes), so splitting them adds indirection without clear benefit. The project already has one service per domain area (`ClubDiscoveryService`, `ClubModerationService`, `ClubRegistrationService`).
   - Effort: Medium

### Recommendation

Use **Approach 2: thin consumers + a dedicated `LikeMatchService`**.

Reasoning:
- It follows the existing architectural pattern (services for business logic, queue receivers as dispatchers).
- It is the most testable approach, which matters because strict TDD is active and the existing test suite already uses Mockito for services.
- It keeps JMS concerns (listener, queue names) separated from persistence/notification logic, making the code easier to reason about and maintain.
- The 400-line review budget is better protected with focused, well-tested units.

High-level flow for the recommended approach:
1. `ReceiverForLike` receives `LikeMessage(fromChatid, toChatid)` and calls `likeMatchService.processLike(fromChatid, toChatid)`.
2. `LikeMatchService` attempts to save the like. If the unique constraint is violated (duplicate like), it silently ignores the request or returns early.
3. After saving, it checks whether a reverse like (`from_chatid = toChatid`, `to_chatid = fromChatid`) already exists.
4. If a reverse like exists, it marks both likes as `matched = true` and sends a `MatchMessage` to `queue.match`.
5. If no reverse like exists, it sends a single notification to the target user: *"Alguien te dio like. Abrí /ver_personas para ver quién."* (kept vague intentionally; no premium "who liked me" feature yet).
6. `ReceiverForMatch` receives `MatchMessage(chatidA, chatidB)` and calls `likeMatchService.notifyMatch(chatidA, chatidB)`.
7. The service looks up both profiles. If both exist and contain `contact_username`, it sends each user a message such as: *"¡Match! Podés escribirle a @username"* with a contact button linking to `https://t.me/<username>`.
8. If a profile or username is missing, the service logs a warning and skips the notification rather than crashing.

### Risks

- **Duplicate likes / race conditions**: The `likes` table has a unique constraint on `(from_chatid, to_chatid)`, which prevents duplicates. The service must handle the resulting `DataIntegrityViolationException` or `DuplicateKeyException` gracefully and not enqueue a match notification for stale duplicate likes.
- **SQLite single writer**: The like save + reverse lookup + matched flag update should be performed in a single, short `@Transactional` block to avoid inconsistent reads between the two operations.
- **Blocked users**: The current design does not block likes from blocked users at the discovery layer because `showNextProfile` already filters blocked users. The service should be defensive: if a user involved in a match is blocked, the notification should not be sent.
- **Missing username**: `contact_username` is mandatory during registration, but the notification code should still handle a null value gracefully (log and skip) to avoid runtime failures.
- **Broken photo handling**: Match notifications should be text-only (no photo), so the `ReceiverForSend` broken-photo path does not apply. However, the service must not accidentally touch `photo_file_id` during match processing.
- **Mutual-match order**: Because the first like creates the row and the second like triggers the match, the order in which the two likes arrive does not matter as long as the transaction is correct. The service must correctly identify the reverse like regardless of who liked first.

### Ready for Proposal

Yes. The existing code, queues, entities, and tests are in place. The scope is clear and bounded to the two queue consumers and a new service. The next phase should be `sdd-propose` to define the exact acceptance criteria, test scenarios, and notification copy before moving to `sdd-spec`.
