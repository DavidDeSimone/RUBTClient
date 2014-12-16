/*@author: David DeSimone, Stephen Chung
 * BitTorrent client build for Internet Technology
 * 
 */

package com.ddsc.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.ddsc.giventools.BencodingException;
import com.ddsc.giventools.TorrentInfo;
import com.ddsc.networking.Torrent;
import com.ddsc.networking.TorrentState;
import com.ddsc.networking.Tracker;

public class RUBTClient {

	public static final String EXTENSION = ".ser";
	public static final long SERIALIZE_DELAY = 10000L;
	
	protected static TorrentState tstate;
	protected static InetAddress ip;
	
	private static final boolean debug = false;

	/*
	 * @param args: Valid input should be in the form <torrent file> <file name to download>
	 * 
	 */
	public static void main(String[] args) {
		
		if (args.length < 2 || args.length > 3) {
			System.out.println("Usage: java RUBTClient [name of .torrent] [name of file to save to]"
					+ " [IP to download from]");
			System.exit(-1);
		}	
		
		ip = null;
		if (args.length == 3) {
			try {
				ip = InetAddress.getByName(args[2]);
			} catch (Exception e) {
				System.err.println("Unable to parse IP address " + args[2]);
				System.exit(-1);
			}
			if (ip == null) {
				System.err.println("Unable to parse IP address " + args[2]);
				System.exit(-1);				
			}
		}
		
		final String torrentPath = args[0];
		final String outputPath = args[1];
		
		File dotTorrent = getTorrentFiles(torrentPath);
		byte[] torbytes = getBytesFromTorrent(dotTorrent);
		
		//Extract the torrent info from the torrent file and capture it to 
		//an object
		TorrentInfo tinfo = getTorrentInfoFromTorrent(torbytes);
		
		File from = new File(torrentPath);
		File to = new File(outputPath);
		if (!from.exists()) {
			System.err.println("Unable to read torrent file " + from.getAbsolutePath());
			System.exit(-1);
		}
		try {
			// if file already exists, this method returns false without throwing an exception
			to.createNewFile();
		} catch (IOException e1) {
			System.err.println("Unable to create output file " + to.getAbsolutePath());
			System.exit(-1);
		}
		if (!to.canWrite()) {
			System.err.println("Unable to write to output file " + to.getAbsolutePath());
			System.exit(-1);
		}
		
		// Check for a previous session that was saved to disk
		final File file = new File(from.getName() + to.getName() + EXTENSION);
		if (file.exists()) {
			
			System.out.println("Found an existing session with same torrent and destination filename. Resuming.");
			FileInputStream fis;
			try {
				fis = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fis);
				tstate = (TorrentState) ois.readObject();
				fis.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			tstate.resumeTorrent(tinfo, ip);

		} else {
			System.out.println("Did not find an existing session. This torrent will be downloaded from scratch.");
			tstate = new TorrentState(tinfo, to, System.nanoTime());
			tstate.startTorrent(ip);
		}
		
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				synchronized(tstate) {
					synchronized(tstate.getTorrent()) {
						FileOutputStream fos;
						try {
							fos = new FileOutputStream(file);
							ObjectOutputStream oos = new ObjectOutputStream(fos);
							oos.writeObject(tstate);
							fos.close();
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}
			}
			
		}, SERIALIZE_DELAY, SERIALIZE_DELAY);
		
		System.out.println("Type 'quit' to exit. Your progress is being periodically saved to disk.");
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String s;
		try {
			while((s = in.readLine()) != null) {
				if (s.equals("quit")) {
					timer.cancel();
					Torrent tor = tstate.getTorrent();
					dPRINT("" + tor.getNumPeers() + " peers running.");
					tstate.getTorrent().terminatePeers();
					try {
						// wait for peers to exit
						System.out.println("Waiting on threads to exit...");
						Thread.sleep(2000L);
					} catch (InterruptedException e) {
						// do nothing
					}
					dPRINT("" + tor.getNumPeers() + " peers are still running. Exiting.");
					System.exit(0);
				} else {
					System.out.println("Unrecognized command: say 'quit' to exit program.");
					System.out.println("Your progress will be saved.");
				}
			}
		} catch (IOException e) {
		}
		
		
	}
	

	protected static TorrentInfo getTorrentInfoFromTorrent(byte[] torbytes) {
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
	protected static byte[] getBytesFromTorrent(File torrent) {
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
	 * Function used to get a file pointer to the torrent file from the filename
	 * 
	 */
	protected static File getTorrentFiles(String torrentName) {
		File fp = new File(torrentName);
		return fp;
	}
	
	
	public static void dPRINT(String s) {
		if(debug) {
			System.out.println(s);
		}
	}
	
	
	
	
}
