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
import org.osbo.bots.util.CityOption;
import org.osbo.bots.util.LookingForOption;
import org.osbo.bots.util.MarkdownEscaper;
import org.springframework.stereotype.Component;

import io.micrometer.common.lang.NonNull;

/**
 * Handles the {@code /editar_perfil} flow for approved friendship club
 * profiles.
 */
@Component
public class ClubProfileEditService {

    public static final String COMMAND_EDIT_PROFILE = "/editar_perfil";
    public static final String COMMAND_CLUB = "/club";
    public static final String COMMAND_START = "/start";

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
    public static final String CALLBACK_EDIT_PHOTO_DONE = "club_edit_photo_done";
    public static final String CALLBACK_EDIT_CONTACT = "club_edit_contact";
    public static final String CALLBACK_EDIT_FINISH = "club_edit_finish";
    public static final String CALLBACK_EDIT_CANCEL = "club_edit_cancel";

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
    private final ClubRegistrationService clubRegistrationService;

    public ClubProfileEditService(NqueueForSend sender, ProfileRepository profileRepository,
            UserRepository userRepository, ClubRegistrationService clubRegistrationService) {
        this.sender = sender;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.clubRegistrationService = clubRegistrationService;
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

        if (COMMAND_EDIT_PROFILE.equals(text)) {
            startEdit(user, update);
            return true;
        }

        if (comando != null && comando.startsWith("club_edit_")) {
            if (COMMAND_CLUB.equals(update.getText()) || COMMAND_START.equals(update.getText())) {
                cancelEdit(user, update);
                return true;
            }
            if (CALLBACK_EDIT_PHOTO_DONE.equals(update.getText()) && STATE_EDIT_PHOTO.equals(comando)) {
                handlePhotoDone(user, update);
                return true;
            }
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

    private void cancelEdit(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (COMMAND_CLUB.equals(update.getText()) && profile != null) {
            user.setComando("start");
            clubRegistrationService.sendApprovedStatus(update.getChatid(), profile);
            return;
        }
        sender.sendMarkdown(update.getChatid(), "🏠 *Volviendo al menú principal.*", true,
                List.of(List.of(new Button("Volver al inicio", "/start"))));
        user.setComando("start");
    }

    private void sendEditMenu(String chatid, Profile profile) {
        String text = buildMenuText(profile);
        List<List<Button>> buttons = buildMenuButtons();
        if (profile.getPhotoFileId() != null && !profile.getPhotoFileId().isBlank()) {
            sender.sendPhoto(chatid, profile.getPhotoFileId(), text, true, buttons, "Markdown");
        } else {
            sender.sendMarkdown(chatid, text, true, buttons);
        }
    }

    private String buildMenuText(Profile profile) {
        StringBuilder text = new StringBuilder();
        text.append("📝 *Editá tu perfil*\n");
        text.append("Seleccioná el campo que querés cambiar:\n\n");
        text.append("*👤 Nombre:* ").append(escapeMarkdown(profile.getName())).append("\n");
        text.append("*🎂 Edad:* ").append(profile.getAge()).append(" años\n");
        text.append("*⚧ Género:* ").append(translateGender(profile.getGender())).append("\n");
        text.append("*💕 Orientación:* ").append(translateOrientation(profile.getOrientation())).append("\n");
        text.append("*📍 Ciudad:* ").append(escapeMarkdown(profile.getCity())).append("\n");
        text.append("*📝 Sobre vos:* ").append(escapeMarkdown(profile.getDescription())).append("\n");
        text.append("*🎸 Gustos:* ").append(escapeMarkdown(profile.getTastes())).append("\n");
        text.append("*🧠 Personalidad:* ").append(escapeMarkdown(profile.getTraits())).append("\n");
        text.append("*💘 Buscás:* ").append(LookingForOption.translate(profile.getLookingFor())).append("\n");
        text.append("*📞 Contacto:* ").append(escapeMarkdown(formatContact(profile))).append("\n");
        return text.toString();
    }

    private String escapeMarkdown(String text) {
        return MarkdownEscaper.escape(text);
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
                Arrays.asList(new Button("👤 Nombre", CALLBACK_EDIT_NAME),
                        new Button("🎂 Fecha de nacimiento", CALLBACK_EDIT_BIRTHDATE),
                        new Button("⚧ Género", CALLBACK_EDIT_GENDER)),
                Arrays.asList(new Button("💕 Orientación", CALLBACK_EDIT_ORIENTATION),
                        new Button("📍 Ciudad", CALLBACK_EDIT_CITY),
                        new Button("📝 Descripción", CALLBACK_EDIT_DESCRIPTION)),
                Arrays.asList(new Button("🎸 Gustos", CALLBACK_EDIT_TASTES),
                        new Button("🧠 Personalidad", CALLBACK_EDIT_TRAITS),
                        new Button("💘 Buscando", CALLBACK_EDIT_LOOKING_FOR)),
                Arrays.asList(new Button("📷 Foto", CALLBACK_EDIT_PHOTO),
                        new Button("📞 Contacto", CALLBACK_EDIT_CONTACT),
                        new Button("✅ Terminar", CALLBACK_EDIT_FINISH)),
                Arrays.asList(new Button("⬅️ Volver al menú del club", COMMAND_CLUB),
                        new Button("🏠 Volver al inicio", COMMAND_START)));
    }

    private void handleEditState(User user, MessageUpdate update) {
        String comando = user.getComando();
        if (CALLBACK_EDIT_CANCEL.equals(update.getText())) {
            Profile profile = requireApprovedProfile(update.getChatid());
            if (profile != null) {
                user.setComando(STATE_EDIT_MENU);
                sendEditMenu(update.getChatid(), profile);
            } else {
                user.setComando("start");
            }
            return;
        }
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
                sendFieldPrompt(update.getChatid(), "👤 Nombre", "escribí tu nuevo nombre o apodo",
                        profile.getName());
            }
            case CALLBACK_EDIT_BIRTHDATE -> {
                user.setComando(STATE_EDIT_BIRTHDATE);
                sendFieldPrompt(update.getChatid(), "🎂 Fecha de nacimiento",
                        "escribí tu nueva fecha en formato DD/MM/AAAA, por ejemplo: 15/03/2000",
                        profile.getBirthDate() != null ? profile.getBirthDate().toString() : null);
            }
            case CALLBACK_EDIT_GENDER -> {
                user.setComando(STATE_EDIT_GENDER);
                List<List<Button>> buttons = Arrays.asList(
                        Arrays.asList(new Button("👨 Hombre", ClubRegistrationService.CALLBACK_GENDER_MALE),
                                new Button("👩 Mujer", ClubRegistrationService.CALLBACK_GENDER_FEMALE),
                                new Button("⚧ Otro", ClubRegistrationService.CALLBACK_GENDER_OTHER)),
                        cancelButtonRow());
                sender.sendMarkdown(update.getChatid(), "*⚧ Género*\nActual: " + translateGender(profile.getGender())
                        + "\n\nSeleccioná tu género:", true, buttons);
            }
            case CALLBACK_EDIT_ORIENTATION -> {
                user.setComando(STATE_EDIT_ORIENTATION);
                List<List<Button>> buttons = Arrays.asList(
                        Arrays.asList(new Button("💕 Hetero", ClubRegistrationService.CALLBACK_ORIENTATION_HETERO),
                                new Button("💜 Bi", ClubRegistrationService.CALLBACK_ORIENTATION_BI)),
                        cancelButtonRow());
                sender.sendMarkdown(update.getChatid(),
                        "*💕 Orientación*\nActual: " + translateOrientation(profile.getOrientation())
                                + "\n\nSeleccioná tu orientación:",
                        true, buttons);
            }
            case CALLBACK_EDIT_CITY -> {
                user.setComando(STATE_EDIT_CITY);
                List<List<Button>> cityButtons = new java.util.ArrayList<>(CityOption.getButtonRows());
                cityButtons.add(cancelButtonRow());
                sender.sendMarkdown(update.getChatid(),
                        "*📍 Ciudad*\nActual: " + MarkdownEscaper.escape(profile.getCity())
                                + "\n\n¿En qué ciudad de Bolivia estás?",
                        true, cityButtons);
            }
            case CALLBACK_EDIT_DESCRIPTION -> {
                user.setComando(STATE_EDIT_DESCRIPTION);
                sendFieldPrompt(update.getChatid(), "📝 Sobre vos", "contanos un poco sobre vos",
                        profile.getDescription());
            }
            case CALLBACK_EDIT_TASTES -> {
                user.setComando(STATE_EDIT_TASTES);
                sendFieldPrompt(update.getChatid(), "🎸 Gustos", "contanos qué cosas te gustan (música, hobbies, etc.)",
                        profile.getTastes());
            }
            case CALLBACK_EDIT_TRAITS -> {
                user.setComando(STATE_EDIT_TRAITS);
                sendFieldPrompt(update.getChatid(), "🧠 Personalidad", "describí tu personalidad",
                        profile.getTraits());
            }
            case CALLBACK_EDIT_LOOKING_FOR -> {
                user.setComando(STATE_EDIT_LOOKING_FOR);
                List<List<Button>> buttons = new java.util.ArrayList<>(LookingForOption.getButtonRows());
                buttons.add(cancelButtonRow());
                sender.sendMarkdown(update.getChatid(),
                        "*💘 Buscando*\nActual: " + LookingForOption.translate(profile.getLookingFor())
                                + "\n\n¿Qué estás buscando?",
                        true, buttons);
            }
            case CALLBACK_EDIT_PHOTO -> {
                user.setComando(STATE_EDIT_PHOTO);
                int current = profile.photoCount();
                String currentInfo = current > 0 ? "Tenés " + current + " foto" + (current > 1 ? "s" : "") + " guardada" + (current > 1 ? "s" : "") + ". Las nuevas *reemplazarán* las actuales." : "Todavía no tenés fotos.";
                sender.sendMarkdown(update.getChatid(),
                        "*📷 Fotos*\n" + currentInfo + "\n\nEnviá tus nuevas fotos (hasta " + ClubRegistrationService.MAX_PHOTOS + "). La primera foto que mandes reemplazará las actuales.",
                        true, cancelButtonRowAsList());
            }
            case CALLBACK_EDIT_CONTACT -> {
                user.setComando(STATE_EDIT_CONTACT);
                askForContact(update.getChatid(), update.getUser(), profile);
            }
            case CALLBACK_EDIT_FINISH -> {
                sender.sendMarkdown(update.getChatid(), "✅ *Perfil actualizado*\n\n¿Querés hacer algo más?", true,
                        List.of(
                                List.of(new Button("🔍 Ver personas", "/ver_personas"),
                                        new Button("✏️ Seguir editando", COMMAND_EDIT_PROFILE)),
                                List.of(new Button("🏠 Volver al inicio", "/start"))));
                user.setComando("start");
            }
            default -> sendEditMenu(update.getChatid(), profile);
        }
    }

    private void sendFieldPrompt(String chatid, String emojiLabel, String instruction, String currentValue) {
        StringBuilder text = new StringBuilder();
        text.append("*").append(emojiLabel).append("*\n");
        if (currentValue != null && !currentValue.isBlank()) {
            text.append("Actual: ").append(escapeMarkdown(currentValue)).append("\n\n");
        }
        text.append(instruction).append(".");
        sender.sendMarkdown(chatid, text.toString(), true, cancelButtonRowAsList());
    }

    private List<List<Button>> cancelButtonRowAsList() {
        return List.of(cancelButtonRow());
    }

    private List<Button> cancelButtonRow() {
        return List.of(new Button("❌ Cancelar edición", CALLBACK_EDIT_CANCEL));
    }

    private void handleName(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            Profile profile = requireApprovedProfile(update.getChatid());
            sendFieldPrompt(update.getChatid(), "👤 Nombre", "escribí tu nuevo nombre o apodo",
                    profile == null ? null : profile.getName());
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
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        if (isEmptyText(update)) {
            sendFieldPrompt(update.getChatid(), "🎂 Fecha de nacimiento",
                    "escribí tu nueva fecha en formato DD/MM/AAAA, por ejemplo: 15/03/2000",
                    profile.getBirthDate() != null ? profile.getBirthDate().toString() : null);
            return;
        }
        LocalDate birthDate;
        try {
            birthDate = AgeCalculator.parseUserDate(update.getText().trim());
        } catch (DateTimeParseException e) {
            sender.sendMarkdown(update.getChatid(),
                    "❌ *Fecha no válida*\n\nUsá el formato DD/MM/AAAA, por ejemplo: 15/03/2000.", true,
                    cancelButtonRowAsList());
            return;
        }
        Integer age = AgeCalculator.calculateAge(birthDate, null);
        if (age == null || age < MINIMUM_AGE) {
            sender.sendMarkdown(update.getChatid(),
                    "❌ *Debés ser mayor de 18 años*\n\nPor favor, ingresá una fecha válida.", true,
                    cancelButtonRowAsList());
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
            List<List<Button>> buttons = Arrays.asList(
                    Arrays.asList(new Button("👨 Hombre", ClubRegistrationService.CALLBACK_GENDER_MALE),
                            new Button("👩 Mujer", ClubRegistrationService.CALLBACK_GENDER_FEMALE),
                            new Button("⚧ Otro", ClubRegistrationService.CALLBACK_GENDER_OTHER)),
                    cancelButtonRow());
            sender.sendMarkdown(update.getChatid(), "*⚧ Género*\nSeleccioná una opción:", true, buttons);
            return;
        }
        profile.setGender(gender);
        if (!isAllowedCombination(profile.getGender(), profile.getOrientation())) {
            profileRepository.save(profile);
            rejectProfile(profile);
            sender.sendMarkdown(update.getChatid(),
                    "⛔ *Combinación no permitida*\n\nLa combinación de género y orientación no está permitida en el club. Tu perfil fue desactivado. Podés volver a registrarte con /club.",
                    true, List.of(List.of(new Button("🤝 Volver al club", "/club"))));
            user.setComando("start");
            return;
        }
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleOrientation(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        String orientation = mapOrientationCallback(update.getText());
        if (orientation == null) {
            List<List<Button>> buttons = Arrays.asList(
                    Arrays.asList(new Button("💕 Hetero", ClubRegistrationService.CALLBACK_ORIENTATION_HETERO),
                            new Button("💜 Bi", ClubRegistrationService.CALLBACK_ORIENTATION_BI)),
                    cancelButtonRow());
            sender.sendMarkdown(update.getChatid(), "*💕 Orientación*\nSeleccioná una opción:", true, buttons);
            return;
        }
        profile.setOrientation(orientation);
        if (!isAllowedCombination(profile.getGender(), profile.getOrientation())) {
            profileRepository.save(profile);
            rejectProfile(profile);
            sender.sendMarkdown(update.getChatid(),
                    "⛔ *Combinación no permitida*\n\nLa combinación de género y orientación no está permitida en el club. Tu perfil fue desactivado. Podés volver a registrarte con /club.",
                    true, List.of(List.of(new Button("🤝 Volver al club", "/club"))));
            user.setComando("start");
            return;
        }
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleCity(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        String city = CityOption.fromCallback(update.getText());
        if (city == null) {
            List<List<Button>> cityButtons = new java.util.ArrayList<>(CityOption.getButtonRows());
            cityButtons.add(cancelButtonRow());
            sender.sendMarkdown(update.getChatid(),
                    "*📍 Ciudad*\nActual: " + MarkdownEscaper.escape(profile.getCity())
                            + "\n\n¿En qué ciudad de Bolivia estás?",
                    true, cityButtons);
            return;
        }
        profile.setCity(city);
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleDescription(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        if (isEmptyText(update)) {
            sendFieldPrompt(update.getChatid(), "📝 Sobre vos", "contanos un poco sobre vos",
                    profile.getDescription());
            return;
        }
        profile.setDescription(update.getText().trim());
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleTastes(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        if (isEmptyText(update)) {
            sendFieldPrompt(update.getChatid(), "🎸 Gustos", "contanos qué cosas te gustan (música, hobbies, etc.)",
                    profile.getTastes());
            return;
        }
        profile.setTastes(update.getText().trim());
        saveAndReturnToMenu(user, update, profile);
    }

    private void handleTraits(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
        if (isEmptyText(update)) {
            sendFieldPrompt(update.getChatid(), "🧠 Personalidad", "describí tu personalidad", profile.getTraits());
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
            List<List<Button>> buttons = new java.util.ArrayList<>(LookingForOption.getButtonRows());
            buttons.add(cancelButtonRow());
            sender.sendMarkdown(update.getChatid(),
                    "*💘 Buscando*\nActual: " + LookingForOption.translate(profile.getLookingFor())
                            + "\n\n¿Qué estás buscando?",
                    true, buttons);
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
            if (profile.photoCount() == 0) {
                sender.sendMarkdown(update.getChatid(),
                        "*📷 Fotos*\n\nNo recibí una foto. Por favor, enviá al menos una imagen.", true,
                        cancelButtonRowAsList());
            }
            return;
        }
        // First photo resets the collection; subsequent ones accumulate
        if (profile.getPhotoFileIds() == null || profile.getPhotoFileIds().isBlank()) {
            profile.resetPhotos(medias[0]);
        } else {
            profile.addPhoto(medias[0]);
        }
        profile.setUpdatedAt(OffsetDateTime.now().toString());
        profileRepository.save(profile);

        int count = profile.photoCount();
        if (count >= ClubRegistrationService.MAX_PHOTOS) {
            saveAndReturnToMenu(user, update, profile);
            return;
        }
        List<List<Button>> buttons = new java.util.ArrayList<>();
        buttons.add(List.of(new Button("✅ Listo (" + count + " foto" + (count > 1 ? "s" : "") + ")", CALLBACK_EDIT_PHOTO_DONE)));
        buttons.add(cancelButtonRow());
        sender.sendMarkdown(update.getChatid(),
                "📸 Foto " + count + " guardada. Podés enviar más (hasta " + ClubRegistrationService.MAX_PHOTOS + ") o tocar Listo.",
                true, buttons);
    }

    private void handlePhotoDone(User user, MessageUpdate update) {
        Profile profile = requireApprovedProfile(update.getChatid());
        if (profile == null) {
            user.setComando("start");
            return;
        }
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
                sender.sendMarkdown(update.getChatid(),
                        "*📞 Contacto*\n\nNo tengo tu usuario de Telegram. Envíá tu número de WhatsApp para continuar.",
                        true, cancelButtonRowAsList());
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
                sender.sendMarkdown(update.getChatid(),
                        "*📞 Contacto*\n\nNecesitamos al menos un medio de contacto. Confirmá tu Telegram o enviá tu WhatsApp.",
                        true, cancelButtonRowAsList());
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
            sender.sendMarkdown(update.getChatid(),
                    "❌ *WhatsApp no válido*\n\nUsá solo números, espacios, guiones y el signo +. Por ejemplo: +591 70012345.",
                    true, cancelButtonRowAsList());
            return;
        }
        profile.setWhatsapp(whatsapp);
        saveAndReturnToMenu(user, update, profile);
    }

    private void askForContact(String chatid, String username, Profile profile) {
        List<List<Button>> buttons = buildContactButtons(username);
        String currentContact;
        if (profile.getContactUsername() != null && !profile.getContactUsername().isBlank()) {
            currentContact = "Telegram @" + profile.getContactUsername();
        } else if (profile.getWhatsapp() != null && !profile.getWhatsapp().isBlank()) {
            currentContact = "WhatsApp " + profile.getWhatsapp();
        } else {
            currentContact = "sin contacto";
        }
        if (username != null && !username.isBlank()) {
            sender.sendMarkdown(chatid,
                    "*📞 Contacto*\nActual: " + currentContact + "\n\nElegí cómo querés que te contacten tus matches.",
                    true, buttons);
        } else {
            sender.sendMarkdown(chatid,
                    "*📞 Contacto*\nActual: " + currentContact
                            + "\n\nPara que tus matches puedan contactarte, enviá tu número de WhatsApp.",
                    true, buttons);
        }
    }

    private List<List<Button>> buildContactButtons(String username) {
        List<List<Button>> buttons = new java.util.ArrayList<>();
        if (username != null && !username.isBlank()) {
            buttons.add(List.of(new Button("✈️ Usar Telegram @" + username,
                    ClubRegistrationService.CALLBACK_CONTACT_TELEGRAM)));
        }
        buttons.add(List.of(new Button("📱 Usar WhatsApp", ClubRegistrationService.CALLBACK_CONTACT_SKIP)));
        buttons.add(cancelButtonRow());
        return buttons;
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
