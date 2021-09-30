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

public class ChokeHandler implements Runnable {
    private int intrvalt;
    private int prefNeighborcount;
    private PeerAdmin peerAdmin;
    private Random rand = new Random();
    private ScheduledFuture<?> job = null;
    private ScheduledExecutorService scheduler = null;

    ChokeHandler(PeerAdmin padm) {
        this.peerAdmin = padm;
        this.intrvalt = padm.getUnchockingInterval();
        this.prefNeighborcount = padm.getNoOfPreferredNeighbors();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startJob() {
        this.job = this.scheduler.scheduleAtFixedRate(this, 6, this.intrvalt, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            HashSet<String> listOfUnchoked = new HashSet<>(this.peerAdmin.getUnchokedList());
            HashSet<String> listn = new HashSet<>();
            List<String> peerinterested = new ArrayList<String>(this.peerAdmin.getInterestedPeers());
            if (peerinterested.size() > 0) {
                int prefi = Math.min(this.prefNeighborcount, peerinterested.size());
                if (this.peerAdmin.getCompletedPieceCount() == this.peerAdmin.getBitsCount()) {
                    int i=0;
                    while(i<prefi)
                    {
                        String nPeer = peerinterested.get(this.rand.nextInt(peerinterested.size()));
                        PeerHandler nextPeerHandler = this.peerAdmin.getPeerHandler(nPeer);
                        while (listn.contains(nPeer)) {
                            nPeer = peerinterested.get(this.rand.nextInt(peerinterested.size()));
                            nextPeerHandler = this.peerAdmin.getPeerHandler(nPeer);
                        }
                        if (!listOfUnchoked.contains(nPeer)) {
                            if (this.peerAdmin.getOptimisticUnchokedPeer() == null
                                    || this.peerAdmin.getOptimisticUnchokedPeer().compareTo(nPeer) != 0) {
                                nextPeerHandler.sendUnChokedMessage();
                            }
                        } 
                        else {
                            listOfUnchoked.remove(nPeer);
                        }
                        listn.add(nPeer);
                        nextPeerHandler.resetDownloadRate();
                        i++;
                    }
                } 
                else {
                    Map<String, Integer> downloads = new HashMap<>(this.peerAdmin.getDownloadRates());
                    Map<String, Integer> rates = downloads.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
                    Iterator<Map.Entry<String, Integer>> iterator = rates.entrySet().iterator();

                    for (int c=0;c < prefi && iterator.hasNext();c++) {

                        Map.Entry<String, Integer> ent = iterator.next();
                        if (peerinterested.contains(ent.getKey())) {
                            PeerHandler nextPeerHandler = this.peerAdmin.getPeerHandler(ent.getKey());
                            if (!listOfUnchoked.contains(ent.getKey())) {
                                String optimUnchoke = this.peerAdmin.getOptimisticUnchokedPeer();
                                if (optimUnchoke == null || optimUnchoke.compareTo(ent.getKey()) != 0) {
                                    nextPeerHandler.sendUnChokedMessage();
                                }
                            } 
                            else {
                                listOfUnchoked.remove(ent.getKey());
                            }
                            listn.add(ent.getKey());
                            nextPeerHandler.resetDownloadRate();
                            
                        }
                    }
                }
                this.peerAdmin.updateUnchokedList(listn);
                if(listn.size() > 0){
                    this.peerAdmin.getLogger().changePreferredNeigbors(new ArrayList<>(listn));
                }
                for (String p : listOfUnchoked) {
                    PeerHandler nextPeerHandler = this.peerAdmin.getPeerHandler(p);
                    nextPeerHandler.sendChokedMessage();
                }
            } 
            else {
                this.peerAdmin.resetUnchokedList();
                for (String p : listOfUnchoked) {
                    PeerHandler nextPeerHandler = this.peerAdmin.getPeerHandler(p);
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
