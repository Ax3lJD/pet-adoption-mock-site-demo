package petadoption.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petadoption.api.events.Events;
import petadoption.api.service.EventService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping
    public ResponseEntity<Events> createEvent(@RequestBody Events event, @RequestParam Long userId) {
        Events createdEvent = eventService.createEvent(event, userId);
        return ResponseEntity.status(201).body(createdEvent);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Events> getEventById(@PathVariable Long id) {
        Optional<Events> event = eventService.getEventById(id);
        return event.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(404).body(null));
    }

    @GetMapping
    public ResponseEntity<List<Events>> getAllEvents() {
        List<Events> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Events> updateEvent(@PathVariable Long id, @RequestBody Events eventDetails) {
        Events updatedEvent = eventService.updateEvent(id, eventDetails);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllEvents() {
        eventService.deleteAllEvents();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/attendees")
    public ResponseEntity<Events> addAttendee(@PathVariable Long eventId, @RequestParam Long userId) {
        Events updatedEvent = eventService.addAttendee(eventId, userId);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{eventId}/attendees")
    public ResponseEntity<Events> removeAttendee(@PathVariable Long eventId, @RequestParam Long userId) {
        Events updatedEvent = eventService.removeAttendee(eventId, userId);
        return ResponseEntity.ok(updatedEvent);
    }
}