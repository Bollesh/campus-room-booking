package com.campusbooking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.repository.FloorManagerRepository;
import com.campusbooking.repository.ProfessorRepository;
import com.campusbooking.repository.StudentRepository;
import com.campusbooking.repository.UserRepository;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private FloorManagerRepository floorManagerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Loading initial data...");

        // Use checks to avoid recreating data if it already exists
        if (userRepository.count() == 0) { // Example check: only load if users table is empty
            loadUsersAndRoles();
            loadRooms();
            loadClubs();
            // Add calls to load other data as needed
            System.out.println("Initial data loaded.");
        } else {
            System.out.println("Data already exists. Skipping data loading.");
        }
    }

    private void loadUsersAndRoles() {
        // --- Create Students using JdbcTemplate ---
        String studentPass1Hash = passwordEncoder.encode("studentpass1");
        jdbcTemplate.update(
            "INSERT INTO users (email, password, name, phone) VALUES (?, ?, ?, ?)",
            "poc.student1@example.com", studentPass1Hash, "PoC Student One", 1112223330L
        );
        jdbcTemplate.update(
            "INSERT INTO student (email, regno) VALUES (?, ?)",
            "poc.student1@example.com", 23100101L
        );

        String studentPass2Hash = passwordEncoder.encode("studentpass2");
        jdbcTemplate.update(
            "INSERT INTO users (email, password, name, phone) VALUES (?, ?, ?, ?)",
            "poc.student2@example.com", studentPass2Hash, "PoC Student Two", 1112223331L
        );
        jdbcTemplate.update(
            "INSERT INTO student (email, regno) VALUES (?, ?)",
            "poc.student2@example.com", 23100102L
        );

        // --- Create Professors using JdbcTemplate ---
        String profPass1Hash = passwordEncoder.encode("profpass1");
        jdbcTemplate.update(
            "INSERT INTO users (email, password, name, phone) VALUES (?, ?, ?, ?)",
            "faculty.head1@example.com", profPass1Hash, "Faculty Head One", 9998887770L
        );
        jdbcTemplate.update(
            "INSERT INTO professor (email, is_cultural) VALUES (?, ?)",
            "faculty.head1@example.com", false
        );

        String profPass2Hash = passwordEncoder.encode("profpass2");
        jdbcTemplate.update(
            "INSERT INTO users (email, password, name, phone) VALUES (?, ?, ?, ?)",
            "faculty.head2@example.com", profPass2Hash, "Faculty Head Two", 9998887771L
        );
        jdbcTemplate.update(
            "INSERT INTO professor (email, is_cultural) VALUES (?, ?)",
            "faculty.head2@example.com", false
        );
        
        String profPass3Hash = passwordEncoder.encode("profpass3");
        jdbcTemplate.update(
            "INSERT INTO users (email, password, name, phone) VALUES (?, ?, ?, ?)",
            "cultural.prof@example.com", profPass3Hash, "Cultural Prof", 8887776661L
        );
        jdbcTemplate.update(
            "INSERT INTO professor (email, is_cultural) VALUES (?, ?)",
            "cultural.prof@example.com", true // Cultural professor
        );

        // --- Create Floor Managers using JdbcTemplate ---
        String managerPass1Hash = passwordEncoder.encode("managerpass1");
        jdbcTemplate.update(
            "INSERT INTO users (email, password, name, phone) VALUES (?, ?, ?, ?)",
            "floor.manager1@example.com", managerPass1Hash, "Floor Manager One", 4445556661L
        );
        jdbcTemplate.update(
            "INSERT INTO floor_manager (email) VALUES (?)",
            "floor.manager1@example.com"
        );

        String managerPass2Hash = passwordEncoder.encode("managerpass2");
        jdbcTemplate.update(
            "INSERT INTO users (email, password, name, phone) VALUES (?, ?, ?, ?)",
            "floor.manager2@example.com", managerPass2Hash, "Floor Manager Two", 4445556662L
        );
        jdbcTemplate.update(
            "INSERT INTO floor_manager (email) VALUES (?)",
            "floor.manager2@example.com"
        );


        // --- Create Student Council Member using JdbcTemplate ---
        String scPass1Hash = passwordEncoder.encode("scpass1");
        jdbcTemplate.update(
            "INSERT INTO users (email, password, name, phone) VALUES (?, ?, ?, ?)",
            "sc.member1@example.com", scPass1Hash, "Student President", 1234567890L
        );
        // SC Member is also a Student first
        jdbcTemplate.update(
            "INSERT INTO student (email, regno) VALUES (?, ?)",
            "sc.member1@example.com", 23100103L
        );
        // Then insert into student_council table
        jdbcTemplate.update(
            "INSERT INTO student_council (email, position) VALUES (?, ?)",
            "sc.member1@example.com", "PRESIDENT"
        );

        // --- Create Security Member using JdbcTemplate ---
        String securityPass1Hash = passwordEncoder.encode("securitypass1");
        jdbcTemplate.update(
            "INSERT INTO users (email, password, name, phone) VALUES (?, ?, ?, ?)",
            "security1@example.com", securityPass1Hash, "Security One", 0000000000L
        );
        jdbcTemplate.update(
            "INSERT INTO security (email) VALUES (?)",
            "security1@example.com"
        );


        System.out.println("Loaded sample users via JdbcTemplate.");
    }

    private void loadRooms() {
        // Check if manager exists (using repository or another JdbcTemplate query) before inserting
        // Simplified example assuming managers are loaded first
        if (floorManagerRepository.existsById("floor.manager1@example.com")) {
             jdbcTemplate.update(
                "INSERT INTO room (block, room, manager_email) VALUES (?, ?, ?)",
                "AB1", "101", "floor.manager1@example.com"
            );
        } else {
             System.err.println("Skipping room AB1-101 creation, manager floor.manager1@example.com not found.");
        }

        if (floorManagerRepository.existsById("floor.manager2@example.com")) {
            jdbcTemplate.update(
                "INSERT INTO room (block, room, manager_email) VALUES (?, ?, ?)",
                "AB2", "101", "floor.manager2@example.com"
            );
        } else {
             System.err.println("Skipping room AB2-101 creation, manager floor.manager2@example.com not found.");
        }
        System.out.println("Loaded sample rooms via JdbcTemplate.");
    }

    private void loadClubs() {
        // Check if prerequisite users exist before inserting club and membership
       if (studentRepository.existsById("poc.student1@example.com") && professorRepository.existsById("faculty.head1@example.com")) {
           jdbcTemplate.update(
               "INSERT INTO club (name, poc_student_email, faculty_head_email) VALUES (?, ?, ?)",
               "Tech Club", "poc.student1@example.com", "faculty.head1@example.com"
           );
           // Also add membership
           jdbcTemplate.update(
               "INSERT INTO club_membership (club_name, stu_email) VALUES (?, ?)",
               "Tech Club", "poc.student1@example.com"
           );
       } else {
            System.err.println("Could not create 'Tech Club' via JdbcTemplate because prerequisite users do not exist.");
       }

       if (studentRepository.existsById("poc.student2@example.com") && professorRepository.existsById("faculty.head2@example.com")) {
            jdbcTemplate.update(
               "INSERT INTO club (name, poc_student_email, faculty_head_email) VALUES (?, ?, ?)",
               "Music Club", "poc.student2@example.com", "faculty.head2@example.com"
           );
           jdbcTemplate.update(
               "INSERT INTO club_membership (club_name, stu_email) VALUES (?, ?)",
               "Music Club", "poc.student2@example.com"
           );
       } else {
           System.err.println("Could not create 'Music Club' via JdbcTemplate because prerequisite users do not exist.");
       }

       System.out.println("Loaded sample clubs via JdbcTemplate.");
   }
}
