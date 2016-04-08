package edu.rasm.pickel.mcp;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class MyDBHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "routeDB.db";
    public static final String TABLE_ROUTES = "routes";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ROUTENAME = "routename";
    public static final String COLUMN_LAT = "latitude";
    public static final String COLUMN_LNG = "longitude";


    public MyDBHandler(Context context, String name,
                       CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_ROUTES_TABLE = "CREATE TABLE " +
                TABLE_ROUTES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_ROUTENAME
                + " TEXT," + COLUMN_LAT + " LONG," + COLUMN_LNG + " LONG" + ")";
        db.execSQL(CREATE_ROUTES_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTES);
        onCreate(db);

    }

    public Cursor getData(String route_name){  // this was originally used with _id, a primary key
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from " + TABLE_ROUTES + " where"
                + COLUMN_ROUTENAME + "=" + route_name + "", null );
        return res;
    }

    public ArrayList<String> getAllLocations()
    {
        ArrayList<String> array_list = new ArrayList<String>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_ROUTES, null);
        res.moveToFirst();

        while(res.isAfterLast() == false){
            array_list.add(res.getString(res.getColumnIndex(COLUMN_ROUTENAME)));
            res.moveToNext();
        }
        return array_list;
    }

    public void addLocation(GPSLocation location) {

        ContentValues values = new ContentValues();
        values.put(COLUMN_ROUTENAME, location.getName());
        values.put(COLUMN_LAT, location.getLat());
        values.put(COLUMN_LNG, location.getLng());

        SQLiteDatabase db = this.getWritableDatabase();

        db.insert(TABLE_ROUTES, null, values);
        db.close();
    }

    /** */
    public List<GPSLocation> findLocation(String routename) {

        String query = "Select * FROM " + TABLE_ROUTES + " WHERE " + COLUMN_ROUTENAME + " =  \"" + routename + "\"";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        List<GPSLocation> route = new ArrayList<GPSLocation>();

        cursor.moveToFirst();
        do {
            //cursor.moveToFirst();
            GPSLocation location = new GPSLocation();
            location.setID(Integer.parseInt(cursor.getString(0)));
            location.setName(cursor.getString(1));
            location.setLat(Long.parseLong(cursor.getString(2)));
            location.setLng(Long.parseLong(cursor.getString(3)));

            route.add(location);
            //cursor.close();
        } while (cursor.moveToNext()); //{
            //location = null;
       // }
        cursor.close();
        db.close();

        return route;
    }

    public boolean deletelocation(String routename) {

        boolean result = false;

        String query = "Select * FROM " + TABLE_ROUTES + " WHERE " + COLUMN_ROUTENAME + " =  \"" + routename + "\"";

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(query, null);

        GPSLocation location = new GPSLocation();

        if (cursor.moveToFirst()) {
            location.setID(Integer.parseInt(cursor.getString(0)));
            db.delete(TABLE_ROUTES, COLUMN_ID + " = ?",
                    new String[] { String.valueOf(location.getID()) });
            cursor.close();
            result = true;
        }
        db.close();
        return result;
    }
}
