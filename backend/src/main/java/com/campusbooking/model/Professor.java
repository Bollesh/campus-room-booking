package com.campusbooking.model;


import jakarta.persistence.Entity;

@Entity
public class Professor extends Users {

    private Boolean isCultural;

    public Professor() {}

    public Professor(String email, String name, Long phone, Boolean isCultural) {
        super(email, name, phone);
        this.isCultural = isCultural;
    }

    public Boolean getIsCultural() {
        return isCultural;
    }

    public void setIsCultural(Boolean isCultural) {
        this.isCultural = isCultural;
    }

    @Override
    public String toString() {
        return "Professor [isCultural=" + isCultural + ", getEmail()=" + getEmail()
                + ", getName()=" + getName() + ", getPhone()=" + getPhone() + "]";
    }
}
