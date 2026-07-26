# Design: Birthdate instead of age

## Technical Approach

Replace the explicit `age` registration step with a `DD/MM/AAAA` birthdate step. The ISO birthdate is stored in `profiles.birth_date` and computed into an age by `AgeCalculator` using `America/La_Paz`. `Profile.getAge()` becomes a computed accessor that uses `birthDate` when present and falls back to the legacy `age` column. All display sites (preview, discovery, match notifications, `/mis_matches`, admin moderation) call `getAge()` and never render the raw birthdate. `ModerationMessage` carries `birthDate` as an ISO string; the admin receiver computes the age for the caption.

## Architecture Decisions

### Decision: Date format and storage
**Choice**: User input `DD/MM/AAAA`; internal storage `LocalDate` in `profiles.birth_date`; DTO field `String` ISO.
**Alternatives considered**: Store the user string in the database; use `LocalDate` in the JMS DTO.
**Rationale**: A typed `LocalDate` prevents invalid persisted values and enables age math. A `String` in the JMS DTO avoids embedded ActiveMQ/Jackson serialization concerns for Java 8 date types.

### Decision: Age computation timezone
**Choice**: Use `ZoneId.of("America/La_Paz")` for the current date.
**Alternatives considered**: JVM default or UTC.
**Rationale**: The bot is for a Bolivian channel; a local-date birthday boundary avoids off-by-one errors around midnight.

### Decision: Computed age accessor
**Choice**: Add `Profile.getAge()` that delegates to `AgeCalculator` with `birthDate` and `age` fallback.
**Alternatives considered**: Compute age inline in every caption builder.
**Rationale**: Single source of truth; all display sites and DTO consumers get the same value.

### Decision: Transitional `age` column
**Choice**: Keep `age` nullable as a fallback.
**Alternatives considered**: Drop the `age` column immediately.
**Rationale**: Rollback safety and no data migration are required.

## Data Flow

### Registration

```
User text
  ↓
ReceiverForProcess
  ↓
ClubRegistrationService.handleBirthdate()
  → parse DD/MM/AAAA → LocalDate
  → validate ≥ 18 in America/La_Paz
  → save Profile.birthDate
  ↓
ClubRegistrationService.sendModerationMessage()
  → ModerationMessage.birthDate = ISO string
  ↓
queue.moderation
  ↓
ReceiverForModeration
  → AgeCalculator.fromIso(birthDate) → age
  → admin caption
```

### Discovery / match display

```
Profile (birthDate or age)
  ↓
Profile.getAge()
  ↓
ClubDiscoveryService / LikeMatchService caption builder
  ↓
NqueueForSend
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/org/osbo/bots/util/AgeCalculator.java` | Create | Calculates age in `America/La_Paz` from `LocalDate` with optional fallback and fixed-clock overload. |
| `src/main/java/org/osbo/bots/model/entity/Profile.java` | Modify | Add `birthDate` field; replace Lombok-generated `getAge()` with computed accessor. |
| `src/main/java/org/osbo/bots/model/services/ClubRegistrationService.java` | Modify | Replace `STATE_REGISTER_AGE` and `handleAge` with birthdate step; store `birthDate`; send it in `ModerationMessage`. |
| `src/main/java/org/osbo/bots/jms/queue/pojos/ModerationMessage.java` | Modify | Add `String birthDate`; keep `Integer age` for legacy/report compatibility. |
| `src/main/java/org/osbo/bots/jms/queue/receiver/ReceiverForModeration.java` | Modify | Compute age from `birthDate` for admin caption; fall back to `age`. |
| `src/main/java/org/osbo/bots/model/services/ClubDiscoveryService.java` | Modify | Use `Profile.getAge()` in caption; set `birthDate` in report messages. |
| `src/main/java/org/osbo/bots/model/services/LikeMatchService.java` | Modify | Use `Profile.getAge()` in match and `/mis_matches` captions; set `birthDate` in report messages. |
| `AGENTS.md` | Modify | Update the registration state list from `club_register_age` to `club_register_birthdate`. |
| `src/test/java/org/osbo/bots/util/AgeCalculatorTest.java` | Create | Unit tests with fixed clock for valid, invalid, and timezone cases. |
| `src/test/java/org/osbo/bots/model/services/ClubRegistrationServiceTest.java` | Modify | Update age step to `DD/MM/AAAA`; add under-18 and invalid-format tests. |
| `src/test/java/org/osbo/bots/model/services/ClubDiscoveryServiceTest.java` | Modify | Assert captions use calculated age from birthdate. |
| `src/test/java/org/osbo/bots/model/services/LikeMatchServiceTest.java` | Modify | Assert match/match-list captions use calculated age. |
| `src/test/java/org/osbo/bots/jms/queue/receiver/ReceiverForModerationTest.java` | Modify | Verify `birthDate` is rendered as calculated age, not raw. |
| `src/test/java/org/osbo/bots/model/repositories/RepositoryTests.java` | Modify | Assert `birthDate` round-trips through JPA. |

## Interfaces / Contracts

```java
public final class AgeCalculator {
    public static Integer calculateAge(LocalDate birthDate, Integer fallbackAge) { ... }
    public static Integer calculateAge(LocalDate birthDate, Integer fallbackAge, Clock clock) { ... }
    public static LocalDate parseUserDate(String input) { ... } // DD/MM/AAAA
}
```

```java
@Entity
public class Profile {
    @Column(name = "birth_date")
    private LocalDate birthDate;

    public Integer getAge() {
        return AgeCalculator.calculateAge(birthDate, age);
    }
}
```

```java
@Data
public class ModerationMessage {
    private String birthDate; // ISO, e.g. "2000-03-15"
    private Integer age;      // legacy fallback
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `AgeCalculator` | Fixed `Clock` for valid adult, birthday not reached, birthday today, New-Year boundary in La Paz, fallback to legacy age, null handling. |
| Unit | Registration birthdate parsing | `ClubRegistrationServiceTest` with `DD/MM/AAAA` input, under-18 rejection, invalid format retry, state advancement. |
| Integration | `Profile.getAge()` and caption builders | `ClubDiscoveryServiceTest`, `LikeMatchServiceTest` set `birthDate` and assert captions contain calculated age and no raw date. |
| Integration | Admin moderation | `ReceiverForModerationTest` receives `birthDate` and asserts `Edad: N` without raw date. |
| Integration | Schema round-trip | `RepositoryTests` saves and reads `birthDate`. |

## Migration / Rollout

No data migration is required because existing profiles are treated as green-field. Rollout steps:
1. Deploy code that writes `birth_date` and reads it via `Profile.getAge()`.
2. Run `ALTER TABLE profiles ADD COLUMN birth_date DATE;` (nullable). With `ddl-auto` update, Hibernate will add the column automatically; prefer explicit migration in production.
3. Keep `age` nullable. A future change can drop the column and the fallback once all profiles have `birth_date`.

## Open Questions

- None.
