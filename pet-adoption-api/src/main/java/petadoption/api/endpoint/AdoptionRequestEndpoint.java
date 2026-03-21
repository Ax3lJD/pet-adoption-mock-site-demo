package petadoption.api.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petadoption.api.notifications.NotificationDTO;
import petadoption.api.notifications.NotificationsService;
import petadoption.api.user.User;
import petadoption.api.user.UserService;
import petadoption.api.pet.Pet;
import petadoption.api.pet.PetService;

import java.util.Map;

@RestController
@RequestMapping("/api/adoption-requests")
public class AdoptionRequestEndpoint {

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    @Autowired
    private NotificationsService notificationsService;

    @PostMapping
    public ResponseEntity<?> createAdoptionRequest(@RequestBody Map<String, Object> request) {
        try {
            Long userId = request.containsKey("userId")
                    ? Long.parseLong(request.get("userId").toString())
                    : null;
            Long petId = request.containsKey("petId")
                    ? Long.parseLong(request.get("petId").toString())
                    : null;
            Long shelterId = request.containsKey("adoptionCenterId")
                    ? Long.parseLong(request.get("adoptionCenterId").toString())
                    : request.containsKey("shelterId")
                    ? Long.parseLong(request.get("shelterId").toString())
                    : null;
            String additionalNotes = request.containsKey("additionalNotes")
                    ? request.get("additionalNotes").toString()
                    : request.containsKey("notes")
                    ? request.get("notes").toString()
                    : "";

            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }
            if (petId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "petId is required"));
            }
            if (shelterId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "adoptionCenterId or shelterId is required"));
            }

            User adopter = userService.findUser(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            Pet pet = petService.findPetById(petId)
                    .orElseThrow(() -> new RuntimeException("Pet not found: " + petId));

            String userName = adopter.getFirstName() != null
                    ? adopter.getFirstName() + (adopter.getLastName() != null ? " " + adopter.getLastName() : "")
                    : adopter.getEmailAddress();
            String notificationText = String.format(
                    "New adoption request from %s (Adopter ID: %d) for pet '%s' (Pet ID: %d)",
                    userName, adopter.getId(), pet.getName(), pet.getId()
            );
            if (!additionalNotes.isBlank()) {
                notificationText += "\nNotes: " + additionalNotes;
            }

            NotificationDTO dto = notificationsService.createAdoptionRequestNotification(
                    notificationText,
                    shelterId,
                    adopter,
                    petId
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ID format: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to submit adoption request: " + e.getMessage()
            ));
        }
    }
}
