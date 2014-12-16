package com.ddsc.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import com.ddsc.main.RUBTClient;


public class Message {

	// Message characteristics
	private int length;
	private byte id;
	
	public static final byte t_keep_alive = -1;
	public static final byte t_choke = 0;
	public static final byte t_unchoke = 1;
	public static final byte t_interested = 2;
	public static final byte t_not_interested = 3;
	public static final byte t_have = 4;
	public static final byte t_bitfield = 5;
	public static final byte t_request = 6;
	public static final byte t_piece = 7;
	public static final byte t_cancel = 8;
	
	public static String[] Message_t_names = { "Choke", "Unchoke",
		"Interested", "Not Interested", "Have", "Bitfield", "Request",
		"Piece", "Cancel" };
	
	public static Message keep_alive = new Message(0, t_keep_alive);
	public static Message choke = new Message(1, t_choke);
	public static Message unchoke = new Message(1, t_unchoke);
	public static Message interested = new Message(1, t_interested);
	public static Message not_interested = new Message(1, t_not_interested);
	
	public int getLength(){
		return length;
	}
	
	public int getMessageId(){
		return id;
	}

	// Message template
	private static byte[] message_array = new byte[] 
			{ 19, 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o',
				't', 'o', 'c', 'o', 'l',
				// Reserved bytes
			    0, 0, 0, 0, 0, 0, 0, 0, 
				// info_hash
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				// peer_id
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	
	
	public static byte[] generateHandShake( byte[] info_hash, byte[] peer_id){
		if (info_hash == null || info_hash.length != 20)
			throw new IllegalArgumentException(
					"Error: info_hash size error");
		if (peer_id == null || peer_id.length != 20)    
			throw new IllegalArgumentException(
		    		"Error: peer_id size error");
		
		//byte[] message = new byte[Message.message_array.length];
		byte[] message = Arrays.copyOf(message_array, message_array.length);
		
		for (int i=28; i<48; i++){
			message[i] = info_hash[i-28];
		}
		for (int i=48; i<68; i++){
			message[i] = peer_id[i-48];
		}

		RUBTClient.dPRINT("HandShake Generated..");
		
		//RUBTClient.dPRINT(new String(message));
		
		return message;
			
	}
	
	// Constructor
	protected Message(final int length, final byte type){
		this.id = type;
		this.length = length;
	}
	
	public static Message read( InputStream in) throws IOException {
		DataInputStream data_in = new DataInputStream(in);
		
		int data_length = data_in.readInt();
		
		if (data_length == 0) {
			 return keep_alive;
		}
		else if (data_length < 0 || data_length > 131081) {
			// this check prevents the client from working!!!
			throw new IOException("Invalid Message Size, Max 128Kb");
		}
		
		byte msg_type = data_in.readByte();
		
		//RUBTClient.dPRINT("Reading Message byte pattern " + msg_type);
		// Return Null if next byte is invalid
		
		if (data_length == 1){
			switch (msg_type){
			case t_choke:
				return choke;
			case t_unchoke:
				return unchoke;
			case t_interested:
				return interested;
			case t_not_interested:
				return not_interested;
		    default:
		    	throw new IOException(
		    			"Unexpected Message of length 1");
			}
		}
		else if (msg_type == t_have && data_length == 5) {
				int piece = data_in.readInt();
				return new HaveMessage(piece);
			}
		else if (msg_type == t_request && data_length == 13) {
				int piece_index = data_in.readInt();
				int block_offset = data_in.readInt();
				int block_length = data_in.readInt();
				
				return new RequestMessage(piece_index, block_offset, block_length);
			}
		else if (msg_type == t_piece && data_length >= 9){
				int index = data_in.readInt();
				int offset = data_in.readInt();
				int piece_length = data_length - 9;
				byte [] piece = new byte[piece_length];
				data_in.readFully(piece);
				
			//	RUBTClient.dPRINT("Getting " + index);
				
				return new PieceMessage(index, offset, piece);
			}
		else if (msg_type == t_cancel && data_length == 13){
				int index = data_in.readInt();
				int offset = data_in.readInt();
				int piece_length = data_in.readInt();
				
				return new CancelMessage(index, offset, piece_length);
			}
		else if (msg_type == t_bitfield) {
			    byte[] bitfield = new byte[data_length - 1];
			    data_in.readFully(bitfield);
			    boolean[] bools = BitConversion.convert(bitfield, false);
			    return new BitfieldMessage(bools);
			}
		
		throw new IOException("Unrecognized message");
				
		}
	
	public static int write(final OutputStream out, final Message message) throws IOException {
		if(message == null){
			throw new IOException("Error: Message cannot be null");
		}
		
		try{
			DataOutputStream data_out = new DataOutputStream(out);
		//	RUBTClient.dPRINT("Writing " + message.getId() + " : Length " + message.getLength());
			
			data_out.writeInt(message.getLength());
			
			if (message.getLength() > 0) {
				data_out.writeByte(message.getMessageId());
				switch (message.getMessageId()){
				case Message.t_have: {
					HaveMessage msg = (HaveMessage) message;
					data_out.writeInt(msg.getPieceIndex());
				    break;}
				case Message.t_request: {
					RequestMessage msg = (RequestMessage) message;
					data_out.writeInt(msg.getPieceIndex());
					data_out.writeInt(msg.getBlockOffset());
					data_out.writeInt(msg.getBlockLength());
					break;}
				case Message.t_piece: {
					PieceMessage msg = (PieceMessage) message;
					data_out.writeInt(msg.getPieceIndex());
					data_out.writeInt(msg.getBlockOffset());
					data_out.write(msg.getBlockData());
					break;			}
				case Message.t_bitfield: {
					byte[] bytes = BitConversion.convert(((BitfieldMessage) message).getBools());
					data_out.write(bytes);
					break;
					}
				
				}
				
			}
			data_out.flush();
		}
		catch (NullPointerException npe) {
			throw new IOException("Cannot write to a null stream.");
		}
		return message.getLength();
	}
	
	public String toString() {
		if (this.id == Message.t_keep_alive)
			return "Keep Alive";
		
		StringBuilder buff = new StringBuilder();
		buff.append(Message_t_names[this.id]);
		return buff.toString();
	}
	
	public byte getId() {
		return id;
	}
}

	

