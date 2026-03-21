// File: java/petadoption/api/controller/AdopterEventController.java
package petadoption.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petadoption.api.events.Events;
import petadoption.api.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/api/adopter/events")
public class AdopterEventController {

    @Autowired
    private EventService eventService;

    @GetMapping
    public ResponseEntity<List<Events>> getEventsForAdopter(@RequestParam Long userId) {
        List<Events> events = eventService.getEventsForAdopter(userId);
        return ResponseEntity.ok(events);
    }
}