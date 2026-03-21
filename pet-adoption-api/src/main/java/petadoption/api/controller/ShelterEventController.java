// File: java/petadoption/api/controller/ShelterEventController.java
package petadoption.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petadoption.api.events.Events;
import petadoption.api.service.EventService;
import petadoption.api.user.UserRepository;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.util.List;
import petadoption.api.repository.EventRepository;
import petadoption.api.user.UserRepository;

@RestController
@RequestMapping("/api/shelter/events")
public class ShelterEventController {

    private static final Logger logger = LoggerFactory.getLogger(ShelterEventController.class);

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @PutMapping("/{id}")
    public ResponseEntity<Events> updateEventForShelter(@PathVariable Long id, @RequestBody Events eventDetails, @RequestParam Long userId) {
        Events updatedEvent = eventService.updateEventForShelter(id, eventDetails, userId);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEventForShelter(@PathVariable Long id, @RequestParam Long userId) {
        eventService.deleteEventForShelter(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Events>> getEventsForShelter(@RequestParam Long userId) {
        System.out.println("GET /api/shelter/events requested for userId: " + userId);

        try {
            List<Events> events = eventService.getSafeEventsForShelter(userId);
            System.out.println("Successfully found " + events.size() + " events for shelter: " + userId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            System.out.println("Error in shelter events controller: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<String> getDebugInfo() {
        StringBuilder debug = new StringBuilder();
        debug.append("Debug Information:\n");

        try {
            boolean userExists = userRepository.existsById(10L);
            debug.append("User ID 10 exists: ").append(userExists).append("\n");

            long eventCount = eventRepository.count();
            debug.append("Total events in database: ").append(eventCount).append("\n");

            try {
                List<Events> events = eventService.getSafeEventsForShelter(10L);
                debug.append("Events found for user 10: ").append(events.size()).append("\n");

                int count = 0;
                for (Events event : events) {
                    if (count++ >= 3) break; // Only show up to 3 events
                    debug.append("Event ID: ").append(event.getId())
                            .append(", Name: ").append(event.getName())
                            .append(", CreatedBy: ").append(event.getCreatedBy() != null ?
                                    event.getCreatedBy().getId() : "null")
                            .append("\n");
                }
            } catch (Exception e) {
                debug.append("Error getting events: ").append(e.getMessage()).append("\n");
            }

            File logFile = new File("app-error.log");
            debug.append("Log file exists: ").append(logFile.exists()).append("\n");
            debug.append("Log file path: ").append(logFile.getAbsolutePath()).append("\n");

            return ResponseEntity.ok(debug.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("Error generating debug info: " + e.getMessage());
        }
    }
}