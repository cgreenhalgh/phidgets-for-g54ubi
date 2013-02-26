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
	static enum State { NEW, CONNECTING, CONNECTED, FAILED }

	private static final String VALUE_ATTR = "value";

	private static final String DELTA_VALUE_ATTR = "delta";

	private static final long WAIT_CONNECT_TIME = 30000;

	private static final String UNION_SERVERNAME = "union.servername";

	private static final String DEFAULT_SERVERNAME = "tryunion.com";

	private static final String UNION_SERVERPORT = "union.serverport";

	private static final int DEFAULT_SERVERPORT = 80;

	private static final String UNION_ROOMPREFIX = "union.roomprefix";
	
	private Mariner mar;
	private State state;
	private int mMyId;
	private String host = "tryunion.com";
	private int port = 80;
	private int requestCount = 0;
	private int responseCount = 0;
	private Object sync;
	
	UnionClient(Object sync, String servername, int serverport) {
		host = servername;
		port = serverport;
		this.sync = sync;
	}
	
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

	    synchronized (sync) {
	    	synchronized (this) {
	    		state = State.CONNECTING;
	    		sync.notifyAll();
	    	}
	    }
	    mar.open(host, port);
	    System.out.println("(open..., isReady="+mar.isReady()+")");
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
		synchronized (sync) {
			synchronized (this) {
	    		state = State.FAILED;
	    		sync.notifyAll();
	    	}
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
    	synchronized (sync) {
    		synchronized (this) {
	    		if (state==State.CONNECTING) {
	    			state = State.CONNECTED;
	    			sync.notifyAll();
	    		}
	    		else {
	    			System.err.println("Received onConnectionReady in invalid state "+state);
	    		}
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
    	synchronized (sync) {
    		synchronized (this) {
	    		responseCount++;
	    		if (responseCount>=requestCount)
	    			sync.notifyAll();
	    	}
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
    	synchronized (sync) {
    		synchronized (this) {
	    		responseCount++;
	    		if (responseCount>=requestCount)
	    			sync.notifyAll();
	    	}
	    }
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Object sync = new Object();
		final ValueSet values = new ValueSet(sync);
		Configuration configuration2 = null;
		try {
			configuration2 = new Configuration();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println("Error reading configuration: "+e);
			System.exit(-1);
		}
		final Configuration configuration = configuration2;
		new Thread() {
			public void run() {
				PhidgetClient.run(new String[0], values, configuration);
			}
		}.start();

		final String roomPrefix = configuration.getProperty(UNION_ROOMPREFIX, "g54ubi.");
		UnionClient client = new UnionClient(sync, configuration.getProperty(UNION_SERVERNAME, DEFAULT_SERVERNAME), configuration.getProperty(UNION_SERVERPORT, DEFAULT_SERVERPORT));
		while (true) {
			try {
				long startTime = System.currentTimeMillis();
				client.connect();
				State state = State.NEW;
				synchronized (sync) {
					synchronized (client) {
						state=client.state;
					}
					while (state==State.CONNECTING) {
						try {
							long now = System.currentTimeMillis();
							long elapsed = now-startTime;
							if (elapsed<WAIT_CONNECT_TIME)
								sync.wait(WAIT_CONNECT_TIME-elapsed);
							else
								client.close();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							System.out.println("client.wait interrupted");
						}
						synchronized (client) {
							state=client.state;
						}
					}
				}
				System.out.println("State: "+state);
				long lastSendTime = System.currentTimeMillis();
	
				for (String valueId : values.getValueIds()) {
					Value value = values.getValue(valueId);
					if (value!=null)
						value.resetPublished();
				}
				long lastUpdateDone = 0;
	
				while (state==State.CONNECTED) {
					synchronized (sync) {
						boolean uptodate = false;
						synchronized (client) {
							state = client.state;
							uptodate = (client.responseCount==client.requestCount);					
						}
						if (state==State.CONNECTED) {
							// check values
							long lastUpdate = values.getLastUpdate();
							//System.out.println("State="+state+", response="+client.responseCount+", request="+client.requestCount+", uptodate="+uptodate+", lastUpdate="+lastUpdate+", lastUpdateDone="+lastUpdateDone);
							if (uptodate && lastUpdate!=lastUpdateDone) {	
								lastUpdateDone = lastUpdate;
								System.out.println("Update to "+lastUpdate);
								for (String valueId : values.getValueIds()) {
									Value value = values.getValue(valueId);
									if (value!=null) {
										if (!value.isPublished()) {
											String room = roomPrefix+value.getName();
											client.createRoom(room);
											String svalue = value.publish();									
											client.setRoomAttr(room, VALUE_ATTR, svalue);
											double dvalue = value.takeValueChange();
											client.setRoomAttr(room, DELTA_VALUE_ATTR, Double.toString(dvalue));
											lastSendTime = System.currentTimeMillis();
										}
										else if (value.isUpdated()) {
											String room = roomPrefix+value.getName();
											String svalue = value.publish();
											client.setRoomAttr(room, VALUE_ATTR, svalue);
											double dvalue = value.takeValueChange();
											client.setRoomAttr(room, DELTA_VALUE_ATTR, Double.toString(dvalue));
											lastSendTime = System.currentTimeMillis();
										}
									}
								}
							}
							try {
								long now = System.currentTimeMillis();
								long elapsed = now-lastSendTime;
								if (elapsed>10000 && uptodate) {
									// don't sync unless uptodate aswell, i.e. max one outstanding sync request
									client.syncTime();
									lastSendTime = System.currentTimeMillis();
								}
								else if (elapsed>WAIT_CONNECT_TIME) {
									System.out.println("Connection timed out");
									client.close();
								}
								else {
									sync.wait(100);
								}
							} catch (InterruptedException e) {
								System.out.println("client.wait interrupted (2)");
							}
						}
					}
				}
			}
			catch(Exception e) {
				System.err.println("Error: "+e);
				e.printStackTrace(System.err);
			}
			System.out.println("Union client failed");
			try {
				Thread.sleep(3000);
			}
			catch (Exception e) {				
			}
			System.out.println("Union client retry...");
		}
	}

}
