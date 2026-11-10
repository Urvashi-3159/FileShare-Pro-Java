package com.fileshare;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class FileServer {
    private static final int PORT_DEFAULT = 5000;
    private static final long MAX_SIZE = 500L * 1024 * 1024;
    private static final Path ROOT = Paths.get("server_files").toAbsolutePath();
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    private record Account(String salt, String hash) {}

    public FileServer() throws Exception { loadAccounts(); }

    private void loadAccounts() throws Exception {
        Files.createDirectories(ROOT);
        Path db = Paths.get("users.db");
        if (!Files.exists(db)) Files.createFile(db);
        for (String line : Files.readAllLines(db)) {
            String[] p = line.split("\\\\|", -1);
            if (p.length == 3) accounts.put(p[0], new Account(p[1], p[2]));
        }
    }

    private synchronized void saveAccount(String user, Account account) throws IOException {
        Files.writeString(Paths.get("users.db"), user + "|" + account.salt + "|" + account.hash
                + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        accounts.put(user, account);
    }

    public void start(int port) throws IOException {
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("FileShare Pro server running on port " + port);
            System.out.println("Storage: " + ROOT);
            while (true) pool.submit(new ClientHandler(server.accept()));
        } finally { pool.shutdown(); }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;
        ClientHandler(Socket socket) { this.socket = socket; }

        public void run() {
            try (socket;
                 DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                 DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
                socket.setSoTimeout(120_000);
                String command = in.readUTF();
                switch (command) {
                    case "REGISTER" -> register(in, out);
                    case "LOGIN" -> login(in, out);
                    case "LIST" -> { if (authenticated(out)) list(out); }
                    case "UPLOAD" -> { if (authenticated(out)) upload(in, out); }
                    case "DOWNLOAD" -> { if (authenticated(out)) download(in, out); }
                    case "DELETE" -> { if (authenticated(out)) delete(in, out); }
                    default -> { out.writeBoolean(false); out.writeUTF("Unknown request."); }
                }
                out.flush();
            } catch (Exception e) {
                System.err.println("Client session: " + e.getMessage());
            }
        }

        private boolean authenticated(DataOutputStream out) throws IOException {
            if (username != null) return true;
            out.writeBoolean(false); out.writeUTF("Please log in first."); return false;
        }

        private void register(DataInputStream in, DataOutputStream out) throws Exception {
            String user = cleanUser(in.readUTF());
            String pass = in.readUTF();
            if (pass.length() < 8) { out.writeBoolean(false); out.writeUTF("Password must be at least 8 characters."); return; }
            if (accounts.containsKey(user)) { out.writeBoolean(false); out.writeUTF("Username already exists."); return; }
            String salt = PasswordUtil.newSalt();
            saveAccount(user, new Account(salt, PasswordUtil.hash(pass, salt)));
            username = user;
            out.writeBoolean(true); out.writeUTF("Account created. You are logged in as " + user + ".");
        }

        private void login(DataInputStream in, DataOutputStream out) throws Exception {
            String user = cleanUser(in.readUTF());
            String pass = in.readUTF();
            Account a = accounts.get(user);
            if (a == null || !PasswordUtil.verify(pass, a.salt, a.hash)) {
                out.writeBoolean(false); out.writeUTF("Invalid username or password."); return;
            }
            username = user;
            out.writeBoolean(true); out.writeUTF("Welcome, " + user + "!");
        }

        private void list(DataOutputStream out) throws IOException {
            Path dir = userDir();
            try (var stream = Files.list(dir)) {
                var list = stream.filter(Files::isRegularFile).sorted().toList();
                out.writeBoolean(true); out.writeInt(list.size());
                for (Path p : list) { out.writeUTF(p.getFileName().toString()); out.writeLong(Files.size(p)); }
            }
        }

        private void upload(DataInputStream in, DataOutputStream out) throws IOException {
            String name = safeName(in.readUTF());
            long size = in.readLong();
            if (size < 0 || size > MAX_SIZE) { out.writeBoolean(false); out.writeUTF("Maximum file size is 500 MB."); return; }
            Path dir = userDir(), target = dir.resolve(name).normalize();
            Path temp = Files.createTempFile(dir, "upload-", ".part");
            boolean ok = false;
            try (OutputStream file = new BufferedOutputStream(Files.newOutputStream(temp))) {
                copyExactly(in, file, size);
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                ok = true;
            } finally { if (!ok) Files.deleteIfExists(temp); }
            out.writeBoolean(true); out.writeUTF("Upload successful: " + name);
        }

        private void download(DataInputStream in, DataOutputStream out) throws IOException {
            Path file = userDir().resolve(safeName(in.readUTF())).normalize();
            if (!file.getParent().equals(userDir()) || !Files.isRegularFile(file)) {
                out.writeBoolean(false); out.writeUTF("File not found."); return;
            }
            out.writeBoolean(true); out.writeLong(Files.size(file));
            try (InputStream stream = new BufferedInputStream(Files.newInputStream(file))) { stream.transferTo(out); }
        }

        private void delete(DataInputStream in, DataOutputStream out) throws IOException {
            Path file = userDir().resolve(safeName(in.readUTF())).normalize();
            boolean deleted = file.getParent().equals(userDir()) && Files.deleteIfExists(file);
            out.writeBoolean(deleted); out.writeUTF(deleted ? "File deleted." : "File not found.");
        }

        private Path userDir() throws IOException {
            Path dir = ROOT.resolve(username).normalize();
            Files.createDirectories(dir);
            return dir;
        }
    }

    private static String cleanUser(String raw) {
        String user = raw.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        if (user.length() < 3 || user.length() > 24) throw new IllegalArgumentException("Username must be 3-24 letters, digits, _ or -.");
        return user;
    }
    private static String safeName(String raw) {
        String name = Paths.get(raw).getFileName().toString().replaceAll("[^a-zA-Z0-9._ -]", "_");
        return name.isBlank() || name.equals(".") || name.equals("..") ? "unnamed_file" : name;
    }
    private static void copyExactly(InputStream in, OutputStream out, long remaining) throws IOException {
        byte[] b = new byte[8192];
        while (remaining > 0) {
            int n = in.read(b, 0, (int)Math.min(b.length, remaining));
            if (n < 0) throw new EOFException("Transfer interrupted.");
            out.write(b, 0, n); remaining -= n;
        }
    }
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? PORT_DEFAULT : Integer.parseInt(args[0]);
        new FileServer().start(port);
    }
}
