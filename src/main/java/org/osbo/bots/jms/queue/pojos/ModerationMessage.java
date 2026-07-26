package org.osbo.bots.jms.queue.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message sent to queue.moderation for admin review.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationMessage {

    private String type;

    private String chatid;

    private String reason;

    private String name;

    private Integer age;

    private String birthDate;

    private String gender;

    private String orientation;

    private String country;

    private String city;

    private String description;

    private String tastes;

    private String traits;

    private String lookingFor;

    private String photoFileId;

    private String contactUsername;

    private String whatsapp;

}
