# Proposal: phase-5-likes-and-matches

## Intent

Enable the friendship-club like/match loop: process likes from discovery, detect mutual matches, notify both users with rich profile and contact information, and let users list their matches. Also expand registration so users provide at least one contact method (Telegram username or WhatsApp number).

## Scope

### In Scope
- `ReceiverForLike` + service: persist like, detect reverse like, mark mutual, enqueue `MatchMessage`.
- Unavailable-target notification: if the liked profile is `REJECTED`, `PAUSED`, or the user is blocked, notify the liker.
- Anonymous non-mutual like notification to the target user.
- `ReceiverForMatch` + service: send rich match notification with photo, profile details, contact buttons, and a report button.
- `/mis_matches` command listing current matches with contact info.
- Registration extension to collect at least one contact method (Telegram or WhatsApp).
- Tests for the new service and both receivers.

### Out of Scope
- Daily limits, payments, or premium features.
- `/editar_perfil`, `/pausar_perfil`, `/activar_perfil` flows.
- "Who liked me" premium feature.

## Capabilities

### New Capabilities
- `club-like-processing`: persist likes, detect mutual matches, handle unavailable targets, and send anonymous non-mutual notifications.
- `club-match-notification`: build and send rich mutual-match messages including profile photo, details, contact buttons, and a report button.
- `club-mis-matches`: `/mis_matches` command that lists a user's current matches and their contact info.
- `club-contact-method`: collect and validate at least one contact method (Telegram username or WhatsApp number) during registration.

### Modified Capabilities
- `club-registration`: add a contact-method step before the preview; validate that at least one of Telegram username or WhatsApp number is present.
- `club-discovery`: keep minimal; only update the immediate like confirmation already implemented.

## Approach

Use the existing thin-consumer + service pattern (`LikeMatchService` or similar). `ReceiverForLike` delegates to the service, which persists the like in a short transaction, checks for the reverse like, and either marks both as matched and enqueues a `MatchMessage` or sends an anonymous notification. `ReceiverForMatch` delegates to the same service to build the rich notification. The service looks up both profiles, handles broken `file_id` for the photo by sending text only (no deactivation), and includes Telegram/WhatsApp contact buttons plus a report button that resubmits the profile to `queue.moderation`. `/mis_matches` queries the `likes` table for mutual matches, fetches the opposite profile, and sends a list with contact info.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `jms/queue/receiver/ReceiverForLike.java` | Modified | Delegate to service; remove stub logging. |
| `jms/queue/receiver/ReceiverForMatch.java` | Modified | Delegate to service; remove stub logging. |
| `model/services/` | New | `LikeMatchService` for like processing and match notifications. |
| `model/repositories/LikeRepository.java` | Modified | Add finder(s) for mutual matches if missing. |
| `processor/MessageProcessor.java` | Modified | Add `/mis_matches` handler. |
| `model/services/ClubRegistrationService.java` | Modified | Add contact-method step and validation. |
| `model/entity/Profile.java` | Modified | Add `whatsapp` field. |
| `jms/queue/pojos/MatchMessage.java` | Modified | Add fields needed for rich match if required. |
| `docs/design-club-amistad.md` | Modified | Update contact method and match notification sections. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Duplicate like due to race condition | Low | Unique constraint on `likes(from_chatid, to_chatid)`; service catches violation gracefully. |
| Broken photo `file_id` in match notification | Low | Send text-only fallback; do not deactivate the profile. |
| SQLite single-writer contention | Low | Keep like save + reverse lookup in one short transaction. |
| Profile becomes unavailable between like callback and queue processing | Low | Check target status and notify the liker; do not crash. |
| Contact method missing after registration change | Low | Validate at least one of Telegram username or WhatsApp before preview. |

## Rollback Plan

1. Revert the commit that introduced the service and receiver changes.
2. Restore the stub `ReceiverForLike` and `ReceiverForMatch` implementations.
3. Remove the `whatsapp` column and registration step if needed, or leave it nullable if the schema was already deployed.
4. Redeploy the previous artifact. The `likes` and `profiles` tables remain compatible because only nullable additions are made.

## Dependencies

- Phase 4 discovery must be in place (already implemented).
- `Like` entity and `likes` unique constraint must exist (already implemented).
- `queue.like` and `queue.match` must be configured (already implemented).

## Success Criteria

- [ ] Liking a profile persists the like and triggers an anonymous notification to the target.
- [ ] A mutual like marks both rows as matched and enqueues a `MatchMessage`.
- [ ] Both users receive a rich match message with the other user's profile photo, details, and contact buttons.
- [ ] A broken match-notification photo falls back to text without deactivating the profile.
- [ ] If the liked profile is unavailable when the like is processed, the liker is notified.
- [ ] `/mis_matches` lists current matches with contact info.
- [ ] Registration rejects a profile that has neither Telegram username nor WhatsApp number.
- [ ] All tests pass (`mvnw test`).
