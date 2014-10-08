package com.ddsc.main;

public class PeerInfo {
	
	public enum Status {
		FREE, IN_PROGRESS, HAVE
	}
	
	int[][] pieces;
	
	public synchronized void update(int fileNum, int piece, Status s) {
		
	}
	
	public synchronized Status getStatus(int fileNum, int piece) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
}
