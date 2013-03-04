/**
 * 
 */
package g54ubi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * @author cmg
 *
 */
public class Configuration {
	static Logger logger = Logger.getLogger(Configuration.class);

	private Properties props = new Properties();
	public static final String ID_SUFFIX = ".id";
	public static final String NAME_SUFFIX = ".name";

	private static final String PROPERTIES_FILE = "phidget.properties";

	public Configuration() throws FileNotFoundException, IOException {
		logger.info("Reading properties from "+PROPERTIES_FILE);
		//Properties props = new Properties();
		props.load(new FileInputStream(PROPERTIES_FILE));
	}
	/** get config helper */
	public String getProperty(String key, String defaultValue) {
		String value = props.getProperty(key);
		if (value==null) {
			logger.info("Property "+key+" = "+defaultValue+ " (default)");
			return defaultValue;
		}
		logger.info("Property "+key+" = "+value);
		return value;		
	}
	/** get config helper */
	public int getProperty(String key, int defaultValue) {
		String svalue = props.getProperty(key);
		if (svalue==null) {
			logger.info("Property "+key+" = "+defaultValue+ " (default)");
			return defaultValue;
		}
		try {
			int value = Integer.parseInt(svalue);
			logger.info("Property "+key+" = "+value);
			return value;				
		}
		catch (NumberFormatException nfe) {
			logger.info("Property "+key+" = "+defaultValue+" (Invalid non-int value: "+svalue+")");
			return defaultValue;
		}
	}
	/** get config helper */
	public double getProperty(String key, double defaultValue) {
		String svalue = props.getProperty(key);
		if (svalue==null) {
			logger.info("Property "+key+" = "+defaultValue+ " (default)");
			return defaultValue;
		}
		try {
			double value = Double.parseDouble(svalue);
			logger.info("Property "+key+" = "+value);
			return value;				
		}
		catch (NumberFormatException nfe) {
			logger.info("Property "+key+" = "+defaultValue+" (Invalid non-double value: "+svalue+")");
			return defaultValue;
		}
	}
	
	/** get config helper - look for <prefix><i>.id = <id> with i=0.. */
	public String findPropertiesKey(String prefix, String id) {
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
			logger.info("Note: using default configuration "+defaulti+" for ID "+id);
			return prefix+defaulti;
		}
		logger.warn("Warning: no configuration found for "+prefix+" for ID "+id);
		return prefix+0;
	}

}
