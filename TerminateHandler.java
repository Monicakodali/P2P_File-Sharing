package src;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.nio.*;
import java.lang.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.*;
import java.util.concurrent.TimeUnit;

public class TerminateHandler implements Runnable {
    private int interval;
    private ScheduledFuture<?> job = null;
    private PeerAdmin peer_admin;
    private ScheduledExecutorService scheduler = null;
    private Random random = new Random();

    TerminateHandler(PeerAdmin peeradmin) {
        this.peer_admin = peeradmin;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startJob(int time_interval) {
        this.interval = time_interval*2;
        this.job = scheduler.scheduleAtFixedRate(this, 30, this.interval, TimeUnit.SECONDS);
    }

    public void cancelJob() {
        this.scheduler.shutdownNow();
    }

    public void run() {
        try {
            if(this.peer_admin.checkIfDone()) {
                this.peer_admin.closeHandlers();
                this.cancelJob();
            }
        } 
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
