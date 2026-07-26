package org.osbo.bots.model.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.UserRepository;
import org.osbo.bots.util.AgeCalculator;
import org.osbo.bots.util.LookingForOption;
import org.springframework.stereotype.Component;

import io.micrometer.common.lang.NonNull;

/**
 * Handles the {@code /editar_perfil} flow for approved friendship club
 * profiles.
 */
@Component
public class ClubProfileEditService {

    public static final String COMMAND_EDIT_PROFILE = "/editar_perfil";

    public static final String CALLBACK_EDIT_NAME = "club_edit_name";
    public static final String CALLBACK_EDIT_BIRTHDATE = "club_edit_birthdate";
    public static final String CALLBACK_EDIT_GENDER = "club_edit_gender";
    public static final String CALLBACK_EDIT_ORIENTATION = "club_edit_orientation";
    public static final String CALLBACK_EDIT_CITY = "club_edit_city";
    public static final String CALLBACK_EDIT_DESCRIPTION = "club_edit_description";
    public static final String CALLBACK_EDIT_TASTES = "club_edit_tastes";
    public static final String CALLBACK_EDIT_TRAITS = "club_edit_traits";
    public static final String CALLBACK_EDIT_LOOKING_FOR = "club_edit_looking_for";
    public static final String CALLBACK_EDIT_PHOTO = "club_edit_photo";
    public static final String CALLBACK_EDIT_CONTACT = "club_edit_contact";
    public static final String CALLBACK_EDIT_FINISH = "club_edit_finish";

    public static final String STATE_EDIT_MENU = "club_edit_menu";
    public static final String STATE_EDIT_NAME = "club_edit_name";
    public static final String STATE_EDIT_BIRTHDATE = "club_edit_birthdate";
    public static final String STATE_EDIT_GENDER = "club_edit_gender";
    public static final String STATE_EDIT_ORIENTATION = "club_edit_orientation";
    public static final String STATE_EDIT_CITY = "club_edit_city";
    public static final String STATE_EDIT_DESCRIPTION = "club_edit_description";
    public static final String STATE_EDIT_TASTES = "club_edit_tastes";
    public static final String STATE_EDIT_TRAITS = "club_edit_traits";
    public static final String STATE_EDIT_LOOKING_FOR = "club_edit_looking_for";
    public static final String STATE_EDIT_PHOTO = "club_edit_photo";
    public static final String STATE_EDIT_CONTACT = "club_edit_contact";

    public static final String GENDER_MALE = ClubRegistrationService.GENDER_MALE;
    public static final String GENDER_FEMALE = ClubRegistrationService.GENDER_FEMALE;
    public static final String GENDER_OTHER = ClubRegistrationService.GENDER_OTHER;

    public static final String ORIENTATION_HETERO = ClubRegistrationService.ORIENTATION_HETERO;
    public static final String ORIENTATION_BI = ClubRegistrationService.ORIENTATION_BI;

    public static final String STATUS_APPROVED = ClubRegistrationService.STATUS_APPROVED;
    public static final String STATUS_REJECTED = ClubRegistrationService.STATUS_REJECTED;

    public static final int MINIMUM_AGE = ClubRegistrationService.MINIMUM_AGE;

