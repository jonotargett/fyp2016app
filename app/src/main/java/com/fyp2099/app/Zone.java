package com.fyp2099.app;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Created by Jono on 26/10/2016.
 */
public class Zone extends NavigationObject {

	public Polygon poly;
	public PolygonOptions opts;


	public Zone(Context c) {
		super(c);
		isZone = true;
	}

	public void addToMap(GoogleMap map) {

		// get the display options prepared
		opts = new PolygonOptions()
				.fillColor(context.getResources().getColor(R.color.map_zone_fill))
				.strokeColor(context.getResources().getColor(R.color.map_zone_outline))
				.strokeWidth(context.getResources().getDimension(R.dimen.map_stroke_width))
				.clickable(true);

		// add all of the defined points into the display model
		for(LatLng ll : points) {
			opts.add(ll);
		}

		// attempt to remove the previous polygon from the map (if it exists)
		try {
			poly.remove();
		} catch (NullPointerException e) {
			Log.i("Null Pointer", "zone doesnt exist yet");
		}

		// add the new polygon to the map, retrieve the handle
		poly = map.addPolygon(opts);

	}

	public void removeFromMap() {
		poly.remove();
	}

	@Override
	protected void recreatePoly() {

	}

	private void correctWinding() {
		if(numPoints() < 3) {
			return;
		}

		LatLng a = getPoint(0);
		LatLng b = getPoint(1);
		LatLng c = getPoint(2);

		LatLng b_a = new LatLng(b.latitude - a.latitude, b.longitude - a.longitude);
		LatLng c_a = new LatLng(c.latitude - a.latitude, c.longitude - a.longitude);

		double winding = b_a.latitude*c_a.longitude - b_a.longitude*c_a.latitude;

		if(winding < 0) {
			// already in the correct winding order
			Log.i("Winding", "Winding NOT corrected");
			return;
		}
		else {
			Log.i("Winding", "Winding IS corrected");

			Collections.reverse(points);
		}

	}

