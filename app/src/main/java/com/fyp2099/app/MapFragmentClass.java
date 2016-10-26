package com.fyp2099.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
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


	private Marker tempMark;
	private Marker quadMark;
	private PolylineOptions quadLineOptions;
	private Polyline quadLine;
	 LatLng central = new LatLng(-34.91702, 138.60391);
	 LatLng quadPos = new LatLng(-34.91702, 138.60391);

	//public HashSet<Polygon> zones;
	public ArrayList<Path> paths;
	public HashSet<Marker> markers;
	public ArrayList<Zone> zones;
	//private PolygonOptions zoneOptions;
	private Zone zone;
	 private Path path;
	 public Path genPath;
	 private LatLng curPos;
	 private LatLng forwardVec;
	 private LatLng downVec;

	 private final double width = 2.0;
	 private final double minimumTravel = 1.0;
	 private final int maxSearchForAcutes = 100;


	 private LatLng addMetres(LatLng ll, double x, double y) {
		 //offset with coordinates relative to
		 double r_earth = 6378000.0;
		 double latitude = ll.latitude + (y / r_earth) * (180 / Math.PI);
		 double longitude = ll.longitude + (x / r_earth) * (180 / Math.PI) / Math.cos(ll.latitude * Math.PI/180);

		 return new LatLng(latitude, longitude);
	 }

	 private LatLng addBearingDistance(LatLng ll, double bearing, double metres) {

		 //bearing is degree east of true north.
		 double x = metres * Math.sin(bearing);
		 double y = metres * Math.cos(bearing);

		 double r_earth = 6378000.0;
		 double latitude = ll.latitude + (y / r_earth) * (180 / Math.PI);
		 double longitude = ll.longitude + (x / r_earth) * (180 / Math.PI) / Math.cos(ll.latitude * Math.PI/180);

		 return new LatLng(latitude, longitude);
	 }

	 public void generatePath() {
		 genPath.removeFromMap();

		 genPath = new Path(getActivity().getApplicationContext());

		 ArrayList<NavigationObject> cz = new ArrayList<NavigationObject>();

		 for(Zone z : zones) {
			 cz.add(z);
		 }
		 for(Path p : paths) {
			 cz.add(p);
		 }


		 genPath.addPoint(quadPos);
		 //lineSearch(zone);


		 curPos = quadPos;

		 while(cz.size() > 0) {
			 Log.i("size: ", "" + cz.size() + " zones");

			 int polyIndex = -1;
			 double minDistance = Double.MAX_VALUE;
			 int index = -1;

			 for(int i=0; i<cz.size(); i++) {

				 NavigationObject p = cz.get(i);
				 Log.i("size: ", "p has " + p.numPoints() + " points");

				 LatLng closePoint = p.getNearestPoint(curPos);
				 double distance = Math.sqrt( Math.pow(curPos.latitude - closePoint.latitude, 2) +
							 Math.pow(curPos.longitude - closePoint.longitude, 2) );

				 if(distance < minDistance) {
					 minDistance = distance;
					 polyIndex = i;
				 }
			 }

			 lineSearch(cz.get(polyIndex));
			 cz.remove(polyIndex);
		 }

		 genPath.isGenerated = true;
		 genPath.addToMap(mMap);

		 //path.addPoint(new Point2D.Double(100, 100));
	 }

	 private void lineSearch(NavigationObject p) {
		 if(p.isZone) {
			 lineSearch((Zone)p);
		 } else {
			 lineSearch((Path)p);
		 }
	 }

	 private void lineSearch(Path p) {
		 if(p.numPoints() < 2) {
			 return;
		 }

		 int i;
		 if(p.getNearestPoint(curPos) == p.getPoint(0)) {
			 for(i=0; i<p.numPoints();i++) {
				 genPath.addPoint(p.getPoint(i));
			 }
			 curPos = p.getPoint(p.numPoints()-1);
		 } else {
			 for(i=p.numPoints()-1; i>=0;i--) {
				 genPath.addPoint(p.getPoint(i));
			 }
			 curPos = p.getPoint(0);
		 }

	 }

	 private void lineSearch(Zone p) {
		 //discover the closest entry point (a polygon corner) ---------------------------

		 double minDistance = Double.MAX_VALUE;
		 int index = -1;

		 for(int i=0; i<p.numPoints(); i++) {
			 double distance = Math.sqrt( Math.pow(curPos.latitude - p.getPoint(i).latitude, 2) +
					 Math.pow(curPos.longitude - p.getPoint(i).longitude, 2) );

			 if(distance < minDistance) {
				 minDistance = distance;
				 index = i;
			 }
		 }

		 curPos = new LatLng(p.getPoint(index).latitude, p.getPoint(index).longitude);


		 genPath.addPoint(new LatLng(curPos.latitude, curPos.longitude));

		 // -----------------------------------------------------------------------------
		 // get scanline vectors, ie forward, and down (next line)

		 int nextPoint = index + 1;
		 if(nextPoint >= p.numPoints())
			 nextPoint = 0;

		 int prevPoint = index - 1;
		 if(prevPoint < 0)
			 prevPoint = p.numPoints()-1;

		 // get a forward vector and normalise it
		 //forwardVec = new LatLng(p.getPoint(nextPoint).latitude - curPos.latitude,
		//		 p.getPoint(nextPoint).longitude - curPos.longitude);
		 //double length = Math.sqrt(Math.pow(forwardVec.latitude, 2) +
		//		 Math.pow(forwardVec.longitude, 2));

		 float[] results = new float[3];
		 Location.distanceBetween(curPos.latitude, curPos.longitude, p.getPoint(nextPoint).latitude, p.getPoint(nextPoint).longitude, results);
		 double forwardBearing = results[1];

		 forwardVec = new LatLng(Math.cos(Math.toRadians(forwardBearing)), Math.sin(Math.toRadians(forwardBearing)));
		 //forwardVec = new LatLng(forwardVec.latitude / length, forwardVec.latitude/length);

		 downVec = new LatLng(forwardVec.longitude, -forwardVec.latitude);

		 // double check that the 'downVec' actually points into the shape, rather than outside

		 //LatLng compVec = new LatLng(p.getPoint(prevPoint).latitude - curPos.latitude,
		//		 p.getPoint(prevPoint).longitude - curPos.longitude);

		 float[] results2 = new float[3];
		 Location.distanceBetween(curPos.latitude, curPos.longitude, p.getPoint(prevPoint).latitude, p.getPoint(prevPoint).longitude, results2);
		 double compBearing = results2[1];

		 LatLng compVec = new LatLng(Math.cos(Math.toRadians(compBearing)), Math.sin(Math.toRadians(compBearing)));


		 double dotProduct = compVec.latitude * downVec.latitude + compVec.longitude * downVec.longitude;

		 if(dotProduct < 0) {
			 // then the vectors do no point in the same direction (ie totally opposed)
			 downVec = new LatLng(-downVec.latitude, -downVec.longitude);
		 }


		 //LatLng tempPos = addMetres(curPos, forwardVec.longitude*results[0], forwardVec.latitude*results[0]);

		 //genPath.addPoint(new LatLng(tempPos.latitude, tempPos.longitude));
		 //genPath.addPoint(new LatLng(curPos.latitude, curPos.longitude));

		 //tempPos = addMetres(curPos, downVec.longitude*100, downVec.latitude*100);
		 //genPath.addPoint(new LatLng(tempPos.latitude, tempPos.longitude));
		 //genPath.addPoint(new LatLng(curPos.latitude, curPos.longitude));


		 // -------------------------------------------------------------------------------
		 //start mapping out the path;

		 // adjust for the potential of starting on an acute angle
		 int steps = 0;

		 // travel inwards
		 //curPos = new LatLng(curPos.latitude + downVec.latitude*width,
		//		 curPos.longitude + downVec.longitude*width);
		 curPos = addMetres(curPos, downVec.longitude*width, downVec.latitude*width);

		 while( !p.contains(curPos) ) {
			 if(steps > maxSearchForAcutes)
				 break;

			 //curPos = new LatLng(curPos.latitude + forwardVec.latitude * minimumTravel,
			//		 curPos.longitude + forwardVec.longitude*minimumTravel);
			 curPos = addMetres(curPos, forwardVec.longitude*minimumTravel, forwardVec.latitude*minimumTravel);
			 steps++;
		 }


		 genPath.addPoint(new LatLng(curPos.latitude, curPos.longitude));

		 int rows = 0;



		 while( true ) {

			 Log.w("TRAVEL", "row " + rows + " ---------------");
			 if(rows > 0) {
				 // travel inwards
				 //curPos = new LatLng(curPos.latitude+downVec.latitude*width,
				//		 curPos.longitude+downVec.longitude*width);
				 curPos = addMetres(curPos, downVec.longitude*width, downVec.latitude*width);
				 Log.w("TRAVEL", "travel inwards");
			 }

			 genPath.addPoint(new LatLng(curPos.latitude, curPos.longitude));

			 // these next two lines make the 'forwards' direction alternate between rows
			 int direction = (rows % 2);
			 if(direction == 0) {direction = -1; }
			 direction *= -1;

			 steps = 0;
			 int waste = 0;

			 while( p.contains(curPos)) {
				 //curPos = new LatLng(curPos.latitude + forwardVec.latitude*minimumTravel*direction,
				//		 curPos.longitude+forwardVec.longitude*minimumTravel*direction);
				 curPos = addMetres(curPos,forwardVec.longitude*minimumTravel*direction, forwardVec.latitude*minimumTravel*direction);
				 steps++;

				 waste++;
				 if(waste > 1000) {
					 //System.out.println("Wasdasste");
				 }
			 }
			 Log.w("TRAVEL", "moved forwards");

			 // take a step back to avoid overshoot
			 //curPos = new LatLng(curPos.latitude - forwardVec.latitude*minimumTravel*direction,
			//		 curPos.longitude-forwardVec.longitude*minimumTravel*direction);
			 curPos = addMetres(curPos,-forwardVec.longitude*minimumTravel*direction, -forwardVec.latitude*minimumTravel*direction);


			 //LatLng tempPos = new LatLng(curPos.latitude+downVec.latitude*width,
				//	 curPos.longitude+downVec.longitude*width);
			 LatLng tempPos = addMetres(curPos, downVec.longitude*width, downVec.latitude*width);

			 while( !p.contains(tempPos) ) {
				 if(steps <= 0)
					 break;

				 //curPos = new LatLng(curPos.latitude - forwardVec.latitude*minimumTravel*direction,
				//		 curPos.longitude-forwardVec.longitude*minimumTravel*direction);
				 curPos = addMetres(curPos,-forwardVec.longitude*minimumTravel*direction, -forwardVec.latitude*minimumTravel*direction);
				 tempPos = addMetres(curPos, downVec.longitude*width, downVec.latitude*width);

				 //genPath.addPoint(new LatLng(curPos.latitude, curPos.longitude));

				 steps--;

				 waste++;
				 if(waste > 1000) {
					 //System.out.println("Wasteedede");
				 }
			 }


			 genPath.addPoint(new LatLng(curPos.latitude, curPos.longitude));

			 if(steps < 1) {
				 Log.w("TRAVEL", "NO STEPS TAKEN. EXITING LOOP -------");
				 break;
			 }


			 rows++;


		 }


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
		paths = new ArrayList<Path>();
		zones = new ArrayList<Zone>();
		genPath = new Path(getActivity().getApplicationContext());

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
		for(Path p : paths) {
			p.poly.setClickable(set);
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
			path = new Path(getActivity().getApplicationContext());


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
						p.remove();
						for(Path pa : paths) {
							if(pa.poly.equals(p)) {
								paths.remove(pa);
								break;
							}
						}
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

	 public void clearTrail() {
		 try {
			 quadLine.remove();
		 } catch(NullPointerException e) {
			 // do nothing
		 }

		 quadLineOptions = new PolylineOptions()
				 .color(getResources().getColor(R.color.map_quad_path_color))
				 .width(getResources().getDimension(R.dimen.map_stroke_width))
				 .clickable(false);
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
	}

	private void addPathPoint(LatLng ll, boolean isFinal) {
		if(isFinal) {
			paths.add(path);
			path = null;
			pathLayingState = false;
			m.pathMode.setChecked(false);
			setObjectsClickable(true);
			return;
		}

		path.addPoint(ll);
		path.addToMap(mMap);
	}


	public void setMapType(int type) {
		mMap.setMapType(type);
	}

	public void clearAll() {
		for(Path p : paths) {
			p.poly.remove();
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
		//mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(central, 15.0f));
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(central, 18.0f));

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