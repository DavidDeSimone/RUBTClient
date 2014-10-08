package com.ddsc.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
public class Tracker implements Runnable {

	public static final long DEFAULT_INTERVAL = 60000L;
	public static final int DEFAULT_TIMEOUT = 5000;
	
	protected InetSocketAddress addr;
	protected TorrentState state;
	protected List<InetSocketAddress> peers;
	protected Socket socket;
	
	public Tracker(InetSocketAddress addr, TorrentState state) {
		this.addr = addr;
		this.state = state;
	}
	
	public void run() {
		long period = DEFAULT_INTERVAL;
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				try {
					Socket socket = new Socket(addr.getAddress(), addr.getPort());
					socket.setSoTimeout(DEFAULT_TIMEOUT);
					socket.getInputStream();
					socket.getOutputStream();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}, period, period);
		
	}

	/**
	 * Peer asks for a piece to work on. Update the pieces array.
	 * @return
	 */
	public synchronized int[] assignPiece(TorrentState state) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	private void updatePeerList() {
		URI uri = new URIBuilder()
			.setScheme("http")
			.setHost(addr.getHostName())
			.setPath("/")
			.setParameter("","")
			.build();
		HttpGet httpget = new HttpGet(uri);
	}
	
	/**
	 * 
	 * @return
	 */
	public List<InetSocketAddress> getPeerList() {
		return peers;
	}
}