    private final NqueueForSend sender;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ClubProfileEditService(NqueueForSend sender, ProfileRepository profileRepository,
            UserRepository userRepository) {
        this.sender = sender;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Handles the {@code /editar_perfil} command and any edit step.
     *
     * @param user   the current user
     * @param update the Telegram update
     * @return true if the message was handled by this service
     */
    public boolean handle(@NonNull User user, @NonNull MessageUpdate update) {
        String comando = user.getComando();
        String text = update.getText();

        if ("start".equals(comando) && COMMAND_EDIT_PROFILE.equals(text)) {
            startEdit(user, update);
            return true;
        }

        if (comando != null && comando.startsWith("club_edit_")) {
            handleEditState(user, update);
            return true;
        }

        return false;
    }

    private void startEdit(User user, MessageUpdate update) {
        Profile profile = profileRepository.findByChatid(update.getChatid());
        if (profile == null || !STATUS_APPROVED.equals(profile.getStatus())) {
            sender.send(update.getChatid(),
                    "Necesitás tener un perfil aprobado para editarlo. Registrate con /club y esperá la aprobación.");
            user.setComando("start");
            return;
        }
        user.setComando(STATE_EDIT_MENU);
        sendEditMenu(update.getChatid(), profile);
    }

    private void sendEditMenu(String chatid, Profile profile) {
        String text = buildMenuText(profile);
        sender.send(chatid, text, true, buildMenuButtons());
    }

    private String buildMenuText(Profile profile) {
        StringBuilder text = new StringBuilder();
        text.append("Editá tu perfil. Seleccioná el campo que querés cambiar:\n\n");
        text.append("Nombre: ").append(profile.getName()).append("\n");
        text.append("Edad: ").append(profile.getAge()).append("\n");
        text.append("Género: ").append(translateGender(profile.getGender())).append("\n");
        text.append("Orientación: ").append(translateOrientation(profile.getOrientation())).append("\n");
        text.append("Ciudad: ").append(profile.getCity()).append("\n");
        text.append("Sobre vos: ").append(profile.getDescription()).append("\n");
        text.append("Gustos: ").append(profile.getTastes()).append("\n");
        text.append("Personalidad: ").append(profile.getTraits()).append("\n");
        text.append("Buscás: ").append(LookingForOption.translate(profile.getLookingFor())).append("\n");
        text.append("Contacto: ").append(formatContact(profile)).append("\n");
        return text.toString();
    }

    private String formatContact(Profile profile) {
        if (profile.getContactUsername() != null && !profile.getContactUsername().isBlank()
                && profile.getWhatsapp() != null && !profile.getWhatsapp().isBlank()) {
            return "Telegram y WhatsApp";
        }
        if (profile.getContactUsername() != null && !profile.getContactUsername().isBlank()) {
            return "Telegram";
        }
        if (profile.getWhatsapp() != null && !profile.getWhatsapp().isBlank()) {
            return "WhatsApp";
        }
        return "Sin contacto";
    }

    private List<List<Button>> buildMenuButtons() {
        return Arrays.asList(
                Arrays.asList(new Button("Nombre", CALLBACK_EDIT_NAME),
                        new Button("Fecha de nacimiento", CALLBACK_EDIT_BIRTHDATE),
                        new Button("Género", CALLBACK_EDIT_GENDER)),
                Arrays.asList(new Button("Orientación", CALLBACK_EDIT_ORIENTATION),
                        new Button("Ciudad", CALLBACK_EDIT_CITY),
                        new Button("Descripción", CALLBACK_EDIT_DESCRIPTION)),
                Arrays.asList(new Button("Gustos", CALLBACK_EDIT_TASTES),
                        new Button("Personalidad", CALLBACK_EDIT_TRAITS),
                        new Button("Buscando", CALLBACK_EDIT_LOOKING_FOR)),
                Arrays.asList(new Button("Foto", CALLBACK_EDIT_PHOTO),
                        new Button("Contacto", CALLBACK_EDIT_CONTACT),
                        new Button("Terminar", CALLBACK_EDIT_FINISH)));
    }

    private void handleEditState(User user, MessageUpdate update) {
        String comando = user.getComando();
        switch (comando) {
            case STATE_EDIT_MENU -> handleMenuSelection(user, update);
            case STATE_EDIT_NAME -> handleName(user, update);
            case STATE_EDIT_BIRTHDATE -> handleBirthdate(user, update);
            case STATE_EDIT_GENDER -> handleGender(user, update);
            case STATE_EDIT_ORIENTATION -> handleOrientation(user, update);
            case STATE_EDIT_CITY -> handleCity(user, update);
            case STATE_EDIT_DESCRIPTION -> handleDescription(user, update);
            case STATE_EDIT_TASTES -> handleTastes(user, update);
            case STATE_EDIT_TRAITS -> handleTraits(user, update);
            case STATE_EDIT_LOOKING_FOR -> handleLookingFor(user, update);
            case STATE_EDIT_PHOTO -> handlePhoto(user, update);
            case STATE_EDIT_CONTACT -> handleContact(user, update);
            default -> {
                sender.send(update.getChatid(),
                        "Hubo un problema con la edición. Escribí /editar_perfil para comenzar de nuevo.");
                user.setComando("start");
            }
        }
    }

    private void handleMenuSelection(User user, MessageUpdate update) {
        String text = update.getText();
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }

        switch (text) {
            case CALLBACK_EDIT_NAME -> {
                user.setComando(STATE_EDIT_NAME);
                sender.send(update.getChatid(), "Escribí tu nuevo nombre o apodo:");
            }
            case CALLBACK_EDIT_BIRTHDATE -> {
                user.setComando(STATE_EDIT_BIRTHDATE);
                sender.send(update.getChatid(),
                        "Escribí tu nueva fecha de nacimiento en formato DD/MM/AAAA, por ejemplo: 15/03/2000.");
            }
            case CALLBACK_EDIT_GENDER -> {
                user.setComando(STATE_EDIT_GENDER);
                List<List<Button>> buttons = Arrays.asList(
                        Arrays.asList(new Button("Hombre", ClubRegistrationService.CALLBACK_GENDER_MALE),
                                new Button("Mujer", ClubRegistrationService.CALLBACK_GENDER_FEMALE),
                                new Button("Otro", ClubRegistrationService.CALLBACK_GENDER_OTHER)));
                sender.send(update.getChatid(), "Seleccioná tu género:", true, buttons);
            }
            case CALLBACK_EDIT_ORIENTATION -> {
                user.setComando(STATE_EDIT_ORIENTATION);
                List<List<Button>> buttons = Arrays.asList(
                        Arrays.asList(new Button("Hetero", ClubRegistrationService.CALLBACK_ORIENTATION_HETERO),
                                new Button("Bi", ClubRegistrationService.CALLBACK_ORIENTATION_BI)));
                sender.send(update.getChatid(), "Seleccioná tu orientación:", true, buttons);
            }
            case CALLBACK_EDIT_CITY -> {
                user.setComando(STATE_EDIT_CITY);
                sender.send(update.getChatid(), "¿En qué ciudad de Bolivia estás?");
            }
            case CALLBACK_EDIT_DESCRIPTION -> {
                user.setComando(STATE_EDIT_DESCRIPTION);
                sender.send(update.getChatid(), "Contanos un poco sobre vos:");
            }
            case CALLBACK_EDIT_TASTES -> {
                user.setComando(STATE_EDIT_TASTES);
                sender.send(update.getChatid(), "¿Qué cosas te gustan? (música, hobbies, etc.)");
            }
            case CALLBACK_EDIT_TRAITS -> {
                user.setComando(STATE_EDIT_TRAITS);
                sender.send(update.getChatid(), "¿Cómo describirías tu personalidad?");
            }
            case CALLBACK_EDIT_LOOKING_FOR -> {
                user.setComando(STATE_EDIT_LOOKING_FOR);
                sender.send(update.getChatid(), "¿Qué estás buscando?", true, LookingForOption.getButtonRows());
            }
            case CALLBACK_EDIT_PHOTO -> {
                user.setComando(STATE_EDIT_PHOTO);
                sender.send(update.getChatid(), "Envíá una nueva foto para tu perfil.");
            }
            case CALLBACK_EDIT_CONTACT -> {
                user.setComando(STATE_EDIT_CONTACT);
                askForContact(update.getChatid(), update.getUser(), profile);
            }
            case CALLBACK_EDIT_FINISH -> {
                sender.send(update.getChatid(), "Tu perfil fue actualizado.", true,
                        List.of(List.of(new Button("Volver al inicio", "/start"))));
                user.setComando("start");
            }
            default -> sendEditMenu(update.getChatid(), profile);
        }
    }

