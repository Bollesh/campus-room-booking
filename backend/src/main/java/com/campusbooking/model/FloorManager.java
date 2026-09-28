package com.campusbooking.model;

import jakarta.persistence.Entity;

@Entity
public class FloorManager extends Users{

    public FloorManager(){
    }
 
    public FloorManager(String email, String name, Long phone) {
        super(email, name, phone);
    }
 
    @Override
    public String toString() {
        return "FloorManager [getEmail()=" + getEmail() + ", getName()=" + getName() + ", getPhone()=" + getPhone() + "]";
    }
}
