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

import com.campusbooking.model.Club;
import com.campusbooking.service.ClubService;
import com.campusbooking.service.ProfessorService;
import com.campusbooking.service.StudentService;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    private final ClubService clubService;
    private final ProfessorService professorService; 
    private final StudentService studentService;

    @Autowired
    public ClubController(ClubService clubService, ProfessorService professorService, StudentService studentService) {
        this.clubService = clubService;
        this.professorService = professorService;
        this.studentService = studentService;
    }

    // Helper for email validation
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    @GetMapping
    public List<Club> getAllClubs() {
        return clubService.getAllClubs();
    }

    @GetMapping("/{name}")
    public ResponseEntity<Club> getClubByName(@PathVariable String name) {
        if (name == null || name.trim().isEmpty()) {
             return ResponseEntity.badRequest().build();
        }
        return clubService.getClubByName(name)
                           .map(ResponseEntity::ok)
                           .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createClub(@RequestBody Club club) {
        // 1. Validate required fields
        if (club.getName() == null || club.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Club name is required.\"}");
        }
        if (club.getPocStudentEmail() == null || club.getPocStudentEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Club Point of Contact (POC) student email is required.\"}");
        }
        if (club.getFacultyHeadEmail() == null || club.getFacultyHeadEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Faculty head email is required.\"}");
        }

        // 2. Validate email formats
        if (!isValidEmail(club.getPocStudentEmail())) {
             return ResponseEntity.badRequest().body("{\"error\": \"Invalid POC student email format.\"}");
        }
         if (!isValidEmail(club.getFacultyHeadEmail())) {
             return ResponseEntity.badRequest().body("{\"error\": \"Invalid faculty head email format.\"}");
        }

        // 3. Check if club name (ID) already exists
        if (clubService.existsByName(club.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("{\"error\": \"Club with name '" + club.getName() + "' already exists.\"}");
        }

        // 4. Check if emails correspond to existing users (delegate to service or check here)
        // Service layer validation is generally preferred, but explicit checks here are also possible
        if (professorService.getProfessorByEmail(club.getFacultyHeadEmail()).isEmpty()) {
             return ResponseEntity.badRequest()
                                  .body("{\"error\": \"Faculty head email '" + club.getFacultyHeadEmail() + "' does not belong to a registered professor.\"}");
        }
        if (studentService.getStudentByEmail(club.getPocStudentEmail()).isEmpty()) {
             return ResponseEntity.badRequest()
                                  .body("{\"error\": \"POC email '" + club.getPocStudentEmail() + "' does not belong to a registered student.\"}");
        }

        // 5. Attempt to save (Service layer will re-validate existence)
        try {
            Club createdClub = clubService.saveClub(club); // Use saveClub
            return ResponseEntity.status(HttpStatus.CREATED).body(createdClub);
        } catch (IllegalArgumentException e) { // Catch validation errors from service
             return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            System.err.println("Error creating club: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while creating the club.\"}");
        }
    }

    @PutMapping("/{name}")
    public ResponseEntity<?> updateClub(@PathVariable String name, @RequestBody Club clubDetails) {
        // 1. Validate path variable
         if (name == null || name.trim().isEmpty()) {
             return ResponseEntity.badRequest().body("{\"error\": \"Club name in path cannot be empty.\"}");
         }

        // 2. Find the existing club
        Optional<Club> existingClubOptional = clubService.getClubByName(name);
        if (existingClubOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Club existingClub = existingClubOptional.get();

        // 3. Validate request body fields
        if (clubDetails.getPocStudentEmail() == null || clubDetails.getPocStudentEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"POC student email is required for update.\"}");
        }
        if (clubDetails.getFacultyHeadEmail() == null || clubDetails.getFacultyHeadEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Faculty head email is required for update.\"}");
        }

         // 4. Validate email formats in request body
         if (!isValidEmail(clubDetails.getPocStudentEmail())) {
             return ResponseEntity.badRequest().body("{\"error\": \"Invalid new POC student email format.\"}");
         }
          if (!isValidEmail(clubDetails.getFacultyHeadEmail())) {
             return ResponseEntity.badRequest().body("{\"error\": \"Invalid new faculty head email format.\"}");
         }

        // 5. Check if the new emails exist (delegate to service or check here)
        if (professorService.getProfessorByEmail(clubDetails.getFacultyHeadEmail()).isEmpty()) {
             return ResponseEntity.badRequest()
                                  .body("{\"error\": \"New faculty head email '" + clubDetails.getFacultyHeadEmail() + "' does not belong to a registered professor.\"}");
        }
         if (studentService.getStudentByEmail(clubDetails.getPocStudentEmail()).isEmpty()) {
             return ResponseEntity.badRequest()
                                  .body("{\"error\": \"New POC email '" + clubDetails.getPocStudentEmail() + "' does not belong to a registered student.\"}");
        }

        // 6. Update the existing entity's fields
        existingClub.setPocStudentEmail(clubDetails.getPocStudentEmail());
        existingClub.setFacultyHeadEmail(clubDetails.getFacultyHeadEmail());

        // 7. Attempt to save the updated entity (Service will re-validate)
        try {
            Club updatedClub = clubService.saveClub(existingClub); // Use saveClub
            return ResponseEntity.ok(updatedClub);
        } catch (IllegalArgumentException e) { // Catch validation errors from service
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            System.err.println("Error updating club '" + name + "': " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while updating the club.\"}");
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> deleteClub(@PathVariable String name) {
        if (name == null || name.trim().isEmpty()) {
             return ResponseEntity.badRequest().body("{\"error\": \"Club name in path cannot be empty.\"}");
         }
        if (!clubService.existsByName(name)) {
            return ResponseEntity.notFound().build();
        }
        try {
            clubService.deleteClub(name);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            System.err.println("Error deleting club '" + name + "': " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while deleting the club.\"}");
        }
    }
}
