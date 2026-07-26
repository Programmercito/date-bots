package org.osbo.bots.util;

import java.util.Arrays;
import java.util.List;

import org.osbo.bots.jms.queue.pojos.Button;

/**
 * Shared constants, callbacks and labels for the "looking for" profile field.
 * Keeps the stored codes stable while showing Spanish labels to users.
 */
public final class LookingForOption {

    public static final String LOOKING_FOR_FRIENDSHIP = "FRIENDSHIP";
    public static final String LOOKING_FOR_RELATIONSHIP = "RELATIONSHIP";
    public static final String LOOKING_FOR_ONLINE_RELATIONSHIP = "ONLINE_RELATIONSHIP";
    public static final String LOOKING_FOR_CASUAL = "CASUAL";
    public static final String LOOKING_FOR_SUGAR_DADDY = "SUGAR_DADDY";
    public static final String LOOKING_FOR_SERIOUS_RELATIONSHIP = "SERIOUS_RELATIONSHIP";
    public static final String LOOKING_FOR_MARRIAGE = "MARRIAGE";
    public static final String LOOKING_FOR_LOVERS = "LOVERS";
    public static final String LOOKING_FOR_INFORMAL_RELATIONSHIP = "INFORMAL_RELATIONSHIP";

    public static final String CALLBACK_LOOKING_FOR_FRIENDSHIP = "club_looking_for_friendship";
    public static final String CALLBACK_LOOKING_FOR_RELATIONSHIP = "club_looking_for_relationship";
    public static final String CALLBACK_LOOKING_FOR_ONLINE_RELATIONSHIP = "club_looking_for_online_relationship";
    public static final String CALLBACK_LOOKING_FOR_CASUAL = "club_looking_for_casual";
    public static final String CALLBACK_LOOKING_FOR_SUGAR_DADDY = "club_looking_for_sugar_daddy";
    public static final String CALLBACK_LOOKING_FOR_SERIOUS_RELATIONSHIP = "club_looking_for_serious_relationship";
    public static final String CALLBACK_LOOKING_FOR_MARRIAGE = "club_looking_for_marriage";
    public static final String CALLBACK_LOOKING_FOR_LOVERS = "club_looking_for_lovers";
    public static final String CALLBACK_LOOKING_FOR_INFORMAL_RELATIONSHIP = "club_looking_for_informal_relationship";

    private LookingForOption() {
    }

    /**
     * Returns the Spanish label for a stored looking-for code.
     *
     * @param code the stored code
     * @return the Spanish label, or the original code if unknown
     */
    public static String translate(String code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case LOOKING_FOR_FRIENDSHIP -> "Amistad";
            case LOOKING_FOR_RELATIONSHIP -> "Relación";
            case LOOKING_FOR_ONLINE_RELATIONSHIP -> "Relación online";
            case LOOKING_FOR_CASUAL -> "Algo casual";
            case LOOKING_FOR_SUGAR_DADDY -> "Sugar";
            case LOOKING_FOR_SERIOUS_RELATIONSHIP -> "Relación seria";
            case LOOKING_FOR_MARRIAGE -> "Matrimonio";
            case LOOKING_FOR_LOVERS -> "Novios";
            case LOOKING_FOR_INFORMAL_RELATIONSHIP -> "Relación informal";
            default -> code;
        };
    }

    /**
     * Maps a callback payload to the stored looking-for code.
     *
     * @param callback the callback data
     * @return the stored code, or null if unknown
     */
    public static String fromCallback(String callback) {
        return switch (callback) {
            case CALLBACK_LOOKING_FOR_FRIENDSHIP -> LOOKING_FOR_FRIENDSHIP;
            case CALLBACK_LOOKING_FOR_RELATIONSHIP -> LOOKING_FOR_RELATIONSHIP;
            case CALLBACK_LOOKING_FOR_ONLINE_RELATIONSHIP -> LOOKING_FOR_ONLINE_RELATIONSHIP;
            case CALLBACK_LOOKING_FOR_CASUAL -> LOOKING_FOR_CASUAL;
            case CALLBACK_LOOKING_FOR_SUGAR_DADDY -> LOOKING_FOR_SUGAR_DADDY;
            case CALLBACK_LOOKING_FOR_SERIOUS_RELATIONSHIP -> LOOKING_FOR_SERIOUS_RELATIONSHIP;
            case CALLBACK_LOOKING_FOR_MARRIAGE -> LOOKING_FOR_MARRIAGE;
            case CALLBACK_LOOKING_FOR_LOVERS -> LOOKING_FOR_LOVERS;
            case CALLBACK_LOOKING_FOR_INFORMAL_RELATIONSHIP -> LOOKING_FOR_INFORMAL_RELATIONSHIP;
            default -> null;
        };
    }

    /**
     * Returns the inline-keyboard rows shown during registration and editing.
     *
     * @return button rows with Spanish labels and stable callback payloads
     */
    public static List<List<Button>> getButtonRows() {
        return Arrays.asList(
                Arrays.asList(
                        new Button("Amistad", CALLBACK_LOOKING_FOR_FRIENDSHIP),
                        new Button("Relación", CALLBACK_LOOKING_FOR_RELATIONSHIP)),
                Arrays.asList(
                        new Button("Relación online", CALLBACK_LOOKING_FOR_ONLINE_RELATIONSHIP),
                        new Button("Algo casual", CALLBACK_LOOKING_FOR_CASUAL)),
                Arrays.asList(
                        new Button("Sugar", CALLBACK_LOOKING_FOR_SUGAR_DADDY),
                        new Button("Relación seria", CALLBACK_LOOKING_FOR_SERIOUS_RELATIONSHIP)),
                Arrays.asList(
                        new Button("Matrimonio", CALLBACK_LOOKING_FOR_MARRIAGE),
                        new Button("Novios", CALLBACK_LOOKING_FOR_LOVERS)),
                Arrays.asList(
                        new Button("Relación informal", CALLBACK_LOOKING_FOR_INFORMAL_RELATIONSHIP)));
    }

}
