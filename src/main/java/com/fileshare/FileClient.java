package com.fileshare;

import java.io.*;
import java.net.*;
import java.nio.file.*;

public class FileClient {
    public interface Progress {
        void update(long done, long total);
    }

    public record RemoteFile(String name, long size) {
        public String toString() {
            return name + "   |   " + sizeText(size);
        }
    }

    private final String host;
    private final int port;

    public FileClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private Socket connect() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 5000);
        s.setSoTimeout(120000);
        return s;
    }

    public String auth(String action, String user, String pass) throws IOException {
        try (Socket s = connect();
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                DataInputStream i = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {
            o.writeUTF(action);
            o.writeUTF(user);
            o.writeUTF(pass);
            o.flush();
            boolean ok = i.readBoolean();
            String msg = i.readUTF();
            if (!ok)
                throw new IOException(msg);
            return msg;
        }
    }

    public RemoteFile[] list(String user, String pass) throws IOException {
        try (Socket s = connect();
                DataOutputStream o = new DataOutputStream(
                        new BufferedOutputStream(s.getOutputStream()));
                DataInputStream i = new DataInputStream(
                        new BufferedInputStream(s.getInputStream()))) {

            o.writeUTF("AUTH_LIST");
            o.writeUTF(user);
            o.writeUTF(pass);
            o.flush();

            if (!i.readBoolean())
                throw new IOException(i.readUTF());

            int n = i.readInt();
            RemoteFile[] a = new RemoteFile[n];

            for (int x = 0; x < n; x++) {
                a[x] = new RemoteFile(i.readUTF(), i.readLong());
            }

            return a;
        }
    }

    public String upload(String user, String pass, Path p, Progress cb) throws IOException {
        try (Socket s = connect();
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                DataInputStream i = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                InputStream f = new BufferedInputStream(Files.newInputStream(p))) {
            // Each operation authenticates using a short-lived connection.
            o.writeUTF("LOGIN");
            o.writeUTF(user);
            o.writeUTF(pass);
            o.flush();
            if (!i.readBoolean())
                throw new IOException(i.readUTF());
            i.readUTF();
            // Login connection is one command per connection; open a fresh authenticated
            // transfer protocol below.
            throw new IOException("Internal protocol error: use transfer connection.");
        }
    }

    public String transferUpload(String user, String pass, Path p, Progress cb) throws IOException {
        try (Socket s = connect();
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                DataInputStream i = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                InputStream f = new BufferedInputStream(Files.newInputStream(p))) {
            // Auth and requested operation are combined in one command in protocol v1.
            o.writeUTF("AUTH_UPLOAD");
            o.writeUTF(user);
            o.writeUTF(pass);
            o.writeUTF(p.getFileName().toString());
            long size = Files.size(p);
            o.writeLong(size);
            byte[] b = new byte[8192];
            long done = 0;
            int n;
            while (done < size && (n = f.read(b, 0, (int) Math.min(b.length, size - done))) != -1) {
                o.write(b, 0, n);
                done += n;
                if (cb != null)
                    cb.update(done, size);
            }
            o.flush();
            if (!i.readBoolean())
                throw new IOException(i.readUTF());
            return i.readUTF();
        }
    }

    public Path transferDownload(String user, String pass, RemoteFile file, Path folder, Progress cb)
            throws IOException {
        try (Socket s = connect();
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                DataInputStream i = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {
            o.writeUTF("AUTH_DOWNLOAD");
            o.writeUTF(user);
            o.writeUTF(pass);
            o.writeUTF(file.name());
            o.flush();
            if (!i.readBoolean())
                throw new IOException(i.readUTF());
            long size = i.readLong();
            Path dir = folder.toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(file.name()).normalize();
            if (!target.getParent().equals(dir))
                throw new IOException("Invalid path.");
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(target))) {
                byte[] b = new byte[8192];
                long done = 0;
                while (done < size) {
                    int n = i.read(b, 0, (int) Math.min(b.length, size - done));
                    if (n < 0)
                        throw new EOFException("Download interrupted.");
                    out.write(b, 0, n);
                    done += n;
                    if (cb != null)
                        cb.update(done, size);
                }
            }
            return target;
        }
    }

    public void delete(String user, String pass, String name) throws IOException {
        try (Socket s = connect();
                DataOutputStream o = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                DataInputStream i = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {
            o.writeUTF("AUTH_DELETE");
            o.writeUTF(user);
            o.writeUTF(pass);
            o.writeUTF(name);
            o.flush();
            if (!i.readBoolean())
                throw new IOException(i.readUTF());
            i.readUTF();
        }
    }

    private static String sizeText(long n) {
        if (n < 1024)
            return n + " B";
        if (n < 1048576)
            return String.format("%.1f KB", n / 1024.0);
        return String.format("%.1f MB", n / 1048576.0);
    }
}
