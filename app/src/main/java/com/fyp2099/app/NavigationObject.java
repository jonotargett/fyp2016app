package com.fyp2099.app;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Jono on 26/10/2016.
 */
public class NavigationObject {

	public List<LatLng> points;

	public NavigationObject() {
		points = new ArrayList<LatLng>();
	}


	public void addPoint(LatLng point) {
		points.add(point);
	}

	public int numPoints() {
		return points.size();
	}

	public LatLng getPoint(int n) {
		if(n > numPoints()) {
			return null;
		}

		return points.get(n);
	}

	public LatLng getNearestPoint(LatLng pos) {
		int nearest = -1;
		float closest = Float.MAX_VALUE;
		float[] results = new float[1];

		for(int i=0; i<numPoints(); i++){
			LatLng point = points.get(i);

			Location.distanceBetween(pos.latitude, pos.longitude, point.latitude, point.longitude, results);

			if(results[0] < closest) {
				closest = results[0];
				nearest = i;
			}
		}

		if(nearest < 0) {
			return null;
		} else {
			return points.get(nearest);
		}
	}
}
