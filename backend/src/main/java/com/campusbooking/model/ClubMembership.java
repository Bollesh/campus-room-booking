package com.campusbooking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@IdClass(ClubMembershipId.class)
public class ClubMembership {

    @Id
    @Column(name="stu_email")
    private String stuEmail;  // student email (FK to Student)

    @Id
    @Column(name="club_name")
    private String clubName;  // club name (FK to Club)

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stu_email", referencedColumnName = "email", insertable = false, updatable = false)
    private Student student;  // relationship to Student

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "club_name", referencedColumnName = "name", insertable = false, updatable = false)
    private Club club;  // relationship to Club


    public ClubMembership() {
    }

    public ClubMembership(String stuEmail, String clubName, Student student, Club club) {
        this.stuEmail = stuEmail;
        this.clubName = clubName;
        this.student = student;
        this.club = club;
    }

    public String getStuEmail() {
        return stuEmail;
    }

    public void setStuEmail(String stuEmail) {
        this.stuEmail = stuEmail;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }
}
