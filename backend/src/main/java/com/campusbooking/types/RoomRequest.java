package com.campusbooking.types;

// A DTO for room associated files

public class RoomRequest {
    private String block;
    private String room; // Corresponds to the 'room' field in the Room entity
    private String managerEmail; // The email of the FloorManager to associate

    public RoomRequest() {}

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public void setManagerEmail(String managerEmail) {
        this.managerEmail = managerEmail;
    }
}
