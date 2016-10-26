package com.fyp2099.app;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;
import android.view.View.OnClickListener;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

import org.w3c.dom.Text;


public class Main extends AppCompatActivity {

    GoogleMap mMap;

	public static FragmentManager FM;
	public static MapFragmentClass MF;
	public static CameraFragment CF;
	public static TextView TV;

	EditText et_ip0;
	EditText et_ip1;
	EditText et_ip2;
	EditText et_ip3;
	private TextView speedLabel;
	private TextView headingLabel;
	private Button emergencyStopButton;
	private Button stopEngineButton;
	private Button debug1;
	private Button debug2;
	private Button joystickButton;
	private Button clearAllButton;
	private Button generateButton;
	private Button beginButton;
	private Button resetGPSButton;
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

	private boolean isNavigating = false;

	public TCPConnection conn;
	private AsyncTask loopTask;
	private AsyncTask connectTask;
	private ArrayList<String> arrayList;
	private SharedPreferences prefs;
	private SharedPreferences.Editor prefsEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	    //create layout for activity
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
	    setSupportActionBar(toolbar);


	    getUiHandles();
	    setEventListeners();


		// add mapfragment to the layout
	    FM = getFragmentManager();
	    MF = new MapFragmentClass();
	    MF.setMain(this);
	    FragmentTransaction FT = FM.beginTransaction();
	    FT.add(R.id.maplayout, MF, "bats");
	    FT.commit();


		/*
		// add camerafragment to the layout
	    FM = getFragmentManager();
	    CF = new CameraFragment();
	    FragmentTransaction FT = FM.beginTransaction();
	    FT.add(R.id.maplayout, CF, "bats");
	    FT.commit();
		*/

	    // general init
	    Init();


	    //obtain preferences file, and set preferences
	    prefs = getPreferences(Context.MODE_PRIVATE);
	    prefsEditor = prefs.edit();


	    int a = prefs.getInt(getString(R.string.ip0), 255);
	    int b = prefs.getInt(getString(R.string.ip1), 255);
	    int c = prefs.getInt(getString(R.string.ip2), 255);
	    int d = prefs.getInt(getString(R.string.ip3), 255);
	    et_ip0.setText(a + "");
	    et_ip1.setText(b + "");
	    et_ip2.setText(c + "");
	    et_ip3.setText(d + "");

	    // check the opening scenario: ie app click, open kml file, etc
	    Intent intent = getIntent();
		String action = intent.getAction();
	    String type = intent.getType();




