package com.campusbooking.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // DTO for POST request
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusbooking.model.ClubMembership;
import com.campusbooking.service.ClubMembershipService;
import com.campusbooking.service.ClubService;
import com.campusbooking.service.StudentService;
import com.campusbooking.types.ClubMembershipRequest;

@RestController
@RequestMapping("/api/memberships") 
public class ClubMembershipController {

    private final ClubMembershipService clubMembershipService;
    private final StudentService studentService;
    private final ClubService clubService;

    @Autowired
    public ClubMembershipController(ClubMembershipService clubMembershipService,
                                    StudentService studentService,
                                    ClubService clubService) {
        this.clubMembershipService = clubMembershipService;
        this.studentService = studentService;
        this.clubService = clubService;
    }

    @PostMapping
    public ResponseEntity<?> addMembership(@RequestBody ClubMembershipRequest request) {
        // 1. Basic Validation
        if (request.getStuEmail() == null || request.getStuEmail().trim().isEmpty() ||
            request.getClubName() == null || request.getClubName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Both stuEmail and clubName are required.\"}");
        }

        // 2. check if student/club exist
        if (studentService.getStudentByEmail(request.getStuEmail()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("{\"error\": \"Student with email '" + request.getStuEmail() + "' not found.\"}");
        }
        if (clubService.getClubByName(request.getClubName()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("{\"error\": \"Club with name '" + request.getClubName() + "' not found.\"}");
        }

        // 3. Attempt to add via service
        try {
            ClubMembership createdMembership = clubMembershipService.addMembership(request.getStuEmail(), request.getClubName());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMembership); // 201 Created
        } catch (NoSuchElementException e) { // Should be caught by early checks if done
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (IllegalStateException e) { // Membership already exists
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\": \"" + e.getMessage() + "\"}"); // 409 Conflict
        } catch (Exception e) {
            System.err.println("Error adding membership: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while adding membership.\"}");
        }
    }

    @DeleteMapping("/student/{studentEmail}/club/{clubName}")
    public ResponseEntity<?> removeMembership(@PathVariable String studentEmail, @PathVariable String clubName) {
        // 1. Basic Validation
        if (studentEmail == null || studentEmail.trim().isEmpty() ||
            clubName == null || clubName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Both studentEmail and clubName path variables are required.\"}");
        }

        // 2. Attempt removal via service
        try {
            boolean deleted = clubMembershipService.removeMembership(studentEmail, clubName);
            if (deleted) {
                return ResponseEntity.noContent().build(); // 204 No Content
            } else {
                // If service returns false, the membership didn't exist
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
        } catch (Exception e) {
            System.err.println("Error removing membership for student '" + studentEmail + "' from club '" + clubName + "': " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while removing membership.\"}");
        }
    }

    @GetMapping("/student/{studentEmail}/club/{clubName}")
    public ResponseEntity<ClubMembership> getMembership(@PathVariable String studentEmail, @PathVariable String clubName) {
        if (studentEmail == null || studentEmail.trim().isEmpty() ||
            clubName == null || clubName.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
        }
        return clubMembershipService.getMembershipById(studentEmail, clubName)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentEmail}")
    public ResponseEntity<?> getMembershipsForStudent(@PathVariable String studentEmail) {
        if (studentEmail == null || studentEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"studentEmail path variable is required.\"}");
        }
        // Check if student exists
        if (studentService.getStudentByEmail(studentEmail).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("{\"error\": \"Student with email '" + studentEmail + "' not found.\"}");
        }
        List<ClubMembership> memberships = clubMembershipService.getMembershipsByStudent(studentEmail);
        return ResponseEntity.ok(memberships); // Returns list of ClubMembership objects
    }

    @GetMapping("/club/{clubName}")
    public ResponseEntity<?> getMembershipsForClub(@PathVariable String clubName) {
        if (clubName == null || clubName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"clubName path variable is required.\"}");
        }
        // Check if club exists
        if (clubService.getClubByName(clubName).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("{\"error\": \"Club with name '" + clubName + "' not found.\"}");
        }
        List<ClubMembership> memberships = clubMembershipService.getMembershipsByClub(clubName);
        return ResponseEntity.ok(memberships); // Returns list of ClubMembership objects
    }

     @GetMapping
     public List<ClubMembership> getAllMemberships() {
        return clubMembershipService.getAllMemberships();
     }
}
