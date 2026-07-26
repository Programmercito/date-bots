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
 * A like given from one user to another.
 */
@Entity(name = "LikeEntity")
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(columnNames = { "from_chatid", "to_chatid" }))
@Data
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_chatid", nullable = false)
    private String fromChatid;

    @Column(name = "to_chatid", nullable = false)
    private String toChatid;

    /**
     * true when the like is part of a mutual match.
     */
    @Column(nullable = false)
    private boolean matched = false;

    @Column(name = "created_at")
    private String createdAt;

}
