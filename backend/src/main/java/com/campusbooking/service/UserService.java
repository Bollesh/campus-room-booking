package com.campusbooking.service;

import com.campusbooking.model.Users;
import com.campusbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Users saveUser(Users user) {
        return userRepository.save(user);
    }

    public Optional<Users> getUserByEmail(String email) {
        return userRepository.findById(email);
    }

    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(String email) {
        userRepository.deleteById(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsById(email);
    }
}
