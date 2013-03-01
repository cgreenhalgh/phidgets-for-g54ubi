/**
 * 
 */
package g54ubi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import com.phidgets.*;
import com.phidgets.event.*;


/** Manage local phidgets and relay state to Union server.
 * 
 * @author cmg
 *
 */
public class PhidgetClient {
	
	private static boolean pollRfid = true;

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
						Collection<Phidget> phidgets2 = null;
						synchronized (phidgets) {
							// clone list to avoid concurrent update problems
							phidgets2 = new Vector<Phidget>(phidgets.values());
						}
						for (Phidget phid : phidgets2) {
							if (phid instanceof RFIDPhidget) {
								RFIDPhidget rfid = (RFIDPhidget)phid;
								try {
									if (rfid.isAttached()) {
										rfid.setAntennaOn(true);
										rfid.setLEDOn(true);
										// There doesn't seem to be a specific minimum to have the antenna on
										// but 20ms seems very unreliable; 50ms a bit unreliable; 100ms fairly reliable.
										wait(100);
										count++;
										if (pollRfid) {
											String tag = rfid.getLastTag();
											if (!rfid.getTagStatus()) {
												tag = null;
											}
											if (tag!=null) {
												// check valid?
												for (int i=0; i<tag.length(); i++) {
													char c = tag.charAt(i);
													if (!Character.isLetterOrDigit(c) || ((int)c)>0x7f) {
														System.err.println("Ignore tag with non-ascii value: "+tag);
														tag = null;
														break;
													}
												}
											}
											if (tag!=null)
												System.out.println("Reader "+rfid.getSerialNumber()+" tag "+tag);

											String valueid = RFID_PREFIX+rfid.getSerialNumber();
											values.setValue(valueid, tag);
										}
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
	
	static ValueSet values;
	
	private static final String PHIDGET_SERVER = "phidget.server";
	private static final String IFKIT_PREFIX = "ifkit.";
	protected static final String SENSOR_INFIX = ".sensor.";
	protected static final String SCALE_SUFFIX = ".scale";
	protected static final String OFFSET_SUFFIX = ".offset";
	private static final String RFID_PREFIX = "rfid.";
	protected static final String PERIOD_SUFFIX = ".period";
	protected static final String TRIGGER_SUFFIX = ".trigger";
	
	static void handleAttachInterfaceKit(String serverID, final int id) throws PhidgetException {
		if (phidgets.containsKey(id)) 
			return;
		final InterfaceKitPhidget ifkit = new InterfaceKitPhidget();
		if (serverID!=null)											
			ifkit.open(id, serverID);
		else
			ifkit.open(id);
		synchronized(phidgets) {
			phidgets.put(id, ifkit);
		}

		// find in properties...
		final String prefix = configuration.findPropertiesKey(IFKIT_PREFIX, String.valueOf(id));
		final String ifkitname = configuration.getProperty(prefix+Configuration.NAME_SUFFIX, "undefined");

		System.out.println("Added InterfaceKit "+id+ "("+ifkitname+")");
		
		ifkit.addSensorChangeListener(new SensorChangeListener() {
			
			@Override
			public void sensorChanged(SensorChangeEvent sce) {
				// TODO Auto-generated method stub
				System.out.println("Sensor change "+sce.getIndex()+" = "+sce.getValue());
				String sensorid = IFKIT_PREFIX+id+SENSOR_INFIX+sce.getIndex();
				values.setValue(sensorid, sce.getValue());
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
						String sensorid = IFKIT_PREFIX+id+SENSOR_INFIX+i;
						
						int value = ifkit.getSensorValue(i);
						System.out.println("Sensor initial "+i+" = "+ifkit.getSensorValue(i));
						
						
						String sensorprefix = prefix+SENSOR_INFIX+i;
						// configure rate/sensitivity to change
						int period = (int)(configuration.getProperty(sensorprefix+PERIOD_SUFFIX, 1.0)*1000);
						ifkit.setDataRate(i, period);
						int trigger = configuration.getProperty(sensorprefix+TRIGGER_SUFFIX, 10);
						ifkit.setSensorChangeTrigger(i, trigger);
						
						String sensorname = configuration.getProperty(sensorprefix+Configuration.NAME_SUFFIX, "undefined"+i);
						// TODO name prefix
						double scale = configuration.getProperty(sensorprefix+SCALE_SUFFIX, 1.0);
						double offset = configuration.getProperty(sensorprefix+OFFSET_SUFFIX, 0.0);
						Value val = values.getValue(sensorid);
						if (val==null){
							val = new Value(sensorid, sensorname, scale, offset, value);
							values.insertValue(val);
						}
						else 
							values.setValue(sensorid, value);
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

	protected static void handleAttachRFID(String serverID, final int id) throws PhidgetException {
		if (phidgets.containsKey(id)) 
			return;
		final RFIDPhidget rfid = new RFIDPhidget();
		if (serverID!=null)
			rfid.open(id, serverID);
		else
			rfid.open(id);

		// find in properties...
		final String prefix = configuration.findPropertiesKey(RFID_PREFIX, String.valueOf(id));
		final String name = configuration.getProperty(prefix+Configuration.NAME_SUFFIX, "undefined");

		System.out.println("Added RFID "+id+" ("+name+")");

		final String valueid = RFID_PREFIX+id;
		Value val = values.getValue(valueid);
		if (val==null){
			val = new Value(valueid, name, null);
			values.insertValue(val);
		}		
		// delay add phidget to attached
		if (!pollRfid) {
			// use polling instead
			rfid.addTagGainListener(new TagGainListener() {

				@Override
				public void tagGained(TagGainEvent tge) {
					//System.out.println("Gained tag "+tge.getValue()+" on "+id);
					values.setValue(valueid, tge.getValue());
				}
			});
			rfid.addTagLossListener(new TagLossListener() {

				@Override
				public void tagLost(TagLossEvent tge) {
					//System.out.println("Lost tag "+tge.getValue()+" on "+id);
					values.setValue(valueid, null);

				}
			});
		}
		rfid.addAttachListener(new AttachListener() {
			
			@Override
			public void attached(AttachEvent ae) {
				// TODO Auto-generated method stub
				System.out.println("RFID "+id+" attached");
				synchronized(phidgets) {
					phidgets.put(id, rfid);
				}
				try {
					rfid.setAntennaOn(false);
					rfid.setLEDOn(false);
				} catch (PhidgetException e) {
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

	static Configuration configuration;
	static Map<Integer,Phidget> phidgets = new HashMap<Integer,Phidget>();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Configuration configuration = null;
		try {
			configuration = new Configuration();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println("Error reading configuration: "+e);
			System.exit(-1);
		}
		run(args, new ValueSet(new Object()), configuration);
	}
	
	static class TimerThread extends Thread {
		private static final String TIMER_NAME = "timer.name";
		private static final String TIMER_PERIOD = "timer.period";
		Value timer;
		long periodms;
		private static String ID = "timer";
		TimerThread() {
			timer = new Value(ID, configuration.getProperty(TIMER_NAME, "timer"), getValue());
			values.insertValue(timer);

			double period = configuration.getProperty(TIMER_PERIOD, 10.0);
			if (period<1)
				period = 1;			
			periodms = (long)(period*1000);
			setDaemon(true);
		}
		private String getValue() {
			return getIPAddress()+" "+new Date().toString();
		}
		private String getIPAddress() {
			StringBuilder sb = new StringBuilder();
			try {
				InetAddress ip = InetAddress.getLocalHost();
				sb.append(ip.getHostAddress());
				sb.append("; ");
			}
			catch (Exception e) {
				System.out.println("Error getting local host: "+e);
				sb.append("unknown; ");
			}
			try {
				Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
				while (ifs.hasMoreElements()) {
					NetworkInterface ni = ifs.nextElement();
					Enumeration<InetAddress> ips = ni.getInetAddresses();
					while(ips.hasMoreElements()) {
						InetAddress ip = ips.nextElement();
						
						sb.append(ip.getHostAddress());
						sb.append("; ");
					}
				}
			}
			catch (Exception e) {
				System.out.println("Error getting local host: "+e);
				sb.append("unknown; ");
			}
			return sb.toString();
		}
		public void run() {
			try {
				while (true) {
					sleep(periodms);
					values.setValue(ID, getValue());
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void run(String [] args, ValueSet values2, Configuration configuration2) {
		values = values2;
		try {
			configuration = configuration2;
			
			String serverProp = configuration.getProperty(PHIDGET_SERVER, null);
			final String serverID = (args.length>0 ? args[0] : serverProp); //"mrlphidgetsbc1";
			System.out.println("using phidget.server "+serverID);
			
			Manager man;
			System.out.println(Phidget.getLibraryVersion());

			new TimerThread().start();
			
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
								handleAttachRFID(serverID, id);
							}
							else if (phid.getDeviceClass()==Phidget.PHIDCLASS_INTERFACEKIT) {
								handleAttachInterfaceKit(serverID, id);
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
				if (serverID!=null)
					man.open(serverID);
				else
					man.open();
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
