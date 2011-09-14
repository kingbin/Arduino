package com.rapplogic.xbee.xmpp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.log4j.Logger;

import com.rapplogic.xbee.XBeeConnection;
import com.rapplogic.xbee.util.ByteUtils;

/**
 * This is a bit of a misnomer as we are not connected to anything.  This class serves as a buffer
 * and provide input/output streams for xbee-api to use
 * 
 * @author andrew
 *
 */
public class XmppXBeeConnection implements XBeeConnection {

	private final static Logger log = Logger.getLogger(XBeeConnection.class);
	
	private int[] outputBuffer = new int[200];
	private int outPosition;
	
	private ConnectionSink sink;
	
	private PipedOutputStream pos = new PipedOutputStream();
	private PipedInputStream pis = new PipedInputStream();
	
	private OutputStream out = new OutputStream() {

		@Override
		public void write(int arg0) throws IOException {
			
			if (outPosition >= outputBuffer.length) {
				throw new IOException("Problem: index at end of output buffer");
			}
			
			outputBuffer[outPosition] = arg0;
			outPosition++;
		}
		
		@Override
		public void flush() throws IOException {
			
			if (outPosition == 0) {
				throw new IOException("Nothing to write!");
			}
			
			// write all avail. bytes to sink
			int[] packet = new int[outPosition];
			System.arraycopy(outputBuffer, 0, packet, 0, outPosition);
			
			try {
				sink.send(packet);	
			} catch (Exception e) {
				throw new IOException("Failed to send packet to XBee", e);
			}
			
			//reset out pos
			outPosition = 0;
		}
	};
	
	public XmppXBeeConnection(ConnectionSink sink) {
		this.sink = sink;
		
		try {
			pis.connect(pos);	
		} catch (IOException e) {
			// won't happen
			throw new RuntimeException(e);
		}
	}
	
	public void addPacket(int[] packet) throws IOException {
		
		log.debug("addPacket -- received " + ByteUtils.toBase16(packet));
		
		boolean wasEmpty = pis.available() == 0;
		
		for (int i = 0; i < packet.length; i++) {
			pos.write(packet[i]);	
		}
		
		if (wasEmpty) {
			// critical: notify XBee that data is available
			synchronized(this) {
				log.debug("Notifying any XBee input stream thread that new data is available");
				this.notify();
			}			
		}
	}
	
	public void close() {
		// nothing to do here
	}

	public InputStream getInputStream() {
		return this.pis;
	}

	public OutputStream getOutputStream() {
		return this.out;
	}
}
