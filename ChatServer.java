import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Set<String> users = new HashSet<>();
    private static final Map<String, PrintWriter> clientWriters = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Server started. Waiting for clients...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Get username
                username = in.readLine();
                synchronized (users) {
                    if (users.contains(username)) {
                        out.println("ERROR: Username already taken.");
                        socket.close();
                        return;
                    }
                    users.add(username);
                    clientWriters.put(username, out);
                    updateUserList();
                }

                System.out.println(username + " has joined the chat.");
                broadcast(username + " has joined the chat.");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("@PRIVATE")) {
                        sendPrivateMessage(message);
                    } else {
                        broadcast(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println(username + " disconnected.");
            } finally {
                cleanup();
            }
        }

        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters.values()) {
                    writer.println(message);
                }
            }
        }

        private void sendPrivateMessage(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) return;

            String recipient = parts[1];
            String privateMessage = parts[2];

            PrintWriter recipientWriter = clientWriters.get(recipient);
            if (recipientWriter != null) {
                recipientWriter.println("(Private) " + username + ": " + privateMessage);
            }
        }

        private void updateUserList() {
            StringBuilder userList = new StringBuilder("USER_LIST ");
            for (String user : users) {
                userList.append(user).append(",");
            }
            broadcast(userList.toString());
        }

        private void cleanup() {
            try {
                if (username != null) {
                    synchronized (users) {
                        users.remove(username);
                        clientWriters.remove(username);
                        updateUserList();
                    }
                    broadcast(username + " has left the chat.");
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
