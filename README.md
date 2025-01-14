# 🌐 Echat Protocol Documentation

## 📖 Overview

Echat is a **multi-user chat system** where clients connect to a central server using a **command-based protocol**. The server handles:

- 🔒 User authentication  
- 🛠️ Room creation and joining  
- ✉️ Message distribution  
- 🛡️ Administrative operations  

💻 Clients use a **dedicated Java program** to manage socket communication, but also suports Telnet connections.

---

## 🛠️ Communication Basics

- **Protocol**: TCP  
- **Default Port**: `42069`  
- **Message Format**: Plain text strings, ending with `\n`  

### 🔗 Connection Flow:
1. Clients connect to the server via a **TCP socket**.  
2. Once connected, they can issue commands and receive responses.  

---

## 🔑 Command Structure

### **General Syntax**:
```
<command> [arg1] [arg2] ...
```

- 📋 **Commands**: Case-insensitive  
- 📂 **Arguments**: Space-separated  
- ✅ **Server Responses**: Success, error, or informational messages  

---

## ✨ Supported Commands

### 🔑 Authentication
1. **Register**  
   `register <username> <password>`  
   ➡️ Register a new user.

2. **Login**  
   `login <username> <password>`  
   ➡️ Authenticate as an existing user.  

### 🏠 Room Management
- **Join Room**: `join <room_name>`  
  ➡️ Join a chat room.  
- **Create Room**: `createroom <room_name> <PERMS>`  
  ➡️ Request a new chat room.  
- **List Rooms**: `listrooms`  
  ➡️ View available rooms.  
- **Leave Room**: `leave`  
  ➡️ Exit the current room.

### 💬 Messaging
- **Room Message**: Simply type your message.  
  ➡️ Sends to everyone in the room.  
- **Direct Message**: `msg <username> <message>`  
  ➡️ Private message another user.

### 👥 User Management
- **List Users**: `users`  
  ➡️ View users in the current room.  
- **Logout**: `logout`  
  ➡️ Sign out.  

### ⚙️ Admin Tools (HIGH Permissions)
- **Approve Room**: `approveroom <room_name>`  
  ➡️ Approve a room creation.  
- **Deny Room**: `denyroom <room_name>`  
  ➡️ Deny a room creation.  
- **Notify Users**: `notify <PERMS> <message>`  
  ➡️ Send notifications to users.  

---

## 📜 Permissions Hierarchy

- **LOW**: Basic users 🟢  
- **MEDIUM**: Intermediate users 🟡  
- **HIGH**: Admin users 🔴  

Permissions affect room management, notifications, and approvals.

---

## 🛠️ Example Workflows

### 1️⃣ **Basic User Workflow**
1. Register:  
   ```
   register john password123
   ```
2. Login:  
   ```
   login john password123
   ```
3. Join a Room:  
   ```
   join General
   ```
4. Send a Message:  
   ```
   Hello, everyone!
   ```

### 2️⃣ **Admin Workflow**
1. Login:  
   ```
   login admin adminpass
   ```
2. View Pending Requests:  
   ```
   listroomrequests
   ```
3. Approve a Room:  
   ```
   approveroom NewRoom
   ```

---

## ❌ Error Handling

- **Unknown Commands**:  
  ```
  Unknown command. Type help for a list of commands.
  ```
- **Invalid Arguments**:  
  ```
  Invalid command. Usage: <correct usage>
  ```
- **Permission Denied**:  
  ```
  Insufficient permissions to execute this command.
  ```
