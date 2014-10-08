package com.ddsc.networking;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import com.ddsc.giventools.TorrentInfo;

public class TorrentState implements Serializable {

	private static final long serialVersionUID = 1L;
	
	// Obtained from .torrent file
	protected String info_hash;

	// Determined by client
	protected String peer_id;
	protected String own_port;
	
	// Current state
	protected int uploaded;
	protected int downloaded;
	protected int left;
	protected String event;
	
	// Obtained from tracker
	protected int interval;
	protected Set<InetSocketAddress> peers;
	
	public TorrentState(String info_hash, String peer_id) {
		this.info_hash = info_hash;
		this.peer_id = peer_id;
	}
	
	public TorrentState(TorrentInfo info) {
		
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

	public synchronized Set<InetSocketAddress> getPeers() {
		return peers;
	}

	public synchronized void updatePeers(Set<InetSocketAddress> remove, Set<InetSocketAddress> add) {
		if (remove != null) {
			peers.removeAll(remove);
		}
		if (add != null) {
			peers.addAll(add);
		}
	}
	
}
