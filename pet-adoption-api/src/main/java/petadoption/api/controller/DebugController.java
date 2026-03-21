package petadoption.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import petadoption.api.repository.EventRepository;
import petadoption.api.user.UserRepository;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @GetMapping("/check")
    public String debugCheck() {
        return "Debug endpoint working! User 10 exists: " + userRepository.existsById(10L);
    }
}