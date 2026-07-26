package org.osbo.bots.model.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import org.osbo.bots.util.AgeCalculator;

/**
 * Friendship club profile linked to a {@link User}.
 */
@Entity
@Table(name = "profiles")
@Data
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chatid", unique = true, nullable = false)
    private String chatid;

    private String name;

    private Integer age;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    private String gender;

    private String orientation;

    private String country;

    private String city;

    @Column(name = "description")
    private String description;

    private String tastes;

    private String traits;

    @Column(name = "looking_for")
    private String lookingFor;

    @Column(name = "photo_file_id")
    private String photoFileId;

    @Column(name = "contact_username")
    private String contactUsername;

    /**
     * Optional WhatsApp number used as an alternative contact method.
     */
    private String whatsapp;

    /**
     * Profile status: PENDING, APPROVED, REJECTED, PAUSED.
     */
    private String status;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    /**
     * Returns the age calculated from the birthdate when available, otherwise falls
     * back to the legacy age column.
     *
     * @return the calculated age, or null if neither value is available
     */
    public Integer getAge() {
        return AgeCalculator.calculateAge(birthDate, age);
    }

}
