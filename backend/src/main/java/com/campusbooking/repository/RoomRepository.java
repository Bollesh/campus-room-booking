package com.campusbooking.repository;

import java.util.Optional;

import com.campusbooking.model.Room;
import com.campusbooking.model.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, RoomId> {

    // Row-locks the room until the transaction ends, serializing booking creation per room.
    // NO KEY UPDATE doesn't block the FOR KEY SHARE locks taken by booking's foreign-key checks.
    @Query(value = "SELECT 1 FROM room WHERE block = :block AND room = :room FOR NO KEY UPDATE", nativeQuery = true)
    Optional<Integer> lockRoom(@Param("block") String block, @Param("room") String room);
}
