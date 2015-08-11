/**
 * Created by christianrachmaninoff on 8/11/15.
 */
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.StringTokenizer;

public class FTPCmd {
    public static String[] userCommands = {"CWD","PWD","EPSV","NLST"};
    public static String[] dataCommands = {"NLST"};

    public static void init(){
        Arrays.sort(userCommands);
        Arrays.sort(dataCommands);
    }

    private String cmdName;
    private String cmdArgs;
    private boolean isUserCmd;
    private boolean isDataCmd;
    private FTPSession session;

    public FTPCmd(String cmd, String args, FTPSession session1){
        cmdName = cmd;
        cmdArgs = args;
        isUserCmd = (Arrays.binarySearch(userCommands, cmd) >= 0);
        isDataCmd = (Arrays.binarySearch(dataCommands, cmd) >= 0);
        session = session1;
    }

    public boolean cmdAction() throws Exception {
        if (isUserCmd && !session.hasCurrentUser()) {
            session.controlWrite("530 Please login with USER and PASS.");
            return true;
        }
        if (isDataCmd && !session.connectData()) {
            return true;
        }
        switch (cmdName) {
            case "USER":
                handleUser(cmdArgs);
                break;
            case "PASS":
                handlePass(cmdArgs);
                break;
            case "SYST":
                handleSyst(cmdArgs);
                break;
            case "FEAT":
                handleFeat(cmdArgs);
                break;
            case "PWD":
                handlePwd(cmdArgs);
                break;
            case "CWD":
                handleCwd(cmdArgs);
                break;
            case "EPSV":
                handleEpsv(cmdArgs);
                break;
            case "NLST":
                handleNlst(cmdArgs);
                break;
            case "QUIT":
                return false;
            default:
                session.controlWrite("502 Command not implemented.");
                break;
        }
        if (isDataCmd) session.closeDataConn();
        return true;
    }

    //
    // Dealing with the CWD command.
    //
    // Acceptable arguments: .. OR . OR relative path name not including .. or .
    //
    private void handleCwd(String args) throws IOException
    {
        String filename = session.getCurrDirectory();

        //
        // First the case where we need to go back up a directory.
        //
        if (args.equals(".."))
        {
            int ind = filename.lastIndexOf(session.getFileSeparator());
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
            filename = filename + session.getFileSeparator() + args;
        }

        //
        // Now make sure that the specified directory exists, and doesn't
        // attempt to go to the FTP root's parent directory.  Note how we
        // use a "File" object to test if a file exists, is a directory, etc.
        //
        File f = new File(filename);

        if (f.exists() && f.isDirectory() && (filename.length() >= session.getRoot().length()))
        {
            session.setCurrDirectory(filename);
            session.controlWrite("250 The current directory has been changed to " + session.getCurrDirectory());
        }
        else
        {
            session.controlWrite("550 Requested action not taken. File unavailable.");
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
        session.setUserNumber(UserHandler.getUserNumber(args));
        session.controlWrite("331 User " + args + " accepted, provide password.");
        session.setUserName(args);
    }

    private void handlePass(String args) throws Exception
    {
        if (session.getUserNumber() == -1 ||  !UserHandler.checkPassword(session.getUserNumber(), args)) {
            session.controlWrite("530 Login incorrect.");
            session.setHasCurrentUser(false);
            return;
        }
        session.controlWrite("230 User " + session.getUserName() + " logged in.");
        session.setHasCurrentUser(true);
        session.setRoot(UserHandler.getHomeDir(session.getUserNumber()));
        session.setCurrDirectory(session.getRoot());
    }

    private void handleSyst(String args) throws IOException {
        session.controlWrite("215 UNIX Type: L8");
    }

    private void handleFeat(String args)throws IOException {
        session.controlWrite("211");
    }

    private void handlePwd(String args) throws IOException {
        session.controlWrite("257 \"" + session.getCurrDirectory() + "\" is the current directory.");
    }

    private void handleEpsv(String args) throws IOException {
        session.controlWrite("229 Entering Extended Passive Mode (|||" + session.createDataServSock() + "|)");
        session.setIsPassiveMode(true);
    }

    private void handleNlst(String args) throws IOException {
        String[] files = nlstHelper(args);
        session.controlWrite("150 Opening ASCII mode data connection for 'file list'.");
        String outLine = StringUtils.join(files, "\r\n");
        session.dataWriteASCII(outLine);
        session.closeDataConn();
        session.controlWrite("226 Transfer complete.");
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
        String filename = session.getCurrDirectory();
        if (args != null)
        {
            filename = filename + session.getFileSeparator() + args;
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
