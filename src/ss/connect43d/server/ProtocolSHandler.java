package ss.connect43d.server;

import java.io.UnsupportedEncodingException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ss.connect43d.game.Game;
import ss.connect43d.game.Player;

public class ProtocolSHandler extends ProtocolHandler {

	public static final int PLAYERS_PER_GAME = 2;

	private Map<SocketChannel, StringBuilder> dataQueues = new HashMap<SocketChannel, StringBuilder>();
	private Map<SocketChannel, ClientData> clients = new HashMap<SocketChannel, ClientData>();
	private Map<ClientData, Game> games = new HashMap<ClientData, Game>();
	private List<ClientData> matchmaking = new ArrayList<ClientData>();

	public ProtocolSHandler() {
		super();
		this.setSendEncoding(ProtocolS.ENCODING);
	}

	@Override
	public void onClientConnected(SocketChannel socket) {
		this.clients.put(socket, new ClientData(socket));
		this.dataQueues.put(socket, new StringBuilder());
	}

	@Override
	public void onClientDisconnected(SocketChannel socket) {
		ClientData client = this.clients.get(socket);

		if (client.getState() == ClientData.State.IN_LOBBY && (client.hasChat() || client.hasChallenge())) {
			for (ClientData recipient : this.clients.values()) {
				if (recipient.getState() == ClientData.State.IN_LOBBY
						&& (recipient.hasChat() || recipient.hasChallenge())) {
					this.sendDisconnected(recipient, client.getName());
				}
			}
		} else if (client.getState() == ClientData.State.IN_GAME) {
			Game game = this.games.get(client);
			for (ClientData recipient : this.getClientsInGame(game)) {
				this.sendDisconnected(recipient, client.getName());
			}
			
			this.broadcastGameEnd(game);
		}

		this.matchmaking.remove(client);
		this.clients.remove(socket);
		this.dataQueues.remove(socket);
	}

	private void startGame(List<ClientData> clients) {
		List<Player> players = new ArrayList<Player>();
		for (ClientData client : clients) {
			Player player = new Player(client.getName());
			players.add(player);
			client.setPlayer(player);
			client.setState(ClientData.State.IN_GAME);

			// Remove client from matchmaking
			if (this.matchmaking.contains(client)) {
				this.matchmaking.remove(client);
			}

			// TODO: CHALLENGE Cancel challenges for client
		}

		Game game = new Game(players);

		for (ClientData client : clients) {
			this.games.put(client, game);
			this.sendStart(client, players);
		}

		this.broadcastGameTurn(game);
	}

	private void broadcastGameTurn(Game game) {
		Player currentPlayer = game.getCurrentPlayer();
		List<ClientData> recipients = this.getClientsInGame(game);

		for (ClientData recipient : recipients) {
			this.sendTurn(recipient, currentPlayer);
		}
	}
	
	private void broadcastGameEnd(Game game) {
		List<ClientData> recipients = this.getClientsInGame(game);

		for (ClientData recipient : recipients) {
			this.sendEnd(recipient);
			this.games.remove(recipient);
			recipient.setState(ClientData.State.IN_LOBBY);
		}
	}

	private List<ClientData> getClientsInGame(Game game) {
		List<ClientData> clients = new ArrayList<ClientData>();

		for (Map.Entry<ClientData, Game> clientGame : this.games.entrySet()) {
			if (clientGame.getValue() == game) {
				clients.add(clientGame.getKey());
			}
		}

		return clients;
	}

