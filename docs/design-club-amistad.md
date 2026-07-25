# Club de Amistad — Design Document

## 1. Overview

This document describes the architecture and implementation plan for adding a **friendship club / matching feature** to the existing `dates-bot` Telegram bot.

The existing bot allows users to publish ephemeral messages to a channel. The new feature adds:

- User profile registration (name, age, gender, orientation, location, description, tastes, traits, what they are looking for chosen from predefined buttons, photo).
- Discovery of other profiles, one at a time.
- Like/skip interaction.
- Mutual-like match that reveals the Telegram username of both users.
- Admin moderation for profile approval.
- A data model prepared for future freemium/paid plans.

## 2. Constraints & Decisions

| Decision | Value | Rationale |
|----------|-------|-----------|
| Database | SQLite, no external DB server | Required by product owner. Simplifies deployment. |
| Messaging | JMS/ActiveMQ queues | Already in use; keeps interactions asynchronous and responsive. |
| Photo storage | Telegram `file_id` only, no local files | Privacy and disk-space optimization. The bot forwards the photo to viewers using Telegram's file ID. |
| Discovery | One profile per message | Cleaner UX in Telegram, easier moderation. |
| Contact on match | Telegram username only | Mandatory field. No WhatsApp/Instagram for now. |
| Target audience | Bolivia first, model open to other countries later | Country stored explicitly to allow expansion. |
| Filters | Hetero man, hetero woman, bi woman only | Product decision. UI enforces this; users outside these options cannot register for the club. |
| Age | 18+ only | Hard validation during registration. |
| Moderation | Manual admin approval | Admin receives new profiles and approves/rejects/blocks. |
| Payments | Not implemented now, but data model is ready | Everything is free while launch. Tables exist for future paid plans. No payment code is written yet. |

## 3. SQLite Concurrency Strategy

SQLite is file-based and allows only one writer at a time. To support concurrent likes/views without blocking the bot:

### 3.1 Enable WAL Mode

```properties
spring.datasource.url=jdbc:sqlite:users.db?_journal_mode=WAL&_busy_timeout=5000
```

- `WAL` (Write-Ahead Logging) allows readers to continue while a writer is active.
- `_busy_timeout=5000` makes writers wait up to 5 seconds instead of failing immediately.

### 3.2 Connection Pool

Keep the pool small. Recommended for SQLite:

