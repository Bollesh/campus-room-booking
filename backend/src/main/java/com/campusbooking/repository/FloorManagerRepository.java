package com.campusbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusbooking.model.FloorManager;

public interface FloorManagerRepository extends JpaRepository<FloorManager, String> {
}
