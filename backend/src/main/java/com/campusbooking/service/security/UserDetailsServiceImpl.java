package com.campusbooking.service.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.Users;
import com.campusbooking.repository.FloorManagerRepository;
import com.campusbooking.repository.ProfessorRepository;
import com.campusbooking.repository.SecurityRepository;
import com.campusbooking.repository.StudentCouncilRepository;
import com.campusbooking.repository.StudentRepository;
import com.campusbooking.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private FloorManagerRepository floorManagerRepository;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private StudentCouncilRepository studentCouncilRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Find the base user record
        Optional<Users> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        Users user = userOptional.get();

        // 2. Determine authorities based on specific role tables
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Check each role table using the email (which is the PK for role entities)
        if (studentCouncilRepository.existsById(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT_COUNCIL"));
        } else if (studentRepository.existsById(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
            // Check if also Student Council SEE ABOVE
        } else if (professorRepository.existsById(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSOR"));
        } else if (floorManagerRepository.existsById(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_FLOOR_MANAGER"));
        } else if (securityRepository.existsById(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SECURITY"));
        }
        // If a user exists in the Users table but not in any specific role table,
        // they will have no authorities. might want to add a default "ROLE_USER"
        // or handle this case based on app logic (e.g., prevent login).
        // For now, we only assign specific roles found.

        // 3. Create and return Spring Security User object
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(), // Assumes password field exists and is populated
                authorities // The list of roles determined above
        );
    }
}
