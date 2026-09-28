package com.campusbooking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.Professor;
import com.campusbooking.repository.ProfessorRepository;
// import com.campusbooking.repository.UserRepository;

@Service
public class ProfessorService {

    @Autowired private ProfessorRepository professorRepository;
    // @Autowired private UserRepository userRepository;

    public List<Professor> getAllProfessors() {
        return professorRepository.findAll();
    }

    public Optional<Professor> getProfessorByEmail(String email) {
        return professorRepository.findById(email);
    }

    @Transactional // Ensure atomicity
    public Professor createProfessor(Professor professor) {
        // 1. Create and save the base Users entity first
        // Users baseUser = new Users();
        // baseUser.setEmail(professor.getEmail());
        // baseUser.setPassword(professor.getPassword()); // Assumes password is set and HASHED before calling this
        // baseUser.setName(professor.getName());
        // baseUser.setPhone(professor.getPhone());
        // userRepository.save(baseUser); // Save the base user

        return professorRepository.save(professor);
    }

    public void deleteProfessor(String email) {
        professorRepository.deleteById(email);
    }
}
