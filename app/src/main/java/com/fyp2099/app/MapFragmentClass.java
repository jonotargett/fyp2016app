package com.fyp2099.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

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
import com.google.maps.android.kml.KmlLayer;


import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;


public class MapFragmentClass extends Fragment implements OnMapReadyCallback,
		GoogleMap.OnMapLongClickListener,
		GoogleMap.OnMapClickListener,
		GoogleMap.OnPolygonClickListener,
		GoogleMap.OnPolylineClickListener,
		GoogleMap.OnMarkerClickListener
 {

	private Main m;
	private GoogleMap mMap;
	private boolean zoneLayingState;
	private boolean pathLayingState;

	private PolylineOptions pathOptions;
	private Polyline path;
	private Marker tempMark;
	private Marker quadMark;
	private PolylineOptions quadLineOptions;
	private Polyline quadLine;
	 LatLng central = new LatLng(-34.91702, 138.60391);
	 LatLng quadPos = new LatLng(-34.91702, 138.60391);

	//public HashSet<Polygon> zones;
	public HashSet<Polyline> paths;
	public HashSet<Marker> markers;
	public ArrayList<Zone> zones;
	//private PolygonOptions zoneOptions;
	private Zone zone;
	 private Path genPath;
	 private LatLng curPos;


	 public void generatePath() {
		 ArrayList<Zone> cz = new ArrayList<Zone>();

		 Collections.copy(cz, zones);


		 genPath.addPoint(quadPos);
		 //lineSearch(zone);


		 curPos = quadPos;

		 while(cz.size() > 0) {

			 int polyIndex = -1;
			 double minDistance = Double.MAX_VALUE;
			 int index = -1;

			 for(int i=0; i<cz.size(); i++) {

				 Zone p = cz.get(i);

				 for(int j=0; j<p.numPoints(); j++) {
					 double distance = Math.sqrt( Math.pow(curPos.latitude - p.getPoint(j).latitude, 2) +
							 Math.pow(curPos.longitude - p.getPoint(j).longitude, 2) );

					 if(distance < minDistance) {
						 minDistance = distance;
						 polyIndex = i;
						 index = j;
					 }
				 }
			 }

			 lineSearch(cz.get(polyIndex));
			 cz.remove(polyIndex);
		 }

		 //path.addPoint(new Point2D.Double(100, 100));
	 }

	 private void lineSearch(Zone p) {

	 }

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.map_fragment_layout, container, false);

		init();

		return v;
	}

	public void setMain(Main main) {
		m = main;
	}

	private void init() {
		zoneLayingState = false;

		markers = new HashSet<Marker>();
		//zones = new HashSet<Polygon>();
		paths = new HashSet<Polyline>();
		zones = new ArrayList<Zone>();

		quadLineOptions = new PolylineOptions()
				.color(getResources().getColor(R.color.map_quad_path_color))
				.width(getResources().getDimension(R.dimen.map_stroke_width))
				.clickable(false);
	}

	public void loadKML(Intent intent, Context c) {

	}

	private void setObjectsClickable(boolean set) {

		if(set) {
			mMap.setOnPolygonClickListener(this);
		}
		else {
			mMap.setOnPolygonClickListener(null);
		}

		for(Zone z : zones) {
			z.poly.setClickable(set);
		}
		for(Polyline p : paths) {
			p.setClickable(set);
		}
	}

	public void setZoneLayingState(boolean set) {
		if(pathLayingState) {
			return;
		}

		if(zoneLayingState == set) {
			//do nothing
			return;
		}

		if(set) {
			zoneLayingState = true;

			setObjectsClickable(false);
			zone = new Zone(getActivity().getApplicationContext());


		} else {
			addZonePoint(new LatLng(0, 0), true);
		}
	}

	public void setPathLayingState(boolean set) {
		if(zoneLayingState) {
			return;
		}

		if(pathLayingState == set) {
			//do nothing
			return;
		}

		if(set) {
			pathLayingState = true;

			setObjectsClickable(false);

			pathOptions = new PolylineOptions()
					.color(getResources().getColor(R.color.map_path_outline))
					.width(getResources().getDimension(R.dimen.map_stroke_width))
					.clickable(true);
		} else {
			addPathPoint(new LatLng(0, 0), true);
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
		if(zoneLayingState) {
			addZonePoint(ll, false);
		}
		if(pathLayingState) {
			addPathPoint(ll, false);
		}
	}

	@Override
	public void onMapLongClick(LatLng ll) {

		if(zoneLayingState) {
			addZonePoint(ll, true);
		}
		else if(pathLayingState) {
			addPathPoint(ll, true);
		}
		else {
			newMarker(ll);
		}

		/*
		// apparently no-one likes the long click as a function.
		// try and keep everything as single taps

		if(zoneLayingState) {
			addZonePoint(ll, false);
			addZonePoint(ll, true);
		} else {
			newMarker(ll);
		}
		*/
	}


	@Override
	public void onPolylineClick(final Polyline p) {
		if(pathLayingState || zoneLayingState)
			return;

		p.setColor(getResources().getColor(R.color.map_szone_outline));

		AlertDialog confirmDelete = new AlertDialog.Builder(m)
				.setTitle("Confirm Delete")
				.setMessage("Delete this path?")
				.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						paths.remove(p);
						p.remove();
						Log.i("Path Clicked", "Path Count: " + paths.size());
						dialog.dismiss();
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						p.setColor(getResources().getColor(R.color.map_path_outline));

						dialog.dismiss();
					}
				})
				.create();

		confirmDelete.show();
	}

	@Override
	public void onPolygonClick(final Polygon p) {
		if(pathLayingState || zoneLayingState) {
			return;
		}

		p.setStrokeColor(getResources().getColor(R.color.map_szone_outline));
		p.setFillColor(getResources().getColor(R.color.map_szone_fill));

		AlertDialog confirmDelete = new AlertDialog.Builder(m)
				.setTitle("Confirm Delete")
				.setMessage("Delete this zone?")
				.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						p.remove();
						for(Zone z : zones) {
							if(z.poly.equals(p)) {
								zones.remove(z);
								break;
							}
						}

						Log.i("Polygon Clicked", "Polygon Count: " + zones.size());
						dialog.dismiss();
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						p.setStrokeColor(getResources().getColor(R.color.map_zone_outline));
						p.setFillColor(getResources().getColor(R.color.map_zone_fill));

						dialog.dismiss();
					}
				})
				.create();

		confirmDelete.show();
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

	public void updateQuadPosition(float x, float y) {

		// set central position
		LatLng ll = central;

		//offset with coordinates relative to
		double r_earth = 6378000.0;
		double latitude = ll.latitude + (y / r_earth) * (180 / Math.PI);
		double longitude = ll.longitude + (x / r_earth) * (180 / Math.PI) / Math.cos(ll.latitude * Math.PI/180);

		quadPos = new LatLng(latitude, longitude);
		quadLineOptions.add(quadPos);

		MarkerOptions newMarker = new MarkerOptions()
				.position(quadPos)
				.icon(BitmapDescriptorFactory.fromResource(R.mipmap.quad_pin))
				.title("Quad bike position")
				.flat(false);

		try {
			quadLine.remove();
		} catch(NullPointerException e) {
			// do nothing
		}
		try {
			quadMark.remove();
		} catch(NullPointerException e) {
			// do nothing
		}

		quadMark = mMap.addMarker(newMarker);
		quadLine = mMap.addPolyline(quadLineOptions);
	}

	private void addZonePoint(LatLng ll, boolean isFinal) {
		if(isFinal) {
			//zones.add(zone);

			zone.removeFromMap();
			ArrayList<Zone> nz = zone.convexSplit();

			Log.e("number of zones", "" + nz.size());

			for(Zone z : nz) {
				zones.add(z);
				z.addToMap(mMap);
			}

			zone = null;
			zoneLayingState = false;
			m.zoneMode.setChecked(false);
			setObjectsClickable(true);
			return;
		}

		zone.addPoint(ll);
		zone.addToMap(mMap);
		/*
		zone.opts.add(ll);
		try {
			zone.poly.remove();
		} catch (NullPointerException e) {
			Log.i("Null Pointer", "zone doesnt exist yet");
		}

		zone.poly = mMap.addPolygon(zone.opts);
		*/
	}

	private void addPathPoint(LatLng ll, boolean isFinal) {
		if(isFinal) {
			path.setClickable(true);
			paths.add(path);
			path = null;
			pathLayingState = false;
			pathOptions = null;
			m.pathMode.setChecked(false);
			setObjectsClickable(true);
			return;
		}

		pathOptions.add(ll);
		try {
			path.remove();
		} catch (NullPointerException e) {
			Log.i("Null Pointer", "zone doesnt exist yet");
		}

		path = mMap.addPolyline(pathOptions);
	}


	public void setMapType(int type) {
		mMap.setMapType(type);
	}

	public void clearAll() {
		for(Polyline p : paths) {
			p.remove();
		}
		paths.clear();

		for(Zone p : zones) {
			p.poly.remove();
		}
		zones.clear();
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		if(googleMap == null) {
			Log.e("GoogleMap", "Failed to load GoogleMap API");
		}

		//mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

		try {
			mMap.setMyLocationEnabled(true);
		} catch(SecurityException e) {

		}

		// Add a marker in [UNIVERSITY OF ADELAIDE] and move the camera

		mMap.addMarker(new MarkerOptions().position(central).title("University of Adelaide")).setDraggable(true);
		mMap.moveCamera(CameraUpdateFactory.newLatLng(central));
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(central, 15.0f));

		mMap.setOnMapClickListener(this);
		mMap.setOnMapLongClickListener(this);
		mMap.setOnPolygonClickListener(this);
		mMap.setOnMarkerClickListener(this);
		mMap.setOnPolylineClickListener(this);

		/*
		PolygonOptions rectOptions = new PolygonOptions()
				.add(new LatLng(-34.922142, 138.587171))
				.add(new LatLng(-34.921003, 138.610665))
				.add(new LatLng(-34.934755, 138.617857))
				.add(new LatLng(-34.936363, 138.588315))
				.fillColor(getResources().getColor(R.color.map_zone_fill))
				.strokeWidth(getResources().getDimension(R.dimen.map_stroke_width))
				.strokeColor(getResources().getColor(R.color.map_zone_outline))
				.clickable(true);

// Get back the mutable Polyline
		Polygon polygon = mMap.addPolygon(rectOptions);
		zones.add(polygon);

		PolylineOptions lineOptions = new PolylineOptions()
				.add(new LatLng(-34.915142, 138.587171))
				.add(new LatLng(-34.914003, 138.610665))
				.color(getResources().getColor(R.color.map_path_outline))
				.width(getResources().getDimension(R.dimen.map_stroke_width))
				.clickable(true);

		Polyline polyline = mMap.addPolyline(lineOptions);
		paths.add(polyline);


		lineOptions = new PolylineOptions()
				.add(new LatLng(-34.9151, 138.587))
				.add(new LatLng(-34.9140, 138.611))
				.color(getResources().getColor(R.color.map_path_outline))
				.width(getResources().getDimension(R.dimen.map_stroke_width))
				.clickable(true);

		polyline = mMap.addPolyline(lineOptions);
		paths.add(polyline);

		*/
	}


}