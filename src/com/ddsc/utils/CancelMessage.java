
public class CancelMessage extends Message {

	private int piece_index;
	private int offset;
	private int piece_length;
	
	
	public CancelMessage(int piece_index, int offset, int piece_length){
		super(13, Message.t_cancel);
		this.piece_index = piece_index;
		this.offset = offset;
		this.piece_length = piece_length;
		
	}
	
	public int getPieceIndex() {
		return piece_index;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int getPieceLength() {
		return piece_length;
	}
	
}
