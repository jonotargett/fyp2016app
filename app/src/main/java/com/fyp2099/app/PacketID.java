package com.fyp2099.app;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jono on 15/08/2016.
 */
public enum PacketID {
	ID_NULL(0x00),
	ID_SOH(0x01),
	ID_EOT(0x04),
	ID_IDLE(0x16),
	ID_ETB(0x17),
	ID_CANCEL(0x18),

	ID_DEBUG(0x40),
	ID_SHOW_FD(0x41),
	ID_SHOW_VP(0x42),

	ID_QUAD_POSITION(0x50),
	ID_QUAD_HEADING(0x51),
	ID_QUAD_SPEED(0x52),
	ID_REQ_QUAD_POSITION(0x53),
	ID_REQ_QUAD_HEADING(0x54),
	ID_REQ_QUAD_SPEED(0x55),

	ID_CLEAR_NAV_POINTS(0x60),
	ID_NAV_POINTS(0x61),
	ID_NAV_PATH(0x62),
	ID_NAV_ZONE(0x63),

	ID_STOP_ENGINE(0x80),
	ID_HANDBRAKE_ON(0x81),
	ID_HANDBRAKE_OFF(0x82),
	ID_MANUALCONTROL_ON(0x83),
	ID_MANUALJOYSTICK(0x84),
	ID_BRAKE(0x85),
	ID_JOYSTICK_HELD(0x86),
	ID_JOYSTICK_RELEASED(0x87),

	ID_EMERGENCY_STOP(0xFF);

	private int packetID;
	private static Map map = new HashMap<>();

	private PacketID(int ID) {
		packetID = ID;
	}

	static {
		for(PacketID p : PacketID.values()) {
			map.put(p.packetID, p);
		}
	}

	public static PacketID valueOf(int p) {
		return (PacketID)map.get(p);
	}

	public byte toByte() {
		return (byte)packetID;
	}
};
