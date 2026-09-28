package com.campusbooking.model;

import jakarta.persistence.Entity;

@Entity
public class Security extends Users{

   public Security(){
   }

    public Security(String email, String name, Long phone) {
        super(email, name, phone);
    }

    @Override
    public String toString() {
        return "Security [getEmail()=" + getEmail() + ", getName()=" + getName() + ", getPhone()=" + getPhone() + "]";
    }
    
}
