package com.example.scanbusapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "rfidCards.db";
    private static final int DATABASE_VERSION = 2; // Updated version

    private static final String TABLE_CARDS = "cards";
    private static final String COLUMN_RFID = "rfid";
    private static final String COLUMN_CLIENT_NAME = "client_name";
    private static final String COLUMN_FORFAIT_ACTIVE = "forfait_active";
    private static final String COLUMN_FORFAIT_EXPIRATION = "forfait_expiration";

    // Nouvelle table pour les transactions hors ligne
    private static final String TABLE_OFFLINE_TRANSACTIONS = "offline_transactions";
    private static final String COLUMN_FORFAIT_TYPE = "forfait_type";

    // Nouvelle table pour les vérifications hors ligne
    private static final String TABLE_OFFLINE_VERIFICATIONS = "offline_verifications";
    private static final String COLUMN_FORFAIT_STATUS = "forfait_status";

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

        String CREATE_OFFLINE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_OFFLINE_TRANSACTIONS + "("
                + COLUMN_RFID + " TEXT,"
                + COLUMN_FORFAIT_TYPE + " TEXT,"
                + "PRIMARY KEY (" + COLUMN_RFID + ", " + COLUMN_FORFAIT_TYPE + ")"
                + ")";

        String CREATE_OFFLINE_VERIFICATIONS_TABLE = "CREATE TABLE " + TABLE_OFFLINE_VERIFICATIONS + "("
                + COLUMN_RFID + " TEXT,"
                + COLUMN_CLIENT_NAME + " TEXT,"
                + COLUMN_FORFAIT_STATUS + " TEXT"
                + ")";

        db.execSQL(CREATE_CARDS_TABLE);
        db.execSQL(CREATE_OFFLINE_TRANSACTIONS_TABLE);
        db.execSQL(CREATE_OFFLINE_VERIFICATIONS_TABLE);  // Create new table for offline verifications
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String CREATE_OFFLINE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_OFFLINE_TRANSACTIONS + "("
                    + COLUMN_RFID + " TEXT,"
                    + COLUMN_FORFAIT_TYPE + " TEXT,"
                    + "PRIMARY KEY (" + COLUMN_RFID + ", " + COLUMN_FORFAIT_TYPE + ")"
                    + ")";
            db.execSQL(CREATE_OFFLINE_TRANSACTIONS_TABLE);

            String CREATE_OFFLINE_VERIFICATIONS_TABLE = "CREATE TABLE " + TABLE_OFFLINE_VERIFICATIONS + "("
                    + COLUMN_RFID + " TEXT,"
                    + COLUMN_CLIENT_NAME + " TEXT,"
                    + COLUMN_FORFAIT_STATUS + " TEXT"
                    + ")";
            db.execSQL(CREATE_OFFLINE_VERIFICATIONS_TABLE);
        }
    }

    // Insérer ou mettre à jour les informations d'une carte
    public void saveCardInfo(String rfid, String clientName, boolean forfaitActive, String forfaitExpiration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RFID, rfid);
        values.put(COLUMN_CLIENT_NAME, clientName);
        values.put(COLUMN_FORFAIT_ACTIVE, forfaitActive ? 1 : 0);
        values.put(COLUMN_FORFAIT_EXPIRATION, forfaitExpiration);

        db.insertWithOnConflict(TABLE_CARDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Insérer une transaction hors ligne
    public void saveOfflineTransaction(String rfid, String forfaitType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RFID, rfid);
        values.put(COLUMN_FORFAIT_TYPE, forfaitType);

        db.insertWithOnConflict(TABLE_OFFLINE_TRANSACTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Enregistrer une vérification hors ligne
    public void saveOfflineVerification(String rfid, String clientName, String forfaitStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_RFID, rfid);
        values.put(COLUMN_CLIENT_NAME, clientName);
        values.put(COLUMN_FORFAIT_STATUS, forfaitStatus);

        db.insertWithOnConflict(TABLE_OFFLINE_VERIFICATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Supprimer une transaction hors ligne spécifique
    public void deleteOfflineTransaction(String rfid, String forfaitType) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_OFFLINE_TRANSACTIONS, COLUMN_RFID + "=? AND " + COLUMN_FORFAIT_TYPE + "=?",
                new String[]{rfid, forfaitType});
        db.close();
    }

    // Supprimer une vérification hors ligne spécifique après synchronisation
    public void deleteOfflineVerification(String rfid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_OFFLINE_VERIFICATIONS, COLUMN_RFID + "=?", new String[]{rfid});
        db.close();
    }

    // Récupérer les informations d'une carte
    public Cursor getCardInfo(String rfid) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CARDS, new String[]{COLUMN_CLIENT_NAME, COLUMN_FORFAIT_ACTIVE, COLUMN_FORFAIT_EXPIRATION},
                COLUMN_RFID + "=?", new String[]{rfid}, null, null, null);
    }

    // Récupérer toutes les transactions hors ligne
    public Cursor getOfflineTransactions() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_OFFLINE_TRANSACTIONS, new String[]{COLUMN_RFID, COLUMN_FORFAIT_TYPE},
                null, null, null, null, null);
    }

    // Récupérer toutes les vérifications hors ligne
    public Cursor getOfflineVerifications() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_OFFLINE_VERIFICATIONS, new String[]{COLUMN_RFID, COLUMN_CLIENT_NAME, COLUMN_FORFAIT_STATUS},
                null, null, null, null, null);
    }

    // Supprimer toutes les transactions hors ligne une fois synchronisées
    public void clearOfflineTransactions() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_OFFLINE_TRANSACTIONS, null, null);
        db.close();
    }
}
