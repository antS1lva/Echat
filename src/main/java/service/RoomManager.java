package service;

import DataPersistance.PersistenceUtil;
import model.Perms;
import model.Room;
import model.User;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chat rooms in the Echat application.
 * This class provides synchronized methods to handle room creation, user access, message broadcasting, and client management.
 */
public class RoomManager {

    /**
     * A thread-safe map of room names to their corresponding {@link Room} objects.
     */
    private final Map<String, Room> roomsDatabase;

    /**
     * Constructs a new RoomManager with the given rooms database.
     * If the provided map is not a {@link ConcurrentHashMap}, it is wrapped in one for thread safety.
     *
     * @param roomsDatabase The initial database of rooms.
     */
    public RoomManager(Map<String, Room> roomsDatabase) {
        this.roomsDatabase = (roomsDatabase instanceof ConcurrentHashMap)
                ? roomsDatabase
                : new ConcurrentHashMap<>(roomsDatabase);
    }

    /**
     * Creates a new chat room with the given name and permissions.
     *
     * @param roomName The name of the room to create.
     * @param perms    The required permissions to join the room.
     * @return {@code true} if the room was successfully created; {@code false} if a room with the same name already exists.
     */
    public synchronized boolean createRoom(String roomName, Perms perms) {
        if (roomsDatabase.containsKey(roomName)) {
            return false;
        }
        Room newRoom = new Room(roomName, perms);
        roomsDatabase.put(roomName, newRoom);
        PersistenceUtil.saveRoomData(roomsDatabase);
        return true;
    }

    /**
     * Lists the names of all available rooms.
     *
     * @return A list of all room names.
     */
    public synchronized List<String> listRooms() {
        return new ArrayList<>(roomsDatabase.keySet());
    }

    /**
     * Retrieves the {@link Room} object for the specified room name.
     *
     * @param roomName The name of the room.
     * @return The corresponding {@link Room}, or {@code null} if the room does not exist.
     */
    public synchronized Room getRoom(String roomName) {
        return roomsDatabase.get(roomName);
    }

    /**
     * Determines whether a user can join a specified room based on their permission level.
     *
     * @param user     The {@link User} attempting to join.
     * @param roomName The name of the room.
     * @return {@code true} if the user has sufficient permissions to join the room; {@code false} otherwise.
     */
    public synchronized boolean canUserJoinRoom(User user, String roomName) {
        Room room = roomsDatabase.get(roomName);
        if (room == null) return false;
        return user.getPerms().ordinal() >= room.getPermsRequired().ordinal();
    }

    /**
     * Adds a client to the specified room.
     *
     * @param roomName The name of the room.
     * @param writer   The {@link PrintWriter} associated with the client.
     */
    public synchronized void addClientToRoom(String roomName, PrintWriter writer) {
        Room room = roomsDatabase.get(roomName);
        if (room != null) {
            room.addClient(writer);
        }
    }

    /**
     * Removes a client from the specified room.
     *
     * @param roomName The name of the room.
     * @param writer   The {@link PrintWriter} associated with the client.
     */
    public synchronized void removeClientFromRoom(String roomName, PrintWriter writer) {
        Room room = roomsDatabase.get(roomName);
        if (room != null) {
            room.removeClient(writer);
        }
    }

    /**
     * Broadcasts a message to all clients in the specified room.
     * Optionally saves the message in the room's message history.
     *
     * @param roomName         The name of the room.
     * @param message          The message to broadcast.
     * @param includeInHistory {@code true} to save the message in the room's history; {@code false} otherwise.
     */
    public synchronized void broadcastToRoom(String roomName, String message, boolean includeInHistory) {
        Room room = roomsDatabase.get(roomName);
        if (room == null) return;

        if (includeInHistory) {
            room.addMessageToHistory(message);
        }

        List<PrintWriter> roomClients = room.getClients();
        for (PrintWriter pw : roomClients) {
            pw.println(message);
        }
        System.out.println("Broadcast to room " + roomName + ": " + message);
    }

    /**
     * Retrieves the message history of a specified room.
     *
     * @param roomName The name of the room.
     * @return A list of messages from the room's history, or an empty list if the room does not exist or has no history.
     */
    public synchronized List<String> getRoomMessageHistory(String roomName) {
        Room room = roomsDatabase.get(roomName);
        if (room != null) {
            return new ArrayList<>(room.getMessageHistory());
        }
        return Collections.emptyList();
    }
}
