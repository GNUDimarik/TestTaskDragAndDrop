package com.test.draganddroptask;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class MyContentProvider extends ContentProvider {

    static private final String TAG = "MyContentProvider";

    static private final String DB_NAME = "mydb";
    static private final int DB_VERSION = 1;

    static private final String DATA_TABLE = "data";

    static private final String DB_CREATE = "create table " + DATA_TABLE + "("
            + Constants.DATA_ID + " integer primary key autoincrement, "
            + Constants.DATA_TITLE + " text, "
            + Constants.DATA_NEXT + " integer default NULL, "
            + Constants.DATA_PREV + " integer default NULL "
            + ");";

    static private final String DATA_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + Constants.AUTHORITY + "." + Constants.DATA_PATH;

    static private final String DATA_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + Constants.AUTHORITY + "." + Constants.DATA_PATH;

    static private final int URI_DATA = 1;
    static private final int URI_DATA_ID = 2;

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(Constants.AUTHORITY, Constants.DATA_PATH, URI_DATA);
        uriMatcher.addURI(Constants.AUTHORITY, Constants.DATA_PATH + "/#", URI_DATA_ID);
    }

    private class DBHelper extends SQLiteOpenHelper {

        static private final int ITEMS_NUMBER = 5;

        public DBHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_CREATE);
            ContentValues cv = new ContentValues();

            for (int i = 1; i <= ITEMS_NUMBER; i++) {
                cv.put(Constants.DATA_TITLE, "title " + i);
                cv.put(Constants.DATA_NEXT, i == ITEMS_NUMBER ? null : i + 1);
                cv.put(Constants.DATA_PREV, i == 1 ? null : i - 1);
                db.insert(DATA_TABLE, null, cv);
            }
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    DBHelper dbHelper;
    SQLiteDatabase db;

    public boolean onCreate() {
        dbHelper = new DBHelper(getContext());
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query, " + uri.toString());
        switch (uriMatcher.match(uri)) {
            case URI_DATA:
                Log.d(TAG, "URI_DATA");
                // Read sort order from db
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Constants.DATA_PREV + " ASC";
                }
                break;
            case URI_DATA_ID: // Uri с ID
                String id = uri.getLastPathSegment();
                Log.d(TAG, "URI_DATA_ID, " + id);

                if (TextUtils.isEmpty(selection)) {
                    selection = Constants.DATA_ID + " = " + id;
                } else {
                    selection = selection + " AND " + Constants.DATA_ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(DATA_TABLE, projection, selection,
                selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(),
                Constants.DATA_CONTENT_URI);
        return cursor;
    }

    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert, " + uri.toString());
        if (uriMatcher.match(uri) != URI_DATA)
            throw new IllegalArgumentException("Wrong URI: " + uri);

        db = dbHelper.getWritableDatabase();
        long rowID = db.insert(DATA_TABLE, null, values);
        Uri resultUri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, rowID);
        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "delete, " + uri.toString());
        switch (uriMatcher.match(uri)) {
            case URI_DATA:
                Log.d(TAG, "URI_DATA");
                break;
            case URI_DATA_ID:
                String id = uri.getLastPathSegment();
                Log.d(TAG, "URI_DATA_ID, " + id);
                if (TextUtils.isEmpty(selection)) {
                    selection = Constants.DATA_ID + " = " + id;
                } else {
                    selection = selection + " AND " + Constants.DATA_ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db = dbHelper.getWritableDatabase();
        int cnt = db.delete(DATA_TABLE, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        Log.d(TAG, "update, " + uri.toString());
        switch (uriMatcher.match(uri)) {
            case URI_DATA:
                Log.d(TAG, "URI_DATA");
                break;
            case URI_DATA_ID:
                String id = uri.getLastPathSegment();
                Log.d(TAG, "URI_DATA_ID, " + id);
                if (TextUtils.isEmpty(selection)) {
                    selection = Constants.DATA_ID + " = " + id;
                } else {
                    selection = selection + " AND " + Constants.DATA_ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        Log.d(TAG, "selection " + selection);
        db = dbHelper.getWritableDatabase();
        int cnt = db.update(DATA_TABLE, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }

    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case URI_DATA:
                return DATA_CONTENT_TYPE;
            case URI_DATA_ID:
                return DATA_CONTENT_ITEM_TYPE;
        }
        return null;
    }
}
