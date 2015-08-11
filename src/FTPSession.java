import java.io.*;
import java.net.*;

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
    private boolean isPassiveMode;
    private boolean isASCIIMode;
    private Socket dataSock;
    private ServerSocket dataServSock;
    private OutputStream dataOut;
    private boolean isDataOpen;
    private boolean isDataServOpen;

    // Is anyone logged in?
    private boolean hasCurrentUser;
    private int userNumber;
    private String userName;

    public FTPSession(Socket client){
        controlSock = client;
    }

    public void run()
    {
        try {
            controlIn = new BufferedReader(new InputStreamReader(controlSock.getInputStream()));
            controlOut = new PrintWriter(controlSock.getOutputStream(), true);
            controlOut.println("220 ::1 " + FTPServer.CONTROL_PORT + " ready.");
            String line;
            while (true){
                line = controlRead();
                if (line == null || !executeCommand(line)) return;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //
    // Execute a single command. This function should return "false" if the
    // command was "quit", and true in every other case.
    //
    private boolean executeCommand(String c) throws Exception
    {
        int index = c.indexOf(' ');
        String command = ((index == -1)? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
        String args = ((index == -1)? null : c.substring(index+1, c.length()));

        //
        // For debugging purposes...
        //
        System.out.println("Command: " + command + " Args: " + args);

        FTPCmd currCmd = new FTPCmd(command, args, this);

        return currCmd.cmdAction();
    }

    public boolean connectData() throws IOException {
        if (dataSock != null) dataSock.close();
        if (isPassiveMode()) {
            if (dataServSock != null) {
                dataSock = dataServSock.accept();
                setIsDataOpen(true);
                return true;
            }
            else {
                controlWrite("425 Can't open data connection.");
                setIsDataOpen(false);
                return false;
            }
        }
        else {
            //need to handle Port scenario
            return false;
        }
    }

    public void closeDataConn() throws IOException {
        if (!isDataOpen()) return;
        setIsDataOpen(false);
        if (dataSock != null) dataSock.close();
        if (dataServSock != null) dataServSock.close();
    }

    public int createDataServSock() throws IOException {
        closeDataConn();
        dataServSock = new ServerSocket(0);
        isDataServOpen = true;
        return dataServSock.getLocalPort();
    }

    public String controlRead() throws IOException {
        return controlIn.readLine();
    }

    public void controlWrite(String line) throws IOException{
        controlOut.println(line);
    }

    public void dataWriteASCII(String line) throws IOException{
        PrintWriter ASCIIPrinter = new PrintWriter(dataSock.getOutputStream(), true);
        ASCIIPrinter.print(line + "\r\n");
        ASCIIPrinter.flush();
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getCurrDirectory() {
        return currDirectory;
    }

    public void setCurrDirectory(String currDirectory) {
        this.currDirectory = currDirectory;
    }

    public String getFileSeparator() {
        return fileSeparator;
    }

    public boolean isPassiveMode() {
        return this.isPassiveMode;
    }

    public void setIsPassiveMode(boolean isPassiveMode) {
        this.isPassiveMode = isPassiveMode;
    }

    public boolean isASCIIMode() {
        return isASCIIMode;
    }

    public void setIsASCIIMode(boolean isASCIIMode) {
        this.isASCIIMode = isASCIIMode;
    }

    public boolean isDataOpen() {
        return isDataOpen;
    }

    public void setIsDataOpen(boolean isDataOpen) {
        this.isDataOpen = isDataOpen;
    }

    public boolean isDataServOpen() {
        return isDataServOpen;
    }

    public void setIsDataServOpen(boolean isDataServOpen) {
        this.isDataServOpen = isDataServOpen;
    }

    public boolean hasCurrentUser() {
        return hasCurrentUser;
    }

    public void setHasCurrentUser(boolean hasCurrentUser) {
        this.hasCurrentUser = hasCurrentUser;
    }

    public int getUserNumber() {
        return userNumber;
    }

    public void setUserNumber(int userNumber) {
        this.userNumber = userNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}