	@Override
	public void onData(SocketChannel socket, byte[] data) {
		String stringData = null;
		try {
			stringData = new String(data, ProtocolS.ENCODING);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		dataQueues.get(socket).append(stringData);

		this.readCommandsFromData(socket);
	}

	private void readCommandsFromData(SocketChannel socket) {
		StringBuilder data = this.dataQueues.get(socket);
		int delimiterIndex = data.indexOf(ProtocolS.DELIM_CMD);

		if (delimiterIndex > -1) {
			String command = data.substring(0, delimiterIndex);
			this.parseCommand(this.clients.get(socket), command);

			// Remove command from data queue
			data.delete(0, delimiterIndex + 1);

			// Recursively read next command
			this.readCommandsFromData(socket);
		}
	}

	private void parseCommand(ClientData client, String command) {
		command = command.trim();
		String[] split = command.split(ProtocolS.DELIM_ARG);
		String argsRaw = "";

		if (split.length > 1) {
			argsRaw = command.split(ProtocolS.DELIM_ARG, 2)[1];
		}

		switch (split[0]) {
		case ProtocolS.CMD_CONNECT:
			this.onConnectCommand(client, split);
			break;
		case ProtocolS.CMD_FEATURED:
			this.onFeaturedCommand(client, split);
			break;
		case ProtocolS.CMD_JOIN:
			this.onJoinCommand(client, split);
			break;
		case ProtocolS.CMD_MOVE:
			this.onMoveCommand(client, split);
			break;
		case ProtocolS.CMD_ERROR:
			this.onErrorCommand(client, split, argsRaw);
			break;
		case ProtocolS.CMD_SAY:
			this.onSayCommand(client, split, argsRaw);
			break;
		case ProtocolS.CMD_LIST:
			this.onListCommand(client, split);
			break;
		case ProtocolS.CMD_CHALLENGE:
			this.onChallengeCommand(client, split);
			break;
		case ProtocolS.CMD_ACCEPT:
			this.onAcceptCommand(client, split);
			break;
		case ProtocolS.CMD_DECLINE:
			this.onDeclineCommand(client, split);
			break;
		default:
			this.onUnrecognizedCommand(client, split);
			break;
		}
	}

	private void onConnectCommand(ClientData client, String[] args) {
		if (args.length != 2) {
			this.sendError(client, ProtocolS.ERR_INVALID_COMMAND);
		} else if (client.getState() != ClientData.State.WAIT_CONNECT) {
			this.sendError(client, ProtocolS.ERR_COMMAND_UNEXPECTED);
		} else {
			String wantedName = args[1];
			boolean allowed = true;

			// Check if there is already a player with that name
			for (ClientData otherClient : this.clients.values()) {
				if (wantedName.equals(otherClient.getName())) {
					allowed = false;
					break;
				}
			}

			if (allowed) {
				client.setName(wantedName);
				client.setState(ClientData.State.WAIT_FEATURED);
				this.sendConnected(client);
			} else {
				this.sendError(client, ProtocolS.ERR_NAME_IN_USE);
			}
		}
	}

	private void onFeaturedCommand(ClientData client, String[] args) {
		if (client.getState() != ClientData.State.WAIT_FEATURED) {
			this.sendError(client, ProtocolS.ERR_COMMAND_UNEXPECTED);
		} else {
			client.setHasChat(false);
			client.setHasChallenge(false);

			// Read out all supported features
			for (int i = 1; i < args.length; i++) {
				if (args[i].equals(ProtocolS.FEAT_CHAT)) {
					client.setHasChat(true);
				} else if (args[i].equals(ProtocolS.FEAT_CHALLENGE)) {
					client.setHasChallenge(true);
				}
			}

			client.setState(ClientData.State.IN_LOBBY);
			this.sendFeatures(client);

			// TODO: CHAT/CHALLENGE Notify clients about joined player
		}
	}

	private void onJoinCommand(ClientData client, String[] args) {
		if (client.getState() != ClientData.State.IN_LOBBY) {
			this.sendError(client, ProtocolS.ERR_COMMAND_UNEXPECTED);
		} else {
			this.matchmaking.add(client);
			if (this.matchmaking.size() == PLAYERS_PER_GAME) {
				// Copy matchmaking list
				this.startGame(new ArrayList<ClientData>(this.matchmaking));
			}
		}
	}

	private void onMoveCommand(ClientData client, String[] args) {
		Game game = this.games.get(client);

		if (client.getState() != ClientData.State.IN_GAME) {
			this.sendError(client, ProtocolS.ERR_COMMAND_UNEXPECTED);
		} else if (game.getCurrentPlayer() != client.getPlayer()) {
			this.sendError(client, ProtocolS.ERR_COMMAND_UNEXPECTED);
		} else if (args.length != 3) {
			this.sendError(client, ProtocolS.ERR_INVALID_COMMAND);
		} else {
			int moveX = 0;
			int moveY = 0;

			try {
				moveX = Integer.parseInt(args[1]);
				moveY = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				this.sendError(client, ProtocolS.ERR_INVALID_COMMAND);
				return;
			}

			if (game.isValidMove(moveX, moveY)) {
				game.doMove(client.getPlayer(), moveX, moveY);

				for (ClientData recipient : this.getClientsInGame(game)) {
					this.sendMoved(recipient, moveX, moveY);
				}
				
				if (game.isEnded()) {
					this.broadcastGameEnd(game);
				} else {
					this.broadcastGameTurn(game);
				}
			} else {
				this.sendError(client, ProtocolS.ERR_INVALID_MOVE);
			}
		}
	}

	private void onErrorCommand(ClientData client, String[] args, String argsRaw) {
		System.out.println("Received error: " + argsRaw);
	}

	private void onSayCommand(ClientData client, String[] args, String argsRaw) {
		if (args.length == 1) {
			this.sendError(client, ProtocolS.ERR_INVALID_COMMAND);
		} else if (client.getState() != ClientData.State.IN_LOBBY && client.getState() != ClientData.State.IN_GAME) {
			this.sendError(client, ProtocolS.ERR_COMMAND_UNEXPECTED);
		} else if (client.hasChat() == false) {
			this.sendError(client, ProtocolS.ERR_COMMAND_NOT_FOUND);
		} else {
			if (client.getState() == ClientData.State.IN_GAME) {
				// Broadcast chat message to all players in game
				for (ClientData recipient : this.getClientsInGame(this.games.get(client))) {
					if (recipient.hasChat()) {
						this.sendSaid(recipient, client.getName(), argsRaw);
					}
				}
			} else {
				// Broadcast chat message to all players in lobby
				for (ClientData recipient : this.clients.values()) {
					if (recipient.hasChat() && recipient.getState() == ClientData.State.IN_LOBBY) {
						this.sendSaid(recipient, client.getName(), argsRaw);
					}
				}
			}
		}
	}

	private void onListCommand(ClientData client, String[] args) {
		throw new UnsupportedOperationException();
	}

	private void onChallengeCommand(ClientData client, String[] args) {
		throw new UnsupportedOperationException();
	}

	private void onAcceptCommand(ClientData client, String[] args) {
		throw new UnsupportedOperationException();
	}

	private void onDeclineCommand(ClientData client, String[] args) {
		throw new UnsupportedOperationException();
	}

	private void onUnrecognizedCommand(ClientData client, String[] args) {
		this.sendError(client, ProtocolS.ERR_COMMAND_NOT_FOUND);
	}

	private void sendCommand(ClientData client, String command) {
		this.send(client.getSocket(), command + ProtocolS.DELIM_CMD);
	}

	private void sendConnected(ClientData client) {
		this.sendCommand(client, ProtocolS.CMD_CONNECTED);
	}

	private void sendFeatures(ClientData client) {
		this.sendCommand(client, ProtocolS.CMD_FEATURES + ProtocolS.DELIM_ARG + ProtocolS.FEAT_CHAT
				+ ProtocolS.DELIM_ARG + ProtocolS.FEAT_CHALLENGE);
	}

	private void sendStart(ClientData client, List<Player> players) {
		StringBuilder command = new StringBuilder(ProtocolS.CMD_START);

		for (Player player : players) {
			command.append(ProtocolS.DELIM_ARG);
			command.append(player.getName());
		}

		this.sendCommand(client, command.toString());
	}

	private void sendTurn(ClientData client, Player player) {
		this.sendCommand(client, ProtocolS.CMD_TURN + ProtocolS.DELIM_ARG + player.getName());
	}

	private void sendMoved(ClientData client, int moveX, int moveY) {
		this.sendCommand(client, ProtocolS.CMD_MOVED + ProtocolS.DELIM_ARG + moveX + ProtocolS.DELIM_ARG + moveY);
	}
	
	private void sendEnd(ClientData client) {
		this.sendCommand(client, ProtocolS.CMD_END);
	}

	private void sendDisconnected(ClientData client, String name) {
		this.sendCommand(client, ProtocolS.CMD_DISCONNECTED + ProtocolS.DELIM_ARG + name);
	}

	private void sendSaid(ClientData client, String name, String message) {
		this.sendCommand(client, ProtocolS.CMD_SAID + ProtocolS.DELIM_ARG + name + ProtocolS.DELIM_ARG + message);
	}

	private void sendError(ClientData client, int errorCode) {
		this.sendCommand(client, ProtocolS.CMD_ERROR + ProtocolS.DELIM_ARG + errorCode);
	}
}
