package com.ddsc.utils;
public class PieceMessage extends Message{
	
	private int piece_index;
	private int block_offset;
	private byte[] block_data;
	
	public PieceMessage(int piece_index, int block_offset, byte[] block_data){
		super(block_data.length + 9, Message.t_piece);
		this.piece_index = piece_index;
		this.block_offset = block_offset;
		this.block_data = block_data;
				
	}
	
	public int getPieceIndex() {
		return piece_index;
	}
	
	public int getBlockOffset() {
		return block_offset;
	}
	
	public byte[] getBlockData() {
		return block_data;
	}
	
	public void setBlockOffset(int offset) {
		block_offset = offset;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Piece (").append(this.piece_index).append(", ")
				.append(this.block_offset).append(", ")
				.append(this.block_data.length).append(')');
		
		return sb.toString();
	}
	
	
	
}
