package src;
import java.util.*;

import src.RemotePeerInfo;

import java.io.*;
public class PeerInfoConfig {
	private HashMap<String,RemotePeerInfo> peer_info;
	private ArrayList<String> peer_list;

	public PeerInfoConfig(){
		this.peer_info = new HashMap<>();
		this.peer_list = new ArrayList<>();
	}

	public void loadConfigFile()
	{
		String s;
		BufferedReader input;
		try {
			input = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while((s = input.readLine()) != null) {
				String[] tok = s.split("\\s+");
				this.peer_info.put(tok[0],new RemotePeerInfo(tok[0], tok[1], tok[2], tok[3]));
				this.peer_list.add(tok[0]);
			}
			input.close();
		}
		catch (Exception ex) {
			System.out.println(ex.toString());
		}
	}

	public RemotePeerInfo getPeerConfig(String peerID){
		return this.peer_info.get(peerID);
	}

	public HashMap<String, RemotePeerInfo> getPeer_info(){
		return this.peer_info;
	}

	public ArrayList<String> getPeer_list(){
		return this.peer_list;
	}
}
