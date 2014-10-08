package com.ddsc.main;

import java.io.File;

/**
 * For each torrent that is added to the program, spawn a thread that runs an
 * instance of this class. Each Torrent is responsible for launching associated
 * Tracker and Peer threads. It will also keep track of the state of each piece
 * of the torrent i.e. have, don't have, in progress.
 * 
 * @author David and Stephen
 */
public class Torrent implements Runnable {
	
	/**
	 * Constructor for the Torrent class. 
	 * @param metainfo is the .torrent metainfo file
	 */
	Torrent(File metainfo){
		
	}
	
	/**
	 * Parse the .torrent file, create the pieces 2D array, launch Tracker
	 * threads, launch Peer threads.
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	


}
