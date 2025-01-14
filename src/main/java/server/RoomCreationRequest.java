package server;

import model.Perms;

/**
 * Represents a request to create a new chat room in the Echat application.
 * Includes details about the requested room name, required permissions, and the user who made the request.
 */
public class RoomCreationRequest {

    /**
     * The name of the room being requested.
     */
    private String roomName;

    /**
     * The permission level required to access the room.
     */
    private Perms perms;

    /**
     * The username of the user who requested the room creation.
     */
    private String requestedBy;

    /**
     * Constructs a new {@code RoomCreationRequest} with the specified room name, permissions, and requester.
     *
     * @param roomName    The name of the room being requested.
     * @param perms       The permission level required to access the room.
     * @param requestedBy The username of the user making the request.
     */
    public RoomCreationRequest(String roomName, Perms perms, String requestedBy) {
        this.roomName = roomName;
        this.perms = perms;
        this.requestedBy = requestedBy;
    }

    /**
     * Retrieves the name of the requested room.
     *
     * @return The room name.
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * Retrieves the permission level required to access the requested room.
     *
     * @return The required permissions for the room.
     */
    public Perms getPerms() {
        return perms;
    }

    /**
     * Retrieves the username of the user who requested the room creation.
     *
     * @return The username of the requester.
     */
    public String getRequestedBy() {
        return requestedBy;
    }
}
