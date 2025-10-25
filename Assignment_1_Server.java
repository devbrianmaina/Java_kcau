import java.lang.*;
import java.net.*;
import java.io.*;

class Assignment_1_Server{
    public static void main(String[] args)
    {
        String data = "  WINNING Class Presentation - Boop Toobie ornaught toobie. Our REG 20/00510.\n";
        try{
            ServerSocket srvr = new ServerSocket(1234);
            Socket skt = srvr.accept();
            System.out.print("Dear Brian Server has connected!!!\n");
            PrintWriter out = new
            PrintWriter(skt.getOutputStream(), true);
            System.out.print("Sending string:" + data + "'\n");
            out.print(data);
            out.flush();
            out.close();
            skt.close();
            srvr.close();
        }
        catch(Exception e ){ System.out.print("Whoops! it didnt work! \n");
    }

        }
    }
