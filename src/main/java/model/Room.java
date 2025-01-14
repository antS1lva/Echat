package model;

import DataPersistance.PersistenceUtil;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Room model class representing a chat room in the system.
 */
public class Room {

    /**
     * The name of the chat room.
     * This identifier is used to reference the room within the server and by clients.
     */
    @SerializedName("name")
    @Expose
    private String name;

    /**
     * The permission level required to join or interact within the chat room.
     * Determines which users have access based on their assigned permissions.
     */
    @SerializedName("permsRequired")
    @Expose
    private Perms permsRequired;

    /**
     * A list of PrintWriter streams corresponding to clients currently connected to the room.
     * This transient field is not serialized, as PrintWriter instances are not serializable.
     * It facilitates real-time message broadcasting to all connected clients.
     */
    private transient List<PrintWriter> clients;

    /**
     * The history of messages sent within the chat room.
     * Maintains a list of recent messages for reference and retrieval by clients.
     */
    @SerializedName("messageHistory")
    @Expose
    private List<String> messageHistory;

    /**
     * The default limit for the number of messages stored in memory for message history.
     * Determines how many recent messages are retained before older messages are discarded.
     */
    private static final int DEFAULT_IN_MEMORY_HISTORY_LIMIT = 100;

    /**
     * Constructs a new Room with the specified name and required permissions.
     * @param name the name of the room
     * @param permsRequired the permission level required to join the room
     */
    public Room(String name, Perms permsRequired) {
        this.name = name;
        this.permsRequired = permsRequired;
        this.clients = new CopyOnWriteArrayList<>();
        this.messageHistory = new CopyOnWriteArrayList<>();
        trimInMemoryHistory();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPermsRequired(Perms permsRequired) {
        this.permsRequired = permsRequired;
    }

    public void setClients(List<PrintWriter> clients) {
        this.clients = clients;
    }

    public String getName() {
        return name;
    }

    public Perms getPermsRequired() {
        return permsRequired;
    }

    public List<PrintWriter> getClients() {
        return clients;
    }

    public List<String> getMessageHistory() {
        return messageHistory;
    }

    /**
     * Sets the message history for the room.
     * @param messageHistory the list of messages to set
     */
    public void setMessageHistory(List<String> messageHistory) {
        this.messageHistory = new CopyOnWriteArrayList<>(messageHistory);
        trimInMemoryHistory();
    }

    /**
     * Adds a message to the history and persists it.
     * @param message the message to add
     */
    public void addMessageToHistory(String message) {
        messageHistory.add(message);
        PersistenceUtil.saveRoomMessages(this);
        trimInMemoryHistory();
    }

    /**
     * Trims the in-memory message history to the defined limit.
     */
    private void trimInMemoryHistory() {
        if (messageHistory.size() > DEFAULT_IN_MEMORY_HISTORY_LIMIT) {
            int excess = messageHistory.size() - DEFAULT_IN_MEMORY_HISTORY_LIMIT;
            for (int i = 0; i < excess; i++) {
                messageHistory.remove(0);
            }
        }
    }

    /**
     * Adds a client to the room.
     * @param clientWriter the PrintWriter of the client to add
     */
    public void addClient(PrintWriter clientWriter) {
        clients.add(clientWriter);
    }

    /**
     * Removes a client from the room.
     * @param clientWriter the PrintWriter of the client to remove
     */
    public void removeClient(PrintWriter clientWriter) {
        clients.remove(clientWriter);
    }

    /**
     * Initializes transient fields after deserialization.
     */
    public void initializeTransientFields() {
        this.clients = new CopyOnWriteArrayList<>();
    }
}
