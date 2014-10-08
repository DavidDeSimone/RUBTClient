/*
 *  RUBTClient is a BitTorrent client written at Rutgers University for 
 *  instructional use.
 *  Copyright (C) 2008-2012  Robert Moore II
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ddsc.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.rutgers.cs.btclient.util.BitToBoolean;

/**
 * Represents a peer wire protocol message. Also generates Handshake messages.
 * 
 * @author Robert Moore II
 */
public class Message {
	/**
	 * Logging facility for the class.
	 */

	/**
	 * Keep-Alive message type value. Keep-Alive messages do not contain a byte
	 * for the message type.
	 */
	public static final byte TYPE_KEEPALIVE = -1;

	/**
	 * Message ID for Choke messages.
	 */
	public static final byte TYPE_CHOKE = 0;

	/**
	 * Message ID for Unchoke messages.
	 */
	public static final byte TYPE_UNCHOKE = 1;

	/**
	 * Message ID for Interested messages.
	 */
	public static final byte TYPE_INTERESTED = 2;

	/**
	 * Message ID for Not Interested messages.
	 */
	public static final byte TYPE_NOT_INTERESTED = 3;

	/**
	 * Message ID for Have messages.
	 */
	public static final byte TYPE_HAVE = 4;

	/**
	 * Message ID for Bitfield messages.
	 */
	public static final byte TYPE_BITFIELD = 5;

	/**
	 * Message ID for Request messages.
	 */
	public static final byte TYPE_REQUEST = 6;

	/**
	 * Message ID for Piece messages.
	 */
	public static final byte TYPE_PIECE = 7;

	/**
	 * Message ID for Cancel messages.
	 */
	public static final byte TYPE_CANCEL = 8;

	/**
	 * Human-readable strings for the different message types.&nbsp; Used for
	 * logging and the {@code toString()} method.
	 */
	public static final String[] MESSAGE_TYPE_NAMES = { "Choke", "Unchoke",
			"Interested", "Not Interested", "Have", "Bitfield", "Request",
			"Piece", "Cancel" };

	/**
	 * A Keep-Alive message.
	 */
	public static final Message MSG_KEEPALIVE = new Message(0,
			TYPE_KEEPALIVE);

	/**
	 * A Choke message.
	 */
	public static final Message MSG_CHOKE = new Message(1, TYPE_CHOKE);

	/**
	 * An Unchoke message.
	 */
	public static final Message MSG_UNCHOKE = new Message(1,
			TYPE_UNCHOKE);

	/**
	 * An Interested Message.
	 */
	public static final Message MSG_INTERESTED = new Message(1,
			TYPE_INTERESTED);

	/**
	 * A Not Interested message.
	 */
	public static final Message MSG_NOT_INTERESTED = new Message(1,
			TYPE_NOT_INTERESTED);

