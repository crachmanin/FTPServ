/**
 * Created by christianrachmaninoff on 8/10/15.
 */
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class UserHandler {
    private static final String userConfigPath = "config/users.cfg";
    // file should contain one line per user, tab delimited with username, password and home directory specified

    private static java.util.List<String> lines;
    private static ArrayList<String> users;
    private static ArrayList<String> passwords;
    private static ArrayList<String> homeDirs;

    public static void init() throws IOException{
        File configFile = new File(userConfigPath);
        if (!configFile.exists())
            throw new IOException("Config file does not exist");
        users = new ArrayList<>();
        passwords = new ArrayList<>();
        homeDirs = new ArrayList<>();
        lines = Files.readAllLines(Paths.get(userConfigPath), Charset.defaultCharset());
        for (int i = 0; i < lines.size(); i++){
            String[] splitLine = lines.get(i).split("\t");
            if (splitLine.length != 3) {
                throw new IOException("Error reading user config");
            }
            users.add(splitLine[0]);
            passwords.add(splitLine[1]);
            homeDirs.add(splitLine[2]);
        }
    }

    public static int getUserNumber(String userToCheck){
        for (int i = 0; i < users.size(); i++){
            if (userToCheck.equals(users.get(i)))
                return i;
        }
        return -1;
    }

    public static boolean checkPassword(int userNumber, String passwordToCheck){
        return (passwordToCheck.equals(passwords.get(userNumber)));
    }

    public static String getHomeDir(int userNumber){
        return homeDirs.get(userNumber);
    }
}
