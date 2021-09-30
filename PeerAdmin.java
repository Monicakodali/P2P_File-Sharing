package src;

import java.io.*;
import java.lang.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import src.PeerHandler;
import src.PeerInfoConfig;
import src.PeerLogger;
import src.PeerServer;
import src.RemotePeerInfo;
import src.ChokeHandler;
import src.OptimisticUnchokeHandler;

public class PeerAdmin {
	private String peerID;
	private RemotePeerInfo currConfig;
	private HashMap<String, RemotePeerInfo> peerMappingInfo;
	private ArrayList<String> peerList;
	private volatile HashMap<String, PeerHandler> connectedPeers;
	private volatile HashMap<String, Thread> connectedThreads;
	private volatile ServerSocket client;
	private PeerServer server;
	private CommonConfig configurations;
	private PeerInfoConfig peerConfigInfo;
	private volatile PeerLogger logger;
	private volatile HashMap<String, BitSet> bitsAvailability;
	private volatile String[] requestedInfo;
	private volatile HashSet<String> unchokedPeers;
	private volatile HashSet<String> interestedPeers;
	private volatile String optimisticUnchokedPeer;
	private int bitsCount;
	private volatile RandomAccessFile randomFile;
	private volatile ChokeHandler chokeHandler;
	private volatile OptimisticUnchokeHandler optUnchockeHandler;
	private volatile TerminateHandler terminateHandler;
	private volatile HashMap<String, Integer> downloadRate;
	private Thread serverThread;
	private volatile Boolean isFinished;

	public PeerAdmin(String peerID) {
		this.peerID = peerID;
		this.peerMappingInfo = new HashMap<>();
		this.bitsAvailability = new HashMap<>();
		this.peerList = new ArrayList<>();
		this.connectedPeers = new HashMap<>();
		this.connectedThreads = new HashMap<>();
		this.configurations = new CommonConfig();
		this.peerConfigInfo = new PeerInfoConfig();
		this.logger = new PeerLogger(this.peerID);
		this.isFinished = false;
		this.unchokedPeers = new HashSet<>();
		this.interestedPeers = new HashSet<>();
		this.initPeer();
		this.chokeHandler = new ChokeHandler(this);
		this.downloadRate = new HashMap<>();
		this.optUnchockeHandler = new OptimisticUnchokeHandler(this);
		this.terminateHandler = new TerminateHandler(this);
		this.chokeHandler.startJob();
		this.optUnchockeHandler.startJob();
	}

