package org.osbo.bots.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static String convertirHoraALaPaz(String horaCEST) {
        // Parsear la hora recibida
        LocalTime time = LocalTime.parse(horaCEST, DateTimeFormatter.ofPattern("HH:mm"));
        // Usar la fecha actual
        LocalDate today = LocalDate.now();
        // Crear un ZonedDateTime en CEST
        ZonedDateTime cestTime = ZonedDateTime.of(today, time, ZoneId.of("Europe/Paris")); // CEST
        // Convertir a La Paz
        ZonedDateTime laPazTime = cestTime.withZoneSameInstant(ZoneId.of("America/La_Paz"));
        // Formatear la hora resultante
        return laPazTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
