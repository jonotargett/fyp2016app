package com.fyp2099.app;


import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class Main extends AppCompatActivity {

    GoogleMap mMap;
	public static FragmentManager FM;
	public static MapFragmentClass MF;
	ToggleButton boundaryMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
	    setSupportActionBar(toolbar);

	    FM = getFragmentManager();
	    FragmentTransaction FT = FM.beginTransaction();

	    MF = new MapFragmentClass();

	    FT.add(R.id.maplayout, MF, "bats");

	    FT.commit();


	    boundaryMode = (ToggleButton)findViewById(R.id.toggleButton);

	    boundaryMode.setOnClickListener(new View.OnClickListener() {
		    @Override
		    public void onClick(View v) {
			    if(boundaryMode.isChecked()) {
				    MF.setBoundaryLayingState(true);
			    } else {
				    MF.setBoundaryLayingState(false);
			    }
		    }
	    });

    }



}
