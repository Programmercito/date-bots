package org.osbo.bots.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/**
 * A profile temporarily hidden after a user skips it during discovery.
 */
@Entity
@Table(name = "skipped_profiles", uniqueConstraints = @UniqueConstraint(columnNames = { "from_chatid", "to_chatid" }))
@Data
public class SkippedProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_chatid", nullable = false)
    private String fromChatid;

    @Column(name = "to_chatid", nullable = false)
    private String toChatid;

    @Column(name = "expires_at_ms", nullable = false)
    private Long expiresAtMs;

}
