package com.ddsc.networking;

import java.util.List;

public class Tracker implements Runnable {

	protected TorrentState state;
	
	public Tracker(TorrentState state) {
		this.state = state;
	}
	
	public void run() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Peer asks for a piece to work on. Update the pieces array.
	 * @return
	 */
	public synchronized int[] assignPiece(TorrentState state) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	public List getPeers() {
		throw new UnsupportedOperationException("Not implemented.");
	}
}
