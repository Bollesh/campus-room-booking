package com.campusbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusbooking.model.StudentCouncil;

public interface StudentCouncilRepository extends JpaRepository<StudentCouncil, String> {
}
