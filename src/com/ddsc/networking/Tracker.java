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

import com.ddsc.giventools.Bencoder2;
import com.ddsc.giventools.BencodingException;

public class Tracker implements Runnable {
	
	/*
	 * ByteBuffers for the responce codes returned by the tracker
	 */
	protected ByteBuffer FAILURE_REASON = ByteBuffer.wrap(new byte[] {
		'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o', 'n'
	});
	
	protected ByteBuffer WARNING_MESSAGE = ByteBuffer.wrap(new byte[] {
		'w', 'a', 'r', 'n', 'i', 'n', 'g', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e'	
	});
	
	protected ByteBuffer INTERVAL = ByteBuffer.wrap(new byte[] {
			'i', 'n', 't', 'e', 'r', 'v', 'a', 'l'	
	});
	
	protected ByteBuffer TRACKER_ID = ByteBuffer.wrap(new byte[] {
			't', 'r', 'a', 'c', 'k', 'e', 'r', ' ', 'i', 'd'
	});
	
	protected ByteBuffer COMPLETE = ByteBuffer.wrap(new byte[] {
		'c', 'o', 'm', 'p', 'l', 'e', 't', 'e'	
	});
	
	protected ByteBuffer INCOMPLETE = ByteBuffer.wrap(new byte[] {
		'i', 'n', 'c', 'o', 'm', 'p', 'l', 'e', 't', 'e'	
	});
	
	protected ByteBuffer PEERS = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', 's'
	});
	
	protected  ByteBuffer IP = ByteBuffer.wrap(new byte[] { 
			'i', 'p' });


	protected  ByteBuffer PORT = ByteBuffer.wrap(new byte[] { 
		'p','o', 'r', 't' });


    protected  ByteBuffer PEERID = ByteBuffer.wrap(new byte[] {
	'p', 'e', 'e', 'r', ' ', 'i', 'd' });




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
		
		
		
		
		System.out.println("Forming connection to Tracker");
		System.out.println("Requesting: " + announce);
		try {

			//We will need to form the correct URI to request information
			//from the tracker
			
			StringBuilder sb = new StringBuilder();
			sb.append(announce);
			sb.append("?");
			sb.append("info_hash=");
			sb.append(escape(state.info_hash));
			sb.append("&peer_id=");
			sb.append(escape(state.peer_id));
			sb.append("&port=");
			sb.append(escape(state.tracker_port));
			sb.append("&uploaded="); 
			sb.append(escape(String.valueOf(state.uploaded)));
			sb.append("&downloaded=");
			sb.append(escape(String.valueOf(state.downloaded)));
			sb.append("&left=");
			sb.append(escape(String.valueOf(state.left)));
			sb.append("&event=");
			sb.append(escape(state.event));
			

		announce = new URL(sb.toString());
		
		
		//Open the HTTP Request and get the responce.
		HttpURLConnection connection = (HttpURLConnection) announce.openConnection();
		connection.setRequestMethod("GET");
		
	
		int responceCode = connection.getResponseCode();
		System.out.println("Repsonce: " + responceCode);
		
		String line;
		BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		
		
		//Read in the trackers response
         while ((line = rd.readLine()) != null) {
        	try {
        		//The tracker will return a Hashmap of const ByteBuffers and objects
        		HashMap<ByteBuffer, Object> map = (HashMap<ByteBuffer, Object>)Bencoder2.decode(line.getBytes());
        		
        		List<HashMap<ByteBuffer, Object>> peer = (List<HashMap<ByteBuffer, Object>>)map.get(PEERS);
        		
        		for(HashMap<ByteBuffer, Object> pe: peer) {
        			ByteBuffer buff = (ByteBuffer)pe.get(PEERID);
        			
        			System.out.println(new String(buff.array()));
        		}
        		
        		
        	} catch(BencodingException e) {
        		e.printStackTrace();
        	}
        	 
            System.out.println(line);
           
         }
		
		} catch(IOException e) {
			System.out.println("Error connecting to tracker!");
			System.exit(1);
		} 
		
	}
	
	

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
