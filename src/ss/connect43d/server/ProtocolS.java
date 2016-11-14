package ss.connect43d.server;

public class ProtocolS {

	public static final String DELIM_CMD = "\n";
	public static final String DELIM_ARG = " ";
	public static final String ENCODING = "UTF-8";

	public static final String CMD_CONNECT = "CONNECT";
	public static final String CMD_CONNECTED = "CONNECTED";
	public static final String CMD_FEATURES = "FEATURES";
	public static final String FEAT_CHAT = "CHAT";
	public static final String FEAT_CHALLENGE = "CHALLENGE";
	public static final String CMD_FEATURED = "FEATURED";
	public static final String CMD_JOIN = "JOIN";
	public static final String CMD_START = "START";
	public static final String CMD_TURN = "TURN";
	public static final String CMD_MOVE = "MOVE";
	public static final String CMD_MOVED = "MOVED";
	public static final String CMD_END = "END";
	public static final String CMD_DISCONNECTED = "DISCONNECTED";
	
	public static final String CMD_ERROR = "ERROR";
	public static final int ERR_UNDEFINED = 0;
	public static final int ERR_COMMAND_NOT_FOUND = 1;
	public static final int ERR_COMMAND_UNEXPECTED = 2;
	public static final int ERR_INVALID_COMMAND = 3;
	public static final int ERR_INVALID_MOVE = 4;
	public static final int ERR_NAME_IN_USE = 5;

	public static final String CMD_SAY = "SAY";
	public static final String CMD_SAID = "SAID";

	public static final String CMD_LIST = "LIST";
	public static final String CMD_LISTED = "LISTED";
	public static final int STATUS_CONNECTED = 0;
	public static final int STATUS_CHALLENGE = 1;
	public static final int STATUS_INGAME = 2;
	public static final String CMD_CHALLENGE = "CHALLENGE";
	public static final String CMD_CHALLENGED = "CHALLENGED";
	public static final String CMD_ACCEPT = "ACCEPT";
	public static final String CMD_DECLINE = "DECLINE";
	public static final String CMD_DECLINED = "DECLINED";
}