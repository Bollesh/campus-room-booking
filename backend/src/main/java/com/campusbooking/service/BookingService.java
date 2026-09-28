package com.campusbooking.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.Booking;
import com.campusbooking.model.BookingApproval;
import com.campusbooking.model.BookingId;
import com.campusbooking.model.Professor;
import com.campusbooking.model.Room;
import com.campusbooking.model.RoomId;
import com.campusbooking.repository.BookingApprovalRepository;
import com.campusbooking.repository.BookingRepository; // For atomic operations
import com.campusbooking.repository.ClubMembershipRepository;
import com.campusbooking.repository.ClubRepository;
import com.campusbooking.repository.FloorManagerRepository;
import com.campusbooking.repository.ProfessorRepository;
import com.campusbooking.repository.RoomRepository;
import com.campusbooking.repository.SecurityRepository;
import com.campusbooking.repository.StudentCouncilRepository;
import com.campusbooking.repository.StudentRepository;
import com.campusbooking.types.ApprovalStatus;
import com.campusbooking.types.ApproverRole;
import com.campusbooking.types.BookingRequest;
import com.campusbooking.types.BookingStatus;

@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingApprovalRepository bookingApprovalRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private StudentCouncilRepository studentCouncilRepository;
    @Autowired private FloorManagerRepository floorManagerRepository;
    @Autowired private SecurityRepository securityRepository;
    @Autowired private ClubMembershipRepository clubMembershipRepository;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsByStudentEmail(String studentEmail) {
        return bookingRepository.findByStudentEmail(studentEmail);
    }

    public Optional<Booking> getBookingById(BookingId id) {
        return bookingRepository.findById(id);
    }

    // Helper to create BookingId easily
    public BookingId createBookingId(String block, String roomNo, LocalDateTime startTime) {
        return new BookingId(block, roomNo, startTime);
    }

    @Transactional
    public Booking createBooking(BookingRequest request) { // Accept DTO
        // 1. Validate startTime vs endTime 
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("Booking end time must be after start time.");
        }
        // Basic duration check (e.g., prevent zero duration bookings)
        if (request.getStartTime().equals(request.getEndTime())) {
             throw new IllegalArgumentException("Booking start and end times cannot be the same.");
        }
        // 1. Validate Room, Student, Club (if provided)
        if (!roomRepository.existsById(new RoomId(request.getBlock(), request.getRoomNo()))) {
             throw new IllegalArgumentException("Room does not exist.");
        }
        if (!studentRepository.existsById(request.getStudentEmail())) {
              throw new IllegalArgumentException("Student does not exist.");
        }
        if (request.getClubName() != null && !request.getClubName().trim().isEmpty() && !clubRepository.existsById(request.getClubName())) {
             throw new IllegalArgumentException("Club does not exist.");
        }

        // Ensure student belongs to club
        if (!clubMembershipRepository.existsByStuEmailAndClubName(request.getStudentEmail(), request.getClubName())) {
            throw new IllegalArgumentException("Student does not belong to club");
        }

        // 2. Check for booking conflict
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                request.getBlock(),
                request.getRoomNo(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!conflictingBookings.isEmpty()) {
            String conflicts = conflictingBookings.stream()
                .map(b -> String.format("Conflict with booking from %s to %s", b.getStartTime(), b.getEndTime()))
                .collect(Collectors.joining(", "));
             throw new IllegalStateException("Time slot conflict for room " + request.getBlock() + "/" + request.getRoomNo() +
                                            " between " + request.getStartTime() + " and " + request.getEndTime() + ". " + conflicts);
        }


        // 4. Create and Save the Booking entity first
        Booking newBooking = new Booking();
        newBooking.setBlock(request.getBlock());
        newBooking.setRoomNo(request.getRoomNo());
        newBooking.setStartTime(request.getStartTime());
        newBooking.setEndTime(request.getEndTime());
        newBooking.setPurpose(request.getPurpose());
        newBooking.setStudentEmail(request.getStudentEmail());
        newBooking.setClubName(request.getClubName()); // Can be null
        newBooking.setOverallStatus(BookingStatus.PENDING_APPROVAL);

        Booking savedBooking = bookingRepository.save(newBooking);

        return savedBooking; // Return the created booking
    }


    @Transactional
    public Booking recordApproval(BookingId bookingId, String approverEmail, ApprovalStatus status, String comments) {
        // 1. Find the booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found"));

        // 2. Check if booking is still pending
        if (booking.getOverallStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Booking is not pending approval (Status: " + booking.getOverallStatus() + ")");
        }

        ApproverRole role = determineApproverRole(approverEmail, booking);
        if (role == null) {
            throw new SecurityException("User " + approverEmail + " is not an authorized approver for this booking.");
        }

        // 4. Check if this user/role combination already submitted an approval
        Optional<BookingApproval> existingApproval = bookingApprovalRepository.findByBookingAndApproverRole(booking, role);
        if (existingApproval.isPresent()) {
              throw new IllegalStateException("Approval for role " + role + " has already been submitted for this booking.");
        }

        // 5. Create and save the new approval record
        BookingApproval approval = new BookingApproval(
                booking,
                role,
                approverEmail,
                status,
                LocalDateTime.now(),
                comments
        );
        bookingApprovalRepository.save(approval);

        // 6. Update the overall booking status based on all approvals
        updateOverallBookingStatus(booking);

        // Return the updated booking (might have new overallStatus)
        return booking;
    }

    private ApproverRole determineApproverRole(String approverEmail, Booking booking) {
        // Check if Floor Manager for this Room
        Room room = roomRepository.findById(new RoomId(booking.getBlock(), booking.getRoomNo())).orElse(null);
        if (room != null && room.getManager() != null && room.getManager().getEmail().equals(approverEmail)) {
            return ApproverRole.FLOOR_MANAGER;
        }

        // Check if Faculty Head for this Club
        if (booking.getClub() != null && booking.getClub().getFacultyHead() != null && booking.getClub().getFacultyHeadEmail().equals(approverEmail)) {
            return ApproverRole.FACULTY_HEAD;
        }

        // Check if Cultural Professor
        Optional<Professor> profOpt = professorRepository.findById(approverEmail);
        if (profOpt.isPresent() && Boolean.TRUE.equals(profOpt.get().getIsCultural())) {
            return ApproverRole.CULTURAL_PROF;
        }

        // Check if Student Council
        if (studentCouncilRepository.existsById(approverEmail)) {
            return ApproverRole.STUDENT_COUNCIL;
        }

        // Check if Security
        if (securityRepository.existsById(approverEmail)) {
            return ApproverRole.SECURITY;
        }

        if (floorManagerRepository.existsById(approverEmail)) {
            return ApproverRole.FLOOR_MANAGER;
        }

        return null; // Not a recognized approver for this booking context
    }

    @Transactional // Should be part of the recordApproval transaction
    protected void updateOverallBookingStatus(Booking booking) {
        // Reload booking to ensure we have the latest state within the transaction
        booking = bookingRepository.findById(new BookingId(booking.getBlock(), booking.getRoomNo(), booking.getStartTime())).orElseThrow();

        // Find all approvals for this booking
        List<BookingApproval> approvals = bookingApprovalRepository.findByBooking(booking);

        // Check for any rejection first
        boolean rejected = approvals.stream()
                .anyMatch(a -> a.getApprovalStatus() == ApprovalStatus.REJECTED);

        if (rejected) {
            booking.setOverallStatus(BookingStatus.REJECTED);
            bookingRepository.save(booking);
            return; // No need to check further
        }

        // Get the set of roles that have approved
        Set<ApproverRole> approvedRoles = approvals.stream()
                .filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED)
                .map(BookingApproval::getApproverRole)
                .collect(Collectors.toSet());

        Set<ApproverRole> requiredRoles = EnumSet.of(
                ApproverRole.FACULTY_HEAD,
                ApproverRole.STUDENT_COUNCIL,
                ApproverRole.CULTURAL_PROF,
                ApproverRole.FLOOR_MANAGER,
                ApproverRole.SECURITY
        );

        if (booking.getClubName() == null || booking.getClubName().isEmpty()) {
              requiredRoles.remove(ApproverRole.FACULTY_HEAD); // Example: No faculty head needed if not a club booking
        }

        // Check if all required roles are present in the approved set
        if (approvedRoles.containsAll(requiredRoles)) {
            booking.setOverallStatus(BookingStatus.APPROVED);
            bookingRepository.save(booking);
        } else {
            booking.setOverallStatus(BookingStatus.PENDING_APPROVAL);
            bookingRepository.save(booking); // Save only if state actually changes
        }
    }

    @Transactional
    public Optional<Booking> updateBookingPurpose(BookingId id, String newPurpose) {
        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            booking.setPurpose(newPurpose);
            return Optional.of(bookingRepository.save(booking));
        }
        return Optional.empty();
    }

    @Transactional
    public boolean deleteBooking(BookingId id) {
        if (bookingRepository.existsById(id)) {
            // 1. Delete associated approvals (handled by cascade on Booking entity)

            // 3. Delete the booking itself
            bookingRepository.deleteById(id);
            return true;
        }
        return false; // Booking not found
    }
}
