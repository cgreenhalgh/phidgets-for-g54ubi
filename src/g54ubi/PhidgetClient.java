/**
 * 
 */
package g54ubi;

import java.io.FileInputStream;
import java.util.Collection;
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
										//String tag = rfid.getLastTag();
										//if (!rfid.getTagStatus())
										//	tag = null;
										//System.out.println("Reader "+rfid.getSerialNumber()+" tag "+tag);
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
	
	private static final String PROPERTIES_FILE = "phidget.properties";
	private static final String PHIDGET_SERVER = "phidget.server";
	private static final String ID_SUFFIX = ".id";
	private static final String NAME_SUFFIX = ".name";
	private static final String IFKIT_PREFIX = "ifkit.";
	protected static final String SENSOR_INFIX = ".sensor.";
	protected static final String SCALE_SUFFIX = ".scale";
	protected static final String OFFSET_SUFFIX = ".offset";
	private static final String RFID_PREFIX = "rfid.";
	
	/** get config helper */
	static String getProperty(Properties props, String key, String defaultValue) {
		String value = props.getProperty(key);
		if (value==null) {
			System.out.println("Property "+key+" = "+defaultValue+ " (default)");
			return defaultValue;
		}
		System.out.println("Property "+key+" = "+value);
		return value;		
	}
	/** get config helper */
	static int getProperty(Properties props, String key, int defaultValue) {
		String svalue = props.getProperty(key);
		if (svalue==null) {
			System.out.println("Property "+key+" = "+defaultValue+ " (default)");
			return defaultValue;
		}
		try {
			int value = Integer.parseInt(svalue);
			System.out.println("Property "+key+" = "+value);
			return value;				
		}
		catch (NumberFormatException nfe) {
			System.out.println("Property "+key+" = "+defaultValue+" (Invalid non-int value: "+svalue+")");
			return defaultValue;
		}
	}
	/** get config helper */
	static double getProperty(Properties props, String key, double defaultValue) {
		String svalue = props.getProperty(key);
		if (svalue==null) {
			System.out.println("Property "+key+" = "+defaultValue+ " (default)");
			return defaultValue;
		}
		try {
			double value = Double.parseDouble(svalue);
			System.out.println("Property "+key+" = "+value);
			return value;				
		}
		catch (NumberFormatException nfe) {
			System.out.println("Property "+key+" = "+defaultValue+" (Invalid non-double value: "+svalue+")");
			return defaultValue;
		}
	}
	
	/** get config helper - look for <prefix><i>.id = <id> with i=0.. */
	static String findPropertiesKey(String prefix, String id) {
		int defaulti = -1;
		for(int i=0; true; i++) {
			String idkey = prefix+i+ID_SUFFIX;
			String idvalue = props.getProperty(idkey);
			String namekey = prefix+i+NAME_SUFFIX;
			String namevalue = props.getProperty(namekey);
			if (idvalue==null && namevalue!=null) {
				defaulti = i;
			}
			else if (idvalue!=null && idvalue.equals(id))
				return prefix+i;
			else if (namevalue==null)
				break;
		}
		if (defaulti>=0) {
			System.out.println("Note: using default configuration "+defaulti+" for ID "+id);
			return prefix+defaulti;
		}
		System.out.println("Warning: no configuration found for "+prefix+" for ID "+id);
		return prefix+0;
	}
	
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
		final String prefix = findPropertiesKey(IFKIT_PREFIX, String.valueOf(id));
		final String ifkitname = getProperty(props, prefix+NAME_SUFFIX, "undefined");

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
						String sensorname = getProperty(props, sensorprefix+NAME_SUFFIX, "undefined"+i);
						// TODO name prefix
						double scale = getProperty(props, sensorprefix+SCALE_SUFFIX, 1.0);
						double offset = getProperty(props, sensorprefix+OFFSET_SUFFIX, 0.0);
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
		final String prefix = findPropertiesKey(RFID_PREFIX, String.valueOf(id));
		final String name = getProperty(props, prefix+NAME_SUFFIX, "undefined");

		System.out.println("Added RFID "+id+" ("+name+")");

		final String valueid = RFID_PREFIX+id;
		Value val = values.getValue(valueid);
		if (val==null){
			val = new Value(valueid, name, null);
			values.insertValue(val);
		}		
		// delay add phidget to attached
		if (true) {
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

	static Properties props = new Properties();
	static Map<Integer,Phidget> phidgets = new HashMap<Integer,Phidget>();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		run(args, new ValueSet(new Object()));
	}
	public static void run(String [] args, ValueSet values2) {
		values = values2;
		try {
			
			System.out.println("Reading properties from "+PROPERTIES_FILE);
			//Properties props = new Properties();
			props.load(new FileInputStream(PROPERTIES_FILE));
			
			String serverProp = getProperty(props, PHIDGET_SERVER, null);
			final String serverID = (args.length>0 ? args[0] : serverProp); //"mrlphidgetsbc1";
			System.out.println("using phidget.server "+serverID);
			
			Manager man;
			System.out.println(Phidget.getLibraryVersion());

			
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
