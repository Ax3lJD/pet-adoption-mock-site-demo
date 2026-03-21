package petadoption.api.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petadoption.api.pet.Pet;
import petadoption.api.pet.PetRepository;
import petadoption.api.user.User;
import petadoption.api.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher; // Import Matcher
import java.util.regex.Pattern; // Import Pattern
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for managing Notifications.
 * Handles requests related to fetching, creating, replying, and managing read status,
 * including the logic for approving adoptions and handling adopter responses.
 * Uses NotificationDTO for API responses.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationsController {

    private static final Logger log = LoggerFactory.getLogger(NotificationsController.class);

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final NotificationsService notificationsService;

    @Autowired
    public NotificationsController(PetRepository petRepository, UserRepository userRepository, NotificationsService notificationsService) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.notificationsService = notificationsService;
    }

    /**
     * Retrieves unread notifications for a specific user as DTOs, sorted descending by creation date.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsByUser(@PathVariable Long userId) {
        log.info("API Request: Get unread notifications for user ID: {}", userId);
        try {
            List<NotificationDTO> notificationDTOs = notificationsService.getUnreadNotificationsByUserId(userId);
            notificationDTOs.sort(Comparator.comparing(NotificationDTO::getCreatedAt).reversed());
            return ResponseEntity.ok(notificationDTOs);
        } catch (EntityNotFoundException e) {
            log.warn("User not found for notification fetch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        } catch (Exception e) {
            log.error("Error fetching notifications for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Marks a specific notification as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id) {
        log.info("API Request: Mark notification ID: {} as read", id);
        try {
            NotificationDTO dto = notificationsService.markAsRead(id);
            return ResponseEntity.ok(dto);
        } catch (EntityNotFoundException e) {
            log.warn("Failed to mark notification {} as read: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error marking notification {} as read: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An internal error occurred while marking the notification as read."));
        }
    }

    /**
     * Submits a reply to an existing notification.
     */
    @PostMapping("/reply")
    public ResponseEntity<?> replyToNotification(@RequestBody NotificationsReplyRequest request) {
        log.info("API Request: Reply to notification ID: {}", request.getNotificationId());
        if (request.getNotificationId() == null || request.getReply() == null || request.getReply().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request must include 'notificationId' and non-empty 'reply'."));
        }
        try {
            NotificationDTO original = notificationsService.getNotificationById(request.getNotificationId());
            NotificationDTO replyDto = notificationsService.replyToNotification(
                    request.getNotificationId(),
                    request.getReply(),
                    original.getPetId(),
                    original.getAdopterId()
            );return ResponseEntity.ok(replyDto);
        } catch (EntityNotFoundException | IllegalStateException | IllegalArgumentException e) {
            log.warn("Failed to send reply for notification {}: {}", request.getNotificationId(), e.getMessage());
            HttpStatus status = (e instanceof EntityNotFoundException) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error sending reply for notification {}: {}", request.getNotificationId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An internal error occurred while sending the reply."));
        }
    }

    /**
     * Creates an initial adoption request notification (sent from adopter to shelter).
     */
    @PostMapping // POST /api/notifications
    public ResponseEntity<?> createNotification(@RequestBody NotificationsRequest request) {
        log.info("API Request: Create adoption request notification from user {}", request.getUserId());
        try {
            // --- Validation ---
            if (request.getPetId() == null) return ResponseEntity.badRequest().body(Map.of("error","petId is required"));
            if (request.getUserId() == null) return ResponseEntity.badRequest().body(Map.of("error","userId (adopter) is required"));
            // --- End Validation ---

            User adopterUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Adopter user not found: " + request.getUserId()));
            Pet pet = petRepository.findById(request.getPetId())
                    .orElseThrow(() -> new EntityNotFoundException("Pet not found: " + request.getPetId()));
            Long shelterId = pet.getAdoptionCenterId();
            if (shelterId == null) {
                throw new IllegalStateException("Pet (ID: " + request.getPetId() + ") does not have an associated shelter.");
            }

            if (!pet.getAvailable()) {
                log.warn("Attempt to create adoption request for unavailable pet ID: {}", request.getPetId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "This pet is no longer available for adoption."));
            }

            String displayName = request.getDisplayName() != null && !request.getDisplayName().isEmpty()
                    ? request.getDisplayName() : adopterUser.getEmailAddress();
            String notificationText = String.format(
                    "New adoption request from %s (Adopter ID: %d) for pet '%s' (Pet ID: %d)",
                    displayName, adopterUser.getId(), pet.getName(), pet.getId());

            NotificationDTO createdDto = notificationsService.createNotificationForShelter(
                    notificationText, shelterId, adopterUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);

        } catch (EntityNotFoundException | IllegalStateException | IllegalArgumentException e) {
            log.warn("Failed to create notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating notification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An internal error occurred while creating the notification."));
        }
    }

    /**
     * Approves an adoption request. This action marks the pet as adopted, links it to the adopter,
     * marks the original request notification as read, and sends an approval notification to the adopter.
     * Requires notificationId (of the request), petId, adopterId, shelterId.
     */
    // Update the approveAdoption method in NotificationsController.java
    @PostMapping("/approve-adoption")
    @Transactional // Ensure atomicity
    public ResponseEntity<?> approveAdoption(@RequestBody Map<String, Object> request) {
        log.info("API Request: Approve adoption related to original notification ID: {}", request.get("notificationId"));
        try {
            // --- Parameter extraction ---
            Long notificationId = parseLongFromMap(request, "notificationId");
            Long petId = parseLongFromMap(request, "petId");
            Long adopterId = parseLongFromMap(request, "adopterId");
            Long shelterId = parseLongFromMap(request, "shelterId");
            String message = request.getOrDefault("message", "").toString();
            // --- End extraction ---

            // --- Load Entities ---
            User shelterUser = userRepository.findById(shelterId)
                    .orElseThrow(() -> new EntityNotFoundException("Shelter user performing approval not found: " + shelterId));
            User adopterUser = userRepository.findById(adopterId)
                    .orElseThrow(() -> new EntityNotFoundException("Adopter user not found: " + adopterId));
            Pet pet = petRepository.findById(petId)
                    .orElseThrow(() -> new EntityNotFoundException("Pet not found: " + petId));
            // --- End Load Entities ---

            // --- Validation and Authorization ---
            if (!"SHELTER".equals(shelterUser.getUserType())) {
                throw new IllegalStateException("User performing action is not a shelter.");
            }
            if (!shelterId.equals(pet.getAdoptionCenterId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Shelter not authorized for this pet."));
            }
            if (!pet.getAvailable()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "This pet is no longer available for adoption."));
            }
            // --- End Validation ---

            // --- Update Pet Status ---
            pet.setAvailable(false);
            pet.setStatus("Adopted");
            pet.setAdopterId(adopterId); // Link pet to adopter
            petRepository.save(pet);
            log.info("Pet {} marked as adopted by user {}", petId, adopterId);
            // --- End Update Pet Status ---

            // --- Mark original request notification as read ---
            notificationsService.markAsRead(notificationId);

            // --- Create and send confirmation notification TO adopter ---
            String approvalText = String.format(
                    "Your adoption of %s has been approved! %s",
                    pet.getName(), message.isEmpty() ? "" : "\n\nMessage from shelter: " + message);
            NotificationDTO approvalDto = notificationsService.createNotificationForUser(
                    approvalText, adopterId, shelterUser); // FROM shelter
            // --- End Send Confirmation Notification ---

            // --- Create system notification for shelter's records ---
            String systemText = String.format(
                    "System confirmation: Pet %s (ID: %d) has been marked as adopted by %s (ID: %d)",
                    pet.getName(), pet.getId(), adopterUser.getEmailAddress(), adopterId);
            notificationsService.createNotificationForUser(systemText, shelterId, null); // System notification
            // --- End System Notification ---

            // --- Build Success Response ---
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Adoption completed. Pet marked as adopted and confirmation sent to adopter.");
            response.put("approvalNotification", approvalDto);
            return ResponseEntity.ok(response);
            // --- End Success Response ---

        } catch (NumberFormatException | NullPointerException e) {
            log.warn("Invalid ID format in approveAdoption request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing ID format provided in request."));
        } catch (EntityNotFoundException | IllegalStateException | IllegalArgumentException e) {
            log.warn("Failed to approve adoption: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error approving adoption: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An internal error occurred during adoption approval."));
        }
    }

    @PostMapping("/adopter-response")
    @Transactional // Ensure atomicity
    public ResponseEntity<?> handleAdopterResponse(@RequestBody Map<String, Object> request) {
        log.info("API Request: Handle adopter response for approval notification ID: {}", request.get("notificationId"));
        try {
            // --- Parameter extraction ---
            Long notificationId = parseLongFromMap(request, "notificationId"); // ID of the approval notification sent TO adopter
            Boolean accepted = parseBooleanFromMap(request, "accepted");
            // --- End extraction ---

            // --- Mark notification as read ---
            notificationsService.markAsRead(notificationId);
            log.info("Marked adoption notification {} as read, accepted: {}", notificationId, accepted);

            // --- Build Success Response ---
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Adoption response processed successfully.");
            response.put("accepted", accepted);
            return ResponseEntity.ok(response);
            // --- End Success Response ---

        } catch (NumberFormatException | NullPointerException e) {
            log.warn("Invalid format in handleAdopterResponse request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing ID/accepted flag in request."));
        } catch (EntityNotFoundException | IllegalStateException | IllegalArgumentException e) {
            log.warn("Failed to handle adopter response: {}", e.getMessage());
            // Return 400 or 404 based on the error
            HttpStatus status = (e instanceof EntityNotFoundException) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error handling adopter response: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An internal error occurred while handling the adopter response."));
        }
    }

    private Long parseLongFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) throw new NullPointerException("Missing required field in request: " + key);
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException e) { throw new NumberFormatException("Invalid format for field '" + key + "': " + value); }
    }

    private Boolean parseBooleanFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) throw new NullPointerException("Missing required field in request: " + key);
        if (value instanceof Boolean) return (Boolean) value;
        String stringValue = value.toString().toLowerCase();
        if ("true".equals(stringValue)) return true;
        if ("false".equals(stringValue)) return false;
        throw new IllegalArgumentException("Invalid boolean format for field '" + key + "': " + value);
    }
}
