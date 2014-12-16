package com.ddsc.utils;

public class BitfieldMessage extends Message {

	private boolean[] bools;
	
	public BitfieldMessage(boolean[] bools){
		super( (int)(Math.ceil((double)bools.length / 8)) + 1 , Message.t_bitfield);
		this.bools = bools;
	}
	
	public boolean[] getBools() {
		return bools;
	}
	
}
