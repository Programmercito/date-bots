package org.osbo.bots.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * User report for moderation.
 */
@Entity
@Table(name = "reports")
@Data
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_chatid", nullable = false)
    private String reporterChatid;

    @Column(name = "reported_chatid", nullable = false)
    private String reportedChatid;

    private String reason;

    /**
     * Report status: OPEN, RESOLVED, IGNORED.
     */
    private String status;

    @Column(name = "created_at")
    private String createdAt;

}
