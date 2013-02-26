/**
 * 
 */
package g54ubi.union;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;


/** Attempt to build a Union client using Java WebSocket
 * @author cmg
 *
 */
public class TestClient {

	private static final int CONNECT_TIMEOUT = 30000;
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String TEXT_UTF8 = "text/plain;charset=UTF-8";
	private static final String UTF_8 = "UTF-8";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// see http://unionplatform.com/specs/upc/
			URL url = new URL("http://tryunion.com:80");
			Charset utf8 = Charset.forName(UTF_8);
			// create a CLIENT_HELLO u65 clientType userAgent version (1.10.3 as of 2013-02-26)
			Message client_hello = Message.newClientHello();
			
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(CONNECT_TIMEOUT);
			conn.addRequestProperty(CONTENT_TYPE, TEXT_UTF8);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			System.out.println("Connect...");
			//conn.connect();
			System.out.println("Write output "+client_hello+"...");
			OutputStream os = conn.getOutputStream();
			
			// Hello is send using mode 'd' (no session ID at this point)
			String postdata = "mode=d&data=";//+URLEncoder.encode(client_hello, UTF_8);
			
			os.write(postdata.getBytes(UTF_8));
			client_hello.write(os);

			os.close();
			System.out.println("Get status...");
			int status = conn.getResponseCode();
			if (status<200 || status>299) {
				throw new IOException("Error status: "+status+": "+conn.getResponseMessage());
			}
			System.out.println("Response status "+status+": "+conn.getResponseMessage());
			//Object response = conn.getContent();
			//System.out.println("Returned: "+response);
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), UTF_8));
			while(true) {
				String line = br.readLine();
				if (line==null)
					break;
				System.out.println("Read: "+line);
			}
			br.close();
			
			// typical response:
			// <U><M>u66</M><L><A><![CDATA[Union Server 2.0.1 (build 583)]]></A><A>8862226353127206a-05f5-457e-a39a-8eea78e67bca</A><A>1.10.3</A><A>true</A><A></A><A>0</A></L></U>
			// u66 SERVER_HELLO serverVersion sessionID upcVersion protocolCompatible affinityAddress affinityDuration [minutes]
			
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}

}
