package com.fyp2099.app;

import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashSet;


public class MapFragmentClass extends Fragment implements OnMapReadyCallback,
		GoogleMap.OnMapLongClickListener,
		GoogleMap.OnMapClickListener,
		GoogleMap.OnPolygonClickListener,
		GoogleMap.OnMarkerClickListener {

	private GoogleMap mMap;
	private boolean boundaryLayingState;
	private PolygonOptions zone;
	private Polygon poly;

	private HashSet<Polygon> polygons;
	private HashSet<Marker> markers;


	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.map_fragment_layout, container, false);

		init();

		return v;


	}

	private void init() {
		boundaryLayingState = false;

		markers = new HashSet<Marker>();
		polygons = new HashSet<Polygon>();

	}

	public void setBoundaryLayingState(boolean setunset) {
		if(boundaryLayingState == setunset) {
			//do nothing
			return;
		}

		if(setunset) {
			boundaryLayingState = true;
			zone = new PolygonOptions()
					.fillColor(getResources().getColor(R.color.map_zone_fill))
					.strokeColor(getResources().getColor(R.color.map_zone_outline))
					.strokeWidth(getResources().getDimension(R.dimen.map_stroke_width))
					.clickable(true);
		} else {
			addPolygonPoint(new LatLng(0, 0), true);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {


		MapFragment mapFragment;

		// API 23 works this way
		//mapFragment = (MapFragment)Main.FM.findFragmentByTag("bats").getChildFragmentManager().findFragmentById(R.id.mapid);

		// API 19 works this way
		mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.mapid);
		mapFragment.getMapAsync(this);

	}

	@Override
	public void onMapClick(LatLng ll) {
		if(boundaryLayingState) {
			addPolygonPoint(ll, false);
		}
	}

	@Override
	public void onMapLongClick(LatLng ll) {
		newMarker(ll);
		/*
		// apparently no-one likes the long click as a function.
		// try and keep everything as single taps

		if(boundaryLayingState) {
			addPolygonPoint(ll, false);
			addPolygonPoint(ll, true);
		} else {
			newMarker(ll);
		}
		*/
	}

	@Override
	public void onPolygonClick(Polygon p) {
		polygons.remove(p);
		p.remove();

		Log.i("Polygon Clicked", "Polygon Count: " + polygons.size());
	}

	@Override
	public boolean onMarkerClick(Marker m) {
		// this does literally nothing except 'consume' the click event
		// this prevents the default behaviour from executing, which is damn annoying
		return true;
	}

	private void newMarker(LatLng ll) {
		MarkerOptions newMarker = new MarkerOptions()
				.position(ll)
				.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher))
				.flat(false);

		Marker m = mMap.addMarker(newMarker);
		markers.add(m);
	}


	private void addPolygonPoint(LatLng ll, boolean isFinal) {
		if(isFinal) {
			polygons.add(poly);
			poly = null;
			boundaryLayingState = false;
			zone = null;
			return;
		}

		zone.add(ll);
		try {
			poly.remove();
		} catch (NullPointerException e) {
			Log.i("Null Pointer", "poly doesnt exist yet");
		}

		poly = mMap.addPolygon(zone);
	}


	public void setMapType(int type) {
		mMap.setMapType(type);
	}



	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		if(googleMap == null) {
			Log.e("pork", "oprokros");
		}

		//mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

		try {
			mMap.setMyLocationEnabled(true);
		} catch(SecurityException e) {

		}

		// Add a marker in [UNIVERSITY OF ADELAIDE] and move the camera
		LatLng central = new LatLng(-34.919, 138.603);
		mMap.addMarker(new MarkerOptions().position(central).title("University of Adelaide")).setDraggable(true);
		mMap.moveCamera(CameraUpdateFactory.newLatLng(central));
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(central, 15.0f));

		mMap.setOnMapClickListener(this);
		mMap.setOnMapLongClickListener(this);
		mMap.setOnPolygonClickListener(this);
		mMap.setOnMarkerClickListener(this);


		PolygonOptions rectOptions = new PolygonOptions()
				.add(new LatLng(-34.922142, 138.587171))
				.add(new LatLng(-34.921003, 138.610665))
				.add(new LatLng(-34.934755, 138.617857))
				.add(new LatLng(-34.936363, 138.588315))
				.fillColor(getResources().getColor(R.color.map_zone_fill))
				.strokeWidth(getResources().getDimension(R.dimen.map_stroke_width))
				.strokeColor(getResources().getColor(R.color.map_zone_outline));

// Get back the mutable Polyline
		Polygon polyline = mMap.addPolygon(rectOptions);
	}

}