    private void handleName(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, escribí tu nombre o apodo.");
            return;
        }
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        profile.setName(update.getText().trim());
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleBirthdate(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(),
                    "Por favor, escribí tu fecha de nacimiento en formato DD/MM/AAAA, por ejemplo: 15/03/2000.");
            return;
        }
        LocalDate birthDate;
        try {
            birthDate = AgeCalculator.parseUserDate(update.getText().trim());
        } catch (DateTimeParseException e) {
            sender.send(update.getChatid(),
                    "No entendí la fecha. Usá el formato DD/MM/AAAA, por ejemplo: 15/03/2000.");
            return;
        }
        Integer age = AgeCalculator.calculateAge(birthDate, null);
        if (age == null || age < MINIMUM_AGE) {
            sender.send(update.getChatid(),
                    "Debés ser mayor de 18 años. Por favor, ingresá una fecha válida.");
            return;
        }
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        profile.setBirthDate(birthDate);
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleGender(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        String gender = mapGenderCallback(update.getText());
        if (gender == null) {
            askForGender(update.getChatid());
            return;
        }
        profile.setGender(gender);
        if (!isAllowedCombination(profile.getGender(), profile.getOrientation())) {
            profileRepository.save(profile);
            rejectProfile(profile);
            sender.send(update.getChatid(),
                    "La combinación de género y orientación no está permitida en el club. Tu perfil fue desactivado. Podés volver a registrarte con /club.");
            user.setComando("start");
            return;
        }
        saveAndReturnToMenu(user, update, profile);
    }

    private void askForGender(String chatid) {
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Hombre", ClubRegistrationService.CALLBACK_GENDER_MALE),
                        new Button("Mujer", ClubRegistrationService.CALLBACK_GENDER_FEMALE),
                        new Button("Otro", ClubRegistrationService.CALLBACK_GENDER_OTHER)));
        sender.send(chatid, "Seleccioná una opción de género:", true, buttons);
    }

    private void handleOrientation(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        String orientation = mapOrientationCallback(update.getText());
        if (orientation == null) {
            askForOrientation(update.getChatid());
            return;
        }
        profile.setOrientation(orientation);
        if (!isAllowedCombination(profile.getGender(), profile.getOrientation())) {
            profileRepository.save(profile);
            rejectProfile(profile);
            sender.send(update.getChatid(),
                    "La combinación de género y orientación no está permitida en el club. Tu perfil fue desactivado. Podés volver a registrarte con /club.");
            user.setComando("start");
            return;
        }
        saveAndReturnToMenu(user, update, profile);
    }

    private void askForOrientation(String chatid) {
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Hetero", ClubRegistrationService.CALLBACK_ORIENTATION_HETERO),
                        new Button("Bi", ClubRegistrationService.CALLBACK_ORIENTATION_BI)));
        sender.send(chatid, "Seleccioná una opción de orientación:", true, buttons);
    }

    private void handleCity(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, escribí tu ciudad.");
            return;
        }
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        profile.setCity(update.getText().trim());
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleDescription(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, escribí una descripción.");
            return;
        }
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        profile.setDescription(update.getText().trim());
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleTastes(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, contanos qué cosas te gustan.");
            return;
        }
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        profile.setTastes(update.getText().trim());
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleTraits(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, describí tu personalidad.");
            return;
        }
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        profile.setTraits(update.getText().trim());
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleLookingFor(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        String lookingFor = LookingForOption.fromCallback(update.getText());
        if (lookingFor == null) {
            sender.send(update.getChatid(), "¿Qué estás buscando?", true, LookingForOption.getButtonRows());
            return;
        }
        profile.setLookingFor(lookingFor);
        saveAndReturnToMenu(user, update, profile);
    }

    private void handlePhoto(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        String[] medias = update.getMedias();
        if (medias == null || medias.length == 0 || medias[0] == null || medias[0].isBlank()) {
            sender.send(update.getChatid(), "No recibí una foto. Por favor, enviá una imagen.");
            return;
        }
        profile.setPhotoFileId(medias[0]);
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleContact(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }

        String text = update.getText();
        if (ClubRegistrationService.CALLBACK_CONTACT_TELEGRAM.equals(text)) {
            String username = update.getUser();
            if (username == null || username.isBlank()) {
                sender.send(update.getChatid(),
                        "No tengo tu usuario de Telegram. Envíá tu número de WhatsApp para continuar.");
                return;
            }
            profile.setContactUsername(username);
            saveAndReturnToMenu(user, update, profile);
            return;
        }

        if (ClubRegistrationService.CALLBACK_CONTACT_SKIP.equals(text)) {
            if (hasContactMethod(profile)) {
                saveAndReturnToMenu(user, update, profile);
            } else {
                sender.send(update.getChatid(),
                        "Necesitamos al menos un medio de contacto. Confirmá tu Telegram o enviá tu WhatsApp.");
                askForContact(update.getChatid(), update.getUser(), profile);
            }
            return;
        }

        if (isEmptyText(update)) {
            askForContact(update.getChatid(), update.getUser(), profile);
            return;
        }

        String whatsapp = update.getText().trim();
        if (!isValidWhatsapp(whatsapp)) {
            sender.send(update.getChatid(),
                    "El número de WhatsApp no es válido. Usá solo números, espacios, guiones y el signo +. Por ejemplo: +591 70012345.");
            return;
        }
        profile.setWhatsapp(whatsapp);
        saveAndReturnToMenu(user, update, profile);
    }

    private void askForContact(String chatid, String username, Profile profile) {
        List<List<Button>> buttons = buildContactButtons(username);
        if (username != null && !username.isBlank()) {
            sender.send(chatid,
                    "Elegí cómo queres que te contacten tus matches.\nTu usuario actual: @" + username,
                    true, buttons);
        } else {
            sender.send(chatid,
                    "Para que tus matches puedan contactarte, enviá tu número de WhatsApp.",
                    true, buttons);
        }
    }

    private List<List<Button>> buildContactButtons(String username) {
        if (username != null && !username.isBlank()) {
            return Arrays.asList(
                    Arrays.asList(new Button("Usar Telegram @" + username,
                            ClubRegistrationService.CALLBACK_CONTACT_TELEGRAM)),
                    Arrays.asList(new Button("Usar WhatsApp", ClubRegistrationService.CALLBACK_CONTACT_SKIP)));
        }
        return List.of();
    }

    private boolean hasContactMethod(Profile profile) {
        return (profile.getContactUsername() != null && !profile.getContactUsername().isBlank())
                || (profile.getWhatsapp() != null && !profile.getWhatsapp().isBlank());
    }

    private boolean isValidWhatsapp(String whatsapp) {
        if (whatsapp == null || whatsapp.isBlank()) {
            return false;
        }
        return whatsapp.matches("^[+\\d\\-\\s]+$");
    }

    private void saveAndReturnToMenu(User user, MessageUpdate update, Profile profile) {
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);
        user.setComando(STATE_EDIT_MENU);
        sendEditMenu(update.getChatid(), profile);
    }

    private Profile requireApprovedProfile(String chatid) {
        Profile profile = profileRepository.findByChatid(chatid);
        if (profile == null || !STATUS_APPROVED.equals(profile.getStatus())) {
            return null;
        }
        return profile;
    }

    private void rejectProfile(Profile profile) {
        profile.setStatus(STATUS_REJECTED);
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);
    }

    private String mapGenderCallback(String callback) {
        return switch (callback) {
            case ClubRegistrationService.CALLBACK_GENDER_MALE -> GENDER_MALE;
            case ClubRegistrationService.CALLBACK_GENDER_FEMALE -> GENDER_FEMALE;
            case ClubRegistrationService.CALLBACK_GENDER_OTHER -> GENDER_OTHER;
            default -> null;
        };
    }

    private String mapOrientationCallback(String callback) {
        return switch (callback) {
            case ClubRegistrationService.CALLBACK_ORIENTATION_HETERO -> ORIENTATION_HETERO;
            case ClubRegistrationService.CALLBACK_ORIENTATION_BI -> ORIENTATION_BI;
            default -> null;
        };
    }

    private boolean isAllowedCombination(String gender, String orientation) {
        return (GENDER_MALE.equals(gender) && ORIENTATION_HETERO.equals(orientation))
                || (GENDER_FEMALE.equals(gender) && ORIENTATION_HETERO.equals(orientation))
                || (GENDER_FEMALE.equals(gender) && ORIENTATION_BI.equals(orientation));
    }

    private boolean isEmptyText(MessageUpdate update) {
        return update.getText() == null || update.getText().isBlank();
    }

    private String translateGender(String gender) {
        return switch (gender) {
            case GENDER_MALE -> "Hombre";
            case GENDER_FEMALE -> "Mujer";
            case GENDER_OTHER -> "Otro";
            default -> gender;
        };
    }

    private String translateOrientation(String orientation) {
        return switch (orientation) {
            case ORIENTATION_HETERO -> "Hetero";
            case ORIENTATION_BI -> "Bi";
            default -> orientation;
        };
    }

    private String isoTimestamp() {
        return OffsetDateTime.now().toString();
    }

}
