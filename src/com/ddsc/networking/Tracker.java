package com.ddsc.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.ddsc.giventools.Bencoder2;
import com.ddsc.giventools.BencodingException;
import com.ddsc.main.RUBTClient;

public class Tracker implements Runnable {

	/*
	 * ByteBuffers for the response codes returned by the tracker
	 */
	protected static final ByteBuffer FAILURE_REASON = ByteBuffer.wrap(new byte[] {
			'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o',
			'n' });

	protected static final ByteBuffer WARNING_MESSAGE = ByteBuffer.wrap(new byte[] {
			'w', 'a', 'r', 'n', 'i', 'n', 'g', ' ', 'm', 'e', 's', 's', 'a',
			'g', 'e' });

	protected static final ByteBuffer INTERVAL = ByteBuffer.wrap(new byte[] { 'i',
			'n', 't', 'e', 'r', 'v', 'a', 'l' });

	protected static final ByteBuffer TRACKER_ID = ByteBuffer.wrap(new byte[] { 't',
			'r', 'a', 'c', 'k', 'e', 'r', ' ', 'i', 'd' });

	protected static final ByteBuffer COMPLETE = ByteBuffer.wrap(new byte[] { 'c',
			'o', 'm', 'p', 'l', 'e', 't', 'e' });

	protected static final ByteBuffer INCOMPLETE = ByteBuffer.wrap(new byte[] { 'i',
			'n', 'c', 'o', 'm', 'p', 'l', 'e', 't', 'e' });

	protected static final ByteBuffer PEERS = ByteBuffer.wrap(new byte[] { 'p', 'e',
			'e', 'r', 's' });

	protected static final ByteBuffer IP = ByteBuffer.wrap(new byte[] { 'i', 'p' });

	protected static final ByteBuffer PORT = ByteBuffer.wrap(new byte[] { 'p', 'o',
			'r', 't' });

	protected static final ByteBuffer PEERID = ByteBuffer.wrap(new byte[] { 'p', 'e',
			'e', 'r', ' ', 'i', 'd' });

	//Default Minimum Interval for Tracker update
	protected static final int DEFAULT_UPDATE_INTERVAL = 180;
	
	
	// Current state that is driving the tracker
	protected TorrentState state;

	// List of peers
	protected List<Peer> peers;

	// List of values returned in a Tracker HTTP responce
	// Time between sending regular requests to the tracker
	protected int interval;

	// Id of the tracker we are talking to
	protected String tracker_id;

	// Number of seeders sent by the tracker
	protected int num_seeders;

	// Torrent the tracker represents
	protected Torrent torrent;
	
	//Minimum Interval for a tracker update
	protected int minInterval;
	
	//Represents if the tracker's auto-update has been initalized
	protected boolean updateInit = false;

	public Tracker(TorrentState state, Torrent torrent) {
		this.state = state;
		this.torrent = torrent;
	}

