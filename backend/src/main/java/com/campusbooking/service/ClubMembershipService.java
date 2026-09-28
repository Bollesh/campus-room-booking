package com.campusbooking.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.Club;
import com.campusbooking.model.ClubMembership;
import com.campusbooking.model.ClubMembershipId;
import com.campusbooking.model.Student;
import com.campusbooking.repository.ClubMembershipRepository;
import com.campusbooking.repository.ClubRepository;
import com.campusbooking.repository.StudentRepository;

@Service
public class ClubMembershipService {

    private final ClubMembershipRepository clubMembershipRepository;
    private final StudentRepository studentRepository;
    private final ClubRepository clubRepository;

    @Autowired
    public ClubMembershipService(ClubMembershipRepository clubMembershipRepository,
                                 StudentRepository studentRepository,
                                 ClubRepository clubRepository) {
        this.clubMembershipRepository = clubMembershipRepository;
        this.studentRepository = studentRepository;
        this.clubRepository = clubRepository;
    }

    @Transactional
    public ClubMembership addMembership(String studentEmail, String clubName) {
        // 1. Validate Student exists
        Student student = studentRepository.findById(studentEmail)
                .orElseThrow(() -> new NoSuchElementException("Student with email '" + studentEmail + "' not found."));

        // 2. Validate Club exists
        Club club = clubRepository.findById(clubName)
                .orElseThrow(() -> new NoSuchElementException("Club with name '" + clubName + "' not found."));

        // 3. Check if membership already exists
        ClubMembershipId id = new ClubMembershipId(studentEmail, clubName);
        if (clubMembershipRepository.existsById(id)) {
            throw new IllegalStateException("Student '" + studentEmail + "' is already a member of club '" + clubName + "'.");
        }

        // 4. Create and save the membership
        ClubMembership membership = new ClubMembership();
        membership.setStuEmail(studentEmail); // Set the ID fields directly
        membership.setClubName(clubName);     // Set the ID fields directly
        membership.setStudent(student);
        membership.setClub(club);

        return clubMembershipRepository.save(membership);
    }

    @Transactional
    public boolean removeMembership(String studentEmail, String clubName) {
        ClubMembershipId id = new ClubMembershipId(studentEmail, clubName);
        if (clubMembershipRepository.existsById(id)) {
            clubMembershipRepository.deleteById(id);
            return true;
        }
        return false; // Membership did not exist
    }

    public Optional<ClubMembership> getMembershipById(String studentEmail, String clubName) {
        ClubMembershipId id = new ClubMembershipId(studentEmail, clubName);
        return clubMembershipRepository.findById(id);
    }

    public List<ClubMembership> getMembershipsByStudent(String studentEmail) {
        return clubMembershipRepository.findByStuEmail(studentEmail);
    }

    public List<ClubMembership> getMembershipsByClub(String clubName) {
        return clubMembershipRepository.findByClubName(clubName);
    }

    public List<ClubMembership> getAllMemberships() {
        return clubMembershipRepository.findAll();
    }
}
