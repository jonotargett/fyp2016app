package com.fyp2099.app;

import android.renderscript.ScriptGroup;
import android.util.Log;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.AbstractQueue;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;


/**
 * Created by Jono on 12/05/2016.
 */
public class TCPConnection {

	String serverMessage;
	public boolean connected = false;

	public static final String SERVERIP = "192.168.1.168"; //your computer IP address
	//public static final String SERVERIP = "129.127.230.131";
	public static final int SERVERPORT = 2099;
	public static final int SLEEP_DURATION = 20;    // milliseconds?
	public static final int RECONNECT_TIMEOUT = 1000;
	public static final int DISCONNECT_TIMEOUT = 6000; // also milliseocnds
	public static final int DATA_UPDATE_PERIOD = 500;

	public InetAddress serverAddr;
	public Socket socket;

	//private BufferedReader in;
	//private BufferedWriter out;
	private InputStream in;
	private OutputStream out;
	byte[] buffer;
	int offset;
	boolean collectingPacket;

	public Queue<Packet> outgoingPackets;

	private long t1;
	private long t2;
	private long tLastQuadReq;

	private Main m;

	public TCPConnection(Main main) {
		m = main;
		buffer = new byte[1024];
		offset = 0;
		collectingPacket = false;
		outgoingPackets = new LinkedList<>();
	}

	public boolean Connect(final String ipAddr) {
		Log.i("NETWORK", "connecting...");

		//byte[] ipAddr = new byte[]{b0, b1, b2, b3};

		try	{
			//serverAddr = InetAddress.getByName(SERVERIP);
			serverAddr = InetAddress.getByName(ipAddr);
			//socket = new Socket(serverAddr, SERVERPORT);
			socket = new Socket();
			socket.connect(new InetSocketAddress(serverAddr, SERVERPORT), 3000);

			//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			in = socket.getInputStream();
			out = socket.getOutputStream();


			//THIS PART IS VERY MUCH REQUIRED. FIRST SENT NEEDS TO BE SYN_ACK OR WILL BE
			// REJECTED, EITHER BY BAD ACCEPT OR TIMEOUT
			//out.write((byte)'\16');
			//out.flush();
			Packet p = new Packet(PacketID.ID_IDLE);
			QueueSend(p);
		}
		catch(SocketTimeoutException e) {
			Log.i("NETWORK", "could not resolve host");
			m.appendLog("Connected timeout occurred\n");
			return false;
		}
		catch(UnknownHostException e) {
			//do nothing
			Log.i("NETWORK", "could not resolve host");
			m.appendLog("Could not resolve host\n");
			return false;
		}
		catch(IOException e) {
			//also do nothing
			Log.i("NETWORK", "could not connect to socket");
			m.appendLog("Could not connect to socket\n");
			return false;
		}


		connected = true;

		t1 = SystemClock.elapsedRealtime();
		tLastQuadReq = t1;
		return true;
		//connectionLoop();
	}

	public boolean connectionLoop() {
		while(connected) {
			t2 = SystemClock.elapsedRealtime();

			//check for incoming/outgoing messages -----------------------------------------------
			boolean r = Receive();
			if(!r) {
				if(t2 > (t1 + RECONNECT_TIMEOUT)) {        // havent received anything for a while
					try {
						out.write((byte) '\16');
						out.flush();
					}
					catch(IOException e) {
						// do nothing
					}
				}
				if(t2 > (t1 + DISCONNECT_TIMEOUT)) {       //havent received anything for too long
					connected = false;
					break;
				}
			} else {
				t1 = SystemClock.elapsedRealtime();
			}


			// send outgoing messages ------------------------------------------------------------
			if(t2 > (tLastQuadReq + DATA_UPDATE_PERIOD)) {
				Packet p1 = new Packet(PacketID.ID_REQ_QUAD_SPEED);
				Packet p2 = new Packet(PacketID.ID_REQ_QUAD_HEADING);
				Packet p3 = new Packet(PacketID.ID_REQ_QUAD_POSITION);

				QueueSend(p1);
				QueueSend(p2);
				QueueSend(p3);

				tLastQuadReq = t2;
			}

			while(outgoingPackets.peek() != null) {
				Send(outgoingPackets.poll());
				try {
					Thread.sleep(0, 100);
				} catch(InterruptedException e) {
					// do nothing?
				}
			}




			// sleep for a bit to stop the thread from going crazy -------------------------------
			try {
				Thread.sleep(0, 10);                   //0ms, 10 nanoseconds of sleep.
			} catch(InterruptedException e) {
				// do nothing?
			}
		}

		//no longer has connected flag set: try disconnecting cleanly
		Disconnect();

		return false;
	}

