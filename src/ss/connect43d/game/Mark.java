package ss.connect43d.game;

public class Mark {
	public static final Mark EMPTY = new Mark(null);
	
	private Player owner;
	
	public Mark(Player owner) {
		this.owner = owner;
	}
	
	public Player getOwner() {
		return this.owner;
	}
	
	public boolean isEmpty() {
		return this.owner == null;
	}
}
