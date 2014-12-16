package com.ddsc.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.ddsc.giventools.TorrentInfo;
import com.ddsc.main.RUBTClient;
import com.ddsc.utils.BitfieldMessage;
import com.ddsc.utils.HaveMessage;
import com.ddsc.utils.Message;
import com.ddsc.utils.PieceMessage;
import com.ddsc.utils.RequestMessage;

public class Peer implements Runnable {

	// time between sending keepalive messages in milliseconds
	private static final long KEEPALIVE_INTERVAL = 1000 * 120;
	// socket timeout
	private static final int DEFAULT_TIMEOUT = 150000;
	// download each piece in blocks of this size
	private static final int BLOCK_SIZE = 16384;
	// if no message received from remote host after this duration (in ms), disconnect
	private static final long MAX_KEEPALIVE = 1000 * 240L;

	protected final InetSocketAddress addr;
	protected TorrentState state;
	protected Torrent torrent;

	protected Socket socket;
	protected Timer keepAliveTimer;
	protected TimerTask timerTask;
	protected DataInputStream inputStream;
	protected DataOutputStream outputStream;

	protected boolean localChoked;
	protected boolean localInterested;
	protected boolean remoteChoked;
	protected boolean remoteInterested;
	protected boolean remoteHas[];
	protected boolean incomingConnection;

	public volatile boolean endThread;

	private int currentPiece;
	private int currentBlock;
	private int pieceLength;
	private int lastBlockLength;

	private byte piece[];
	private final TorrentInfo info;

	/* Fields for measuring throughput, */
	private double throughPut;
	private long startTime;
	private int localDownloaded; 
	private int localUploaded;

