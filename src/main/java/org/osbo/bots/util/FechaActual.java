package org.osbo.bots.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FechaActual {

    // Método estático para obtener la fecha actual en formato "YYYY-MM-DD"
    public static String obtenerFechaActual() {
        // Definimos el formato solo con fecha
        SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd");
        // Obtenemos la fecha actual
        Date fecha = new Date();
        // Formateamos y devolvemos la fecha como cadena
        return formatoFecha.format(fecha);
    }

    public static String obtenerFechaActualConHora() {
        // Definimos el formato con fecha y hora
        SimpleDateFormat formatoFechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Obtenemos la fecha actual
        Date fecha = new Date();
        // Formateamos y devolvemos la fecha y hora como cadena
        return formatoFechaHora.format(fecha);
    }

}