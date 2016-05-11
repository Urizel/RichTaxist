package tt.richTaxist.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import tt.richTaxist.DB.Tables.BillingsTable;
import tt.richTaxist.DB.Tables.LocationsTable;
import tt.richTaxist.DB.Tables.OrdersTable;
import tt.richTaxist.DB.Tables.ShiftsTable;
import tt.richTaxist.DB.Tables.TaxoparksTable;
import tt.richTaxist.FirstScreenActivity;
import tt.richTaxist.Units.Billing;
import tt.richTaxist.Units.Taxopark;

public class MySQLHelper extends SQLiteOpenHelper {
    //helper is one no matter how much tables there are
    private static final String DB_NAME = "taxiDB";
    private static final int DB_VERSION = 1;
    private static final String LOG_TAG = FirstScreenActivity.LOG_TAG;
    private static MySQLHelper instance;
    public static final String CREATE_TABLE = "CREATE TABLE %s ( %s);";
    public static final String DROP_TABLE = "DROP TABLE IF EXISTS %s";
    public static final String PRIMARY_KEY = BaseColumns._ID + " integer primary key autoincrement, ";

    //singleton protects db from multi thread concurrent access
    private MySQLHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static MySQLHelper getInstance(Context context) {
        if (instance == null) {
            instance = new MySQLHelper(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        updateMyDatabase(db, 0, DB_VERSION);
        Log.d(LOG_TAG, "MySQLHelper: onCreate");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "Found new DB version. About to update to: " + String.valueOf(DB_VERSION));
        updateMyDatabase(db, oldVersion, newVersion);
    }

    private void updateMyDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            if (oldVersion < 1) {
                db.execSQL(getCreateSql(OrdersTable.TABLE_NAME, OrdersTable.FIELDS));
                db.execSQL(getCreateSql(ShiftsTable.TABLE_NAME, ShiftsTable.FIELDS));
                db.execSQL(getCreateSql(TaxoparksTable.TABLE_NAME, TaxoparksTable.FIELDS));
                db.execSQL(getCreateSql(BillingsTable.TABLE_NAME, BillingsTable.FIELDS));
                db.execSQL(getCreateSql(LocationsTable.TABLE_NAME, LocationsTable.FIELDS));
            }
            if (oldVersion < 2) {
//                db.execSQL("ALTER TABLE " + BillingsTable.TABLE_NAME + " ADD COLUMN " + BillingsTable.FAVORITE + " NUMERIC;");
            }
            populateBase(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    private void populateBase(SQLiteDatabase db){
        insertObject(db, new Billing("85/15", 15f));
        insertObject(db, new Billing("50/50", 50f));
        insertObject(db, new Taxopark("Таксовичков", true, 0));
    }
    private void insertObject(SQLiteDatabase db, Object object){
        if (object instanceof Billing) {
            ContentValues cv = BillingsTable.getContentValues((Billing) object);
            db.insert(BillingsTable.TABLE_NAME, null, cv);
        } else if (object instanceof Taxopark) {
            ContentValues cv = TaxoparksTable.getContentValues((Taxopark) object);
            db.insert(TaxoparksTable.TABLE_NAME, null, cv);
        }
    }

    private String getCreateSql(String tableName, String fields) {
        return String.format(CREATE_TABLE, tableName, fields);
    }

    private String getDropSql(String tableName) {
        return String.format(DROP_TABLE, tableName);
    }
}