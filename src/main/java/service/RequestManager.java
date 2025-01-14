package service;

import model.Perms;
import server.RoomCreationRequest;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages room creation requests for the chat application.
 * This class provides synchronized methods to handle adding, removing, and retrieving room creation requests.
 */
public class RequestManager {

    /**
     * A thread-safe list of pending room creation requests.
     */
    private final List<RoomCreationRequest> pendingRoomRequests;

    /**
     * Constructs a new RequestManager instance with a given list of pending room requests.
     * If the provided list is not a {@link CopyOnWriteArrayList}, it is wrapped in one for thread safety.
     *
     * @param pendingRoomRequests The initial list of pending room requests.
     */
    public RequestManager(List<RoomCreationRequest> pendingRoomRequests) {
        this.pendingRoomRequests = (pendingRoomRequests instanceof CopyOnWriteArrayList)
                ? pendingRoomRequests
                : new CopyOnWriteArrayList<>(pendingRoomRequests);
    }

    /**
     * Adds a new room creation request to the pending requests list.
     *
     * @param requestedBy The username of the user requesting the room creation.
     * @param roomName    The name of the room to be created.
     * @param perms       The required permissions for accessing the room.
     */
    public synchronized void requestRoomCreation(String requestedBy, String roomName, Perms perms) {
        RoomCreationRequest request = new RoomCreationRequest(roomName, perms, requestedBy);
        pendingRoomRequests.add(request);
    }

    /**
     * Adds an existing room creation request to the pending requests list.
     *
     * @param request The room creation request to be added.
     */
    public synchronized void addRequest(RoomCreationRequest request) {
        pendingRoomRequests.add(request);
    }

    /**
     * Finds a room creation request by its room name.
     *
     * @param roomName The name of the room to search for.
     * @return The corresponding {@link RoomCreationRequest}, or {@code null} if no request is found.
     */
    public synchronized RoomCreationRequest findRequestByName(String roomName) {
        for (RoomCreationRequest req : pendingRoomRequests) {
            if (req.getRoomName().equals(roomName)) {
                return req;
            }
        }
        return null;
    }

    /**
     * Removes a specific room creation request from the pending requests list.
     *
     * @param req The room creation request to be removed.
     */
    public synchronized void removeRequest(RoomCreationRequest req) {
        pendingRoomRequests.remove(req);
    }

    /**
     * Retrieves a copy of all pending room creation requests.
     *
     * @return A list of all pending room creation requests.
     */
    public synchronized List<RoomCreationRequest> getPendingRequests() {
        return new ArrayList<>(pendingRoomRequests);
    }
}
