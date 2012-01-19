package edu.mit.pt.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import edu.mit.pt.maps.PlacesItemizedOverlay;

import android.os.AsyncTask;
import android.util.Log;

public class RoomLoader extends AsyncTask<PlacesItemizedOverlay, Integer, Void> {
	public Set<Place> getRooms() {
		Set<Place> roomSet = new HashSet<Place>();
		String roomJSON = readRoomJSON();
		try {
			JSONObject rooms = new JSONObject(roomJSON);
			Log.i(RoomLoader.class.getName(),
					"Number of rooms " + rooms.length());
			
			JSONArray roomList = rooms.names();
			for (int i = 0; i < roomList.length(); i++) {
				String name = roomList.getString(i);
				JSONObject coords = rooms.getJSONObject(name);
				long lat = coords.getLong("lat");
				long lon = coords.getLong("lon");
				Place room = new Place(name, lat, lon);
				roomSet.add(room);
				Log.i(RoomLoader.class.getName(), roomList.getString(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return roomSet;
	}
	
	public String readRoomJSON() {
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(
				"http://mit.edu/~georgiou/pt/rooms.json");
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e(RoomLoader.class.toString(), "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
	
	@Override
	protected Void doInBackground(PlacesItemizedOverlay... params) {
		Set<Place> rooms = getRooms();
		for (Place p: rooms) {
			GeoPoint point = new GeoPoint((int)p.getLatE6(), (int)p.getLonE6());
			OverlayItem overlayItem = new OverlayItem(point, p.getName(), "");
			params[0].addOverlay(overlayItem);
		}
		return null;
	}
}