	/**
	 * Used as the initial array for handshake messages.
	 */
	private static final byte[] HANDSHAKE_SKELETON = new byte[]
	// Protocol string
	{ 19, 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o',
			't', 'o', 'c', 'o', 'l',
			// Reserved bytes
			0, 0, 0, 0, 0, 0, 0, 0,
			// Info hash placeholder
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			// Peer ID placeholder
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/**
	 * The length of this message.
	 */
	private final int length;

	/**
	 * The byte value of the the message ID.
	 */
	private final byte id;

	/**
	 * Creates a basic BitTorrent peer wire protocol message.
	 * 
	 * @param length
	 *            the length of the encoded message in bytes.
	 * @param type
	 *            the type of message (e.g., Interested, Choke, Bitfield).
	 */
	protected Message(final int length, final byte type) {
		this.id = type;
		this.length = length;
	}

	/**
	 * Generates a peer protocol handshake.
	 * 
	 * @param infoHash
	 *            the SHA-1 hash of the 'info' dictionary of the metainfo
	 *            (.torrent) file.
	 * @param peerId
	 *            the peer ID of the client.
	 * @return a peer protocol handshake message.&nbsp; The returned array may
	 *         be modified by the calling function.
	 */
	public static byte[] generateHandShake(byte[] infoHash, byte[] peerId) {
		if (infoHash == null || infoHash.length != 20)
			throw new IllegalArgumentException(
					"PeerMessage: Info hash must be 20 bytes.");
		if (peerId == null || peerId.length < 20)
			throw new IllegalArgumentException(
					"PeerMessage: Peer ID must be at least 20 bytes.");
		byte[] handshake = new byte[Message.HANDSHAKE_SKELETON.length];
		System.arraycopy(Message.HANDSHAKE_SKELETON, 0, handshake, 0,
				Message.HANDSHAKE_SKELETON.length);
		System.arraycopy(infoHash, 0, handshake, 28, 20);
		System.arraycopy(peerId, 0, handshake, 48, 20);
		return handshake;
	}

	/**
	 * Reads the next peer protocol message from the specified
	 * {@code InputStream}. Does not read handshake messages.
	 * 
	 * @param in
	 *            the {@code InputStream} containing the peer protocol message.
	 * @return the next peer protocol message on the stream, blocking until a
	 *         message arrives.
	 * @throws IOException
	 *             if reading from the {@code InputStream} generates an
	 *             {@code IOException}.
	 */
	public static Message read(InputStream in) throws IOException {
		// A nice wrapper
		DataInputStream din = new DataInputStream(in);

		// Read the message length
		int length = din.readInt();
		
		// Corrupt data (happens occasionally). Messages are max 128KiB + 9 B
		// overhead.
		if (length < 0 || length > 131081) {
			throw new IOException("Received invalid message length: " + length);
		}

		// Only Keep-Alive messages have a length of 0
		if (length == 0)
			return MSG_KEEPALIVE;

		// At least 1 byte, so read the message type
		byte type = din.readByte();

		// Return null if the next byte is invalid
		if (type >= 0 && type < Message.MESSAGE_TYPE_NAMES.length)

		else {
			throw new IOException("Received unknown message. ID: "
					+ Integer.toHexString(type & 0xFF) + " Length: " + length);
		}

		// Static messages
		if (length == 1) {
			switch (type) {
			case TYPE_CHOKE:
				return MSG_CHOKE;
			case TYPE_UNCHOKE:
				return MSG_UNCHOKE;
			case TYPE_INTERESTED:
				return MSG_INTERESTED;
			case TYPE_NOT_INTERESTED:
				return MSG_NOT_INTERESTED;
			default:
				throw new IOException(
						"Received unrecognized message with length 1. ID: "
								+ Integer.toHexString(type & 0xFF));
			}
		}
		// Have message
		else if (type == TYPE_HAVE && length == 5) {
			int piece = din.readInt();
			return new HaveMessage(piece);
		}
		// Request message
		else if (type == TYPE_REQUEST && length == 13) {
			int pieceIndex = din.readInt();
			int blockOffset = din.readInt();
			int blockLength = din.readInt();
			return new RequestMessage(pieceIndex, blockOffset, blockLength);
		}
		// Piece message
		else if (type == TYPE_PIECE && length >= 9) {
			int index = din.readInt();
			int begin = din.readInt();
			int p_length = length - 9;

			byte[] piece = new byte[p_length];
			din.readFully(piece);

			return new PieceMessage(index, begin, piece);
		}
		// Cancel message
		else if (type == TYPE_CANCEL && length == 13) {
			int index = din.readInt();
			int offset = din.readInt();
			int piece_length = din.readInt();

			return new CancelMessage(index, offset, piece_length);
		}
		// Bitfield message
		else if (type == TYPE_BITFIELD) {
			byte[] bitfield = new byte[length - 1];
			din.readFully(bitfield);
			boolean[] bools = BitToBoolean.convert(bitfield);
			return new BitfieldMessage(bools);
		}
		throw new IOException("Unrecognized message: L " + 
				Integer.valueOf(length) + ", T " +  Integer.toHexString(type & 0xFF));
	}

	/**
	 * Writes this peer protocol message to the {@code OutputStream} specified.
	 * 
	 * @param out
	 *            the {@code OutputStream} to write the message onto.
	 * @return the number of bytes written to the output stream.
	 * @throws IOException
	 */
	public static int write(final OutputStream out, final Message message)
			throws IOException {
	  
	  if(message == null){
	    throw new IOException("Cannot write a null message.");
	  }
	  
	  
	  try {
		DataOutputStream dout = new DataOutputStream(out);


		dout.writeInt(message.getLength());
		if (message.getLength() > 0) {
			dout.writeByte(message.getId());

			switch (message.getId()) {
			case Message.TYPE_BITFIELD:
				byte[] bytes = BitToBoolean.convert(((BitfieldMessage) message)
						.getBits());
				dout.write(bytes);
				break;
			case Message.TYPE_PIECE: {
				PieceMessage msg = (PieceMessage) message;
				dout.writeInt(msg.getPieceIndex());
				dout.writeInt(msg.getBlockOffset());
				dout.write(msg.getBlockData());
				break;
			}
			case Message.TYPE_REQUEST: {
				RequestMessage msg = (RequestMessage) message;
				dout.writeInt(msg.getPieceIndex());
				dout.writeInt(msg.getBlockOffset());
				dout.writeInt(msg.getBlockLength());

			}
				break;
			case Message.TYPE_HAVE: {
				HaveMessage msg = (HaveMessage) message;
				dout.writeInt(msg.getPieceIndex());
			}
				break;
			}
		}
		dout.flush();
	  }catch(NullPointerException npe){
	    throw new IOException("Cannot write to a null stream.");
	  }

		return message.getLength();
	}

	/**
	 * Returns a human-readable description of the message.
	 */
	@Override
	public String toString() {
		if (this.id == Message.TYPE_KEEPALIVE)
			return "Keep-alive";

		StringBuilder buff = new StringBuilder();

		buff.append(MESSAGE_TYPE_NAMES[this.id]);

		return buff.toString();
	}

	public int getLength() {
		return this.length;
	}

	public byte getId() {
		return this.id;
	}
}
