package com.ddsc.utils;
public class HaveMessage extends Message{

	private int piece_index;
	
	public HaveMessage(int piece_index){
		super(5, Message.t_have);
		this.piece_index = piece_index;
	}
	
	public int getPieceIndex(){
		return piece_index;
	}
		
}
