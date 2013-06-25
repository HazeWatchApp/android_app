package me.ebernie.mapi.db;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import me.ebernie.mapi.model.AirPolutionIndex;

import org.json.JSONArray;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@SuppressLint("SimpleDateFormat")
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = DatabaseHelper.class.getName();

	private static DatabaseHelper instance;

	private static final int SCHEMA_VERSION = 1;
	private static final String DB_NAME = "mapi.db";
	private final Context context;
	private RequestQueue rqueue;
	private static final String HAZE_URL = "http://66.175.221.183/hazewatch/";
	private static final String COL_AREA = "area";
	private static final String COL_TOWN = "town";
	private static final String COL_STATE = "state";
	private static final String COL_TIME1 = "time1";
	private static final String COL_TIME2 = "time2";
	private static final String COL_TIME3 = "time3";
	private static final String TABLE_DATA_API = "api_data";
	private static final String COL_LAST_UPDATE = "last_update";
	private static final String TABLE_UPDATE_TIME = "update_times";

	public DatabaseHelper(Context context, CursorFactory factory) {
		super(context, DB_NAME, factory, SCHEMA_VERSION);
		this.context = context;
	}

	public static final synchronized DatabaseHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DatabaseHelper(context, null);
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			db.beginTransaction();
			/*
			 * Update time table
			 */
			db.execSQL("CREATE TABLE update_times (_id INTEGER PRIMARY KEY AUTOINCREMENT, last_update TEXT);");
			/*
			 * API data
			 */
			db.execSQL("CREATE TABLE api_data (_id INTEGER PRIMARY KEY AUTOINCREMENT, area TEXT, state TEXT, time1 TEXT, time2 TEXT, time3 TEXT);");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// drop and recreate everything
		db.execSQL("DROP TABLE IF EXISTS api_data");
		db.execSQL("DROP TABLE IF EXISTS update_times");
		onCreate(db);
	}

	public interface PersistableDataListener {
		void updateList(List<AirPolutionIndex> index);
	}

	public void saveIndex(List<AirPolutionIndex> indices) {
		new SaveIndexTask().execute(indices.toArray());
	}

	private class SaveIndexTask extends AsyncTask<Object, Void, Void> {

		@Override
		protected Void doInBackground(Object... params) {
			SQLiteDatabase db = getWritableDatabase();
			ContentValues values = new ContentValues();
			try {
				db.beginTransaction();
				db.delete(TABLE_DATA_API, null, null);
				// store indices
				for (int i = 0; i < params.length; i++) {
					AirPolutionIndex index = (AirPolutionIndex) params[i];
					values.put(COL_AREA, index.getArea());
					values.put(COL_STATE, index.getState());
					values.put(COL_TIME1, index.getTime1());
					values.put(COL_TIME2, index.getTime2());
					values.put(COL_TIME3, index.getTime3());
					db.insert(TABLE_DATA_API, COL_AREA, values);
				}

				// store last update time
				values.clear();
				values.put(COL_LAST_UPDATE, Calendar.getInstance().getTime()
						.getTime());
				Cursor updateTime = getReadableDatabase().rawQuery(
						"SELECT " + COL_LAST_UPDATE + " FROM update_times;",
						null);
				if (updateTime != null && updateTime.getCount() > 0) {
					db.update(TABLE_UPDATE_TIME, values, null, null);
				} else {
					db.insert(TABLE_UPDATE_TIME, COL_LAST_UPDATE, values);
				}
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			return null;
		}

	}

	private class GetIndexTask extends
			AsyncTask<Void, Void, List<AirPolutionIndex>> {

		private final PersistableDataListener listener;

		public GetIndexTask(PersistableDataListener listener) {
			this.listener = listener;
		}

		@Override
		protected List<AirPolutionIndex> doInBackground(Void... params) {

			Cursor updateTime = null;
			Cursor indexCursor = null;
			List<AirPolutionIndex> indices = new ArrayList<AirPolutionIndex>();
			boolean networkFetch = false;

			try {
				// check if last update time was too long ago
				updateTime = getReadableDatabase().rawQuery(
						"SELECT " + COL_LAST_UPDATE + " FROM update_times;",
						null);
				if (updateTime.getCount() > 0) {
					updateTime.moveToFirst();
					long time = updateTime.getLong(updateTime
							.getColumnIndex(COL_LAST_UPDATE));
					Calendar cal = Calendar.getInstance();
					cal.setTime(new Date(time));
					cal.add(Calendar.MINUTE, 1);
					Date curTime = Calendar.getInstance().getTime();
					if (!curTime.after(cal.getTime())) {
						// fetch from network if there's no fresh data
						getIndexFromNetwork();
						networkFetch = true;
					}
				} else {
					// first time running this app
					getIndexFromNetwork();
					networkFetch = true;
				}

				if (!networkFetch) {
					// return db values
					indexCursor = getReadableDatabase().rawQuery(
							"SELECT * FROM api_data", null);
					indexCursor.moveToFirst();
					while (!indexCursor.isAfterLast()) {
//						String town = indexCursor.getString(indexCursor
//								.getColumnIndex(COL_TOWN));
						String area = indexCursor.getString(indexCursor
								.getColumnIndex(COL_AREA));
						String state = indexCursor.getString(indexCursor
								.getColumnIndex(COL_STATE));
						String time1 = indexCursor.getString(indexCursor
								.getColumnIndex(COL_TIME1));
						String time2 = indexCursor.getString(indexCursor
								.getColumnIndex(COL_TIME2));
						String time3 = indexCursor.getString(indexCursor
								.getColumnIndex(COL_TIME3));
						AirPolutionIndex index = new AirPolutionIndex(area,
								state, time1, time2, time3);
						indices.add(index);
						indexCursor.moveToNext();
					}
				}

			} finally {
				if (updateTime != null) {
					updateTime.close();
				}
				if (indexCursor != null) {
					indexCursor.close();
				}
			}

			return indices;
		}

		@Override
		protected void onPostExecute(List<AirPolutionIndex> result) {
			listener.updateList(result);
		}

		private ErrorListener errorListener = new ErrorListener() {

			@Override
			public void onErrorResponse(final VolleyError error) {
				((Activity) context).runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(context, "Unable to fetch API data",
								Toast.LENGTH_LONG).show();
						Log.e(TAG, error.getMessage(), error);
					}
				});
			}
		};

		private void getIndexFromNetwork() {
			if (rqueue == null) {
				rqueue = Volley.newRequestQueue(context);
			}
			fetchData();
			// new JsonObjectRequest(url, jsonRequest, listener, errorListener)
			// rqueue.add(new JsonObjectRequest(HAZE_URL, new JSONObject(),
			// new Response.Listener<JSONObject>() {
			// @Override
			// public void onResponse(JSONObject jsonObject) {
			// try {
			// if (!"204".equals(jsonObject.get("status"))) {
			// fetchData();
			// }
			// } catch (JSONException e) {
			// e.printStackTrace();
			// }
			// }
			// }, errorListener));
		}

		public void fetchData() {
			// SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			// String date = df.format(new Date());
			// TODO for testing, remove me!
			// String param = "?date=" + "2013-06-24";

			rqueue.add(new JsonArrayRequest(HAZE_URL,
					new Response.Listener<JSONArray>() {
						@Override
						public void onResponse(JSONArray jsonArray) {
							Gson gson = new Gson();
							Type type = new TypeToken<List<AirPolutionIndex>>() {
							}.getType();
							List<AirPolutionIndex> indices = gson.fromJson(
									jsonArray.toString(), type);
							listener.updateList(indices);
							// also perform a save to the db after a network
							// fetch
							DatabaseHelper.getInstance(context).saveIndex(
									indices);
						}
					}, errorListener));
		}

	}
	
	public void getIndexFromNetwork(PersistableDataListener listener) {
		new GetIndexFromNetworkTask(listener).execute();
	}

	private class GetIndexFromNetworkTask extends
			AsyncTask<Void, Void, Void> {

		private final PersistableDataListener listener;

		public GetIndexFromNetworkTask(PersistableDataListener listener) {
			this.listener = listener;
		}

		@Override
		protected Void doInBackground(Void... params) {

			if (rqueue == null) {
				rqueue = Volley.newRequestQueue(context);
			}

			// SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			// String date = df.format(new Date());
			// TODO for testing, remove me!
			// String param = "?date=" + "2013-06-24";

			rqueue.add(new JsonArrayRequest(HAZE_URL,
					new Response.Listener<JSONArray>() {
						@Override
						public void onResponse(JSONArray jsonArray) {
							Gson gson = new Gson();
							Type type = new TypeToken<List<AirPolutionIndex>>() {
							}.getType();
							List<AirPolutionIndex> indices = gson.fromJson(
									jsonArray.toString(), type);
							listener.updateList(indices);
							// also perform a save to the db after a network
							// fetch
							DatabaseHelper.getInstance(context).saveIndex(
									indices);
						}
					}, errorListener));
			
			return null;
		}

		private ErrorListener errorListener = new ErrorListener() {

			@Override
			public void onErrorResponse(final VolleyError error) {
				((Activity) context).runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(context, "Unable to fetch API data",
								Toast.LENGTH_LONG).show();
						Log.e(TAG, error.getMessage(), error);
					}
				});
			}
		};

	}

	public void getIndex(PersistableDataListener listener) {
		new GetIndexTask(listener).execute();
	}

}