	public void Disconnect() {
		try {
			socket.close();
			in.close();
			out.close();
			connected = false;
		} catch(IOException e) {
			// do nothing. i dont care if i cant close this socket.
		}
	}

	public void QueueSend(Packet p) {
		if(connected) {
			outgoingPackets.offer(p);
		}
	}

	private void Send(Packet p) {
		if(!connected) {
			m.appendLog("No connection!\n");
			return;
		}
		//m.appendLog("Attempting to send...\n");

		byte[] buf = p.toBytes();

		try {
			out.write(buf, 0, p.getByteLength());
			out.flush();
		} catch(IOException e) {
			Log.e("packet send ERROR", e.getMessage());
			m.appendLog(e.getMessage() + "\n");
		}
	}


	private boolean Receive() {
		byte[] buf = new byte[1];
		boolean wasReady = false;

		try {

			while(in.available() > 0) {
				wasReady = true;
				in.read(buf, 0, 1);       //read a single character

				if(collectingPacket) {
					//spuds
					if(buf[0] == 0x17) {
						collectingPacket = false;
						processPacket();
						break;
					} else {
						buffer[offset++] = buf[0];
					}
				}
				else if(buf[0] == 0x01) {      // start of a new transmission
					//m.appendLog("start of a packet received...\n");
					collectingPacket = true;
				}
				else if(buf[0] == 0x16) {     // syn_ack
					//buf[0] = 0x16;          //SYN: synchronous ack for maintaining connection when no data is passed
					//out.write(buf, 0, 1);
					//out.flush();
				}
			}

			if(wasReady) {
				return true;
			}

		}catch(IOException e) {
			m.appendLog("ERROR: " + e.getMessage());
			return false;
		}
		return false;
	}

	private void processPacket() {
		byte id = buffer[0];
		int len = buffer[1];
		int o = 2;

		Packet p = new Packet();
		p.setID(PacketID.valueOf(id));
		float f[] = new float[len];
		int fo = 0;

		if(offset != len*4+2) {
			m.appendLog("Invalid packet received!\n");

			offset = 0;
			return;
		}
		//m.appendLog("full packet being processed...\n");
		//m.appendLog("Length " + len + " floats\n");
		//m.appendLog(":: " + (len*4+2) + " / " + offset +"\n");


		while(o < offset-1) {
			byte b0 = (byte)buffer[o++];
			byte b1 = (byte)buffer[o++];
			byte b2 = (byte)buffer[o++];
			byte b3 = (byte)buffer[o++];


			int asInt = (b0 & 0xFF)
					| ((b1 & 0xFF) << 8)
					| ((b2 & 0xFF) << 16)
					| ((b3 & 0xFF) << 24);

			float nf = Float.intBitsToFloat(asInt);
			f[fo] = nf;
			fo++;
		}


		offset = 0;
		p.setData(f);

		if(id == 0x16) {        // ditch early, this is just a syn_ack
			Send(p);            // bounce it right back

			/*
			byte[] buf = new byte[1];
			buf[0] = 0x16;          //SYN: synchronous ack for maintaining connection when no data is passed
			try {
				out.write(buf, 0, 1);
				out.flush();
			} catch(IOException e){
				//nothing
			}
			*/

			return;
		}

		// a legitimate info packet
		// let the ui thread handle this
		m.handlePacket(p);

	}
}
