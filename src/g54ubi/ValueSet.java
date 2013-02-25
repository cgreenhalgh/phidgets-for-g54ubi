/**
 * 
 */
package g54ubi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author cmg
 *
 */
public class ValueSet {
	private Object sync;
	/** all current known values */
	private Map<String,Value> values = new HashMap<String,Value>();
	/** all current know value ids; NB not updated, replaced! */
	private Set<String> valueIds = new HashSet<String>();
	private long lastUpdate = 0;

	void insertValue(Value val) {
		synchronized (sync) {
			synchronized (this) {
				values.put(val.id, val);
				// copy key set for thread safety
				valueIds = new HashSet<String>(values.keySet());			
				lastUpdate++;
			}
			sync.notifyAll();
		}
	}

	ValueSet(Object sync) {
		this.sync = sync;
	}
	
	void setValue(String id, String value) {
		synchronized (sync) {
			synchronized (this) {
				Value v = values.get(id);
				if (v!=null)
					v.setValue(value);
				else
					System.err.println("setValue for unknown Value "+id);
				lastUpdate++;
			}
			sync.notifyAll();
		}
	}
	void setValue(String id, int value) {
		synchronized (sync) {
			synchronized (this) {
				Value v = values.get(id);
				if (v!=null)
					v.setValue(value);
				else
					System.err.println("setValue for unknown Value "+id);
				lastUpdate++;
			}
			sync.notifyAll();
		}
	}
	
	synchronized Set<String> getValueIds() {
		return valueIds;
	}
	
	synchronized Value getValue(String id) {
		return values.get(id);
	}
	
	synchronized long getLastUpdate() {
		return lastUpdate;
	}
}
