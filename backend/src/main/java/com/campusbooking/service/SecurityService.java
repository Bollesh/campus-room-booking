package com.campusbooking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.Security;
import com.campusbooking.repository.SecurityRepository;

@Service
public class SecurityService {

    @Autowired private SecurityRepository securityRepository;
    // @Autowired private UserRepository userRepository;

    public List<Security> getAllSecurity() {
        return securityRepository.findAll();
    }

    public Optional<Security> getSecurityByEmail(String email) {
        return securityRepository.findById(email);
    }

    @Transactional // Ensure atomicity
    public Security createSecurity(Security security) {
        // 1. Create and save the base Users entity first
        // Users baseUser = new Users();
        // baseUser.setEmail(security.getEmail());
        // baseUser.setPassword(security.getPassword()); // Assumes password is set and HASHED before calling this
        // baseUser.setName(security.getName());
        // baseUser.setPhone(security.getPhone());
        // userRepository.save(baseUser); // Save the base user

        return securityRepository.save(security);
    }

    public void deleteSecurity(String email) {
        securityRepository.deleteById(email);
    }
}
