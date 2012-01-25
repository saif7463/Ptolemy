package edu.mit.pt.bookmarks;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import edu.mit.pt.ActionBar;
import edu.mit.pt.R;
import edu.mit.pt.VerticalTextView;
import edu.mit.pt.data.PlacesTable;
import edu.mit.pt.data.PtolemyDBOpenHelperSingleton;
import edu.mit.pt.maps.PtolemyMapActivity;

public class BookmarksActivity extends ListActivity {

	ResourceCursorAdapter adapter;
	private final String ACTIVITY_TILE = "Bookmarks";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookmarks);

		ActionBar.setTitle(this, ACTIVITY_TILE);
		ActionBar.setDefaultBackAction(this);

		final Activity that = this;

		// Add nav button.
		ImageButton addButton = (ImageButton) getLayoutInflater().inflate(
				R.layout.menu_nav_button, null);
		addButton.setImageResource(R.drawable.ic_menu_bookmark_add);
		addButton.setContentDescription(getString(R.string.add_bookmark));
		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(that, AddBookmarkActivity.class);
				startActivity(intent);
			}
		});
		ActionBar.setButton(this, addButton);

		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Cursor cursor = (Cursor) parent.getItemAtPosition(position);
				long placeId = cursor.getLong(cursor
						.getColumnIndex(PlacesTable.PLACES_TABLE_NAME
								+ BaseColumns._ID));
				Intent intent = new Intent(that, PtolemyMapActivity.class);
				Uri.Builder builder = Uri
						.parse("content://edu.mit.pt.data.placescontentprovider/")
						.buildUpon().path(Long.toString(placeId));
				intent.setData(builder.build());
				intent.setAction(Intent.ACTION_SEARCH);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
			}
		});

		SQLiteDatabase db = PtolemyDBOpenHelperSingleton
				.getPtolemyDBOpenHelper(this).getReadableDatabase();
		// Select bookmarks_table.id as _ID and places_table.id as
		// places_table._ID
		Cursor cur = db.rawQuery("SELECT "
				+ BookmarksTable.BOOKMARKS_TABLE_NAME + "."
				+ BookmarksTable.COLUMN_ID + " AS " + BaseColumns._ID + ", "
				+ BookmarksTable.COLUMN_NAME + ","
				+ BookmarksTable.BOOKMARKS_TABLE_NAME + "."
				+ BookmarksTable.COLUMN_TYPE + " AS "
				+ BookmarksTable.COLUMN_TYPE + ", " + PlacesTable.COLUMN_NAME
				+ ", " + PlacesTable.PLACES_TABLE_NAME + "."
				+ PlacesTable.COLUMN_ID + " AS "
				+ PlacesTable.PLACES_TABLE_NAME + BaseColumns._ID + " FROM "
				+ BookmarksTable.BOOKMARKS_TABLE_NAME + ", "
				+ PlacesTable.PLACES_TABLE_NAME + " WHERE "
				+ BookmarksTable.BOOKMARKS_TABLE_NAME + "."
				+ BookmarksTable.COLUMN_PLACE_ID + "="
				+ PlacesTable.PLACES_TABLE_NAME + "." + PlacesTable.COLUMN_ID,
				null);

		adapter = new BookmarksListItemAdapter(this,
				R.layout.bookmark_list_item, cur, true);

		// new String[] { BookmarksTable.COLUMN_NAME,
		// PlacesTable.COLUMN_NAME, BaseColumns._ID }, new int[] {
		// R.id.name, R.id.location, R.id.idtext });

		setListAdapter(adapter);
	}

	@Override
	public void onStart() {
		super.onStart();
		adapter.getCursor().requery();
	}

	private class BookmarksListItemAdapter extends ResourceCursorAdapter {
		public BookmarksListItemAdapter(Context context, int layout, Cursor c,
				boolean autoRequery) {
			super(context, layout, c, autoRequery);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			String bookmarkName = cursor.getString(cursor
					.getColumnIndex(BookmarksTable.COLUMN_NAME));
			String locationName = cursor.getString(cursor
					.getColumnIndex(PlacesTable.COLUMN_NAME));
			String typeName = cursor.getString(cursor
					.getColumnIndex(BookmarksTable.COLUMN_TYPE));
			BookmarkType type = BookmarkType.valueOf(typeName);
			((TextView) view.findViewById(R.id.name)).setText(bookmarkName);
			((TextView) view.findViewById(R.id.location)).setText(locationName);
			switch (type) {
			case LECTURE:
				((VerticalTextView) view.findViewById(R.id.label)).setText(type
						.getShortName());
				view.findViewById(R.id.label_wrapper).setBackgroundColor(
						Color.parseColor("#b6db49"));
				break;
			case RECITATION:
				((VerticalTextView) view.findViewById(R.id.label)).setText(type
						.getShortName());
				view.findViewById(R.id.label_wrapper).setBackgroundColor(
						Color.parseColor("#6dcaec"));
				break;
			case OFFICE_HOURS:
				((VerticalTextView) view.findViewById(R.id.label)).setText(type
						.getShortName());
				break;
			case OTHER:
				break;
			}
		}
	}

}
