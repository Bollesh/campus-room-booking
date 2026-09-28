package com.campusbooking.controller;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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

import com.campusbooking.model.StudentCouncil;
import com.campusbooking.service.StudentCouncilService;
import com.campusbooking.service.UserService;

@RestController
@RequestMapping("/api/studentcouncil") 
public class StudentCouncilController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    private final StudentCouncilService studentCouncilService;
    private final UserService userService; // Check if user already exists in Users table

    @Autowired
    public StudentCouncilController(StudentCouncilService studentCouncilService, UserService userService) {
        this.studentCouncilService = studentCouncilService;
        this.userService = userService;
    }

    // Helper for email validation
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    @GetMapping
    public List<StudentCouncil> getAllCouncilMembers() {
        return studentCouncilService.getAllCouncilMembers();
    }

    @GetMapping("/{email}")
    public ResponseEntity<StudentCouncil> getCouncilMemberByEmail(@PathVariable String email) {
        if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
            return ResponseEntity.badRequest().build(); 
        }
        return studentCouncilService.getCouncilMemberByEmail(email)
                .map(ResponseEntity::ok) // 200 OK if found
                .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found otherwise
    }

    @PostMapping
    public ResponseEntity<?> createCouncilMember(@RequestBody StudentCouncil member) {
        // 1. Validate required fields (basic check, service layer does more thorough)
        if (member.getEmail() == null || member.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Email is required.\"}");
        }
        if (!isValidEmail(member.getEmail())) {
             return ResponseEntity.badRequest().body("{\"error\": \"Invalid email format.\"}");
        }
        if (member.getName() == null || member.getName().trim().isEmpty()) {
             return ResponseEntity.badRequest().body("{\"error\": \"Name is required.\"}");
        }
        if (member.getPhone() == null) {
             return ResponseEntity.badRequest().body("{\"error\": \"Phone number is required.\"}");
        }
        if (member.getRegno() == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Registration number (regno) is required.\"}");
        }
        if (member.getPosition() == null || member.getPosition().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Position is required for Student Council members.\"}");
        }

        // 2. Check if a user with this email already exists in the base Users table
        if (userService.existsByEmail(member.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("{\"error\": \"User with email '" + member.getEmail() + "' already exists.\"}"); // 409 Conflict
        }

        // 3. Attempt to save via service (which includes its own validation)
        try {
            StudentCouncil createdMember = studentCouncilService.saveCouncilMember(member);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMember); // 201 Created
        } catch (IllegalArgumentException e) { // Catch validation errors from service
             return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}"); // 400 Bad Request
        } catch (Exception e) {
            System.err.println("Error creating student council member: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while creating the member.\"}"); // 500 Internal Server Error
        }
    }

    @PutMapping("/{email}")
    public ResponseEntity<?> updateCouncilMember(@PathVariable String email, @RequestBody StudentCouncil memberDetails) {
        // 1. Validate path variable
        if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
            return ResponseEntity.badRequest().body("{\"error\": \"Valid email required in path.\"}");
        }

        // 2. Find the existing member
        Optional<StudentCouncil> existingMemberOptional = studentCouncilService.getCouncilMemberByEmail(email);
        if (existingMemberOptional.isEmpty()) {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
        StudentCouncil existingMember = existingMemberOptional.get();

        // 3. Validate request body fields
        // Allow nulls for fields that might not be updated, but enforce for required ones.
        if (memberDetails.getName() == null || memberDetails.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Name cannot be empty.\"}");
        }
         if (memberDetails.getPhone() == null) {
             return ResponseEntity.badRequest().body("{\"error\": \"Phone number cannot be null.\"}");
        }
        if (memberDetails.getRegno() == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Registration number (regno) cannot be null.\"}");
        }
        if (memberDetails.getPosition() == null || memberDetails.getPosition().trim().isEmpty()) {
             return ResponseEntity.badRequest().body("{\"error\": \"Position cannot be null or empty.\"}");
        }


        // 4. Update the existing entity's fields
        // Email (PK) should not be changed here.
        existingMember.setName(memberDetails.getName());
        existingMember.setPhone(memberDetails.getPhone());
        existingMember.setRegno(memberDetails.getRegno());
        existingMember.setPosition(memberDetails.getPosition());

        // 5. Attempt to save the updated entity (Service will re-validate)
        try {
            StudentCouncil updatedMember = studentCouncilService.saveCouncilMember(existingMember);
            return ResponseEntity.ok(updatedMember); // 200 OK
        } catch (IllegalArgumentException e) { // Catch validation errors from service
             return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}"); // 400 Bad Request
        } catch (Exception e) {
            System.err.println("Error updating student council member '" + email + "': " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while updating the member.\"}"); // 500 Internal Server Error
        }
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<?> deleteCouncilMember(@PathVariable String email) {
        // 1. Validate path variable
        if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
            return ResponseEntity.badRequest().body("{\"error\": \"Valid email required in path.\"}");
        }

        // 2. Check if member exists
        if (!studentCouncilService.existsByEmail(email)) {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }

        // 3. Attempt deletion via service
        try {
            studentCouncilService.deleteCouncilMember(email);
            return ResponseEntity.noContent().build(); // 204 No Content on success
        } catch (Exception e) { // Catch potential constraint violations or other errors
            System.err.println("Error deleting student council member '" + email + "': " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while deleting the member.\"}"); // 500 Internal Server Error
        }
    }
}
