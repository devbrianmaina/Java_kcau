import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class myChatApp_client {
    public static final String HOST = "localhost";
    public static final int PORT = 1333;

    public static void main(String[] args) {
        System.out.println("Connecting to " + HOST + ':' + PORT);
        try (Socket socket = new Socket(HOST, PORT);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            // Reader thread: listens for messages from the server
            Thread reader = new Thread(() -> {
                try {
                    while (true) {
                        String incoming = dis.readUTF();
                        System.out.println("Server: " + incoming);
                        if ("exit".equalsIgnoreCase(incoming.trim())) {
                            System.out.println("Server requested exit. Closing reader.");
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server: " + e.getMessage());
                }
            });
            reader.setDaemon(true);
            reader.start();

            System.out.println("Type messages and press Enter. Type 'exit' to quit.");
            while (true) {
                String line = scanner.nextLine();
                dout.writeUTF(line);
                dout.flush();
                if ("exit".equalsIgnoreCase(line.trim())) {
                    System.out.println("Exiting client.");
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
