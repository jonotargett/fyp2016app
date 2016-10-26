package com.fyp2099.app;

import android.content.Context;
import android.location.Location;

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

	public Path(Context c) {
		super(c);
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
