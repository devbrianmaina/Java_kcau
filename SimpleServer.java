import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleServer {
    public static final int PORT = 1333;

    public static void main(String[] args) {
        System.out.println("SimpleServer starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Waiting for client...");
            try (Socket client = serverSocket.accept();
                 DataInputStream dis = new DataInputStream(client.getInputStream());
                 DataOutputStream dout = new DataOutputStream(client.getOutputStream())) {

                System.out.println("Client connected: " + client.getRemoteSocketAddress());
                while (true) {
                    String msg;
                    try {
                        msg = dis.readUTF();
                    } catch (IOException e) {
                        System.out.println("Client disconnected: " + e.getMessage());
                        break;
                    }
                    System.out.println("Client: " + msg);
                    if ("exit".equalsIgnoreCase(msg.trim())) {
                        dout.writeUTF("exit");
                        dout.flush();
                        System.out.println("Received exit from client. Closing connection.");
                        break;
                    }
                    // Echo back
                    dout.writeUTF("Echo: " + msg);
                    dout.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
