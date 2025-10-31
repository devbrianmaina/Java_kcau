import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MyChatAppClientGUI {
    private static final String HOST = "localhost";
    private static final int PORT = 1333;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dout;

    public MyChatAppClientGUI() {
        buildUI();
        connectToServer();
        startReaderThread();
    }

    private void buildUI() {
        frame = new JFrame("MyChatApp - GUI Client");
        chatArea = new JTextArea(20, 50);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        inputField = new JTextField();
        sendButton = new JButton("Send");

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(bottom, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // Window close handling
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeAndExit();
            }
        });

        // Send on button click or Enter key
        ActionListener sendAction = evt -> sendMessage();
        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        frame.setVisible(true);
    }

    private void connectToServer() {
        appendToChat("Connecting to " + HOST + ':' + PORT + "...");
        try {
            socket = new Socket(HOST, PORT);
            dis = new DataInputStream(socket.getInputStream());
            dout = new DataOutputStream(socket.getOutputStream());
            appendToChat("Connected to server: " + socket.getRemoteSocketAddress());
        } catch (IOException e) {
            appendToChat("Failed to connect: " + e.getMessage());
            disableInput();
        }
    }

    private void startReaderThread() {
        Thread reader = new Thread(() -> {
            try {
                while (socket != null && socket.isConnected() && !socket.isClosed()) {
                    String incoming = dis.readUTF();
                    appendToChat("Server: " + incoming);
                    if ("exit".equalsIgnoreCase(incoming.trim())) {
                        appendToChat("Server requested exit. Closing connection.");
                        break;
                    }
                }
            } catch (IOException e) {
                appendToChat("Connection closed: " + e.getMessage());
            } finally {
                disableInput();
                safeClose();
            }
        });
        reader.setDaemon(true);
        reader.start();
    }

    private void sendMessage() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty()) return;
        try {
            if (dout != null) {
                dout.writeUTF(text);
                dout.flush();
                appendToChat("Me: " + text);
            } else {
                appendToChat("Not connected.");
            }
        } catch (IOException e) {
            appendToChat("Failed to send: " + e.getMessage());
            disableInput();
            safeClose();
        } finally {
            inputField.setText("");
        }

        if ("exit".equalsIgnoreCase(text.trim())) {
            // if user typed exit, close after sending
            disableInput();
            safeClose();
        }
    }

    private void appendToChat(String line) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(line + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void disableInput() {
        SwingUtilities.invokeLater(() -> {
            inputField.setEditable(false);
            sendButton.setEnabled(false);
        });
    }

    private void safeClose() {
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

    private void closeAndExit() {
        // send exit to server then close
        try {
            if (dout != null) {
                dout.writeUTF("exit");
                dout.flush();
            }
        } catch (IOException ignored) {}
        safeClose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MyChatAppClientGUI::new);
    }
}
