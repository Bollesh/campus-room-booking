package com.campusbooking.controller.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusbooking.model.FloorManager;
import com.campusbooking.model.Professor;
import com.campusbooking.model.Security;
import com.campusbooking.model.Student;
import com.campusbooking.model.StudentCouncil;
import com.campusbooking.security.JwtUtil;
import com.campusbooking.service.FloorManagerService;
import com.campusbooking.service.ProfessorService;
import com.campusbooking.service.SecurityService;
import com.campusbooking.service.StudentCouncilService;
import com.campusbooking.service.StudentService;
import com.campusbooking.service.UserService;
import com.campusbooking.types.AuthenticationRequest;
import com.campusbooking.types.AuthenticationResponse;
import com.campusbooking.types.RegistrationRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated // Enable validation for request bodies annotated with @Valid
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserDetailsService userDetailsService; // UserDetailsServiceImpl
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;

    // --- Inject ALL necessary services ---
    @Autowired private UserService userService;
    @Autowired private StudentService studentService;
    @Autowired private ProfessorService professorService;
    @Autowired private FloorManagerService floorManagerService;
    @Autowired private SecurityService securityService;
    @Autowired private StudentCouncilService studentCouncilService;

    @PostMapping("/register")
    // Use @Valid to trigger DTO validation
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest request) {

        // 1. Check if email already exists in the base Users table
        if (userService.existsByEmail(request.email())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT) // 409 Conflict
                    .body("{\"error\": \"Email is already taken!\"}");
        }

        // 2. Hash the password
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. Delegate creation based on userType
        try {
            Object createdUserSpecific; // To hold the result from specific services

            switch (request.userType().toUpperCase()) {
                case "STUDENT" -> {
                    if (request.regno() == null) {
                        return ResponseEntity.badRequest().body("{\"error\": \"Registration number (regno) is required for students.\"}");
                    }
                    Student student = new Student();
                    // Set common fields (assuming Student inherits or shares these conceptually)
                    student.setEmail(request.email());
                    student.setPassword(encodedPassword); // Set hashed password
                    student.setName(request.name());
                    student.setPhone(request.phone());
                    // Set specific fields
                    student.setRegno(request.regno());
                    createdUserSpecific = studentService.createStudent(student); // Service handles saving Users + Student
                }

                case "PROFESSOR" -> {
                    if (request.isCultural() == null) {
                        return ResponseEntity.badRequest().body("{\"error\": \"'isCultural' field is required for professors.\"}");
                    }
                    Professor professor = new Professor();
                    professor.setEmail(request.email());
                    professor.setPassword(encodedPassword);
                    professor.setName(request.name());
                    professor.setPhone(request.phone());
                    professor.setIsCultural(request.isCultural());
                    createdUserSpecific = professorService.createProfessor(professor); // Service handles saving Users + Professor
                }

                case "FLOOR_MANAGER" -> {
                    FloorManager floorManager = new FloorManager();
                    floorManager.setEmail(request.email());
                    floorManager.setPassword(encodedPassword);
                    floorManager.setName(request.name());
                    floorManager.setPhone(request.phone());
                    createdUserSpecific = floorManagerService.createFloorManager(floorManager); // Service handles saving Users + FloorManager
                }

                case "SECURITY" -> {
                    Security security = new Security();
                    security.setEmail(request.email());
                    security.setPassword(encodedPassword);
                    security.setName(request.name());
                    security.setPhone(request.phone());
                    createdUserSpecific = securityService.createSecurity(security); // Service handles saving Users + Security
                }

                case "STUDENT_COUNCIL" -> {
                    if (request.regno() == null) {
                        return ResponseEntity.badRequest().body("{\"error\": \"Registration number (regno) is required for Student Council members.\"}");
                    }
                    if (request.position() == null || request.position().isBlank()) {
                        return ResponseEntity.badRequest().body("{\"error\": \"Position is required for Student Council members.\"}");
                    }
                    StudentCouncil councilMember = new StudentCouncil();
                    councilMember.setEmail(request.email());
                    councilMember.setPassword(encodedPassword);
                    councilMember.setName(request.name());
                    councilMember.setPhone(request.phone());
                    councilMember.setRegno(request.regno());
                    councilMember.setPosition(request.position());
                    createdUserSpecific = studentCouncilService.saveCouncilMember(councilMember); // Service handles saving Users + Student + StudentCouncil
                }

                default -> {
                    return ResponseEntity.badRequest().body("{\"error\": \"Invalid userType specified.\"}");
                }
            }

            // 4. Return success response (including the created specific user data)
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUserSpecific);

        } catch (IllegalArgumentException e) {
            // Catch specific validation errors from services if they throw them
             return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            // Catch broader errors during the creation process in services
            System.err.println("Error during user registration: " + e.getMessage()); // Log the error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred during registration.\"}");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.email(), authenticationRequest.password())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Incorrect email or password\"}"); // Return 401
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.email());
        final String jwt = jwtUtil.generateToken(userDetails);

        String role = userDetails.getAuthorities().toArray()[0].toString(); // Assuming single role per user

        // Create response including JWT and the user role
        AuthenticationResponse response = new AuthenticationResponse(jwt, role);

        return ResponseEntity.ok(response);
    }
}
