package com.campusbooking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.campusbooking.model.ClubMembership;
import com.campusbooking.model.ClubMembershipId;

public interface ClubMembershipRepository extends JpaRepository<ClubMembership, ClubMembershipId> {
    
    @Query("SELECT cm FROM ClubMembership cm WHERE cm.id.stuEmail = :stuEmail")
    List<ClubMembership> findByStuEmail(String stuEmail);

    @Query("SELECT cm FROM ClubMembership cm WHERE cm.id.clubName = :clubName")
    List<ClubMembership> findByClubName(String clubName);

    @Query("SELECT COUNT(cm) > 0 FROM ClubMembership cm WHERE cm.id.stuEmail = :stuEmail AND cm.id.clubName = :clubName")
    boolean existsByStuEmailAndClubName(String stuEmail, String clubName);

}
