package org.osbo.bots.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldReturnEmptyTempPhotoListWhenNull() {
        User user = new User();

        List<String> result = user.getTempPhotoList();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldParseTempPhotoListFromPipeSeparatedString() {
        User user = new User();
        user.setTempPhotoFileIds("A|B|C");

        List<String> result = user.getTempPhotoList();

        assertThat(result).containsExactly("A", "B", "C");
    }

    @Test
    void shouldSeedTempPhotosFromProfilePhotoFileIds() {
        User user = new User();
        Profile profile = new Profile();
        profile.setPhotoFileIds("A|B");

        user.setTempPhotosFromProfile(profile);

        assertThat(user.getTempPhotoList()).containsExactly("A", "B");
        assertThat(profile.getPhotoFileIds()).isEqualTo("A|B");
    }

    @Test
    void shouldSeedTempPhotosFromLegacySinglePhotoFileId() {
        User user = new User();
        Profile profile = new Profile();
        profile.setPhotoFileId("A");

        user.setTempPhotosFromProfile(profile);

        assertThat(user.getTempPhotoList()).containsExactly("A");
        assertThat(profile.getPhotoFileId()).isEqualTo("A");
    }

    @Test
    void shouldSeedEmptyTempPhotosWhenProfileHasNoPhotos() {
        User user = new User();
        Profile profile = new Profile();

        user.setTempPhotosFromProfile(profile);

        assertThat(user.getTempPhotoList()).isEmpty();
    }

    @Test
    void shouldAppendTempPhoto() {
        User user = new User();
        user.setTempPhotoFileIds("A");

        boolean added = user.addTempPhoto("B");

        assertThat(added).isTrue();
        assertThat(user.getTempPhotoFileIds()).isEqualTo("A|B");
    }

    @Test
    void shouldIgnoreNullOrBlankTempPhoto() {
        User user = new User();
        user.setTempPhotoFileIds("A");

        assertThat(user.addTempPhoto(null)).isFalse();
        assertThat(user.addTempPhoto("  ")).isFalse();
        assertThat(user.getTempPhotoFileIds()).isEqualTo("A");
    }

    @Test
    void shouldIgnoreDuplicateTempPhoto() {
        User user = new User();
        user.setTempPhotoFileIds("A|B");

        boolean added = user.addTempPhoto("A");

        assertThat(added).isFalse();
        assertThat(user.getTempPhotoFileIds()).isEqualTo("A|B");
    }

    @Test
    void shouldEnforceMaximumTempPhotos() {
        User user = new User();
        user.setTempPhotoFileIds("A|B|C|D|E|F|G|H|I|J");

        boolean added = user.addTempPhoto("K");

        assertThat(added).isFalse();
        assertThat(user.getTempPhotoFileIds()).isEqualTo("A|B|C|D|E|F|G|H|I|J");
    }

    @Test
    void shouldClearTempPhotos() {
        User user = new User();
        user.setTempPhotoFileIds("A|B");

        user.clearTempPhotos();

        assertThat(user.getTempPhotoFileIds()).isNull();
        assertThat(user.getTempPhotoList()).isEmpty();
    }

    @Test
    void shouldReturnTempPhotoCount() {
        User user = new User();
        user.setTempPhotoFileIds("A|B|C");

        assertThat(user.getTempPhotoCount()).isEqualTo(3);
    }

    @Test
    void shouldReturnZeroTempPhotoCountWhenNull() {
        User user = new User();

        assertThat(user.getTempPhotoCount()).isZero();
    }
}
