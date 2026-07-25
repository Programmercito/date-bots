package org.osbo.bots.model.services;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.osbo.bots.jms.queue.enqueue.NqueueForSend;
import org.osbo.bots.jms.queue.pojos.Button;
import org.osbo.bots.jms.queue.pojos.MessageUpdate;
import org.osbo.bots.jms.queue.pojos.ModerationMessage;
import org.osbo.bots.model.entity.Profile;
import org.osbo.bots.model.entity.User;
import org.osbo.bots.model.entity.UserPlan;
import org.osbo.bots.model.repositories.ProfileRepository;
import org.osbo.bots.model.repositories.UserPlanRepository;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import io.micrometer.common.lang.NonNull;

/**
 * Handles the friendship club registration flow using the {@code users.comando}
 * state machine.
 */
@Component
public class ClubRegistrationService {

    public static final String COMMAND_CLUB = "/club";
    public static final String CALLBACK_CLUB_ENTER = "club_register";

    public static final String CALLBACK_GENDER_MALE = "club_gender_male";
    public static final String CALLBACK_GENDER_FEMALE = "club_gender_female";
    public static final String CALLBACK_GENDER_OTHER = "club_gender_other";

    public static final String CALLBACK_ORIENTATION_HETERO = "club_orientation_hetero";
    public static final String CALLBACK_ORIENTATION_BI = "club_orientation_bi";

    public static final String CALLBACK_PREVIEW_OK = "club_preview_ok";
    public static final String CALLBACK_PREVIEW_EDIT = "club_preview_edit";

    public static final String STATE_REGISTER_NAME = "club_register_name";
    public static final String STATE_REGISTER_AGE = "club_register_age";
    public static final String STATE_REGISTER_GENDER = "club_register_gender";
    public static final String STATE_REGISTER_ORIENTATION = "club_register_orientation";
    public static final String STATE_REGISTER_CITY = "club_register_city";
    public static final String STATE_REGISTER_DESCRIPTION = "club_register_description";
    public static final String STATE_REGISTER_TASTES = "club_register_tastes";
    public static final String STATE_REGISTER_TRAITS = "club_register_traits";
    public static final String STATE_REGISTER_LOOKING_FOR = "club_register_looking_for";
    public static final String STATE_REGISTER_PHOTO = "club_register_photo";
    public static final String STATE_REGISTER_PREVIEW = "club_register_preview";

    public static final String GENDER_MALE = "MALE";
    public static final String GENDER_FEMALE = "FEMALE";
    public static final String GENDER_OTHER = "OTHER";

    public static final String ORIENTATION_HETERO = "HETERO";
    public static final String ORIENTATION_BI = "BI";

    public static final String STATUS_INCOMPLETE = "INCOMPLETE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_PAUSED = "PAUSED";

    public static final String COUNTRY_BOLIVIA = "BO";
    public static final String PLAN_FREE = "FREE";

    public static final int MINIMUM_AGE = 18;

    private final NqueueForSend sender;
    private final ProfileRepository profileRepository;
    private final UserPlanRepository userPlanRepository;
    private final JmsTemplate jmsTemplate;

    public ClubRegistrationService(NqueueForSend sender, ProfileRepository profileRepository,
            UserPlanRepository userPlanRepository, JmsTemplate jmsTemplate) {
        this.sender = sender;
        this.profileRepository = profileRepository;
        this.userPlanRepository = userPlanRepository;
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Handles the {@code /club} command and any registration step.
     *
     * @param user   the current user
     * @param update the Telegram update
     * @return true if the message was handled by this service
     */
    public boolean handle(@NonNull User user, @NonNull MessageUpdate update) {
        String comando = user.getComando();
        String text = update.getText();

        if ("start".equals(comando) && (COMMAND_CLUB.equals(text) || CALLBACK_CLUB_ENTER.equals(text))) {
            handleClubCommand(user, update);
            return true;
        }

        if (comando != null && comando.startsWith("club_register_")) {
            handleRegistrationState(user, update);
            return true;
        }

        return false;
    }

    private void handleClubCommand(User user, MessageUpdate update) {
        if (update.getUser() == null || update.getUser().isBlank()) {
            sender.send(update.getChatid(),
                    "Necesitás tener un usuario de Telegram configurado para entrar al club de amistad. Por favor, configurá tu usuario en Telegram y volvé a intentar con /club.");
            return;
        }

        Profile profile = profileRepository.findByChatid(update.getChatid());
        if (profile == null) {
            startRegistration(user, update);
            return;
        }

        switch (profile.getStatus()) {
            case STATUS_APPROVED -> sendApprovedStatus(update.getChatid(), profile);
            case STATUS_PENDING -> sender.send(update.getChatid(),
                    "Tu perfil está pendiente de aprobación. Te avisamos cuando sea aprobado.");
            case STATUS_REJECTED -> sendRejectedStatus(update.getChatid(), profile);
            case STATUS_PAUSED -> sendPausedStatus(update.getChatid(), profile);
            default -> startRegistration(user, update);
        }
    }

    private void startRegistration(User user, MessageUpdate update) {
        Profile profile = new Profile();
        profile.setChatid(update.getChatid());
        profile.setContactUsername(update.getUser());
        profile.setCountry(COUNTRY_BOLIVIA);
        profile.setStatus(STATUS_INCOMPLETE);
        profile.setCreatedAt(isoTimestamp());
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_NAME);
        sender.send(update.getChatid(),
                "Para unirte al club de amistad, completá tu perfil. Primero, ¿cómo querés que te llamemos?");
    }

