/*@author: David DeSimone, Stephen Chung
 * BitTorrent client build for Internet Technology
 * 
 */

package com.ddsc.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ddsc.giventools.BencodingException;
import com.ddsc.giventools.TorrentInfo;
import com.ddsc.networking.TorrentState;

public class RUBTClient {

	private static TorrentState[] states;
	private static File[] torToDownload;
	
	
	/*
	 * @param args: Valid input should be in the form <torrent file> <file name to download>
	 * 
	 */
	public static void main(String[] args) {
		checkArgs(args);
		
		//String torrentName = args[0];
	//	String fileName = args[1];
		
		String torrentName = "resources/project2.torrent";
		
		File torrent = getTorrentFiles(torrentName);
		byte[] torbytes = getBytesFromTorrent(torrent);
		
		//Extract the torrent info from the torrent file and capture it to 
		//an object
		TorrentInfo tinfo = getTorrentInfoFromTorrent(torbytes);
		
		
		//Create a torrent object that will actually spawn the tracker
		TorrentState tState = new TorrentState(tinfo);
		//tState.run();
		
		
	}
	
	public static TorrentInfo getTorrentInfoFromTorrent(byte[] torbytes) {
		TorrentInfo tinfo = null;
		
		try {
		tinfo = new TorrentInfo(torbytes);
		} catch(BencodingException e) { 
			System.out.println("Error with Bencoding/Decode");
		}
		
		return tinfo;
	}
	
	
	/*
	 * Function used to turn a torrent file pointer into a byte array of the data in
	 * the file itself
	 */
	public static byte[] getBytesFromTorrent(File torrent) {
		byte[] arry = null;
		
		try {
		Path path = torrent.toPath();
		arry = Files.readAllBytes(path);
		} catch(IOException e) {
			System.out.println("Error reading file bytes, file may not exist");
			System.exit(1);
		}
		
		return arry;
	}
	
	/*
	 * Function used to check the arguments given to the program
	 * for valditity
	 */
	private static void checkArgs(String[] args) {
		
	}
	
	
	/*
	 * Function used to get a file pointer to the torrent file from the filename
	 * 
	 */
	private static File getTorrentFiles(String torrentName) {
		File fp = new File(torrentName);
		return fp;
	}
	
}
