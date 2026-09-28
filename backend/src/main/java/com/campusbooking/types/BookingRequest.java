package com.campusbooking.types;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;

// DTO for creating a booking, including required equipment
public class BookingRequest {
    private String block;
    private String roomNo;

    @Future(message = "start time must be in the future")
    private LocalDateTime startTime;

    @Future(message = "end time must be in the future")
    private LocalDateTime endTime;

    private String purpose;
    private String studentEmail;
    private String clubName; // Optional

    public BookingRequest() {}

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }
}
