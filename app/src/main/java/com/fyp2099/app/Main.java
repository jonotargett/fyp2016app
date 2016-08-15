package com.fyp2099.app;


import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;


public class Main extends AppCompatActivity {

    GoogleMap mMap;

	public static FragmentManager FM;
	public static MapFragmentClass MF;
	public static TextView TV;

	private Button stopEngineButton;
	private Button debug1;
	private Button debug2;
	private ToggleButton boundaryMode;
	private ToggleButton connectToggle;
	private ToggleButton mapType;
	private ImageButton IB0;
	private ImageButton IB1;
	private ImageButton IB2;
	private ViewFlipper VF;

	public TCPConnection conn;
	private ArrayList<String> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
	    setSupportActionBar(toolbar);

	    FM = getFragmentManager();
	    MF = new MapFragmentClass();
	    FragmentTransaction FT = FM.beginTransaction();
	    FT.add(R.id.maplayout, MF, "bats");
	    FT.commit();

	    // network code
	    conn = new TCPConnection(this);

		TV = (TextView)findViewById(R.id.textView);
	    TV.setMovementMethod(new ScrollingMovementMethod());

	    boundaryMode = (ToggleButton)findViewById(R.id.toggleButton);
	    connectToggle = (ToggleButton)findViewById(R.id.connectToggle);
	    mapType = (ToggleButton)findViewById(R.id.mapTypeButton);
	    VF = (ViewFlipper)findViewById(R.id.viewFlipper);

	    IB0 = (ImageButton)findViewById(R.id.imageButton);
	    IB1 = (ImageButton)findViewById(R.id.imageButton2);
	    IB2 = (ImageButton)findViewById(R.id.imageButton3);
	    stopEngineButton = (Button)findViewById(R.id.stopEngineButton);
	    debug1 = (Button)findViewById(R.id.debugButton1);
	    debug2 = (Button)findViewById(R.id.debugButton2);





	    mapType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			    if (isChecked) {
				    MF.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			    } else {
				    MF.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			    }
		    }
	    });

	    connectToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    @Override
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			    appendLog("toggle change event\n");
		    }
	    });

	    connectToggle.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
			    appendLog("click event\n");
		    }
	    });

	    connectToggle.setOnTouchListener(new View.OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
			    appendLog("touche event\n");
			    if (conn.connected) {
				    appendLog("already connected\n");
				    return true;
			    }

			    //connect
			    String ipAddr = "";
			    EditText et = (EditText) findViewById(R.id.ip1);
			    int b = Integer.parseInt(et.getText().toString());
			    if (b > 254 || b < 0) {
				    appendLog("Invalid IP address.\n");
				    return false;
			    }
			    ipAddr += b + ".";

			    et = (EditText) findViewById(R.id.ip2);
			    b = Integer.parseInt(et.getText().toString());
			    if (b > 254 || b < 0) {
				    appendLog("Invalid IP address.\n");
				    return false;
			    }
			    ipAddr += b + ".";

			    et = (EditText) findViewById(R.id.ip3);
			    b = Integer.parseInt(et.getText().toString());
			    if (b > 254 || b < 0) {
				    appendLog("Invalid IP address.\n");
				    return false;
			    }
			    ipAddr += b + ".";

			    et = (EditText) findViewById(R.id.ip4);
			    b = Integer.parseInt(et.getText().toString());
			    if (b > 254 || b < 0) {
				    appendLog("Invalid IP address.\n");
				    return false;
			    }
			    ipAddr += b;

			    appendLog(ipAddr + "\n");
			    new connectTask().execute(ipAddr);
			    appendLog("connect task started\n");

			    return true;
		    }
	    });

		IB0.setOnClickListener(button_listener);
	    IB1.setOnClickListener(button_listener);
	    IB2.setOnClickListener(button_listener);
	    stopEngineButton.setOnClickListener(button_listener);
	    boundaryMode.setOnClickListener(button_listener);
	    debug1.setOnClickListener(button_listener);
	    debug2.setOnClickListener(button_listener);

		appendLog("views initialised...\n");

    }

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

	private OnClickListener button_listener = new OnClickListener() {
		public void onClick(View v) {
			Packet p;

			switch(v.getId()) {
				case R.id.imageButton:
					VF.setDisplayedChild(0);
					break;
				case R.id.imageButton2:
					VF.setDisplayedChild(1);
					break;
				case R.id.imageButton3:
					VF.setDisplayedChild(2);
					break;
				case R.id.stopEngineButton:
					appendLog("stop engine pressed\n");
					p = new Packet(PacketID.ID_STOP_ENGINE);
					conn.QueueSend(p);
					break;
				case R.id.toggleButton:
					if (boundaryMode.isChecked()) {
						MF.setBoundaryLayingState(true);
					} else {
						MF.setBoundaryLayingState(false);
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
				default:
					VF.setDisplayedChild(0);
			}
		}
	};



	public class connectTask extends AsyncTask<String,String,TCPConnection> {

		@Override
		protected TCPConnection doInBackground(String... message) {

			//we create a TCPClient object and
			conn.Connect(message[0]);
			/*
			if(conn.connected) {
				while(true) {
					conn.Receive();
				}
			}
			*/
			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);

			//in the arrayList we add the messaged received from server
			arrayList.add(values[0]);
			// notify the adapter that the data set has changed. This means that new message received
			// from server was added to the list
			// --->mAdapter.notifyDataSetChanged();
		}
	}

}
