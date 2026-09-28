package com.campusbooking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.Student;
import com.campusbooking.repository.StudentRepository;
// import com.campusbooking.repository.UserRepository;

@Service
public class StudentService {

    // @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentByEmail(String email) {
        return studentRepository.findById(email);
    }

    @Transactional // Ensure atomicity
    public Student createStudent(Student student) {
        // 1. Create and save the base Users entity first
        // Users baseUser = new Users();
        // baseUser.setEmail(student.getEmail());
        // baseUser.setPassword(student.getPassword()); // Assumes password is set and HASHED before calling this
        // baseUser.setName(student.getName());
        // baseUser.setPhone(student.getPhone());
        // userRepository.save(baseUser); // Save the base user

        // 2. Save the Student entity (which uses the same email PK)
        // The student object passed in already has all details (email, name, phone, regno, hashed_password)
        // If Student doesn't have a password field, ensure Users has it.
        return studentRepository.save(student); // Save the student-specific details
    }

    public void deleteStudent(String email) {
        studentRepository.deleteById(email);
    }
}