	public void run() {
		// Get the announce URL
		getTrackerInfoFromUrl();
		// When shutting down, tell the tracker.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				state.setEvent("stopped");
				//pushUpdates();
				state.setEvent("started");
			}
		});

	}

	private String getTrackerUrl() {

		URL announce = state.info.announce_url;

		StringBuilder sb = new StringBuilder();
		sb.append(announce);
		sb.append("?");
		sb.append("info_hash=");
		sb.append(escape(state.info_hash));
		sb.append("&peer_id=");
		sb.append(escape(new String(state.peer_id)));
		sb.append("&port=");
		sb.append(escape(String.valueOf(state.listeningPort)));
		sb.append("&uploaded=");
		sb.append(escape(String.valueOf(state.getUploaded())));
		sb.append("&downloaded=");
		sb.append(escape(String.valueOf(state.getDownloaded())));
		sb.append("&left=");
		sb.append(escape(String.valueOf(state.getLeft())));
		sb.append("&event=");
		sb.append(escape(state.getEvent()));

		return sb.toString();
	}

	/*
	 * Gets the current tracker information from the announce url in
	 * TorrentState.info.annouce via an HTTP GET request.
	 */
	private void getTrackerInfoFromUrl() {

		URL announce;

		RUBTClient.dPRINT("Forming connection to Tracker");

		try {

			// We will need to form the correct URI to request information
			// from the tracker

			String trackerURL = getTrackerUrl();

			announce = new URL(trackerURL);

			// Open the HTTP Request and get the responce.
			HttpURLConnection connection = (HttpURLConnection) announce
					.openConnection();
			connection.setRequestMethod("GET");

			int responceCode = connection.getResponseCode();
			RUBTClient.dPRINT("Repsonce: " + responceCode);

			String line;
			String total = new String();
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));

			while((line = rd.readLine()) != null) {
				total += line;
			}
			
			
			System.out.println("Total is " + total);
			// Read in the trackers response
		//	while ((line = rd.readLine()) != null) {
				try {
					// The tracker will return a Hashmap of const ByteBuffers
					// and objects
					HashMap<ByteBuffer, Object> map = (HashMap<ByteBuffer, Object>) Bencoder2
							.decode(total.getBytes());

					if(!updateInit) {
					try {
						minInterval = (Integer)map.get(INTERVAL);
					} catch(Exception e) {
						//Key Not Found
						minInterval = DEFAULT_UPDATE_INTERVAL;
					}	
					
					//Convert from Miliseconds to Seconds
					minInterval *= 1000;
					autoUpdate();
					updateInit = true;
					}

					if (torrent.getDownloadFrom() == null) {
						List<HashMap<ByteBuffer, Object>> peer = (List<HashMap<ByteBuffer, Object>>) map
								.get(PEERS);

						for (HashMap<ByteBuffer, Object> pe : peer) {

							checkCandidate(pe);
							ByteBuffer buff = (ByteBuffer) pe.get(PEERID);

							RUBTClient.dPRINT(new String(buff.array()));
						}
					} else {
						// User specified the IP to download from in a command-line argument
						torrent.spawnPeer(torrent.getDownloadFrom());
					}

				} catch (BencodingException e) {
					System.err.println("Unable to parse response from tracker.");
					e.printStackTrace();
				} catch(Exception e) {
					System.err.println("Unable to parse response from tracker, malformed response.");
					e.printStackTrace();
				}

				//RUBTClient.dPRINT(line);

		//	}

		} catch (IOException e) {
			RUBTClient.dPRINT("Error connecting to tracker!");
			System.exit(1);
		}

	}

	private void checkCandidate(HashMap<ByteBuffer, Object> peer) {
		ByteBuffer id = (ByteBuffer) peer.get(PEERID);
		String name = new String(id.array());

		// DEBUG
		torrent.spawnPeer(peer);
		// DEBUG

		/*
		 * if(name.length() < 6) { return; }
		 * 
		 * String prefix = name.substring(0, 6); if(prefix.equals("RUBT11")) {
		 * torrent.spawnPeer(peer, this); }
		 */

	}

	/*
	 * Function used to escape strings for the tracker URL requests.
	 */
	private static String escape(String url) {
		/*if(url == null) {
			RUBTClient.dPRINT("Cannot Print Null URL");
			return null;
		}*/
		
		
		
		String escaped = null;
		try {
			escaped = URLEncoder.encode(url, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			RUBTClient.dPRINT("Unsupported Encoding Exception!");
		}
		return escaped;
	}

	/**
	 * Contact the tracker now, telling it our current state.
	 */
	public void pushUpdates() {

		URL announce;

		RUBTClient.dPRINT("Forming connection to Tracker");

		try {
			// We will need to form the correct URI to request information
			// from the tracker

			String trackerURL = getTrackerUrl();

			announce = new URL(trackerURL);

			// Open the HTTP Request and get the responce.
			HttpURLConnection connection = (HttpURLConnection) announce
					.openConnection();
			connection.setRequestMethod("GET");
			
			connection.getResponseCode();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void autoUpdate() {
		
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Timer keepAliveTimer = new Timer();
				keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						//getTrackerInfoFromUrl();
						pushUpdates();
					}
				}, minInterval, minInterval);
				
			}
		});
		t.start();
		
		
		
		
	}
	
	
	
}
