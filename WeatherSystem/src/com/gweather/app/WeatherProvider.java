package com.gweather.app;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class WeatherProvider extends ContentProvider {
	private static final String TAG = "Gweather.WeatherProvider";
	
	private static final String AUTHORITIES = "com.gweather.app.weather";
	private static final String TABLE_WEATHER = "gweather";

	private static final String TABLE_CITY = "gcity";
	private static final int WEATHER = 1;
	private static final int ITEM = 2;
	private static final int CITY = 3;
	private static final int CITY_ITEM= 4;

	public static final int DEFAULT_CITY = 666;
	
	public static final int CONDITION_INDEX = -1;
	public static final int FLAG_GPS = 2333;
	
	public static final String INDEX = "gIndex";
	public static final String WOEID = "woeid";
	public static final String NAME = "name";
	public static final String CODE = "code";
	public static final String DATE = "date";
	public static final String DAY = "day";
	public static final String TEMP = "tmp";
	public static final String HIGH = "high";
	public static final String LOW = "low";
	public static final String TEXT = "text";
	public static final String GPS = "isGps";
	public static final String UPDATE_TIME = "updateTime";
	
	public static final String CITY_WOEID = "woeid";
	public static final String CITY_NAME = "name";
	public static final String CITY_LAT = "lat";
	public static final String CITY_LON = "lon";
	public static final String CITY_SWLAT = "southWestLat";
	public static final String CITY_SWLON = "southWestLon";
	public static final String CITY_NELAT = "northEastLat";
	public static final String CITY_NELON = "northEastLon";
	
	private DBHelper dbHelper;
	
	private static UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITIES, "gweather", WEATHER);
		uriMatcher.addURI(AUTHORITIES, "gweather/#", ITEM);
		uriMatcher.addURI(AUTHORITIES, "gcity", CITY);
		uriMatcher.addURI(AUTHORITIES, "gcity/#", CITY_ITEM);
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;
		switch (uriMatcher.match(uri)) {
		case WEATHER:
			count = db.delete(TABLE_WEATHER, selection, selectionArgs);
			return count;
		case ITEM:
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			count = db.delete(TABLE_WEATHER, where, selectionArgs);
			return count;
		case CITY:
			count = db.delete(TABLE_CITY, selection, selectionArgs);
			return count;
		default:
			break;
		}
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case WEATHER:
			return AUTHORITIES + "/gweather";
		case ITEM:
			return AUTHORITIES + "/gweather";
		case CITY:
			return AUTHORITIES + "/gcity";
		case CITY_ITEM:
			return AUTHORITIES + "/gcity";
		default:
			throw new IllegalArgumentException("Unknow Uri: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.i(TAG, "insert: " + values + "\nuri: " + uri);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (uriMatcher.match(uri)) {
		case WEATHER: {
			long rowId = db.insert(TABLE_WEATHER, null, values);
			if (rowId < 0) {
				throw new SQLiteException("Unable to insert " + values + " for " + uri);
			}
			Uri insertUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(insertUri, null);
			return insertUri;
		}
		case CITY: {
			long rowId = db.insert(TABLE_CITY, null, values);
			if (rowId < 0) {
				throw new SQLiteException("Unable to insert " + values + " for " + uri);
			}
			Uri insertUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(insertUri, null);
			return insertUri;
		}
		default:
			throw new IllegalArgumentException("Unknow Uri: " + uri);
		}
	}

	@Override
	public boolean onCreate() {
		Log.d(TAG, "WeatherProvider > onCreate");
		dbHelper = new DBHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Log.d(TAG, "query");
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		switch (uriMatcher.match(uri)) {
		case WEATHER:
			Log.d(TAG, "WEATHER");
			return database.query(TABLE_WEATHER, projection, selection, selectionArgs,
					null, null, sortOrder);

		case ITEM: {
			Log.d(TAG, "ITEM");
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			if (selection != null && !"".equals(selection)) {
				where = selection + " and " + where;
			}

			return database.query(TABLE_WEATHER, projection, where, selectionArgs,
					null, null, sortOrder);
		}
			
		case CITY:
			Log.d(TAG, "CITY");
			return database.query(TABLE_CITY, projection, selection, selectionArgs,
					null, null, sortOrder);
		case CITY_ITEM: {
			Log.d(TAG, "CITY_ITEM");
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			if (selection != null && !"".equals(selection)) {
				where = selection + " and " + where;
			}

			return database.query(TABLE_CITY, projection, where, selectionArgs,
					null, null, sortOrder);
			}
		default:
			throw new IllegalArgumentException("Unknow Uri: " + uri);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;
		switch (uriMatcher.match(uri)) {
		case WEATHER:
			count = db.update(TABLE_WEATHER, values, selection, selectionArgs);
			return count;
		case ITEM:
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			if (selection != null && !"".equals(selection)) {
				where = selection + " and " + where;
			}
			count = db.update(TABLE_WEATHER, values, where, selectionArgs);
			return count;
		case CITY:
			count = db.update(TABLE_CITY, values, selection, selectionArgs);
			return count;
		default:
			throw new IllegalArgumentException("Unknow Uri: " + uri);
		}
	}
	
	class DBHelper extends SQLiteOpenHelper {

		private static final String DATABASE_NAME = "gweather.db";
		private static final int DATABASE_VERSION = 3;
		
		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "DB Create");
			db.execSQL("CREATE TABLE "+ TABLE_WEATHER+ "("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT," 
					+ "gIndex INTEGER,"
					+ "woeid TEXT NOT NULL,"
					+ "name TEXT,"
					+ "code TEXT NOT NULL,"
					+ "date TEXT,"
					+ "day TEXT,"
					+ "tmp TEXT,"
					+ "high TEXT,"
					+ "low TEXT,"
					+ "text TEXT,"
					+ "isGps INTEGER DEFAULT '0',"
					+ "updateTime INTEGER);"
					);
			
			db.execSQL("CREATE TABLE "+ TABLE_CITY+ "("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT," 
					+ "woeid TEXT NOT NULL,"
					+ "name TEXT NOT NULL,"
					+ "lat TEXT,"
					+ "lon TEXT,"
					+ "southWestLat TEXT,"
					+ "southWestLon TEXT,"
					+ "northEastLat TEXT,"
					+ "northEastLon TEXT);"
					);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS gweather");
			db.execSQL("DROP TABLE IF EXISTS gcity");
			onCreate(db);
		}
		
	}
}
