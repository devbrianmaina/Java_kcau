import java.io.*;
import java.net.*;

class Assignment_1_Client{
    public static void main(String args[])
    {
        try{
            Socket skt = new Socket("localhost", 1234);
            BufferedReader in = new BufferedReader(new 
            InputStreamReader(skt.getInputStream()));
            System.out.print("Received string: '");
            while(!in.ready()){}

            System.out.println(in.readLine());//Read one line and output it
            System.out.print("'\n");
        }
            catch(Exception e){
            System.out.print("Whoops!! brayo hio imekwara \n");
    

        }
    }
}