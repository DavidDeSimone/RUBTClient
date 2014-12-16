package com.ddsc.networking;

class Piece implements Comparable<Piece>{
	int pieceNum;
	int available;
	
	Piece(int pieceNum, int available) {
		this.pieceNum = pieceNum;
		this.available = available;
	}
	
	@Override
	public int compareTo(Piece o) {
		return o.available - this.available;
	}
}
