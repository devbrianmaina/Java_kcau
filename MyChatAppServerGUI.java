import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyChatAppServerGUI {
    private JFrame frame;
    private JTextArea logArea;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private JButton startButton;
    private JButton stopButton;
    private JButton broadcastButton;
    private JTextField portField;
    private JTextField broadcastField;

    private ServerController controller;

    public MyChatAppServerGUI() {
        buildUI();
    }

    private void buildUI() {
        frame = new JFrame("MyChatApp Server GUI");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        logArea = new JTextArea(20, 50);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setVisibleRowCount(10);
        JScrollPane clientsScroll = new JScrollPane(clientList);

        JPanel right = new JPanel(new BorderLayout(5,5));
        right.add(new JLabel("Connected Clients:"), BorderLayout.NORTH);
        right.add(clientsScroll, BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        portField = new JTextField("1333", 6);
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        top.add(new JLabel("Port:"));
        top.add(portField);
        top.add(startButton);
        top.add(stopButton);

        JPanel bottom = new JPanel(new BorderLayout(6,6));
        broadcastField = new JTextField();
        broadcastButton = new JButton("Broadcast");
        broadcastButton.setEnabled(false);
        bottom.add(broadcastField, BorderLayout.CENTER);
        bottom.add(broadcastButton, BorderLayout.EAST);

        frame.add(top, BorderLayout.NORTH);
        frame.add(logScroll, BorderLayout.CENTER);
        frame.add(right, BorderLayout.EAST);
        frame.add(bottom, BorderLayout.SOUTH);

        // Actions
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        broadcastButton.addActionListener(e -> broadcastFromServer());

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            appendLog("Invalid port number.");
            return;
        }
        controller = new ServerController(port);
        controller.start();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        broadcastButton.setEnabled(true);
        appendLog("Server started on port " + port);
    }

    private void stopServer() {
        if (controller != null) {
            controller.shutdown();
            controller = null;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            broadcastButton.setEnabled(false);
            appendLog("Server stopped.");
            clientListModel.clear();
        }
    }

    private void broadcastFromServer() {
        String msg = broadcastField.getText();
        if (msg == null || msg.trim().isEmpty()) return;
        if (controller != null) {
            controller.broadcast("[SERVER]: " + msg);
            appendLog("Broadcast: " + msg);
            broadcastField.setText("");
        }
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ServerController: runs server logic on background threads and updates GUI via callbacks
    private class ServerController {
        private final int port;
        private ServerSocket serverSocket;
        private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private volatile boolean running = false;

        ServerController(int port) {
            this.port = port;
        }

        void start() {
            running = true;
            executor.submit(() -> {
                try (ServerSocket ss = new ServerSocket(port)) {
                    serverSocket = ss;
                    appendLog("Listening on port " + port);
                    while (running) {
                        try {
                            Socket client = ss.accept();
                            ClientHandler ch = new ClientHandler(client);
                            clients.add(ch);
                            executor.submit(ch);
                            SwingUtilities.invokeLater(() -> clientListModel.addElement(ch.name));
                            appendLog("Accepted: " + ch.name);
                        } catch (IOException e) {
                            if (running) appendLog("Accept error: " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    appendLog("Server socket error: " + e.getMessage());
                }
            });
        }

        void broadcast(String message) {
            for (ClientHandler c : clients) {
                try {
                    c.send(message);
                } catch (IOException e) {
                    appendLog("Failed to send to " + c.name + ": " + e.getMessage());
                    c.closeSilently();
                    removeClient(c);
                }
            }
        }

        void removeClient(ClientHandler c) {
            clients.remove(c);
            SwingUtilities.invokeLater(() -> clientListModel.removeElement(c.name));
            appendLog("Removed: " + c.name);
        }

        void shutdown() {
            running = false;
            try {
                if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            } catch (IOException ignored) {}
            for (ClientHandler c : clients) c.closeSilently();
            clients.clear();
            executor.shutdownNow();
        }

        private class ClientHandler implements Runnable {
            final Socket socket;
            final String name;
            DataInputStream dis;
            DataOutputStream dout;

            ClientHandler(Socket socket) {
                this.socket = socket;
                this.name = socket.getRemoteSocketAddress().toString();
            }

            @Override
            public void run() {
                try {
                    dis = new DataInputStream(socket.getInputStream());
                    dout = new DataOutputStream(socket.getOutputStream());
                    // notify others
                    broadcast("[" + name + "] joined the chat.");
                    appendLog(name + " handler started.");
                    while (running && !socket.isClosed()) {
                        String msg;
                        try {
                            msg = dis.readUTF();
                        } catch (IOException e) {
                            appendLog("Client disconnected: " + name);
                            break;
                        }
                        appendLog(name + ": " + msg);
                        if ("exit".equalsIgnoreCase(msg.trim())) {
                            broadcast("[" + name + "] left the chat.");
                            break;
                        }
                        broadcast(name + ": " + msg);
                    }
                } catch (IOException e) {
                    appendLog("I/O error with client " + name + ": " + e.getMessage());
                } finally {
                    closeSilently();
                    removeClient(this);
                }
            }

            void send(String message) throws IOException {
                if (dout != null) {
                    dout.writeUTF(message);
                    dout.flush();
                } else {
                    throw new IOException("Output closed");
                }
            }

            void closeSilently() {
                try { if (dout != null) dout.close(); } catch (IOException ignored) {}
                try { if (dis != null) dis.close(); } catch (IOException ignored) {}
                try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MyChatAppServerGUI::new);
    }
}
