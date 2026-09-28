package com.campusbooking.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Users {
    @Id
    private String email;
    private String name;
    private Long phone;

    // Bcrypt hash. Accepted in request bodies (create/update endpoints) but never serialized:
    // users are embedded in booking and club responses.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    public Users(){
    }
    public Users(String email, String name, Long phone) {
        this.email = email;
        this.name = name;
        this.phone = phone;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Long getPhone() {
        return phone;
    }
    public void setPhone(Long phone) {
        this.phone = phone;
    }
    @Override
    public String toString() {
        return "User [email=" + email + ", name=" + name + ", phone=" + phone + "]";
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
