import java.lang.*;
import java.io.*;
import java.net.*;


public class myChatApp_server {
        static ServerSocket ss;
        static Socket s;
        static DataInputStream dis;
        static DataOutputStream dout;

    public static void main(String[] args){
        try {
            
            String margin = "";
            String msg = "";
            ss  = new ServerSocket(1333);
            s = ss.accept();
            dis = new DataInputStream(s.getInputStream());
            dout = new DataOutputStream(s.getOutputStream());
            while(!margin.equals("exit")){
                margin = dis.readUTF();
                System.out.println("\n client: " + margin);
            
            msg = msg_text.getText();
            dout.writeUTF(msg);
            msg_text.setText("");
            }
        } catch (Exception e) {
            // TODO: handle exception
            
        }
   

    }
    
}
