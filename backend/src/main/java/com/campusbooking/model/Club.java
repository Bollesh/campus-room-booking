package com.campusbooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class Club {
    @Id
    private String name;

    @Column(name = "poc_student_email", unique = true)
    private String pocStudentEmail; 

    @Column(name = "faculty_head_email", unique = true)
    private String facultyHeadEmail; 

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "poc_student_email", referencedColumnName = "email", insertable = false, updatable = false, unique = true) 
    private Student pocStudent;

    @OneToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "faculty_head_email", referencedColumnName = "email", insertable = false, updatable = false, unique = true) 
    private Professor facultyHead;

    public Club() {
    }

    public Club(String name, String pocStudentEmail, String facultyHeadEmail) {
        this.name = name;
        this.pocStudentEmail = pocStudentEmail;
        this.facultyHeadEmail = facultyHeadEmail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPocStudentEmail() {
        return pocStudentEmail;
    }

    public void setPocStudentEmail(String pocStudentEmail) {
        this.pocStudentEmail = pocStudentEmail;
    }

    public String getFacultyHeadEmail() {
        return facultyHeadEmail;
    }

    public void setFacultyHeadEmail(String facultyHeadEmail) {
        this.facultyHeadEmail = facultyHeadEmail;
    }

    public Student getPocStudent() {
        return pocStudent;
    }

    public void setPocStudent(Student pocStudent) {
        this.pocStudent = pocStudent;
    }

    public Professor getFacultyHead() {
        return facultyHead;
    }

    public void setFacultyHead(Professor facultyHead) {
        this.facultyHead = facultyHead;
    }

    @Override
    public String toString() {
        return "Club [name=" + name + ", pocStudentEmail=" + pocStudentEmail + ", facultyHeadEmail=" + facultyHeadEmail + "]";
    }
}
