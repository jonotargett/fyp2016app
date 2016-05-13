package com.fyp2099.app;

import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Jono on 12/05/2016.
 */
public class TCPConnection {

	String serverMessage;
	public boolean connected = false;

	public static final String SERVERIP = "192.168.1.171"; //your computer IP address
	public static final int SERVERPORT = 2099;

	public InetAddress serverAddr;
	public Socket socket;

	private BufferedReader in;
	private BufferedWriter out;

	private Main m;

	public TCPConnection(Main main) {
		m = main;
	}

	public void Connect() {
		Log.i("NETWORK", "connecting...");

		try	{
			serverAddr = InetAddress.getByName(SERVERIP);
			socket = new Socket(serverAddr, SERVERPORT);

			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		}
		catch(UnknownHostException e) {
			//do nothing
			Log.i("NETWORK", "could not resolve host");
			return;
		}

		catch(IOException e) {
			//also do nothing
			Log.i("NETWORK", "could not connect to socket");
			return;
		}

		connected = true;
	}

	public void Respond() {
		char[] buf = new char[1];

		try {
			in.read(buf, 0, 1);
			m.appendLog(buf[0]);

			buf[0] = 0x16;          //SYN: synchronous ack for maintaining connection when no data is passed
			out.write(buf, 0, 1);
			out.flush();
		}catch(IOException e) {
			//do nothing
		}
	}
}
