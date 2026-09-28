package com.campusbooking.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // For cleaner error responses
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.campusbooking.model.Booking;
import com.campusbooking.model.BookingId;
import com.campusbooking.service.BookingService;
import com.campusbooking.types.ApprovalRequest;
import com.campusbooking.types.ApprovalStatus;
import com.campusbooking.types.BookingRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings") // Base path for booking-related endpoints
@Validated
public class BookingController {

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // GET /api/bookings - Retrieve all bookings
    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    // GET /api/bookings/{block}/{roomNo}/{startTime} - Retrieve a single booking by composite ID
    @GetMapping("/{block}/{roomNo}/{startTime}")
    public ResponseEntity<Booking> getBookingById(
            @PathVariable String block,
            @PathVariable String roomNo,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime) {

        BookingId id = bookingService.createBookingId(block, roomNo, startTime);
        Optional<Booking> bookingOptional = bookingService.getBookingById(id);

        // If using LAZY fetch for BookingEquipment in Booking entity, it won't be loaded here.
        // If needed, either change to EAGER (potentially inefficient) or use a DTO
        // and populate it manually in the service/controller after fetching.

        return bookingOptional.map(ResponseEntity::ok)
                              .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(params = "studentEmail")
    public List<Booking> getBookingsByStudentEmail(@RequestParam String studentEmail) {
        return bookingService.getBookingsByStudentEmail(studentEmail);
    }

    // POST /api/bookings - Create a new booking
    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequest request) { // Use BookingRequest DTO
        try {
            // Basic input validation
            if (request.getBlock() == null || request.getBlock().trim().isEmpty() ||
                request.getRoomNo() == null || request.getRoomNo().trim().isEmpty() ||
                request.getStartTime() == null ||
                request.getStudentEmail() == null || request.getStudentEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("{\"error\": \"Missing required fields: block, roomNo, startTime, studentEmail.\"}");
            }

            // Call the service method
            Booking createdBooking = bookingService.createBooking(request);

            // returning the created Booking object.
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking); // 201 Created

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Handle validation errors from the service (room/student/club/equipment not found, conflict)
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}"); // 400 Bad Request
        } catch (Exception e) {
            // Generic internal server error
            System.err.println("Error creating booking: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("{\"error\": \"An internal error occurred while creating the booking.\"}");
        }
    }

    // PUT /api/bookings/{block}/{roomNo}/{startTime}/purpose - Update booking purpose (example update)
    @PutMapping("/{block}/{roomNo}/{startTime}/purpose")
    public ResponseEntity<?> updateBookingPurpose(
            @PathVariable String block,
            @PathVariable String roomNo,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestBody String newPurpose) { // Assuming the body just contains the new purpose string

        BookingId id = bookingService.createBookingId(block, roomNo, startTime);
        try {
            Optional<Booking> updatedBooking = bookingService.updateBookingPurpose(id, newPurpose);
            return updatedBooking.map(ResponseEntity::ok) // 200 OK with updated booking
                                 .orElseGet(() -> ResponseEntity.notFound().build()); // 404 if not found
        } catch (Exception e) {
             System.err.println("Error updating booking purpose: " + e.getMessage()); // Replace with proper logging
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("An internal error occurred while updating the booking purpose.");
        }
    }


    // DELETE /api/bookings/{block}/{roomNo}/{startTime} - Delete a booking by composite ID
    @DeleteMapping("/{block}/{roomNo}/{startTime}")
    public ResponseEntity<Void> deleteBooking(
            @PathVariable String block,
            @PathVariable String roomNo,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime) {

        BookingId id = bookingService.createBookingId(block, roomNo, startTime);
        boolean deleted = bookingService.deleteBooking(id);

        if (deleted) {
            return ResponseEntity.noContent().build(); // 204 No Content on success
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found if booking didn't exist
        }
    }

    @PostMapping("/{block}/{roomNo}/{startTime}/approvals")
    public ResponseEntity<?> submitApproval(
            @PathVariable String block,
            @PathVariable String roomNo,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestBody ApprovalRequest approvalRequest) { // Use the DTO

        BookingId bookingId = bookingService.createBookingId(block, roomNo, startTime);

        String approverEmail = approvalRequest.getApproverEmail();
        if (approverEmail == null || approverEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Approver email is required in the request body.");
        }

        ApprovalStatus status;
        try {
            // Ensure ApprovalStatus enum exists in com.campusbooking.types
            status = ApprovalStatus.valueOf(approvalRequest.getStatus().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body("Invalid approval status. Must be 'APPROVED' or 'REJECTED'.");
        }

        // manual checks (determining role based on email, checking booking status, etc.)
        try {
            Booking updatedBooking = bookingService.recordApproval(
                    bookingId,
                    approverEmail, // Pass the manually provided email
                    status,
                    approvalRequest.getComments()
            );
            // Return the booking, potentially with updated overallStatus
            return ResponseEntity.ok(updatedBooking);
        } catch (NoSuchElementException e) {
            // Booking not found
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            // Business logic errors (e.g., already approved/rejected, not pending)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (SecurityException e) {
            // If the service layer performs role validation based on the provided email
            // and determines the user is not authorized for this booking/role.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            // Generic internal error
            System.err.println("Error submitting approval: " + e.getMessage()); // Replace logging
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred while submitting approval.", e);
        }
    }
}
