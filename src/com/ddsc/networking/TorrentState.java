package com.ddsc.networking;

import java.io.Serializable;
import java.util.List;

import com.ddsc.giventools.TorrentInfo;

public class TorrentState implements Serializable {

	public enum PieceState {
		NOT_STARTED, STARTED, DONE
	}
	
	private static final long serialVersionUID = 1L;
	
	// Obtained from .torrent file
	protected String tracker_ip;
	protected String tracker_port;
	protected String info_hash;

	// Determined by client
	protected String peer_id;
	protected String own_port;
	
	// Current state
	protected int uploaded;
	protected int downloaded;
	protected int left;
	protected String event;
	protected PieceState[] localHas;
	
	// Obtained from tracker
	protected int interval;
	protected List<Peer> peers;
	
	//Generate torrentInfo object for this torrent
	protected TorrentInfo info;
	
	//Reference to the torrent file for this state
	protected Torrent torrent;
	
	public TorrentState(TorrentInfo info) {
		this.info = info;
		
		//Set the TorrentState fields
		
		
		
		// Initialize the pieces state array
		localHas = new PieceState[info.piece_hashes.length];
		for(int i = 0; i < localHas.length; i++) {
			localHas[i] = PieceState.NOT_STARTED;
		}
		
		//Spawn and run the torrent for this state
		torrent = new Torrent(this);
		Thread t = new Thread(torrent);
		t.run();
	}
	
	public synchronized PieceState[] getPieceState() {
		return localHas;
	}
	
	/**
	 * A peer reports that it wishes to begin download a piece. Update the record of pieces.
	 * @param num piece to be downloaded
	 * @return true if this piece is needed, false if it is done or in progress
	 */
	public synchronized boolean lockPiece(int num) {
		if (localHas[num] == PieceState.NOT_STARTED) {
			localHas[num] = PieceState.STARTED;
			return true;
		}
		return false;
	}
	
	/**
	 * A peer reports that it has finished a piece. Update the record of pieces.
	 * @param num piece that was finished
	 */
	public synchronized void gotPiece(int num) {
		if (localHas[num] == PieceState.STARTED) {
			localHas[num] = PieceState.DONE;
		} else {
			throw new IllegalStateException("Finished a piece that wasn't started.");
		}
	}
	
	public synchronized TorrentInfo getTorrentInfo() {
		return info;
	}

	public synchronized String getTracker_ip() {
		return tracker_ip;
	}

	public synchronized void setTracker_ip(String tracker_ip) {
		this.tracker_ip = tracker_ip;
	}

	public synchronized String getTracker_port() {
		return tracker_port;
	}

	public synchronized void setTracker_port(String tracker_port) {
		this.tracker_port = tracker_port;
	}

	public synchronized String getInfo_hash() {
		return info_hash;
	}

	public synchronized void setInfo_hash(String info_hash) {
		this.info_hash = info_hash;
	}

	public synchronized String getPeer_id() {
		return peer_id;
	}

	public synchronized void setPeer_id(String peer_id) {
		this.peer_id = peer_id;
	}

	public synchronized String getOwn_port() {
		return own_port;
	}

	public synchronized void setOwn_port(String own_port) {
		this.own_port = own_port;
	}

	public synchronized int getUploaded() {
		return uploaded;
	}

	public synchronized void setUploaded(int uploaded) {
		this.uploaded = uploaded;
	}

	public synchronized int getDownloaded() {
		return downloaded;
	}

	public synchronized void setDownloaded(int downloaded) {
		this.downloaded = downloaded;
	}

	public synchronized int getLeft() {
		return left;
	}

	public synchronized void setLeft(int left) {
		this.left = left;
	}

	public synchronized String getEvent() {
		return event;
	}

	public synchronized void setEvent(String event) {
		this.event = event;
	}

	public synchronized int getInterval() {
		return interval;
	}

	public synchronized void setInterval(int interval) {
		this.interval = interval;
	}

	public synchronized List getPeers() {
		return peers;
	}

	public synchronized void setPeers(List peers) {
		this.peers = peers;
	}
	
}
