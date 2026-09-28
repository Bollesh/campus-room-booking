package com.campusbooking.model;

import java.time.LocalDateTime;
import java.util.Set;

import com.campusbooking.types.BookingStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
@IdClass(BookingId.class)
public class Booking {

    @Id
    private String block;

    @Id
    @Column(name = "room_no") // ensures the DB column matches Room's field
    private String roomNo;

    @Id
    @Column(name = "start_time") // explicitly specify column name
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    private String purpose;

    @Enumerated(EnumType.STRING)
    private BookingStatus overallStatus = BookingStatus.PENDING_APPROVAL; //DEFAULT

    @Column(name = "student_email")
    private String studentEmail;
    @Column(name="club_name")
    private String clubName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({
        @JoinColumn(name = "block", referencedColumnName = "block", insertable = false, updatable = false),
        @JoinColumn(name = "room_no", referencedColumnName = "room", insertable = false, updatable = false)
    })
    private Room room;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_email", referencedColumnName = "email", insertable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "club_name", referencedColumnName = "name", insertable = false, updatable = false)
    private Club club;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, 
                fetch = FetchType.EAGER)
    private Set<BookingApproval> approvals; 

    // Constructors

    public Booking() {}

    public Booking(String block, String roomNo, LocalDateTime startTime, LocalDateTime endTime, String purpose
        , Room room, Student student, Club club) {
        this.block = block;
        this.roomNo = roomNo;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.room = room;
        this.student = student;
        this.club = club;

        this.studentEmail = student.getEmail();
        this.clubName = club.getName();
    }

    public BookingStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(BookingStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public Set<BookingApproval> getApprovals() {
        return approvals;
    }

    public void setApprovals(Set<BookingApproval> approvals) {
        this.approvals = approvals;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
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

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getClubName() {
        return clubName;
    }
}
