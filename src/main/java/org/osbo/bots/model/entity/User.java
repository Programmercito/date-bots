package org.osbo.bots.model.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static final int MAX_PHOTOS = 10;

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
     * Telegram message ID of the editable photo-edit prompt. Used to update the
     * counter and buttons in place while the user adds or removes photos.
     */
    @Column(name = "photo_edit_prompt_message_id")
    private Integer photoEditPromptMessageId;

    /**
     * Pipe-separated Telegram message IDs of the media group photos currently
     * shown in discovery. Used to delete all photos when advancing to the next profile.
     */
    @Column(name = "media_group_message_ids")
    private String mediaGroupMessageIds;

    /**
     * Pipe-separated list of photo file IDs staged during profile photo editing.
     * Committed to {@link Profile#photoFileIds} only when the user explicitly saves.
     */
    @Column(name = "temp_photo_file_ids")
    private String tempPhotoFileIds;

    /**
     * Returns the staged photo file IDs as a list.
     */
    public List<String> getTempPhotoList() {
        if (tempPhotoFileIds == null || tempPhotoFileIds.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(tempPhotoFileIds.split("\\|")));
    }

    /**
     * Adds a photo file ID to the staged list. Ignores null/blank values,
     * deduplicates, and enforces {@link #MAX_PHOTOS}.
     *
     * @return true if added, false if ignored due to duplicate or limit
     */
    public boolean addTempPhoto(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return false;
        }
        List<String> list = getTempPhotoList();
        if (list.size() >= MAX_PHOTOS || list.contains(fileId)) {
            return false;
        }
        list.add(fileId);
        tempPhotoFileIds = String.join("|", list);
        return true;
    }

    /**
     * Seeds the staged photo list from the profile's current photos without
     * modifying the profile.
     */
    public void setTempPhotosFromProfile(Profile profile) {
        if (profile.getPhotoFileIds() != null && !profile.getPhotoFileIds().isBlank()) {
            tempPhotoFileIds = profile.getPhotoFileIds();
            return;
        }
        if (profile.getPhotoFileId() != null && !profile.getPhotoFileId().isBlank()) {
            tempPhotoFileIds = profile.getPhotoFileId();
            return;
        }
        tempPhotoFileIds = null;
    }

    /**
     * Clears the staged photo list.
     */
    public void clearTempPhotos() {
        tempPhotoFileIds = null;
    }

    /**
     * Returns the number of staged photos.
     */
    public int getTempPhotoCount() {
        return getTempPhotoList().size();
    }

}
