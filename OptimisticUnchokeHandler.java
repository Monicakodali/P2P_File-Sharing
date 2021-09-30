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

public class OptimisticUnchokeHandler implements Runnable {
    private int intrvalt;
    private PeerAdmin peerAdmin;
    private Random rand = new Random();
    private ScheduledFuture<?> job = null;
    private ScheduledExecutorService scheduler = null;

    OptimisticUnchokeHandler(PeerAdmin padm) {
        this.peerAdmin = padm;
        this.intrvalt = padm.getOptimisticUnchockingInterval();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startJob() {
        this.job = this.scheduler.scheduleAtFixedRate(this, 6, this.intrvalt, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            String optimisUnchokedpeer = this.peerAdmin.getOptimisticUnchokedPeer();
            List<String> peersinterested = new ArrayList<String>(this.peerAdmin.getInterestedPeers());
            peersinterested.remove(optimisUnchokedpeer);
            int iLength = peersinterested.size();
            if (iLength > 0) {
                String nPeer = peersinterested.get(rand.nextInt(iLength));
                while (this.peerAdmin.getUnchokedList().contains(nPeer)) {
                    peersinterested.remove(nPeer);
                    iLength--;
                    if(iLength <= 0) {
                        nPeer = null;
                        break;
                    }
                    else {
                        nPeer = peersinterested.get(rand.nextInt(iLength));
                    }
                }
                this.peerAdmin.setOptimisticUnchokdPeer(nPeer);
                if(nPeer != null) {
                    PeerHandler nextPeerHandler = this.peerAdmin.getPeerHandler(nPeer);
                    nextPeerHandler.sendUnChokedMessage();
                    this.peerAdmin.getLogger()
                            .changeOptimisticallyUnchokedNeighbor(this.peerAdmin.getOptimisticUnchokedPeer());
                } 
                if (optimisUnchokedpeer != null && !this.peerAdmin.getUnchokedList().contains(optimisUnchokedpeer)) {
                    this.peerAdmin.getPeerHandler(optimisUnchokedpeer).sendChokedMessage();
                }  
            } 
            else {
                String currentOptimisticunchokdPeer = this.peerAdmin.getOptimisticUnchokedPeer();
                this.peerAdmin.setOptimisticUnchokdPeer(null);
                if (currentOptimisticunchokdPeer != null && !this.peerAdmin.getUnchokedList().contains(currentOptimisticunchokdPeer)) {
                    PeerHandler nextPeerHandler = this.peerAdmin.getPeerHandler(currentOptimisticunchokdPeer);
                    nextPeerHandler.sendChokedMessage();
                }
                if(this.peerAdmin.checkIfAllPeersAreDone()) {
                    this.peerAdmin.cancelChokes();
                }
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelJob() {
        this.scheduler.shutdownNow();
    }
}
