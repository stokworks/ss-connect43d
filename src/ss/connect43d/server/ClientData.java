package ss.connect43d.server;

import java.nio.channels.SocketChannel;

import ss.connect43d.game.Player;

public class ClientData {
	private SocketChannel socket;
	private State state;
	private String name;
	private boolean hasChat = false;
	private boolean hasChallenge = false;
	private Player player;
	
	public enum State {
		WAIT_CONNECT,
		WAIT_FEATURED,
		IN_LOBBY,
		IN_GAME
	}
	
	public ClientData(SocketChannel socket) {
		this.socket = socket;
		this.state = State.WAIT_CONNECT;
	}

	public SocketChannel getSocket() {
		return this.socket;
	}
	
	public void setState(State state) {
		this.state = state;
	}
	
	public State getState() {
		return this.state;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setHasChat(boolean hasChat) {
		this.hasChat = hasChat;
	}
	
	public void setHasChallenge(boolean hasChallenge) {
		this.hasChallenge = hasChallenge;
	}
	
	public boolean hasChat() {
		return this.hasChat;
	}
	
	public boolean hasChallenge() {
		return this.hasChallenge;
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public Player getPlayer() {
		return this.player;
	}
}
