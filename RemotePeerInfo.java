package src;
public class RemotePeerInfo {
	public String peerId;
	public String peerLocation;
	public int peerPortNo;
	public int containsFileFlag;

	public RemotePeerInfo(String pId, String pAddress, String pPort, String cFile) {
		this.peerId = pId;
		this.peerLocation = pAddress;
		this.peerPortNo = Integer.parseInt(pPort);
		this.containsFileFlag = Integer.parseInt(cFile);
	}
	
}
