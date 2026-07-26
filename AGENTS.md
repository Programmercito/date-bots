# Agent Guide — date-bots

This document is a reference for any AI agent or developer working on the `dates-bot` Telegram bot. Read it before making changes.

## 1. Project Overview

`dates-bot` is a Telegram bot for a Bolivian friendship/dating channel. It has two main features:

1. **Channel publishing** — users send messages/photos to a moderation queue; approved posts are published to a Telegram channel and deleted after 1 hour.
2. **Friendship Club** — users create a profile, get approved by an admin, and discover other profiles with a like/skip/match mechanic.

Everything runs in a single JVM with no external database server.

## 2. Stack

- **Java 21**
- **Spring Boot 3.4.4** with Spring Data JPA and Spring JMS
- **SQLite** (file-based, WAL mode enabled)
- **ActiveMQ** embedded broker for JMS queues
- **java-telegram-bot-api** (`com.github.pengrad`) to talk to Telegram
- **Lombok** for boilerplate reduction
- **Maven** for builds

## 3. Project Structure

```
src/main/java/org/osbo/bots/
  DatesBotApplication.java       # Spring Boot entry point
  controllers/PingController.java
  crons/
    DeleteVencidos.java          # Deletes expired channel messages every minute
    Horarios.java                # Opens/closes channel at configured hours
  jms/
    queue/enqueue/               # Helpers to put messages on queues
      NqueueForProcess.java      # queue.process
      NqueueForSend.java         # queue.send
    queue/pojos/                 # DTOs for JMS messages
      MessageUpdate.java         # Incoming Telegram update
      MessageSend.java           # Outgoing Telegram message
      Button.java                # Inline keyboard button
      LikeMessage.java
      MatchMessage.java
      ModerationMessage.java
      AnalyticsMessage.java
    queue/receiver/              # JMS listeners
      ReceiverForProcess.java    # Main business logic
      ReceiverForSend.java       # Sends messages to Telegram
      ReceiverForLike.java
      ReceiverForMatch.java
      ReceiverForModeration.java
      ReceiverForAnalytics.java
  model/
    entity/                      # JPA entities
    repositories/                # Spring Data repositories
    services/                    # Business services
  processor/MessageProcessor.java # Receives Telegram webhook updates
  runner/StartupRunner.java      # Registers Telegram update listener
  util/                          # Helpers (dates, sleep, looking-for options, markdown escaping)
```

## 4. User Commands

**All commands must work both as typed commands (`/command`) and as inline buttons.** When adding a new command, always expose it as a button as well.

### Global commands

| Command | Button label | Who | Description |
|---------|--------------|-----|-------------|
| `/start` | "Volver al inicio" | All | Main menu |
| `/publicar` | "✏️ Publicar" | All | Publish to channel |
| `/ver_canal` | "📢 Ver canal" | All | Show channel link |
| `/club` | "🤝 Entrar al club de amistad" | All | Enter the friendship club |
| `/ver_personas` | "Ver personas" | Approved profiles | Start discovery |
| `/editar_perfil` | "Editar perfil" | Approved profiles | Edit approved profile fields |
| `/pausar_perfil` | "Pausar perfil" | Approved profiles | Hide from discovery (placeholder) |
| `/activar_perfil` | "Activar perfil" | Paused profiles | Show in discovery (placeholder) |
| `/mis_matches` | "Mis matches" | All | List current matches (placeholder) |

### Admin commands

| Command | Button label | Description |
|---------|--------------|-------------|
| `/aprobar_<id>` | "✅ Aprobar" | Approve a channel message |
| `/rechazar_<id>` | "❌ Rechazar" | Reject a channel message |
| `/bloquear_<chatid>` | "⛔ Bloquear" | Block a user |
| `/aprobar_perfil_<chatid>` | "✅ Aprobar" | Approve a friendship-club profile |
| `/rechazar_perfil_<chatid>` | "❌ Rechazar" | Reject a friendship-club profile |

## 5. Bot State Machine

