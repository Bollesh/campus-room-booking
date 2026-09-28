package com.campusbooking.service;

// Thrown when a booking overlaps an active booking of the same room, whether caught by the
// conflict query or by the no_overlapping_active_booking constraint. Mapped to 409 Conflict.
public class BookingConflictException extends IllegalStateException {

    public BookingConflictException(String message) {
        super(message);
    }

    public BookingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
