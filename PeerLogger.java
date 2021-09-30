package src;

import java.util.*;
import java.io.*;
import java.text.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class PeerLogger {

    private String log_fname;
    private String peer_id;
    private FileHandler log_fhandler;
    private SimpleDateFormat datetime_format = null;
    private Logger peer_logger;

    public PeerLogger(String peer_id) {
        this.peer_id = peer_id;
        startLogger();
    }

    public void startLogger() {
        try {
            this.datetime_format = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
            this.log_fname = "log_peer_" + this.peer_id + ".log";
            this.log_fhandler = new FileHandler(this.log_fname, false);
            System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s %n");
            this.log_fhandler.setFormatter(new SimpleFormatter());
            this.peer_logger = Logger.getLogger("PeerLogs");
            this.peer_logger.setUseParentHandlers(false);
            this.peer_logger.addHandler(this.log_fhandler);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void genTCPConnLogSender(String peer) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO,
                "[" + currTime + "]: Peer [" + this.peer_id + "] makes a connection to Peer " + "[" + peer + "].");
    }

    public synchronized void genTCPConnLogReceiver(String peer) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO,
                "[" + currTime + "]: Peer [" + this.peer_id + "] is connected from Peer " + "[" + peer + "].");
    }

    public synchronized void downloadPiece(String peer, int ind, int pieces) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO,
                "[" + currTime + "]: Peer [" + this.peer_id + "] has downloaded the piece [" + String.valueOf(ind)
                        + "] from [" + peer + "]. Now the number of pieces it has is [" + String.valueOf(pieces)
                        + "].");
    }

    public synchronized void downloadComplete() {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO,
                "[" + currTime + "]: Peer [" + this.peer_id + "] has downloaded the complete file.");
    }

    public void closeLogger() {
        try {
            if (this.log_fhandler != null) {
                this.log_fhandler.close();
            }
        }
        catch (Exception e) {
            System.out.println("Failed to close peer logger");
            e.printStackTrace();
        }
    }

    public synchronized void changeOptimisticallyUnchokedNeighbor(String peer) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peer_id
                + "] has the optimistically unchoked neighbor [" + peer + "].");
    }

    public synchronized void changePreferredNeigbors(List<String> neighbors) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        String neighList = "";
        for (String neighbor : neighbors) {
            neighList += neighbor + ",";
        }
        neighList = neighList.substring(0, neighList.length() - 1);
        this.peer_logger.log(Level.INFO,
                "[" + currTime + "]: Peer [" + this.peer_id + "] has the preferred neighbors [" + neighList + "].");
    }

    public synchronized void unchokedNeighbor(String peer) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO,
                "[" + currTime + "]: Peer [" + this.peer_id + "] is unchoked by [" + peer + "].");
    }

    public synchronized void receiveInterested(String peer) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peer_id
                + "] received the ‘interested’ message from [" + peer + "].");
    }

    public synchronized void chokingNeighbor(String peer) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peer_id + "] is choked by [" + peer + "].");
    }

    public synchronized void receiveHave(String peer, int index) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peer_id
                + "] received the ‘have’ message from [" + peer + "] for the piece [" + String.valueOf(index) + "].");
    }

    public synchronized void receiveNotInterested(String peer) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.datetime_format.format(calendar.getTime());
        this.peer_logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peer_id
                + "] received the ‘not interested’ message from [" + peer + "].");
    }

}