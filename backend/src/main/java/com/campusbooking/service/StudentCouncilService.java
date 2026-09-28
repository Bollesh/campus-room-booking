package com.campusbooking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.StudentCouncil;
import com.campusbooking.repository.StudentCouncilRepository;

@Service
public class StudentCouncilService {

    @Autowired private StudentCouncilRepository studentCouncilRepository;
    // @Autowired private StudentRepository studentRepository;
    // @Autowired private UserRepository userRepository;

    public List<StudentCouncil> getAllCouncilMembers() {
        return studentCouncilRepository.findAll();
    }

    public Optional<StudentCouncil> getCouncilMemberByEmail(String email) {
        return studentCouncilRepository.findById(email);
    }

    public boolean existsByEmail(String email) {
        return studentCouncilRepository.existsById(email);
    }

    @Transactional
    public StudentCouncil saveCouncilMember(StudentCouncil member) {
        // Basic validation for required fields (inherited and specific)
        if (member.getEmail() == null || member.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        if (member.getName() == null || member.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        if (member.getPhone() == null) {
             throw new IllegalArgumentException("Phone number cannot be null.");
        }
        if (member.getRegno() == null) {
            throw new IllegalArgumentException("Registration number (regno) cannot be null.");
        }
        if (member.getPosition() == null || member.getPosition().trim().isEmpty()) {
            throw new IllegalArgumentException("Position cannot be null or empty for Student Council members.");
        }

        // Student baseStudent = new Student();
        // baseStudent.setEmail(member.getEmail());
        // baseStudent.setPassword(member.getPassword());
        // baseStudent.setName(member.getName());
        // baseStudent.setPhone(member.getPhone());
        // baseStudent.setRegno(member.getRegno());
        // studentRepository.save(baseStudent);

        // Users baseUser = new Users();
        // baseUser.setEmail(member.getEmail());
        // baseUser.setPassword(member.getPassword()); // Assumes password is set and HASHED before calling this
        // baseUser.setName(member.getName());
        // baseUser.setPhone(member.getPhone());
        // userRepository.save(baseUser);

        return studentCouncilRepository.save(member);
    }

    @Transactional
    public void deleteCouncilMember(String email) {
        // Check if member exists
        if (!studentCouncilRepository.existsById(email)) {
             return;
        }
        studentCouncilRepository.deleteById(email);
    }
}
