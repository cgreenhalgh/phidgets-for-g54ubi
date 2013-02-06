package g54ubi;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import net.user1.mariner.Mariner;
import net.user1.mariner.MarinerEvent;
import net.user1.mariner.MessageEvent;


public class MarinerTest {

	Logger logger = Logger.getLogger(MarinerTest.class);
	
	Mariner mar;

	boolean connected = false;
	
	int mMyId;
	public void Connect() {

		logger.info("Connecting...");
		
	    mar = new Mariner();

	    mar.addEventListener(MarinerEvent.READY, this, "onConnectionReady");
	    mar.addEventListener(MarinerEvent.SHUTDOWN, this, "onConnectionShutdown");

	    // u6 JOINED_ROOM
	    mar.getMessageManager().addEventListener("u6", this, "onJoinedRoom");
	    // u7 RECEIVE_MESSAGE
	    mar.getMessageManager().addEventListener("u7", this, "onMessage");
	    
	    // u8 CLIENT_ATTR_UPDATE
	    mar.getMessageManager().addEventListener("u8", this, "onClientAttrUpdate");
	    // u9 ROOM_ATTR_UPDATE
	    mar.getMessageManager().addEventListener("u9", this, "onRoomAttrUpdate");

	    // u29 CLIENT_METADATA
	    mar.getMessageManager().addEventListener("u29", this, "onClientMetadata");

	    // u32 CLIENT_ROOM_RESULT
	    mar.getMessageManager().addEventListener("u32", this, "onCreateRoomResult"); 
	    // u33 REMOVE_ROOM_RESULT
	    mar.getMessageManager().addEventListener("u33", this, "onMessage");
	    // u34 CLIENT_COUNT_SNAPSHOT
	    mar.getMessageManager().addEventListener("u34", this, "onMessage");
	    // u36 CLIENT_ADDED_TO_ROOM
	    mar.getMessageManager().addEventListener("u36", this, "onMessage");
	    // u37 CLIENT_REMOVED_FROM_ROOM
	    mar.getMessageManager().addEventListener("u37", this, "onMessage");
	    // u38 ROOMLIST_SNAPSHOT
	    mar.getMessageManager().addEventListener("u38", this, "onMessage");
	    // u39 ROOM_ADDED
	    mar.getMessageManager().addEventListener("u39", this, "onMessage");
	    // u40 ROOM_REMOVED
	    mar.getMessageManager().addEventListener("u40", this, "onMessage");
	    // u42 WATCH_FOR_ROOMS_RESULT
	    mar.getMessageManager().addEventListener("u42", this, "onMessage");
	    // u43 STOP_WATCHING_FOR_ROOMS_RESULT
	    mar.getMessageManager().addEventListener("u43", this, "onMessage");
	    // u44 LEFT_ROOM
	    mar.getMessageManager().addEventListener("u44", this, "onMessage");
	    // u47 CREATE_ACCOUNT_RESULT
	    // u48 REMOVE_ACCOUNT_RESULT
	    // u49 LOGIN_RESULT
	    // u50 SERVER_TIME_UPDATE
	    mar.getMessageManager().addEventListener("u50", this, "onServerTimeUpdate");
	    
	    // u54 ROOM_SNAPSHOT
	    // u59 OBSERVED_ROOM
	    // u60 GET_ROOM_SNAPSHOT_RESULT
	    // u62 STOPPED_OBSERVING_ROOM
	    // u63 CLIENT_READY
	    mar.getMessageManager().addEventListener("u63", this, "onClientReady");
	    // u66 SERVER_HELLO
	    mar.getMessageManager().addEventListener("u66", this, "onServerHello");
	    // u72 JOIN_ROOM_RESULT
	    mar.getMessageManager().addEventListener("u72", this, "onJoinRoomResult"); 
	    // u73 SET_CLIENT_ATTR_RESULT
	    // u74 SET_ROOM_ATTR_RESULT
	    // u75 GET_CLIENTCOUNT_SNAPSHOT_RESULT
	    // u76 LEAVE_ROOM_RESULT
	    // u77 OBSERVE_ROOM_RESULT
	    // u78 STOP_OBSERVING_ROOM_RESULT
	    // u79 ROOM_ATTR_REMOVED
	    // u80 REMOVE_ROOM_ATTR_RESULT
	    // u81 CLIENT_ATTR_REMOVED
	    // u82 REMOVE_CLIENT_ATTR_RESULT
	    // u84 SESSION_TERMINATED
	    mar.getMessageManager().addEventListener("u84", this, "onSessionTerminated");
	    // u85 SESSION_NOT_FOUND
	    // u87 LOGOFF_RESULT
	    // u88 LOGGED_IN
	    // u89 LOGGED_OFF
	    // u90 ACCOUNT_PASSWORD_CHANGED
	    // u101 CLIENTLIST_SNAPSHOT
	    // u102 CLIENT_ADDED_TO_SERVER
	    // u103 CLIENT_REMOVED_FROM_SERVER
	    // u104 CLIENT_SNAPSHOT
	    // u105 OBSERVE_CLIENT_RESULT
	    // u106 STOP_OBSERVING_CLIENT_RESULT
	    // u107 WATCH_FOR_CLIENTS_RESULT
	    // u108 STOP_WATCHING_FOR_CLIENTS_RESULT
	    // u109 WATCH_FOR_ACCOUNTS_RESULT
	    // u110 STOP_WATCHING_FOR_ACCOUNTS_RESULT
	    // u111 ACCOUNT_ADDED
	    // u112 ACCOUNT_REMOVED
	    // u113 JOINED_ROOM_ADDED_TO_CLIENT
	    mar.getMessageManager().addEventListener("u113", this, "onMessage");
	    // u114 JOINED_ROOM_REMOVED_FROM_CLIENT
	    mar.getMessageManager().addEventListener("u114", this, "onMessage");
	    // u115 GET_CLIENT_SNAPSHOT_RESULT
	    // u116 GET_ACCOUNT_SNAPSHOT_RESULT
	    // u117 OBSERVED_ROOM_ADDED_TO_CLIENT
	    // u118 OBSERVED_ROOM_REMOVED_FROM_CLIENT
	    // u119 CLIENT_OBSERVED
	    // u120 STOPPED_OBSERVING_CLIENT
	    // u123 OBSERVE_ACCOUNT_RESULT
	    // u124 ACCOUNT_OBSERVED
	    // u125 STOP_OBSERVING_ACCOUNT_RESULT
	    // u126 STOPPED_OBSERVING_ACCOUNT
	    // u127 ACCOUNTLIST_SNAPSHOT
	    // u128 UPDATE_LEVELS_UPDATE
	    // u129 CLIENT_OBSERVED_ROOM
	    // u130 CLIENT_STOPPED_OBSERVING_ROOM
	    // u131 ROOM_OCCUPANTCOUNT_UPDATE
	    // u132 ROOM_OBSERVERCOUNT_UPDATE
	    // u134 ADD_ROLE_RESULT
	    // u136 REMOVE_ROLE_RESULT
	    // u138 BAN_RESULT
	    // u140 UNBAN_RESULT
	    // u142 BANNED_LIST_SNAPSHOT
	    // u144 WATCH_FOR_BANNED_ADDRESSES_RESULT
	    // u146 STOP_WATCHING_FOR_BANNED_ADDRESSES_RESULT
	    // u147 BANNED_ADDRESS_ADDED
	    // u148 BANNED_ADDRESS_REMOVED
	    // u150 KICK_CLIENT_RESULT
	    // u152 SERVERMODULELIST_SNAPSHOT
	    // u155 GET_UPC_STATS_SNAPSHOT_RESULT
	    // u156 UPC_STATS_SNAPSHOT
	    // u158 RESET_UPC_STATS_RESULT
	    // u160 WATCH_FOR_PROCESSED_UPCS_RESULT
	    // u161 PROCESSED_UPC_ADDED
	    // u163 STOP_WATCHING_FOR_PROCESSED_UPCS_RESULT
	    // u164 CONNECTION_REFUSED
	    mar.getMessageManager().addEventListener("u164", this, "onMessage");
	    // u166 NODELIST_SNAPSHOT
	    // u168 GATEWAYS_SNAPSHOT
	    
	    System.out.println("Preparing to connect.....");

	    mar.open("tryunion.com", 80);
	    // this should ave sent a u65 (CLIENT_HELLO)
	    // in return we should get u66, then if OK u29 (CLIENT_METADATA: clientID), u63 (CLIENT_READY)
	}
	public void onConnectionShutdown(MarinerEvent evt) {
		System.out.println("Connection shutdown");
	}
	public void onConnectionReady(MarinerEvent evt) {

	    System.out.println("Connection established.");

	    // start polling thread doing SYNC_TIME
	    connected = true;
	    new Thread() {
	    	public void run() {
	    		try {
    				sleep(10000);
	    			while (connected) {
	    				// u19 SYNC_TIME
	    				System.out.println("Send SYNC_TIME");
	    				mar.getMessageManager().sendUPC("u19");
	    				sleep(10000);
	    			}
	    		} catch (Exception e) 
	    		{
	    			System.out.println("Error in send SYNC_TIME: "+e);
	    		}
	    	}
	    }.start();

	}
	public void createRoom(String roomID) {
		//logger.debug("request room");
		// dur, CREATE_ROOM is u24
		System.out.println("Create room...");
		//roomID
		//roomSettingName1[RS]roomSettingValue1 [RS] roomSettingNamen[RS]roomSettingValuen
		//attrName1[RS]attrVal1[RS]attrOptions [RS] attrName2[RS]attrVal2[RS]attrOptions [RS]...attrNamen[RS]attrValn[RS]attrOptions
		//CLASS[RS]qualifiedClassName1 [RS] CLASS[RS]qualifiedClassNamen [RS] SCRIPT[RS]pathToScript1 [RS] SCRIPT[RS]pathToScriptn
		mar.getMessageManager().sendUPC("u24", roomID, "", "", ""); // create a room called "chatRoom" with default settings, no attributes, and no room modules
	}
	public void onMessage(MessageEvent evt) {
		System.out.println("Message: "+evt.getUPCMessage());
	}
	public void onCreateRoomResult(MessageEvent evt) {
		// u32 CLIENT_ROOM_RESULT
		//roomID
		//status SUCCESS | ROOM_EXISTS
		String roomID = evt.getUPCMessage().getArgText(0);
		String status = evt.getUPCMessage().getArgText(1);
	    System.out.println("CLIENT_ROOM_RESULT: room=" + roomID + " status=" + status);
	    
	    joinRoom(roomID);
	}
	public void joinRoom(String roomID) {
	    System.out.println("Join room...");
	    // u3 JOIN_ROOM
	    // roomID, password
	    mar.getMessageManager().sendUPC("u4", roomID, "");
	    // returns u72
	}
	public void onJoinedRoom(MessageEvent evt) {
		// u6 JOINED_ROOM
		//roomID
		String roomID = evt.getUPCMessage().getArgText(0);
	    System.out.println("JOINED_ROOM: room=" + roomID);
	}
	public void onJoinRoomResult(MessageEvent evt) {
		// u72 JOIN_ROOM_RESULT
		//roomID
		//status 
		String roomID = evt.getUPCMessage().getArgText(0);
		String status = evt.getUPCMessage().getArgText(1);
	    System.out.println("JOIN_ROOM_RESULT: room=" + roomID + " status=" + status);
	}	    
	public void onServerHello(MessageEvent evt) {
		// u66 SERVER_HELLO
		//serverVersion
		//sessionID
		//upcVersion
		//protocolCompatible
		//affinityAddress
		//affinityDuration
	    System.out.println("SERVER_HELLO: " + evt.getUPCMessage());
	    System.out.println("Compatible: "+evt.getUPCMessage().getArgText(3));
	}
	public void onClientMetadata(MessageEvent evt) {
		// u29 CLIENT_METADATA
		// clientID
	    System.out.println("CLIENT_METADATA: " + evt.getUPCMessage());

	    mMyId = Integer.parseInt(evt.getUPCMessage().getArgText(0));
	    System.out.println("ClientID = "+mMyId);
	}
	public void onClientReady(MessageEvent evt) {
		// u63 CLIENT_READY
		// (no args)
	    System.out.println("CLIENT_READY");

	    //createRoom();
	    createRoom("cmg.chatRoom");
	}
	public void onServerTimeUpdate(MessageEvent evt) {
		// u50 SERVER_TIME_UPDATE
		// timeOnServer
		System.out.println("SERVER_TIME_UPDATE: "+evt.getUPCMessage().getArgText(0));
	}
	public void onClientAttrUpdate(MessageEvent evt) {
		// u8
		//roomID
		//clientID
		//userID
		//attrName
		//attrVal
		//attrOptions
	    System.out.println("CLIENT_ATTR_UPDATE: " + evt.getUPCMessage());

	}
	public void onRoomAttrUpdate(MessageEvent evt) {
		// u9
		//roomID
		//clientID
		//attrName
		//attrVal
	    System.out.println("ROOM_ATTR_UPDATE: " + evt.getUPCMessage());

	}
	public void onSessionTerminated(MessageEvent evt) {
		// u84 SESSION_TERMINATED
		System.out.println("SESSION_TERMINATED!");
	}
	public static void main(String args[]) {

	    MarinerTest client = new MarinerTest();

	    client.Connect();

	}//end of main
}
