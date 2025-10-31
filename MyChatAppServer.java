import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyChatAppServer {
    public static final int PORT = 1333;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        new MyChatAppServer().start();
    }

    public void start() {
        System.out.println("MyChatAppServer starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                executor.submit(handler);
                System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private void broadcast(String message, ClientHandler exclude) {
        for (ClientHandler c : clients) {
            if (c != exclude) {
                try {
                    c.send(message);
                } catch (IOException e) {
                    System.err.println("Failed to send to " + c.getClientName() + ": " + e.getMessage());
                    c.closeSilently();
                    clients.remove(c);
                }
            }
        }
    }

    private void removeClient(ClientHandler c) {
        clients.remove(c);
        System.out.println("Removed client: " + c.getClientName());
    }

    private void shutdown() {
        System.out.println("Shutting down server...");
        for (ClientHandler c : clients) c.closeSilently();
        executor.shutdownNow();
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private DataInputStream dis;
        private DataOutputStream dout;
        private final String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientName = socket.getRemoteSocketAddress().toString();
        }

        String getClientName() {
            return clientName;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());

                // Notify others that this client joined
                broadcast("[" + clientName + "] joined the chat.", this);

                while (true) {
                    String msg;
                    try {
                        msg = dis.readUTF();
                    } catch (IOException e) {
                        System.out.println("Connection lost from " + clientName + ": " + e.getMessage());
                        break;
                    }
                    System.out.println("Received from " + clientName + ": " + msg);
                    if ("exit".equalsIgnoreCase(msg.trim())) {
                        // notify and close
                        broadcast("[" + clientName + "] left the chat.", this);
                        break;
                    }
                    // Broadcast message to others
                    broadcast("" + clientName + ": " + msg, this);
                }
            } catch (IOException e) {
                System.err.println("I/O error with client " + clientName + ": " + e.getMessage());
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
                throw new IOException("Output stream closed");
            }
        }

        void closeSilently() {
            try {
                if (dout != null) dout.close();
            } catch (IOException ignored) {}
            try {
                if (dis != null) dis.close();
            } catch (IOException ignored) {}
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}
