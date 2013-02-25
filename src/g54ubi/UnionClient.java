/**
 * 
 */
package g54ubi;

import net.user1.mariner.Mariner;
import net.user1.mariner.MarinerEvent;
import net.user1.mariner.MessageEvent;

/**
 * @author cmg
 *
 */
public class UnionClient {
	static enum State { NEW, CONNECTING, CONNECTED, FAILED };
	
	private Mariner mar;
	private State state;
	private int mMyId;
	private String host = "tryunion.com";
	private int port = 80;
	private int requestCount = 0;
	private int responseCount = 0;
	
	private void connect() {
		
	    mar = new Mariner();

	    mar.addEventListener(MarinerEvent.READY, this, "onConnectionReady");
	    mar.addEventListener(MarinerEvent.SHUTDOWN, this, "onConnectionShutdown");

	    // u29 CLIENT_METADATA
	    mar.getMessageManager().addEventListener("u29", this, "onClientMetadata");
	    // u32 CLIENT_ROOM_RESULT
	    mar.getMessageManager().addEventListener("u32", this, "onCreateRoomResult"); 
	    // u50 SERVER_TIME_UPDATE
	    mar.getMessageManager().addEventListener("u50", this, "onServerTimeUpdate");
	    // u74 SET_ROOM_ATTR_RESULT
	    mar.getMessageManager().addEventListener("u74", this, "onSetRoomAttrResult"); 
	    // u84 SESSION_TERMINATED
	    mar.getMessageManager().addEventListener("u84", this, "onSessionTerminated");
	    // u164 CONNECTION_REFUSED
	    mar.getMessageManager().addEventListener("u164", this, "onConnectionRefused");
	    
	    System.out.println("open...");

	    synchronized (this) {
	    	state = State.CONNECTING;
	    	this.notifyAll();
	    }
	    mar.open(host, port);
	    System.out.println("(open...)");
	    // this should ave sent a u65 (CLIENT_HELLO)
	    // in return we should get u66, then if OK u29 (CLIENT_METADATA: clientID), u63 (CLIENT_READY)
	}
	public void onConnectionShutdown(MarinerEvent evt) {
		System.out.println("Connection shutdown");
		close();
	}
	public void onSessionTerminated(MessageEvent evt) {
		// u84 SESSION_TERMINATED
		System.out.println("SESSION_TERMINATED!");
		close();
	}
	public void onConnectionRefused(MessageEvent evt) {
		// u164 CONNECTION_REFUSE
		System.out.println("CONNECTION_REFUSE!");
		close();
	}
	public void close() {
	    synchronized (this) {
	    	state = State.FAILED;
	    	this.notifyAll();
	    }
		try {
			mar.close();
		}
		catch(Exception e) {
			System.err.println("Error closing Mariner: "+e);
		}
	}
	public void onConnectionReady(MarinerEvent evt) {
	    System.out.println("Connection ready.");

	    // start polling thread doing SYNC_TIME
	    synchronized (this) {
	    	if (state==State.CONNECTING) {
	    		state = State.CONNECTED;
	    		this.notifyAll();
	    	}
	    	else {
	    		System.err.println("Received onConnectionReady in invalid state "+state);
	    		close();
	    	}
	    }
	}
	public void onClientMetadata(MessageEvent evt) {
		// u29 CLIENT_METADATA
		// clientID
	    System.out.println("CLIENT_METADATA: " + evt.getUPCMessage());

	    mMyId = Integer.parseInt(evt.getUPCMessage().getArgText(0));
	    System.out.println("ClientID = "+mMyId);
	}
	public void createRoom(String roomID) {
		// dur, CREATE_ROOM is u24
		System.out.println("Send CREATE_ROOM...");
		//roomID
		//roomSettingName1[RS]roomSettingValue1 [RS] roomSettingNamen[RS]roomSettingValuen
		//attrName1[RS]attrVal1[RS]attrOptions [RS] attrName2[RS]attrVal2[RS]attrOptions [RS]...attrNamen[RS]attrValn[RS]attrOptions
		//CLASS[RS]qualifiedClassName1 [RS] CLASS[RS]qualifiedClassNamen [RS] SCRIPT[RS]pathToScript1 [RS] SCRIPT[RS]pathToScriptn
		mar.getMessageManager().sendUPC("u24", roomID, "", "", ""); // create a room called "chatRoom" with default settings, no attributes, and no room modules
	}
	public void onCreateRoomResult(MessageEvent evt) {
		// u32 CLIENT_ROOM_RESULT
		//roomID
		//status SUCCESS | ROOM_EXISTS
		String roomID = evt.getUPCMessage().getArgText(0);
		String status = evt.getUPCMessage().getArgText(1);
	    System.out.println("CLIENT_ROOM_RESULT: room=" + roomID + " status=" + status);
	}
	public void setRoomAttr(String roomID, String name, String value) {
		System.out.println("Send SET_ROOM_ATTR "+roomID+" "+name+" = "+value);
		// u5 SET_ROOM_ATTR
		//roomID
		//attrName
		//escapedAttrValue
		//attrOptions, an integer whose bits have the following meaning when set:
		// 2 - shared
		// 3 - persistent
		// 8 - evaluate
		// shared
		String escapedValue = value.replace("<[CDATA[","<([CDATA[").replace("]]>","]])>");
	    mar.getMessageManager().sendUPC("u5", roomID, name, escapedValue, Integer.toString(0x04));
		synchronized (this) {
			requestCount++;
		}
	}
	public void onSetRoomAttrResult(MessageEvent evt) {
		// u74
		// roomID
		// attrName
		// status
	    System.out.println("SET_ROOM_ATTR_RESULT: " + evt.getUPCMessage());
	    synchronized (this) {
	    	responseCount++;
	    	if (responseCount>=requestCount)
	    		this.notifyAll();
	    }
	}
	public void syncTime() {
		System.out.println("Send SYNC_TIME");
		mar.getMessageManager().sendUPC("u19");
		synchronized (this) {
			requestCount++;
		}
	}
	public void onServerTimeUpdate(MessageEvent evt) {
		// u50 SERVER_TIME_UPDATE
		// timeOnServer
		System.out.println("SERVER_TIME_UPDATE: "+evt.getUPCMessage().getArgText(0));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		UnionClient client = new UnionClient();
		client.connect();
		State state = State.NEW;
		synchronized (client) {
			while ((state=client.state)==State.CONNECTING)
				try {
					client.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					System.out.println("client.wait interrupted");
				}
		}
		System.out.println("State: "+state);
			
		while (state==State.CONNECTED) {
			synchronized (client) {
				state = client.state;
				if (state==State.CONNECTED) {
					try {
						client.wait(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						System.out.println("client.wait interrupted (2)");
					}
				}
			}
			if (state==State.CONNECTED) {
				client.syncTime();
			}
		}
	}

}
