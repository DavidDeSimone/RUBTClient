package com.ddsc.networking;

public class Torrent implements Runnable{

	private TorrentState state;
	
	public Torrent(TorrentState state) {
		this.state = state;
		
		
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//Form the tracker object for this torrent
				Tracker tracker = new Tracker(state);
				Thread t = new Thread(tracker);
				t.run();
	}
	
	
}
