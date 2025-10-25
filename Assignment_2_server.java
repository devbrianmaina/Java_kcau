import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Assignment_2_server {
    public static void main(String[] args) throws IOException {
        // server side socket and client handling using try-with-resources to ensure closure
        try (ServerSocket serverSocket = new ServerSocket(1301);
             Socket clientsInfo = serverSocket.accept();
             Scanner info = new Scanner(clientsInfo.getInputStream());
             PrintStream result = new PrintStream(clientsInfo.getOutputStream())) {

            // process with the info
            int number1;
            int number2;
            String operator;
            double calculationResult;

            number1 = info.nextInt();
            number2 = info.nextInt();
            operator = info.next();

            switch (operator) {
                case "+":
                    calculationResult = number1 + number2;
                    result.println(calculationResult);
                    break;
                case "-":
                    calculationResult = number1 - number2;
                    result.println(calculationResult);
                    break;
                case "*":
                    calculationResult = number1 * number2;
                    result.println(calculationResult);
                    break;
                case "/":
                    if (number2 != 0) {
                        calculationResult = (double) number1 / number2;
                        result.println(calculationResult);
                    } else {
                        result.println("Error: Division by zero is not allowed.");
                    }
                    break;
                default:
                    result.println("Error: Invalid operator.");
                    break;
            }
        }
    }
}
