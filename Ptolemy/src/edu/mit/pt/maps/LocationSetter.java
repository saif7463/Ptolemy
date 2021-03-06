package edu.mit.pt.maps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import edu.mit.pt.location.APGeoPoint;
import edu.mit.pt.location.WifiLocation;

// FIXME: This also throws a 
public class LocationSetter {
	// singleton
	static LocationSetter locationSetter = null;

	// Available data
	private double bearing;
	private APGeoPoint currentLocation;

	private Context context;
	private XPSOverlay overlay;

	// Bearings
	private SensorManager sensorManager;
	private SensorEventListener compassListener;
	private Sensor accelerometerSensor;
	private Sensor magneticFieldSensor;

	private final float minGPSAccuracy = (float) 30.0; // meters
	private long lastGPSTime = 0;

	// Location
	private Handler updateLocationHandler;
	private boolean isStopped;
	private LocationListener locationListener; // listens to gps

	private static LocationManager locationManager;

	public static LocationSetter getInstance(Context context, XPSOverlay overlay) {
		if (locationSetter == null)
			locationSetter = new LocationSetter(context, overlay);
		return locationSetter;

	}

	private LocationSetter(Context context, XPSOverlay overlay) {
		this.context = context;
		this.overlay = overlay;
		updateLocationHandler = new Handler();
		isStopped = true;
		initBearing(context);
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		String locationProvider = LocationManager.GPS_PROVIDER;
		currentLocation = null;
		locationListener = new LocationListener() {

			public void onLocationChanged(Location location) {
				Log.v(LocationSetter.class.getName(), "Location changed: "
						+ location.toString());
				if (location.getAccuracy() < minGPSAccuracy) {
					// want to use GPS instead
					currentLocation = new APGeoPoint(
							(int) (location.getLatitude() * 1e6),
							(int) (location.getLongitude() * 1e6), 1); // assume
																		// first
																		// floor
																		// if
																		// gps
																		// works
					lastGPSTime = System.currentTimeMillis();
					locationSetter.setLocation(currentLocation);
				}
			}

			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub

			}

			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub

			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub

			}

		};
		locationManager.requestLocationUpdates(locationProvider, 0, 2,
				locationListener);
	}

	public APGeoPoint getPoint(Context context) {
		WifiLocation wifiLocation = WifiLocation.getInstance(context);
		APGeoPoint point = wifiLocation.getLocation(true);
		if (point == null)
			return null;
		setLocation(point);
		return currentLocation;
	}

	public void pause() {
		isStopped = true;
		pauseLocation();
		pauseBearing();
	}

	public void resume() {
		isStopped = false;
		resumeLocation();
		resumeBearing();
	}

	public void stop() {
		pause();
	}

	private void pauseLocation() {
		WifiLocation.getInstance(context).pause();
		locationManager.removeUpdates(locationListener);
	}

	private void resumeLocation() {
		WifiLocation.getInstance(context).resume();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, locationListener);
		getPoint(context);

	}

	private void initBearing(Context context) {
		sensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		accelerometerSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magneticFieldSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		compassListener = new SensorEventListener() {
			private float[] accData;
			private float[] magData;

			public void onSensorChanged(SensorEvent event) {
				switch (event.sensor.getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					accData = event.values;
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
					magData = event.values;
					break;
				}
				if (accData != null && magData != null) {
					float R[] = new float[9];
					float I[] = new float[9];
					boolean success = SensorManager.getRotationMatrix(R, I,
							accData, magData);
					if (success) {
						float orientation[] = new float[3];
						SensorManager.getOrientation(R, orientation);
						handleBearing(orientation[0] * 180.0 / Math.PI);
					}
				}
			}

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}

	private void pauseBearing() {
		// Unregister listeners
		sensorManager.unregisterListener(compassListener, magneticFieldSensor);
		sensorManager.unregisterListener(compassListener, accelerometerSensor);
	}

	private void resumeBearing() {
		// Register listeners
		sensorManager.registerListener(compassListener, magneticFieldSensor,
				SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(compassListener, accelerometerSensor,
				SensorManager.SENSOR_DELAY_UI);
	}

	private GeoPoint exponentialWeightedMovingAverage(GeoPoint avg,
			GeoPoint newPoint, double factor) {
		int newLatitude = (int) (avg.getLatitudeE6() * factor + newPoint
				.getLatitudeE6() * (1 - factor));
		int newLongitude = (int) (avg.getLongitudeE6() * factor + newPoint
				.getLongitudeE6() * (1 - factor));
		return new GeoPoint(newLatitude, newLongitude);
	}
	
	private double exponentialWeightedMovingAverage(double avg, double newV, double factor) {
		return avg*factor + newV*(1-factor);
	}

	public static float distanceBetweenGeoPoints(GeoPoint a, GeoPoint b) {
		float r[] = new float[1];
		Location.distanceBetween(a.getLatitudeE6() / 1e6,
				a.getLongitudeE6() / 1e6, b.getLatitudeE6() / 1e6,
				b.getLongitudeE6() / 1e6, r);
		return r[0];
	}

	public void setLocation(APGeoPoint p) {
		// 20s
		if (!(System.currentTimeMillis() - lastGPSTime < 20000)) {
			if (currentLocation == null) {
				currentLocation = p;
			} else {
				if (p == null)
					return;
				// snap if you're farther than 100
				if (distanceBetweenGeoPoints(currentLocation, p) > 100.0) {
					currentLocation = p;
				} else {
					currentLocation = new APGeoPoint(
							exponentialWeightedMovingAverage(currentLocation,
									p, 0.6), p.getFloor());
				}

			}
		}
		overlay.setLocation(currentLocation);
	}

	protected void handleBearing(double bng) {
		bearing = exponentialWeightedMovingAverage(bearing,
				bng, 0.9);
		overlay.setBearing(bearing);
	}
}