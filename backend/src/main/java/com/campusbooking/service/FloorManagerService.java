package com.campusbooking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.FloorManager;
import com.campusbooking.repository.FloorManagerRepository;

@Service
public class FloorManagerService {

    @Autowired private FloorManagerRepository floorManagerRepository;
    // @Autowired private UserRepository userRepository;

    public List<FloorManager> getAllFloorManagers() {
        return floorManagerRepository.findAll();
    }

    public Optional<FloorManager> getFloorManagerByEmail(String email) {
        return floorManagerRepository.findById(email);
    }

    @Transactional // Ensure atomicity
    public FloorManager createFloorManager(FloorManager manager) {
        // 1. Create and save the base Users entity first
        // Users baseUser = new Users();
        // baseUser.setEmail(manager.getEmail());
        // baseUser.setPassword(manager.getPassword()); // Assumes password is hashed before calling this
        // baseUser.setName(manager.getName());
        // baseUser.setPhone(manager.getPhone());
        // userRepository.save(baseUser); // Save the base user

        return floorManagerRepository.save(manager);
    }


    public void deleteFloorManager(String email) {
        floorManagerRepository.deleteById(email);
    }
}
