# Design: phase-5-likes-and-matches

## Technical Approach

Use the existing thin-consumer + dedicated-service pattern. `ReceiverForLike` and `ReceiverForMatch` become dispatchers; a new `LikeMatchService` holds all persistence and message-building logic. `ClubRegistrationService` gains a contact-method step after the photo. `ReceiverForProcess` delegates `/mis_matches` to `LikeMatchService`.

All data access happens inside short `@Transactional` service methods so SQLite sees one writer per like and the reverse-like check is atomic. Outbound messages go to `queue.send` via `NqueueForSend` only.

## Architecture Decisions

| Decision | Option | Tradeoff | Choice |
|----------|--------|----------|--------|
| Service consolidation | One `LikeMatchService` for likes, matches, and `/mis_matches` | Reuses like data for matches | ✅ Single service |
| Self-like handling | Ignore silently | Prevents bad data | ✅ Guard at consumer entry |
| Mutual match detection | Save new like then check reverse in same transaction | Required for SQLite atomicity | ✅ One `@Transactional` method |
| Duplicate like | Catch `DataIntegrityViolationException` | Unique constraint already exists | ✅ Ignore without error |
| Match photo failure | Text-only fallback | Discovery already deactivates broken `file_id` profiles | ✅ Text fallback, no status change |
| Contact method | Telegram username and/or WhatsApp | WhatsApp is more reliable in Bolivia | ✅ Require at least one; store in `profile.whatsapp` |
| Anonymous like notification | Send to target when not mutual | Privacy-first | ✅ Separate `queue.send` message |
| Report from match | Resubmit to `queue.moderation` | Reuses moderation flow | ✅ `REPORT` message with both chat IDs |
| `/mis_matches` | Handled in `ReceiverForProcess` via service | Keeps dispatcher as router | ✅ Service query + list rendering |

## Data Flow

### Like Processing

```
Discovery (ClubDiscoveryService)
    │
    ▼
queue.like ──► ReceiverForLike ──► LikeMatchService.processLike()
    │
    ├── target unavailable? ──► notify liker
    ├── self like? ──► ignore
    ├── duplicate? ──► ignore
    ├── save Like
    ├── reverse Like exists?
    │       ├── YES ──► mark both matched=true ──► queue.match
    │       └── NO  ──► notify target anonymously
    └── queue.send
```

### Match Notification

```
queue.match ──► ReceiverForMatch ──► LikeMatchService.notifyMatch()
    │
    ├── fetch both profiles
    ├── build caption + contact/report buttons
    ├── queue.send photo message for each user
    └── photo failure ──► text-only fallback
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/org/osbo/bots/model/services/LikeMatchService.java` | Create | Persist likes, detect mutual matches, send notifications, render `/mis_matches` |
| `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForLike.java` | Modify | Delegate to `LikeMatchService` |
| `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForMatch.java` | Modify | Delegate to `LikeMatchService` |
| `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForProcess.java` | Modify | Add `/mis_matches` branch |
| `src/main/java/org/osbo/bots/model/services/ClubRegistrationService.java` | Modify | Add `club_register_contact` state; validate contact method |
| `src/main/java/org/osbo/bots/model/entity/Profile.java` | Modify | Add `whatsapp` column |
| `src/main/java/org/osbo/bots/model/repositories/LikeRepository.java` | Modify | Add mutual-match finder |
| `src/test/java/org/osbo/bots/model/services/LikeMatchServiceTest.java` | Create | Unit tests for service logic |
| `src/test/java/org/osbo/bots/jms/queue/receiver/ReceiverForLikeTest.java` | Create | Consumer delegates to service |
| `src/test/java/org/osbo/bots/jms/queue/receiver/ReceiverForMatchTest.java` | Create | Consumer delegates to service |
| `src/test/java/org/osbo/bots/model/services/ClubRegistrationServiceTest.java` | Modify | Contact-step tests |
| `docs/design-club-amistad.md` | Modify | Update match/contact sections |

## Interfaces / Contracts

### New POJO field

`Profile.whatsapp` is a nullable `String` column.

### New/finder repository methods

```java
public interface LikeRepository extends JpaRepository<Like, Long> {
    Like findByFromChatidAndToChatid(String fromChatid, String toChatid);
    List<Like> findByFromChatidOrToChatidAndMatchedTrue(String chatidA, String chatidB, boolean matched);
}
```

### `LikeMatchService` public interface

```java
public void processLike(LikeMessage message);
public void notifyMatch(MatchMessage message);
public void listMatches(String chatid);
```

### Callbacks

- `club_match_report_<chatid>` → sends a `ModerationMessage` of type `REPORT` to `queue.moderation`.
- `club_match_next` → unused in this phase; each match is a single message.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `LikeMatchService` | Mockito repositories + `NqueueForSend`; assert rows, `MatchMessage`, and fallback |
| Unit | `ClubRegistrationService` contact step | Assert state transitions and validation rejection |
| Consumer | `ReceiverForLike` and `ReceiverForMatch` | Verify service invocation only |
| Integration | Compile + existing test suite | `mvnw test` must pass |

## Migration / Rollout

1. Add nullable `whatsapp` column to `profiles`. JPA creates it automatically.
2. No data migration required.
3. Rollback: revert the commit. Only nullable additions are made.

## Open Questions

- None.
