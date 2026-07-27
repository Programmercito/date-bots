package org.osbo.bots.util;

import java.util.Arrays;
import java.util.List;

import org.osbo.bots.jms.queue.pojos.Button;

/**
 * Shared constants and helpers for the city profile field.
 * Lists the main cities of Bolivia as selectable buttons.
 */
public final class CityOption {

    public static final String CITY_LA_PAZ = "La Paz";
    public static final String CITY_EL_ALTO = "El Alto";
    public static final String CITY_COCHABAMBA = "Cochabamba";
    public static final String CITY_SANTA_CRUZ = "Santa Cruz";
    public static final String CITY_ORURO = "Oruro";
    public static final String CITY_SUCRE = "Sucre";
    public static final String CITY_POTOSI = "Potosí";
    public static final String CITY_TARIJA = "Tarija";
    public static final String CITY_TRINIDAD = "Trinidad";
    public static final String CITY_COBIJA = "Cobija";

    public static final String CALLBACK_CITY_LA_PAZ = "club_city_la_paz";
    public static final String CALLBACK_CITY_EL_ALTO = "club_city_el_alto";
    public static final String CALLBACK_CITY_COCHABAMBA = "club_city_cochabamba";
    public static final String CALLBACK_CITY_SANTA_CRUZ = "club_city_santa_cruz";
    public static final String CALLBACK_CITY_ORURO = "club_city_oruro";
    public static final String CALLBACK_CITY_SUCRE = "club_city_sucre";
    public static final String CALLBACK_CITY_POTOSI = "club_city_potosi";
    public static final String CALLBACK_CITY_TARIJA = "club_city_tarija";
    public static final String CALLBACK_CITY_TRINIDAD = "club_city_trinidad";
    public static final String CALLBACK_CITY_COBIJA = "club_city_cobija";

    private CityOption() {
    }

    /**
     * Maps a callback payload to the stored city name.
     *
     * @param callback the callback data
     * @return the city name, or null if unknown
     */
    public static String fromCallback(String callback) {
        if (callback == null) {
            return null;
        }
        return switch (callback) {
            case CALLBACK_CITY_LA_PAZ -> CITY_LA_PAZ;
            case CALLBACK_CITY_EL_ALTO -> CITY_EL_ALTO;
            case CALLBACK_CITY_COCHABAMBA -> CITY_COCHABAMBA;
            case CALLBACK_CITY_SANTA_CRUZ -> CITY_SANTA_CRUZ;
            case CALLBACK_CITY_ORURO -> CITY_ORURO;
            case CALLBACK_CITY_SUCRE -> CITY_SUCRE;
            case CALLBACK_CITY_POTOSI -> CITY_POTOSI;
            case CALLBACK_CITY_TARIJA -> CITY_TARIJA;
            case CALLBACK_CITY_TRINIDAD -> CITY_TRINIDAD;
            case CALLBACK_CITY_COBIJA -> CITY_COBIJA;
            default -> null;
        };
    }

    /**
     * Returns whether the given string is a known city callback.
     */
    public static boolean isCallback(String text) {
        return fromCallback(text) != null;
    }

    /**
     * Returns the inline-keyboard rows for city selection.
     */
    public static List<List<Button>> getButtonRows() {
        return Arrays.asList(
                Arrays.asList(
                        new Button("La Paz", CALLBACK_CITY_LA_PAZ),
                        new Button("El Alto", CALLBACK_CITY_EL_ALTO)),
                Arrays.asList(
                        new Button("Cochabamba", CALLBACK_CITY_COCHABAMBA),
                        new Button("Santa Cruz", CALLBACK_CITY_SANTA_CRUZ)),
                Arrays.asList(
                        new Button("Oruro", CALLBACK_CITY_ORURO),
                        new Button("Sucre", CALLBACK_CITY_SUCRE)),
                Arrays.asList(
                        new Button("Potosí", CALLBACK_CITY_POTOSI),
                        new Button("Tarija", CALLBACK_CITY_TARIJA)),
                Arrays.asList(
                        new Button("Trinidad", CALLBACK_CITY_TRINIDAD),
                        new Button("Cobija", CALLBACK_CITY_COBIJA)));
    }
}