The bot stores the current state in `users.comando`. Always set and persist this field after each interaction.

### Channel-publishing states

- `start` — main menu
- `publicar` — waiting for the user to type a channel post

### Club registration states

- `club_register_name`
- `club_register_birthdate`
- `club_register_gender`
- `club_register_orientation`
- `club_register_city`
- `club_register_description`
- `club_register_tastes`
- `club_register_traits`
- `club_register_looking_for`
- `club_register_photo`
- `club_register_preview`

### Club profile edit states

- `club_edit_menu`
- `club_edit_name`
- `club_edit_birthdate`
- `club_edit_gender`
- `club_edit_orientation`
- `club_edit_city`
- `club_edit_description`
- `club_edit_tastes`
- `club_edit_traits`
- `club_edit_looking_for`
- `club_edit_photo`
- `club_edit_contact`

## 6. JMS Queues

Never block the Telegram update thread with slow work. Put heavy or risky operations on queues.

| Queue | Purpose |
|-------|---------|
| `queue.process` | Decouples incoming Telegram updates from business logic |
| `queue.send` | Decouples outgoing Telegram API calls |
| `queue.like` | Processes likes and detects mutual matches |
| `queue.match` | Sends match notifications |
| `queue.moderation` | Sends new/reported profiles to the admin |
| `queue.analytics` | Updates daily usage counters |

## 7. Channel Publishing Flow

1. User taps **Publicar** (`/publicar`).
2. Bot asks for the message.
3. User sends text/photo.
4. If `telegram.aprob=true`, the message goes to the admin for approval.
5. Admin approves → message is posted to `telegram.channel`.
6. After 1 hour, `DeleteVencidos` deletes the channel message.

## 8. Friendship Club Flow

### 8.1 Registration

1. User taps **Entrar al club de amistad** (`/club`).
2. Bot asks for: name, birthdate (DD/MM/AAAA), gender, orientation, city, description, tastes, traits, looking for (predefined buttons), photo.
3. Validates:
   - Age >= 18 (calculated from birthdate).
   - Telegram username is set.
   - Allowed combinations only: **male hetero**, **female hetero**, **female bi**.
4. Profile is saved with status `PENDING`.
5. A `ModerationMessage` is sent to `queue.moderation`.

### 8.2 Admin Moderation

1. `ReceiverForModeration` sends the admin a photo + profile caption + buttons.
2. Admin taps **Aprobar**, **Rechazar**, or **Bloquear**.
3. `ClubModerationService` updates the profile/user and notifies both parties.

### 8.3 Discovery & Likes (UX Rule)

**Predefined "looking for" options (Spanish label → stored value):**

- "Amistad" → `FRIENDSHIP`
- "Relación" → `RELATIONSHIP`
- "Relación online" → `ONLINE_RELATIONSHIP`
- "Algo casual" → `CASUAL`
- "Sugar" → `SUGAR_DADDY`
- "Relación seria" → `SERIOUS_RELATIONSHIP`
- "Matrimonio" → `MARRIAGE`
- "Novios" → `LOVERS`
- "Relación informal" → `INFORMAL_RELATIONSHIP`

Labels are translated from the stored code by `LookingForOption.translate(code)` so users never see raw values.

**Broken/expired photo `file_id` handling:**

If forwarding a profile photo during discovery fails because the `file_id` is invalid, the target profile is marked `REJECTED` (deactivated), the owner is notified, and the viewer can continue with `/ver_personas`. The owner can re-register with `/club`.

**Important UX rule for the discovery flow:**

- Each profile is shown in **a single Telegram message** with inline buttons.
- When the user taps **Like** or **Skip**, the **same message is edited** to show the action taken (e.g., "Le diste like a María" or "Skippeaste a María").
- When the user swipes to the next profile (via `/ver_personas` or a "Siguiente" button), a **new message is sent** with the next profile.
- The previous message (the edited confirmation) is **deleted**.
- The bot must persist, per user, the IDs of these two messages so it can edit/delete them:
  - `current_profile_message_id` — the message currently showing a profile or confirmation.
  - `previous_profile_message_id` — the previous message, to be deleted before showing the next one.

