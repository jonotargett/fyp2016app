package com.fyp2099.app;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;
import android.view.View.OnClickListener;


import java.util.ArrayList;

import com.google.android.gms.maps.GoogleMap;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;


public class Main extends AppCompatActivity {

    GoogleMap mMap;

	public static FragmentManager FM;
	public static MapFragmentClass MF;
	public static TextView TV;

	private Button emergencyStopButton;
	private Button stopEngineButton;
	private Button debug1;
	private Button debug2;
	private Button joystickButton;
	public ToggleButton zoneMode;
	public ToggleButton pathMode;
	private ToggleButton connectToggle;
	private ToggleButton mapType;
	private ImageButton IB0;
	private ImageButton IB1;
	private ImageButton IB2;
	private Joystick joystick;
	private ViewFlipper VF;

	private static final int JOYSTICK_INTERVAL = 25;
	private long lastJoystickUpdate;
	private long current;

	public TCPConnection conn;
	private AsyncTask loopTask;
	private AsyncTask connectTask;
	private ArrayList<String> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
	    setSupportActionBar(toolbar);

	    FM = getFragmentManager();
	    MF = new MapFragmentClass();
	    MF.setMain(this);
	    FragmentTransaction FT = FM.beginTransaction();
	    FT.add(R.id.maplayout, MF, "bats");
	    FT.commit();

	    // network code
	    conn = new TCPConnection(this);

		TV = (TextView)findViewById(R.id.textView);
	    TV.setMovementMethod(new ScrollingMovementMethod());

	    zoneMode = (ToggleButton)findViewById(R.id.toggleZone);
	    pathMode = (ToggleButton)findViewById(R.id.togglePath);
	    connectToggle = (ToggleButton)findViewById(R.id.connectToggle);
	    mapType = (ToggleButton)findViewById(R.id.mapTypeButton);
	    VF = (ViewFlipper)findViewById(R.id.viewFlipper);

	    IB0 = (ImageButton)findViewById(R.id.imageButton);
	    IB1 = (ImageButton)findViewById(R.id.imageButton2);
	    IB2 = (ImageButton)findViewById(R.id.imageButton3);
	    stopEngineButton = (Button)findViewById(R.id.stopEngineButton);
	    debug1 = (Button)findViewById(R.id.debugButton1);
	    debug2 = (Button)findViewById(R.id.debugButton2);
		emergencyStopButton = (Button)findViewById(R.id.emergencyStop);
		joystickButton = (Button)findViewById(R.id.joystickButton);
	    joystick = (Joystick)findViewById(R.id.joystick);


	    IB0.setOnClickListener(button_listener);
	    IB1.setOnClickListener(button_listener);
	    IB2.setOnClickListener(button_listener);
	    stopEngineButton.setOnClickListener(button_listener);
	    zoneMode.setOnClickListener(button_listener);
	    pathMode.setOnClickListener(button_listener);
	    mapType.setOnClickListener(button_listener);
	    debug1.setOnClickListener(button_listener);
	    debug2.setOnClickListener(button_listener);
	    emergencyStopButton.setOnClickListener(button_listener);
	    joystickButton.setOnClickListener(button_listener);

	    joystick.setJoystickListener(new JoystickListener() {
		    @Override
		    public void onDown() {
			    Packet p = new Packet(PacketID.ID_JOYSTICK_HELD);
			    conn.QueueSend(p);
		    }

		    @Override
		    public void onDrag(float degrees, float offset) {
			    current = SystemClock.elapsedRealtime();
			    if (current > lastJoystickUpdate + JOYSTICK_INTERVAL) {
				    lastJoystickUpdate = current;

				    Packet p = new Packet(PacketID.ID_MANUALJOYSTICK);
				    float[] f = new float[2];
				    f[0] = degrees;
				    f[1] = offset;
				    p.setData(f);
				    conn.QueueSend(p);
			    }
		    }

		    @Override
		    public void onUp() {
			    Packet p = new Packet(PacketID.ID_JOYSTICK_RELEASED);
			    conn.QueueSend(p);
		    }
	    });


	    connectToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

