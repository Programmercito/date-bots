package org.osbo.bots.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static String convertirHoraALaPaz(String horaUTC) {
        // Parsear la hora recibida en UTC
        LocalTime time = LocalTime.parse(horaUTC, DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate today = LocalDate.now();
        // Crear un ZonedDateTime en UTC
        ZonedDateTime utcTime = ZonedDateTime.of(today, time, ZoneId.of("UTC"));
        // Convertir a La Paz
        ZonedDateTime laPazTime = utcTime.withZoneSameInstant(ZoneId.of("America/La_Paz"));
        // Formatear la hora resultante
        return laPazTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
