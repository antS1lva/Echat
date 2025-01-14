package service;

import DataPersistance.PersistenceUtil;
import model.Perms;
import model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user data and operations for the Echat application.
 * Provides synchronized methods to handle user registration, login, permission updates, and offline messaging.
 */
public class UserManager {

    /**
     * A thread-safe map of usernames to their corresponding {@link User} objects.
     */
    private final Map<String, User> usersDatabase;

    /**
     * Constructs a new UserManager with the given users database.
     * If the provided map is not a {@link ConcurrentHashMap}, it is wrapped in one for thread safety.
     *
     * @param usersDatabase The initial database of users.
     */
    public UserManager(Map<String, User> usersDatabase) {
        this.usersDatabase = (usersDatabase instanceof ConcurrentHashMap)
                ? usersDatabase
                : new ConcurrentHashMap<>(usersDatabase);
    }

    /**
     * Registers a new user with the given username, password, and initial permissions.
     *
     * @param username    The username of the new user.
     * @param password    The password of the new user.
     * @param initialPerms The initial permissions for the user.
     * @return {@code true} if the user was successfully registered; {@code false} if the username already exists.
     */
    public synchronized boolean registerUser(String username, String password, Perms initialPerms) {
        if (usersDatabase.containsKey(username)) {
            return false;
        }
        User newUser = new User(username, password, initialPerms);
        usersDatabase.put(username, newUser);
        PersistenceUtil.saveUserData(usersDatabase);
        return true;
    }

    /**
     * Logs in a user by verifying their credentials.
     *
     * @param username The username of the user.
     * @param password The password of the user.
     * @return The {@link User} object if the credentials are valid; {@code null} otherwise.
     */
    public synchronized User loginUser(String username, String password) {
        User user = usersDatabase.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * Retrieves the {@link User} object for the given username.
     *
     * @param username The username of the user.
     * @return The {@link User} object, or {@code null} if the user does not exist.
     */
    public synchronized User getUser(String username) {
        return usersDatabase.get(username);
    }

    /**
     * Adds an offline message for the specified user.
     *
     * @param username The username of the recipient.
     * @param message  The message to store.
     */
    public synchronized void addOfflineMessage(String username, String message) {
        User user = getUser(username);
        if (user != null) {
            user.addOfflineMessage(message);
            PersistenceUtil.saveUserData(usersDatabase);
        }
    }

    /**
     * Retrieves all offline messages for the specified user.
     *
     * @param username The username of the user.
     * @return A list of offline messages, or an empty list if there are no messages or the user does not exist.
     */
    public synchronized List<String> getOfflineMessages(String username) {
        User user = getUser(username);
        if (user != null) {
            return new ArrayList<>(user.getOfflineMessages());
        }
        return Collections.emptyList();
    }

    /**
     * Clears all offline messages for the specified user.
     *
     * @param username The username of the user.
     */
    public synchronized void clearOfflineMessages(String username) {
        User user = getUser(username);
        if (user != null) {
            user.clearOfflineMessages();
            PersistenceUtil.saveUserData(usersDatabase);
        }
    }

    /**
     * Updates the permissions of the specified user.
     *
     * @param username The username of the user.
     * @param perms    The new permissions for the user.
     */
    public synchronized void setUserPerms(String username, Perms perms) {
        User user = getUser(username);
        if (user != null) {
            user.setPerms(perms);
            PersistenceUtil.saveUserData(usersDatabase);
        }
    }

    /**
     * Checks if a user with the given username exists in the database.
     *
     * @param username The username to check.
     * @return {@code true} if the user exists; {@code false} otherwise.
     */
    public synchronized boolean userExists(String username) {
        return usersDatabase.containsKey(username);
    }

    /**
     * Retrieves a copy of all users in the database.
     *
     * @return A map of usernames to their corresponding {@link User} objects.
     */
    public synchronized Map<String, User> getAllUsers() {
        return new HashMap<>(usersDatabase);
    }
}