```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

### 3.3 Database Constraints

- `likes(from_chatid, to_chatid)` must be `UNIQUE` to prevent duplicate likes under race conditions.
- All write operations must be short transactions.

## 4. Data Model

### 4.1 `users` (extended)

| Column | Type | Notes |
|--------|------|-------|
| `chatid` | TEXT PK | Telegram chat ID |
| `username` | TEXT | Telegram username |
| `estado` | TEXT | `activo`, `bloqueado`, `pendiente` |
| `rol` | TEXT | `user`, `admin` |
| `comando` | TEXT | Current bot state |
| `created_at` | TEXT | ISO-8601 timestamp |

### 4.2 `profiles`

| Column | Type | Notes |
|--------|------|-------|
| `id` | INTEGER PK AUTOINCREMENT | Internal profile ID |
| `chatid` | TEXT UNIQUE FK | Links to `users` |
| `name` | TEXT | Display name |
| `age` | INTEGER | Must be >= 18 |
| `gender` | TEXT | `MALE`, `FEMALE`, `OTHER` |
| `orientation` | TEXT | `HETERO`, `BI` |
| `country` | TEXT | `BO` for now |
| `city` | TEXT | Free text, Bolivia city |
| `description` | TEXT | Self description |
| `tastes` | TEXT | Comma-separated or free text |
| `traits` | TEXT | Comma-separated or free text |
| `looking_for` | TEXT | What the user wants: `FRIENDSHIP`, `RELATIONSHIP`, `ONLINE_RELATIONSHIP`, `SUGAR_DADDY`, `LOVERS` |
| `photo_file_id` | TEXT | Telegram file_id used to forward the photo |
| `contact_username` | TEXT | Telegram username for matches |
| `status` | TEXT | `PENDING`, `APPROVED`, `REJECTED`, `PAUSED` |
| `created_at` | TEXT | ISO-8601 timestamp |
| `updated_at` | TEXT | ISO-8601 timestamp |

### 4.3 `likes`

| Column | Type | Notes |
|--------|------|-------|
| `id` | INTEGER PK AUTOINCREMENT | |
| `from_chatid` | TEXT | Who gave the like |
| `to_chatid` | TEXT | Who received the like |
| `matched` | INTEGER DEFAULT 0 | 0 = pending, 1 = mutual match |
| `created_at` | TEXT | ISO-8601 timestamp |

**Constraint:** `UNIQUE(from_chatid, to_chatid)`.

### 4.4 `daily_limits`

| Column | Type | Notes |
|--------|------|-------|
| `chatid` | TEXT PK part | |
| `date` | TEXT PK part | `YYYY-MM-DD` |
| `likes_used` | INTEGER DEFAULT 0 | |
| `views_used` | INTEGER DEFAULT 0 | |

Used for future freemium enforcement. For now, values are tracked but not limiting.

### 4.5 `reports`

| Column | Type | Notes |
|--------|------|-------|
| `id` | INTEGER PK AUTOINCREMENT | |
| `reporter_chatid` | TEXT | Who reported |
| `reported_chatid` | TEXT | Who was reported |
| `reason` | TEXT | Free text or predefined |
| `status` | TEXT | `OPEN`, `RESOLVED`, `IGNORED` |
| `created_at` | TEXT | ISO-8601 timestamp |

### 4.6 `user_plans` (future payments — table only, no logic yet)

This table is created now so future payment features do not require schema changes. All users start as `FREE` and no limits are enforced during launch.

| Column | Type | Notes |
|--------|------|-------|
| `chatid` | TEXT PK | |
| `plan` | TEXT | `FREE`, `PREMIUM`, `PLUS` |
| `started_at` | TEXT | |
| `expires_at` | TEXT | Nullable for lifetime/free |

## 5. JMS Queues

| Queue | Purpose |
|-------|---------|
| `queue.process` | Existing. Receives Telegram updates for business logic. |
| `queue.send` | Existing. Sends messages/photos to Telegram. |
| `queue.like` | New. Processes a like asynchronously and detects mutual matches. |
| `queue.match` | New. Sends match notifications to both users. |
| `queue.moderation` | New. Sends new/reported profiles to the admin. |
| `queue.analytics` | New. Updates daily usage counters without blocking the UI. |

## 6. Bot States

The bot uses a state machine stored in `users.comando`.

| State | Description |
|-------|-------------|
| `start` | Main menu |
| `club_register_name` | Asking for name |
| `club_register_age` | Asking for age |
| `club_register_gender` | Asking for gender (buttons) |
| `club_register_orientation` | Asking for orientation (buttons) |
| `club_register_city` | Asking for city |
| `club_register_description` | Asking for description |
| `club_register_tastes` | Asking for tastes |
| `club_register_traits` | Asking for traits |
| `club_register_looking_for` | Asking for what they want (predefined buttons) |
| `club_register_photo` | Waiting for profile photo |
| `club_register_preview` | Showing preview before submission |
| `club_browsing` | Browsing discovery profiles |
| `publicar` | Existing: writing a channel post |

## 7. Commands & Callbacks

### 7.1 Commands

| Command | Who | Description |
|---------|-----|-------------|
| `/start` | All | Main menu |
| `/club` | All | Enter the friendship club |
| `/publicar` | All | Existing: publish to channel |
| `/ver_canal` | All | Existing: show channel link |
| `/ver_personas` | Approved profiles | Start discovery |
| `/editar_perfil` | Approved/PENDING profiles | Edit profile |
| `/pausar_perfil` | Approved profiles | Hide from discovery |
| `/activar_perfil` | Paused profiles | Show in discovery |
| `/mis_matches` | All | List current matches |
| `/aprobar_perfil_<chatid>` | Admin | Approve a profile |
| `/rechazar_perfil_<chatid>` | Admin | Reject a profile |
| `/bloquear_<chatid>` | Admin | Block a user |

### 7.2 Inline Callbacks

| Callback | Action |
|----------|--------|
| `club_register` | Start registration |
| `club_like_<chatid>` | Like a profile |
| `club_skip_<chatid>` | Skip a profile |
| `club_report_<chatid>` | Report a profile |
| `club_next` | Show next profile |
| `club_preview_ok` | Submit profile for approval |
| `club_preview_edit` | Edit profile before submission |

## 8. User Flows

### 8.1 Registration

1. User taps **"Entrar al club de amistad"** (`/club`).
2. Bot validates the user does not already have a profile.
3. Bot asks for `name` → state `club_register_name`.
4. Bot asks for `age` → validate >= 18.
5. Bot asks for `gender` via buttons.
6. Bot asks for `orientation` via buttons. Only `HETERO` and `BI` are valid.
   - If gender is `MALE` and orientation is `HETERO` → valid (hombre hetero).
   - If gender is `FEMALE` and orientation is `HETERO` → valid (mujer hetero).
   - If gender is `FEMALE` and orientation is `BI` → valid (mujer bi).
   - Any other combination → reject and explain the club only accepts those 3 categories.
7. Bot asks for `country` (default Bolivia, but stored for future expansion).
8. Bot asks for `city`.
9. Bot asks for `description`.
10. Bot asks for `tastes`.
11. Bot asks for `traits`.
12. Bot asks for `looking_for` using predefined buttons. Stored values are `FRIENDSHIP`, `RELATIONSHIP`, `ONLINE_RELATIONSHIP`, `SUGAR_DADDY`, `LOVERS` (button labels are Spanish).
13. Bot asks for `photo`.
14. Bot shows preview.
15. User confirms → profile saved with status `PENDING` and sent to `queue.moderation`.
16. Admin receives the profile with approve/reject/block buttons.

### 8.2 Discovery

1. User taps **"Ver personas"** (`/ver_personas`).
2. System picks the next unseen approved profile matching the user's filters.
3. Bot sends the profile as a photo with caption and inline buttons: **Like**, **Skip**, **Report**.
4. If the profile photo `file_id` is broken/expired and the send fails, the target profile is marked `REJECTED`, the owner is notified to re-register with `/club`, and the viewer is asked to continue with `/ver_personas`.
5. `views_used` is incremented via `queue.analytics`.
5. On **Skip**: show next profile.
6. On **Like**: send to `queue.like`, respond immediately "Guardado".
7. On **Report**: save report and notify admin via `queue.moderation`.

### 8.3 Like & Match

1. `queue.like` consumer receives `(from_chatid, to_chatid)`.
2. Within a single transaction:
   - Insert like (ignore if duplicate).
   - Check if reverse like exists.
3. If reverse like exists:
   - Mark both likes as `matched = 1`.
   - Send both chat IDs to `queue.match`.
4. If no reverse like:
   - Notify `to_chatid`: "Someone liked you. Open /ver_personas to see who."

### 8.4 Match Notification

1. `queue.match` consumer receives `(chatid_a, chatid_b)`.
2. Fetch both profiles.
3. Send to both users:
   - "¡Match! Puedes escribirle a @username"
4. Include contact button linking to `https://t.me/<username>`.

