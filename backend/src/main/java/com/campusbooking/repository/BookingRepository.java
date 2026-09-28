package com.campusbooking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusbooking.model.Booking;
import com.campusbooking.model.BookingId;

public interface BookingRepository extends JpaRepository<Booking, BookingId> {

       @Query("SELECT b FROM Booking b WHERE b.block = :block AND b.roomNo = :roomNo")
       List<Booking> findByBlockAndRoomNo(String block, String room);

       @Query("SELECT b FROM Booking b WHERE b.studentEmail = :studentEmail")
       List<Booking> findByStudentEmail(String studentEmail);

       // An overlap occurs if:
       // (Existing Start Time < New End Time) AND (Existing End Time > New Start Time)
       // We also exclude bookings that are CANCELLED or REJECTED.
       @Query("SELECT b FROM Booking b " +
              "WHERE b.id.block = :block " +
              "AND b.id.roomNo = :roomNo " +
              "AND b.startTime < :newEndTime " +
              "AND b.endTime > :newStartTime " +
              "AND b.overallStatus NOT IN (com.campusbooking.types.BookingStatus.CANCELLED, com.campusbooking.types.BookingStatus.REJECTED)")
       List<Booking> findConflictingBookings(
              @Param("block") String block,
              @Param("roomNo") String roomNo,
              @Param("newStartTime") LocalDateTime newStartTime,
              @Param("newEndTime") LocalDateTime newEndTime);
}
