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

import com.campusbooking.model.Professor;
import com.campusbooking.service.ProfessorService;
import com.campusbooking.service.UserService;

@RestController
@RequestMapping("/api/professors") 
public class ProfessorController {

    private final ProfessorService professorService;
    private final UserService userService; 

    @Autowired
    public ProfessorController(ProfessorService professorService, UserService userService) {
        this.professorService = professorService;
        this.userService = userService;
    }

    @GetMapping
    public List<Professor> getAllProfessors() {
        return professorService.getAllProfessors();
    }

    @GetMapping("/{email}")
    public ResponseEntity<Professor> getProfessorByEmail(@PathVariable String email) {
        Optional<Professor> professorOptional = professorService.getProfessorByEmail(email);
        return professorOptional.map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createProfessor(@RequestBody Professor professor) {

         if (professor.getIsCultural() == null) {
              return ResponseEntity.badRequest().body("'isCultural' field is required for professors.");
         }

        if (userService.existsByEmail(professor.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("User with email " + professor.getEmail() + " already exists.");
        }

        try {

            Professor createdProfessor = professorService.createProfessor(professor);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProfessor);
        } catch (Exception e) {

             System.err.println("Error creating professor: " + e.getMessage()); 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("An error occurred while creating the professor.");
        }
    }

    @PutMapping("/{email}")
    public ResponseEntity<?> updateProfessor(@PathVariable String email, @RequestBody Professor professorDetails) {
        Optional<Professor> existingProfessorOptional = professorService.getProfessorByEmail(email);

        if (existingProfessorOptional.isEmpty()) {
            return ResponseEntity.notFound().build(); 
        }

        Professor existingProfessor = existingProfessorOptional.get();

         if (professorDetails.getIsCultural() == null) {
             return ResponseEntity.badRequest().body("'isCultural' field cannot be null for professors.");
         }

        existingProfessor.setName(professorDetails.getName()); 
        existingProfessor.setPhone(professorDetails.getPhone()); 
        existingProfessor.setIsCultural(professorDetails.getIsCultural()); 

        try {

            Professor updatedProfessor = professorService.createProfessor(existingProfessor);
            return ResponseEntity.ok(updatedProfessor);
        } catch (Exception e) {

             System.err.println("Error updating professor: " + e.getMessage()); 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("An error occurred while updating the professor.");
        }
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteProfessor(@PathVariable String email) {

        if (professorService.getProfessorByEmail(email).isEmpty()) {
            return ResponseEntity.notFound().build(); 
        }
        try {

            professorService.deleteProfessor(email);
            return ResponseEntity.noContent().build(); 
        } catch (Exception e) {

             System.err.println("Error deleting professor: " + e.getMessage()); 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}