package com.campusbooking.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping; // For simple update request body
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusbooking.model.Room;
import com.campusbooking.model.RoomId;
import com.campusbooking.service.FloorManagerService;
import com.campusbooking.service.RoomService;
import com.campusbooking.types.RoomRequest;

@RestController
@RequestMapping("/api/rooms") 
public class RoomController {

    private final RoomService roomService;
    private final FloorManagerService floorManagerService; 

    @Autowired
    public RoomController(RoomService roomService, FloorManagerService floorManagerService) {
        this.roomService = roomService;
        this.floorManagerService = floorManagerService;
    }

    @GetMapping
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/{block}/{room}")
    public ResponseEntity<Room> getRoomById(@PathVariable String block, @PathVariable String room) {
        // Validate path variables
        if (block == null || block.trim().isEmpty() || room == null || room.trim().isEmpty()) {
            return ResponseEntity.badRequest().build(); // Or throw ResponseStatusException
        }
        RoomId id = new RoomId(block, room);
        return roomService.getRoomById(id)
                .map(ResponseEntity::ok) // 200 OK if found
                .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found otherwise
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody RoomRequest request) {
        // 1. Basic validation of request body
        if (request.getBlock() == null || request.getBlock().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Block is required.\"}");
        }
        if (request.getRoom() == null || request.getRoom().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Room number/name is required.\"}");
        }
        if (request.getManagerEmail() == null || request.getManagerEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Manager email is required.\"}");
        }

        // 2. Check if manager exists here before calling service
        if (floorManagerService.getFloorManagerByEmail(request.getManagerEmail()).isEmpty()) {
             return ResponseEntity.badRequest()
                                  .body("{\"error\": \"Floor Manager with email '" + request.getManagerEmail() + "' not found.\"}");
        }

        // 3. Attempt to create via service
        try {
            Room createdRoom = roomService.createRoom(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRoom); // 201 Created
        } catch (IllegalStateException e) { // Catch conflict error from service
            return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\": \"" + e.getMessage() + "\"}"); // 409 Conflict
        } catch (IllegalArgumentException e) { // Catch manager not found error from service 
             return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}"); // 400 Bad Request
        } catch (Exception e) {
            System.err.println("Error creating room: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while creating the room.\"}"); // 500 Internal Server Error
        }
    }

    // Using a Map to accept simple JSON like {"managerEmail": "new.manager@example.com"}
    @PutMapping("/{block}/{room}/manager")
    public ResponseEntity<?> updateRoomManager(
            @PathVariable String block,
            @PathVariable String room,
            @RequestBody Map<String, String> payload) {

        // 1. Validate path variables
        if (block == null || block.trim().isEmpty() || room == null || room.trim().isEmpty()) {
             return ResponseEntity.badRequest().body("{\"error\": \"Block and Room must be provided in the path.\"}");
        }

        // 2. Validate request body
        String newManagerEmail = payload.get("managerEmail");
        if (newManagerEmail == null || newManagerEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"managerEmail is required in the request body.\"}");
        }

        // 3. Check if the new manager exists
        if (floorManagerService.getFloorManagerByEmail(newManagerEmail).isEmpty()) {
            return ResponseEntity.badRequest()
                                 .body("{\"error\": \"New Floor Manager with email '" + newManagerEmail + "' not found.\"}");
        }

        // 4. Attempt update via service
        RoomId id = new RoomId(block, room);
        try {
            Optional<Room> updatedRoomOpt = roomService.updateRoomManager(id, newManagerEmail);

            return updatedRoomOpt
                    .map(ResponseEntity::ok) // 200 OK with updated room
                    .orElseGet(() -> ResponseEntity.notFound().build()); // 404 if room not found by service

        } catch (IllegalArgumentException e) { // Catch manager not found error from service (if not checked above)
             return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}"); // 400 Bad Request
        } catch (Exception e) {
            System.err.println("Error updating room manager for " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"An internal error occurred while updating the room manager.\"}"); // 500 Internal Server Error
        }
    }

    @DeleteMapping("/{block}/{room}")
    public ResponseEntity<?> deleteRoom(@PathVariable String block, @PathVariable String room) {
        // 1. Validate path variables
        if (block == null || block.trim().isEmpty() || room == null || room.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Block and Room must be provided in the path.\"}");
        }

        // 2. Attempt deletion via service
        RoomId id = new RoomId(block, room);
        try {
            boolean deleted = roomService.deleteRoom(id);
            if (deleted) {
                return ResponseEntity.noContent().build(); // 204 No Content on success
            } else {
                // If service returns false, it means the room wasn't found initially
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
        } catch (RuntimeException e) { // Catch potential errors like FK constraints from service
             System.err.println("Error deleting room " + id + ": " + e.getMessage());
             // Provide a more specific error if possible, e.g., 409 Conflict if deletion is blocked
             return ResponseEntity.status(HttpStatus.CONFLICT)
                                  .body("{\"error\": \"" + e.getMessage() + "\"}"); // Or 500 for unexpected errors
        }
    }
}
