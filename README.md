# Date Bot

Date Bot es una aplicación desarrollada en Java que proporciona funcionalidades relacionadas con la gestión de fechas y horarios. Este proyecto utiliza Spring Boot como framework principal y sigue una arquitectura modular para organizar sus componentes.

## Características principales

- **Controladores REST**: Incluye controladores como `PingController` para manejar solicitudes HTTP.
- **Tareas programadas**: Implementa tareas automáticas como `DeleteVencidos` y `Horarios`.
- **Procesamiento de mensajes**: Configuración de JMS para manejar colas y procesar mensajes.
- **Servicios y repositorios**: Gestión de entidades como `User` y `Message` mediante servicios y repositorios.
- **Utilidades**: Clases de utilidad como `DateUtils` y `CustomProperties` para operaciones comunes.

## Estructura del proyecto

El proyecto sigue la estructura estándar de un proyecto Spring Boot:

```
src/
  main/
    java/
      org/osbo/bots/
        DatesBotApplication.java
        controllers/
        crons/
        jms/
        model/
        processor/
        runner/
        util/
  resources/
    application.properties
    static/
    templates/
```

## Requisitos previos

- Java 17 o superior
- Maven 3.8 o superior

## Configuración

1. Clona este repositorio:
   ```bash
   git clone <URL-del-repositorio>
   ```
2. Navega al directorio del proyecto:
   ```bash
   cd date-bot
   ```
3. Compila el proyecto con Maven:
   ```bash
   ./mvnw clean install
   ```

## Ejecución

Para ejecutar la aplicación, utiliza el siguiente comando:

```bash
./mvnw spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`.

## Pruebas

Ejecuta las pruebas unitarias con el siguiente comando:

```bash
./mvnw test
```

## Contribuciones

Si deseas contribuir a este proyecto, por favor sigue los pasos a continuación:

1. Haz un fork del repositorio.
2. Crea una nueva rama para tu funcionalidad o corrección de errores:
   ```bash
   git checkout -b feature/nueva-funcionalidad
   ```
3. Realiza tus cambios y haz un commit:
   ```bash
   git commit -m "Agrega nueva funcionalidad"
   ```
4. Envía tus cambios al repositorio remoto:
   ```bash
   git push origin feature/nueva-funcionalidad
   ```
5. Abre un Pull Request en GitHub.

## Licencia

Este proyecto está licenciado bajo los términos de la licencia MIT. Consulta el archivo `LICENSE` para más detalles.

## Contacto

Para más información, puedes contactar al autor del proyecto a través de [correo electrónico](mailto:tu-email@example.com).