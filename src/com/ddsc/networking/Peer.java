package com.ddsc.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import com.ddsc.utils.Message;

public class Peer implements Runnable {

	private static final Charset utf8;
	private static final long KEEPALIVE_INTERVAL = 1000 * 120; // two minutes
	private static final int DEFAULT_TIMEOUT = 5000;	
	
	protected InetSocketAddress addr;
	protected TorrentState state;
	protected Tracker tracker;
	
	protected Socket socket;
	protected Timer keepAliveTimer;
	protected DataInputStream inputStream;
	protected DataOutputStream outputStream;
	
	protected boolean remoteChoked;
	protected boolean remoteInterested;
	protected boolean remoteHas[];
	
	public volatile boolean localChoked;
	public volatile boolean localInterested;
	public volatile boolean endThread;
	
	static {
		utf8 = Charset.forName("UTF-8");
	}
	
	/*
	 * @param addr
	 * The address of the peer
	 * @param state
	 * The torrent state object with the relevant object
	 */
	public Peer(InetSocketAddress addr, TorrentState state, Tracker tracker) {
		this.addr = addr;
		this.state = state;
		this.endThread = false;
		this.tracker = tracker;
	}
	
	/**
	 * Open a new socket, connect, and set up input / output streams.
	 * @return true if no exceptions occurred, otherwise false
	 */
	public boolean connect() {
		try {
			if (socket != null) {
				socket.close();
			}
			socket = new Socket();
			socket.connect(addr, DEFAULT_TIMEOUT);
			socket.setSoTimeout(DEFAULT_TIMEOUT);
			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Disconnect from the remote peer, closing resources.
	 * @return true if no exceptions occurred, otherwise false
	 */
	public boolean disconnect() {
		try {
			socket.close();
			inputStream.close();
			outputStream.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public boolean handshake() {
		byte[] infoHash = state.getInfo_hash().getBytes(utf8);
		byte[] peerId = state.getPeer_id().getBytes(utf8);
		byte[] handshake = Message.generateHandShake(infoHash, peerId);
		byte[] response = new byte[68];
		try {
			outputStream.write(handshake);
			outputStream.flush();
			inputStream.readFully(response);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		for (int i = 28; i < 48; i++) {
			if (handshake[i] != response[i]) {
				System.err.println("Peer's reponse info hash does not match!");
				return false;
			}
		}
		byte[] responsePeerId = Arrays.copyOfRange(response, 48, 68);
		for (int i = 0; i < 20; i++) {
			if (peerId[i] != responsePeerId[i]) {
				System.err.println("Peer's response ID does not match!");
				return false;
			}
		}
		return true;
		
	}
	
	public void run() {
		
		if (!connect()) {
			System.err.println("Could not connect to remote peer.");
			return;
		}
		
		keepAliveTimer = new Timer();
		keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				sendMessage(Message.MSG_KEEPALIVE);
			}
		}, KEEPALIVE_INTERVAL, KEEPALIVE_INTERVAL);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				
			}
		}).start();
		
		Message message = null;
		while(!endThread) {
			try {
				message = Message.read(inputStream);
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
			if (!handleMessage(message)) {
				System.err.println("Unable to process message: " + message.toString());
				break;
			}
		}
		
	}

	/**
	 * Handle a message that we have received.
	 * @param message
	 * @return
	 */
	private boolean handleMessage(Message message) {
		switch(message.getId()) {
		case Message.TYPE_KEEPALIVE:
			// do nothing
			break;
		case Message.TYPE_CHOKE:
			localChoked = true;
			break;
		case Message.TYPE_UNCHOKE:
			localChoked = false;
			break;
		case Message.TYPE_INTERESTED:
			remoteInterested = true;
			break;
		case Message.TYPE_NOT_INTERESTED:
			remoteInterested = false;
			break;
		case Message.TYPE_HAVE:
			remoteHas[message.getPieceIndex()] = true;
			if (getInterest(remoteHas)) {
				localInterested = true;
				sendMessage(Message.MSG_INTERESTED);
			}
			break;
		}
		return false;
	}

	protected void sendMessage(Message msg) {
		synchronized(outputStream) {
			try {
		      if (msg.getId() == Message.TYPE_INTERESTED)
		          localInterested = true;
		        if (msg.getId() == Message.TYPE_NOT_INTERESTED)
		        if (msg.getId() == Message.TYPE_CHOKE)
		          remoteChoked = true;
		        if (msg.getId() == Message.TYPE_UNCHOKE)
		          remoteChoked = false;
		        Message.write(outputStream, msg);
		     
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