			    if (!isChecked) {     //the togglebutton is checked, so it should be connected
				    conn.connected = false;
				    connectToggle.setTextOff(getResources().getString(R.string.connect_toggle_off));
				    connectToggle.setTextOn(getResources().getString(R.string.connect_toggle_progress));
			    } else {
				    if (conn.connected) {
					    appendLog("ERROR: already connected?");
					    return;
				    }
				    String ipAddr = getIP();
				    appendLog("Connecting to " + ipAddr + "\n");
				    connectTask = new connectTask().execute(ipAddr);
			    }
		    }
	    });

		mapType.setChecked(true);
		lastJoystickUpdate = SystemClock.elapsedRealtime();

		appendLog("views initialised...\n");

    }



	private OnClickListener button_listener = new OnClickListener() {
		public void onClick(View v) {
			Packet p;

			switch(v.getId()) {
				case R.id.imageButton:
					VF.setDisplayedChild(0);
					IB0.setBackground(getResources().getDrawable(R.drawable.menu_button_selected));
					IB1.setBackground(getResources().getDrawable(R.drawable.menu_button));
					IB2.setBackground(getResources().getDrawable(R.drawable.menu_button));
					break;
				case R.id.imageButton2:
					VF.setDisplayedChild(1);
					IB0.setBackground(getResources().getDrawable(R.drawable.menu_button));
					IB1.setBackground(getResources().getDrawable(R.drawable.menu_button_selected));
					IB2.setBackground(getResources().getDrawable(R.drawable.menu_button));
					break;
				case R.id.imageButton3:
					VF.setDisplayedChild(2);
					IB0.setBackground(getResources().getDrawable(R.drawable.menu_button));
					IB1.setBackground(getResources().getDrawable(R.drawable.menu_button));
					IB2.setBackground(getResources().getDrawable(R.drawable.menu_button_selected));
					break;
				case R.id.emergencyStop:
					p = new Packet(PacketID.ID_EMERGENCY_STOP);
					conn.QueueSend(p);
					appendLog("E-STOP pressed!\n");
					break;
				case R.id.stopEngineButton:
					p = new Packet(PacketID.ID_STOP_ENGINE);
					conn.QueueSend(p);
					break;
				case R.id.toggleZone:
					if (zoneMode.isChecked()) {
						MF.setZoneLayingState(true);
					} else {
						MF.setZoneLayingState(false);
					}
					break;
				case R.id.togglePath:
					appendLog("yrs, actually been pressed\n");
					if (pathMode.isChecked()) {
						MF.setPathLayingState(true);
					} else {
						MF.setPathLayingState(false);
					}
					break;
				case R.id.mapTypeButton:
					if (mapType.isChecked()) {
						MF.setMapType(GoogleMap.MAP_TYPE_NORMAL);
					} else {
						MF.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
					}
					break;
				case R.id.debugButton1:
					p = new Packet(PacketID.ID_DEBUG);
					conn.QueueSend(p);
					break;
				case R.id.debugButton2:
					p = new Packet(PacketID.ID_CLEAR_NAV_POINTS);
					conn.QueueSend(p);
					break;
				case R.id.joystickButton:
					p = new Packet(PacketID.ID_BRAKE);
					conn.QueueSend(p);
					break;
				default:
					VF.setDisplayedChild(0);
			}
		}
	};

	public void appendLog(final String v) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView tv = Main.TV;
				//String s = new String("" + v);

				tv.append(v);
			}
		});
	}

	public void handlePacket(final Packet p) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				switch(p.getID()) {
					case ID_IDLE:
						break;
					default:
						appendLog("unknown packet received");
						break;
				}
			}
		});
	}

	private String getIP() {
		//connect
		String ipAddr = "";
		String fail = "0.0.0.0";

		EditText et = (EditText) findViewById(R.id.ip1);
		int b = Integer.parseInt(et.getText().toString());
		if (b > 254 || b < 0) {
			appendLog("Invalid IP address.\n");
			return fail;
		}
		ipAddr += b + ".";

		et = (EditText) findViewById(R.id.ip2);
		b = Integer.parseInt(et.getText().toString());
		if (b > 254 || b < 0) {
			appendLog("Invalid IP address.\n");
			return fail;
		}
		ipAddr += b + ".";

		et = (EditText) findViewById(R.id.ip3);
		b = Integer.parseInt(et.getText().toString());
		if (b > 254 || b < 0) {
			appendLog("Invalid IP address.\n");
			return fail;
		}
		ipAddr += b + ".";

		et = (EditText) findViewById(R.id.ip4);
		b = Integer.parseInt(et.getText().toString());
		if (b > 254 || b < 0) {
			appendLog("Invalid IP address.\n");
			return fail;
		}
		ipAddr += b;

		return ipAddr;
	}

	public class tcpLoopTask extends AsyncTask<Void,Void,Boolean> {
		@Override
		protected Boolean doInBackground(Void... d) {
			boolean result = conn.connectionLoop();
			return result;
		}
		@Override
		protected void onPostExecute(Boolean connected) {
			if(connected) {
				// this shouldnt ever happen. I don't know what circumstances could lead to this
			} else {
				connectToggle.setTextOff(getResources().getString(R.string.connect_toggle_off));
				connectToggle.setTextOn(getResources().getString(R.string.connect_toggle_progress));
				connectToggle.setChecked(false);
				appendLog("Connection lost.\n");
			}
		}
	}

	public class connectTask extends AsyncTask<String,String,Boolean> {
		@Override
		protected Boolean doInBackground(String... message) {
			boolean result = conn.Connect(message[0]);
			return result;
		}

		@Override
		protected void onPostExecute(Boolean connected) {
			if(connected) {
				loopTask = new tcpLoopTask().execute();
				connectToggle.setTextOff(getResources().getString(R.string.connect_toggle_off));
				connectToggle.setTextOn(getResources().getString(R.string.connect_toggle_on));
				connectToggle.setChecked(true);
				appendLog("Connected!\n");
			} else {
				connectToggle.setTextOff(getResources().getString(R.string.connect_toggle_off));
				connectToggle.setTextOn(getResources().getString(R.string.connect_toggle_progress));
				connectToggle.setChecked(false);
				appendLog("Could not connect.\n");
			}
		}
	}

}
