package com.example.scanbusapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "rfidCards.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CARDS = "cards";
    private static final String COLUMN_RFID = "rfid";
    private static final String COLUMN_CLIENT_NAME = "client_name";
    private static final String COLUMN_FORFAIT_ACTIVE = "forfait_active";
    private static final String COLUMN_FORFAIT_EXPIRATION = "forfait_expiration";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CARDS_TABLE = "CREATE TABLE " + TABLE_CARDS + "("
                + COLUMN_RFID + " TEXT PRIMARY KEY,"
                + COLUMN_CLIENT_NAME + " TEXT,"
                + COLUMN_FORFAIT_ACTIVE + " INTEGER,"
                + COLUMN_FORFAIT_EXPIRATION + " TEXT"
                + ")";
        db.execSQL(CREATE_CARDS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARDS);
        onCreate(db);
    }

    // Insérer ou mettre à jour les informations d'une carte
    public void saveCardInfo(String rfid, String clientName, boolean forfaitActive, String forfaitExpiration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RFID, rfid);
        values.put(COLUMN_CLIENT_NAME, clientName);
        values.put(COLUMN_FORFAIT_ACTIVE, forfaitActive ? 1 : 0);
        values.put(COLUMN_FORFAIT_EXPIRATION, forfaitExpiration);

        // Insérer ou mettre à jour la carte
        db.insertWithOnConflict(TABLE_CARDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Récupérer les informations d'une carte
    public Cursor getCardInfo(String rfid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CARDS, new String[]{COLUMN_CLIENT_NAME, COLUMN_FORFAIT_ACTIVE, COLUMN_FORFAIT_EXPIRATION},
                COLUMN_RFID + "=?", new String[]{rfid}, null, null, null);
        return cursor;
    }
}
