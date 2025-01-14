package client;

import DataPersistance.PersistenceUtil;
import model.Perms;
import model.Room;
import model.User;
import server.RoomCreationRequest;
import service.RequestManager;
import service.RoomManager;
import service.UserManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * Handles communication with a client.
 */
public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private final UserManager userManager;
    private final RoomManager roomManager;
    private final RequestManager requestManager;
    private final Map<String, PrintWriter> activeUsers;

    private static final Perms initialPerms = Perms.LOW;

    private boolean isAuthenticated = false;
    private String loggedInUser;
    private String currentRoomName;
    private PrintWriter output;

    public ClientHandler(Socket clientSocket,
                         UserManager userManager,
                         RoomManager roomManager,
                         RequestManager requestManager,
                         Map<String, PrintWriter> activeUsers) {
        this.clientSocket = clientSocket;
        this.userManager = userManager;
        this.roomManager = roomManager;
        this.requestManager = requestManager;
        this.activeUsers = activeUsers;
    }

    private void CommandList() {
        output.println("Available commands:");
        output.println("- register <username> <password>: Register a new user.");
        output.println("- login <username> <password>: Log in with your credentials.");
        output.println("- join <room_name>: Join a room with the specified name.");
        output.println("- createroom <room_name> <PERMS>: Request creation of a new room.");
        if (isAuthenticated && userManager.getUser(loggedInUser).getPerms() == Perms.HIGH) {
            output.println("- approveroom <roomName>: Approve a pending room creation request.");
            output.println("- denyroom <roomName>: Deny a pending room creation request.");
            output.println("- listroomrequests: List all pending room creation requests.");
        }
        output.println("- msg <username> <message>: Send a direct message to another user.");
        output.println("- leave: Leave the current room.");
        output.println("- listrooms: List all available chat rooms.");
        output.println("- invite <username>: Invite a user to join your current room.");
        output.println("- users: List all active users in the current room.");
        output.println("- logout: Log out from the current session.");
        output.println("- help: Show this list of commands.");
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            output.println("Welcome! Please register or login.");
            CommandList();

            String message;
            while ((message = input.readLine()) != null) {
                handleClientMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Error handling client communication: " + e.getMessage());
        } finally {
            if (isAuthenticated) {
                synchronized (activeUsers) {
                    activeUsers.remove(loggedInUser);
                }
            }
            closeClientSocket();
        }
    }

    private void handleLeaveCommand() {
        if (currentRoomName != null) {
            roomManager.removeClientFromRoom(currentRoomName, output);
            roomManager.broadcastToRoom(currentRoomName, loggedInUser + " has left the room.", false);
            output.println("You have left the room: " + currentRoomName);
            currentRoomName = null;
        } else {
            output.println("You are not currently in any room.");
        }
    }

    private void handleListRoomsCommand() {
        output.println("Available chat rooms:");
        List<String> rooms = roomManager.listRooms();
        for (String roomName : rooms) {
            output.println("- " + roomName + " (Requires " + roomManager.getRoom(roomName).getPermsRequired() + " permissions)");
        }
    }

    private void handleListUsersCommand() {
        if (currentRoomName != null) {
            output.println("Active users in room " + currentRoomName + ":");
            synchronized (activeUsers) {
                roomManager.getRoom(currentRoomName).getClients().forEach(clientWriter -> {
                    String username = getUsernameByWriter(clientWriter);
                    if (username != null) {
                        output.println("- " + username);
                    }
                });
            }
        } else {
            output.println("You are not in a room. Use the join command to join a room.");
        }
    }

    private String getUsernameByWriter(PrintWriter writer) {
        synchronized (activeUsers) {
            for (Map.Entry<String, PrintWriter> entry : activeUsers.entrySet()) {
                if (entry.getValue().equals(writer)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private void handleLogoutCommand() {
        output.println("You have been logged out.");
        if (currentRoomName != null) {
            handleLeaveCommand();
        }
        synchronized (activeUsers) {
            activeUsers.remove(loggedInUser);
        }
        isAuthenticated = false;
        loggedInUser = null;
    }

    private void handleClientMessage(String message) {
        String[] commandParts = message.split(" ", 3);
        if (commandParts.length > 0) {
            if (!isAuthenticated) {
                handleAuthenticationCommands(commandParts);
            } else {
                handleAuthenticatedCommands(commandParts);
            }
        }
    }

    private void handleAuthenticationCommands(String[] commandParts) {
        switch (commandParts[0].toLowerCase()) {
            case "register":
                handleRegisterCommand(commandParts);
                break;
            case "login":
                handleLoginCommand(commandParts);
                break;
            default:
                output.println("Please register or login first.");
                break;
        }
    }

    private void handleRegisterCommand(String[] commandParts) {
        if (commandParts.length == 3) {
            String username = commandParts[1];
            String password = commandParts[2];
            boolean success = userManager.registerUser(username, password, initialPerms);
            if (success) {
                output.println("Registration successful for user: " + username);
            } else {
                output.println("Username already exists.");
            }
        } else {
            output.println("Invalid register command. Usage: register <username> <password>");
        }
    }

    private void handleLoginCommand(String[] commandParts) {
        if (commandParts.length == 3) {
            String username = commandParts[1];
            String password = commandParts[2];
            User user = userManager.loginUser(username, password);
            if (user != null) {
                isAuthenticated = true;
                loggedInUser = username;
                output.println("Login successful. Welcome, " + username + "! You can now chat.");
                synchronized (activeUsers) {
                    activeUsers.put(username, output);
                }
                List<String> offlineMessages = userManager.getOfflineMessages(username);
                if (!offlineMessages.isEmpty()) {
                    output.println("You have " + offlineMessages.size() + " new messages:");
                    for (String msg : offlineMessages) {
                        output.println(msg);
                    }
                    userManager.clearOfflineMessages(username);
                }
            } else {
                output.println("Invalid username or password.");
            }
        } else {
            output.println("Invalid login command. Usage: login <username> <password>");
        }
    }

    private void handleAuthenticatedCommands(String[] commandParts) {
        switch (commandParts[0].toLowerCase()) {
            case "join":
                if (commandParts.length == 2) {
                    handleJoinRoomCommand(commandParts[1]);
                } else {
                    output.println("Invalid join command. Usage: join <room_name>");
                }
                break;
            case "leave":
                handleLeaveCommand();
                break;
            case "listrooms":
                handleListRoomsCommand();
                break;
            case "users":
                handleListUsersCommand();
                break;
            case "logout":
                handleLogoutCommand();
                break;
            case "msg":
                if (commandParts.length == 3) {
                    handleDirectMessageCommand(commandParts[1], commandParts[2]);
                } else {
                    output.println("Invalid msg command. Usage: msg <username> <message>");
                }
                break;
            case "createroom":
                if (commandParts.length == 3) {
                    handleCreateRoomRequest(commandParts[1], commandParts[2]);
                } else {
                    output.println("Invalid createroom command. Usage: createroom <roomName> <PERMS>");
                }
                break;
            case "approveroom":
                if (commandParts.length == 2) {
                    handleApproveRoomCommand(commandParts[1]);
                } else {
                    output.println("Invalid approveroom command. Usage: approveroom <roomName>");
                }
                break;
            case "denyroom":
                if (commandParts.length == 2) {
                    handleDenyRoomCommand(commandParts[1]);
                } else {
                    output.println("Invalid denyroom command. Usage: denyroom <roomName>");
                }
                break;
            case "listroomrequests":
                handleListRoomRequests();
                break;
            case "invite":
                if (commandParts.length == 2) {
                    handleInviteCommand(commandParts[1]);
                } else {
                    output.println("Invalid invite command. Usage: invite <username>");
                }
                break;
            case "help":
                CommandList();
                break;
            default:
                if (currentRoomName != null) {
                    roomManager.broadcastToRoom(currentRoomName, loggedInUser + ": " + String.join(" ", commandParts), true);
                } else {
                    output.println("You are not in a room. Use the join command to join a room.");
                }
                break;
        }
    }

    private void handleInviteCommand(String username) {
        if (currentRoomName == null) {
            output.println("You are not in a room. Join a room first to invite users.");
            return;
        }

        User targetUser = userManager.getUser(username);
        if (targetUser == null) {
            output.println("User '" + username + "' does not exist.");
            return;
        }

        User currentUser = userManager.getUser(loggedInUser);
        Room currentRoom = roomManager.getRoom(currentRoomName);

        if (targetUser.getPerms().ordinal() < currentRoom.getPermsRequired().ordinal()) {
            output.println("User '" + username + "' does not have sufficient permissions to join this room.");
            return;
        }

        synchronized (activeUsers) {
            PrintWriter recipientWriter = activeUsers.get(username);
            if (recipientWriter != null) {
                recipientWriter.println("You have been invited by " + loggedInUser + " to join room '" + currentRoomName + "'. Use 'join " + currentRoomName + "' to join.");
                output.println("Invitation sent to '" + username + "'.");
            } else {
                userManager.addOfflineMessage(username, "You have been invited by " + loggedInUser + " to join room '" + currentRoomName + "'. Use 'join " + currentRoomName + "' to join.");
                output.println("User '" + username + "' is offline. Invitation will be delivered when they come online.");
            }
        }
    }

    private void handleApproveRoomCommand(String roomName) {
        User user = userManager.getUser(loggedInUser);
        if (user.getPerms() != Perms.HIGH) {
            output.println("You do not have permission to approve room creation requests.");
            return;
        }

        synchronized (requestManager) {
            RoomCreationRequest requestToApprove = requestManager.findRequestByName(roomName);
            if (requestToApprove != null) {
                boolean created = roomManager.createRoom(requestToApprove.getRoomName(), requestToApprove.getPerms());
                if (created) {
                    requestManager.removeRequest(requestToApprove);

                    PrintWriter requesterWriter = activeUsers.get(requestToApprove.getRequestedBy());
                    if (requesterWriter != null) {
                        requesterWriter.println("Your room creation request for '" + requestToApprove.getRoomName() + "' has been approved.");
                    }

                    output.println("Room '" + requestToApprove.getRoomName() + "' has been approved and created.");
                } else {
                    output.println("Room already exists. Cannot approve request.");
                }
            } else {
                output.println("No pending room creation request for room '" + roomName + "'.");
            }
        }
    }

    private void handleDenyRoomCommand(String roomName) {
        User user = userManager.getUser(loggedInUser);
        if (user.getPerms() != Perms.HIGH) {
            output.println("You do not have permission to deny room creation requests.");
            return;
        }

        synchronized (requestManager) {
            RoomCreationRequest requestToDeny = requestManager.findRequestByName(roomName);
            if (requestToDeny != null) {
                requestManager.removeRequest(requestToDeny);

                PrintWriter requesterWriter = activeUsers.get(requestToDeny.getRequestedBy());
                if (requesterWriter != null) {
                    requesterWriter.println("Your room creation request for '" + requestToDeny.getRoomName() + "' has been denied.");
                }

                output.println("Room creation request for '" + requestToDeny.getRoomName() + "' has been denied.");
            } else {
                output.println("No pending room creation request for room '" + roomName + "'.");
            }
        }
    }

    private void handleListRoomRequests() {
        User user = userManager.getUser(loggedInUser);
        if (user.getPerms() != Perms.HIGH) {
            output.println("You do not have permission to list room creation requests.");
            return;
        }

        List<RoomCreationRequest> requests = requestManager.getPendingRequests();
        if (requests.isEmpty()) {
            output.println("No pending room creation requests.");
        } else {
            output.println("Pending room creation requests:");
            for (RoomCreationRequest request : requests) {
                output.println("- Room Name: " + request.getRoomName() + ", Permissions: " + request.getPerms() + ", Requested by: " + request.getRequestedBy());
            }
        }
    }

    private void handleCreateRoomRequest(String roomName, String permsStr) {
        Perms requiredPerms;
        try {
            requiredPerms = Perms.valueOf(permsStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            output.println("Invalid permission level. Available levels: LOW, MEDIUM, HIGH");
            return;
        }

        User user = userManager.getUser(loggedInUser);
        if (user == null) {
            output.println("User not found. Please log in again.");
            return;
        }

        if (user.getPerms().ordinal() < requiredPerms.ordinal()) {
            output.println("You cannot request a room with a higher permission level than your own.");
            return;
        }

        synchronized (requestManager) {
            if (roomManager.getRoom(roomName) != null) {
                output.println("Room '" + roomName + "' already exists.");
                return;
            }

            requestManager.requestRoomCreation(loggedInUser, roomName, requiredPerms);
        }

        notifyHighPermissionUsersOfRoomRequest(roomName, requiredPerms);
        output.println("Room creation request submitted. Waiting for approval from a HIGH permission user.");
    }

    private void notifyHighPermissionUsersOfRoomRequest(String roomName, Perms perms) {
        String notification = "Room creation request: '" + roomName + "' with permissions " + perms + " requested by " + loggedInUser;
        synchronized (activeUsers) {
            activeUsers.forEach((username, writer) -> {
                User user = userManager.getUser(username);
                if (user != null && user.getPerms() == Perms.HIGH) {
                    writer.println("[REQUEST] " + notification);
                    writer.println("Use 'approveroom " + roomName + "' to approve or 'denyroom " + roomName + "' to deny.");
                }
            });
        }
    }

    private void handleJoinRoomCommand(String roomName) {
        User user = userManager.getUser(loggedInUser);
        if (user != null && roomManager.canUserJoinRoom(user, roomName)) {
            if (currentRoomName != null) {
                roomManager.removeClientFromRoom(currentRoomName, output);
            }
            currentRoomName = roomName;
            roomManager.addClientToRoom(roomName, output);
            output.println("Joined room: " + roomName);
            roomManager.broadcastToRoom(roomName, loggedInUser + " has joined the room.", false);

            List<String> recentMessages = roomManager.getRoomMessageHistory(roomName);
            if (!recentMessages.isEmpty()) {
                output.println("Recent messages in " + roomName + ":");
                for (String msg : recentMessages) {
                    output.println(msg);
                }
            } else {
                output.println("No recent messages in " + roomName + ".");
            }
        } else {
            output.println("Insufficient permissions or room does not exist.");
        }
    }

    private void handleDirectMessageCommand(String recipient, String message) {
        synchronized (activeUsers) {
            PrintWriter recipientWriter = activeUsers.get(recipient);
            if (recipientWriter != null) {
                recipientWriter.println("Direct message from " + loggedInUser + ": " + message);
                output.println("Direct message sent to " + recipient + ": " + message);
            } else {
                User recipientUser = userManager.getUser(recipient);
                if (recipientUser != null) {
                    userManager.addOfflineMessage(recipient, "Direct message from " + loggedInUser + ": " + message);
                    output.println("User " + recipient + " is offline. Your message will be delivered when they come online.");
                } else {
                    output.println("User not found: " + recipient);
                }
            }
        }
    }

    private void closeClientSocket() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }
}
