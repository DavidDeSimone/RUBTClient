package com.ddsc.networking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.ddsc.main.RUBTClient;
import com.ddsc.utils.Message;

public class Torrent implements Runnable, Serializable {

	public static final int MAX_UNCHOKED = 6;
	public static final int MAX_CONNECTIONS = 30;
	public static final int CHOKE_CHECK_INTERVAL = 30000;
	private final static int FINISHED = 2;
	private final static int DOWNLOADING = 1;
	private final static int FREE = 0;

	private TorrentState state;

	// The array of file data that will be turned into the file
	// on the system
	byte[][] file;

	// Integer array used to determine the status of a piece of the file
	private int[] pieces;

	protected transient Tracker tracker;
	protected transient Semaphore available;
	private transient Vector<Peer> peers;
	private transient HashSet<InetAddress> ip_addrs;
	public transient InetAddress downloadFrom;

	/**
	 * Construct a new torrent.
	 * 
	 * @param state a TorrentState object to be shared by this Torrent and
	 * any Tracker or Peer threads that are created
	 */
	public Torrent(TorrentState state) {
		this.state = state;

		file = new byte[state.num_pieces][];
		System.out.println("Creating pieces array of size " + state.num_pieces);
		pieces = new int[state.num_pieces];
		initializeTransients();
	}

	
	private void initializeTransients() {
		tracker = new Tracker(state, this);
		peers = new Vector<Peer>();
		available = new Semaphore(MAX_UNCHOKED, true);
		ip_addrs = new HashSet<InetAddress>();
		setDownloadFrom(null);
	}

	/**
	 * Resuming a torrent.
	 * 
	 * Set all DOWNLOADING pieces to FREE (i.e. realize that all pieces that
	 * were in progress are lost).
	 */
	public void resume() {
		initializeTransients();
		for (int i = 0; i < pieces.length; i++) {
			// System.out.println("Piece " + i + " is " + pieces[i]);
			if (pieces[i] == DOWNLOADING) {
				pieces[i] = FREE;
			}
		}
		if (state.getLeft() == 0) {
			System.out.println("This torrent has finished downloading. Now seeding.");
		}
	}

	public void terminatePeers() {
		for (Peer p : peers) {
			p.endThread = true;
		}
	}

	public int getNumPeers() {
		return peers.size();
	}

	protected void spawnPeer(InetAddress ip) {
		int socket = TorrentState.DEFAULT_LISTENING_PORT;
		for (int i = 0; i < 10; i++) {
			InetSocketAddress addr = new InetSocketAddress(ip, socket + i);
			synchronized(ip_addrs) {
				if(!ip_addrs.contains(addr) && ip_addrs.size() < MAX_CONNECTIONS) {
					ip_addrs.add(addr.getAddress());
					Peer pen = new Peer(addr, state, this);
					Thread t = new Thread(pen);
					t.start();

					synchronized(peers) {
						peers.add(pen);
					}

				}
			}
		}
	}
	
	/*
	 * Spawns a peer for the given information provided from the tracker
	 */
	protected void spawnPeer(HashMap<ByteBuffer, Object> peer) {
		if (downloadFrom != null) {
			throw new IllegalStateException("This method should not be called when IP to"
					+ " download from has been set on the commandline.");
		}
		ByteBuffer id = (ByteBuffer) peer.get(Tracker.PEERID);
		ByteBuffer ip = (ByteBuffer) peer.get(Tracker.IP);
		int port_num = (Integer) peer.get(Tracker.PORT);

		String id_s = new String(id.array());
		String ip_s = new String(ip.array());

		RUBTClient.dPRINT("Connecting to " + ip_s + " at port " + port_num);

		// Hard Coded accepted values for peers
		RUBTClient.dPRINT("Ip Check @ " + ip_s);
		 if (ip_s.equals("128.6.171.130") || ip_s.equals("128.6.171.131")) {
		// if (ip_s.equals("24.0.74.244")) {
		InetSocketAddress addr = new InetSocketAddress(ip_s, port_num);

		synchronized (ip_addrs) {
			if (!ip_addrs.contains(addr) && ip_addrs.size() < MAX_CONNECTIONS) {
				ip_addrs.add(addr.getAddress());
				Peer pen = new Peer(addr, state, this);
				Thread t = new Thread(pen);
				t.start();

				synchronized (peers) {
					peers.add(pen);
				}

			}
		}
		 }
	}

	private static String formatInterval(final long interval) {
		final long hr = TimeUnit.MILLISECONDS.toHours(interval);
		final long min = TimeUnit.MILLISECONDS.toMinutes(interval
				- TimeUnit.HOURS.toMillis(hr));
		final long sec = TimeUnit.MILLISECONDS.toSeconds(interval
				- TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
		final long ms = TimeUnit.MILLISECONDS.toMillis(interval
				- TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min)
				- TimeUnit.SECONDS.toMillis(sec));
		return String.format("%02d hours %02d minutes %02d.%03d seconds", hr,
				min, sec, ms);
	}

