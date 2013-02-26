package g54ubi.union;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Message {
	private static final String UTF_8 = "UTF-8";
	private String id;
	private String args[];

	public static String CLIENT_HELLO = "u65";
	
	public Message(String id, String[] args) {
		super();
		this.id = id;
		this.args = args;
	}
	
	public static Message newClientHello(String clientType, String userAgent, String upcVersion) {
		return new Message(CLIENT_HELLO, new String[] { clientType, userAgent, upcVersion });
	}
	public static Message newClientHello() {
		return newClientHello("Java-g54ubi","Java test client g54ubi.union","1.10.3");
	}
	
	public void write(OutputStream os) throws UnsupportedEncodingException, IOException {
		// urlencode < %3C > %3E / %2F ! %21  [ %5B ] %5D
		os.write("%3CU%3E%3CM%3E".getBytes(UTF_8));
		os.write(id.getBytes(UTF_8));
		os.write("%3C%2FM%3E%3CL%3E".getBytes(UTF_8));
		for (int i=0; i<args.length; i++) {
			os.write("%3CA%3E".getBytes(UTF_8));
			if (args[i]!=null) {
				if (args[i].indexOf("<")>=0) {					
					os.write("%3C%21%5BCDATA%5B".getBytes(UTF_8));
					os.write(URLEncoder.encode(args[i],UTF_8).getBytes(UTF_8));
					os.write("%5D%5D%3E".getBytes(UTF_8));
				} else
					os.write(args[i].getBytes(UTF_8));
			}
			os.write("%3C%2FA%3E".getBytes(UTF_8));
		}
		os.write("%3C%2FL%3E%3C%2FU%3E".getBytes(UTF_8));
	}
}
