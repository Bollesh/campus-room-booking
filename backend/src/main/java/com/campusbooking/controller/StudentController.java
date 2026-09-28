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

import com.campusbooking.model.Student;
import com.campusbooking.service.StudentService;
import com.campusbooking.service.UserService;

@RestController
@RequestMapping("/api/students") 
public class StudentController {

    private final StudentService studentService;
    private final UserService userService;

    @Autowired
    public StudentController(StudentService studentService, UserService userService) {
        this.studentService = studentService;
        this.userService = userService; 
    }

    @GetMapping
    public List<Student> getAllStudents() {
        return studentService.getAllStudents();
    }

    @GetMapping("/{email}")
    public ResponseEntity<Student> getStudentByEmail(@PathVariable String email) {
        Optional<Student> studentOptional = studentService.getStudentByEmail(email);
        return studentOptional.map(ResponseEntity::ok)
                              .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createStudent(@RequestBody Student student) {
        if (student.getRegno() == null) {
            return ResponseEntity.badRequest().body("Registration number (regno) is required for students.");
        }

        if (userService.existsByEmail(student.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("User with email " + student.getEmail() + " already exists.");
        }

        try {
            Student createdStudent = studentService.createStudent(student);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStudent);
        } catch (Exception e) {
             System.err.println("Error creating student: " + e.getMessage());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("An error occurred while creating the student.");
        }
    }

    @PutMapping("/{email}")
    public ResponseEntity<?> updateStudent(@PathVariable String email, @RequestBody Student studentDetails) {
        Optional<Student> existingStudentOptional = studentService.getStudentByEmail(email);

        if (existingStudentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Student existingStudent = existingStudentOptional.get();

        if (studentDetails.getRegno() == null) {
             return ResponseEntity.badRequest().body("Registration number (regno) cannot be null for students.");
        }

        existingStudent.setName(studentDetails.getName());
        existingStudent.setPhone(studentDetails.getPhone());
        existingStudent.setRegno(studentDetails.getRegno());

        try {
            Student updatedStudent = studentService.createStudent(existingStudent);
            return ResponseEntity.ok(updatedStudent);
        } catch (Exception e) {
             System.err.println("Error updating student: " + e.getMessage());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("An error occurred while updating the student.");
        }
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteStudent(@PathVariable String email) {
        if (studentService.getStudentByEmail(email).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            studentService.deleteStudent(email);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
             System.err.println("Error deleting student: " + e.getMessage());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
