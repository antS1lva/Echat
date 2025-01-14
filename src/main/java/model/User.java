package model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * User model class representing a user in the system.
 */
public class User {
    private static AtomicInteger idCounter = new AtomicInteger();

    @SerializedName("id")
    @Expose
    private int id;

    @SerializedName("username")
    @Expose
    private String username;

    @SerializedName("password")
    @Expose
    private String password;

    @SerializedName("perms")
    @Expose
    private Perms perms;

    @SerializedName("offlineMessages")
    @Expose
    private List<String> offlineMessages;

    /**
     * Constructs a new user with the specified username and password.
     * @param username the username of the user
     * @param password the password of the user
     */
    public User(String username, String password, Perms perms) {
        this.id = idCounter.getAndIncrement();
        this.username = username;
        this.password = password;
        this.perms = perms;
        this.offlineMessages = new CopyOnWriteArrayList<>();
    }

    public static AtomicInteger getIdCounter() {
        return idCounter;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Perms getPerms() {
        return perms;
    }

    public void setPerms(Perms perms) {
        this.perms = perms;
    }

    public void setOfflineMessages(List<String> offlineMessages) {
        this.offlineMessages = offlineMessages;
    }

    public List<String> getOfflineMessages() {
        return offlineMessages;
    }

    public void addOfflineMessage(String message) {
        offlineMessages.add(message);
    }

    public void clearOfflineMessages() {
        offlineMessages.clear();
    }
}
