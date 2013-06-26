package me.ebernie.mapi.db;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
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
	private static RequestQueue rqueue;
	private static final String HAZE_URL = "http://66.175.221.183/hazewatch/";
	private static final String COL_AREA = "area";
	// private static final String COL_TOWN = "town";
	private static final String COL_STATE = "state";
	private static final String COL_TIME1 = "time1";
	private static final String COL_TIME2 = "time2";
	private static final String COL_TIME3 = "time3";
	private static final String TABLE_DATA_API = "api_data";
	private static final String COL_LAST_UPDATE = "last_update";
	private static final String TABLE_UPDATE_TIME = "update_times";
	// 2 hours
	private static final int UPDATE_DURATION_IN_MIL = 1000 * 60 * 60 * 2;

	public DatabaseHelper(Context context, CursorFactory factory) {
		super(context, DB_NAME, factory, SCHEMA_VERSION);
		this.context = context;
	}

	public static final synchronized DatabaseHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DatabaseHelper(context, null);
		}
		if (rqueue == null) {
			rqueue = Volley.newRequestQueue(context);
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
		void setUpdateDate(Date date);
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
					values.put(COL_TIME1, index.getSevenAmIndex());
					values.put(COL_TIME2, index.getElevenAmIndex());
					values.put(COL_TIME3, index.getFivePmIndex());
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
				try {
					db.endTransaction();
				} catch (Exception e) {
					Log.e(TAG, "Unable to save index", e);
				}
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
					Calendar timeToUpdate = Calendar.getInstance();
					timeToUpdate.setTime(new Date(time));
					timeToUpdate.add(Calendar.MILLISECOND, UPDATE_DURATION_IN_MIL);
					Date curTime = Calendar.getInstance().getTime();
					if (curTime.after(timeToUpdate.getTime())) {
						// fetch from network if there's no fresh data
						getIndexFromNetwork(listener);
						networkFetch = true;
					}
				} else {
					// first time running this app
					getIndexFromNetwork(listener);
					networkFetch = true;
				}

				if (!networkFetch) {
					// return db values
					indexCursor = getReadableDatabase().rawQuery(
							"SELECT * FROM api_data", null);
					indexCursor.moveToFirst();
					while (!indexCursor.isAfterLast()) {
						// String town = indexCursor.getString(indexCursor
						// .getColumnIndex(COL_TOWN));
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
	}

	private class RetryingErrorListener implements Response.ErrorListener {

		private final PersistableDataListener listener;
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		public RetryingErrorListener(PersistableDataListener listener) {
			this.listener = listener;
		}

		@Override
		public void onErrorResponse(final VolleyError error) {

			((Activity) context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.e(TAG, error.getMessage(), error);
					Calendar yesterday = Calendar.getInstance();
					yesterday.set(Calendar.DAY_OF_MONTH,
							yesterday.get(Calendar.DAY_OF_MONTH) - 1);
					String param = "?date=" + sdf.format(yesterday.getTime());
					getIndexFromNetwork(listener, param);
					listener.updateList(null);
				}
			});
		}
	}
	
	private class NonRetryingErrorListener implements Response.ErrorListener {

		private final PersistableDataListener listener;

		public NonRetryingErrorListener(PersistableDataListener listener) {
			this.listener = listener;
		}

		@Override
		public void onErrorResponse(final VolleyError error) {

			((Activity) context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.e(TAG, error.getMessage(), error);
					listener.updateList(null);
				}
			});
		}
	}

	private RetryingErrorListener retryingErrorListener;
	private NonRetryingErrorListener nonRetryingErrorListener;

	public void getIndexFromNetwork(final PersistableDataListener listener,
			String params) {
		String requestUri = params == null ? HAZE_URL : HAZE_URL + params;
		if (retryingErrorListener == null) {
			retryingErrorListener = new RetryingErrorListener(listener);
		}
		
		if (nonRetryingErrorListener == null) {
			nonRetryingErrorListener = new NonRetryingErrorListener(listener);
		}
		
		Response.ErrorListener errorListener; 
		// if the request is for today (no params), then it is possible
		// that we will fail, hence the error listener will attempt to retry
		if (params == null) {
			errorListener = retryingErrorListener;
		} else {
			errorListener = nonRetryingErrorListener;
		}
		rqueue.add(new JsonArrayRequest(requestUri,
				new Response.Listener<JSONArray>() {
					@Override
					public void onResponse(JSONArray jsonArray) {
						Gson gson = new Gson();
						Type type = new TypeToken<List<AirPolutionIndex>>() {
						}.getType();
						List<AirPolutionIndex> indices = gson.fromJson(
								jsonArray.toString(), type);
						if (!indices.isEmpty()) {
							listener.setUpdateDate(new Date());
						}
						listener.updateList(indices);
						// also perform a save to the db after a network
						// fetch
						DatabaseHelper.getInstance(context).saveIndex(indices);
					}
				}, errorListener));
	}

	public void getIndexFromNetwork(final PersistableDataListener listener) {
		getIndexFromNetwork(listener, null);
	}

	public void getIndex(PersistableDataListener listener) {
		new GetIndexTask(listener).execute();
	}

}
