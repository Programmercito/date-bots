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

    /**
     * Telegram message ID of the profile currently shown in discovery or the
     * confirmation shown after like/skip/report.
     */
    @Column(name = "current_profile_message_id")
    private Integer currentProfileMessageId;

    /**
     * Telegram message ID of the previous discovery message, to be deleted before
     * showing the next profile.
     */
    @Column(name = "previous_profile_message_id")
    private Integer previousProfileMessageId;

    /**
     * Pipe-separated Telegram message IDs of the media group photos currently
     * shown in discovery. Used to delete all photos when advancing to the next profile.
     */
    @Column(name = "media_group_message_ids")
    private String mediaGroupMessageIds;

}
