# 🚀 FileShare Pro – Java-Based File Sharing Application

A client-server file-sharing application developed using Java Socket Programming and Swing GUI. It allows users to register, log in, upload, download, view, and delete files through a graphical interface.

## 📌 Project Overview

FileShare Pro is a networking-based application that demonstrates TCP client-server communication, user authentication, file handling, and multithreading.

The application provides a simple graphical interface through which users can securely manage their files within their own accounts.

## ✨ Features

- **User Registration:** Create a new account.
- **User Authentication:** Login using username and password.
- **File Upload:** Upload files from your local system.
- **File Download:** Download stored files.
- **File Listing:** View uploaded files with their sizes.
- **File Deletion:** Delete selected files.
- **Progress Tracking:** Monitor file transfer progress.
- **User-Specific Storage:** Maintain separate storage directories for users.
- **Multithreading:** Handle multiple client connections.

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Java | Core programming language |
| TCP Socket Programming | Client-server communication |
| Java Swing | Graphical User Interface |
| Java I/O Streams | File transfer and data handling |
| Multithreading | Multiple client handling |
| PBKDF2 | Password hashing |

## 🏗️ Project Architecture

```text
             CLIENT
               |
               |
          ClientGUI.java
               |
               |
          FileClient.java
               |
               |
        TCP Socket Connection
               |
               |
          FileServer.java
               |
        -----------------
        |               |
   Authentication   File Handling
        |               |
     users.db       server_files/
```

## 📂 Project Structure

```text
FileShare-Pro-Java/
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── fileshare/
│                   ├── ClientGUI.java
│                   ├── FileClient.java
│                   ├── FileServer.java
│                   └── PasswordUtil.java
│
├── .gitignore
└── README.md
```

## ⚙️ How to Run

### Prerequisites

- Java JDK 21 or later
- VS Code or any Java IDE
- Git

### Step 1: Clone Repository

```bash
git clone https://github.com/Urvashi-3159/FileShare-Pro-Java.git
```

### Step 2: Open Project Directory

```bash
cd FileShare-Pro-Java

### Step 3: Compile Java Files

```bash
javac -d out src/main/java/com/fileshare/*.java

### Step 4: Start the Server

```bash
java -cp out com.fileshare.FileServer

### Step 5: Start the Client GUI

Open a new terminal while keeping the server terminal running.

```bash
java -cp out com.fileshare.ClientGUI

### Step 6: Use the Application

1. Register a new account using your username and password.
2. Log in with your registered credentials.
3. Select a file and click **Upload** to upload it.
4. View your uploaded files in the file list.
5. Select a file and click **Download** to download it.
6. Select a file and click **Delete** to remove it from your account.
