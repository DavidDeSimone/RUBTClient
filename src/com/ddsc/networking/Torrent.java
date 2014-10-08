package com.ddsc.networking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Torrent implements Runnable{

	

	public final static int FINISHED = 2;
	public final static int DOWNLOADING = 1;
	public final static int FREE = 0;
	
	
	private TorrentState state;
	
	//The array of file data that will be turned into the file
	// on the system
	Object[] file;
	
	//Integer array used to determine the status of a piece of the file
	int[] pieces;
	
	
	public Torrent(TorrentState state) {
		this.state = state;
		
		//Create file Object array
		//used to re-create file
		file = new Object[state.num_pieces];
		pieces = new int[state.num_pieces];
	}
	
	/*
     * Function used to mark a piece of file as downloaded
	 * @param payload
	 * Byte array of the piece downloaded
	 * @piece_num
	 * The piece number
	 */
	public synchronized boolean setPiece(byte[] payload, int piece_num) {
		file[piece_num] = payload;
		pieces[piece_num] = FINISHED;
		
		//Check if the file is done loading and is ready to be stiched
		checkFileStatus();
		
		
		return true;
	}
	
	
	/*
     * Function used to assign a peer to download a certain piece of the
	 * file
	 * 
	 * 
	 * 
	 */
	public synchronized int getPiece(boolean[] peerToGet) {
		int i = 0;
		
		for(i = 0; i < peerToGet.length; i++) {
			if(peerToGet[i] == true && pieces[i] == FREE) {
				return i;
			}
		}
	
		
		return -1;
	}
	
	/*
	 * Function used to check if the current file is done downloading
	 * 
	 */
	private void checkFileStatus() {
		int i;
		boolean isDone = true;
		
		for(i = 0; i < pieces.length; i++ ) {
			if(pieces[i] == 0) {
				isDone = false;
			}
		}
		
		if(isDone) {
			stich();
		}
		
	}
	
	/*
	 * Function used to stich the completed file together
	 * 
	 */
	private void stich() {
		try {
		File f = new File(state.info.file_name);
		FileOutputStream fos = new FileOutputStream(f);
		
		for(int i = 0; i < file.length; i++) {
			byte[] toAdd = (byte[])file[i];
			fos.write(toAdd);
			
		}
		
		System.out.println("File Completed!");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static String generatePeerId() {
		ByteBuffer byt = ByteBuffer.wrap(new byte[] {
		'1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
		'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
		'k'
		});
		String ret = new String(byt.array());
		return ret;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//Form the tracker object for this torrent
				Tracker tracker = new Tracker(state);
				Thread t = new Thread(tracker);
				t.run();
	}
	
	
}
