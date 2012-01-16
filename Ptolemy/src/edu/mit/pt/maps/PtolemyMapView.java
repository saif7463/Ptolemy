package edu.mit.pt.maps;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import edu.mit.pt.Config;
import edu.mit.pt.R;

public class PtolemyMapView extends MapView {

	Context ctx;
	private final int SUPPORTED_ZOOM_LEVEL = 21;
	private final int IMAGE_TILE_SIZE = 256;

	private static final int WEST_LONGITUDE_E6 = -71132032;
	private static final int EAST_LONGITUDE_E6 = -71004543;
	private static final int NORTH_LATITUDE_E6 = 42385049;
	private static final int SOUTH_LATITUDE_E6 = 42339688;

	private int pNumRows = 3;
	private int pNumColumns = 3;
	Bitmap bm;

	public PtolemyMapView(Context context, String key) {
		super(context, key);
		ctx = context;
		setup();
	}

	public PtolemyMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		ctx = context;
		setup();
	}

	public PtolemyMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		ctx = context;
		setup();
	}

	private void setup() {
		List<Overlay> overlays = getOverlays();
		overlays.add(new TileOverlay());

		setRowsCols();
	}

	private void setRowsCols() {
		Display display = ((WindowManager) ctx
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		int w = display.getWidth();
		int h = display.getHeight();

		pNumRows = h / IMAGE_TILE_SIZE;
		pNumColumns = w / IMAGE_TILE_SIZE;

		if (h % IMAGE_TILE_SIZE != 0) {
			pNumRows++;
		}

		if (w % IMAGE_TILE_SIZE != 0) {
			pNumColumns++;
		}
	}

	/**
	 * PtolemyMapView uses one giant TileOverlay to draw its maps on top of
	 * Google Maps.
	 * 
	 * @author Josh
	 * 
	 */
	class TileOverlay extends Overlay {

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {

			super.draw(canvas, mapView, shadow);
			// draw() is called twice, once with shadow=true for shadow layer
			// and once with shadow=false for the real layer.
			if (shadow) {
				return;
			}
			int zoomLevel = mapView.getZoomLevel();
			if (zoomLevel != SUPPORTED_ZOOM_LEVEL) {
				return;
			}

			Log.v(Config.TAG,
					"Drawing! MapZoomLevel is " + mapView.getZoomLevel());

			int tileSize = computeTileSize(mapView, zoomLevel);

			GeoPoint topleftGeoPoint = mapView.getProjection().fromPixels(0, 0);
			// googleX and googleY correspond to the ints that google maps uses
			// to ID tiles
			double googleX = computeGoogleX(topleftGeoPoint.getLongitudeE6(),
					zoomLevel);
			double googleY = computeGoogleY(topleftGeoPoint.getLatitudeE6(),
					zoomLevel);

			// Tile[X/Y] is integer part of google[X/Y].
			int tileX = (int) googleX;
			int tileY = (int) googleY;
			// Offset is the remaining part.
			int offsetX = -(int) Math.round((googleX - tileX) * tileSize);
			int offsetY = -(int) Math.round((googleY - tileY) * tileSize);

			drawTiles(canvas, tileX, tileY, offsetX, offsetY, zoomLevel,
					tileSize, true);

			Log.v(Config.TAG, "googleX: " + googleX + ", googleY: " + googleY);

		}

		private void drawTiles(Canvas canvas, int tileX, int tileY,
				int offsetX, int offsetY, int zoomLevel, int tileSize,
				boolean fillScreen) {

			Rect src = new Rect();
			Rect dest = new Rect();

			int tileRow, tileCol;

			int numRows;
			int numColumns;
			//TODO(Josh) is this necessary
			if (fillScreen && tileSize < IMAGE_TILE_SIZE - 2) {
				// this a crude way to do the calculation
				// but anything else seems to be noticably slow
				numRows = pNumRows + 1;
				numColumns = pNumColumns + 2;
			} else {
				numRows = pNumRows;
				numColumns = pNumColumns;
			}
			
			Log.v(Config.TAG, "DRAWING TILES (" + numRows + "x" + numColumns + ")");
			
			if (bm == null) {
				bm = BitmapFactory.decodeResource(getResources(), R.drawable.sample_tile);
			}

			for (int row = 0; row < numRows + 1; row++) {
				for (int col = 0; col < numColumns + 1; col++) {

					tileRow = row + tileY;
					tileCol = col + tileX;

					if (!isTileOnMap(tileCol, tileRow, zoomLevel)) {
						continue;
					}

					/*
					if (mtm.TileOutOfBounds(tileCol, numRows, zoomLevel)) {
						continue;
					}
					*/

					int tileOriginX = col * tileSize + offsetX;
					int tileOriginY = row * tileSize + offsetY;

					//bm = mtm.getBitmap(tileCol, tileRow, zoomLevel, true);

					if (bm != null) {

						src.bottom = IMAGE_TILE_SIZE;
						src.left = 0;
						src.right = IMAGE_TILE_SIZE;
						src.top = 0;

						dest.bottom = tileOriginY + tileSize;
						dest.left = tileOriginX;
						dest.right = tileOriginX + tileSize;
						dest.top = tileOriginY;

						canvas.drawBitmap(bm, src, dest, null);

					} else {

						boolean block = (tileSize != IMAGE_TILE_SIZE)
								&& (bm == null);

						//bm = mtm.fetchBitmapOnThread(tileCol, tileRow,
						//		zoomLevel, block);

						if (bm != null) {
							src.bottom = IMAGE_TILE_SIZE;
							src.left = 0;
							src.right = IMAGE_TILE_SIZE;
							src.top = 0;

							dest.bottom = tileOriginY + tileSize;
							dest.left = tileOriginX;
							dest.right = tileOriginX + tileSize;
							dest.top = tileOriginY;

							canvas.drawBitmap(bm, src, dest, null);
						}
					}
				}

			}
		}
	}

	// TODO(josh) If only one zoom level arrays aren't necessary.
	// Tile boundaries are calculated per zoom level to avoid doing calculations
	// when no map tiles are available.
	private int[] pWestX = new int[22];
	private int[] pEastX = new int[22];
	private int[] pNorthY = new int[22];
	private int[] pSouthY = new int[22];

	private boolean isTileOnMap(int tileX, int tileY, int zoomLevel) {
		initZoomLevel(zoomLevel);

		if (tileX < pWestX[zoomLevel] || tileX > pEastX[zoomLevel]
				|| tileY < pNorthY[zoomLevel] || tileY > pSouthY[zoomLevel]) {
			return false;
		}

		return true;
	}

	private void initZoomLevel(int zoomLevel) {
		if (pWestX[zoomLevel] == 0) {
			pWestX[zoomLevel] = (int) Math.floor(computeGoogleX(
					WEST_LONGITUDE_E6, zoomLevel));
		}

		if (pEastX[zoomLevel] == 0) {
			pEastX[zoomLevel] = (int) Math.ceil(computeGoogleX(
					EAST_LONGITUDE_E6, zoomLevel));
		}

		if (pNorthY[zoomLevel] == 0) {
			pNorthY[zoomLevel] = (int) Math.floor(computeGoogleY(
					NORTH_LATITUDE_E6, zoomLevel));
		}

		if (pSouthY[zoomLevel] == 0) {
			pSouthY[zoomLevel] = (int) Math.ceil(computeGoogleY(
					SOUTH_LATITUDE_E6, zoomLevel));
		}
	}

	private int computeTileSize(MapView mapView, int zoomLevel) {
		Projection projection = mapView.getProjection();

		GeoPoint topLeftPoint = projection.fromPixels(0, 0);
		double googleX = computeGoogleX(topLeftPoint.getLongitudeE6(),
				zoomLevel);
		double nextTileGoogleX = googleX + 1;
		int nextTileLongitudeE6 = computeLongitudeE6(nextTileGoogleX, zoomLevel);
		GeoPoint nextPoint = new GeoPoint(topLeftPoint.getLatitudeE6(),
				nextTileLongitudeE6);
		Point nextTilePoint = projection.toPixels(nextPoint, null);
		return nextTilePoint.x;
	}

	private static int computeLongitudeE6(double googleX, int zoomLevel) {
		double longitude = -180. + (360. * googleX) / Math.pow(2.0, zoomLevel);
		return (int) Math.round(longitude * 1000000.);
	}

	private static int computeLatitudeE6(double googleY, int zoomLevel) {
		double mercatorY = Math.PI
				* (1 - 2 * (googleY / Math.pow(2.0, zoomLevel)));
		double phi = Math.atan(Math.sinh(mercatorY));

		// Convert from radians to microdegrees.
		return (int) Math.round(phi * 180. / Math.PI * 1000000.);
	}

	private double computeGoogleX(int longitudeE6, int zoomLevel) {
		return (180. + ((double) longitudeE6 / 1000000.)) / (360.)
				* Math.pow(2.0, zoomLevel);
	}

	private double computeGoogleY(int latitudeE6, int zoomLevel) {
		// Convert to radians.
		double phi = (double) latitudeE6 / 1000000. * Math.PI / 180.;
		// Calculate Mercator coordinate.
		double mercatorY = Math.log(Math.tan(phi) + 1. / Math.cos(phi));
		// Rescale to Google coordinate.
		return (Math.PI - mercatorY) / (2. * Math.PI)
				* Math.pow(2.0, zoomLevel);
	}

}
