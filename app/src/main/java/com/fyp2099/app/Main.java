package com.fyp2099.app;


import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Text;

public class Main extends AppCompatActivity {

    GoogleMap mMap;
	public static FragmentManager FM;
	public static MapFragmentClass MF;
	public static TextView TV;
	ToggleButton boundaryMode;
	public TCPConnection conn;
	private ArrayList<String> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

	    conn = new TCPConnection(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
	    setSupportActionBar(toolbar);

	    FM = getFragmentManager();
	    FragmentTransaction FT = FM.beginTransaction();

	    MF = new MapFragmentClass();

	    FT.add(R.id.maplayout, MF, "bats");

	    FT.commit();

		TV = (TextView)findViewById(R.id.textView);
	    boundaryMode = (ToggleButton)findViewById(R.id.toggleButton);

	    boundaryMode.setOnClickListener(new View.OnClickListener() {
		    @Override
		    public void onClick(View v) {
			    if (boundaryMode.isChecked()) {
				    MF.setBoundaryLayingState(true);
			    } else {
				    MF.setBoundaryLayingState(false);
			    }
		    }
	    });


	    // network code
		new connectTask().execute("");
    }

	public void appendLog(final char v) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView tv = Main.TV;
				String s = new String("" + v);

				tv.append(s);
			}
		});
	}

	public class connectTask extends AsyncTask<String,String,TCPConnection> {

		@Override
		protected TCPConnection doInBackground(String... message) {

			//we create a TCPClient object and
			conn.Connect();

			if(conn.connected) {
				while(true) {
					conn.Respond();
				}
			}

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
