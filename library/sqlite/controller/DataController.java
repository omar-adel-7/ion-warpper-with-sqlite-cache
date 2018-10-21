package com.freelanceapps33.weather.sqlite.controller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import com.freelanceapps33.weather.sqlite.DatabaseHelper;
import com.freelanceapps33.weather.sqlite.listener.DBResultListener;
import org.json.JSONException;

public class DataController {
	
	private static final String REQUEST =  "request";
	private static final String PARAMS =  "params";
	private static final String RESULT =  "result";
	private static final String LAST_DATE =  "lastDate";
	
	private static final int RESULT_COLUM_INDEX =  2;
	
	private final Context context;
	
	public DataController(Context context) {
		this.context = context;
	}
	
	public void getData(String request, String params, DBResultListener listener) {
		new GetDataAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request, params, listener);
	}
	
	public void saveData(String request, String params, String result, long lastDate) {
		new SaveDataAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, request, params, result, lastDate);
	}

	private void pSaveData(String request, String params, String result, long lastDate) {
		
		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(REQUEST, request);
	    values.put(PARAMS, params);
	    values.put(RESULT, result);
	    values.put(LAST_DATE, lastDate);
		
		// Update row if exists, otherwise insert new data
		if (hasRequest(databaseHelper, db, request, params)) {
			db.update(databaseHelper.getTableName(), values, REQUEST + " LIKE '" + request
					+ "' AND " + PARAMS + " LIKE '" + params + "'", null);
			Log.d("DEBUG", "pUpdateData : " + request);
		} else {
			db.insert(databaseHelper.getTableName(), null, values);
			Log.d("DEBUG", "pSaveData : " + request);
		}

		db.close();
	}

	public void clearCachedData() {
		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + databaseHelper.getTableName());
        db.execSQL(databaseHelper.createTableRequest());
		db.close();
	}
	
	private String pGetData(String request, String params) throws JSONException {
		
		Log.d("DEBUG", "pGetData : " + request + "(" + params + ")");
		
		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		SQLiteDatabase db = databaseHelper.getReadableDatabase();

		String result = null;
		Cursor cursor = db.rawQuery("SELECT * FROM " + databaseHelper.getTableName() + " WHERE "
				+ REQUEST + " LIKE '" + request + "' AND " + PARAMS + " LIKE '" + params + "'", null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				result = cursor.getString(RESULT_COLUM_INDEX);
			}
			cursor.close();
		}
		
		Log.d("DEBUG", "pGetData result : " + result);
		
		db.close();
		return result;
	}

	private boolean hasRequest(DatabaseHelper databaseHelper, SQLiteDatabase db, String request, String params) {
		boolean hasRequest = false;
		Cursor cursor = db.rawQuery("SELECT * FROM " + databaseHelper.getTableName() + " WHERE "
				+ REQUEST + " LIKE '" + request + "' AND " + PARAMS + " LIKE '" + params + "'", null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				hasRequest = true;
			}
			cursor.close();
		}
		return hasRequest;
	}
	
	private class SaveDataAsyncTask extends AsyncTask<Object, String, Void> {

		@Override
		protected Void doInBackground(Object... args) {
			String request = (String) args[0];
			String params = (String) args[1];
			String result = (String) args[2];
			long lastDate = (Long) args[3];
			pSaveData(request, params, result, lastDate);
			return null;
		}
	}
	
	private class GetDataAsyncTask extends AsyncTask<Object, String, String> {

		private DBResultListener listener;

		@Override
		protected String doInBackground(Object... args) {
			String request = (String) args[0];
			String params = (String) args[1];
			listener = (DBResultListener) args[2];
			String result;
			try {
				result = pGetData(request, params);
			} catch (Exception e) {
				Log.e(getClass().getCanonicalName(), e.getMessage());
				return null;
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (listener != null) {
				listener.onDBResult(result);
			}
		}
	}
}
