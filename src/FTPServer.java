import java.io.*;
import java.net.*;

public class FTPServer
{
    public static final int CONTROL_PORT = 9090;

    public static void main(String[] args)
    {
        try
        {
            ServerSocket listener = new ServerSocket(CONTROL_PORT);
            UserHandler.init();
            FTPCmd.init();
            System.out.println("Server started. Please connect to it using" +
                    "the FTP command in another window.");

            //
            // An infinite loop. Hit Control-C to kill your server.
            //
            while (true)
            {
                new FTPSession(listener.accept()).start();
                Thread.sleep(1000);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}