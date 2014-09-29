package com.ddsc.networking;

/**
 * An instance of this object will be shared between the client and all Peer instances.
 * 
 * @author Stephen Chung & David DeSimone
 *
 */

public class PeerState {

	protected final String info_id;
	protected final String peer_id;
	
	// TODO determine format of bitfield of pieces downloaded and verified
	protected String have;
	// TODO some data structure for keeping track of which peer object is responsible for which piece
	
	
	public PeerState(String info_id, String peer_id) {
		this.info_id = info_id;
		this.peer_id = peer_id;
	}
	
	public synchronized void setHave(String have) {
		this.have = have;
	}
	
	public synchronized String getHave() {
		return have;
	}
	
}
