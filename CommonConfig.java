package src;

import java.io.*;
import java.util.*;

public class CommonConfig {
    public int noOfDesiredNeighbourPeers;
    public int unchokingInterval;
    public int optUnchokingInterval;
    public String fileName;
    public int fileSize;
    public int bitSize;

    public void loadCommonFile() {
        try {
            // System.out.println(System.getProperty("user.dir"));
            FileReader fobj = new FileReader("Common.cfg");
            Scanner fReader = new Scanner(fobj);
            while (fReader.hasNextLine()) {
                String line = fReader.nextLine();
                String[] temp = line.split(" ");
                if (temp[0].equals("NumberOfPreferredNeighbors")) {
                    this.noOfDesiredNeighbourPeers = Integer.parseInt(temp[1]);
                } 
                else if (temp[0].equals("UnchokingInterval")) {
                    this.unchokingInterval = Integer.parseInt(temp[1]);
                } 
                else if (temp[0].equals("OptimisticUnchokingInterval")) {
                    this.optUnchokingInterval = Integer.parseInt(temp[1]);
                } 
                else if (temp[0].equals("FileName")) {
                    this.fileName = temp[1];
                } 
                else if (temp[0].equals("FileSize")) {
                    this.fileSize = Integer.parseInt(temp[1]);
                } 
                else if (temp[0].equals("PieceSize")) {
                    this.bitSize = Integer.parseInt(temp[1]);
                } else {
                    // Do Nothing
                }
            }
            fReader.close();
        } 
        catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
