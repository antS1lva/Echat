package server;

import DataPersistance.PersistenceUtil;
import client.ClientHandler;
import model.Perms;
import model.User;
import model.Room;
import service.UserManager;
import service.RoomManager;
import service.RequestManager;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * MultiThreadedServer class contains all elements methods to start and operate the server.
 */
public class MultiThreadedServer {

    /**
     * The directory path where reports are stored.
     * This path is set to the default user directory appended with "/Echat/Reports".
     * Reports may include logs, user activity summaries, or other relevant data.
     */
    private static final String REPORTS_DIRECTORY_PATH =
            FileSystemView.getFileSystemView().getDefaultDirectory().getPath() + "/Echat/Reports";

    /**
     * The server socket used to accept incoming client connections.
     * It listens on a specified port and establishes communication channels with clients.
     */
    private ServerSocket serverSocket;

    /**
     * The timer for managing periodic tasks such as maintenance operations.
     * Examples include generating periodic reports, etc...
     */
    private Timer timer;

    /**
     * The database of users loaded from persistence.
     */
    private Map<String, User> usersDatabase;

    /**
     * The database of rooms loaded from persistence.
     */
    private Map<String, Room> roomsDatabase;

    /**
     * Manages user-related operations.
     */
    private UserManager userManager;

    /**
     * Manages room-related operations.
     */
    private RoomManager roomManager;

    /**
     * Manages room creation requests.
     */
    private RequestManager requestManager;

    /**
     * A map of active users, associating usernames with their respective PrintWriter streams for communication.
     */
    private Map<String, PrintWriter> activeUsers;

    /**
     * A flag indicating whether the server is currently running.
     */
    private volatile boolean running = true;

    /**
     * A thread-safe list holding requests for creating new chat rooms.
     */
    private List<RoomCreationRequest> pendingRoomRequests = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new MultiThreadedServer instance listening on the specified port.
     * Initializes the server socket, timer, and loads existing user and room data.
     *
     * @param port The port number on which the server will listen for client connections.
     * @throws IOException If an I/O error occurs when opening the socket.
     */
    public MultiThreadedServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        timer = new Timer(true);
        activeUsers = new ConcurrentHashMap<>();

        Map<String, Room> loadedRooms = PersistenceUtil.loadRoomData();
        roomsDatabase = (loadedRooms != null) ? loadedRooms : new ConcurrentHashMap<>();

        if (!roomsDatabase.containsKey("OEM")) {
            Room dreRoom = new Room("Distribuicao de Recursos de Emergencia", Perms.LOW);
            roomsDatabase.put("OEM", dreRoom);
        }
        if (!roomsDatabase.containsKey("ACE")) {
            Room oemRoom = new Room("Operacao de Evacuacao em Massa", Perms.HIGH);
            roomsDatabase.put("ACE", oemRoom);
        }
        if (!roomsDatabase.containsKey("DRE")) {
            Room aceRoom = new Room("Ativacao de Comunicacoes de Emergencia", Perms.MEDIUM);
            roomsDatabase.put("DRE", aceRoom);
        }

        Map<String, User> loadedData = PersistenceUtil.loadUserData();
        usersDatabase = (loadedData != null) ? loadedData : new HashMap<>();

        userManager = new UserManager(usersDatabase);
        roomManager = new RoomManager(roomsDatabase);
        requestManager = new RequestManager(pendingRoomRequests);