This keeps the chat clean and gives clear feedback for every action.

### 8.4 Match

1. User A likes user B → like is sent to `queue.like`.
2. Consumer checks if B already liked A.
3. If yes → both likes are marked `matched`, and a `MatchMessage` goes to `queue.match`.
4. `queue.match` consumer notifies both users and shares Telegram usernames.

## 9. Data Model

### `users`
- `chatid` (PK)
- `username`
- `estado` — `activo`, `bloqueado`
- `rol` — `user`, `admin`
- `comando` — current bot state
- `fecha_registro`, `created_at`

### `profiles`
- `id` (PK)
- `chatid` (FK, unique)
- `name`, `age`, `gender`, `orientation`
- `country`, `city`
- `description`, `tastes`, `traits`, `looking_for`
- `photo_file_id`
- `contact_username`
- `status` — `INCOMPLETE`, `PENDING`, `APPROVED`, `REJECTED`, `PAUSED`
- `created_at`, `updated_at`

### `likes`
- `id` (PK)
- `from_chatid`, `to_chatid` (unique pair)
- `matched`
- `created_at`

### `daily_limits`
- `chatid`, `date` (composite PK)
- `likes_used`, `views_used`

### `reports`
- `id`, `reporter_chatid`, `reported_chatid`, `reason`, `status`, `created_at`

### `user_plans`
- `chatid` (PK)
- `plan` — `FREE`, `PREMIUM`, `PLUS`
- `started_at`, `expires_at`

## 10. Coding Conventions

- **Language**: code, identifiers, comments, and Javadoc in English.
- **UI copy**: Spanish, matching the existing bot style.
- Use Lombok `@Data` for entities and POJOs.
- Keep business logic in services; keep `ReceiverForProcess` as a dispatcher.
- Always persist `users.comando` after handling a step.
- Use `NqueueForSend` for outgoing messages, never call `TelegramBot.execute()` directly from business logic.
- Use JMS queues for anything that touches Telegram or might be slow.
- Captions that include user-generated text should be sent with `parseMode="Markdown"` and the user text escaped with `MarkdownEscaper.escape(...)` to avoid broken formatting.

## 11. Testing

Run tests with the Maven wrapper:

```bash
./mvnw test
```

On Windows:

```bash
.\mvnw test
```

All changes must keep the existing tests passing.

## 12. SQLite Notes

- WAL mode is enabled in `application.properties`.
- Keep the Hikari pool small (max 5).
- Keep transactions short.
- Use unique constraints to prevent race conditions (e.g., `likes(from_chatid, to_chatid)`).

## 13. Payments

Payments are **not implemented**. The `user_plans` table exists only as preparation. All users are currently `FREE` and have no limits.

## 14. TODO / Remaining Phases

Already implemented:
- Phase 1: Foundation (data model, SQLite WAL, JMS queues).
- Phase 2: Registration (`/club`) with age and gender/orientation validation.
- Phase 3: Admin moderation (approve/reject/block).
- Phase 4: Discovery (`/ver_personas`) with like/skip/report UX rule.
- Registration uses predefined "looking for" buttons and broken `file_id` deactivates the profile.

Still to implement:
- Phase 5: Likes processing and mutual match detection (`queue.like`, `queue.match`).
- Phase 6: Daily limits enforcement, reports handling, `/mis_matches`.
- Phase 7: Pause/activate profile flows (edit is implemented; pause remains).
- Future: payment integration.

## 15. Common Pitfalls

- Do not call `clubRegistrationService.handle()` inside the `if ("start".equals(comando))` block — registration states are `club_register_*` and must be handled before the main state switch.
- Do not store photos on disk; use Telegram `file_id` only.
- Do not expose contact info before a mutual match.
- Remember that SQLite allows only one writer at a time; put writes in short transactions.
