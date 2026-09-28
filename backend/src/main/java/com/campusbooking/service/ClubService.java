package com.campusbooking.service;

import java.util.List;
import java.util.Optional;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.Club;
import com.campusbooking.repository.ClubRepository;
import com.campusbooking.repository.ProfessorRepository;
import com.campusbooking.repository.StudentRepository;

@Service
public class ClubService {

    private final ClubRepository clubRepository;
    private final ProfessorRepository professorRepository; // To validate facultyHead email
    private final StudentRepository studentRepository;     // To validate POC student email

    @Autowired
    public ClubService(ClubRepository clubRepository, ProfessorRepository professorRepository, StudentRepository studentRepository) {
        this.clubRepository = clubRepository;
        this.professorRepository = professorRepository;
        this.studentRepository = studentRepository;
    }

    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    public Optional<Club> getClubByName(String name) {
        return clubRepository.findById(name);
    }

    public boolean existsByName(String name) {
        return clubRepository.existsById(name);
    }

    @Transactional
    public Club saveClub(Club club) {
        // Validate Faculty Head email exists as a Professor
        if (club.getFacultyHeadEmail() != null && !professorRepository.existsById(club.getFacultyHeadEmail())) {
            throw new IllegalArgumentException("Faculty head email '" + club.getFacultyHeadEmail() + "' does not correspond to a registered professor.");
        }

        // Validate POC Student email exists as a Student
        if (club.getPocStudentEmail() != null && !studentRepository.existsById(club.getPocStudentEmail())) {
            throw new IllegalArgumentException("POC email '" + club.getPocStudentEmail() + "' does not correspond to a registered student.");
        }

        return clubRepository.save(club);
    }

    @Transactional
    public Club createClub(Club club) {
        // Additional creation-specific logic could go here if needed
        return saveClub(club);
    }

    @Transactional
    public void deleteClub(String name) {
        clubRepository.deleteById(name);
    }
}
