package DataPersistance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import model.User;
import model.Room;

import javax.swing.filechooser.FileSystemView;

/**
 * PersistenceUtil contains all data related utils ex: save / load.
 */
public class PersistenceUtil {

    /**
     * The Gson instance used for JSON serialization and deserialization.
     * Configured to exclude fields without @Expose annotation.
     */
    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    /**
     * The directory path where Echat-related data and reports are stored.
     * This path is set to the default user directory appended with "/Echat".
     */
    private static final String DIRECTORY_PATH =
            FileSystemView.getFileSystemView().getDefaultDirectory().getPath() + "/Echat";

    /**
     * The file path for storing user data in JSON format.
     * Located within the Echat directory as "userData.json".
     * This file maintains information about registered users.
     */
    private static final String USER_DATA_FILE = DIRECTORY_PATH + "/userData.json";

    /**
     * The directory path where chat room data is stored.
     * Located within the Echat directory as "Rooms".
     * Each chat room may have its own data files within this directory.
     */
    private static final String ROOM_DATA_DIRECTORY = DIRECTORY_PATH + "/Rooms";

    /**
     * The file path for storing chat room data in JSON format.
     * Located within the Rooms directory as "roomsData.json".
     * This file maintains information about existing chat rooms.
     */
    private static final String ROOMS_DATA_FILE = ROOM_DATA_DIRECTORY + "/roomsData.json";

    /**
     * Saves the user database to a JSON file.
     * @param usersDatabase the map of users to save
     */
    public static void saveUserData(Map<String, User> usersDatabase) {
        File directory = new File(DIRECTORY_PATH);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        try (Writer writer = new FileWriter(USER_DATA_FILE)) {
            gson.toJson(usersDatabase, writer);
        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
        }
    }

    /**
     * Loads the user database from a JSON file.
     * @return the map of users loaded from the file
     */
    public static Map<String, User> loadUserData() {
        File file = new File(USER_DATA_FILE);

        if (!file.exists()) {
            try {
                new File(DIRECTORY_PATH).mkdirs();
                file.createNewFile();
                System.out.println("User data file created as it did not exist.");
                return new ConcurrentHashMap<>();
            } catch (IOException e) {
                System.err.println("Error creating user data file: " + e.getMessage());
                return new ConcurrentHashMap<>();
            }
        }

        try (Reader reader = new FileReader(USER_DATA_FILE)) {
            Type userMapType = new TypeToken<Map<String, User>>() {}.getType();
            Map<String, User> users = gson.fromJson(reader, userMapType);
            return (users != null) ? new ConcurrentHashMap<>(users) : new ConcurrentHashMap<>();
        } catch (IOException e) {
            System.err.println("Error loading user data: " + e.getMessage());
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * Saves the message history for a specific room.
     * @param room the room whose messages are to be saved
     */
    public static void saveRoomMessages(Room room) {
        File roomDir = new File(ROOM_DATA_DIRECTORY);
        if (!roomDir.exists()) {
            roomDir.mkdirs();
        }

        String roomFilePath = ROOM_DATA_DIRECTORY + "/" + room.getName() + "_messages.json";
        try (Writer writer = new FileWriter(roomFilePath)) {
            gson.toJson(room.getMessageHistory(), writer);
        } catch (IOException e) {
            System.err.println("Error saving messages for room " + room.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Loads the message history for a specific room.
     * @param room the room whose messages are to be loaded
     */
    public static void loadRoomMessages(Room room) {
        File roomFile = new File(ROOM_DATA_DIRECTORY + "/" + room.getName() + "_messages.json");

        if (!roomFile.exists()) {
            try {
                roomFile.getParentFile().mkdirs();
                roomFile.createNewFile();
                System.out.println("Message file created for room: " + room.getName());
                return;
            } catch (IOException e) {
                System.err.println("Error creating message file for room " + room.getName() + ": " + e.getMessage());
                return;
            }
        }

        try (Reader reader = new FileReader(roomFile)) {
            Type messageListType = new TypeToken<List<String>>() {}.getType();
            List<String> messages = gson.fromJson(reader, messageListType);
            if (messages != null) {
                room.setMessageHistory(messages);
            }
        } catch (IOException e) {
            System.err.println("Error loading messages for room " + room.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Saves the rooms metadata to a JSON file.
     * @param roomsDatabase the map of rooms to save
     */
    public static void saveRoomData(Map<String, Room> roomsDatabase) {
        File roomDir = new File(ROOM_DATA_DIRECTORY);
        if (!roomDir.exists()) {
            roomDir.mkdirs();
        }

        try (Writer writer = new FileWriter(ROOMS_DATA_FILE)) {
            gson.toJson(roomsDatabase, writer);
            System.out.println("Room data saved successfully.");
        } catch (IOException e) {
            System.err.println("Error saving room data: " + e.getMessage());
        }
    }

    /**
     * Loads the rooms metadata from a JSON file.
     * @return the map of rooms loaded from the file, or an empty map if none exist
     */
    public static Map<String, Room> loadRoomData() {
        File file = new File(ROOMS_DATA_FILE);

        if (!file.exists()) {
            try {
                new File(ROOM_DATA_DIRECTORY).mkdirs();
                file.createNewFile();
                System.out.println("Rooms data file created as it did not exist.");
                return new ConcurrentHashMap<>();
            } catch (IOException e) {
                System.err.println("Error creating rooms data file: " + e.getMessage());
                return new ConcurrentHashMap<>();
            }
        }

        try (Reader reader = new FileReader(file)) {
            Type roomMapType = new TypeToken<Map<String, Room>>() {}.getType();
            Map<String, Room> rooms = gson.fromJson(reader, roomMapType);
            if (rooms != null) {
                for (Room room : rooms.values()) {
                    room.initializeTransientFields();
                    loadRoomMessages(room);
                }
                return new ConcurrentHashMap<>(rooms);
            } else {
                return new ConcurrentHashMap<>();
            }
        } catch (IOException e) {
            System.err.println("Error loading rooms data: " + e.getMessage());
            return new ConcurrentHashMap<>();
        }
    }

}
