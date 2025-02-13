import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClientGUI {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;
    private String username;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel usernameLabel;
    private JComboBox<String> userDropdown;

    private Map<String, PrivateChatWindow> privateChats = new HashMap<>();

    public ChatClientGUI() {
        getUsernameFromUser();
        setupGUI();
        connectToServer();
    }

    private void getUsernameFromUser() {
        username = JOptionPane.showInputDialog(null, "Enter your username:", "Username", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            username = "Guest" + (int) (Math.random() * 1000);
        }
    }

    private void setupGUI() {
        frame = new JFrame("Chat - " + username);
        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        usernameLabel = new JLabel("Logged in as: " + username, JLabel.CENTER);
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        frame.add(usernameLabel, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        userDropdown = new JComboBox<>();
        userDropdown.addItem("Select User");
        userDropdown.addActionListener(e -> openPrivateChat());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(usernameLabel, BorderLayout.CENTER);
        topPanel.add(userDropdown, BorderLayout.EAST);
        frame.add(topPanel, BorderLayout.NORTH);

        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(username);

            new Thread(() -> {
                try {
                    while (true) {
                        String receivedMessage = in.readLine();
                        if (receivedMessage == null) break;

                        if (receivedMessage.startsWith("USER_LIST")) {
                            updateUserList(receivedMessage);
                        } else {
                            chatArea.append(receivedMessage + "\n");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Server disconnected.");
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Failed to connect to server.");
        }
    }

    private void updateUserList(String userListMessage) {
        userDropdown.removeAllItems();
        userDropdown.addItem("Select User");

        String[] users = userListMessage.substring(10).split(",");
        for (String user : users) {
            if (!user.equals(username)) {
                userDropdown.addItem(user);
            }
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            messageField.setText("");
        }
    }

    private void openPrivateChat() {
        String selectedUser = (String) userDropdown.getSelectedItem();
        if (selectedUser != null && !selectedUser.equals("Select User") && !selectedUser.equals(username)) {
            if (!privateChats.containsKey(selectedUser)) {
                privateChats.put(selectedUser, new PrivateChatWindow(username, selectedUser, out));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}

class PrivateChatWindow {
    private String sender;
    private String receiver;
    private PrintWriter out;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;

    public PrivateChatWindow(String sender, String receiver, PrintWriter out) {
        this.sender = sender;
        this.receiver = receiver;
        this.out = out;

        frame = new JFrame("Private Chat - " + sender + " -> " + receiver);
        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println("@PRIVATE " + receiver + " " + message);
            chatArea.append("You: " + message + "\n");
            messageField.setText("");
        }
    }
}
