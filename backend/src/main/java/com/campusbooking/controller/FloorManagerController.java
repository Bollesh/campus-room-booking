package com.campusbooking.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusbooking.model.FloorManager;
import com.campusbooking.service.FloorManagerService;
import com.campusbooking.service.UserService;

@RestController
@RequestMapping("/api/floormanagers") 
public class FloorManagerController {

    private final FloorManagerService floorManagerService;
    private final UserService userService; 

    @Autowired
    public FloorManagerController(FloorManagerService floorManagerService, UserService userService) {
        this.floorManagerService = floorManagerService;
        this.userService = userService;
    }

    @GetMapping
    public List<FloorManager> getAllFloorManagers() {
        return floorManagerService.getAllFloorManagers();
    }

    @GetMapping("/{email}")
    public ResponseEntity<FloorManager> getFloorManagerByEmail(@PathVariable String email) {
        Optional<FloorManager> managerOptional = floorManagerService.getFloorManagerByEmail(email);
        return managerOptional.map(ResponseEntity::ok)
                               .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createFloorManager(@RequestBody FloorManager floorManager) {

        if (floorManager.getEmail() == null || floorManager.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }

        if (userService.existsByEmail(floorManager.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("User with email " + floorManager.getEmail() + " already exists.");
        }

        try {

            FloorManager createdManager = floorManagerService.createFloorManager(floorManager);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdManager);
        } catch (Exception e) {

             System.err.println("Error creating floor manager: " + e.getMessage()); 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("An error occurred while creating the floor manager.");
        }
    }

    @PutMapping("/{email}")
    public ResponseEntity<?> updateFloorManager(@PathVariable String email, @RequestBody FloorManager managerDetails) {
        Optional<FloorManager> existingManagerOptional = floorManagerService.getFloorManagerByEmail(email);

        if (existingManagerOptional.isEmpty()) {
            return ResponseEntity.notFound().build(); 
        }

        FloorManager existingManager = existingManagerOptional.get();

        existingManager.setName(managerDetails.getName());
        existingManager.setPhone(managerDetails.getPhone());

        try {

            FloorManager updatedManager = floorManagerService.createFloorManager(existingManager);
            return ResponseEntity.ok(updatedManager);
        } catch (Exception e) {

             System.err.println("Error updating floor manager: " + e.getMessage()); 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("An error occurred while updating the floor manager.");
        }
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteFloorManager(@PathVariable String email) {

        if (floorManagerService.getFloorManagerByEmail(email).isEmpty()) {
            return ResponseEntity.notFound().build(); 
        }
        try {

            floorManagerService.deleteFloorManager(email);
            return ResponseEntity.noContent().build(); 
        } catch (Exception e) {

             System.err.println("Error deleting floor manager: " + e.getMessage()); 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}