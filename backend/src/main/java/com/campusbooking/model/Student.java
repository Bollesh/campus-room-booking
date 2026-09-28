package com.campusbooking.model;

import jakarta.persistence.Entity;

@Entity
public class Student extends Users{

    private Long regno;

    public Student() {
    }

    public Student(String email, String name, Long phone, Long regno) {
        super(email, name, phone);
        this.regno = regno;
    }

    public Long getRegno() {
        return regno;
    }

    public void setRegno(Long regno) {
        this.regno = regno;
    }

    @Override
    public String toString() {
        return "Student [regno=" + regno + ", getEmail()=" + getEmail() + ", getName()=" + getName() + ", getPhone()="
                + getPhone() + "]";
    }
    
}