        startPeriodicReports(1, TimeUnit.HOURS);
    }

    /**
     * Starts the server, listening for client connections and handling console commands.
     */
    public void start() {
        System.out.println("Server started and listening on port " + serverSocket.getLocalPort());
        CommandList();

        new Thread(() -> {
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            try {
                String commandLine;
                while ((commandLine = consoleReader.readLine()) != null) {
                    handleCommand(commandLine.trim());
                }
            } catch (IOException e) {
                System.err.println("Error reading console input: " + e.getMessage());
            }
        }).start();

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, userManager, roomManager, requestManager, activeUsers);
                new Thread(clientHandler).start();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Handles server console commands.
     * @param commandLine The full command line input.
     */
    private void handleCommand(String commandLine) {
        if (commandLine.isEmpty()) {
            return;
        }

        String[] parts = commandLine.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "stop":
                stopServer();
                break;

            case "giveperms":
                handleGivePerms(parts);
                break;

            case "adduser":
                handleAddUser(parts);
                break;

            case "removeuser":
                handleRemoveUser(parts);
                break;

            case "listusers":
                handleListUsers();
                break;

            case "notify":
                handleNotify(parts);
                break;

            case "createroom":
                handleCreateRoom(parts);
                break;

            case "help":
                CommandList();
                break;

            default:
                System.out.println("Unknown command. Type help for a list of commands.");
        }
    }

    /**
     * Handles the GIVE_PERMS command to set a user's permissions.
     * Usage: GIVE_PERMS <username> <PERMS>
     */
    private void handleGivePerms(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Usage: givePerms <username> <PERMS>");
            return;
        }

        String username = parts[1];
        String permsStr = parts[2].toUpperCase();

        Perms newPerms;
        try {
            newPerms = Perms.valueOf(permsStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid permission level. Available levels: LOW, MEDIUM, HIGH");
            return;
        }

        User user = userManager.getUser(username);
        if (user == null) {
            System.out.println("User '" + username + "' does not exist.");
            return;
        }

        userManager.setUserPerms(username, newPerms);
        System.out.println("Updated permissions for user '" + username + "' to " + newPerms);
    }

    /**
     * Handles the CREATE_ROOM server command to create a new chat room.
     * Usage: createRoom <roomName> <PERMS>
     */
    private void handleCreateRoom(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Usage: createRoom <roomName> <PERMS>");
            return;
        }

        String roomName = parts[1];
        String permsStr = parts[2].toUpperCase();

        Perms requiredPerms;
        try {
            requiredPerms = Perms.valueOf(permsStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid permission level. Available levels: LOW, MEDIUM, HIGH");
            return;
        }

        boolean created = roomManager.createRoom(roomName, requiredPerms);
        if (!created) {
            System.out.println("Room '" + roomName + "' already exists.");
            return;
        }

        System.out.println("Room '" + roomName + "' created with required permissions: " + requiredPerms);
        broadcastNewRoom(roomName, requiredPerms);
    }

    /**
     * Broadcasts a notification about the new room to all active users.
     */
    private void broadcastNewRoom(String roomName, Perms permsRequired) {
        String notification = "A new room '" + roomName + "' has been created. Required permissions: " + permsRequired;
        activeUsers.forEach((username, writer) -> {
            writer.println("[SERVER] " + notification);
        });
    }

    /**
     * Handles the ADD_USER command to add a new user.
     * Usage: ADD_USER <username> <password> <PERMS>
     */
    private void handleAddUser(String[] parts) {
        if (parts.length != 4) {
            System.out.println("Usage: addUser <username> <password> <PERMS>");
            return;
        }

        String username = parts[1];
        String password = parts[2];
        String permsStr = parts[3].toUpperCase();

        Perms perms;
        try {
            perms = Perms.valueOf(permsStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid permission level. Available levels: LOW, MEDIUM, HIGH");
            return;
        }

        boolean success = userManager.registerUser(username, password, perms);
        if (!success) {
            System.out.println("User '" + username + "' already exists.");
        } else {
            System.out.println("Added new user '" + username + "' with permissions " + perms);
        }
    }

    /**
     * Handles the REMOVE_USER command to remove an existing user.
     * Usage: REMOVE_USER <username>
     */
    private void handleRemoveUser(String[] parts) {
        if (parts.length != 2) {
            System.out.println("Usage: removeUser <username>");
            return;
        }

        String username = parts[1];

        User user = userManager.getUser(username);
        if (user == null) {
            System.out.println("User '" + username + "' does not exist.");
            return;
        }

        synchronized (usersDatabase) {
            usersDatabase.remove(username);
        }
        PersistenceUtil.saveUserData(usersDatabase);

        PrintWriter activeUser = activeUsers.remove(username);
        if (activeUser != null) {
            activeUser.println("You have been removed from the server.");
            activeUser.close();
        }

        System.out.println("Removed user '" + username + "'.");
    }

    /**
     * Handles the LIST_USERS command to display all users.
     * Usage: LIST_USERS
     */
    private void handleListUsers() {
        Map<String, User> allUsers = userManager.getAllUsers();
        if (allUsers.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        System.out.println("List of users:");
        allUsers.values().forEach(user -> {
            System.out.println("Username: " + user.getUsername() + ", Permissions: " + user.getPerms());
        });
    }

    /**
     * Handles the NOTIFY command to send notifications to users.
     * Usage: NOTIFY <PERMS> <message>
     */
    private void handleNotify(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Usage: notify <PERMS> <message>");
            return;
        }

        String permsStr = parts[1].toUpperCase();
        String message = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));

        Perms targetPerms;
        try {
            targetPerms = Perms.valueOf(permsStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid permission level. Available levels: LOW, MEDIUM, HIGH");
            return;
        }

        sendNotification(message, targetPerms);
    }

    /**
     * Sends a notification to all active users with the specified permission level or higher.
     */
    public void sendNotification(String message, Perms minPerms) {
        String formattedMessage = "[ALERT] " + message;
        activeUsers.forEach((username, writer) -> {
            User user = userManager.getUser(username);
            if (user != null && user.getPerms().ordinal() >= minPerms.ordinal()) {
                writer.println(formattedMessage);
            }
        });
        System.out.println("Notification sent to users with permissions " + minPerms + " or higher.");
    }

    /**
     * Starts the periodic report generation at the specified interval.
     */
    private void startPeriodicReports(long interval, TimeUnit unit) {
        File reportsDirectory = new File(REPORTS_DIRECTORY_PATH);
        if (!reportsDirectory.exists()) {
            reportsDirectory.mkdirs();
        }

        long intervalInMillis = unit.toMillis(interval);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                generateAndSaveReport();
            }
        }, intervalInMillis, intervalInMillis);

        System.out.println("Periodic reports scheduled every " + interval + " " + unit.toString().toLowerCase());
    }

    /**
     * Generates a server report and sends it to users with HIGH permissions.
     */
    private void generateAndSaveReport() {
        Map<String, User> allUsers = userManager.getAllUsers();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("----- Server Report - ").append(timestamp).append(" -----\n");

        reportBuilder.append("Total Users: ").append(allUsers.size()).append("\n");
        reportBuilder.append("Active Users: ").append(activeUsers.size()).append("\n");
        reportBuilder.append("Active Users:\n");
        activeUsers.keySet().forEach(username -> {
            User user = userManager.getUser(username);
            if (user != null) {
                reportBuilder.append("- ").append(username)
                        .append(" (").append(user.getPerms()).append(")\n");
            }
        });

        reportBuilder.append("----- End of Report -----\n");

        String report = reportBuilder.toString();
        saveReportToFile(report, timestamp);

        activeUsers.forEach((username, writer) -> {
            User user = userManager.getUser(username);
            if (user != null && user.getPerms() == Perms.HIGH) {
                writer.println("[REPORT] " + report);
            }
        });

        System.out.println("Periodic report generated and sent to users with HIGH permissions.");
    }

    /**
     * Saves the generated report to a file in the reports directory.
     */
    private void saveReportToFile(String report, String timestamp) {
        String reportFilePath = REPORTS_DIRECTORY_PATH + "/EchatReport_" + timestamp + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFilePath))) {
            writer.write(report);
            System.out.println("Report saved to: " + reportFilePath);
        } catch (IOException e) {
            System.err.println("Error saving report to file: " + e.getMessage());
        }
    }

    /**
     * Prints the list of available commands.
     */
    private void CommandList() {
        System.out.println("Available commands:");
        System.out.println("- stop: Stops the server.");
        System.out.println("- givePerms <username> <PERMS>: Sets the permissions for a user.");
        System.out.println("- addUser <username> <password> <PERMS>: Adds a new user.");
        System.out.println("- removeUser <username>: Removes the specified user.");
        System.out.println("- listUsers: Lists all registered users and their permissions.");
        System.out.println("- notify <PERMS> <message>: Sends a notification to users with the specified permissions or higher.");
        System.out.println("- createRoom <roomName> <PERMS>: Creates a new chat room.");
        System.out.println("- help: Displays this help message.");
    }

    /**
     * Stops the server gracefully.
     */
    public void stopServer() {
        System.out.println("Stopping server...");
        running = false;
        try {
            serverSocket.close();
            timer.cancel();
        } catch (IOException e) {
        } finally {
            System.exit(0);
        }
    }

    /**
     * The entry point for the MultiThreadedServer application.
     */
    public static void main(String[] args) {
        try {
            MultiThreadedServer server = new MultiThreadedServer(42069);
            server.start();
        } catch (IOException e) {
            System.err.println("Server could not start: " + e.getMessage());
        }
    }
}
