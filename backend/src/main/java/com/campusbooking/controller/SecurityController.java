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

import com.campusbooking.model.Security;
import com.campusbooking.service.SecurityService;
import com.campusbooking.service.UserService;

@RestController
@RequestMapping("/api/security") 
public class SecurityController {

    private final SecurityService securityService;
    private final UserService userService; 

    @Autowired
    public SecurityController(SecurityService securityService, UserService userService) {
        this.securityService = securityService;
        this.userService = userService;
    }

    @GetMapping
    public List<Security> getAllSecurity() {
        return securityService.getAllSecurity();
    }

    @GetMapping("/{email}")
    public ResponseEntity<Security> getSecurityByEmail(@PathVariable String email) {
        Optional<Security> securityOptional = securityService.getSecurityByEmail(email);
        return securityOptional.map(ResponseEntity::ok)
                               .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createSecurity(@RequestBody Security security) {

        if (security.getEmail() == null || security.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }

        if (userService.existsByEmail(security.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("User with email " + security.getEmail() + " already exists.");
        }

        try {

            Security createdSecurity = securityService.createSecurity(security);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSecurity);
        } catch (Exception e) {

             System.err.println("Error creating security personnel: " + e.getMessage()); 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("An error occurred while creating the security personnel.");
        }
    }

    @PutMapping("/{email}")
    public ResponseEntity<?> updateSecurity(@PathVariable String email, @RequestBody Security securityDetails) {
        Optional<Security> existingSecurityOptional = securityService.getSecurityByEmail(email);

        if (existingSecurityOptional.isEmpty()) {
            return ResponseEntity.notFound().build(); 
        }

        Security existingSecurity = existingSecurityOptional.get();

        existingSecurity.setName(securityDetails.getName());
        existingSecurity.setPhone(securityDetails.getPhone());

        try {

            Security updatedSecurity = securityService.createSecurity(existingSecurity);
            return ResponseEntity.ok(updatedSecurity);
        } catch (Exception e) {

             System.err.println("Error updating security personnel: " + e.getMessage()); 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("An error occurred while updating the security personnel.");
        }
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteSecurity(@PathVariable String email) {

        if (securityService.getSecurityByEmail(email).isEmpty()) {
            return ResponseEntity.notFound().build(); 
        }
        try {

            securityService.deleteSecurity(email);
            return ResponseEntity.noContent().build(); 
        } catch (Exception e) {

             System.err.println("Error deleting security personnel: " + e.getMessage()); 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}