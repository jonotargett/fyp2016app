package com.fyp2099.app;

import android.util.Log;

import java.util.Arrays;

/**
 * Created by Jono on 15/08/2016.
 */
public class Packet {
	private PacketID packetId;
	private int length;
	private float[] data;

	Packet() {
		packetId = PacketID.ID_NULL;
		length = 0;
		data = new float[0];
	}

	Packet(PacketID ID) {
		packetId = ID;
		length = 0;
		data = new float[0];
	}

	boolean setID(PacketID ID) {
		packetId = ID;
		return true;
	}

	boolean setData(float[] newData) {

		int l = newData.length;
		if(l == 0 || l > 62) {
			Log.e("PACKET", "Dataset is too long");
			return false;
		}

		length = l;
		data = Arrays.copyOf(newData, l);
		//System.arraycopy(newData, 0, data, 0, length);
		return true;
	}

	int getLength() {
		return length;
	}

	PacketID getID() {
		return packetId;
	}

	int getByteLength() {
		return (4 + length * 4);
	}

	byte[] toBytes() {
		byte[] ret = new byte[getByteLength()];

		ret[0] = 0x01;                  // ID_SOH
		ret[1] = (byte)packetId.toByte();
		ret[2] = (byte)length;

		int offset = 2;

		for(int i=0; i<length; i++) {
			float f = data[i];

			int bits = Float.floatToIntBits(f);
			ret[++offset] = (byte)(bits & 0xFF);
			ret[++offset] = (byte)((bits >> 8) & 0xFF);
			ret[++offset] = (byte)((bits >> 16) & 0xFF);
			ret[++offset] = (byte)((bits >> 24) & 0xFF);
		}

		ret[++offset] = 0x17;           // ID_ETB

		return ret;
	}


}
