package tw.ironthomas.smartiot;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {


    public static final String DATABASE_NAME = "mydata.db";

    public static final int VERSION = 1;

    //public String _TableName = "devices";
    private static SQLiteDatabase db;


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }


    public static SQLiteDatabase getDatabase(Context context) {
        if (db == null || !db.isOpen()) {
            db = new DBHelper(context).getWritableDatabase();
        }

        return db;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL = "CREATE TABLE IF NOT EXISTS devices ( " +
        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "MANAGER_ID VARCHAR(40), " +
        "DEVICE_NAME VARCHAR(50), " +
        "DEVICE_ID VARCHAR(20), " +
        "SERVER_IP VARCHAR(60), " +
        "CAMERA_IP VARCHAR(60), " +
        "BTN1 VARCHAR(30), " +
        "BTN2 VARCHAR(30), " +
        "BTN3 VARCHAR(30), " +
        "BTN4 VARCHAR(30)," +
        "SECURITY INT(1)" +
        ");";
        db.execSQL(SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}