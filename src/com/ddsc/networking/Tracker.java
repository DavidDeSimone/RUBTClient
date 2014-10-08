package com.ddsc.networking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;

public class Tracker implements Runnable {

	//Current state that is driving the tracker
	protected TorrentState state;
	
	//List of peers
	protected List<Peer> peers;
	
	//List of values returned in a Tracker HTTP responce
	//Time between sending regular requests to the tracker
	protected int interval;
	
	//Id of the tracker we are talking to
	protected String tracker_id;
	
	//Number of seeders sent by the tracker
	protected int num_seeders;
	
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
		
		
		
		URI uri = new URIBuilder()
		   .setScheme("http")
		   .setHost(announce.getHost())
		   .setPort(announce.getPort())
		   .setPath("/")
		   .setParameter("info_hash", escape(state.info_hash))
		   .build();
		
		System.out.println("Forming connection to Tracker");
		System.out.println("Requesting: " + announce);
		try {
		HttpURLConnection connection = (HttpURLConnection) announce.openConnection();
		
		connection.setRequestMethod("GET");
		
		//All strings sent over the HTTP request must be properly escaped
		connection.setRequestProperty("info_hash", escape(state.info_hash));
		connection.setRequestProperty("peer_id", escape(state.peer_id));
		connection.setRequestProperty("port", escape(state.tracker_port));
		connection.setRequestProperty("uploaded", escape(String.valueOf(state.uploaded)));
		connection.setRequestProperty("downloaded", escape(String.valueOf(state.downloaded)));
		connection.setRequestProperty("left", escape(String.valueOf(state.left)));
		connection.setRequestProperty("compact", "0");
		connection.setRequestProperty("no_peer_id", "0");
		connection.setRequestProperty("event", escape(state.event));
		
		
		int responceCode = connection.getResponseCode();
		
		
		System.out.println(connection.getRequestProperty("failure reason"));
		System.out.println("Repsonce: " + responceCode);
		
		
		} catch(IOException e) {
			System.out.println("Error connecting to tracker!");
			System.exit(1);
		}
		
	}
	
	
	/*
	 * Formats a HTTP Get request URL based off paramaters
	 * in TorrentState
	 */
	
	
	/*
	 * Function used to escape strings for the tracker URL requsts.
	 * This may need to be in another class
	 */
	public String escape(String url) {
		String escaped = null;
		
		try {
		escaped = URLEncoder.encode(url, "ISO-8859-1");
		} catch(UnsupportedEncodingException e) {
			System.out.println("Unsupported Encoding Exception!");
		}
		return escaped;
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
