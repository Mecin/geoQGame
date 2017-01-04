package pl.dmcs.mecin.geoqgame;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "geoQGame.db";
    private static final String TABLE_HISTORY = "history";

    private static final String KEY_ID = "id";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_DISTANCE = "distance";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
                + KEY_ID + " INTEGER PRIMARY KEY, "
                + KEY_DISTANCE + " REAL, "
                + KEY_DURATION + " REAL"
                + ")";
        db.execSQL(CREATE_HISTORY_TABLE);

        Log.d("dbOnCreate","database onCreate");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    protected void addHistoryEntry(Double distance, long duration) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_DISTANCE, distance);
        values.put(KEY_DURATION, duration);

        // Inserting Row
        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }

    protected String getHistoryEntry(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_HISTORY, new String[] { KEY_ID,
                        KEY_DISTANCE, KEY_DURATION }, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();


        String returnString = cursor.getString(0) + " " + cursor.getString(1) + " " + cursor.getString(2);

        return returnString;
    }

    protected List<String> getHistory() {
        List<String> historyList = new ArrayList<String>();

        String selectQuery = "SELECT * FROM " + TABLE_HISTORY;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String historyEntryString = cursor.getString(0) + " " + cursor.getString(1) + " " + cursor.getString(2);
                historyList.add(historyEntryString);
            } while (cursor.moveToNext());
        }

        return historyList;
    }
}