### 8.5 Admin Moderation

1. New profile created → `queue.moderation`.
2. Admin bot receives photo + caption + buttons:
   - `✅ Aprobar` → `/aprobar_perfil_<chatid>`
   - `❌ Rechazar` → `/rechazar_perfil_<chatid>`
   - `⛔ Bloquear` → `/bloquear_<chatid>`
3. On approval:
   - Set `profiles.status = APPROVED`.
   - Notify user: "Tu perfil fue aprobado. Usá /ver_personas para empezar."
4. On rejection:
   - Set `profiles.status = REJECTED`.
   - Notify user with reason.
5. On block:
   - Set `users.estado = bloqueado`.

## 9. Privacy & Security

- **No photos stored on disk**: only Telegram `file_id` is kept.
- **Contact shared only on mutual match**.
- **Phone number never requested**.
- **Age validated**; under 18 cannot register.
- **Block and report** available on every profile.
- **Admin approval** required before any profile is visible.
- **Profile pausing**: users can hide themselves without deleting data.

## 10. Future Payments (Planned, Not Implemented Now)

**Important:** no payment logic is implemented in the current phases. The tables `user_plans` and `payments` exist only to avoid future migrations.

### 10.1 Freemium Limits (example)

| Feature | Free | Premium |
|---------|------|---------|
| Likes/day | 10 | unlimited |
| Views/day | 20 | unlimited |
| See who liked me | no | yes |
| Undo skip | no | yes |
| Profile boost | no | 1/week |

### 10.2 Payment Tables

- `user_plans`
- `payments`

### 10.3 Payment Providers to Evaluate

- MercadoPago (Latin America)
- Stripe (international cards)
- Telegram Stars (if available and suitable)

## 11. Implementation Phases

### Phase 1 — Foundation
- Enable SQLite WAL mode.
- Create `Profile`, `Like`, `Report`, `DailyLimit`, `UserPlan` entities.
- Extend `User` entity with `rol` and `created_at`.
- Add new JMS queues: `queue.like`, `queue.match`, `queue.moderation`, `queue.analytics`.
- Create queue consumers skeletons.

### Phase 2 — Registration
- Implement `/club` command and registration flow.
- Validate age >= 18.
- Validate allowed gender/orientation combinations.
- Save profile as `PENDING`.
- Send new profile to admin via `queue.moderation`.

### Phase 3 — Admin Moderation
- Admin receives profile with approve/reject/block buttons.
- Approve enables discovery.
- Reject/block notify user.

### Phase 4 — Discovery
- `/ver_personas` command.
- Query approved profiles matching filters, excluding self and already seen/liked.
- Send one profile per message with photo + buttons.

### Phase 5 — Likes & Matches
- Like callback sends to `queue.like`.
- Consumer detects mutual match and sends to `queue.match`.
- Match consumer notifies both users with Telegram usernames.

### Phase 6 — Daily Limits & Reports
- Track likes/views in `daily_limits` via `queue.analytics`.
- Add report button and `reports` table.
- Add `/mis_matches` command.

### Phase 7 — Edit / Pause / Payment-Ready Hooks
- Edit profile flow.
- Pause/activate profile.
- Create `user_plans` table and helper methods.
- **No payment collection code is added.** Only the data structure is ready.

## 12. Open Questions / Risks

- **SQLite at scale**: if the club grows beyond a few hundred concurrent users, migration to PostgreSQL should be reconsidered.
- **Moderation bottleneck**: manual approval does not scale. Future phases may need auto-approval with report-based review.
- **Telegram file_id expiration**: Telegram file IDs are long-lived but not guaranteed forever. If a photo stops loading during discovery, the profile is automatically deactivated (`REJECTED`), the owner is notified, and the user can re-register with `/club`.
- **Limited orientation/gender options**: enforced in code; may require customer support for edge cases.

---

*Document version: 1.0*  
*Database target: SQLite (WAL mode enabled)*  
*Backend: Spring Boot 3.4 + Java 21 + JMS/ActiveMQ*
