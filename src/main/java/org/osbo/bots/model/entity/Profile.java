package org.osbo.bots.model.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Pipe-separated list of all photo file IDs (up to 10).
     * The first entry matches {@link #photoFileId}.
     */
    @Column(name = "photo_file_ids")
    private String photoFileIds;

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

    /**
     * Returns all photo file IDs as a list. Falls back to {@link #photoFileId}
     * if {@link #photoFileIds} is not set (backward compat).
     */
    public List<String> getPhotoList() {
        if (photoFileIds != null && !photoFileIds.isBlank()) {
            return new ArrayList<>(Arrays.asList(photoFileIds.split("\\|")));
        }
        if (photoFileId != null && !photoFileId.isBlank()) {
            return List.of(photoFileId);
        }
        return new ArrayList<>();
    }

    /**
     * Adds a photo file ID. Keeps {@link #photoFileId} in sync as the primary photo.
     * Maximum 10 photos.
     *
     * @return true if added, false if limit reached or already present
     */
    public boolean addPhoto(String fileId) {
        List<String> list = getPhotoList();
        if (list.size() >= 10 || list.contains(fileId)) {
            return false;
        }
        list.add(fileId);
        photoFileIds = String.join("|", list);
        photoFileId = list.get(0);
        return true;
    }

    /**
     * Replaces all photos with a fresh list starting with the given file ID.
     */
    public void resetPhotos(String firstFileId) {
        photoFileId = firstFileId;
        photoFileIds = firstFileId;
    }

    /**
     * Returns how many photos this profile has.
     */
    public int photoCount() {
        return getPhotoList().size();
    }

}
