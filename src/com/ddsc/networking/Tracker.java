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

	public List getPeers() {
		throw new UnsupportedOperationException("Not implemented.");
	}
}
