package ss.connect43d.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
	public static final int BOARD_SIZE_X = 4;
	public static final int BOARD_SIZE_Y = 4;
	public static final int BOARD_SIZE_Z = 4;
	public static final int WIN_LENGTH   = 4;
	
	private List<Player> players = new ArrayList<Player>();
	private int playerTurnIndex = 0;
	private Board board;
	private Map<Player, Mark> playerMarks = new HashMap<Player, Mark>();
	
	public Game(List<Player> players) {
		this.players = players;
		this.board = new Board(BOARD_SIZE_X, BOARD_SIZE_Y, BOARD_SIZE_Z, WIN_LENGTH);
		
		for (Player player : players) {
			Mark playerMark = new Mark(player);
			playerMarks.put(player, playerMark);
		}
	}
	
	public Player getCurrentPlayer() {
		if (this.isEnded()) {
			return null;
		} else {
			return this.players.get(this.playerTurnIndex);
		}
	}
	
	public void nextPlayer() {
		this.playerTurnIndex = (this.playerTurnIndex + 1) % this.players.size();
	}
	
	public Player getWinner() {
		Mark winningMark = this.board.getWinningMark();
		
		if (winningMark == null) {
			return null;
		} else {
			return winningMark.getOwner();
		}
	}
	
	public boolean isEnded() {
		return this.getWinner() != null;
	}
	
	public boolean isValidMove(int x, int y) {
		return this.board.isValidMove(x, y);
	}
	
	public void doMove(Player player, int x, int y) {
		if (this.isValidMove(x, y)) {
			this.board.doMove(this.playerMarks.get(player), x ,y);
			this.nextPlayer();
		}
	}
}
