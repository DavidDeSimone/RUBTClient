package com.ddsc.utils;
public class RequestMessage extends Message {
	
	
	private int piece_index;
	private int block_offset;
	private int block_length;
	
	public RequestMessage(int piece_index, int block_offset, int block_length) {
		super(13, Message.t_request);
		this.piece_index = piece_index;
		this.block_offset = block_offset;
		this.block_length = block_length;
		
	}
	
	public int getPieceIndex() {
		return piece_index;
	}
	
	public int getBlockOffset() {
		return block_offset;
	}
	
	public int getBlockLength() {
		return block_length;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Request (").append(this.piece_index).append(", ").append(this.block_offset)
				.append(", ").append(this.block_length).append(')');
		return sb.toString();
	}
	
	
	
	
	
	
	

}
