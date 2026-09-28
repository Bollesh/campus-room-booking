package com.campusbooking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@IdClass(RoomId.class) //composite PK (block + room)
public class Room {

    @Id
    private String block;

    @Id
    private String room;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manager_email", referencedColumnName = "email")
    private FloorManager manager;

    public Room() {}

    public Room(String block, String room, FloorManager manager) {
        this.block = block;
        this.room = room;
        this.manager = manager;
    }

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

    public FloorManager getManager() {
        return manager;
    }

    public void setManager(FloorManager manager) {
        this.manager = manager;
    }

    @Override
    public String toString() {
        return "Room [block=" + block + ", room=" + room + ", manager=" + manager.getEmail() + "]";
    }
}