	/*
	 * Verify a piece that has been downloaded, then update the status of the
	 * piece. If all pieces have been successfully downloaded, write the
	 * finished file to disk.
	 * 
	 * @param payload Byte array of the piece downloaded
	 * @param piece_num the piece number (zero-indexed)
	 */
	public synchronized boolean setPiece(byte[] payload, int piece_num,
			int offet) {
		System.out.println("Finished " + piece_num);

		file[piece_num] = payload;
		pieces[piece_num] = FINISHED;

		if (!verify(payload, piece_num)) {
			System.out.println("Piece number " + piece_num + " failed verification. Will redownload.");
			pieces[piece_num] = FREE;
			file[piece_num] = null;
			return false;
		}

		if (piece_num % 10 == 0) {
			System.out.println("Finished piece " + piece_num);
		}
		state.updateDownloadStats(payload.length);

		// Check if the file is done loading and is ready to be stitched
		RUBTClient.dPRINT("Bytes left to download: " + state.getLeft());
		if (state.getLeft() == 0) {
			stitch();
			state.setEvent("completed");
			state.setFinishTime(System.nanoTime());
			System.out.println("Finished downloading. Total time elapsed: "
					+ formatInterval(state.getElapsedTime() / 1000000));
		}

		return true;
	}

	/**
	 * Determine whether or not the peer calling this method should send an
	 * interested message.
	 * 
	 * @param peerToGet an array representing the pieces that the remote
	 * peer already has
	 * @return true if remote peer has a piece that we do not, otherwise false
	 */
	public synchronized int interested(boolean[] peerToGet) {

		RUBTClient.dPRINT("Interested");
		int i = 0;

		for (i = 0; i < peerToGet.length; i++) {
			if (peerToGet[i] == true && pieces[i] == FREE) {
				RUBTClient.dPRINT("interest in " + i);
				return i;
			}
		}

		return -1;
	}

	/*
	 * Confirms if the client has downloaded the requested piece
	 */
	public synchronized boolean hasPiece(int index) {
		return pieces[index] == FINISHED;
	}

	/*
	 * Function used to verify the hashes of the downloaded piece with the piece
	 * on file
	 */
	private boolean verify(byte[] arry, int piece_num) {
		ByteBuffer[] hashes = state.info.piece_hashes;

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(arry);
			byte[] info_hash = digest.digest();

			if (MessageDigest.isEqual(hashes[piece_num].array(), info_hash)) {
				return true;
			}

		} catch (NoSuchAlgorithmException nsae) {
			RUBTClient.dPRINT("Failure to hash string in placement "
					+ piece_num);
			return false;
		}

