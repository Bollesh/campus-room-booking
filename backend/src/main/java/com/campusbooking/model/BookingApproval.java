package com.campusbooking.model;

import java.time.LocalDateTime;

import com.campusbooking.types.ApprovalStatus;
import com.campusbooking.types.ApproverRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

@Entity
// @IdClass(BookingApprovalId.class)
public class BookingApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ensures these are set via the booking relationship
    @Column(name = "block", insertable = false, updatable = false)
    private String block;
    @Column(name = "room_no", insertable = false, updatable = false)
    private String roomNo;
    @Column(name = "start_time", insertable = false, updatable = false)
    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    private ApproverRole approverRole; // Changed from approverType

    private String approverEmail;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus; // Changed from String
    
    private LocalDateTime approvalTime;
    private String comments;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumns({
        @JoinColumn(name = "block", referencedColumnName = "block"),
        @JoinColumn(name = "room_no", referencedColumnName = "room_no"), 
        @JoinColumn(name = "start_time", referencedColumnName = "start_time") 
    })
    private Booking booking;

    public BookingApproval() {}

    public BookingApproval(Booking booking, ApproverRole approverRole, String approverEmail, ApprovalStatus approvalStatus, LocalDateTime approvalTime, String comments) {
        this.booking = booking;
        this.block = booking.getBlock();
        this.roomNo = booking.getRoomNo();
        this.startTime = booking.getStartTime();
        this.approverRole = approverRole;
        this.approverEmail = approverEmail;
        this.approvalStatus = approvalStatus;
        this.approvalTime = approvalTime;
        this.comments = comments;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public ApproverRole getApproverRole() { return approverRole; }
    public void setApproverRole(ApproverRole approverRole) { this.approverRole = approverRole; }

    public String getApproverEmail() { return approverEmail; }
    public void setApproverEmail(String approverEmail) { this.approverEmail = approverEmail; }

    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }

    public LocalDateTime getApprovalTime() { return approvalTime; }
    public void setApprovalTime(LocalDateTime approvalTime) { this.approvalTime = approvalTime; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
}
