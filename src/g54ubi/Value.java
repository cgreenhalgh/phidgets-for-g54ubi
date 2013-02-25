package g54ubi;

/** a value to be published, including sensor value scaling logic */
class Value {
	String id;
	private String name;
	private String value;
	private int ivalue;
	private double dvalue;
	private boolean changed;
	private long changedTime;
	private double scale = 1.0;
	private double offset;
	private String publishedValue;
	private int ivalueChange;
	private boolean published;
	private long publishedTime;
	
	Value(String id, String name, String initialValue) {
		this.id = id;
		this.name = name;
		this.value = initialValue;
		if (initialValue!=null) {
			this.changed = true;
			this.changedTime = System.currentTimeMillis();
		}
	}
	
	public Value(String id, String sensorname, double scale, double offset, int value) {
		this.id = id;
		this.name = sensorname;
		this.scale = scale;
		this.offset = offset;
		setValue(value);
	}

	synchronized void setValue(String value) {
		if (value!=this.value && (value==null || !value.equals(this.value))) {
			this.value = value;
			this.changed = true;
			this.changedTime = System.currentTimeMillis();
			System.out.println("Value "+id+" ("+name+") = "+value);
			this.notifyAll();
		}
	}

	synchronized public void setValue(int value) {
		if (value!=this.ivalue) {
			ivalue = value;
			ivalueChange += Math.abs(value-this.ivalue);
			dvalue = value*scale+offset;
			this.value = Double.toString(dvalue);
			changed = true;
			changedTime = System.currentTimeMillis();
			System.out.println("Value "+id+" ("+name+") = "+this.value);
			this.notifyAll();
		}
	}
}