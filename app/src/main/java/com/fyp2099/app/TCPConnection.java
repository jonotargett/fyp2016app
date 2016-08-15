package com.fyp2099.app;

import android.util.Log;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
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
	public static final int KEEPALIVE_PERIOD = 500; // also milliseocnds

	public InetAddress serverAddr;
	public Socket socket;

	private BufferedReader in;
	private BufferedWriter out;
	char[] buffer;
	int offset;
	boolean collectingPacket;

	public Queue<Packet> outgoingPackets;

	private long t1;
	private long t2;

	private Main m;

	public TCPConnection(Main main) {
		m = main;
		buffer = new char[1024];
		offset = 0;
		collectingPacket = false;
		outgoingPackets = new LinkedList<>();
	}

	public void Connect(final String ipAddr) {
		Log.i("NETWORK", "connecting...");

		//byte[] ipAddr = new byte[]{b0, b1, b2, b3};

		try	{
			//serverAddr = InetAddress.getByName(SERVERIP);
			serverAddr = InetAddress.getByName(ipAddr);
			socket = new Socket(serverAddr, SERVERPORT);

			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

			//THIS PART IS VERY MUCH REQUIRED. FIRST SENT NEEDS TO BE SYN_ACK OR WILL BE
			// REJECTED, EITHER BY BAD ACCEPT OR TIMEOUT
			out.write('\16');
			out.flush();
		}
		catch(UnknownHostException e) {
			//do nothing
			Log.i("NETWORK", "could not resolve host");
			m.appendLog("Could not resolve host\n");
			return;
		}

		catch(IOException e) {
			//also do nothing
			Log.i("NETWORK", "could not connect to socket");
			m.appendLog("Could not connect to socket\n");
			return;
		}

		connected = true;

		t1 = SystemClock.elapsedRealtime();
		connectionLoop();
	}

	private void connectionLoop() {
		while(connected) {
			t2 = SystemClock.elapsedRealtime();

			// ping the quad to keep connected
			// ACTUALLY NO. THE QUAD WILL PING. WE WILL DO NOTHING.
			// JUST RESPOND TO THE QUADS PINGS. NO POINT US BOTH DOING THE TIMING
			//if(t2 > (t1 + KEEPALIVE_PERIOD)) {
			//	Receive();
			//	t1 = SystemClock.elapsedRealtime();
			//}

			//check for incoming/outgoing messages
			Receive();

			while(outgoingPackets.peek() != null) {
				Send(outgoingPackets.poll());
			}


			// sleep for a bit to stop the thread from going crazy
			try {
				Thread.sleep(0, 1000);                   //0ms, 1000 nanoseconds of sleep.
			} catch(InterruptedException e) {
				// do nothing?
			}


		}
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
		outgoingPackets.offer(p);
	}

	private void Send(Packet p) {
		if(!connected) {
			m.appendLog("No connection!\n");
			return;
		}
		m.appendLog("Attempting to send...\n");

		char[] buf = p.toBytes();



		try {
			out.write(buf, 0, p.getByteLength());
			out.flush();
		} catch(IOException e) {
			//do nothing
		}
	}


	private void Receive() {
		char[] buf = new char[1];

		try {

			while(in.ready()) {

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
					m.appendLog("start of a packet received...\n");
					collectingPacket = true;
				} else if(buf[0] == 0x16) {     // syn_ack
					buf[0] = 0x16;          //SYN: synchronous ack for maintaining connection when no data is passed
					out.write(buf, 0, 1);
					out.flush();
				}
			}

		}catch(IOException e) {
			//do nothing
		}
	}

	private void processPacket() {
		char id = buffer[0];
		int len = buffer[1];
		int o = 0;

		Packet p = new Packet();
		p.setID(PacketID.valueOf(id));
		float f[] = new float[len];
		int fo = 0;

		if(offset != len*4+3) {
			m.appendLog("Invalid packet received!\n");

			offset = 0;
			return;
		}
		m.appendLog("full packet being processed...\n");

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
			f[fo++] = nf;
		}

		offset = 0;
		p.setData(f);

		if(id == 0x16) {        // ditch early, this is just a syn_ack
			Send(p);            // bounce it right back

			char[] buf = new char[1];
			buf[0] = 0x16;          //SYN: synchronous ack for maintaining connection when no data is passed
			try {
				out.write(buf, 0, 1);
				out.flush();
			} catch(IOException e){
				//nothing
			}

			return;
		}

		// a legitimate info packet
		// let the ui thread handle this
		m.handlePacket(p);
	}
}