    private void sendApprovedStatus(String chatid, Profile profile) {
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Ver personas", "/ver_personas"),
                        new Button("Editar perfil", "/editar_perfil")),
                Arrays.asList(new Button("Pausar perfil", "/pausar_perfil"),
                        new Button("Volver al inicio", "/start")));
        sender.send(chatid,
                "Tu perfil de @" + profile.getContactUsername() + " está aprobado. Usá los botones para continuar.",
                true, buttons);
    }

    private void sendRejectedStatus(String chatid, Profile profile) {
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Volver a registrarse", CALLBACK_CLUB_ENTER),
                        new Button("Volver al inicio", "/start")));
        sender.send(chatid,
                "Tu perfil fue rechazado. Podés volver a registrarte con los datos correctos.",
                true, buttons);
    }

    private void sendPausedStatus(String chatid, Profile profile) {
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Activar perfil", "/activar_perfil"),
                        new Button("Volver al inicio", "/start")));
        sender.send(chatid,
                "Tu perfil está pausado y no aparece en la búsqueda. Activalo cuando quieras.",
                true, buttons);
    }

    private void handleRegistrationState(User user, MessageUpdate update) {
        String comando = user.getComando();
        switch (comando) {
            case STATE_REGISTER_NAME -> handleName(user, update);
            case STATE_REGISTER_AGE -> handleAge(user, update);
            case STATE_REGISTER_GENDER -> handleGender(user, update);
            case STATE_REGISTER_ORIENTATION -> handleOrientation(user, update);
            case STATE_REGISTER_CITY -> handleCity(user, update);
            case STATE_REGISTER_DESCRIPTION -> handleDescription(user, update);
            case STATE_REGISTER_TASTES -> handleTastes(user, update);
            case STATE_REGISTER_TRAITS -> handleTraits(user, update);
            case STATE_REGISTER_LOOKING_FOR -> handleLookingFor(user, update);
            case STATE_REGISTER_PHOTO -> handlePhoto(user, update);
            case STATE_REGISTER_PREVIEW -> handlePreview(user, update);
            default -> {
                sender.send(update.getChatid(),
                        "Hubo un problema con el registro. Escribí /club para comenzar de nuevo.");
                user.setComando("start");
            }
        }
    }

    private void handleName(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            askForName(update.getChatid());
            return;
        }
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        profile.setName(update.getText().trim());
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_AGE);
        sender.send(update.getChatid(), "¿Cuántos años tenés?");
    }

    private void askForName(String chatid) {
        sender.send(chatid, "Por favor, escribí tu nombre o apodo para el perfil.");
    }

    private void handleAge(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, escribí tu edad en números.");
            return;
        }
        int age;
        try {
            age = Integer.parseInt(update.getText().trim());
        } catch (NumberFormatException e) {
            sender.send(update.getChatid(),
                    "No entendí la edad. Escribí un número, por ejemplo: 25.");
            return;
        }
        if (age < MINIMUM_AGE) {
            sender.send(update.getChatid(),
                    "Debés ser mayor de 18 años para unirte al club. Por favor, ingresá una edad válida.");
            return;
        }
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        profile.setAge(age);
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_GENDER);
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Hombre", CALLBACK_GENDER_MALE),
                        new Button("Mujer", CALLBACK_GENDER_FEMALE),
                        new Button("Otro", CALLBACK_GENDER_OTHER)));
        sender.send(update.getChatid(), "Seleccioná tu género:", true, buttons);
        answerCallback(update);
    }

    private void handleGender(User user, MessageUpdate update) {
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        String gender = mapGenderCallback(update.getText());
        if (gender == null) {
            askForGender(update.getChatid());
            return;
        }
        profile.setGender(gender);
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_ORIENTATION);
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Hetero", CALLBACK_ORIENTATION_HETERO),
                        new Button("Bi", CALLBACK_ORIENTATION_BI)));
        sender.send(update.getChatid(), "Seleccioná tu orientación:", true, buttons);
        answerCallback(update);
    }

    private void askForGender(String chatid) {
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Hombre", CALLBACK_GENDER_MALE),
                        new Button("Mujer", CALLBACK_GENDER_FEMALE),
                        new Button("Otro", CALLBACK_GENDER_OTHER)));
        sender.send(chatid, "Seleccioná una opción de género:", true, buttons);
    }

    private void handleOrientation(User user, MessageUpdate update) {
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        String orientation = mapOrientationCallback(update.getText());
        if (orientation == null) {
            askForOrientation(update.getChatid());
            return;
        }
        profile.setOrientation(orientation);
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        if (!isAllowedCombination(profile.getGender(), profile.getOrientation())) {
            profileRepository.delete(profile);
            user.setComando("start");
            sender.send(update.getChatid(),
                    "Por ahora el club solo acepta los siguientes perfiles: hombre hetero, mujer hetero o mujer bi."
                            + " Si querés, podés volver a intentar con /club.");
            answerCallback(update);
            return;
        }

        user.setComando(STATE_REGISTER_CITY);
        sender.send(update.getChatid(), "¿En qué ciudad de Bolivia estás?");
        answerCallback(update);
    }

    private void askForOrientation(String chatid) {
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Hetero", CALLBACK_ORIENTATION_HETERO),
                        new Button("Bi", CALLBACK_ORIENTATION_BI)));
        sender.send(chatid, "Seleccioná una opción de orientación:", true, buttons);
    }

    private void handleCity(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, escribí tu ciudad.");
            return;
        }
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        profile.setCity(update.getText().trim());
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_DESCRIPTION);
        sender.send(update.getChatid(), "Contanos un poco sobre vos:");
    }

    private void handleDescription(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, escribí una descripción para tu perfil.");
            return;
        }
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        profile.setDescription(update.getText().trim());
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_TASTES);
        sender.send(update.getChatid(), "¿Qué cosas te gustan? (música, hobbies, etc.)");
    }

    private void handleTastes(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, contanos qué cosas te gustan.");
            return;
        }
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        profile.setTastes(update.getText().trim());
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_TRAITS);
        sender.send(update.getChatid(), "¿Cómo describirías tu personalidad?");
    }

    private void handleTraits(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, describí tu personalidad.");
            return;
        }
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        profile.setTraits(update.getText().trim());
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_LOOKING_FOR);
        sender.send(update.getChatid(), "¿Qué estás buscando en el club de amistad?");
    }

    private void handleLookingFor(User user, MessageUpdate update) {
        if (isEmptyText(update)) {
            sender.send(update.getChatid(), "Por favor, contanos qué estás buscando.");
            return;
        }
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        profile.setLookingFor(update.getText().trim());
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_PHOTO);
        sender.send(update.getChatid(),
                "Por último, enviá una foto para tu perfil. Podés usar una selfie o una foto que te represente.");
    }

    private void handlePhoto(User user, MessageUpdate update) {
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        String[] medias = update.getMedias();
        if (medias == null || medias.length == 0 || medias[0] == null || medias[0].isBlank()) {
            sender.send(update.getChatid(),
                    "No recibí una foto. Por favor, enviá una imagen para tu perfil.");
            return;
        }
        profile.setPhotoFileId(medias[0]);
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        user.setComando(STATE_REGISTER_PREVIEW);
        sendPreview(update.getChatid(), profile);
    }

    private void sendPreview(String chatid, Profile profile) {
        String preview = buildProfilePreview(profile);
        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Confirmar", CALLBACK_PREVIEW_OK),
                        new Button("Editar", CALLBACK_PREVIEW_EDIT)));
        sender.send(chatid, preview, true, buttons);
    }

    private void handlePreview(User user, MessageUpdate update) {
        Profile profile = requireIncompleteProfile(update.getChatid());
        if (profile == null) {
            restart(user, update.getChatid());
            return;
        }
        String text = update.getText();
        if (CALLBACK_PREVIEW_OK.equals(text)) {
            confirmRegistration(user, update, profile);
        } else if (CALLBACK_PREVIEW_EDIT.equals(text)) {
            sender.send(update.getChatid(),
                    "La edición de perfil estará disponible pronto. Volviendo al menú principal.");
            user.setComando("start");
        } else {
            sendPreview(update.getChatid(), profile);
        }
        answerCallback(update);
    }

    private void confirmRegistration(User user, MessageUpdate update, Profile profile) {
        profile.setStatus(STATUS_PENDING);
        profile.setUpdatedAt(isoTimestamp());
        profileRepository.save(profile);

        ensureFreePlan(update.getChatid());
        sendModerationMessage(profile);

        List<List<Button>> buttons = Arrays.asList(
                Arrays.asList(new Button("Volver al inicio", "/start")));
        sender.send(update.getChatid(),
                "Tu perfil fue enviado a revisión. Te avisamos cuando sea aprobado.",
                true, buttons);
        user.setComando("start");
    }

    private void ensureFreePlan(String chatid) {
        if (userPlanRepository.existsById(chatid)) {
            return;
        }
        UserPlan plan = new UserPlan();
        plan.setChatid(chatid);
        plan.setPlan(PLAN_FREE);
        plan.setStartedAt(isoTimestamp());
        userPlanRepository.save(plan);
    }

    private void sendModerationMessage(Profile profile) {
        ModerationMessage message = new ModerationMessage();
        message.setType("NEW_PROFILE");
        message.setChatid(profile.getChatid());
        message.setName(profile.getName());
        message.setAge(profile.getAge());
        message.setGender(profile.getGender());
        message.setOrientation(profile.getOrientation());
        message.setCountry(profile.getCountry());
        message.setCity(profile.getCity());
        message.setDescription(profile.getDescription());
        message.setTastes(profile.getTastes());
        message.setTraits(profile.getTraits());
        message.setLookingFor(profile.getLookingFor());
        message.setPhotoFileId(profile.getPhotoFileId());
        message.setContactUsername(profile.getContactUsername());
        jmsTemplate.convertAndSend("queue.moderation", message);
    }

    private Profile requireIncompleteProfile(String chatid) {
        Profile profile = profileRepository.findByChatid(chatid);
        if (profile == null || !STATUS_INCOMPLETE.equals(profile.getStatus())) {
            return null;
        }
        return profile;
    }

    private void restart(User user, String chatid) {
        sender.send(chatid,
                "No encontré tu registro en curso. Escribí /club para comenzar de nuevo.");
        user.setComando("start");
    }

    private String mapGenderCallback(String callback) {
        return switch (callback) {
            case CALLBACK_GENDER_MALE -> GENDER_MALE;
            case CALLBACK_GENDER_FEMALE -> GENDER_FEMALE;
            case CALLBACK_GENDER_OTHER -> GENDER_OTHER;
            default -> null;
        };
    }

    private String mapOrientationCallback(String callback) {
        return switch (callback) {
            case CALLBACK_ORIENTATION_HETERO -> ORIENTATION_HETERO;
            case CALLBACK_ORIENTATION_BI -> ORIENTATION_BI;
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

    private void answerCallback(MessageUpdate update) {
        if (update.getCallbackQueryId() != null && !update.getCallbackQueryId().isBlank()) {
            sender.answerCallbackQuery(update.getCallbackQueryId());
        }
    }

    private String buildProfilePreview(Profile profile) {
        return "Así se verá tu perfil:\n\n"
                + "Nombre: " + profile.getName() + "\n"
                + "Edad: " + profile.getAge() + "\n"
                + "Género: " + translateGender(profile.getGender()) + "\n"
                + "Orientación: " + translateOrientation(profile.getOrientation()) + "\n"
                + "Ciudad: " + profile.getCity() + "\n"
                + "Sobre vos: " + profile.getDescription() + "\n"
                + "Gustos: " + profile.getTastes() + "\n"
                + "Personalidad: " + profile.getTraits() + "\n"
                + "Buscás: " + profile.getLookingFor() + "\n\n"
                + "¿Confirmás que querés enviarlo a revisión?";
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
