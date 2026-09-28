package com.campusbooking.model;

import java.io.Serializable;
import java.util.Objects;


public class RoomId implements Serializable {
    private String block;
    private String room;

    public RoomId() {}

    public RoomId(String block, String room) {
        this.block = block;
        this.room = room;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomId)) return false;
        RoomId that = (RoomId) o;
        return Objects.equals(block, that.block) && Objects.equals(room, that.room);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, room);
    }

    public String getBlock() {
        return block;
    }

    public String getRoom() {
        return room;
    }
}
