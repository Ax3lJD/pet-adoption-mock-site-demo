package petadoption.api.endpoint;

import petadoption.api.pet.Pet;
import petadoption.api.recommendation.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationEndpoint {

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Pet>> getRecommendations(
            @PathVariable Long userId,
            @RequestParam(value = "exclude", required = false) List<Long> excludeIds
    ) {
        List<Pet> recommendations =
                recommendationService.getRecommendationsForUser(userId, excludeIds);
        return ResponseEntity.ok(recommendations);
    }
}

