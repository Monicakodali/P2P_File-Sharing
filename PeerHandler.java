package src;

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.*;
import java.util.BitSet;
import java.nio.*;
import java.lang.*;

import src.ActualMessage;
import src.HandshakeMessage;
import src.PeerAdmin;

public class PeerHandler implements Runnable {
	private Socket client;
	private PeerAdmin peerAdmin;
	private String idEndPeer;
	private boolean isConnectionEstablished = false;
	private boolean initializer = false;
	private HandshakeMessage handshakeMsg;
	private volatile int downloadRate = 0;
	private volatile ObjectOutputStream out;
	private volatile ObjectInputStream in;

	public PeerHandler(Socket client, PeerAdmin admin) {
		this.client = client;
		this.peerAdmin = admin;
		initStreams();
		this.handshakeMsg = new HandshakeMessage(this.peerAdmin.getPeerID());

	}

	public PeerAdmin getPeerAdmin() {
		return this.peerAdmin;
	}

	public void initStreams() {
		try {
			this.out = new ObjectOutputStream(this.client.getOutputStream());
			this.out.flush();
			this.in = new ObjectInputStream(this.client.getInputStream());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized Socket getClient() {
		return this.client;
	}

	public void setIdEndPeer(String pid) {
		this.idEndPeer = pid;
		this.initializer = true;
	}

	public void run() {
		try {
			byte[] msg = this.handshakeMsg.generateHandShakeMessage();
			this.out.write(msg);
			this.out.flush();
			while (1 != 2) {
				if (!this.isConnectionEstablished) {
					byte[] response = new byte[32];
					this.in.readFully(response);
					this.processHandShakeMessage(response);
					if (this.peerAdmin.hasFile() || this.peerAdmin.getAvailabilityOf(this.peerAdmin.getPeerID()).cardinality() > 0) {
						this.sendBitField();
					}
				} 
				else {
					while (this.in.available() < 4) {
					}
					int respLen = this.in.readInt();
					byte[] response = new byte[respLen];
					this.in.readFully(response);
					char mType = (char) response[0];
					ActualMessage am = new ActualMessage();
					am.readActualMessage(respLen, response);
					if (mType == '0') {
						// Handles choke message
						this.peerAdmin.resetRequested(this.idEndPeer);
						this.peerAdmin.getLogger().chokingNeighbor(this.idEndPeer);
					} 
					else if (mType == '1') {
						// Handles unchoke message
						int requestindex = this.peerAdmin.checkForRequested(this.idEndPeer);
						if (requestindex == -1) {
							this.sendNotInterestedMessage();
						} 
						else {
							this.sendRequestMessage(requestindex);
						}
						this.peerAdmin.getLogger().unchokedNeighbor(this.idEndPeer);
					} 
					else if (mType == '2') {
						// Handles Interested Message
						this.peerAdmin.addToInterestedList(this.idEndPeer);
						this.peerAdmin.getLogger().receiveInterested(this.idEndPeer);
					} 
					else if (mType == '3') {
						// Handles NotInterested Message
						this.peerAdmin.removeFromInterestedList(this.idEndPeer);
						this.peerAdmin.getLogger().receiveNotInterested(this.idEndPeer);
					} 
					else if (mType == '4') {
						// Handles Have Message
						int pieceIndex = am.getPieceIndexFromPayload();
						this.peerAdmin.updatePieceAvailability(this.idEndPeer, pieceIndex);
						if (this.peerAdmin.checkIfAllPeersAreDone()) {
							this.peerAdmin.cancelChokes();
						}
						if (this.peerAdmin.checkIfInterested(this.idEndPeer)) {
							this.sendInterestedMessage();
						} 
						else {
							this.sendNotInterestedMessage();
						}
						this.peerAdmin.getLogger().receiveHave(this.idEndPeer, pieceIndex);
					} 
					else if (mType == '5') {
						// Handles BitField message
						BitSet bset = am.getBitFieldMessage();
						this.processBitFieldMessage(bset);
						if (!this.peerAdmin.hasFile()) {
							if (this.peerAdmin.checkIfInterested(this.idEndPeer)) {
								this.sendInterestedMessage();
							} 
							else {
								this.sendNotInterestedMessage();
							}
						}
					} 
					else if (mType == '6') {
						// Handles Request Message
						if (this.peerAdmin.getUnchokedList().contains(this.idEndPeer)
								|| (this.peerAdmin.getOptimisticUnchokedPeer() != null && this.peerAdmin.getOptimisticUnchokedPeer().compareTo(this.idEndPeer) == 0)) {
							int pieceIndex = am.getPieceIndexFromPayload();
							this.sendPieceMessage(pieceIndex, this.peerAdmin.readFromFile(pieceIndex));
						} 
					} 
					else if (mType == '7') {
						// Handle Piece Message
						int pieceIndex = am.getPieceIndexFromPayload();
						byte[] piece = am.getPieceFromPayload();
						this.peerAdmin.writeToFile(piece, pieceIndex);
						this.peerAdmin.updatePieceAvailability(this.peerAdmin.getPeerID(), pieceIndex);
						this.downloadRate++;
						Boolean alldone = this.peerAdmin.checkIfAllPeersAreDone();
						this.peerAdmin.getLogger().downloadPiece(this.idEndPeer, pieceIndex,
								this.peerAdmin.getCompletedPieceCount());
						this.peerAdmin.setRequestedInfo(pieceIndex, null);
						this.peerAdmin.broadcastHave(pieceIndex);
						if (this.peerAdmin.getAvailabilityOf(this.peerAdmin.getPeerID()).cardinality() != this.peerAdmin
								.getBitsCount()) {
							int requestindex = this.peerAdmin.checkForRequested(this.idEndPeer);
							if (requestindex != -1) {
								this.sendRequestMessage(requestindex);
							} 
							else {
								this.sendNotInterestedMessage();
							}
						} 
						else {
							this.peerAdmin.getLogger().downloadComplete();
							if (alldone) {
								this.peerAdmin.cancelChokes();
							}
							this.sendNotInterestedMessage();
						}
					} 
					else {
						System.out.println("Received other message");
					}
				}
			}
		} 
		catch (SocketException e) {
			System.out.println("Socket exception");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void send(byte[] obj) {
		try {
			this.out.write(obj);
			this.out.flush();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendChokedMessage() {
		try {
			ActualMessage am = new ActualMessage('0');
			this.send(am.generateActualMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendUnChokedMessage() {
		try {
			ActualMessage am = new ActualMessage('1');
			this.send(am.generateActualMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendInterestedMessage() {
		try {
			ActualMessage am = new ActualMessage('2');
			this.send(am.generateActualMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendNotInterestedMessage() {
		try {
			ActualMessage am = new ActualMessage('3');
			this.send(am.generateActualMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendHaveMessage(int pieceIndex) {
		try {
			byte[] bytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			ActualMessage am = new ActualMessage('4', bytes);
			this.send(am.generateActualMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendBitField() {
		try {
			BitSet myAvailability = this.peerAdmin.getAvailabilityOf(this.peerAdmin.getPeerID());
			ActualMessage am = new ActualMessage('5', myAvailability.toByteArray());
			this.send(am.generateActualMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendRequestMessage(int pieceIndex) {
		try {
			byte[] bytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			ActualMessage am = new ActualMessage('6', bytes);
			this.send(am.generateActualMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendPieceMessage(int pieceIndex, byte[] payload) {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			byte[] bytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			stream.write(bytes);
			stream.write(payload);
			ActualMessage am = new ActualMessage('7', stream.toByteArray());
			this.send(am.generateActualMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processHandShakeMessage(byte[] message) {
		try {
			this.handshakeMsg.readHandShakeMessage(message);
			this.idEndPeer = this.handshakeMsg.getPeerID();
			this.peerAdmin.addJoinedPeer(this, this.idEndPeer);
			this.peerAdmin.addJoinedThreads(this.idEndPeer, Thread.currentThread());
			this.isConnectionEstablished = true;
			if (this.initializer) {
				this.peerAdmin.getLogger().genTCPConnLogSender(this.idEndPeer);
			} 
			else {
				this.peerAdmin.getLogger().genTCPConnLogReceiver(this.idEndPeer);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processBitFieldMessage(BitSet b) {
		this.peerAdmin.updateBitset(this.idEndPeer, b);
	}

	public int getDownloadRate() {
		return this.downloadRate;
	}

	public void resetDownloadRate() {
		this.downloadRate = 0;
	}

}