	private boolean sameSide(LatLng pos, int edge) {
		int n = numPoints();
		edge = edge % n;

		LatLng a = getPoint((edge + 0) % n);
		LatLng b = getPoint((edge + 1) % n);
		LatLng c = getPoint((edge + 2) % n);

		// ALGORITHM IS:
		//cp1 = CrossProduct(b-a, p1-a)
		//cp2 = CrossProduct(b-a, p2-a)
		//if DotProduct(cp1, cp2) >= 0 then return true
		//else return false

		// u x v = [u2v3 - u3v2, u3v1 - u1v3, u1v2 - u2v1]

		LatLng b_a = new LatLng(b.latitude - a.latitude, b.longitude - a.longitude);
		LatLng p1_a = new LatLng(c.latitude - a.latitude, c.longitude - a.longitude);
		LatLng p2_a = new LatLng(pos.latitude - a.latitude, pos.longitude - a.longitude);

		double[] cp1 = {0, 0, b_a.latitude*p1_a.longitude - b_a.longitude*p1_a.latitude};
		double[] cp2 = {0, 0, b_a.latitude*p2_a.longitude - b_a.longitude*p2_a.latitude};

		double dp = cp1[2] * cp2[2];

		if(dp >= 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean contains(LatLng pos) {
		//if(numPoints() != 3) {   // only for triangular decomposititions atm
		//	return false;
		//}

		// CHANGED: ONLY WORKS FOR THINGS THAT ARE ALREADY CONVEX

		for(int i=0; i<numPoints(); i++) {
			if(!sameSide(pos, i)) {
				return false;
			}
		}

		//if(!sameSide(pos, 0)) return false;
		//if(!sameSide(pos, 1)) return false;
		//if(!sameSide(pos, 2)) return false;

		return true;
	}

	public ArrayList<Zone> convexSplit() {

		// hertelMehlhornPartition
		// algorithm for splitting a concave closed polygon into convex parts.
		// https://www8.cs.umu.se/kurser/TDBA77/VT06/algorithms/BOOK/BOOK5/NODE194.HTM
		//
		// 1.	triangulate everything. attempt at ear clipping algorithm
		// 2.	decomposition of edges, where edge removal does not introduce concavity
		// 3.	exit
		//---------------------------------


		// ----------------------------------------------------------------------------
		// step 1:
		/*
		 * From Polygon Triangulation by Daniel Taylor @ gamedev.net
		 *
		 * create a list of the vertices (perferably in CCW order, starting anywhere)
			while true
			  for every vertex
			    let pPrev = the previous vertex in the list
			    let pCur = the current vertex;
			    let pNext = the next vertex in the list
			    if the vertex is not an interior vertex (the wedge product of (pPrev - pCur) and (pNext - pCur) <= 0, for CCW winding);
			      continue;
			    if there are any vertices in the polygon inside the triangle made by the current vertex and the two adjacent ones
			      continue;
			    create the triangle with the points pPrev, pCur, pNext, for a CCW triangle;
			    remove pCur from the list;
			  if no triangles were made in the above for loop
			    break;
		 */

		// ----------------------------------------------------------------------------
		ArrayList<Zone> convexZones = new ArrayList<Zone>();

		// correct for CCW winding, as required
		correctWinding();


		// make working copy of zone
		Zone work = new Zone(context);
		for(int i=0; i<numPoints(); i++) {
			work.addPoint(getPoint(i));
		}

		int c = 0;

		while(true) {
			c++;
			boolean madeATriangle = false;

			// break early if its not possible to make further triangles
			if(work.numPoints() < 3) {
				Log.i("break", "no more triangles");
				break;
			}
			// for each vertex in the working set
			for(int i=0; i<work.numPoints(); i++) {

				// form triangles
				int pPrev = i-1;
				int pCur = i;
				int pNext = i + 1;

				if(pPrev < 0)
					pPrev = work.numPoints() - 1;
				if(pNext == work.numPoints())
					pNext = 0;

				// calculate the determinate to find concavity/convexity

				LatLng pc = new LatLng(work.getPoint(pPrev).latitude - work.getPoint(pCur).latitude,
						work.getPoint(pPrev).longitude - work.getPoint(pCur).longitude);
				LatLng nc = new LatLng(work.getPoint(pNext).latitude - work.getPoint(pCur).latitude,
						work.getPoint(pNext).longitude - work.getPoint(pCur).longitude);

				double det = pc.latitude * nc.longitude - pc.longitude * nc.latitude;

				// if this is NOT an interior vertex
				if(det < 0) {
					// then skip, move to the next vertex
					Log.i("break", "not an interior vertex");
					continue;
				}


				Zone test = new Zone(context);
				test.addPoint(work.getPoint(pPrev));
				test.addPoint(work.getPoint(pCur));
				test.addPoint(work.getPoint(pNext));

				boolean shouldContinue = false;

				// test if any of the vertices in the working set lie within the new
				// polygon, created around the current vertex
				for(int j=0; j<work.numPoints(); j++) {
					if(j == pPrev ||
							j == pCur ||
							j == pNext) {
						continue;
					}

					if (test.contains(work.getPoint(j))) {
						Log.i("break", "point " + j + " lies within the triangle");
						shouldContinue = true;
						break;
					}
					//if(test.contains(new Point2D.Double(work.xpoints[i], work.ypoints[i]))) {
					//	shouldContinue = true;
					//	break;
					//}
				}

				// ignore this vertex if any other vertex lies within the created polygon
				if(shouldContinue)
					continue;


				// success! found a possible triangle
				Log.i("success", "found a triange");
				convexZones.add(test);

				// now recreate the working set
				Zone newWork = new Zone(context);
				for(int j=0; j<work.numPoints(); j++) {
					if(j == pCur)
						continue;
					newWork.addPoint(work.getPoint(j));
				}
				work = null;
				work = newWork;


				madeATriangle = true;
				break;
			}

			if(madeATriangle == false)
				break;

		}

		Log.e("Iterations", "" + c);
		Log.e("Number of triangles", "" + convexZones.size());



		// DECOMPOSITION ---------------------------------------------------------------------

		// iterate through the polygons, try to find a pair which share a chord
		// see if that chord can be decomposed without introducing concavity
		// TODO: introduce some minimum 'closeness' factor to decompose vertices within a certain distance of each other



		while(true) {

			boolean breaking = false;

			for(int i=0; i<convexZones.size(); i++) {

				if(breaking)
					break;

				for(int j=0; j<convexZones.size(); j++) {
					if(i == j)
						continue;

					Zone p1 = convexZones.get(i);
					Zone p2 = convexZones.get(j);

					int index11 = 0, index12 = 0, index21 = 0, index22 = 0;

					int shared = 0;

					for(int k=0; k<p1.numPoints(); k++) {
						LatLng pp = new LatLng(p1.getPoint(k).latitude, p1.getPoint(k).longitude);

						for(int l=0; l<p2.numPoints(); l++) {
							LatLng pc = new LatLng(p2.getPoint(l).latitude, p2.getPoint(l).longitude);

							if(pp.latitude == pc.latitude && pp.longitude == pc.longitude) {

								if(shared == 0) {
									index11 = k;
									index21 = l;
								} else {
									index12 = k;
									index22 = l;
								}
								shared++;
							}
						}
					}

					if(shared == 2) {
						// A CHORD IS SHARED. PRAISE THE SUN

						// this is where we merge them

						Zone newPoly = new Zone(context);

						boolean started = false;
						int k = 0;

						while(true) {
							if(k == index11)
								started = true;

							if(k == index12 && started)
								break;

							if(started) {
								newPoly.addPoint(p1.getPoint(k));
							}

							k++;
							if(k == p1.numPoints())
								k = 0;
						}

						started = false;
						k = 0;
						while(true) {
							if(k == index22)
								started = true;

							if(k == index21 && started)
								break;

							if(started) {
								newPoly.addPoint(p2.getPoint(k));
							}

							k++;
							if(k == p2.numPoints())
								k = 0;
						}


						// check for concavity
						// TODO: this needs to be more aggressive. this is missing some obvious additions
						// that are still convex. appears to form quads and then give up
						// TODO: needs to check for internal points
						boolean concave = false;

						// for each vertex in the working set
						for(int l=0; l<newPoly.numPoints(); l++) {

							// form triangles
							int pPrev = l-1;
							int pCur = l;
							int pNext = l + 1;

							if(pPrev < 0)
								pPrev = newPoly.numPoints() - 1;
							if(pNext == newPoly.numPoints())
								pNext = 0;

							// calculate the determinate to find concavity/convexity
							LatLng pc = new LatLng(newPoly.getPoint(pPrev).latitude - newPoly.getPoint(pCur).latitude,
									newPoly.getPoint(pPrev).longitude - newPoly.getPoint(pCur).longitude);
							LatLng nc = new LatLng(newPoly.getPoint(pNext).latitude - newPoly.getPoint(pCur).latitude,
									newPoly.getPoint(pNext).longitude - newPoly.getPoint(pCur).longitude);

							//Point2D.Double pc = new Point2D.Double(newPoly.xpoints[pPrev] - newPoly.xpoints[pCur],
							//		newPoly.ypoints[pPrev] - newPoly.ypoints[pCur]);
							//Point2D.Double nc = new Point2D.Double(newPoly.xpoints[pNext] - newPoly.xpoints[pCur],
							//		newPoly.ypoints[pNext] - newPoly.ypoints[pCur]);

							double det = pc.latitude * nc.longitude - pc.longitude * nc.latitude;

							// if this is NOT an interior vertex
							if(det <= 0) {
								concave = true;
								break;
							}
						}

						if(!concave) {
							convexZones.remove(p1);
							convexZones.remove(p2);
							convexZones.add(newPoly);


							breaking = true;
							break;
						}
					}
				}
			}

			if(breaking == false) {
				break;
			}
		}


		return convexZones;


	}

}
