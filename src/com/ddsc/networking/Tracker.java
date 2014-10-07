package com.ddsc.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class Tracker implements Runnable {

	//Current state that is driving the tracker
	protected TorrentState state;
	
	//List of peers
	protected List<Peer> peers;
	
	public Tracker(TorrentState state) {
		this.state = state;
	}
	
	public void run() {
		//Get the announce URL
		getTrackerInfoFromUrl();
		
	}
	
	/*
	 * Gets the current tracker information from the announce url in TorrentState.info.annouce
	 * via an HTTP GET request. 
	 */
	private void getTrackerInfoFromUrl() {
		URL announce = state.info.announce_url;
		
		System.out.println("Forming connection to Tracker");
		System.out.println("Requesting: " + announce);
		try {
		HttpURLConnection connection = (HttpURLConnection) announce.openConnection();
		
		connection.setRequestMethod("GET");
		
		connection.addRequestProperty("info_hash", state.info_hash);
		connection.addRequestProperty("peer_id", state.peer_id);
		connection.addRequestProperty("port", state.tracker_port);
		connection.addRequestProperty("uploaded", String.valueOf(state.uploaded));
		connection.addRequestProperty("downloaded", String.valueOf(state.downloaded));
		connection.addRequestProperty("left", String.valueOf(state.left));
		connection.addRequestProperty("compact", "0");
		connection.addRequestProperty("no_peer_id", "0");
		connection.addRequestProperty("event", state.event);
		
		int responceCode = connection.getResponseCode();
		System.out.println("Repsonce: " + responceCode);
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(connection.getInputStream()));
		
		String beEncode;
		
		while((beEncode = in.readLine()) != null) {
			System.out.println(beEncode);
		}
		
		
		} catch(IOException e) {
			System.out.println("Error connecting to tracker!");
			System.exit(1);
		}
		
	}

	/**
	 * Peer asks for a piece to work on. Update the pieces array.
	 * @return
	 */
	public synchronized int[] assignPiece(TorrentState state) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	public List<Peer> getPeers() {
		throw new UnsupportedOperationException("Not implemented.");
	}
}
