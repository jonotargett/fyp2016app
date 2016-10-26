package com.fyp2099.app;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Created by Jono on 26/10/2016.
 */
public class Path extends NavigationObject {

	public Polyline poly;
	private PolylineOptions opts;
	public boolean isGenerated;

	public Path(Context c) {
		super(c);
		isZone = false;
		isGenerated = false;
	}


	public void addToMap(GoogleMap map) {

		// get the display options prepared
		opts = new PolylineOptions()
				.clickable(true);

		if(isGenerated) {
			opts.color(context.getResources().getColor(R.color.map_path_outline))
					.width(context.getResources().getDimension(R.dimen.map_gen_stroke_width));
		} else {
			opts.color(context.getResources().getColor(R.color.map_zone_outline))
					.width(context.getResources().getDimension(R.dimen.map_stroke_width));
		}

		// add all of the defined points into the display model
		for(LatLng ll : points) {
			opts.add(ll);
		}

		// attempt to remove the previous polygon from the map (if it exists)
		try {
			poly.remove();
		} catch (NullPointerException e) {
			Log.i("Null Pointer", "path doesnt exist yet");
		}

		// add the new polygon to the map, retrieve the handle
		poly = map.addPolyline(opts);

	}

	public void removeFromMap() {
		try {
			poly.remove();
		} catch (NullPointerException e) {
			Log.i("Null Pointer", "path doesnt exist yet");
		}
	}

	@Override
	protected void recreatePoly() {

	}

	@Override
	public LatLng getNearestPoint(LatLng pos) {
		if(numPoints() < 2) {
			return null;
		}

		LatLng first = points.get(0);
		LatLng last = points.get(numPoints()-1);

		float[] firstresults = new float[1];
		float[] lastresults = new float[1];

		Location.distanceBetween(pos.latitude, pos.longitude, first.latitude, first.longitude, firstresults);
		Location.distanceBetween(pos.latitude, pos.longitude, last.latitude, last.longitude, lastresults);

		if(lastresults[0] < firstresults[0]) {
			return last;
		} else {
			return first;
		}
	}
}
