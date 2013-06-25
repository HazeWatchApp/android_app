package me.ebernie.mapi.api;

import me.ebernie.mapi.db.DatabaseHelper;
import me.ebernie.mapi.db.DatabaseHelper.PersistableDataListener;
import android.content.Context;

public enum DataApi {

	INSTANCE;

	private DatabaseHelper db;

	public void getIndex(final Context context,
			final PersistableDataListener listener, boolean forceRefresh) {
		if (db == null) {
			db = DatabaseHelper.getInstance(context);
		}
		if (!forceRefresh) {
			// we see if there's fresh data in the db
			db.getIndex(listener);
		} else {
			// fetch from network
			db.getIndexFromNetwork(listener);
		}
	}

	public void getIndex(final Context context,
			final PersistableDataListener listener) {
		getIndex(context, listener, false);
	}

	public void destroy() {
		if (db != null) {
			db.close();
		}
	}

}
