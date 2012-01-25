package edu.mit.pt.data;

import android.os.Parcel;
import edu.mit.pt.R;

public class Classroom extends Place {

	public Classroom(long id, String name, int latE6, int lonE6, int floor) {
		super(id, name, latE6, lonE6, floor);
	}

	public Classroom(Parcel in) {
		super(in);
	}

	@Override
	public PlaceType getPlaceType() {
		return PlaceType.CLASSROOM;
	}

	@Override
	public int getMarkerId() {
		return R.drawable.green_point;
	}

}