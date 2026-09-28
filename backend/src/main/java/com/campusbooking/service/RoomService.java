package com.campusbooking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusbooking.model.Booking;
import com.campusbooking.model.FloorManager;
import com.campusbooking.model.Room;
import com.campusbooking.model.RoomId;
import com.campusbooking.repository.BookingRepository;
import com.campusbooking.repository.FloorManagerRepository;
import com.campusbooking.repository.RoomRepository;
import com.campusbooking.types.RoomRequest;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final FloorManagerRepository floorManagerRepository; // To validate manager existence
    private final BookingRepository bookingRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository, 
                       FloorManagerRepository floorManagerRepository, 
                       BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.floorManagerRepository = floorManagerRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(RoomId id) {
        return roomRepository.findById(id);
    }

    public boolean existsById(RoomId id) {
        return roomRepository.existsById(id);
    }

    @Transactional
    public Room saveRoom(Room room, String managerEmail) {
        // Validate the manager exists
        FloorManager manager = floorManagerRepository.findById(managerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Floor Manager with email '" + managerEmail + "' not found."));

        room.setManager(manager); // Associate the found manager
        return roomRepository.save(room);
    }

    @Transactional
    public Room createRoom(RoomRequest request) {
         RoomId roomId = new RoomId(request.getBlock(), request.getRoom());
         if (existsById(roomId)) {
             throw new IllegalStateException("Room with Block '" + request.getBlock() + "' and Room '" + request.getRoom() + "' already exists.");
         }
         Room newRoom = new Room();
         newRoom.setBlock(request.getBlock());
         newRoom.setRoom(request.getRoom());
         // saveRoom will validate and set the manager
         return saveRoom(newRoom, request.getManagerEmail());
    }

    @Transactional
    public Optional<Room> updateRoomManager(RoomId id, String newManagerEmail) {
        Optional<Room> existingRoomOpt = roomRepository.findById(id);
        if (existingRoomOpt.isPresent()) {
            Room existingRoom = existingRoomOpt.get();
            // saveRoom method handles validation and setting the manager
            Room updatedRoom = saveRoom(existingRoom, newManagerEmail);
            return Optional.of(updatedRoom);
        } else {
            return Optional.empty(); // Room not found
        }
    }
    
    @Transactional
    public boolean deleteRoom(RoomId id) {
        if (existsById(id)) {
            try {
                // 1. Find all bookings associated with this room
                List<Booking> bookingsToDelete = bookingRepository.findByBlockAndRoomNo(id.getBlock(), id.getRoom());

                // 2. Delete the associated bookings
                if (!bookingsToDelete.isEmpty()) {
                    // This will also delete associated BookingApprovals if cascade is set correctly on Booking entity
                    bookingRepository.deleteAllInBatch(bookingsToDelete);
                    // or bookingRepository.deleteAll(bookingsToDelete);
                }

                // 3. Delete the room itself
                roomRepository.deleteById(id);
                return true;

            } catch (Exception e) {
                System.err.println("Error deleting room " + id + " and associated bookings: " + e.getMessage());
                // Re-throw a runtime exception to trigger transaction rollback
                throw new RuntimeException("Could not delete room and associated bookings.", e);
            }
        }
        return false; // Room not found initially
    }
}
