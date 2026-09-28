package com.campusbooking.repository;

import com.campusbooking.model.Room;
import com.campusbooking.model.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, RoomId> {
}