		/*

	    if (Intent.ACTION_SEND.equals(action) && type != null) {

	    }

	    if(Intent.ACTION_VIEW.equals(action) && type != null) {

		    if ("application/vnd.google-earth.kml+xml".equals(type)) {
			    Uri fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			    appendLog("loading KML...");

			    try {
				    InputStream kmlInputStream = getContentResolver().openInputStream(fileUri);
				    //KmlLayer layer = new KmlLayer(mMap, kmlInputStream, c);
				    //layer.addLayerToMap();
			    }
			    catch(FileNotFoundException e) {
				    // do nothing
			    }


			    MF.loadKML(intent, getApplicationContext()); // Handle text being sent
		    } else {
			    // why did i even get sent this??!
		    }
	    }
		*/
    }

	private void getUiHandles() {
		IB0 = (ImageButton)findViewById(R.id.imageButton);
		IB1 = (ImageButton)findViewById(R.id.imageButton2);
		IB2 = (ImageButton)findViewById(R.id.imageButton3);
		VF = (ViewFlipper)findViewById(R.id.viewFlipper);

		emergencyStopButton = (Button)findViewById(R.id.emergencyStop);

		speedLabel = (TextView)findViewById(R.id.speedLabel);
		headingLabel = (TextView)findViewById(R.id.headingLabel);
		stopEngineButton = (Button)findViewById(R.id.stopEngineButton);
		joystickButton = (Button)findViewById(R.id.joystickButton);
		joystick = (Joystick)findViewById(R.id.joystick);

		clearAllButton = (Button)findViewById(R.id.clearPathsAndZones);
		generateButton = (Button)findViewById(R.id.generateRoute);
		beginButton = (Button)findViewById(R.id.beginRoute);
		zoneMode = (ToggleButton)findViewById(R.id.toggleZone);
		pathMode = (ToggleButton)findViewById(R.id.togglePath);
		mapType = (ToggleButton)findViewById(R.id.mapTypeButton);

		connectToggle = (ToggleButton)findViewById(R.id.connectToggle);
		et_ip0 = (EditText) findViewById(R.id.ip1);
		et_ip1 = (EditText) findViewById(R.id.ip2);
		et_ip2 = (EditText) findViewById(R.id.ip3);
		et_ip3 = (EditText) findViewById(R.id.ip4);

		resetGPSButton = (Button)findViewById(R.id.rgpspos);
		debug1 = (Button)findViewById(R.id.debugButton1);
		debug2 = (Button)findViewById(R.id.debugButton2);
		TV = (TextView)findViewById(R.id.textView);
		TV.setMovementMethod(new ScrollingMovementMethod());

	}

	private void setEventListeners() {
		IB0.setOnClickListener(button_listener);
		IB1.setOnClickListener(button_listener);
		IB2.setOnClickListener(button_listener);

		debug1.setOnClickListener(button_listener);
		debug2.setOnClickListener(button_listener);
		emergencyStopButton.setOnClickListener(button_listener);
		stopEngineButton.setOnClickListener(button_listener);
		resetGPSButton.setOnClickListener(button_listener);

		clearAllButton.setOnClickListener(button_listener);
		generateButton.setOnClickListener(button_listener);
		beginButton.setOnClickListener(button_listener);
		zoneMode.setOnClickListener(button_listener);
		pathMode.setOnClickListener(button_listener);
		mapType.setOnClickListener(button_listener);

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
	}

	private void Init() {
		// network code
		conn = new TCPConnection(this);

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

					p = new Packet(PacketID.ID_MANUALCONTROL_ON);
					conn.QueueSend(p);
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
				case R.id.clearPathsAndZones:
					MF.clearAll();
					p = new Packet(PacketID.ID_CLEAR_NAV_POINTS);
					conn.QueueSend(p);
					break;
				case R.id.generateRoute:
					for(Polyline poly : MF.paths) {
						p = new Packet(PacketID.ID_NAV_PATH);
						List<LatLng> points = poly.getPoints();
						double[] data = new double[points.size()*2];
						int offset = 0;
						for(LatLng ll : points) {
							data[offset++] = ll.latitude;
							data[offset++] = ll.longitude;
						}
						p.setData(data);
						conn.QueueSend(p);
					}
					for(Zone poly : MF.zones) {
						p = new Packet(PacketID.ID_NAV_ZONE);
						List<LatLng> points = poly.getPoints();
						double[] data = new double[points.size()*2];
						int offset = 0;
						for(LatLng ll : points) {
							data[offset++] = ll.latitude;
							data[offset++] = ll.longitude;
						}
						p.setData(data);
						conn.QueueSend(p);
					}
					p = new Packet(PacketID.ID_NAV_GENERATE);
					conn.QueueSend(p);

					break;
				case R.id.beginRoute:
					if(isNavigating) {
						beginButton.setText("Begin Scanning");
						generateButton.setEnabled(true);
						p = new Packet(PacketID.ID_AUTO_NAV_OFF);
						conn.QueueSend(p);
					} else {
						beginButton.setText("Pause Scanning");
						generateButton.setEnabled(false);
						p = new Packet(PacketID.ID_AUTO_NAV_ON);
						conn.QueueSend(p);
					}
					break;
				case R.id.joystickButton:
					p = new Packet(PacketID.ID_BRAKE);
					conn.QueueSend(p);
					break;
				case R.id.rgpspos: {
					p = new Packet(PacketID.ID_QUAD_POSITION);
					//send the position data
					conn.QueueSend(p);
					break;
				}
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
					case ID_QUAD_SPEED:
						//speedLabel.setText(getResources().getString(R.string.speed_label) + " " + p.getData(0));
						speedLabel.setText(getResources().getString(R.string.speed_label)
								+ String.format(" %-4.1f km/h", p.getData(0)*3.6 ));
						break;
					case ID_QUAD_HEADING:
						headingLabel.setText(getResources().getString(R.string.heading_label)
								+ String.format(" %-4.1f degrees", p.getData(0)/(Math.PI/180) ));
						break;
					case ID_QUAD_POSITION:
						//MF.updateQuadPosition(p.getData(0), p.getData(1));
						break;
					default:
						appendLog("unknown packet received\n");
						break;
				}
			}
		});
	}

	private String getIP() {
		//connect
		String ipAddr;
		String fail = "0.0.0.0";

		int a = Integer.parseInt(et_ip0.getText().toString());
		int b = Integer.parseInt(et_ip1.getText().toString());
		int c = Integer.parseInt(et_ip2.getText().toString());
		int d = Integer.parseInt(et_ip3.getText().toString());

		if (    a > 254 || a < 0
				|| b > 254 || b < 0
				|| c > 254 || c < 0
				|| d > 254 || d < 0 ) {
			appendLog("Invalid IP address.\n");
			return fail;
		}

		prefsEditor.putInt(getString(R.string.ip0), a);
		prefsEditor.putInt(getString(R.string.ip1), b);
		prefsEditor.putInt(getString(R.string.ip2), c);
		prefsEditor.putInt(getString(R.string.ip3), d);
		prefsEditor.commit();

		ipAddr = a + "." + b + "." + c + "." + d;

		return ipAddr;
	}

	public class tcpLoopTask extends AsyncTask<Void,Void,Boolean> {
		@Override
		protected Boolean doInBackground(Void... d) {
			return conn.connectionLoop();
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
			return conn.Connect(message[0]);
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











































/*
for(Polygon poly : MF.zones) {
							p = new Packet(PacketID.ID_NAV_ZONE);
							List<LatLng> points = poly.getPoints();
							float[] data = new float[points.size()*4];
							int offset = 0;
							for(LatLng ll : points) {
								long bits = Double.doubleToLongBits(ll.latitude);
								int bitsUpper, bitsLower;
								float f1, f2;

								bitsLower = (int)(bits & 0xFFFFFFFF);
								bitsUpper = (int)((bits >> 32) & 0xFFFFFFFF);
								f1 = Float.intBitsToFloat(bitsLower);
								f2 = Float.intBitsToFloat(bitsUpper);
								//f2 = Float.intBitsToFloat(0x40590010);
								//f1 = Float.intBitsToFloat(0x6680A3BB);

								data[offset++] = (float)f1;
								data[offset++] = (float)f2;

								bits = Double.doubleToLongBits(ll.longitude);
								bitsLower = (int)(bits & 0xFFFFFFFF);
								bitsUpper = (int)((bits >> 32) & 0xFFFFFFFF);
								f1 = Float.intBitsToFloat(bitsLower);
								f2 = Float.intBitsToFloat(bitsUpper);
								//f2 = Float.intBitsToFloat(0x40590010);
								//f1 = Float.intBitsToFloat(0x6680A3BB);

								data[offset++] = (float)f1;
								data[offset++] = (float)f2;
							}
							p.setData(data);
							conn.QueueSend(p);
						}
 */