	public void initPeer() {
		try {
			this.configurations.loadCommonFile();
			this.peerConfigInfo.loadConfigFile();
			this.bitsCount = this.calcPieceCount();
			this.requestedInfo = new String[this.bitsCount];
			this.currConfig = this.peerConfigInfo.getPeerConfig(this.peerID);
			this.peerMappingInfo = this.peerConfigInfo.getPeer_info();
			this.peerList = this.peerConfigInfo.getPeer_list();
			String filepath = "peer_" + this.peerID;
			File file = new File(filepath);
			file.mkdir();
			String filename = filepath + "/" + getFileName();
			file = new File(filename);
			if (!hasFile()) {
				file.createNewFile();
			}
			this.randomFile = new RandomAccessFile(file, "rw");
			if (!hasFile()) {
				this.randomFile.setLength(this.getFileSize());
			}
			this.initializePieceAvailability();
			this.startServer();
			this.createNeighbourConnections();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startServer() {
		try {
			this.client = new ServerSocket(this.currConfig.peerPortNo);
			this.server = new PeerServer(this.peerID, this.client, this);
			this.serverThread = new Thread(this.server);
			this.serverThread.start();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createNeighbourConnections() {
		try {
			Thread.sleep(5000);
			for (String pid : this.peerList) {
				if (pid.equals(this.peerID)) {
					break;
				} 
				else {
					RemotePeerInfo peer = this.peerMappingInfo.get(pid);
					Socket temp = new Socket(peer.peerLocation, peer.peerPortNo);
					PeerHandler p = new PeerHandler(temp, this);
					p.setIdEndPeer(pid);
					this.addJoinedPeer(p, pid);
					Thread t = new Thread(p);
					this.addJoinedThreads(pid, t);
					t.start();
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initializePieceAvailability() {
		for (String pid : this.peerMappingInfo.keySet()) {
			BitSet availability = new BitSet(this.bitsCount);
			if (this.peerMappingInfo.get(pid).containsFileFlag == 1) {
				availability.set(0, this.bitsCount);
				this.bitsAvailability.put(pid, availability);
			} 
			else {
				availability.clear();
				this.bitsAvailability.put(pid, availability);
			}
		}
	}

	public synchronized void writeToFile(byte[] data, int pieceindex) {
		try {
			int position = this.getPieceSize() * pieceindex;
			this.randomFile.seek(position);
			this.randomFile.write(data);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized byte[] readFromFile(int pieceindex) {
		try {
			int position = this.getPieceSize() * pieceindex;
			int size = this.getPieceSize();
			if (pieceindex == getBitsCount() - 1) {
				size = this.getFileSize() % this.getPieceSize();
			}
			this.randomFile.seek(position);
			byte[] data = new byte[size];
			this.randomFile.read(data);
			return data;
		} 
		catch (Exception e) {
			e.printStackTrace();

		}
		return new byte[0];
	}

	public HashMap<String, Integer> getDownloadRates() {
		HashMap<String, Integer> rates = new HashMap<>();
		for (String key : this.connectedPeers.keySet()) {
			rates.put(key, this.connectedPeers.get(key).getDownloadRate());
		}
		return rates;
	}

	public synchronized void broadcastHave(int pieceIndex) {
		for (String key : this.connectedPeers.keySet()) {
			this.connectedPeers.get(key).sendHaveMessage(pieceIndex);
		}
	}

	public synchronized void updatePieceAvailability(String peerID, int index) {
		this.bitsAvailability.get(peerID).set(index);
	}

	public synchronized void updateDownloadRate(String endpeerid) {
		this.downloadRate.put(endpeerid, this.downloadRate.get(endpeerid) + 1);
	}

	public synchronized void updateBitset(String peerID, BitSet b) {
		this.bitsAvailability.remove(peerID);
		this.bitsAvailability.put(peerID, b);
	}

	public synchronized void addJoinedPeer(PeerHandler p, String endpeerid) {
		this.connectedPeers.put(endpeerid, p);
	}

	public synchronized void addJoinedThreads(String epeerid, Thread th) {
		this.connectedThreads.put(epeerid, th);
	}

	public synchronized HashMap<String, Thread> getConnectedThreads() {
		return this.connectedThreads;
	}

	public PeerHandler getPeerHandler(String peerid) {
		return this.connectedPeers.get(peerid);
	}

	public BitSet getAvailabilityOf(String pid) {
		return this.bitsAvailability.get(pid);
	}

	public synchronized boolean checkIfInterested(String endpeerid) {
		BitSet end = this.getAvailabilityOf(endpeerid);
		BitSet mine = this.getAvailabilityOf(this.peerID);
		for (int i = 0; i < end.size() && i < this.bitsCount; i++) {
			if (end.get(i) == true && mine.get(i) == false) {
				return true;
			}
		}
		return false;
	}

	public synchronized void setRequestedInfo(int id, String peerID) {
		this.requestedInfo[id] = peerID;
	}

	public synchronized int checkForRequested(String endpeerid) {
		BitSet end = this.getAvailabilityOf(endpeerid);
		BitSet mine = this.getAvailabilityOf(this.peerID);
		for (int i = 0; i < end.size() && i < this.bitsCount; i++) {
			if (end.get(i) == true && mine.get(i) == false && this.requestedInfo[i] == null) {
				setRequestedInfo(i, endpeerid);
				return i;
			}
		}
		return -1;
	}

	public synchronized void resetRequested(String endpeerid) {
		for (int i = 0; i < this.requestedInfo.length; i++) {
			if (this.requestedInfo[i] != null && this.requestedInfo[i].compareTo(endpeerid) == 0) {
				setRequestedInfo(i, null);
			}
		}
	}

	public String getPeerID() {
		return this.peerID;
	}

	public PeerLogger getLogger() {
		return this.logger;
	}

	public boolean hasFile() {
		return this.currConfig.containsFileFlag == 1;
	}

	public int getNoOfPreferredNeighbors() {
		return this.configurations.noOfDesiredNeighbourPeers;
	}

	public int getUnchockingInterval() {
		return this.configurations.unchokingInterval;
	}

	public int getOptimisticUnchockingInterval() {
		return this.configurations.optUnchokingInterval;
	}

	public String getFileName() {
		return this.configurations.fileName;
	}

	public int getFileSize() {
		return this.configurations.fileSize;
	}

	public int getPieceSize() {
		return this.configurations.bitSize;
	}

	public int calcPieceCount() {
		int len = (getFileSize() / getPieceSize());
		if (getFileSize() % getPieceSize() != 0) {
			len += 1;
		}
		return len;
	}

	public int getBitsCount() {
		return this.bitsCount;
	}

	public int getCompletedPieceCount() {
		return this.bitsAvailability.get(this.peerID).cardinality();
	}

	public synchronized void addToInterestedList(String endPeerId) {
		this.interestedPeers.add(endPeerId);
	}

	public synchronized void removeFromInterestedList(String endPeerId) {
		if (this.interestedPeers != null) {
			this.interestedPeers.remove(endPeerId);
		}
	}

	public synchronized void resetInterestedList() {
		this.interestedPeers.clear();
	}

	public synchronized HashSet<String> getInterestedPeers() {
		return this.interestedPeers;
	}

	public synchronized boolean addUnchokedPeer(String peerid) {
		return this.unchokedPeers.add(peerid);
	}

	public synchronized HashSet<String> getUnchokedList() {
		return this.unchokedPeers;
	}

	public synchronized void resetUnchokedList() {
		this.unchokedPeers.clear();
	}

	public synchronized void updateUnchokedList(HashSet<String> newSet) {
		this.unchokedPeers = newSet;
	}

	public synchronized void setOptimisticUnchokdPeer(String peerid) {
		this.optimisticUnchokedPeer = peerid;
	}

	public synchronized String getOptimisticUnchokedPeer() {
		return this.optimisticUnchokedPeer;
	}

	public synchronized boolean checkIfAllPeersAreDone() {
		for (String peer : this.bitsAvailability.keySet()) {
			if (this.bitsAvailability.get(peer).cardinality() != this.bitsCount) {
				return false;
			}
		}
		return true;
	}

	public synchronized OptimisticUnchokeHandler getoptHandler() {
		return this.optUnchockeHandler;
	}

	public synchronized ChokeHandler getchHandler() {
		return this.chokeHandler;
	}

	public synchronized RandomAccessFile getRefFile() {
		return this.randomFile;
	}

	public synchronized ServerSocket getClient() {
		return this.client;
	}

	public synchronized Thread getServerThread() {
		return this.serverThread;
	}

	public synchronized Boolean checkIfDone() {
		return this.isFinished;
	}

	public synchronized void closeHandlers() {
		for (String peer : this.connectedThreads.keySet()) {
			this.connectedThreads.get(peer).stop();
		}
	}

	public synchronized void cancelChokes() {
		try {
			this.getoptHandler().cancelJob();
			this.getchHandler().cancelJob();
			this.resetUnchokedList();
			this.setOptimisticUnchokdPeer(null);
			this.resetInterestedList();
			this.getRefFile().close();
			this.getLogger().closeLogger();
			this.getClient().close();
			this.getServerThread().stop();
			this.isFinished = true;
			this.terminateHandler.startJob(6);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
