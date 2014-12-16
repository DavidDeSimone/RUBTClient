package com.ddsc.utils;
import java.util.Arrays;
import java.util.BitSet;

public class BitConversion {
	
	public static boolean[] convert(byte[] bits, int sig_bits, boolean val) {
		boolean[] return_value = new boolean[sig_bits];
		int bool_index = 0;
		
		if(val) {
		for (int byte_index = 0; byte_index < bits.length; ++byte_index){
			for (int bit_index = 0; bit_index < 8 ; ++bit_index){
				if (bool_index >= sig_bits){
					return return_value;
				}
				
				return_value[bool_index++] = (bits[byte_index] >> bit_index & 0x01) == 1 ? true
				: false;
			
			}
			
			
		}
		return return_value;
		} else {
			for (int byte_index = 0; byte_index < bits.length; ++byte_index){
				for (int bit_index = 7; bit_index >= 0; --bit_index){
					if (bool_index >= sig_bits){
						return return_value;
					}
					
					return_value[bool_index++] = (bits[byte_index] >> bit_index & 0x01) == 1 ? true
					: false;
				
				}
				
				
			}
			return return_value;
		}
	}
	
	
	public static boolean[] convert(byte[] bits, boolean val) {
		return BitConversion.convert(bits, (bits.length * 8), val);
	}
	
	public static byte[] convert(boolean[] bools) {
		BitSet bs = new BitSet(bools.length);
		
		for(int i = 0; i < bools.length; i++) {
			if (bools[i]) {
				bs.set(i);
			}
		}
		byte[] bitfield = Arrays.copyOf(bs.toByteArray(), (int)Math.ceil((double)bools.length / 8));
		
		
		return bitfield;
	}
	
	/**
	public static byte[] convert(boolean[] bools) {
		int length = bools.length / 8;
		int mod = bools.length % 8;
		if(mod != 0){
			++length; }
		
		byte[] return_value = new byte[length];
		int boolIndex = 0;
		for (int byteIndex = 0; byteIndex < return_value.length; ++byteIndex) {
			for (int bitIndex = 7; bitIndex >= 0; --bitIndex) {
				// Another bad idea
				if (boolIndex >= bools.length) {
					return return_value;
				}
				if (bools[boolIndex++]) {
					return_value[byteIndex] |= (byte) (1 << bitIndex);
				}
			}
		}

		return return_value;
	}
	*/


}