		RUBTClient.dPRINT("Hashing Failed");
		return false;
	}

	/*
	 * Function used to assign a peer to download a certain piece of the file
	 */
	public synchronized int getPiece(boolean[] peerToGet) {

		int[] available = new int[pieces.length];
		for (Peer p: peers) {
			boolean[] remoteHas = p.getRemoteHas();
			if (remoteHas != null) {
				for(int i = 0; i < available.length; i++) {
					if (remoteHas[i]) {
						available[i]++;
					}
				}
			}
		}
		
		List<Piece> counts = new LinkedList<Piece>();
		for (int i = 0; i < available.length; i++) {
			counts.add(new Piece(i, available[i]));
		}
		Collections.sort(counts);
		

		System.out.println("Pieces ordered by availability.");
	/*	for (Piece current: counts) {
	 * 
		for (int i = 0; i < 5; i++) {
			Piece current = counts.get(i);
			System.out.println("piece: " + current.pieceNum + " available: " + current.available);
		}*/
		
		for (Piece current: counts) {
			int pieceNum = current.pieceNum;
			if (peerToGet[pieceNum] && pieces[pieceNum] == FREE) {
				pieces[pieceNum] = DOWNLOADING;
				return pieceNum;
			}
		}

		return -1;
	}

	/*
	 * Function used to determine pieces currently finished by the Torrent
	 */
	protected synchronized boolean[] getCompletedArray() {
		int i;
		boolean[] ret = new boolean[pieces.length];

		for (i = 0; i < pieces.length; i++) {
			if (pieces[i] == FINISHED) {
				ret[i] = true;
			} else {
				ret[i] = false;
			}
		}

		return ret;
	}

	/*
	 * Returns a requested piece that was previously downloaded.
	 * @param num index number of the piece requested
	 * @return a byte array containing the piece's data
	 */
	protected synchronized byte[] getPayload(int num) {
		if(num < 0 || num > pieces.length) {
			return null;
		}

		if(pieces[num] != FINISHED) {
			return null;
		}

		return file[num];
	}

	/*
	 * Function used to stitch the completed file together TODO: use filename
	 * set by user.
	 */
	private void stitch() {
		RUBTClient.dPRINT("Stitching ABOUT TO GO DOWN....");

		try {

			File f = state.getOutfile();
			FileOutputStream fos = new FileOutputStream(f);

			for (int i = 0; i < file.length; i++) {
				byte[] toAdd = file[i];
				fos.write(toAdd);
			}

			fos.close();
			RUBTClient.dPRINT("File Completed!");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	protected static byte[] generatePeerId() {
		ByteBuffer byt = ByteBuffer.wrap(new byte[] { '1', '2', '3', '4', '5',
				'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
				'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
				'u', 'v', 'w', 'x', 'y', 'z' });

		byte[] ret = new byte[20];
		byte[] pool = byt.array();
		for (int i = 0; i < 20; i++) {
			Random rand = new Random();
			int marker = rand.nextInt(20);
			ret[i] = pool[marker];
		}

		return ret;
	}

	private void listenForSeeders() {
		if (downloadFrom != null) {
			return;
		}
		ServerSocket listen = null;
		state.setListeningPort(TorrentState.DEFAULT_LISTENING_PORT);
		RUBTClient.dPRINT("Listening for seeders....");

		int port = state.getListeningPort() - 1;
		int maxPort = port + 10;
		while (listen == null && port < maxPort) {
			port++;
			try {
				listen = new ServerSocket(port);
			} catch (IOException e) {
				RUBTClient.dPRINT("Could not connect to port " + port);
			}
		}

		if (listen == null) {
			System.err.println("Unable to open listening socket!");
			return;
		} else {
			state.setListeningPort(port);
		}

		try {

			while (true) {
				RUBTClient.dPRINT("Peer attempting to connect....");

				Socket accept = listen.accept();

				if (!ip_addrs.contains(accept.getInetAddress())) {
					Peer seed = new Peer(accept, state, this);
					Thread t = new Thread(seed);
					t.run();

					synchronized (peers) {
						peers.add(seed);
					}

					ip_addrs.add(accept.getInetAddress());
				}
			}
		} catch (IOException e) {
			RUBTClient.dPRINT("Could not open socket!");
		}

	}

	private void chokeCheck() {
		Thread chokeCheck = new Thread(new Runnable() {
			public void run() {

				// Every 30 seconds....
				Timer timer = new Timer();
				timer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						RUBTClient.dPRINT("Iterating on timer @ 30sec");
						// If we are at the maximum number of peers unchocked
						if (available.availablePermits() == 0) {
							// Select the worst performing unchocked peer
							RUBTClient.dPRINT("Case 1: Maximum Choked! Num Peers " + peers.size() + " num avail " + available.availablePermits());
							if (peers.size() > 0) {
								Peer worstPerf = peers.get(0);
								for (Peer p : peers) {
									p.updateThroughPut();
									RUBTClient.dPRINT("Throughput for peer " + p.getThroughPut());
									if (p.getThroughPut() < worstPerf.getThroughPut()) {
										worstPerf = p;
									}
								}

								// Choke it
								worstPerf.sendMessage(Message.choke);
								available.release();
								
								RUBTClient.dPRINT("Peer Choked!");
								// Unchoke a random non-chocked peer
								boolean peerHit = false;			
								
								while(!peerHit && peers.size() > 0) {
									Random rand = new Random();
									Peer p = peers.get(rand.nextInt(peers.size()));
									
									if(!p.localChoked) {
										try {
											RUBTClient.dPRINT("Peer unchoked!");
											p.sendMessage(Message.unchoke);
											available.acquire();
											peerHit = true;
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
								}
							}

						} else {
							// Else we have slots for unchoked peers!
							RUBTClient.dPRINT("Case 2: Unmax Choked!");
							for (Peer p : peers) {
								
								p.updateThroughPut();
								
								RUBTClient.dPRINT("Downloaded " + p.getDownloaded());
								RUBTClient.dPRINT("TP " + p.getThroughPut());
								if (available.availablePermits() < MAX_UNCHOKED) {
									try {
										available.acquire();
										p.sendMessage(Message.unchoke);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}
						}
						//Reset the throughput timers for the peers
						for(Peer p : peers) {
							p.resetTPMeasures();
						}
					}		
				}, CHOKE_CHECK_INTERVAL, CHOKE_CHECK_INTERVAL);
			}
		});

		chokeCheck.run();
	}

	@Override
	public void run() {
		// Form the tracker object for this torrent

		Thread seed_lst = new Thread(new Runnable() {

			public void run() {
				listenForSeeders();
			}
		});
		seed_lst.start();

		tracker = new Tracker(state, this);
		Thread t = new Thread(tracker);
		t.run();
		chokeCheck();
	}

	public synchronized void sendMsgToPeers(Message msg) {
		synchronized (peers) {
			RUBTClient.dPRINT("Thread " + Thread.currentThread().getId()
					+ " Enetered Method");
			for (int i = 0; i < peers.size(); i++) {
				Peer p = peers.get(i);
				if (p.endThread) {
					peers.remove(p);
				} else {
					p.sendMessage(msg);
				}
			}
			RUBTClient.dPRINT("Thread " + Thread.currentThread().getId()
					+ " exited Method");
		}
	}

	public InetAddress getDownloadFrom() {
		return downloadFrom;
	}

	public void setDownloadFrom(InetAddress ip) {
		downloadFrom = ip;
	}
	
}
