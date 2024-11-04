package com.example.scanbusapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "rfidCards.db";
    private static final int DATABASE_VERSION = 4;
    private static final String TAG = "LocalDatabaseHelper";

    // Table des cartes
    private static final String TABLE_CARDS = "cards";
    private static final String COLUMN_RFID = "rfid";
    private static final String COLUMN_CLIENT_NAME = "client_name";
    private static final String COLUMN_FORFAIT_ACTIVE = "forfait_active";
    private static final String COLUMN_FORFAIT_EXPIRATION = "forfait_expiration";
    private static final String COLUMN_FORFAIT_STATUS = "forfait_status";

    // Table des transactions hors ligne
    private static final String TABLE_OFFLINE_TRANSACTIONS = "offline_transactions";
    private static final String COLUMN_FORFAIT_TYPE = "forfait_type";

    // Table des vérifications de forfaits
    private static final String TABLE_FORFAIT_VERIFICATIONS = "forfait_verifications";
    private static final String COLUMN_NOM_CLIENT = "nom_client";
    private static final String COLUMN_STATUT_FORFAIT = "statut_forfait";
    private static final String COLUMN_ANDROID_ID = "android_id";
    private static final String COLUMN_ROLE_UTILISATEUR = "role_utilisateur";
    private static final String COLUMN_NOM_UTILISATEUR = "nom_utilisateur";
    private static final String COLUMN_FORFAIT_ACTIVER_PAR_CLIENT = "forfait_activer_par_client";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            String CREATE_CARDS_TABLE = "CREATE TABLE " + TABLE_CARDS + " ("
                    + COLUMN_RFID + " TEXT PRIMARY KEY, "
                    + COLUMN_CLIENT_NAME + " TEXT, "
                    + COLUMN_FORFAIT_ACTIVE + " INTEGER, "
                    + COLUMN_FORFAIT_EXPIRATION + " TEXT, "
                    + COLUMN_FORFAIT_STATUS + " TEXT)";
            db.execSQL(CREATE_CARDS_TABLE);

            String CREATE_OFFLINE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_OFFLINE_TRANSACTIONS + " ("
                    + COLUMN_RFID + " TEXT, "
                    + COLUMN_FORFAIT_TYPE + " TEXT, "
                    + "PRIMARY KEY (" + COLUMN_RFID + ", " + COLUMN_FORFAIT_TYPE + "))";
            db.execSQL(CREATE_OFFLINE_TRANSACTIONS_TABLE);

            String CREATE_FORFAIT_VERIFICATIONS_TABLE = "CREATE TABLE " + TABLE_FORFAIT_VERIFICATIONS + " ("
                    + COLUMN_RFID + " TEXT, "
                    + COLUMN_NOM_CLIENT + " TEXT, "
                    + COLUMN_STATUT_FORFAIT + " TEXT, "
                    + COLUMN_ANDROID_ID + " TEXT, "
                    + COLUMN_ROLE_UTILISATEUR + " TEXT, "
                    + COLUMN_NOM_UTILISATEUR + " TEXT, "
                    + COLUMN_FORFAIT_ACTIVER_PAR_CLIENT + " INTEGER)";
            db.execSQL(CREATE_FORFAIT_VERIFICATIONS_TABLE);

            Log.d(TAG, "Tables créées avec succès.");
        } catch (SQLException e) {
            Log.e(TAG, "Erreur lors de la création des tables : " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_CARDS + " ADD COLUMN " + COLUMN_FORFAIT_STATUS + " TEXT");
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORFAIT_VERIFICATIONS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFLINE_TRANSACTIONS);
                onCreate(db);
                Log.d(TAG, "Mise à jour des tables réussie.");
            } catch (SQLException e) {
                Log.e(TAG, "Erreur lors de la mise à jour des tables : " + e.getMessage());
            }
        }
    }

    // Méthode pour sauvegarder une transaction hors ligne
    public synchronized void saveOfflineTransaction(String rfid, String forfaitType) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_RFID, rfid);
            values.put(COLUMN_FORFAIT_TYPE, forfaitType);

            db.insertWithOnConflict(TABLE_OFFLINE_TRANSACTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
            Log.d(TAG, "Transaction hors ligne enregistrée avec succès.");
        } catch (SQLException e) {
            Log.e(TAG, "Erreur lors de la sauvegarde de la transaction hors ligne : " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // Méthode pour récupérer toutes les transactions hors ligne
    public Cursor getOfflineTransactions() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_OFFLINE_TRANSACTIONS,
                new String[]{COLUMN_RFID, COLUMN_FORFAIT_TYPE},
                null, null, null, null, null
        );
    }

    // Méthode pour sauvegarder ou mettre à jour les informations de la carte
    public synchronized void saveCardInfo(String rfid, String clientName, boolean forfaitActive, String forfaitExpiration, String forfaitStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_RFID, rfid);
            values.put(COLUMN_CLIENT_NAME, clientName);
            values.put(COLUMN_FORFAIT_ACTIVE, forfaitActive ? 1 : 0);
            values.put(COLUMN_FORFAIT_EXPIRATION, forfaitExpiration);
            values.put(COLUMN_FORFAIT_STATUS, forfaitStatus);

            db.insertWithOnConflict(TABLE_CARDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
            Log.d(TAG, "Informations de la carte enregistrées avec succès.");
        } catch (SQLException e) {
            Log.e(TAG, "Erreur lors de la sauvegarde des informations de la carte : " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // Méthode pour récupérer les informations d'une carte par RFID
    public Cursor getCardInfo(String rfid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_CARDS,
                new String[]{COLUMN_CLIENT_NAME, COLUMN_FORFAIT_ACTIVE, COLUMN_FORFAIT_EXPIRATION, COLUMN_FORFAIT_STATUS},
                COLUMN_RFID + "=?",
                new String[]{rfid},
                null, null, null
        );
        return cursor;
    }

    // Méthode pour sauvegarder une vérification de forfait hors ligne
    public synchronized void saveOfflineVerification(String rfid, String nomClient, String statutForfait) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_RFID, rfid);
            values.put(COLUMN_NOM_CLIENT, nomClient);
            values.put(COLUMN_STATUT_FORFAIT, statutForfait);

            db.insert(TABLE_FORFAIT_VERIFICATIONS, null, values);
            db.setTransactionSuccessful();
            Log.d(TAG, "Vérification de forfait hors ligne enregistrée avec succès.");
        } catch (SQLException e) {
            Log.e(TAG, "Erreur lors de la sauvegarde de la vérification de forfait : " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }
}
