/**
 * 
 */
package g54ubi;

import java.util.HashMap;
import java.util.Map;

import com.phidgets.*;
import com.phidgets.event.*;


/** Manage local phidgets and relay state to Union server.
 * 
 * @author cmg
 *
 */
public class PhidgetClient {

	static class RFIDPollThread extends Thread {
		private boolean done = false;
		private Map<Integer,Phidget> phidgets;
		
		RFIDPollThread(Map<Integer,Phidget> phidgets) {
			this.phidgets = phidgets;
		}
		@Override
		public void run() {
			try {
				synchronized (this) {
					while(!done) {
						int count = 0;
						for (Phidget phid : phidgets.values()) {
							if (phid instanceof RFIDPhidget) {
								RFIDPhidget rfid = (RFIDPhidget)phid;
								try {
									if (rfid.isAttached()) {
										rfid.setAntennaOn(true);
										rfid.setLEDOn(true);
										wait(100);
										count++;
										String tag = rfid.getLastTag();
										if (!rfid.getTagStatus())
											tag = null;
										System.out.println("Reader "+rfid.getSerialNumber()+" tag "+tag);
										rfid.setAntennaOn(false);
										rfid.setLEDOn(false);

									}
								} catch (PhidgetException e) {
									// TODO Auto-generated catch block
									System.out.println("Error checking rfid: "+e);
									e.printStackTrace();
								}
							}
						}
						if (count<5)
							wait(20*(5-count));
					}
				}
			}
			catch (InterruptedException ie) {
				System.out.println("poll interrupted");
			}
		}

		synchronized void end() {
			done= true;
			notify();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			final String serverID = (args.length>0 ? args[0] : null); //"mrlphidgetsbc1";
			
			Manager man;
			System.out.println(Phidget.getLibraryVersion());

			final Map<Integer,Phidget> phidgets = new HashMap<Integer,Phidget>();
			
			man = new Manager();
			man.addAttachListener(new AttachListener()
			{
				public void attached(AttachEvent ae) {
					System.out.println("attachment of " + ae);
					try {
						final Phidget phid = ae.getSource();
						final int id = phid.getSerialNumber();
						if (!phidgets.containsKey(ae.getSource().getSerialNumber())) {
							if (phid.getDeviceClass()==Phidget.PHIDCLASS_RFID) {
								System.out.println("Added RFID "+id);
								final RFIDPhidget rfid = new RFIDPhidget();
								if (serverID!=null)
									rfid.open(id, serverID);
								else
									rfid.open(id);
								phidgets.put(ae.getSource().getSerialNumber(), rfid);
								if (false) {
									// use polling instead
									rfid.addTagGainListener(new TagGainListener() {

										@Override
										public void tagGained(TagGainEvent tge) {
											// TODO Auto-generated method stub
											System.out.println("Read tag "+tge.getValue()+" on "+id);
										}
									});
									rfid.addTagLossListener(new TagLossListener() {

										@Override
										public void tagLost(TagLossEvent tge) {
											// TODO Auto-generated method stub
											System.out.println("Lost tag "+tge.getValue()+" on "+id);

										}
									});
								}
								rfid.addAttachListener(new AttachListener() {
									
									@Override
									public void attached(AttachEvent ae) {
										// TODO Auto-generated method stub
										System.out.println("RFID "+id+" attached");
										try {
											rfid.setAntennaOn(false);
											rfid.setLEDOn(false);
										} catch (PhidgetException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								});
								rfid.addDetachListener(new DetachListener() {
									
									@Override
									public void detached(DetachEvent arg0) {
										// TODO Auto-generated method stub
										System.out.println("RFID "+id+" detached");
									}
								});
							}
							else if (phid.getDeviceClass()==Phidget.PHIDCLASS_INTERFACEKIT) {
								System.out.println("Added InterfaceKit "+id);
								final InterfaceKitPhidget ifkit = new InterfaceKitPhidget();
								if (serverID!=null)											
									ifkit.open(id, serverID);
								else
									ifkit.open(id);
								phidgets.put(ae.getSource().getSerialNumber(), ifkit);
								ifkit.addSensorChangeListener(new SensorChangeListener() {
									
									@Override
									public void sensorChanged(SensorChangeEvent sce) {
										// TODO Auto-generated method stub
										System.out.println("Sensor change "+sce.getIndex()+" = "+sce.getValue());
									}
								});
								ifkit.addAttachListener(new AttachListener() {
									
									@Override
									public void attached(AttachEvent ae) {
										// TODO Auto-generated method stub
										System.out.println("interfacekit "+id+" attached");
										int sensors;
										try {
											sensors = ifkit.getSensorCount();
											System.out.println("interfacekit reports "+sensors+" sensors");
											if (sensors>8)
												sensors = 8;
											for (int i=0; i<sensors; i++) {
												int value = ifkit.getSensorValue(i);
												System.out.println("Sensor initial "+i+" = "+ifkit.getSensorValue(i));
											}
										} catch (PhidgetException e) {
											// TODO Auto-generated catch block
											System.err.println("Error reading ifkit:" +e);
											e.printStackTrace(System.err);
										}
									}
								});
								ifkit.addDetachListener(new DetachListener() {
									
									@Override
									public void detached(DetachEvent arg0) {
										// TODO Auto-generated method stub
										System.out.println("interfacekit "+id+" detached");
									}
								});
							}
						}
					} catch (PhidgetException e) {
						// TODO Auto-generated catch block
						System.err.println("Error attach: "+e);
						e.printStackTrace(System.err);
					}
					
				}
			});
			man.addDetachListener(new DetachListener()
			{
				public void detached(DetachEvent ae) {
					System.out.println("detachment of " + ae);
				}
			});
			try
			{
				man.open("mrlphidgetsbc1");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			RFIDPollThread poll = new RFIDPollThread(phidgets);
			poll.start();
			System.out.println("Press any key to exit...");
			System.in.read();

			poll.end();
			poll.interrupt();
			Thread.sleep(100);
			man.close();
			Thread.sleep(1000);
			man = null;
			Thread.sleep(1000);
			System.out.println(" ok");
			Thread.sleep(1000);
		} catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}

}
