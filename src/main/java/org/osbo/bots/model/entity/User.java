package org.osbo.bots.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Telegram user of the bot.
 */
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    private String chatid;
    private String user;
    private String fecha_registro;
    private String estado;
    private String comando;

    /**
     * User role: "user" or "admin".
     */
    private String rol;

    /**
     * ISO-8601 timestamp when the user was created.
     */
    @Column(name = "created_at")
    private String createdAt;

}
