import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class FTPSession extends Thread
{
    // Path information
    private String root;
    private String currDirectory;
    private String fileSeparator;

    // Control Connection
    private Socket controlSock;
    private PrintWriter controlOut;
    private BufferedReader controlIn;

    // Data Connection
    private Socket dataSock;
    private OutputStream dataOut;

    // Is anyone logged in?
    private boolean hasCurrentUser;
    private int userNumber;
    private String userName;

    public FTPSession(Socket client){
        controlSock = client;
    }

    // The run method...
    public void run()
    {
        try {
            controlIn = new BufferedReader(new InputStreamReader(controlSock.getInputStream()));
            controlOut = new PrintWriter(controlSock.getOutputStream(), true);
            controlOut.println("220 ::1 9090 ready.");
            String line;
            while (true){
                line = controlRead();
                if (line == null)
                    return;
                executeCommand(line);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private String controlRead() throws IOException {
        return controlIn.readLine();
    }

    private void controlWrite(String line){
        controlOut.println(line);
    }

    //
    // Execute a single command. This function should return "false" if the
    // command was "quit", and true in every other case.
    //
    private boolean executeCommand(String c) throws IOException
    {
        int index = c.indexOf(' ');
        String command = ((index == -1)? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
        String args = ((index == -1)? null : c.substring(index+1, c.length()));

        //
        // For debugging purposes...
        //
        System.out.println("Command: " + command + " Args: " + args);

        try {
            switch (command) {
                case "USER":
                    handleUser(args);
                    break;
                case "PASS":
                    handlePass(args);
                    break;
                case "SYST":
                    handleSyst(args);
                    break;
                case "FEAT":
                    handleFeat(args);
                    break;
                case "PWD":
                    handlePwd(args);
                    break;
                case "CWD":
                    handleCwd(args);
                    break;
                default:
                    controlWrite("502 Command not implemented.");
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }

    //
    // Dealing with the CWD command.
    //
    // Acceptable arguments: .. OR . OR relative path name not including .. or .
    //
    private void handleCwd(String args)
    {
        String filename = currDirectory;

        //
        // First the case where we need to go back up a directory.
        //
        if (args.equals(".."))
        {
            int ind = filename.lastIndexOf(fileSeparator);
            if (ind > 0)
            {
                filename = filename.substring(0, ind);
            }
        }

        //
        // Don't do anything if the user did "cd .". In the other cases,
        // append the argument to the current directory.
        //
        else if ((args != null) && (!args.equals(".")))
        {
            filename = filename + fileSeparator + args;
        }

        //
        // Now make sure that the specified directory exists, and doesn't
        // attempt to go to the FTP root's parent directory.  Note how we
        // use a "File" object to test if a file exists, is a directory, etc.
        //
        File f = new File(filename);

        if (f.exists() && f.isDirectory() && (filename.length() >= root.length()))
        {
            currDirectory = filename;
            controlOut.println("250 The current directory has been changed to " + currDirectory);
        }
        else
        {
            controlOut.println("550 Requested action not taken. File unavailable.");
        }
    }

    private void handlePort(String args) throws Exception
    {
        //
        // Extract the host name (well, really its IP address) and the port number
        // from the arguments.
        //
        StringTokenizer st = new StringTokenizer(args, ",");
        String hostName = st.nextToken() + "." + st.nextToken() + "." +
                st.nextToken() + "." + st.nextToken();

        int p1 = Integer.parseInt(st.nextToken());
        int p2 = Integer.parseInt(st.nextToken());
        int p = p1*256 + p2;

        //
        // You need to complete this one.
        //
    }

    private void handleUser(String args) throws Exception
    {
        userNumber = UserHandler.getUserNumber(args);
        controlWrite("331 User " + args + " accepted, provide password.");
        userName = args;
        executeCommand(controlRead());
    }

    private void handlePass(String args) throws Exception
    {
        if (userNumber == -1 ||  !UserHandler.checkPassword(userNumber, args)) {
            controlWrite("530 Login incorrect.");
            controlSock.close();
            return;
        }
        controlWrite("230 User " + userName + " logged in.");
        hasCurrentUser = true;
        root = UserHandler.getHomeDir(userNumber);
        currDirectory = root;
    }

    private void handleSyst(String args){
        controlWrite("215 UNIX Type: L8");
    }

    private void handleFeat(String args){
        controlWrite("211-Features supported\n211 End");
    }

    private void handlePwd(String args){
        controlWrite("257 \"" + currDirectory + "\" is the current directory.");
    }

    //
    // A helper for the NLST command. The directory name is obtained by
    // appending "args" to the current directory.
    //
    // Return an array containing names of files in a directory. If the given
    // name is that of a file, then return an array containing only one element
    // (this name). If the file or directory does not exist, return nul.
    //
    private String[]  nlstHelper(String args) throws IOException
    {
        //
        // Construct the name of the directory to list.
        //
        String filename = currDirectory;
        if (args != null)
        {
            filename = filename + fileSeparator + args;
        }

        //
        // Now get a File object, and see if the name we got exists and is a
        // directory.
        //
        File f = new File(filename);

        if (f.exists() && f.isDirectory())
        {
            return f.list();
        }
        else if (f.exists() && f.isFile())
        {
            String[] allFiles = new String[1];
            allFiles[0] = f.getName();
            return allFiles;
        }
        else
        {
            return null;
        }
    }

}
