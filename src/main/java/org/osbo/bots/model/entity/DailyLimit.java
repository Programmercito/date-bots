package org.osbo.bots.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Daily usage counters for a user.
 */
@Entity
@Table(name = "daily_limits")
@IdClass(DailyLimitId.class)
@Data
public class DailyLimit {

    @Id
    private String chatid;

    /**
     * Date in YYYY-MM-DD format.
     */
    @Id
    private String date;

    @Column(name = "likes_used", nullable = false)
    private int likesUsed = 0;

    @Column(name = "views_used", nullable = false)
    private int viewsUsed = 0;

}
