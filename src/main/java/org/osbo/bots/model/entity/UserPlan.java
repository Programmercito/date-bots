package org.osbo.bots.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Current plan for a user. No enforcement logic yet.
 */
@Entity
@Table(name = "user_plans")
@Data
public class UserPlan {

    @Id
    private String chatid;

    @Column(name = "plan", nullable = false)
    private String plan = "FREE";

    @Column(name = "started_at")
    private String startedAt;

    @Column(name = "expires_at")
    private String expiresAt;

}