	public static InetAddress haveAll;
	static {
		try {
			haveAll = InetAddress.getByName("128.6.171.131");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * @param addr the address of the peer
	 * @param state the TorrentState for the torrent being worked on
	 * @param torrent the Torrent object calling this constructor
	 */
	public Peer(InetSocketAddress addr, TorrentState state, Torrent torrent) {
		if (addr == null || state == null || torrent == null) {
			throw new IllegalArgumentException("Arguments can't be null.");
		}

		this.addr = addr;
		this.state = state;
		this.info = state.getTorrentInfo();
		this.endThread = false;
		this.torrent = torrent;
		this.remoteChoked = false;
		pieceLength = info.piece_length;
		incomingConnection = false;
		
		throughPut = 0;
		localDownloaded = 0;
		localUploaded = 0;
		startTime = System.nanoTime();
	}

	/**
	 * Constructor used if remote host initiated connection.
	 * 
	 * @param sock an open socket that can be used to send / receive messages
	 * @param state the TorrentState object for the torrent being worked on
	 * @param torrent the Torrent object calling this constructor
	 */
	public Peer(Socket sock, TorrentState state, Torrent torrent) {
		if (sock == null || state == null || torrent == null) {
			throw new IllegalArgumentException("Arguments Invalid!");
		}

		this.socket = sock;
		this.state = state;
		this.info = state.getTorrentInfo();
		this.endThread = false;
		this.torrent = torrent;
		this.remoteChoked = false;
		this.addr = new InetSocketAddress(sock.getInetAddress(), sock.getLocalPort());
		pieceLength = info.piece_length;
		incomingConnection = true;
		
		throughPut = 0;
		localDownloaded = 0;
		localUploaded = 0;
		startTime = System.nanoTime();
	}

	protected boolean[] getRemoteHas() {
		return remoteHas;
	}
	
	/**
	 * Open a new socket, connect, and set up input / output streams.
	 * 
	 * @return true if no connection successful, otherwise false
	 */
	protected boolean connect() {
		try {
			if (socket != null) {
				socket.close();
			}
			socket = new Socket();
			socket.connect(addr, DEFAULT_TIMEOUT);
			socket.setSoTimeout(DEFAULT_TIMEOUT);
			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());
			RUBTClient.dPRINT("Connection Formed....");
			return true;
		} catch (IOException e) {
			// e.printStackTrace();
			return false;
		}
	}

	/**
	 * Disconnect from the remote peer, closing resources.
	 * 
	 * @return true if no exceptions occurred, otherwise false
	 */
	protected boolean disconnect() {
		try {
			endThread = true;
			inputStream.close();
			outputStream.close();
			socket.close();
			if (!remoteChoked) {
				remoteChoked = true;
				torrent.available.release();
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	protected boolean handshake() {
		byte[] infoHash = state.info.info_hash.array();
		byte[] peerId = state.getPeerId();
		byte[] handshake = Message.generateHandShake(infoHash, peerId);
		final byte[] response = new byte[68];
		try {
			RUBTClient.dPRINT("About to Read... " + handshake.length);
			outputStream.write(handshake);
			outputStream.flush();
			RUBTClient.dPRINT("Reading Responce..");
			inputStream.readFully(response);
			RUBTClient.dPRINT("Response Read.." + response.length);
			RUBTClient.dPRINT("Response" + new String(response));
		} catch (EOFException e) {
			RUBTClient.dPRINT("Error, Peer returned 0xFF");
		} catch (IOException e) {
			RUBTClient.dPRINT(e.getMessage() + " during handshake()");
			return false;
		}
		
		System.out.println("Not checking peer's response info hash.");
//		for (int i = 28; i < 48; i++) {
//			if (handshake[i] != response[i]) {
//				System.err.println("Peer's response info hash does not match: "
//						+ socket.getInetAddress());
//				return false;
//			}
//		}
		
		/*
		 * byte[] responsePeerId = Arrays.copyOfRange(response, 48, 68); for
		 * (int i = 0; i < 20; i++) { if (peerId[i] != responsePeerId[i]) {
		 * System.err.println("Peer's response ID does not match!"); return
		 * false; } }
		 */
		RUBTClient.dPRINT("Peer Success");
		return true;

	}

	private void setUpIncoming() {

		try {
			socket.setSoTimeout(DEFAULT_TIMEOUT);
			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());
			System.out.println("Connection Formed....");
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Accepting Peer Connection...");

		byte[] info_hash = info.info_hash.array();
		byte[] peer_id = state.getPeerId();
		byte[] response = Message.generateHandShake(info_hash, peer_id);
		byte[] handshake = new byte[68];

		try {
			RUBTClient.dPRINT("Reading Handshake...");
			inputStream.readFully(handshake);
			outputStream.write(response);
			outputStream.flush();
			for (int i = 28; i < 48; i++) {
				if (handshake[i] != response[i]) {
					RUBTClient
							.dPRINT("Peer's reponse info hash does not match!");
					disconnect();
					return;
				}
			}

		} catch (EOFException e) {
			RUBTClient.dPRINT("Peer returned 0xFF");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Run a peer thread. Connect, send bitfield message, set a task for sending
	 * keepalive messages, set a task for shutting down if keepalive interval
	 * exceeded.
	 * 
	 * Receive and send message to remote peer until endThread set to true or
	 * remote host disconnects.
	 */
	public void run() {
		// If we have an incoming peer connection
		if (incomingConnection) {
			setUpIncoming();
		} else {
			// We are being connected to
			if (!connect()) {
				System.err.println("Could not connect to remote peer " + addr.getHostString());
				return;
			}

			if (!handshake()) {
				System.err.println("Handshake failed: " + addr.getHostString());
				disconnect();
				return;
			}

		}

		// Send bitfield Message
		BitfieldMessage bit = new BitfieldMessage(torrent.getCompletedArray());
		sendMessage(bit);
		
		// Unchoke the peer as to seed
		// Improper: Only unchoke in response to an interested message
		// sendMessage(Message.unchoke);
		// Send keep-alive messages at regular intervals

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				sendMessage(Message.keep_alive);
			}
		}, KEEPALIVE_INTERVAL, KEEPALIVE_INTERVAL);

		// Disconnect from remote peer if we do not receive a message
		// in a reasonable amount of time
		keepAliveTimer = new Timer();
		timerTask = new TimerTask() {
			@Override
			public void run() {
				disconnect();
			};
		};
		keepAliveTimer.schedule(timerTask, MAX_KEEPALIVE);
		
		/*
		 * Hard code host ...131 to have all pieces.
		 */
		if (addr.getAddress().equals(haveAll)) {
			remoteHas = new boolean[state.num_pieces];
			for (int i = 0; i < remoteHas.length; i++) {
				remoteHas[i] = true;
			}
		}
		/*
		 * End hard code.
		 */
		
		Message message = null;
		while (!endThread) {
			try {
				message = Message.read(inputStream);
				RUBTClient.dPRINT("Message" + message.toString());
			} catch (IOException e) {
				System.err.println("IOException while reading from socket connected to "
						+ addr.getHostString());
				e.printStackTrace();
				break;
			}
			if (endThread) {
				break;
			}
			if (!handleMessage(message)) {
				System.err.println("Unable to process message: "
						+ message.toString() + " from " + socket.getInetAddress());
				break;
			}
		}
		disconnect();
		System.out.println("Connection closed: " + addr.toString());

	}

	/**
	 * Handle a message that we have received.
	 * 
	 * @param message
	 * @return
	 */
	private boolean handleMessage(Message message) {
		
		// Received a message, postpone exit.
		timerTask.cancel();
		timerTask = new TimerTask() {
			@Override
			public void run() {
				disconnect();
			};
		};
		keepAliveTimer.schedule(timerTask, MAX_KEEPALIVE);
		
		switch (message.getId()) {

		case Message.t_keep_alive:
			// do nothing
			break;
		case Message.t_bitfield:
			if (addr.equals(haveAll)) {
				break;
			}
			remoteHas = ((BitfieldMessage) message).getBools();
			if (remoteHas.length != state.num_pieces) {
				System.err.println("Received bitfield of wrong size: " + remoteHas.length);
			}
			if (torrent.interested(remoteHas) != -1) {
				localInterested = true;
				sendMessage(Message.interested);
			}
			break;
		case Message.t_choke:
			localChoked = true;
			break;
		case Message.t_unchoke:
			localChoked = false;
			if (localInterested) {
				int piece_i = torrent.getPiece(remoteHas);
				if (piece_i == -1) {
					sendMessage(Message.not_interested);
				} else {
					currentPiece = piece_i;
					currentBlock = 0;

					if (currentPiece == (info.piece_hashes.length - 1)) {
						pieceLength = info.file_length % info.piece_length;
					} else {
						pieceLength = info.piece_length;

					}

					piece = new byte[pieceLength];
					lastBlockLength = pieceLength % BLOCK_SIZE;

					RequestMessage requestMsg;
					if (lastBlockLength == pieceLength) {
						// Request the last piece
						requestMsg = new RequestMessage(currentPiece,
								currentBlock, lastBlockLength);
					} else {
						requestMsg = new RequestMessage(currentPiece,
								currentBlock, BLOCK_SIZE);
					}

					sendMessage(requestMsg);
				}
			}
			break;
		case Message.t_interested:
			remoteInterested = true;
			if (torrent.available.tryAcquire()) {
				sendMessage(Message.unchoke);
			}
			break;
		case Message.t_not_interested:
			remoteInterested = false;
			if (!remoteChoked) {
				remoteChoked = true;
				torrent.available.release();
			}
			break;
		case Message.t_have:
			if (addr.equals(haveAll)) {
				break;
			}
			if (remoteHas == null) {
				System.err.println(addr.getHostName() + " sent a Have message without ever sending a Bitfield message.");
				// initializing remoteHas to empty
				remoteHas = new boolean[state.num_pieces];
			}
			remoteHas[((HaveMessage) message).getPieceIndex()] = true;
			if (!localInterested && torrent.interested(remoteHas) != -1) {
				localInterested = true;
				sendMessage(Message.interested);
			}
			break;

		case Message.t_request:

			if (!remoteChoked) {
				RequestMessage request = (RequestMessage) message;

				int index = request.getPieceIndex();
				int offset = request.getBlockOffset();
				int len = request.getBlockLength();

				if (len < 0) {
					System.err.println("Requested Length Negative");
					return false;
				}

				byte[] fullPayload = torrent.getPayload(index);

				byte[] payload = new byte[len];
				System.arraycopy(fullPayload, offset, payload, 0, len);

				// Form Piece Message
				PieceMessage to_send = new PieceMessage(index, offset, payload);
				sendMessage(to_send);

				updateUploaded(len);
				state.updateUploadStats(len);

				RUBTClient.dPRINT("Seed Data sent correctly");
			}
			break;

		case Message.t_piece:

			/*try {
			if(torrent.downloadFrom != null) {
				Thread.sleep(500);
			}
			} catch(Exception e) { }*/
			
			PieceMessage pieceMsg = (PieceMessage) message;

			// Make sure that we are downloading the correct piece,
			// and that we were sent the correct block
			if (pieceMsg.getPieceIndex() != currentPiece) {
				RUBTClient.dPRINT("Inproper Piece sent, PID: " + pieceMsg);
			} else if (pieceMsg.getBlockOffset() != currentBlock) {
				RUBTClient.dPRINT("Inproper Block sent, BID: " + pieceMsg);
			} else {
				// We are getting the correct piece
				if (pieceMsg.getBlockData().length == lastBlockLength) {
					// Write the block
					System.arraycopy(pieceMsg.getBlockData(), 0, piece,
							currentBlock, this.lastBlockLength);

					// Set the piece
					torrent.setPiece(piece, currentPiece, currentBlock);
					updateDownloaded(piece.length);
					
					Message msgRet = new HaveMessage(currentPiece);
					torrent.sendMsgToPeers(msgRet);

					if (localInterested) {
						int piece_i = torrent.getPiece(remoteHas);
						if (piece_i == -1) {
							// there are no FREE pieces; all are downloaded or
							// in progress
							sendMessage(Message.not_interested);
						} else {
							currentPiece = piece_i;
							currentBlock = 0;

							if (currentPiece == (info.piece_hashes.length - 1)) {
								// this is the last piece in the torrent
								pieceLength = info.file_length
										% info.piece_length;
							} else {
								pieceLength = info.piece_length;

							}

							piece = new byte[pieceLength];
							lastBlockLength = pieceLength % BLOCK_SIZE;
							RequestMessage requestMsg;
							if (lastBlockLength == pieceLength) {
								// Request the last piece
								requestMsg = new RequestMessage(currentPiece,
										currentBlock, lastBlockLength);
							} else {
								requestMsg = new RequestMessage(currentPiece,
										currentBlock, BLOCK_SIZE);
							}

							sendMessage(requestMsg);
						}
					}

					break;

				} else if (((pieceMsg.getBlockOffset() + BLOCK_SIZE + lastBlockLength) == pieceLength)
						&& this.lastBlockLength == 0) {
					// Write the block
					System.arraycopy(pieceMsg.getBlockData(), 0, piece,
							currentBlock, BLOCK_SIZE);

					// Set the piece
					torrent.setPiece(piece, currentPiece, currentBlock);
					updateDownloaded(piece.length);
					
					Message msgRet = new HaveMessage(currentPiece);
					torrent.sendMsgToPeers(msgRet);

					if (localInterested) {
						int piece_i = torrent.getPiece(remoteHas);
						if (piece_i == -1) {
							sendMessage(Message.not_interested);
						} else {
							currentPiece = piece_i;
							currentBlock = 0;

							if (currentPiece == (info.piece_hashes.length - 1)) {
								pieceLength = info.file_length
										% info.piece_length;
							} else {
								pieceLength = info.piece_length;

							}

							piece = new byte[pieceLength];
							lastBlockLength = pieceLength % BLOCK_SIZE;
							RequestMessage requestMsg;
							if (lastBlockLength == pieceLength) {
								// Request the last piece
								requestMsg = new RequestMessage(currentPiece,
										currentBlock, lastBlockLength);
							} else {
								requestMsg = new RequestMessage(currentPiece,
										currentBlock, BLOCK_SIZE);
							}

							sendMessage(requestMsg);
						}
					}

					break;
				} else if ((pieceMsg.getBlockOffset() + BLOCK_SIZE + lastBlockLength) == pieceLength) {
					System.arraycopy(pieceMsg.getBlockData(), 0, piece,
							currentBlock, BLOCK_SIZE);
					RequestMessage requestMsg;

					// Request another piece
					currentBlock += Peer.BLOCK_SIZE;
					requestMsg = new RequestMessage(currentPiece, currentBlock,
							lastBlockLength);
					sendMessage(requestMsg);
				} else {
					// Add block data to the piece
					System.arraycopy(pieceMsg.getBlockData(), 0, piece,
							currentBlock, BLOCK_SIZE);
					RequestMessage requestMsg;
					
					// Request another piece
					currentBlock += BLOCK_SIZE;
					requestMsg = new RequestMessage(currentPiece, currentBlock,
							BLOCK_SIZE);
					sendMessage(requestMsg);

				}
			}
			break;
		default:
			return false;
		}
		return true;

	}

	public synchronized void sendMessage(Message msg) {
		if (endThread) {
			return;
		}

		try {
			if (msg.getId() == Message.t_interested)
				localInterested = true;
			if (msg.getId() == Message.t_not_interested)
				localInterested = false;
			if (msg.getId() == Message.t_choke)
				remoteChoked = true;
			if (msg.getId() == Message.t_unchoke) {
				remoteChoked = false;
			}
			Message.write(outputStream, msg);

		} catch (IOException e) {
			endThread = true;
		}
	}

	public double getThroughPut() {
		return throughPut;
	}
	
	public void resetTPMeasures() {
		resetThroughPut();
		resetDownloaded();
		resetUploaded();
		resetTime();
	}
	
	protected void resetTime() {
		startTime = System.nanoTime();
	}
	
	public int getDownloaded() {
		return localDownloaded;
	}
	
	protected void updateDownloaded(int downloaded) {
		this.localDownloaded += downloaded;
	}
	
	protected void resetDownloaded() {
		this.localDownloaded = 0;
	}
	
	protected void updateUploaded(int uploaded) {
		this.localUploaded += uploaded;
	}
	
	protected void resetUploaded() {
		this.localUploaded = 0;
	}
	
	protected void resetThroughPut() {
		throughPut = 0;
	}
	protected void updateThroughPut() {
		long local = 0;
		if(state.getLeft() == 0) {
			local = localUploaded;
		} else {
			local = localDownloaded;
		}
		
		long currentTime = System.nanoTime();
		double diff = (currentTime - startTime) / Math.pow(10, 9);
		throughPut = (local / diff);
		RUBTClient.dPRINT("TP is " + throughPut + " Stuffs " + diff + " Downloaded " + local);
	}
	
}
