package petadoption.api.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import petadoption.api.pet.Pet;
import petadoption.api.pet.PetService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets")
public class PetEndpoint {

    private static final Logger log = LoggerFactory.getLogger(PetEndpoint.class);
    private final PetService petService;

    @Autowired
    public PetEndpoint(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public ResponseEntity<Page<Pet>> getAvailablePets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city) {
        log.info("API Request: getAvailablePets page={}, size={}, state={}, city={}",
                page, size, state, city);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Pet> pets;
        if ((state != null && !state.isEmpty()) || (city != null && !city.isEmpty())) {
            pets = petService.getAvailablePetsByLocation(state, city, pageable);
        } else {
            pets = petService.getAllAvailablePets(pageable);
        }
        return ResponseEntity.ok(pets);
    }


    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addPet(@ModelAttribute Pet pet,
                                    @RequestPart(value = "image", required = false) MultipartFile image) {
        log.info("API Request: addPet name='{}', shelterId={}", pet.getName(), pet.getAdoptionCenterId());
        try {
            if (image != null && !image.isEmpty()) {
                String storedPath = petService.storeImage(image);
                pet.setImageUrl(storedPath);
                log.info("Image stored at {}", storedPath);
            } else {
                log.info("No image uploaded for pet '{}'", pet.getName());
                pet.setImageUrl(null);
            }
            if (pet.getName() == null || pet.getName().trim().isEmpty() || pet.getAdoptionCenterId() == null) {
                log.warn("Validation failed: missing name or shelterId");
                return ResponseEntity.badRequest().body(Map.of("error", "Pet name and shelter ID are required."));
            }
            Pet saved = petService.addPet(pet);
            log.info("Pet created with id={}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IOException e) {
            log.error("Error storing image for pet '{}': {}", pet.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process uploaded image."));
        } catch (Exception e) {
            log.error("Error adding pet '{}': {}", pet.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add pet: " + e.getMessage()));
        }
    }

    @GetMapping("/import-csv")
    public ResponseEntity<?> importCSV() {
        log.info("API Request: import-csv");
        try {
            List<Pet> imported = petService.importPetsFromCSV();
            log.info("Imported {} pets from CSV", imported.size());
            return ResponseEntity.ok(imported);
        } catch (IOException e) {
            log.error("IOException during CSV import: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error importing CSV file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during CSV import: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error during CSV import."));
        }
    }

    @PostMapping("/import-csv")
    public ResponseEntity<?> importCSVData(@RequestBody String csvData) {
        log.info("API Request: import-csv (data payload)");
        if (csvData == null || csvData.trim().isEmpty()) {
            log.warn("Empty CSV data payload");
            return ResponseEntity.badRequest().body(Map.of("error", "CSV data cannot be empty."));
        }
        try {
            List<Pet> imported = petService.importPetsFromCSVData(csvData);
            log.info("Imported {} pets from data", imported.size());
            return ResponseEntity.ok(imported);
        } catch (IOException e) {
            log.error("Error parsing CSV data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error parsing CSV data: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during CSV data import: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error during CSV data import."));
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllPets() {
        log.warn("API Request: deleteAllPets");
        try {
            petService.deleteAllPets();
            log.info("All pets deleted");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting all pets: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/adopter/{userId}")
    public ResponseEntity<List<Pet>> getAdoptedPetsByUser(@PathVariable Long userId) {
        log.info("API Request: getAdoptedPetsByUser userId={}", userId);
        try {
            List<Pet> adopted = petService.getPetsByAdopter(userId);
            return ResponseEntity.ok(adopted);
        } catch (Exception e) {
            log.error("Error fetching adopted pets for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/shelter/{shelterId}")
    public ResponseEntity<List<Pet>> getPetsByShelter(@PathVariable Long shelterId) {
        log.info("API Request: getPetsByShelter shelterId={}", shelterId);
        try {
            List<Pet> shelterPets = petService.getPetsByShelter(shelterId);
            return ResponseEntity.ok(shelterPets);
        } catch (Exception e) {
            log.error("Error fetching pets for shelter {}: {}", shelterId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{petId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePet(@PathVariable Long petId) {
        petService.deletePetById(petId);
        return ResponseEntity.noContent().build();
    }


}
