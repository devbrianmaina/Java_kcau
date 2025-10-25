import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Assignment_2_Client {
    public static void main(String[] args) throws IOException {
        // taking input from client
        Scanner input = new Scanner(System.in);
        System.out.println("Enter two numbers you want to get the sum of: ");
        int number1;
        int number2;
        number1 = input.nextInt();
        number2 = input.nextInt();

        // create socket and related streams in try-with-resources so they are closed automatically
        try (Socket socket = new Socket("localhost", 1301);
             PrintStream toServer = new PrintStream(socket.getOutputStream());
             Scanner receiver = new Scanner(socket.getInputStream())) {

            // giving information to server
            toServer.println(number1);
            toServer.println(number2);

            // receiving information from server
            int result = receiver.nextInt();
            System.out.println("sum of the two numbers is: " + result);
        }
    }
} 

