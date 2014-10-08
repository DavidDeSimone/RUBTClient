package com.ddsc.networking;

import java.nio.ByteBuffer;

public class Torrent implements Runnable{

	private TorrentState state;
	
	public Torrent(TorrentState state) {
		this.state = state;
		
		
		
	}
	
	public static String generatePeerId() {
		ByteBuffer byt = ByteBuffer.wrap(new byte[] {
		'1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
		'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
		'k'
		});
		String ret = byt.toString();
		return ret;
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
